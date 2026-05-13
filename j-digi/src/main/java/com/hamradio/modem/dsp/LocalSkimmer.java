package com.hamradio.modem.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * LocalSkimmer — multi-channel CW signal detector that runs in parallel
 * with whatever mode the operator has selected. Identifies every CW
 * carrier above a noise-floor threshold across the audio passband and
 * publishes them as <em>activity</em> events so j-hub can render them
 * to other modules (J-Map waterfall colouring, J-Log spot pane).
 *
 * <p>This is the foundation tier of Phase 2's "embedded CW skimmer".
 * Per-channel decoding (multiple {@link com.hamradio.modem.mode.CwMode}
 * instances behind Goertzel band-pass filters, then callsign
 * extraction) is a follow-up — but even on its own, the detector turns
 * J-Digi into a band-activity gauge for the audio passband which is
 * useful for finding open frequencies during contests.
 *
 * <p>Algorithm per frame:
 * <ol>
 *   <li>Take the magnitude spectrum from the existing
 *       {@link FftAnalyzer}.</li>
 *   <li>Smooth it with a 5-bin median filter so transient FFT peaks
 *       don't trigger phantom signals.</li>
 *   <li>Track a slow-moving noise floor per-bin (EMA, 5-second time
 *       constant) so QSB and band changes don't push the threshold up.</li>
 *   <li>Pick peaks {@code peakDbThreshold} dB above the local noise
 *       floor and at least {@code peakSeparationHz} apart.</li>
 *   <li>Confirm narrowness — a "real" CW carrier sits in roughly one
 *       FFT bin pair. Voice/wideband signals are rejected.</li>
 *   <li>Emit the top-N peaks as a single {@link Snapshot} via the
 *       registered listener.</li>
 * </ol>
 */
public class LocalSkimmer {

    private static final Logger log = LoggerFactory.getLogger(LocalSkimmer.class);

    private final int   fftSize;
    private final float sampleRate;
    private final double binHz;

    // ── Tunables (operators rarely need to change these) ───────────
    /** Audio band to scan for CW signals (Hz). Above 200 Hz to avoid
     *  speaker hum; below 1500 Hz to avoid the high-pass of most SSB
     *  filters and the audio rolloff above 2 kHz. */
    private double minScanHz = 300.0;
    private double maxScanHz = 1500.0;
    /** SNR threshold in dB above the local noise floor. */
    private double peakDbThreshold = 12.0;
    /** Minimum separation between two detected peaks (Hz). CW signals
     *  are ~50 Hz wide; 80 Hz dedup is conservative. */
    private double peakSeparationHz = 80.0;
    /** Maximum number of peaks to surface per snapshot. */
    private int maxPeaks = 16;
    /** Maximum spectral width (Hz) for a peak to count as CW —
     *  rejects wideband interference (voice, digital modes, splatter). */
    private double maxCwBandwidthHz = 120.0;

    // ── State ──────────────────────────────────────────────────────
    /** Per-bin EMA noise-floor estimate. Lazily sized on first frame. */
    private double[] noiseFloor;
    /** EMA coefficient — ~5-second time constant at the 8 kHz/256 frame
     *  rate (about 31 frames/sec → α = 1 - exp(-1/(5*31)) ≈ 0.006). */
    private static final double NOISE_ALPHA = 0.006;

    private volatile Consumer<Snapshot> listener = s -> {};
    private volatile boolean enabled = false;

    public LocalSkimmer(int fftSize, float sampleRate) {
        this.fftSize    = fftSize;
        this.sampleRate = sampleRate;
        this.binHz      = sampleRate / (double) fftSize;
    }

    // ── Public controls ────────────────────────────────────────────

    public void setEnabled(boolean on) { this.enabled = on; }
    public boolean isEnabled() { return enabled; }

    public void setListener(Consumer<Snapshot> l) { this.listener = l != null ? l : s -> {}; }

    public void setPeakDbThreshold(double db)   { this.peakDbThreshold = db; }
    public void setMaxPeaks(int n)              { this.maxPeaks = Math.max(1, n); }
    public void setScanBand(double minHz, double maxHz) {
        if (maxHz > minHz) { this.minScanHz = minHz; this.maxScanHz = maxHz; }
    }

    // ── Per-frame processing ───────────────────────────────────────

