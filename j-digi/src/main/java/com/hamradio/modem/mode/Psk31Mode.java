package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;
import com.hamradio.modem.tx.Psk31Varicode;

import java.util.Arrays;
import java.util.Optional;

/**
 * PSK31 (BPSK31) decoder — differential BPSK with PSK31 Varicode.
 *
 * Pipeline (per audio frame):
 *   1. AFC         — snap carrier to FFT peak before lock (500–2500 Hz)
 *   2. Baseband    — per-sample quadrature mix + one-pole LP (~32 Hz cutoff)
 *   3. Acquisition — scan all symbol offsets; lock on highest mean |cosine|
 *   4. Symbols     — average I/Q over centre window; differential decode
 *   5. Varicode    — accumulate bits; emit char on each "00" separator
 *   6. Emit        — flush on newline or ≥ 8 pending characters
 *
 * Timing constants (8000 Hz sample rate):
 *   Symbol rate     : 31.25 baud
 *   Samples/symbol  : 256
 *   LP α            : 0.025  (–3 dB ≈ 32 Hz)
 *   Carrier default : 1000 Hz (AFC-corrected before lock)
 */
public class Psk31Mode implements DigitalMode {

    // ── Protocol ──────────────────────────────────────────────────────
    private static final double SYMBOL_RATE     = 31.25;
    private static final double DEFAULT_CARRIER = 1000.0;

    // ── AFC ───────────────────────────────────────────────────────────
    private static final double AFC_MIN_HZ      = 500.0;
    private static final double AFC_MAX_HZ      = 2500.0;
    private static final double AFC_MAX_JUMP_HZ = 100.0;  // reject implausibly large steps

    // ── Signal thresholds ─────────────────────────────────────────────
    private static final double MIN_RMS         = 0.003;
    private static final double MIN_SYMBOL_MAG  = 0.006;

    // ── Lock / quality ────────────────────────────────────────────────
    /** Mean |cosine| across eval symbols required to acquire lock. */
    private static final double ACQUIRE_SCORE   = 0.45;
    /** Mean |cosine| across running quality window to stay locked. */
    private static final double QUALITY_FLOOR   = 0.25;
    /** Consecutive low-quality symbols before forced unlock. */
    private static final int    BAD_UNLOCK      = 24;
    /** Minimum symbols buffered before attempting acquisition. */
    private static final int    MIN_ACQ_SYMS    = 8;
    /** Symbols evaluated during each acquisition scan. */
    private static final int    EVAL_SYMS       = 24;

    // ── Output ────────────────────────────────────────────────────────
    private static final int    MIN_EMIT_CHARS  = 8;
    private static final int    MAX_BIT_BUF     = 512;

    // ── Debug ─────────────────────────────────────────────────────────
    private static final boolean DEBUG          = true;

    // =================================================================
    // Per-instance state
    // =================================================================

    // ── Init ──────────────────────────────────────────────────────────
    private boolean initialized = false;
    private double  sampleRate  = 8000.0;
    private int     spS         = 256;      // samples per symbol

    // ── Carrier oscillator ────────────────────────────────────────────
    private double carrierHz = DEFAULT_CARRIER;
    private double oscCos    = 1.0;
    private double oscSin    = 0.0;
    private double rotCos;                  // rotation step — set by setCarrier()
    private double rotSin;

    // ── One-pole LP (I and Q) ─────────────────────────────────────────
    private double lpAlpha = 0.025;
    private double lpI     = 0.0;
    private double lpQ     = 0.0;

    // ── Baseband sample buffer ────────────────────────────────────────
    private double[] iBuf = new double[32768];
    private double[] qBuf = new double[32768];
    private int      nBuf = 0;

    // ── Timing / lock ─────────────────────────────────────────────────
    private boolean locked     = false;
    /**
     * Buffer index of the CENTRE of the next symbol to process.
     * Advanced by spS each symbol.  Single variable replaces the
     * fragile (symOffset + processedSymbols) two-variable scheme.
     */
    private int     nextCenter = 0;

    // ── Differential decode ───────────────────────────────────────────
    private boolean havePrev = false;
    private double  prevI    = 0.0;
    private double  prevQ    = 0.0;

    // ── Quality tracking ──────────────────────────────────────────────
    private double qualSum   = 0.0;
    private int    qualCount = 0;
    private int    badCount  = 0;

    // ── Text output ───────────────────────────────────────────────────
    private final StringBuilder bits    = new StringBuilder();
    private final StringBuilder pending = new StringBuilder();

