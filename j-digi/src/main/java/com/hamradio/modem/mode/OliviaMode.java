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

    private static final boolean DEBUG            = true;

    // ── AFC ───────────────────────────────────────────────────────────
    /** How far the centroid may sit from current centerHz before we'd retune.
     *  Smaller than tone spacing so jitter doesn't trigger constant retunes. */
    private static final double AFC_DEADBAND_HZ   = 25.0;
    /** How far AFC is allowed to jump in one update. */
    private static final double AFC_MAX_JUMP_HZ   = 400.0;

    // ── Symbol re-lock ────────────────────────────────────────────────
    /** Rolling-mean peakness threshold below which we force re-acquisition.
     *  Lead-in (all tone 0) gives ~numTones; content with varying tones gives
     *  more like numTones/2 if alignment is right, less if it's wrong. */
    private static final double RELOCK_PEAK_FLOOR = 1.8;
    /** Symbols of low peakness needed to trigger re-lock. */
    private static final int    RELOCK_BAD_SYMS   = 16;

    // =================================================================
    // Per-instance configuration (set in constructor)
    // =================================================================
    private final int      numTones;
    private final int      bitsPerSymbol;
    private final double   toneSpacing;
    /** AFC-adjusted band centre. Updated at runtime when unlocked. */
    private double   centerHz;
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

    // ── Re-lock tracking ──────────────────────────────────────────────
    private double  peakAvg      = 0.0;  // EMA of per-symbol peakness
    private int     peakSamples  = 0;
    private int     badSyms      = 0;

    // ── AFC running spectrum ──────────────────────────────────────────
    /** Peak-held FFT magnitude across recent frames. A single Olivia
     *  frame only contains ~8 symbols, so individual frames miss most
     *  tones; integrating over a few seconds reveals the full band. */
    private double[] afcSpectrum;
    private int      afcFrames = 0;
    /** Minimum frames of spectrum integration before lock attempts are
     *  allowed. Gives AFC time to retune off-centre signals before the
     *  symbol-sync scan would commit to wrong tone frequencies. */
    private static final int AFC_SETTLE_FRAMES = 8;

    // ── Block accumulation ────────────────────────────────────────────
    /** Soft bit values: [symbol index 0..63][bit plane 0..bitsPerSymbol-1]. */
    private double[][] blockSoft;
    private int     blockSymIdx  = 0;
    private int     blocksDecoded = 0;

    // ── Block sync ────────────────────────────────────────────────────
    /** Set once we've found block alignment via exhaustive Walsh-confidence
     *  search over all 64 candidate starting offsets. */
    private boolean blockSynced  = false;
    /** Rolling 128-symbol soft-value buffer used to find block sync. */
    private double[][] syncBuf;
    private int     syncBufIdx   = 0;
    private static final int SYNC_BUF_SYMS = BLOCK_SYMBOLS * 2;

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
        this.syncBuf   = new double[SYNC_BUF_SYMS][bitsPerSymbol];
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
            blockSynced  = false;
            syncBufIdx   = 0;
            rawSymCount  = 0;
            blockSymIdx  = 0;
            blocksDecoded = 0;
            // Drop the integrated AFC spectrum so a fresh QSO at a
            // different audio offset doesn't get pulled toward the
            // previous QSO's tones, and snap the centre back to the
            // default so AFC starts each transmission from scratch.
            if (afcSpectrum != null) Arrays.fill(afcSpectrum, 0.0);
            afcFrames    = 0;
            if (Math.abs(centerHz - DEFAULT_CENTER) > 1.0) {
                retune(DEFAULT_CENTER);
            }
            return emitPending(rigHz);
        }

        // AFC: snap centerHz to the midpoint of detected band occupancy.
        // Fires only when ≥75 % of the expected Olivia bandwidth is
        // occupied, so single-tone lead-in can't drag the centre off.
        // Allowed any time before block sync so content arriving after a
        // single-tone lead-in still gets a chance to retune.
        if (!blockSynced) {
            afcUpdate(snap);
        }

        appendSamples(snap.getSamples());

        // Don't attempt symbol-sync until AFC has had a chance to retune
        // an off-centre signal — locking with wrong tones would commit us
        // to spurious symbol boundaries that AFC can't undo cheaply.
        if (!symLocked && afcFrames >= AFC_SETTLE_FRAMES) {
            tryAcquireSymbolSync();
        }

        if (symLocked) {
            extractSymbols();
        }

        return emitPending(rigHz);
    }

    // =================================================================
    // AFC — centroid-based band centre tracking
    // =================================================================

    /**
     * Estimate the Olivia band centre from the FFT magnitude spectrum.
     *
     * Strategy: find the lowest and highest bins above 25 % of the in-band
     * peak. If the occupied bandwidth is at least half the expected
     * numTones × toneSpacing, treat this as a real Olivia signal and use
     * (low_edge + high_edge) / 2 as the band centre. A single-tone lead-in
     * (the all-space preamble emitted by j-digi's TX) occupies only one
     * bin's worth and is skipped — otherwise AFC would chase that single
     * tone and detune the band by a full half-bandwidth.
     */
    private void afcUpdate(SignalSnapshot snap) {
        double[] mag = snap.getMagnitudes();
        if (mag == null || mag.length < 2) return;

        // Maintain a peak-held running spectrum. Each frame only contains
        // ~8 symbols out of 64, so any single frame misses most tones —
        // integrating across many frames reveals the full Olivia band.
        if (afcSpectrum == null || afcSpectrum.length != mag.length) {
            afcSpectrum = new double[mag.length];
            afcFrames   = 0;
        }
        // Pure peak-hold (no decay) so all 8 Olivia tones accumulate
        // their peak power within the first few content blocks, even
        // though each FFT frame only contains ~8 symbols and misses most
        // tones. Reset on silence keeps this from staling across QSOs.
        for (int i = 0; i < mag.length; i++) {
            if (mag[i] > afcSpectrum[i]) afcSpectrum[i] = mag[i];
        }
        afcFrames++;
        // Need a few frames to build up enough peak-held magnitude to
        // distinguish real signal from FFT noise floor, but no need to
        // wait for the full integration — the bandwidth check gates AFC.
        if (afcFrames < 4) return;

        double binHz   = snap.getSampleRate() / (2.0 * mag.length);
        double expectedBw = numTones * toneSpacing;
        // Search far enough to fully capture a signal that's MAX_JUMP_HZ
        // off-centre — the signal occupies ±expectedBw/2, so we need to
        // see from centre − (MAX_JUMP + Bw/2) to centre + (MAX_JUMP + Bw/2).
        double searchLow  = centerHz - (AFC_MAX_JUMP_HZ + expectedBw / 2.0);
        double searchHigh = centerHz + (AFC_MAX_JUMP_HZ + expectedBw / 2.0);
        int loBin = Math.max(1,            (int) Math.floor(searchLow  / binHz));
        int hiBin = Math.min(mag.length-1, (int) Math.ceil (searchHigh / binHz));

        double peakP = 0.0;
        for (int i = loBin; i <= hiBin; i++) peakP = Math.max(peakP, afcSpectrum[i]);
        if (peakP <= 0.0) return;
        double thresh = peakP * 0.25;
        int firstAbove = -1, lastAbove = -1;
        for (int i = loBin; i <= hiBin; i++) {
            if (afcSpectrum[i] >= thresh) {
                if (firstAbove < 0) firstAbove = i;
                lastAbove = i;
            }
        }
        if (firstAbove < 0) return;

        double occupiedBw = (lastAbove - firstAbove + 1) * binHz;
        // 75 % bandwidth ensures the spectrum is showing nearly the full
        // Olivia signal so the edge midpoint is an accurate centre estimate.
        if (occupiedBw < expectedBw * 0.75) return;

        double newCenter = ((firstAbove + lastAbove) / 2.0) * binHz;
        double jump = Math.abs(newCenter - centerHz);
        if (jump < AFC_DEADBAND_HZ || jump > AFC_MAX_JUMP_HZ) return;

        debug(String.format("AFC %.1f → %.1f Hz (Δ%.1f, bw≈%.0f)",
                centerHz, newCenter, jump, occupiedBw));
        retune(newCenter);
    }

    /** Move the band centre to {@code newCenter} and rebuild the tone table. */
    private void retune(double newCenter) {
        centerHz = newCenter;
        double bw         = numTones * toneSpacing;
        double lowestTone = newCenter - bw / 2.0 + toneSpacing / 2.0;
        for (int i = 0; i < numTones; i++) toneFreqs[i] = lowestTone + i * toneSpacing;
        // After a retune the buffered baseband is at the wrong frequency
        // for the Goertzels, so drop it and re-acquire from scratch.
        nSamples    = 0;
        symLocked   = false;
        blockSynced = false;
        syncBufIdx  = 0;
        symOffset   = 0;
        peakAvg     = 0.0;
        peakSamples = 0;
        badSyms     = 0;
        // Don't reset afcSpectrum — keep integrated FFT data so we can
        // confirm the new centre fits the signal (no double-AFC oscillation
        // since AFC won't fire again unless the centroid jumps > deadband).
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
        blockSynced  = false;
        syncBufIdx   = 0;
        peakAvg      = 0.0;
        peakSamples  = 0;
        badSyms      = 0;
        afcSpectrum  = null;
        afcFrames    = 0;
        pending.setLength(0);
        lastSnr  = 0.0;
        lastConf = 0.0;
        for (double[] row : blockSoft) Arrays.fill(row, 0.0);
        for (double[] row : syncBuf)   Arrays.fill(row, 0.0);
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
        // Track distinct max-tone indices across the best offset's eval
        // window. A single-tone lead-in (all symbols → tone 0) gives only
        // one distinct max — locking there picks an arbitrary offset that
        // misaligns content, so we require ≥ 2 distinct tones before lock.
        int    bestDistinct = 0;

        // Anchor the eval window to the END of the buffer so the lead-in
        // (tone 0 only) doesn't get used — we want recent symbols where
        // content has started varying tones.
        int evalEnd = nSamples;
        int evalStart = evalEnd - evalSyms * spS;
        if (evalStart < 0) evalStart = 0;

        for (int off = 0; off < spS; off++) {
            double score = 0.0;
            int    count = 0;
            int    seenMask = 0;
            for (int s = 0; s < evalSyms; s++) {
                int start = evalStart + off + s * spS;
                if (start + spS > nSamples) break;
                double[] p = goertzelAll(start, spS);
                double max = 0.0, sum = 0.0;
                int    maxIdx = 0;
                for (int t = 0; t < numTones; t++) {
                    if (p[t] > max) { max = p[t]; maxIdx = t; }
                    sum += p[t];
                }
                if (sum > 1e-12) {
                    score += max / sum * numTones;
                    count++;
                    seenMask |= (1 << maxIdx);
                }
            }
            if (count > 0) {
                score /= count;
                if (score > bestScore) {
                    bestScore = score;
                    bestOff   = off;
                    bestDistinct = Integer.bitCount(seenMask);
                }
            }
        }

        // Reject signals with too few distinct tones — single-tone lead-in
        // gives 1, off-centre signals (where Goertzels barely overlap the
        // real tones) typically give 2-3, while correctly-tuned content
        // gives 4+. Requiring ≥ 4 prevents committing to wrong frequencies.
        if (bestDistinct < 4) return;

        if (bestScore >= LOCK_PEAK_RATIO) {
            symLocked    = true;
            blockSynced  = false;
            syncBufIdx   = 0;
            // Start processing at the eval window, not the buffer start —
            // the buffer start is lead-in, the eval window is content.
            symOffset    = evalStart + bestOff;
            blockSymIdx  = 0;
            blocksDecoded = 0;
            peakAvg      = 0.0;
            peakSamples  = 0;
            badSyms      = 0;
            debug(String.format("SYM LOCK off=%d (abs=%d) score=%.2f distinct=%d",
                    bestOff, symOffset, bestScore, bestDistinct));
        }
    }

    // =================================================================
    // Symbol extraction and block accumulation
    // =================================================================

    private void extractSymbols() {
        while (symOffset + spS <= nSamples) {
            double[] powers = goertzelAll(symOffset, spS);
            double[] softs  = new double[bitsPerSymbol];
            for (int b = 0; b < bitsPerSymbol; b++) {
                double v = 0.0;
                for (int i = 0; i < numTones; i++) {
                    v += ((i >> b) & 1) == 1 ? powers[i] : -powers[i];
                }
                softs[b] = v;
            }

            // Update SNR + per-symbol peakness
            double maxP = 0.0, sumP = 0.0;
            for (double p : powers) { sumP += p; if (p > maxP) maxP = p; }
            if (sumP > 1e-12) {
                lastSnr = 10.0 * Math.log10(maxP / (sumP / numTones));
            }
            double peakness = (sumP > 1e-12) ? (maxP / (sumP / numTones)) : 0.0;
            peakAvg = (peakSamples < 8) ? peakness
                                        : (peakAvg * 0.85 + peakness * 0.15);
            peakSamples++;
            if (peakSamples > 8 && peakAvg < RELOCK_PEAK_FLOOR) {
                if (++badSyms >= RELOCK_BAD_SYMS) {
                    debug(String.format("RELOCK — peakAvg %.2f < %.2f for %d syms",
                            peakAvg, RELOCK_PEAK_FLOOR, badSyms));
                    symLocked   = false;
                    blockSynced = false;
                    syncBufIdx  = 0;
                    blockSymIdx = 0;
                    rawSymCount = nSamples / spS;
                    peakAvg     = 0.0;
                    peakSamples = 0;
                    badSyms     = 0;
                    return;
                }
            } else {
                badSyms = 0;
            }

            symOffset += spS;

            if (!blockSynced) {
                // Buffer soft values until we have 2 blocks worth, then run
                // exhaustive Walsh-confidence search to find block alignment.
                for (int b = 0; b < bitsPerSymbol; b++) syncBuf[syncBufIdx][b] = softs[b];
                syncBufIdx++;
                if (syncBufIdx == SYNC_BUF_SYMS) {
                    finishBlockSync();
                }
            } else {
                for (int b = 0; b < bitsPerSymbol; b++) blockSoft[blockSymIdx][b] = softs[b];
                blockSymIdx++;
                if (blockSymIdx == BLOCK_SYMBOLS) {
                    decodeBlock(blockSoft, 0, true);
                    blockSymIdx = 0;
                    blocksDecoded++;
                }
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

    /**
     * Scan the 128-symbol sync buffer for the block offset (0..63) whose
     * trial Walsh decode gives the highest average peak / runner-up ratio.
     * Decode the first full block at that offset and copy the remainder
     * into the active block accumulator.
     */
    private void finishBlockSync() {
        int    bestOff  = 0;
        double bestConf = -1.0;
        for (int off = 0; off < BLOCK_SYMBOLS; off++) {
            double conf = trialWalshConfidence(syncBuf, off);
            if (conf > bestConf) { bestConf = conf; bestOff = off; }
        }
        debug(String.format("BLOCK SYNC off=%d conf=%.2f", bestOff, bestConf));

        // Emit the first full content block (suppressed if it's still
        // lead-in spaces — decodeBlock appends to pending which gets
        // trimmed on emit anyway).
        decodeBlock(syncBuf, bestOff, true);
        blocksDecoded++;

        // The remainder of syncBuf (after the decoded block) becomes the
        // start of the next active block.
        int consumed = bestOff + BLOCK_SYMBOLS;
        int carry    = SYNC_BUF_SYMS - consumed;
        for (int k = 0; k < carry; k++) {
            for (int b = 0; b < bitsPerSymbol; b++) {
                blockSoft[k][b] = syncBuf[consumed + k][b];
            }
        }
        blockSymIdx = carry;
        syncBufIdx  = 0;
        blockSynced = true;
    }

    /** Trial decode at {@code startSym}: average Walsh peak / runner-up
     *  ratio across all bit planes. Higher = cleaner alignment. */
    private double trialWalshConfidence(double[][] soft, int startSym) {
        double avg = 0.0;
        for (int b = 0; b < bitsPerSymbol; b++) {
            double[] v = new double[BLOCK_SYMBOLS];
            for (int k = 0; k < BLOCK_SYMBOLS; k++) v[k] = soft[startSym + k][b];
            fwht(v);
            double best = 0.0, second = 0.0;
            for (int c = 0; c < CHAR_SET_SIZE; c++) {
                double a = Math.abs(v[c]);
                if (a > best)        { second = best; best = a; }
                else if (a > second) { second = a; }
            }
            avg += (second > 1e-12) ? (best / second) : 10.0;
        }
        return avg / bitsPerSymbol;
    }

    // =================================================================
    // Block decode: Walsh-Hadamard FEC
    // =================================================================

    /**
     * Decode one 64-symbol block.  For each bit plane b, the 64 soft values
     * are Walsh-Hadamard transformed; the maximum-magnitude index is the
     * character code.  The character is appended to the pending buffer.
     */
    private void decodeBlock(double[][] soft, int startSym, boolean appendToPending) {
        double blockConf = 0.0;

        for (int b = 0; b < bitsPerSymbol; b++) {
            double[] v = new double[BLOCK_SYMBOLS];
            for (int k = 0; k < BLOCK_SYMBOLS; k++) v[k] = soft[startSym + k][b];

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
            if (appendToPending) pending.append(ch);
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
