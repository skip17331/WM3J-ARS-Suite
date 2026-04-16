package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.Goertzel;
import com.hamradio.modem.dsp.ViterbiK7;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Arrays;
import java.util.Optional;

/**
 * MFSK16 decoder with K=7 rate-1/2 Viterbi FEC (matching Mfsk16Transmitter).
 *
 * Parameters:
 *   16 tones, 15.625 Hz spacing = symbol rate
 *   Samples/symbol : 512 at 8 kHz  (exact FFT-bin alignment)
 *   Default centre : 1500 Hz; tone range ~1391–1625 Hz (bins 89–104)
 *
 * Decode pipeline per tone:
 *   1. Goertzel → 16 tone powers → peak tone index.
 *   2. IFK+ (mod 16) → 4-bit nibble.
 *   3. Nibble bits fed pairwise to the streaming ViterbiK7 decoder
 *      (2 feedSymbol calls per nibble = 2 decoded bits per tone).
 *   4. Collect 8 decoded bits → assemble byte → emit printable char.
 *
 * Latency: ViterbiK7.DEPTH = 35 symbols = 35 / 2 = 17–18 tones before
 * output begins.  The 16-byte preamble (64 tones) covers this completely.
 */
public class Mfsk16Mode implements DigitalMode {

    // ── Protocol ──────────────────────────────────────────────────────
    private static final int    NUM_TONES      = 16;
    /** Bin 89 at 8 kHz: 89 × 8000/512 = 1390.625 Hz — lowest tone. */
    private static final int    FIRST_BIN      = 89;
    /** Exact tone spacing / symbol rate (Hz). */
    private static final double SPACING        = 8000.0 / 512;   // 15.625 Hz

    // ── Thresholds ────────────────────────────────────────────────────
    private static final double MIN_RMS        = 0.003;
    /** Peakness must exceed this multiple of random (1/16) to lock. */
    private static final double LOCK_RATIO     = 2.5;
    /** Fraction of max-power tone that second-best must be under to trust a decode. */
    private static final double SOFT_THRESHOLD = 0.70;
    /** Minimum consecutive good symbols before text is emitted. */
    private static final int    MIN_GOOD_SYMS  = 4;

    private static final boolean DEBUG         = false;

    // =================================================================
    // Per-instance state
    // =================================================================

    private boolean initialized = false;
    private double  sampleRate  = 8000.0;
    private int     spS         = 512;
    private double[] toneFreqs  = buildToneFreqs(8000.0);

    // ── Sample accumulation ───────────────────────────────────────────
    /** Raw samples buffered for acquisition or feeding symBuf. */
    private float[] rawBuf      = new float[16384];
    private int     nRaw        = 0;

    // ── Symbol timing ─────────────────────────────────────────────────
    private boolean symLocked   = false;
    /** Phase within the symbol period where each new symbol starts (index into rawBuf). */
    private int     symOffset   = 0;

    // ── Per-symbol decode buffer ──────────────────────────────────────
    private float[] symBuf;

    // ── IFK+ state ────────────────────────────────────────────────────
    /** Previous received tone index; -1 = no reference yet. */
    private int     prevTone     = -1;

    // ── Viterbi FEC decoder ───────────────────────────────────────────
    private final ViterbiK7 viterbi = new ViterbiK7();
    /** Decoded-bit accumulator; filled LSB-first, 8 bits → 1 byte. */
    private int     decodedBits  = 0;
    private int     decodedCount = 0;

    // ── Confidence gate ───────────────────────────────────────────────
    private int     goodSymCount = 0;

    // ── Output ────────────────────────────────────────────────────────
    private final StringBuilder pending = new StringBuilder();
    private double lastSnr  = 0.0;
    private double lastConf = 0.0;

    // =================================================================
    // DigitalMode contract
    // =================================================================