    /**
     * Process one frame's spectrum. Called from the audio thread —
     * keep allocations to a minimum.
     */
    public void process(double[] magnitudes) {
        if (!enabled || magnitudes == null || magnitudes.length < 4) return;

        if (noiseFloor == null || noiseFloor.length != magnitudes.length) {
            noiseFloor = new double[magnitudes.length];
            // Seed with the median of the in-band slice. That gives a
            // representative "quiet bin" value without latching onto any
            // signal that happens to be in the first frame.
            int startSeed = (int) Math.floor(minScanHz / binHz);
            int endSeed   = Math.min(magnitudes.length - 1, (int) Math.ceil(maxScanHz / binHz));
            double[] slice = Arrays.copyOfRange(magnitudes, startSeed, endSeed + 1);
            Arrays.sort(slice);
            double seed = slice.length > 0 ? slice[slice.length / 2] : 1e-6;
            Arrays.fill(noiseFloor, seed);
        }

        // Update noise floor only on bins whose magnitude isn't more than
        // 3× the current floor. That blocks signal energy from ratcheting
        // the floor up while still letting it drift in response to gain
        // changes / band noise / QSB.
        for (int i = 0; i < magnitudes.length; i++) {
            if (magnitudes[i] < noiseFloor[i] * 3.0) {
                noiseFloor[i] += NOISE_ALPHA * (magnitudes[i] - noiseFloor[i]);
            }
        }

        int startBin = (int) Math.floor(minScanHz / binHz);
        int endBin   = (int) Math.ceil(maxScanHz / binHz);
        endBin = Math.min(endBin, magnitudes.length - 1);
        if (startBin >= endBin) return;

        // Median-smooth the in-band slice to reject spurious single-bin
        // glitches. 5-bin window — wider than a CW signal so it doesn't
        // smear them out.
        double[] smooth = medianSmooth(magnitudes, startBin, endBin, 5);

        // Compute dB-over-floor for each bin in band
        double[] db = new double[smooth.length];
        for (int i = 0; i < smooth.length; i++) {
            int absBin = startBin + i;
            double floor = Math.max(noiseFloor[absBin], 1e-9);
            db[i] = 20.0 * Math.log10((smooth[i] + 1e-9) / floor);
        }

        // Find local maxima above threshold
        List<Peak> peaks = new ArrayList<>();
        for (int i = 1; i < db.length - 1; i++) {
            if (db[i] <= peakDbThreshold) continue;
            if (db[i] < db[i - 1] || db[i] < db[i + 1]) continue;
            int absBin = startBin + i;
            // Estimate signal bandwidth (3 dB)
            double bw = estimateBandwidth(smooth, i);
            if (bw > maxCwBandwidthHz / binHz) continue; // too wide for CW
            // Sub-bin frequency refinement via parabolic interpolation
            double delta = parabolicOffset(smooth, i);
            double freqHz = (absBin + delta) * binHz;
            peaks.add(new Peak(freqHz, db[i], bw * binHz));
        }

        if (peaks.isEmpty()) {
            listener.accept(new Snapshot(System.currentTimeMillis(), Collections.emptyList()));
            return;
        }

        // Dedup peaks within peakSeparationHz of each other — keep highest
        peaks.sort((a, b) -> Double.compare(b.snrDb, a.snrDb));
        List<Peak> kept = new ArrayList<>();
        for (Peak p : peaks) {
            boolean tooClose = false;
            for (Peak k : kept) {
                if (Math.abs(p.freqHz - k.freqHz) < peakSeparationHz) {
                    tooClose = true; break;
                }
            }
            if (!tooClose) kept.add(p);
            if (kept.size() >= maxPeaks) break;
        }

        listener.accept(new Snapshot(System.currentTimeMillis(),
                                     Collections.unmodifiableList(kept)));
    }

    // ── DSP helpers ────────────────────────────────────────────────

    private static double[] medianSmooth(double[] src, int from, int to, int win) {
        int len = to - from;
        double[] out = new double[len];
        int half = win / 2;
        double[] buf = new double[win];
        for (int i = 0; i < len; i++) {
            int copied = 0;
            for (int j = -half; j <= half; j++) {
                int idx = from + i + j;
                if (idx >= 0 && idx < src.length) buf[copied++] = src[idx];
            }
            double[] slice = Arrays.copyOf(buf, copied);
            Arrays.sort(slice);
            out[i] = slice[slice.length / 2];
        }
        return out;
    }

    /** Width (in bins) at -3 dB from the peak. */
    private static double estimateBandwidth(double[] smooth, int peakIdx) {
        double peak = smooth[peakIdx];
        double half = peak / Math.sqrt(2.0); // -3 dB amplitude
        int left = peakIdx, right = peakIdx;
        while (left  > 0 && smooth[left]  > half) left--;
        while (right < smooth.length - 1 && smooth[right] > half) right++;
        return right - left;
    }

    /** Parabolic interpolation around the peak bin for sub-bin frequency
     *  accuracy. Returns the offset in bins from peakIdx. */
    private static double parabolicOffset(double[] s, int i) {
        if (i <= 0 || i >= s.length - 1) return 0.0;
        double a = s[i - 1], b = s[i], c = s[i + 1];
        double denom = (a - 2 * b + c);
        if (Math.abs(denom) < 1e-12) return 0.0;
        return 0.5 * (a - c) / denom;
    }

    // ── Result records ─────────────────────────────────────────────

    /** A single detected CW carrier. */
    public static final class Peak {
        public final double freqHz;
        public final double snrDb;
        public final double bandwidthHz;
        public Peak(double freqHz, double snrDb, double bandwidthHz) {
            this.freqHz = freqHz; this.snrDb = snrDb; this.bandwidthHz = bandwidthHz;
        }
    }

    /** Per-frame snapshot of all detected CW carriers, ordered by SNR. */
    public static final class Snapshot {
        public final long timestampMs;
        public final List<Peak> peaks;
        public Snapshot(long timestampMs, List<Peak> peaks) {
            this.timestampMs = timestampMs;
            this.peaks       = peaks;
        }
    }
}
