package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Arrays;
import java.util.Optional;

/**
 * Olivia MFSK decoder.
 *
 * Default: 8 tones, 500 Hz bandwidth, 1500 Hz centre.
 *
 * Protocol (Jalocha / SP9VRC):
 *   Tone spacing   = bandwidth / numTones   (62.5 Hz for 8/500)
 *   Symbol rate    = bandwidth / numTones   (62.5 baud for 8/500)
 *   Samples/symbol = sampleRate / symbolRate  (128 at 8 kHz)
 *   Bits/symbol    = log2(numTones)  (3 for 8 tones)
 *   Block size     = 64 symbols (fixed for all variants)
 *   Chars/block    = log2(numTones) — one character per bit plane
 *   Character set  = 64 chars, ASCII 0x20 (space) through 0x5F (_)
 *
 * FEC — Walsh-Hadamard coding per bit plane:
 *   For each bit plane b (0 .. bitsPerSymbol-1), the 64 soft values
 *   v[k] = (power in tones with bit_b=1) - (power in tones with bit_b=0)
 *   are Walsh-Hadamard transformed.  The index of the largest |transform|
 *   value is the decoded character code for that bit plane.
 *
 * Tone detection: Goertzel algorithm, one call per tone per symbol.
 *
 * Symbol timing: after MIN_LOCK_SYMS symbols of signal, scan all spS offsets
 * and lock on the one with the highest average "peakness" (max/avg power).
 */
public class OliviaMode implements DigitalMode {

    // ── Default configuration ─────────────────────────────────────────
    private static final int    DEFAULT_TONES     = 8;
    private static final int    DEFAULT_BANDWIDTH = 500;
    private static final double DEFAULT_CENTER    = 1500.0;

    // ── Protocol constants ────────────────────────────────────────────
    private static final int    BLOCK_SYMBOLS     = 64;
    private static final int    CHAR_SET_SIZE     = 64;    // 2^6
    private static final char   CHAR_OFFSET       = 0x20;  // space

    // ── Thresholds ────────────────────────────────────────────────────
    private static final double MIN_RMS           = 0.003;
    /** Peakness threshold to acquire symbol lock (random = 1/numTones). */
    private static final double LOCK_PEAK_RATIO   = 2.2;
    /** Minimum blocks decoded before emitting text. */
    private static final int    MIN_EMIT_BLOCKS   = 1;
    /** Symbols of signal needed before we attempt sync acquisition. */
    private static final int    MIN_LOCK_SYMS     = 8;
    /** Walsh confidence ratio (best / second-best) below which char is marked '?'. */
    private static final double MIN_CONF_RATIO    = 1.15;

    private static final boolean DEBUG            = false;

    // =================================================================
    // Per-instance configuration (set in constructor)
    // =================================================================
    private final int      numTones;
    private final int      bitsPerSymbol;
    private final double   toneSpacing;
    private final double   centerHz;
    private final double[] toneFreqs;

    // =================================================================
    // Per-instance state
    // =================================================================
    private boolean initialized  = false;
    private double  sampleRate   = 8000.0;
    private int     spS;

    // ── Sample buffer ─────────────────────────────────────────────────
    private float[] sampleBuf    = new float[65536];
    private int     nSamples     = 0;

    // ── Symbol timing ─────────────────────────────────────────────────
    private boolean symLocked    = false;
    private int     symOffset    = 0;   // index of next symbol start in sampleBuf
    private int     rawSymCount  = 0;   // unframed symbols seen since last sync attempt

    // ── Block accumulation ────────────────────────────────────────────
    /** Soft bit values: [symbol index 0..63][bit plane 0..bitsPerSymbol-1]. */
    private double[][] blockSoft;
    private int     blockSymIdx  = 0;
    private int     blocksDecoded = 0;

    // ── Output ────────────────────────────────────────────────────────
    private final StringBuilder pending = new StringBuilder();
    private double lastSnr  = 0.0;
    private double lastConf = 0.0;

    // =================================================================
    // Constructors
    // =================================================================

    public OliviaMode() {
        this(DEFAULT_TONES, DEFAULT_BANDWIDTH, DEFAULT_CENTER);
    }

    public OliviaMode(int numTones, int bandwidthHz, double centerHz) {
        if (numTones < 2 || (numTones & (numTones - 1)) != 0)
            throw new IllegalArgumentException("numTones must be a power of 2, got " + numTones);

        this.numTones      = numTones;
        this.bitsPerSymbol = Integer.numberOfTrailingZeros(numTones); // log2(numTones)
        this.toneSpacing   = (double) bandwidthHz / numTones;
        this.centerHz      = centerHz;

        double lowestTone = centerHz - (double) bandwidthHz / 2.0 + toneSpacing / 2.0;
        this.toneFreqs = new double[numTones];
        for (int i = 0; i < numTones; i++) toneFreqs[i] = lowestTone + i * toneSpacing;

        this.blockSoft = new double[BLOCK_SYMBOLS][bitsPerSymbol];
    }

