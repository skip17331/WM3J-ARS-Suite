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
 * AFC: a peak-hold spectrum is built over a wide search band (~1200..1800 Hz).
 * Once enough energy has accumulated and occupies ~75% of the expected 250 Hz
 * MFSK16 bandwidth, the centroid drives a retune that rebuilds toneFreqs and
 * forces re-acquisition.
 */
public class Mfsk16Mode implements DigitalMode {

    // ── Protocol ──────────────────────────────────────────────────────
    private static final int    NUM_TONES      = 16;
    /** Bin 89 at 8 kHz: 89 × 8000/512 = 1390.625 Hz — lowest tone. */
    private static final int    FIRST_BIN      = 89;
    /** Exact tone spacing / symbol rate (Hz). */
    private static final double SPACING        = 8000.0 / 512;   // 15.625 Hz
    /** Default centre frequency (Hz). This is the midpoint of bins 89..104
     *  (the encoder's default tone grid): (89 + 104) / 2 × spacing
     *  = 96.5 × 15.625 = 1507.8125 Hz. Nominally referred to as "1500 Hz". */
    private static final double DEFAULT_CENTRE = (FIRST_BIN + 7.5) * SPACING;  // 1507.8125 Hz
    /** Full MFSK16 bandwidth (16 × 15.625 Hz). */
    private static final double MFSK16_BW      = NUM_TONES * SPACING;  // 250 Hz

    // ── Thresholds ────────────────────────────────────────────────────
    private static final double MIN_RMS        = 0.003;
    /** Peakness needed to acquire symbol timing. Legitimate locks score 12–16;
     *  Goertzel leakage from a wrong-centre signal scores 3–4, so 6.0 gives a
     *  clean separation. */
    private static final double LOCK_ACQUIRE   = 6.0;
    /** Per-symbol peakness needed to trust an individual decoded nibble. */
    private static final double LOCK_RATIO     = 2.5;
    /** Fraction of max-power tone that second-best must be under to trust a decode. */
    private static final double SOFT_THRESHOLD = 0.70;
    /** Minimum consecutive good symbols before text is emitted. */
    private static final int    MIN_GOOD_SYMS  = 4;

    // ── AFC parameters ────────────────────────────────────────────────
    /** Search range around DEFAULT_CENTRE for AFC (Hz). Wide enough to span
     *  the standard off-centre test variants at 1203 Hz (tones from 1094 Hz)
     *  and 1797 Hz (tones up to 1922 Hz). */
    private static final double AFC_SEARCH_HZ  = 500.0;
    /** Min frames of integrated spectrum before AFC can fire. Enough to
     *  let the preamble chirp visit all 16 tones (16 nibbles + decay margin). */
    private static final int    AFC_MIN_FRAMES = 20;
    /** Centre adjustment less than half a bin is ignored — Goertzel snaps to
     *  the nearest bin, so any sub-bin AFC move would mis-align tones. */
    private static final double AFC_DEADBAND   = SPACING * 0.5;  // ~7.8 Hz
    /** Re-tune step cap (Hz) to avoid wild jumps; large enough to reach any
     *  variant within the AFC search range in a single step. */
    private static final double AFC_MAX_JUMP   = 600.0;
    /** Min fraction of MFSK16 bandwidth that must be occupied to allow AFC. */
    private static final double AFC_BW_GATE    = 0.60;

    private static final boolean DEBUG         = false;

    // =================================================================
    // Per-instance state
    // =================================================================

    private boolean initialized = false;
    private double  sampleRate  = 8000.0;
    private int     spS         = 512;
    /** Currently-tuned centre frequency (Hz). */
    private double  centreHz    = DEFAULT_CENTRE;
    private double[] toneFreqs  = buildToneFreqs(8000.0, DEFAULT_CENTRE);

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
    /** Previous received tone index. Initialised to 0 (the encoder's
     *  starting state) so the first received tone — typically tone 1 from
     *  the first preamble nibble — yields nibble 0 directly instead of
     *  being thrown away. Skipping the first tone shifts the bit stream
     *  by 1 nibble (2 data bits) and breaks byte alignment. */
    private int     prevTone     = 0;

    // ── Viterbi FEC decoder ───────────────────────────────────────────
    private final ViterbiK7 viterbi = new ViterbiK7();
    /** Decoded-bit accumulator; filled LSB-first, 8 bits → 1 byte. */
    private int     decodedBits  = 0;
    private int     decodedCount = 0;

    // ── Confidence gate ───────────────────────────────────────────────
    private int     goodSymCount = 0;

