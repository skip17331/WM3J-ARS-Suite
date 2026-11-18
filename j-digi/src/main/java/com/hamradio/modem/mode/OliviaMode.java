package com.hamradio.modem.mode;

import com.hamradio.modem.dsp.OliviaCodec;
import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Arrays;
import java.util.Optional;

/**
 * Olivia MFSK decoder: standard Olivia, as fldigi and other Olivia programs send it (punch list 3-12).
 *
 * Default: 8 tones, 500 Hz bandwidth, 1500 Hz centre.
 *
 *   Tone spacing   = bandwidth / numTones   (62.5 Hz for 8/500)
 *   Symbol rate    = bandwidth / numTones   (62.5 baud for 8/500)
 *   Samples/symbol = sampleRate / symbolRate  (128 at 8 kHz)
 *   Block          = 64 symbols, one 7-bit ASCII character per bit of the symbol (3 for 8 tones)
 *
 * Block coding is {@link OliviaCodec}: Gray-decoded soft bits per symbol, the diagonal spread undone, descrambled, and a fast
 * Hadamard transform per character. The scrambling also makes the right block alignment stand out, so block sync tries every
 * start and keeps the one whose characters decode most confidently.
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

    // ── Thresholds ────────────────────────────────────────────────────
    private static final double MIN_RMS           = 0.003;
    /** Symbol lock needs a mean peakness of at least this fraction of numTones (a clean, correctly tuned signal scores
     *  about numTones). It used to be a fixed 2.2, below band noise itself (the strongest of 8 random tones averages about
     *  2.7 times the mean), so the decoder could lock on noise or on a signal AFC had not yet tuned to, then stop AFC at
     *  block sync and print garbage for the whole transmission (a signal 250 Hz off-centre). */
    private static final double LOCK_PEAK_FRACTION = 0.5;
    /** Minimum blocks decoded before emitting text. */
    private static final int    MIN_EMIT_BLOCKS   = 1;
    /** Symbols of signal needed before we attempt sync acquisition. */
    private static final int    MIN_LOCK_SYMS     = 8;
    /** Walsh confidence ratio (best / second-best) below which char is marked '?'. */
    private static final double MIN_CONF_RATIO    = 1.15;

    private static final boolean DEBUG            = false;

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
    /** Best timing score of the last acquisition attempt that did not lock yet (-1: none). Lock waits for it to stop rising. */
    private double  candScore    = -1.0;

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
    /** Audio kept while not locked: enough to decode back to the start of a transmission AFC had to find first. */
    private static final double MAX_UNLOCKED_SECONDS = 16.0;
    /** Mean peakness (of numTones) of the symbols just before a lock point for backtracking to take them as signal. */
    private static final double BACKTRACK_PEAK_FRACTION = 0.55;

    // ── Block accumulation ────────────────────────────────────────────
    /** Soft bit values: [symbol index 0..63][bit plane 0..bitsPerSymbol-1]. */
    private double[][] blockSoft;
    private int     blockSymIdx  = 0;
    private int     blocksDecoded = 0;

    // ── Block sync ────────────────────────────────────────────────────
    /** Set once we've found block alignment via exhaustive Walsh-confidence
     *  search over all 64 candidate starting offsets. */
    private boolean blockSynced  = false;
    /** Soft-value buffer used to find block sync: SYNC_BLOCKS blocks for every candidate start, plus one block of slack. */
    private double[][] syncBuf;
    private int     syncBufIdx   = 0;
    /** Blocks scored together for each candidate block start. One was not enough: with no scrambling, a window shifted by
     *  16 or 32 symbols can decode as a clean (wrong) codeword, so 3 in 4 transmissions locked onto the wrong boundary. */
    private static final int SYNC_BLOCKS   = 4;
    private static final int SYNC_BUF_SYMS = BLOCK_SYMBOLS * (SYNC_BLOCKS + 1);

    // ── Output ────────────────────────────────────────────────────────
    private final StringBuilder pending = new StringBuilder();
    /** Text has been emitted since the last block sync: leading spaces are lead-in only before that. */
    private boolean emittedSinceSync = false;
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
            candScore    = -1.0;
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
        // Much wider than an Olivia signal is band noise: peak-held for long enough, noise fills the whole search range
        // evenly (1,000-1,300 Hz "occupied" for a 500 Hz mode), and AFC used to retune to the middle of it before the
        // signal arrived, then lock with every tone off.
        if (occupiedBw > expectedBw * 1.5) return;

        // The edge midpoint is only a rough guess: the edges of the peak-held spectrum are not symmetric about the tones, and
        // it landed half a tone spacing off (1468.8 Hz for a signal at 1500), just outside the deadband, so AFC detuned a
        // centred signal and every tone was measured between two real ones. Refine it by fitting the tone comb: the centre
        // within one tone spacing of the guess where the spectrum is strongest at all numTones tone positions.
        double guess = ((firstAbove + lastAbove) / 2.0) * binHz;
        double newCenter = guess, bestComb = -1.0;
        for (double c = guess - toneSpacing; c <= guess + toneSpacing; c += 1.0) {
            double comb = 0.0;
            for (int t = 0; t < numTones; t++) comb += spectrumAt(c + (t - (numTones - 1) / 2.0) * toneSpacing, binHz);
            if (comb > bestComb) { bestComb = comb; newCenter = c; }
        }
        double jump = Math.abs(newCenter - centerHz);
        if (jump < AFC_DEADBAND_HZ || jump > AFC_MAX_JUMP_HZ) return;

        debug(String.format("AFC %.1f → %.1f Hz (Δ%.1f, bw≈%.0f)",
                centerHz, newCenter, jump, occupiedBw));
        retune(newCenter);
    }

    /** The peak-held spectrum at {@code hz}, interpolated between bins; 0 outside it. */
    private double spectrumAt(double hz, double binHz) {
        double x = hz / binHz;
        int i = (int) Math.floor(x);
        if (i < 0 || i + 1 >= afcSpectrum.length) return 0.0;
        double f = x - i;
        return afcSpectrum[i] * (1 - f) + afcSpectrum[i + 1] * f;
    }

    /** Move the band centre to {@code newCenter} and rebuild the tone table. */
    private void retune(double newCenter) {
        centerHz = newCenter;
        double bw         = numTones * toneSpacing;
        double lowestTone = newCenter - bw / 2.0 + toneSpacing / 2.0;
        for (int i = 0; i < numTones; i++) toneFreqs[i] = lowestTone + i * toneSpacing;
        // Re-acquire timing and block sync at the new tones, but keep the buffered audio: it is raw audio, which the
        // Goertzels measure at whatever toneFreqs now are. Dropping it (as this used to) threw away everything heard
        // before AFC could recognise an off-centre signal, so a short transmission lost its start.
        symLocked   = false;
        blockSynced = false;
        syncBufIdx  = 0;
        symOffset   = 0;
        peakAvg     = 0.0;
        peakSamples = 0;
        badSyms     = 0;
        candScore   = -1.0;
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
        candScore    = -1.0;
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
        // Unlocked, symOffset stays 0, so the compaction below never dropped anything and the buffer doubled whenever it
        // filled: listening to band noise in Olivia for an hour grew it by about 100 MB. Keep the last MAX_UNLOCKED_SECONDS.
        int cap = (int) (sampleRate * MAX_UNLOCKED_SECONDS);
        if (!symLocked && nSamples + samples.length > cap) {
            int drop = nSamples + samples.length - cap;
            drop = Math.min(nSamples, drop - drop % spS);              // whole symbols, so timing found later still fits
            if (drop > 0) {
                System.arraycopy(sampleBuf, drop, sampleBuf, 0, nSamples - drop);
                nSamples -= drop;
            }
        }
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
    /** Mean peakness of the eval window at sample offset {@code off}, and how many distinct strongest tones it saw;
     *  null when no symbol fits. */
    private double[] scoreOffset(int off, int evalStart, int evalSyms) {
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
        return count == 0 ? null : new double[]{score / count, Integer.bitCount(seenMask)};
    }

    /** The earliest symbol start at or before {@code start}, on the same timing, from which every 3-symbol run is signal. */
    private int backtrack(int start) {
        double need = BACKTRACK_PEAK_FRACTION * numTones;
        int s = start;
        while (s - 3 * spS >= 0) {
            double sum = 0.0;
            for (int k = 1; k <= 3; k++) sum += symbolPeakness(s - k * spS);
            if (sum / 3 < need) break;
            s -= spS;
        }
        if (s != start) debug(String.format("BACKTRACK %d symbols", (start - s) / spS));
        return s;
    }

    /** Strongest tone power over the mean (numTones for a clean single tone, about 2 for noise) of the symbol at {@code start}. */
    private double symbolPeakness(int start) {
        double[] p = goertzelAll(start, spS);
        double max = 0.0, sum = 0.0;
        for (double v : p) { sum += v; if (v > max) max = v; }
        return sum > 1e-12 ? max / (sum / numTones) : 0.0;
    }

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

        // Coarse then fine: every COARSE-th offset, then each offset within COARSE of the best. Peakness changes smoothly
        // with the offset (the Goertzel windows overlap the true symbol by spS - |error|), so this finds the same best
        // offset as trying all spS of them, with about a quarter of the work. Trying every offset on every frame while
        // unlocked was the modem's one real CPU cost (3.8% of a core; every other mode is under 0.1%).
        int coarse = Math.max(1, spS / 16);
        for (int off = 0; off < spS; off += coarse) {
            double[] r = scoreOffset(off, evalStart, evalSyms);
            if (r != null && r[0] > bestScore) { bestScore = r[0]; bestOff = off; bestDistinct = (int) r[1]; }
        }
        int centre = bestOff;
        for (int d = -coarse + 1; d < coarse; d++) {
            if (d == 0) continue;
            int off = Math.floorMod(centre + d, spS);
            double[] r = scoreOffset(off, evalStart, evalSyms);
            if (r != null && r[0] > bestScore) { bestScore = r[0]; bestOff = off; bestDistinct = (int) r[1]; }
        }

        // Reject signals with too few distinct tones — single-tone lead-in
        // gives 1, off-centre signals (where Goertzels barely overlap the
        // real tones) typically give 2-3, while correctly-tuned content
        // gives 4+. Requiring ≥ 4 prevents committing to wrong frequencies.
        if (bestDistinct < 4) return;

        if (bestScore < LOCK_PEAK_FRACTION * numTones) { candScore = -1.0; return; }
        // Wait for the score to stop rising. While the signal is still arriving, the eval window is part noise: its best
        // offset is a poor estimate (scores of 4.8 and 7.1 against 8.0 clean) and, once locked, nothing corrected it, so a
        // signal that began while the decoder was hearing noise was decoded with the timing off. Each new frame of signal
        // raises the score; once it no longer does, the window is all signal and its offset is the right one.
        if (bestScore > candScore + 0.05) { candScore = bestScore; return; }
        candScore = -1.0;
        {
            symLocked    = true;
            blockSynced  = false;
            syncBufIdx   = 0;
            // Start at the eval window, then step back over earlier symbols that are clearly this signal (a strong single
            // tone each): an off-centre signal is only found after AFC has heard much of it, and its start is still in the
            // buffer. Noise before the signal fails the test, so it is not decoded into garbage.
            symOffset    = backtrack(evalStart + bestOff);
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
            double[] softs  = OliviaCodec.softBits(powers, bitsPerSymbol);

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
                    candScore   = -1.0;
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
        double bestConf = Double.NEGATIVE_INFINITY;
        for (int off = 0; off < BLOCK_SYMBOLS; off++) {
            double conf = 0.0;                                   // log-confidence summed over SYNC_BLOCKS consecutive blocks
            for (int j = 0; j < SYNC_BLOCKS; j++) conf += Math.log(Math.max(1e-9, trialWalshConfidence(syncBuf, off + j * BLOCK_SYMBOLS)));
            if (conf > bestConf) { bestConf = conf; bestOff = off; }
        }
        debug(String.format("BLOCK SYNC off=%d logconf=%.2f", bestOff, bestConf));

        // Emit the blocks the search covered (lead-in spaces among them are dropped on emit).
        for (int j = 0; j < SYNC_BLOCKS; j++) {
            decodeBlock(syncBuf, bestOff + j * BLOCK_SYMBOLS, true);
            blocksDecoded++;
        }

        // The remainder of syncBuf (after the decoded blocks) becomes the
        // start of the next active block.
        int consumed = bestOff + SYNC_BLOCKS * BLOCK_SYMBOLS;
        int carry    = SYNC_BUF_SYMS - consumed;
        for (int k = 0; k < carry; k++) {
            for (int b = 0; b < bitsPerSymbol; b++) {
                blockSoft[k][b] = syncBuf[consumed + k][b];
            }
        }
        blockSymIdx = carry;
        if (blockSymIdx == BLOCK_SYMBOLS) {                       // sync at offset 0 leaves one whole block over: decode it now
            decodeBlock(blockSoft, 0, true);
            blocksDecoded++;
            blockSymIdx = 0;
        }
        syncBufIdx  = 0;
        blockSynced = true;
        emittedSinceSync = false;
    }

    /** Trial decode at {@code startSym}: average Walsh peak / runner-up
     *  ratio across all bit planes. Higher = cleaner alignment. */
    private double trialWalshConfidence(double[][] soft, int startSym) {
        double avg = 0.0;
        for (int b = 0; b < bitsPerSymbol; b++) avg += OliviaCodec.decodeChar(soft, startSym, b).ratio();
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
            OliviaCodec.Decoded d = OliviaCodec.decodeChar(soft, startSym, b);
            blockConf += Math.min(d.ratio() / 10.0, 1.0);
            debug(String.format("CHAR %d ratio=%.2f plane=%d", d.ch(), d.ratio(), b));
            if (appendToPending && d.ratio() >= MIN_CONF_RATIO) pending.append((char) d.ch());
        }
        lastConf = Math.min(0.99, blockConf / bitsPerSymbol);
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

        // Each emit is one 3-character block or a few. Trimming every one removed real word spaces whenever they fell at a
        // block edge ("CQ CQ DE" came out as "CQCQDE"). Only the lead-in (spaces before the first text after sync) and
        // all-space blocks (lead-in / lead-out) are dropped now.
        String text = shown(pending);
        pending.setLength(0);
        if (text.isBlank()) return Optional.empty();
        if (!emittedSinceSync) text = text.stripLeading();
        emittedSinceSync = true;

        debug("EMIT '" + text + "'");
        return Optional.of(new DecodeMessage(
                ModeType.OLIVIA, text, rigHz, centerHz, lastSnr, lastConf));
    }

    /** What a person sees: printable ASCII, tabs and line breaks (CR LF as one); NUL (the idle fill) and other control codes dropped. */
    static String shown(CharSequence raw) {
        StringBuilder b = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= 0x20 && c < 0x7F || c == '\n' || c == '\t') b.append(c);
        }
        return b.toString();
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[OLIVIA] " + msg);
    }
}