    // ── Diagnostics (exposed to callers via DecodeMessage) ────────────
    private double lastSnr  = 0.0;
    private double lastConf = 0.0;

    // =================================================================
    // DigitalMode contract
    // =================================================================

    @Override
    public String getName() {
        return "PSK31";
    }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snap, long rigHz) {
        if (snap == null || snap.getSamples() == null || snap.getSamples().length == 0)
            return Optional.empty();

        initIfNeeded(snap.getSampleRate());

        // ── Gate on signal presence ───────────────────────────────────
        if (snap.getRms() < MIN_RMS) {
            if (locked) unlock("low RMS");
            return emitPending(rigHz);
        }

        // ── AFC (unlocked only — don't disturb a running decode) ──────
        if (!locked) {
            afcUpdate(snap.getPeakFrequencyHz());
        }

        // ── Down-convert and buffer ────────────────────────────────────
        feedSamples(snap.getSamples());

        // ── Acquire or decode ──────────────────────────────────────────
        if (!locked) {
            tryAcquire();
        } else {
            processSymbols();
        }

        return emitPending(rigHz);
    }

    // =================================================================
    // Initialisation
    // =================================================================

    private void initIfNeeded(double sr) {
        if (initialized && Math.abs(sr - sampleRate) < 0.5) return;
        sampleRate = sr;
        spS = Math.max(1, (int) Math.round(sr / SYMBOL_RATE));
        // One-pole LP: –3 dB at ~32 Hz  (just above symbol-rate/2 = 15.6 Hz)
        double w = 2.0 * Math.PI * 32.0;
        lpAlpha = w / (w + sr);
        setCarrier(DEFAULT_CARRIER);
        resetAll();
        initialized = true;
        debug(String.format("init sr=%.0f spS=%d lpAlpha=%.4f", sr, spS, lpAlpha));
    }

    /** Update the rotation step WITHOUT resetting the running phasor. */
    private void setCarrier(double hz) {
        carrierHz = hz;
        double step = 2.0 * Math.PI * hz / sampleRate;
        rotCos    = Math.cos(step);
        rotSin    = Math.sin(step);
    }

    private void resetAll() {
        oscCos = 1.0;  oscSin = 0.0;
        lpI    = 0.0;  lpQ    = 0.0;
        nBuf       = 0;
        locked     = false;
        nextCenter = 0;
        havePrev   = false;  prevI = 0.0;  prevQ = 0.0;
        qualSum    = 0.0;    qualCount = 0;  badCount = 0;
        bits.setLength(0);
        pending.setLength(0);
        lastSnr  = 0.0;
        lastConf = 0.0;
    }

    private void unlock(String reason) {
        debug("unlock: " + reason);
        locked     = false;
        havePrev   = false;
        qualSum    = 0.0;    qualCount = 0;  badCount = 0;
        bits.setLength(0);
        // Keep nextCenter — if signal returns at same frequency, re-acquisition
        // is faster because the buffer still contains recent samples.
    }

    // =================================================================
    // AFC — carrier frequency tracking
    // =================================================================

    /**
     * Snap the local oscillator to the FFT peak when it falls in the
     * valid PSK31 band.  Resets the buffer on large frequency jumps to
     * avoid mixing baseband samples computed at different carrier frequencies.
     */
    private void afcUpdate(double peakHz) {
        if (peakHz < AFC_MIN_HZ || peakHz > AFC_MAX_HZ) return;
        double jump = Math.abs(peakHz - carrierHz);
        if (jump < 3.0)                return;   // within FFT quantisation noise
        if (jump > AFC_MAX_JUMP_HZ)    return;   // implausibly large — ignore

        debug(String.format("AFC %.1f → %.1f Hz (Δ%.1f)", carrierHz, peakHz, jump));

        if (jump > 15.0) {
            // Significant shift: flush stale baseband data and LP state
            nBuf = 0;
            oscCos = 1.0;  oscSin = 0.0;
            lpI    = 0.0;  lpQ    = 0.0;
        }
        setCarrier(peakHz);
    }

    // =================================================================
    // Baseband conversion
    // =================================================================

    private void feedSamples(float[] samples) {
        ensureBuf(nBuf + samples.length);
        for (float s : samples) {
            // Quadrature mix: shift carrier to DC
            double mI =  s * oscCos;
            double mQ = -s * oscSin;

            // Narrow LP — removes out-of-band noise, leaves PSK31 baseband
            lpI += lpAlpha * (mI - lpI);
            lpQ += lpAlpha * (mQ - lpQ);

            iBuf[nBuf] = lpI;
            qBuf[nBuf] = lpQ;
            nBuf++;

            // Advance running phasor (rotation-matrix multiply)
            double nc = oscCos * rotCos - oscSin * rotSin;
            double ns = oscSin * rotCos + oscCos * rotSin;
            oscCos = nc;
            oscSin = ns;
        }
    }

    // =================================================================
    // Timing acquisition
    // =================================================================

    /**
     * Scan all {@code spS} candidate symbol offsets.  For each, estimate
     * {@link #EVAL_SYMS} symbol vectors (mean I/Q over a centre window)
     * and score the offset by the mean absolute cosine between consecutive
     * symbols.
     *
     * <p>High |cosine| ≈ 1 means each symbol sits cleanly near ±1 on the
     * phase axis, which is true both during preamble (alternating +1/−1)
     * and normal data (mix of same/opposite phase).  This approach works
     * mid-stream, not only on preamble, unlike a pure anti-correlation score.
     */
    private void tryAcquire() {
        if (nBuf < spS * MIN_ACQ_SYMS) return;

        int    bestOff   = 0;
        double bestScore = -1.0;
        int    winH      = spS / 4;         // averaging window half-width

        for (int off = 0; off < spS; off++) {
            int nSyms = (nBuf - off) / spS;
            if (nSyms < MIN_ACQ_SYMS) continue;
            int eval = Math.min(nSyms, EVAL_SYMS);

            double sumAbsCos = 0.0;
            double pI = 0.0, pQ = 0.0;
            boolean hp = false;
            int pairs = 0;

            for (int s = 0; s < eval; s++) {
                int center = off + s * spS + spS / 2;
                int ws = Math.max(0, center - winH);
                int we = Math.min(nBuf, center + winH);
                if (we <= ws) continue;

                double sI = 0.0, sQ = 0.0;
                for (int i = ws; i < we; i++) { sI += iBuf[i]; sQ += qBuf[i]; }
                double n = we - ws;
                sI /= n;  sQ /= n;
                if (Math.sqrt(sI * sI + sQ * sQ) < MIN_SYMBOL_MAG) continue;

                if (hp) {
                    double dot   = pI * sI + pQ * sQ;
                    double denom = Math.sqrt((pI * pI + pQ * pQ) * (sI * sI + sQ * sQ));
                    if (denom > 1e-10) {
                        sumAbsCos += Math.abs(dot / denom);
                        pairs++;
                    }
                }
                pI = sI;  pQ = sQ;  hp = true;
            }

            if (pairs >= 4) {
                double score = sumAbsCos / pairs;
                if (score > bestScore) { bestScore = score; bestOff = off; }
            }
        }

        if (bestScore >= ACQUIRE_SCORE) {
            // Position nextCenter just before the end of the buffer so we
            // start decoding fresh from current time, not from old preamble.
            int nSymsAt = (nBuf - bestOff) / spS;
            int startSym = Math.max(0, nSymsAt - 2);
            nextCenter = bestOff + startSym * spS + spS / 2;
            havePrev   = false;
            badCount   = 0;
            locked     = true;
            debug(String.format("LOCK off=%d score=%.3f carrier=%.1f Hz", bestOff, bestScore, carrierHz));
        }
    }

    // =================================================================
    // Symbol extraction and differential BPSK decode
    // =================================================================

    /**
     * Process all complete symbols now available in the buffer.
     *
     * <p>For each symbol the I/Q phasor is estimated by averaging the
     * filtered baseband over the centre ±¼-symbol window.  The dot
     * product with the previous symbol phasor gives the phase transition:
     * <pre>
     *   dot &lt; 0  →  180° flip  →  bit '0'
     *   dot ≥ 0  →  no flip    →  bit '1'
     * </pre>
     * |cosine| is tracked as a running quality metric; consistently low
     * quality forces an unlock so the decoder can re-acquire cleanly.
     */
    private void processSymbols() {
        int winH = spS / 4;

        while (true) {
            // Need the full averaging window inside the buffer
            if (nextCenter + winH >= nBuf) break;
            if (nextCenter - winH < 0)     { nextCenter += spS; continue; }

            int ws = nextCenter - winH;
            int we = nextCenter + winH;

            double sI = 0.0, sQ = 0.0;
            for (int i = ws; i < we; i++) { sI += iBuf[i]; sQ += qBuf[i]; }
            double n   = we - ws;
            double cI  = sI / n;
            double cQ  = sQ / n;
            double mag = Math.sqrt(cI * cI + cQ * cQ);

            if (mag >= MIN_SYMBOL_MAG) {
                if (havePrev) {
                    double dot   = prevI * cI + prevQ * cQ;
                    double denom = Math.sqrt((prevI * prevI + prevQ * prevQ)
                                          * (cI   * cI   + cQ   * cQ));
                    double cosine  = (denom > 1e-10) ? (dot / denom) : 0.0;
                    double absCos  = Math.abs(cosine);

                    // ── Quality tracking ──────────────────────────────
                    qualSum += absCos;
                    qualCount++;

                    if (absCos < QUALITY_FLOOR) {
                        if (++badCount >= BAD_UNLOCK) {
                            unlock(String.format("quality %.3f < %.3f for %d symbols",
                                    absCos, QUALITY_FLOOR, badCount));
                            return;
                        }
                    } else {
                        badCount = 0;
                    }

                    // ── Differential BPSK decode ──────────────────────
                    char bit = (dot < 0.0) ? '0' : '1';
                    debug(String.format("SYM dot=%.3f cos=%.3f bit=%c mag=%.4f", dot, cosine, bit, mag));
                    pushBit(bit);

                    // ── Update diagnostics ────────────────────────────
                    lastConf = Math.min(0.99, qualSum / Math.max(1, qualCount));
                    lastSnr  = 20.0 * Math.log10(Math.max(mag, 1e-9) / MIN_SYMBOL_MAG);
                }
                prevI = cI;  prevQ = cQ;  havePrev = true;
            }

            nextCenter += spS;
        }

        compactBuffer();
    }

    // =================================================================
    // Varicode assembly
    // =================================================================

    /**
     * Append one bit, then extract all complete codewords delimited by "00".
     *
     * <p>PSK31 Varicode encoding: each ASCII character maps to a unique
     * sequence of 1s (minimum one 1) with no two consecutive 0s inside
     * the codeword.  The inter-character separator is two consecutive 0s ("00").
     */
    private void pushBit(char b) {
        bits.append(b);

        // Hard cap — prevents runaway on bad lock or all-noise
        if (bits.length() > MAX_BIT_BUF) {
            bits.delete(0, bits.length() - MAX_BIT_BUF);
        }

        // Harvest complete characters
        while (true) {
            int sep = bits.indexOf("00");
            if (sep < 0) break;
            String code = bits.substring(0, sep);
            bits.delete(0, sep + 2);
            if (!code.isEmpty()) {
                Character ch = Psk31Varicode.decodeVaricodeBits(code);
                if (ch != null) {
                    debug("CHAR '" + printable(ch) + "' ← " + code);
                    pending.append(ch);
                }
            }
        }
    }

    // =================================================================
    // Text emission
    // =================================================================

    private Optional<DecodeMessage> emitPending(long rigHz) {
        if (pending.length() == 0) return Optional.empty();

        boolean hasNewline = pending.indexOf("\n") >= 0
                          || pending.indexOf("\r") >= 0;

        if (!hasNewline && pending.length() < MIN_EMIT_CHARS) return Optional.empty();

        String text = pending.toString();
        pending.setLength(0);
        debug("EMIT " + printable(text));

        return Optional.of(new DecodeMessage(
                ModeType.PSK31, text, rigHz, carrierHz, lastSnr, lastConf));
    }

    // =================================================================
    // Buffer management
    // =================================================================

    /**
     * Discard consumed samples, keeping a short tail for context.
     * Adjusts {@link #nextCenter} by the same amount.
     */
    private void compactBuffer() {
        int keepFrom = Math.max(0, nextCenter - spS * 4);
        if (keepFrom < spS) return;   // not worth the copy yet

        int rem = nBuf - keepFrom;
        if (rem <= 0) { nBuf = 0; nextCenter = 0; return; }

        System.arraycopy(iBuf, keepFrom, iBuf, 0, rem);
        System.arraycopy(qBuf, keepFrom, qBuf, 0, rem);
        nBuf -= keepFrom;
        nextCenter -= keepFrom;
        if (nextCenter < 0) nextCenter = 0;
    }

    private void ensureBuf(int needed) {
        if (needed <= iBuf.length) return;
        int sz = iBuf.length;
        while (sz < needed) sz *= 2;
        iBuf = Arrays.copyOf(iBuf, sz);
        qBuf = Arrays.copyOf(qBuf, sz);
    }

    // =================================================================
    // Debug helpers
    // =================================================================

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[PSK31] " + msg);
    }

    private static String printable(char c) {
        if (c == '\n') return "\\n";
        if (c == '\r') return "\\r";
        if (c == ' ')  return "' '";
        return String.valueOf(c);
    }

    private static String printable(String s) {
        return s.replace("\n", "\\n").replace("\r", "\\r");
    }
}