    // ── AFC peak-hold spectrum ────────────────────────────────────────
    /** Frequencies sampled by the AFC scanner (cover DEFAULT_CENTRE ± AFC_SEARCH_HZ). */
    private double[] afcFreqs;
    /** Peak-hold per-bin power. */
    private double[] afcSpectrum;
    private int      afcFrames    = 0;
    /** Frames since last AFC retune; used to throttle adjustments. */
    private int      afcSettle    = 0;

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

        // Update AFC spectrum and (if not yet locked) consider retuning.
        // tryAcquire is gated behind 2 × AFC_MIN_FRAMES so AFC has time to
        // detect an off-centre signal before any premature symbol lock.
        if (!symLocked) {
            updateAfcSpectrum();
            maybeRetune();
            if (afcFrames >= 2 * AFC_MIN_FRAMES) {
                tryAcquire();
            }
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
        centreHz   = DEFAULT_CENTRE;
        toneFreqs  = buildToneFreqs(sr, centreHz);
        symBuf     = new float[spS];
        afcFreqs   = buildAfcFreqs(sr);
        afcSpectrum = new double[afcFreqs.length];
        resetDecodeState();
        initialized = true;
        debug(String.format("init sr=%.0f spS=%d centre=%.1f Hz tone[0]=%.3f tone[15]=%.3f",
                sr, spS, centreHz, toneFreqs[0], toneFreqs[15]));
    }

    /** Build 16 tone frequencies symmetric about centre. */
    private static double[] buildToneFreqs(double sr, double centre) {
        double spacing = sr / 512;
        // Place tones so they are symmetric around centre:
        //  tone[i] = centre - 7.5*spacing + i*spacing
        double[] f = new double[NUM_TONES];
        double base = centre - 7.5 * spacing;
        for (int i = 0; i < NUM_TONES; i++) f[i] = base + i * spacing;
        return f;
    }

    /** Build AFC scan frequencies: bin-aligned around DEFAULT_CENTRE ± AFC_SEARCH_HZ. */
    private static double[] buildAfcFreqs(double sr) {
        double spacing = sr / 512;
        int firstBin = (int) Math.round((DEFAULT_CENTRE - AFC_SEARCH_HZ) / spacing);
        int lastBin  = (int) Math.round((DEFAULT_CENTRE + AFC_SEARCH_HZ) / spacing);
        int n = lastBin - firstBin + 1;
        double[] f = new double[n];
        for (int i = 0; i < n; i++) f[i] = (firstBin + i) * spacing;
        return f;
    }

