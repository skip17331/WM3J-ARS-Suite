package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.Goertzel;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Arrays;
import java.util.Optional;

/**
 * DominoEX MFSK decoder (Greenman ZL1BPU / Wassilieff ZL2AFP).
 *
 * Supported variants (select via {@link Variant}):
 *
 *   Variant      | Baud    | spS (8 kHz) | BW (18 tones)
 *   -------------|---------|-------------|---------------
 *   DOMINOEX4    | 3.90625 |    2048     | ~66 Hz
 *   DOMINOEX8    | 7.8125  |    1024     | ~133 Hz
 *   DOMINOEX16   | 15.625  |     512     | ~266 Hz
 *
 * Default: DOMINOEX8 (most common for general HF work).
 *
 * Key difference from MFSK16:
 *   DominoEX uses 18 tones (not 16).  With 4-bit nibbles and IFK+1, the
 *   maximum possible tone jump is nibble(15) + 1 = 16, which is less than
 *   18, so the same tone is NEVER repeated.  Received "nibble" values of
 *   16 or 17 are impossible for valid data and are used as idle/sync markers.
 *
 * IFK+ (Incremental Frequency Keying Plus):
 *   tx_tone = (prev_tone + nibble + 1) mod 18
 *   rx_nibble = (tone − prev_tone − 1 + 18) mod 18
 *   Values 16–17 → treated as inter-word / sync (emit a space if
 *   pending nibble assembly is in-progress, then reset state).
 *
 * Character encoding: two nibbles per ASCII byte (high nibble first).
 * Tone frequencies: 18 exact FFT-bin-aligned tones centred near 1500 Hz.
 */
public class DominoExMode implements DigitalMode {

    // ── Variants ──────────────────────────────────────────────────────
    public enum Variant {
        DOMINOEX4 (8000.0 / 2048),
        DOMINOEX8 (8000.0 / 1024),
        DOMINOEX16(8000.0 /  512);

        /** Exact symbol rate at 8 kHz (Hz = baud). */
        public final double symbolRate;
        Variant(double sr) { this.symbolRate = sr; }
    }

    // ── Protocol constants ────────────────────────────────────────────
    private static final int    NUM_TONES    = 18;
    /** Maximum valid data nibble (0–15); values 16–17 are sync markers. */
    private static final int    MAX_NIBBLE   = 15;

    // ── Thresholds ────────────────────────────────────────────────────
    private static final double MIN_RMS      = 0.003;
    private static final double LOCK_RATIO   = 2.3;   // peakness to acquire lock
    private static final double CONF_GATE    = 0.70;  // max(second/best) to trust symbol
    private static final int    MIN_GOOD     = 4;     // good symbols before emitting

    private static final boolean DEBUG       = false;

    // =================================================================
    // Per-instance configuration
    // =================================================================
    private final Variant  variant;
    private final double   nominalRate;   // symbol rate this variant was built for

    // =================================================================
    // Per-instance state (reset on sample-rate change)
    // =================================================================
    private boolean initialized = false;
    private double  sampleRate  = 8000.0;
    private int     spS;           // samples per symbol
    private double[] toneFreqs;    // 18 tone frequencies

    // ── Sample buffer ─────────────────────────────────────────────────
    private float[] rawBuf  = new float[32768];
    private int     nRaw    = 0;

    // ── Symbol timing ─────────────────────────────────────────────────
    private boolean symLocked  = false;
    private int     symOffset  = 0;
    private float[] symBuf;

    // ── IFK+ ──────────────────────────────────────────────────────────
    private int  prevTone  = -1;

    // ── Nibble assembly ───────────────────────────────────────────────
    private boolean haveHigh  = false;
    private int     highNibble = 0;

    // ── Quality ───────────────────────────────────────────────────────
    private int goodSymCount = 0;

    // ── Output ────────────────────────────────────────────────────────
    private final StringBuilder pending = new StringBuilder();
    private double lastSnr  = 0.0;
    private double lastConf = 0.0;

    // =================================================================
    // Constructors
    // =================================================================

    public DominoExMode() {
        this(Variant.DOMINOEX8);
    }

    public DominoExMode(Variant variant) {
        this.variant     = variant;
        this.nominalRate = variant.symbolRate;
    }

    // =================================================================
    // DigitalMode contract
    // =================================================================

    @Override
    public String getName() { return variant.name().replace("DOMINOEX", "DominoEX "); }

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

        if (!symLocked) tryAcquire();
        if  (symLocked) drainSymbols();