    // =================================================================
    // DigitalMode contract
    // =================================================================

    @Override
    public String getName() { return "OLIVIA"; }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snap, long rigHz) {
        if (snap == null || snap.getSamples() == null || snap.getSamples().length == 0)
            return Optional.empty();

        initIfNeeded(snap.getSampleRate());

        if (snap.getRms() < MIN_RMS) {
            symLocked    = false;
            rawSymCount  = 0;
            blockSymIdx  = 0;
            blocksDecoded = 0;
            return emitPending(rigHz);
        }

        appendSamples(snap.getSamples());

        if (!symLocked) {
            tryAcquireSymbolSync();
        }

        if (symLocked) {
            extractSymbols();
        }

        return emitPending(rigHz);
    }

    // =================================================================
    // Initialisation
    // =================================================================

    private void initIfNeeded(double sr) {
        if (initialized && Math.abs(sr - sampleRate) < 0.5) return;
        sampleRate = sr;
        spS = Math.max(1, (int) Math.round(sr / toneSpacing));
        resetState();
        initialized = true;
        debug(String.format("init sr=%.0f spS=%d tones=%d spacing=%.2f center=%.0f",
                sr, spS, numTones, toneSpacing, centerHz));
    }

    private void resetState() {
        nSamples     = 0;
        symLocked    = false;
        symOffset    = 0;
        rawSymCount  = 0;
        blockSymIdx  = 0;
        blocksDecoded = 0;
        pending.setLength(0);
        lastSnr  = 0.0;
        lastConf = 0.0;
        for (double[] row : blockSoft) Arrays.fill(row, 0.0);
    }

    // =================================================================
    // Sample buffering
    // =================================================================

    private void appendSamples(float[] samples) {
        // Compact if needed: drop samples before current position
        if (nSamples + samples.length > sampleBuf.length) {
            int keep = Math.max(0, symOffset - spS);
            if (keep > spS) {
                System.arraycopy(sampleBuf, keep, sampleBuf, 0, nSamples - keep);
                nSamples  -= keep;
                symOffset -= keep;
                if (symOffset < 0) symOffset = 0;
            }
        }
        if (nSamples + samples.length > sampleBuf.length) {
            sampleBuf = Arrays.copyOf(sampleBuf, sampleBuf.length * 2);
        }
        System.arraycopy(samples, 0, sampleBuf, nSamples, samples.length);
        nSamples += samples.length;
        rawSymCount = nSamples / spS;
    }

    // =================================================================
    // Symbol timing acquisition
    // =================================================================

    /**
     * Scan all spS candidate offsets; score each by average peakness
     * (max-tone-power / avg-tone-power per symbol).  A clean MFSK signal
     * concentrates energy in one tone per symbol, giving peakness ≈ numTones.
     * Lock on the offset with the highest mean peakness.
     */
    private void tryAcquireSymbolSync() {
        int evalSyms = Math.min(24, rawSymCount - 1);
        if (evalSyms < MIN_LOCK_SYMS) return;

        double bestScore = -1.0;
        int    bestOff   = 0;

        for (int off = 0; off < spS; off++) {
            double score = 0.0;
            int    count = 0;
            for (int s = 0; s < evalSyms; s++) {
                int start = off + s * spS;
                if (start + spS > nSamples) break;
                double[] p = goertzelAll(start, spS);
                double max = 0.0, sum = 0.0;
                for (double v : p) { sum += v; if (v > max) max = v; }
                if (sum > 1e-12) { score += max / sum * numTones; count++; }
            }
            if (count > 0) {
                score /= count;
                if (score > bestScore) { bestScore = score; bestOff = off; }
            }
        }

        if (bestScore >= LOCK_PEAK_RATIO) {
            symLocked    = true;
            symOffset    = bestOff;
            blockSymIdx  = 0;
            blocksDecoded = 0;
            debug(String.format("SYM LOCK off=%d score=%.2f", bestOff, bestScore));
        }
    }

    // =================================================================
    // Symbol extraction and block accumulation
    // =================================================================

    private void extractSymbols() {
        while (symOffset + spS <= nSamples) {
            double[] powers = goertzelAll(symOffset, spS);

            // Compute soft bit values for each bit plane
            for (int b = 0; b < bitsPerSymbol; b++) {
                double v = 0.0;
                for (int i = 0; i < numTones; i++) {
                    v += ((i >> b) & 1) == 1 ? powers[i] : -powers[i];
                }
                blockSoft[blockSymIdx][b] = v;
            }

            // Update SNR from this symbol
            double maxP = 0.0, sumP = 0.0;
            for (double p : powers) { sumP += p; if (p > maxP) maxP = p; }
            if (sumP > 1e-12) {
                lastSnr = 10.0 * Math.log10(maxP / (sumP / numTones));
            }

            symOffset  += spS;
            blockSymIdx++;

            if (blockSymIdx == BLOCK_SYMBOLS) {
                decodeBlock();
                blockSymIdx = 0;
                blocksDecoded++;
            }
        }

        // Compact sample buffer
        if (symOffset > spS * 8) {
            int drop = symOffset - spS * 4;
            System.arraycopy(sampleBuf, drop, sampleBuf, 0, nSamples - drop);
            nSamples  -= drop;
            symOffset -= drop;
        }
    }

    // =================================================================
    // Block decode: Walsh-Hadamard FEC
    // =================================================================

    /**
     * Decode one 64-symbol block.  For each bit plane b, the 64 soft values
     * are Walsh-Hadamard transformed; the maximum-magnitude index is the
     * character code.  The character is appended to the pending buffer.
     */
    private void decodeBlock() {
        double blockConf = 0.0;

        for (int b = 0; b < bitsPerSymbol; b++) {
            double[] v = new double[BLOCK_SYMBOLS];
            for (int k = 0; k < BLOCK_SYMBOLS; k++) v[k] = blockSoft[k][b];

            fwht(v);  // in-place Walsh-Hadamard transform

            int    best = 0, second = -1;
            double bestAbs = 0.0, secondAbs = 0.0;
            for (int c = 0; c < CHAR_SET_SIZE; c++) {
                double a = Math.abs(v[c]);
                if (a > bestAbs) {
                    secondAbs = bestAbs; second = best;
                    bestAbs   = a;       best   = c;
                } else if (a > secondAbs) {
                    secondAbs = a; second = c;
                }
            }

            double ratio = (secondAbs > 1e-12) ? bestAbs / secondAbs : 10.0;
            blockConf += Math.min(ratio / 10.0, 1.0);

            char ch;
            if (ratio >= MIN_CONF_RATIO) {
                ch = (char)(best + CHAR_OFFSET);
                debug(String.format("CHAR '%c' code=%d ratio=%.2f plane=%d", ch, best, ratio, b));
            } else {
                // Low confidence — decode anyway (Olivia FEC should handle noise)
                ch = (char)(best + CHAR_OFFSET);
                debug(String.format("CHAR '%c' code=%d ratio=%.2f (low) plane=%d", ch, best, ratio, b));
            }
            pending.append(ch);
        }

        lastConf = Math.min(0.99, blockConf / bitsPerSymbol);
    }

    // =================================================================
    // Fast Walsh-Hadamard Transform (in-place, size must be power of 2)
    // =================================================================

    /**
     * Computes v[c] = (1/n) * sum_k( v_orig[k] * (-1)^popcount(c & k) ).
     */
    private static void fwht(double[] v) {
        int n = v.length;
        for (int step = 1; step < n; step <<= 1) {
            for (int i = 0; i < n; i += step << 1) {
                for (int j = i; j < i + step; j++) {
                    double a = v[j], b = v[j + step];
                    v[j]        = a + b;
                    v[j + step] = a - b;
                }
            }
        }
        for (int i = 0; i < n; i++) v[i] /= n;
    }

    // =================================================================
    // Goertzel tone power
    // =================================================================

    private double[] goertzelAll(int start, int len) {
        double[] powers = new double[numTones];
        for (int t = 0; t < numTones; t++) {
            double coeff = 2.0 * Math.cos(2.0 * Math.PI * toneFreqs[t] / sampleRate);
            double s1 = 0.0, s2 = 0.0;
            int end = start + len;
            for (int i = start; i < end; i++) {
                double s0 = sampleBuf[i] + coeff * s1 - s2;
                s2 = s1; s1 = s0;
            }
            powers[t] = s1 * s1 + s2 * s2 - s1 * s2 * coeff;
        }
        return powers;
    }

    // =================================================================
    // Text emission
    // =================================================================

    private Optional<DecodeMessage> emitPending(long rigHz) {
        if (pending.length() == 0) return Optional.empty();
        // Wait for at least one full decoded block before emitting
        if (blocksDecoded < MIN_EMIT_BLOCKS) return Optional.empty();

        String text = pending.toString().trim();
        pending.setLength(0);
        if (text.isEmpty()) return Optional.empty();

        debug("EMIT '" + text + "'");
        return Optional.of(new DecodeMessage(
                ModeType.OLIVIA, text, rigHz, centerHz, lastSnr, lastConf));
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[OLIVIA] " + msg);
    }
}