    private void resetDecodeState() {
        prevTone     = 0;
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
        if (afcSpectrum != null) Arrays.fill(afcSpectrum, 0.0);
        afcFrames = 0;
        afcSettle = 0;
        centreHz  = DEFAULT_CENTRE;
        if (initialized) toneFreqs = buildToneFreqs(sampleRate, centreHz);
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
    // AFC: integrated spectrum + centroid-based retune
    // =================================================================

    private void updateAfcSpectrum() {
        if (nRaw < spS) return;
        // Take the most recent symbol-length slice for the AFC scan
        int start = nRaw - spS;
        for (int i = 0; i < afcFreqs.length; i++) {
            double p = Goertzel.power(rawBuf, start, spS, (float) sampleRate, afcFreqs[i]);
            // Peak-hold with slow decay so transient bursts don't dominate
            afcSpectrum[i] = Math.max(p, afcSpectrum[i] * 0.998);
        }
        afcFrames++;
        afcSettle++;
    }

    private void maybeRetune() {
        if (afcFrames < AFC_MIN_FRAMES) return;
        if (afcSettle < AFC_MIN_FRAMES) return;
        int n = afcSpectrum.length;
        if (n < NUM_TONES) return;

        // Find peak power across the AFC scan
        double maxP = 0.0;
        for (double p : afcSpectrum) if (p > maxP) maxP = p;
        if (maxP <= 0.0) return;

        // Find first and last bin with power >= 50% of max — the "lit" span.
        // For a fully-developed MFSK16 signal this span is exactly 16 bins
        // (= MFSK16_BW); fewer means the preamble chirp hasn't visited all
        // tones yet and we should wait before retuning.
        double thresh = maxP * 0.5;
        int firstLit = -1, lastLit = -1;
        for (int i = 0; i < n; i++) {
            if (afcSpectrum[i] >= thresh) {
                if (firstLit < 0) firstLit = i;
                lastLit = i;
            }
        }
        if (firstLit < 0) return;
        int span = lastLit - firstLit + 1;
        // Require span to be EXACTLY NUM_TONES bins so the lit edges define
        // the band unambiguously. Anything else means the preamble hasn't
        // fully developed (chirp incomplete) and we should wait.
        if (span != NUM_TONES) return;

        double winFirstHz = afcFreqs[firstLit];
        double newCentre  = winFirstHz + (NUM_TONES - 1) * 0.5 * SPACING;

        double delta = newCentre - centreHz;
        if (Math.abs(delta) < AFC_DEADBAND) return;
        if (delta >  AFC_MAX_JUMP) delta =  AFC_MAX_JUMP;
        if (delta < -AFC_MAX_JUMP) delta = -AFC_MAX_JUMP;
        newCentre = centreHz + delta;
        retune(newCentre);
        debug(String.format("AFC retune centre %.1f → %.1f (span=%d, frames=%d)",
                centreHz - delta, newCentre, span, afcFrames));
    }

    private void retune(double newCentre) {
        centreHz  = newCentre;
        toneFreqs = buildToneFreqs(sampleRate, centreHz);
        // Reset symbol-level state so we re-acquire timing with the new tones
        symLocked    = false;
        symOffset    = 0;
        prevTone     = 0;
        decodedBits  = 0;
        decodedCount = 0;
        viterbi.reset();
        goodSymCount = 0;
        afcSettle    = 0;
        // Clear peak-hold so stale peaks from the wrong centre don't
        // influence the next retune decision.
        if (afcSpectrum != null) Arrays.fill(afcSpectrum, 0.0);
    }

    // =================================================================
    // Symbol timing acquisition
    // =================================================================

    /**
     * Evaluate 8 evenly-spaced candidate offsets over 8 symbols.  The MFSK16
     * preamble walks tone 1, 2, 3, …, 15, 0, 1, 2, … (16-byte preamble of
     * 0x00, encoder state cycling), so the correct timing offset gives
     * consistently high peakness while wrong offsets straddle adjacent
     * symbols and dilute the peak. Eight samples gives a reliable best.
     */
    private void tryAcquire() {
        int evalSyms = 8;
        int nCand    = 8;
        if (nRaw < spS * (evalSyms + 1)) return;

        double bestScore = -1.0;
        int    bestOff   = 0;

        int step = spS / nCand;
        for (int i = 0; i < nCand; i++) {
            int off = i * step;
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
                if (score > bestScore) { bestScore = score; bestOff = off; }
            }
        }

        if (bestScore >= LOCK_ACQUIRE) {
            symLocked    = true;
            symOffset    = bestOff;
            prevTone     = 0;
            decodedBits  = 0;
            decodedCount = 0;
            viterbi.reset();
            goodSymCount = 0;
            debug(String.format("SYM LOCK off=%d score=%.2f centre=%.1f", bestOff, bestScore, centreHz));
        }
    }

    /** Peakness = (max tone power / avg tone power) normalised to [0, numTones]. */
    private double peaknessAt(int start) {
        double max = 0.0, sum = 0.0;
        for (double f : toneFreqs) {
            double p = Goertzel.power(rawBuf, start, spS, (float) sampleRate, f);
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
        // secondP near zero means one tone has all the energy — that's a
        // PERFECT signal, not a confused one. Treat as max confidence.
        double conf     = (secondP > 1e-12) ? maxP / (maxP + secondP) : 1.0;

        lastSnr  = (sumP > 1e-12) ? 10.0 * Math.log10(maxP / (sumP / NUM_TONES)) : 0.0;
        lastConf = conf;

        // Quality gate: accept symbols where one tone clearly dominates.
        boolean good = peakness >= LOCK_RATIO && conf > SOFT_THRESHOLD;
        if (!good) {
            goodSymCount = 0;
            prevTone = bestT;  // keep tracking but don't decode this nibble
            return;
        }
        goodSymCount++;

        // IFK+ differential decode against prevTone, which starts at 0
        // (matching the TX encoder's initial state).
        int nibble = (bestT - prevTone - 1 + NUM_TONES) % NUM_TONES;
        prevTone = bestT;

        feedNibbleToViterbi(nibble);
    }

    // =================================================================
    // Nibble → Viterbi → byte assembly
    // =================================================================

    /**
     * Feed the 4 bits of a nibble as two rate-1/2 encoded symbols into the
     * Viterbi decoder (MSB pair first), then collect any decoded bits.
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

        debug("EMIT '" + text + "'");
        return Optional.of(new DecodeMessage(
                ModeType.MFSK16, text, rigHz, centreHz, lastSnr, lastConf));
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[MFSK16] " + msg);
    }
}