    @Override
    public String getName() { return "MFSK16"; }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snap, long rigHz) {
        if (snap == null || snap.getSamples() == null || snap.getSamples().length == 0)
            return Optional.empty();

        initIfNeeded(snap.getSampleRate());

        if (snap.getRms() < MIN_RMS) {
            resetDecodeState();
            return emitPending(rigHz);
        }

        appendRaw(snap.getSamples());

        if (!symLocked) {
            tryAcquire();
        }

        if (symLocked) {
            drainSymbols();
        }

        return emitPending(rigHz);
    }

    // =================================================================
    // Initialisation
    // =================================================================

    private void initIfNeeded(double sr) {
        if (initialized && Math.abs(sr - sampleRate) < 0.5) return;
        sampleRate = sr;
        spS        = Math.max(1, (int) Math.round(sr / SPACING));
        toneFreqs  = buildToneFreqs(sr);
        symBuf     = new float[spS];
        resetDecodeState();
        initialized = true;
        debug(String.format("init sr=%.0f spS=%d tone[0]=%.3f Hz tone[15]=%.3f Hz",
                sr, spS, toneFreqs[0], toneFreqs[15]));
    }

    private static double[] buildToneFreqs(double sr) {
        double spacing = sr / 512;
        double[] f = new double[NUM_TONES];
        for (int i = 0; i < NUM_TONES; i++) f[i] = (FIRST_BIN + i) * spacing;
        return f;
    }

    private void resetDecodeState() {
        prevTone     = -1;
        decodedBits  = 0;
        decodedCount = 0;
        viterbi.reset();
        goodSymCount = 0;
        symLocked    = false;
        symOffset    = 0;
        nRaw         = 0;
        pending.setLength(0);
        lastSnr  = 0.0;
        lastConf = 0.0;
    }

    // =================================================================
    // Raw sample buffer
    // =================================================================

    private void appendRaw(float[] samples) {
        // Compact: drop samples we've already processed
        if (nRaw + samples.length > rawBuf.length) {
            int keep = symLocked ? Math.max(0, symOffset - spS) : 0;
            if (keep > 0) {
                System.arraycopy(rawBuf, keep, rawBuf, 0, nRaw - keep);
                nRaw      -= keep;
                symOffset -= keep;
                if (symOffset < 0) symOffset = 0;
            }
            if (nRaw + samples.length > rawBuf.length) {
                rawBuf = Arrays.copyOf(rawBuf, rawBuf.length * 2);
                if (symBuf == null) symBuf = new float[spS];
            }
        }
        System.arraycopy(samples, 0, rawBuf, nRaw, samples.length);
        nRaw += samples.length;
    }

    // =================================================================
    // Symbol timing acquisition
    // =================================================================

    /**
     * Try four evenly-spaced candidate offsets.  Score each by the mean
     * peakness (max-bin-power / avg-bin-power × numTones) across 4 symbols.
     * Lock when any candidate exceeds LOCK_RATIO × expected random level.
     */
    private void tryAcquire() {
        int evalSyms = 4;
        if (nRaw < spS * (evalSyms + 1)) return;

        double bestScore = -1.0;
        int    bestOff   = 0;

        for (int i = 0; i < 4; i++) {
            int off   = i * (spS / 4);
            double score = 0.0;
            int    count = 0;
            for (int s = 0; s < evalSyms; s++) {
                int start = off + s * spS;
                if (start + spS > nRaw) break;
                double peakness = peaknessAt(start);
                score += peakness;
                count++;
            }
            if (count > 0) {
                score /= count;
                if (score > bestScore) { bestScore = score; bestOff = i * (spS / 4); }
            }
        }

        if (bestScore >= LOCK_RATIO) {
            symLocked    = true;
            symOffset    = bestOff;
            prevTone     = -1;
            decodedBits  = 0;
            decodedCount = 0;
            viterbi.reset();
            goodSymCount = 0;
            debug(String.format("SYM LOCK off=%d score=%.2f", bestOff, bestScore));
        }
    }

    /** Peakness = (max tone power / avg tone power) normalised to [0, numTones]. */
    private double peaknessAt(int start) {
        System.arraycopy(rawBuf, start, symBuf, 0, spS);
        double max = 0.0, sum = 0.0;
        for (double f : toneFreqs) {
            double p = Goertzel.power(symBuf, (float) sampleRate, f);
            sum += p;
            if (p > max) max = p;
        }
        return (sum > 1e-12) ? (max / sum * NUM_TONES) : 0.0;
    }

    // =================================================================
    // Symbol extraction and decode
    // =================================================================

    private void drainSymbols() {
        while (symOffset + spS <= nRaw) {
            System.arraycopy(rawBuf, symOffset, symBuf, 0, spS);
            decodeSymbol(symBuf);
            symOffset += spS;
        }

        // Compact the raw buffer
        if (symOffset > spS * 4) {
            int drop = symOffset - spS * 2;
            System.arraycopy(rawBuf, drop, rawBuf, 0, nRaw - drop);
            nRaw      -= drop;
            symOffset -= drop;
        }
    }

    private void decodeSymbol(float[] sym) {
        // Compute power at all 16 tone frequencies
        double[] powers = new double[NUM_TONES];
        double   maxP   = 0.0;
        int      bestT  = 0;
        double   sumP   = 0.0;

        for (int i = 0; i < NUM_TONES; i++) {
            powers[i] = Goertzel.power(sym, (float) sampleRate, toneFreqs[i]);
            sumP += powers[i];
            if (powers[i] > maxP) { maxP = powers[i]; bestT = i; }
        }

        // Find second-best for confidence estimate
        double secondP = 0.0;
        for (int i = 0; i < NUM_TONES; i++) {
            if (i != bestT && powers[i] > secondP) secondP = powers[i];
        }

        double peakness = (sumP > 1e-12) ? maxP / sumP * NUM_TONES : 0.0;
        double conf     = (secondP > 1e-12) ? maxP / (maxP + secondP) : 0.5;

        lastSnr  = (sumP > 1e-12) ? 10.0 * Math.log10(maxP / (sumP / NUM_TONES)) : 0.0;
        lastConf = conf;

        debug(String.format("TONE %2d peakness=%.2f conf=%.3f", bestT, peakness, conf));

        // Quality gate: reject noisy symbols (prevents garbage in pending)
        boolean good = peakness >= LOCK_RATIO && conf < SOFT_THRESHOLD;
        if (!good) {
            goodSymCount = 0;
            // On repeated bad symbols, unlock and re-acquire
            if (prevTone >= 0) prevTone = bestT;  // keep tracking but don't emit
            return;
        }
        goodSymCount++;

        // IFK+ differential decode
        if (prevTone < 0) {
            // First tone after sync: establish reference, no data output
            prevTone = bestT;
            return;
        }

        int nibble = (bestT - prevTone - 1 + NUM_TONES) % NUM_TONES;
        prevTone = bestT;

        debug(String.format("  → nibble 0x%X", nibble));
        feedNibbleToViterbi(nibble);
    }

    // =================================================================
    // Nibble → Viterbi → byte assembly
    // =================================================================

    /**
     * Feed the 4 bits of a nibble as two rate-1/2 encoded symbols into the
     * Viterbi decoder (MSB pair first), then collect any decoded bits.
     *
     * Nibble bit layout (MSB at bit 3):
     *   feedSymbol(bit3, bit2) — upper encoded symbol
     *   feedSymbol(bit1, bit0) — lower encoded symbol
     * → 2 decoded data bits per nibble, assembled LSB-first into bytes.
     */
    private void feedNibbleToViterbi(int nibble) {
        // Upper encoded symbol
        int b1 = viterbi.feedSymbol((nibble >> 3) & 1, (nibble >> 2) & 1);
        if (b1 >= 0) collectBit(b1);

        // Lower encoded symbol
        int b2 = viterbi.feedSymbol((nibble >> 1) & 1, nibble & 1);
        if (b2 >= 0) collectBit(b2);
    }

    private void collectBit(int bit) {
        // Accumulate LSB-first: first decoded bit → bit 0 of the byte
        decodedBits |= (bit << decodedCount);
        decodedCount++;
        if (decodedCount == 8) {
            int b = decodedBits & 0xFF;
            decodedBits  = 0;
            decodedCount = 0;
            if (b == '\n' || b == '\r' || (b >= 0x20 && b <= 0x7E)) {
                debug("CHAR '" + (char) b + "'");
                pending.append((char) b);
            }
        }
    }

    // =================================================================
    // Text emission
    // =================================================================

    private Optional<DecodeMessage> emitPending(long rigHz) {
        if (pending.length() == 0) return Optional.empty();
        if (goodSymCount < MIN_GOOD_SYMS && pending.length() < 8) return Optional.empty();

        String text = pending.toString();
        pending.setLength(0);
        if (text.isBlank()) return Optional.empty();

        debug("EMIT '" + text.trim() + "'");
        return Optional.of(new DecodeMessage(
                ModeType.MFSK16, text, rigHz, toneFreqs[NUM_TONES / 2], lastSnr, lastConf));
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[MFSK16] " + msg);
    }
}