        return emitPending(rigHz);
    }

    // =================================================================
    // Initialisation
    // =================================================================

    private void initIfNeeded(double sr) {
        if (initialized && Math.abs(sr - sampleRate) < 0.5) return;
        sampleRate = sr;
        // Use the nearest integer spS to the nominal rate
        spS       = Math.max(1, (int) Math.round(sr / nominalRate));
        toneFreqs = buildToneFreqs(sr, spS);
        symBuf    = new float[spS];
        resetDecodeState();
        initialized = true;
        debug(String.format("init %s sr=%.0f spS=%d tone[0]=%.3f Hz tone[17]=%.3f Hz",
                variant, sr, spS, toneFreqs[0], toneFreqs[17]));
    }

    /**
     * Build 18 tone frequencies aligned to exact FFT bins of width (sr/spS).
     * The first bin is chosen so that the 18 tones straddle 1500 Hz.
     */
    private static double[] buildToneFreqs(double sr, int spS) {
        double binWidth = sr / spS;
        // Center of 18 tones at ~1500 Hz: firstBin = round(1500/binWidth) - 9 + 1
        // (9 tones below centre + tone 9 above = symmetric around the gap between tones 8 and 9)
        int firstBin = (int) Math.round(1500.0 / binWidth) - 8;
        double[] f = new double[NUM_TONES];
        for (int i = 0; i < NUM_TONES; i++) f[i] = (firstBin + i) * binWidth;
        return f;
    }

    private void resetDecodeState() {
        prevTone     = -1;
        haveHigh     = false;
        highNibble   = 0;
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
        if (nRaw + samples.length > rawBuf.length) {
            int keep = symLocked ? Math.max(0, symOffset - spS) : 0;
            if (keep > 0) {
                System.arraycopy(rawBuf, keep, rawBuf, 0, nRaw - keep);
                nRaw      -= keep;
                symOffset -= keep;
                if (symOffset < 0) symOffset = 0;
            }
            if (nRaw + samples.length > rawBuf.length)
                rawBuf = Arrays.copyOf(rawBuf, rawBuf.length * 2);
        }
        System.arraycopy(samples, 0, rawBuf, nRaw, samples.length);
        nRaw += samples.length;
    }

    // =================================================================
    // Symbol timing acquisition
    // =================================================================

    private void tryAcquire() {
        int evalSyms = 4;
        if (nRaw < spS * (evalSyms + 1)) return;

        double bestScore = -1.0;
        int    bestOff   = 0;

        for (int i = 0; i < 4; i++) {
            int off = i * (spS / 4);
            double score = 0.0;
            int    count = 0;
            for (int s = 0; s < evalSyms; s++) {
                int start = off + s * spS;
                if (start + spS > nRaw) break;
                score += peaknessAt(start);
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
            haveHigh     = false;
            goodSymCount = 0;
            debug(String.format("SYM LOCK off=%d score=%.2f", bestOff, bestScore));
        }
    }

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
        if (symOffset > spS * 4) {
            int drop = symOffset - spS * 2;
            System.arraycopy(rawBuf, drop, rawBuf, 0, nRaw - drop);
            nRaw      -= drop;
            symOffset -= drop;
        }
    }

    private void decodeSymbol(float[] sym) {
        double[] powers = new double[NUM_TONES];
        double   maxP   = 0.0, sumP = 0.0;
        int      bestT  = 0;

        for (int i = 0; i < NUM_TONES; i++) {
            powers[i] = Goertzel.power(sym, (float) sampleRate, toneFreqs[i]);
            sumP += powers[i];
            if (powers[i] > maxP) { maxP = powers[i]; bestT = i; }
        }

        double secondP = 0.0;
        for (int i = 0; i < NUM_TONES; i++) {
            if (i != bestT && powers[i] > secondP) secondP = powers[i];
        }

        double peakness = (sumP > 1e-12) ? maxP / sumP * NUM_TONES : 0.0;
        double conf     = (secondP > 1e-12) ? maxP / (maxP + secondP) : 0.5;

        lastSnr  = (sumP > 1e-12) ? 10.0 * Math.log10(maxP / (sumP / NUM_TONES)) : 0.0;
        lastConf = conf;

        boolean good = peakness >= LOCK_RATIO && conf < CONF_GATE;
        if (!good) {
            goodSymCount = 0;
            if (prevTone >= 0) prevTone = bestT;
            return;
        }
        goodSymCount++;

        // IFK+ decode
        if (prevTone < 0) {
            prevTone = bestT;
            return;
        }

        int nibble = (bestT - prevTone - 1 + NUM_TONES) % NUM_TONES;
        prevTone = bestT;

        debug(String.format("TONE %2d → nibble %2d", bestT, nibble));

        if (nibble > MAX_NIBBLE) {
            // Values 16–17: sync / inter-word marker
            // If we were mid-byte, discard the partial byte and emit a space
            if (haveHigh) {
                haveHigh = false;
                pending.append(' ');
            }
            return;
        }

        assembleNibble(nibble);
    }

    // =================================================================
    // Nibble → character assembly
    // =================================================================

    private void assembleNibble(int nibble) {
        if (!haveHigh) {
            highNibble = nibble;
            haveHigh   = true;
        } else {
            int b = (highNibble << 4) | nibble;
            haveHigh = false;
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
        if (goodSymCount < MIN_GOOD && pending.length() < 6) return Optional.empty();

        String text = pending.toString();
        pending.setLength(0);
        if (text.isBlank()) return Optional.empty();

        debug("EMIT '" + text.trim() + "'");
        return Optional.of(new DecodeMessage(
                ModeType.DOMINOEX, text, rigHz,
                toneFreqs[NUM_TONES / 2], lastSnr, lastConf));
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[DominoEX] " + msg);
    }
}
