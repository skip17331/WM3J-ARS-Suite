package com.hamradio.modem.dsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * SignalClassifier — on-demand "what mode is this signal?" tagger.
 *
 * <p>Subscribes to the audio stream as an {@link java.util.function.Consumer Consumer&lt;float[]&gt;}
 * listener and keeps a rolling ~16-second window of samples (long enough to
 * see one full FT8 slot). When the operator clicks a signal in the waterfall
 * the UI calls {@link #classify(double)} with the center frequency; the
 * classifier returns a best-guess {@link ClassifiedMode} with confidence and
 * a few diagnostic notes.
 *
 * <p>Intentionally heuristic: we don't try to demodulate, just measure a few
 * spectral and temporal characteristics that distinguish the common modes:
 * <ul>
 *   <li>Bandwidth at -10 dB (CW ≈ 50 Hz, RTTY ≈ 250 Hz incl. mark+space,
 *       PSK31 ≈ 60 Hz, FT8 ≈ 50 Hz, SSB ≈ 2.4 kHz)
 *   <li>Second tone ~170 Hz away (RTTY's mark/space pair)
 *   <li>Envelope variability over time (CW keys on/off; FT8 has a characteristic
 *       13.6 s on / 1.4 s off duty cycle on 15 s slot boundaries; PSK31/RTTY
 *       are nearly continuous; SSB is voice-modulated)
 *   <li>FT4: 4.5 s on / 3 s off cycle on 7.5 s slots
 * </ul>
 *
 * <p>Thread-safe for the producer/consumer split: {@link #accept(float[])} runs
 * on the audio capture thread; {@link #classify(double)} runs on the FX
 * thread. The shared frame deque is guarded by {@code synchronized} blocks
 * (frames are short and writes are infrequent enough that contention is
 * negligible — premature optimization here would cost clarity).
 */
public class SignalClassifier implements java.util.function.Consumer<float[]> {

    private static final Logger log = LoggerFactory.getLogger(SignalClassifier.class);

    /** What we tell the operator. CW/RTTY/PSK31 overlap j-digi's existing
     *  ModeType enum; FT8/FT4/SSB are classifier-only (j-digi can't decode
     *  those, but it can still <em>identify</em> them on the band). */
    public enum ClassifiedMode { CW, RTTY, PSK31, FT8, FT4, SSB, UNKNOWN }

    public static final class Result {
        public final ClassifiedMode mode;
        public final double confidence;        // 0..1
        public final double centerHz;
        public final double bandwidthHz;       // -10 dB width
        public final String notes;

        public Result(ClassifiedMode mode, double confidence,
                      double centerHz, double bandwidthHz, String notes) {
            this.mode = mode;
            this.confidence = confidence;
            this.centerHz = centerHz;
            this.bandwidthHz = bandwidthHz;
            this.notes = notes;
        }

        @Override public String toString() {
            return String.format("%s @ %.0f Hz (BW %.0f Hz, conf %.2f)%s",
                    mode, centerHz, bandwidthHz, confidence,
                    notes == null || notes.isEmpty() ? "" : " — " + notes);
        }
    }

    private final float sampleRate;
    private final int   frameSize;
    private final int   maxFrames;             // ~16 s of frames

    /** Frames in arrival order. Oldest at head, newest at tail. */
    private final Deque<float[]> frames = new ArrayDeque<>();
    private final Object frameLock = new Object();

    private final FftAnalyzer fft;

    public SignalClassifier(float sampleRate, int frameSize) {
        this.sampleRate = sampleRate;
        this.frameSize  = frameSize;
        // 16 s coverage — frames are frameSize samples (e.g. 128 ms at 8 kHz),
        // so 16 s ≈ 125 frames. Round up to leave a little margin.
        this.maxFrames  = (int) Math.ceil(16.0 * sampleRate / frameSize) + 4;
        this.fft        = new FftAnalyzer(frameSize, sampleRate);
    }

    // ── Audio listener ───────────────────────────────────────────────

    @Override
    public void accept(float[] samples) {
        if (samples == null || samples.length == 0) return;
        // Only buffer frames that match our expected size — partial reads are
        // dropped to keep envelope timing math simple.
        if (samples.length != frameSize) return;
        synchronized (frameLock) {
            frames.addLast(samples.clone());
            while (frames.size() > maxFrames) frames.removeFirst();
        }
    }

    // ── Main entry point ─────────────────────────────────────────────

    /**
     * Classify the signal at {@code centerHz}. Safe to call from any thread.
     * Returns {@link ClassifiedMode#UNKNOWN} when the buffer is empty or the
     * requested frequency falls outside the audio passband.
     */
    public Result classify(double centerHz) {
        if (centerHz <= 0 || centerHz >= sampleRate / 2.0) {
            return new Result(ClassifiedMode.UNKNOWN, 0.0, centerHz, 0.0, "out of band");
        }

        float[][] snapshot;
        synchronized (frameLock) {
            if (frames.isEmpty()) {
                return new Result(ClassifiedMode.UNKNOWN, 0.0, centerHz, 0.0, "no audio yet");
            }
            snapshot = frames.toArray(new float[0][]);
        }

        // ── Spectral measurements (averaged over recent frames) ──────
        // Single-frame FFT is too noisy: a wide signal (SSB, raw noise) has
        // bin-to-bin variation that breaks the -10 dB walk-out. Averaging
        // ~8 recent frames smooths out the variability so the bandwidth
        // measurement reflects the actual spectral envelope.
        int avgFrames = Math.min(snapshot.length, 8);
        double[] mags = null;
        for (int i = snapshot.length - avgFrames; i < snapshot.length; i++) {
            FftAnalyzer.SpectrumResult one = fft.analyze(snapshot[i]);
            if (mags == null) mags = one.magnitudes().clone();
            else {
                double[] m = one.magnitudes();
                for (int k = 0; k < mags.length; k++) mags[k] += m[k];
            }
        }
        for (int k = 0; k < mags.length; k++) mags[k] /= avgFrames;
        double hzPerBin = sampleRate / frameSize;

        int peakBin = nearestPeakBin(mags, centerHz, hzPerBin);
        double peakMag = mags[peakBin];
        if (peakMag < 1.0) {
            // Noise-floor bin only; nothing to talk about.
            return new Result(ClassifiedMode.UNKNOWN, 0.1, centerHz, 0.0, "no signal at this freq");
        }
        double bw = bandwidthAtMinus10dB(mags, peakBin, hzPerBin);

        // ── RTTY mark/space detector ─────────────────────────────────
        double rttyShiftHz = 170.0; // standard amateur shift
        boolean hasSecondPeak = peakAt(mags, peakBin, rttyShiftHz, hzPerBin, peakMag);

        // ── Envelope over time at this center frequency ──────────────
        double[] env = envelopeOverTime(snapshot, centerHz);
        double envMean = mean(env);
        double envStd  = stddev(env, envMean);
        double envCv   = envMean > 1e-9 ? envStd / envMean : 0.0;
        double dutyCycle = duty(env, envMean);
        double burstSec  = longestBurstSec(env, envMean);

        // ── Decision tree ────────────────────────────────────────────
        // Tested against synthetic fixtures in SignalClassifierTest. Order
        // matters — RTTY check must precede the narrow-CW branch since RTTY's
        // mark+space combined width also looks "narrow".
        if (bw > 1500.0) {
            return new Result(ClassifiedMode.SSB, 0.7, peakHz(peakBin, hzPerBin), bw,
                    "wide envelope, voice-shaped");
        }
        if (hasSecondPeak && bw < 500.0) {
            return new Result(ClassifiedMode.RTTY, 0.85, peakHz(peakBin, hzPerBin), bw,
                    "two tones ~170 Hz apart");
        }
        if (bw < 120.0) {
            // Narrow signal — disambiguate by temporal pattern.
            // FT8: 13.6 s on / 1.4 s off in a 15 s slot.
            if (burstSec > 11.0 && burstSec < 14.5 && dutyCycle > 0.75 && dutyCycle < 0.97) {
                return new Result(ClassifiedMode.FT8, 0.8, peakHz(peakBin, hzPerBin), bw,
                        "13.6 s burst on 15 s slot");
            }
            // FT4: 4.5 s on / 3 s off in a 7.5 s slot.
            if (burstSec > 3.5 && burstSec < 5.2 && dutyCycle > 0.45 && dutyCycle < 0.75) {
                return new Result(ClassifiedMode.FT4, 0.7, peakHz(peakBin, hzPerBin), bw,
                        "4.5 s burst on 7.5 s slot");
            }
            // CW: high envelope variance from on/off keying.
            if (envCv > 0.45) {
                return new Result(ClassifiedMode.CW, 0.75, peakHz(peakBin, hzPerBin), bw,
                        "narrow tone, keyed on/off");
            }
            // PSK31: nearly steady narrow carrier (BPSK keeps the envelope ~constant).
            if (envCv < 0.25 && bw < 80.0) {
                return new Result(ClassifiedMode.PSK31, 0.65, peakHz(peakBin, hzPerBin), bw,
                        "steady narrow carrier");
            }
        }

        return new Result(ClassifiedMode.UNKNOWN, 0.4, peakHz(peakBin, hzPerBin), bw,
                String.format("BW=%.0f Hz, CV=%.2f, duty=%.2f", bw, envCv, dutyCycle));
    }

    // ── Helpers ──────────────────────────────────────────────────────

    /** Find the local peak bin nearest the requested center frequency. Search
     *  ±150 Hz so a slightly-off click still snaps to the actual signal. */
    private int nearestPeakBin(double[] mags, double centerHz, double hzPerBin) {
        int targetBin = (int) Math.round(centerHz / hzPerBin);
        int radius    = (int) Math.round(150.0 / hzPerBin);
        int lo = Math.max(0, targetBin - radius);
        int hi = Math.min(mags.length - 1, targetBin + radius);
        int best = targetBin;
        double bestMag = -1;
        for (int i = lo; i <= hi; i++) {
            if (mags[i] > bestMag) { bestMag = mags[i]; best = i; }
        }
        return best;
    }

    /** Walk outward from {@code peakBin} until the magnitude drops 10 dB. */
    static double bandwidthAtMinus10dB(double[] mags, int peakBin, double hzPerBin) {
        double threshold = mags[peakBin] / Math.pow(10.0, 10.0 / 20.0); // -10 dB
        int lo = peakBin;
        while (lo > 0 && mags[lo] >= threshold) lo--;
        int hi = peakBin;
        while (hi < mags.length - 1 && mags[hi] >= threshold) hi++;
        return (hi - lo) * hzPerBin;
    }

    /** True if there's another bin within ±20 Hz of (peakBin ± shiftHz) whose
     *  magnitude is at least 30% of the peak — RTTY's mark/space detector. */
    static boolean peakAt(double[] mags, int peakBin, double shiftHz,
                          double hzPerBin, double peakMag) {
        int shiftBins = (int) Math.round(shiftHz / hzPerBin);
        int slop      = Math.max(2, (int) Math.round(20.0 / hzPerBin));
        double thresh = peakMag * 0.30;
        for (int side = -1; side <= 1; side += 2) {
            int target = peakBin + side * shiftBins;
            int lo = Math.max(0, target - slop);
            int hi = Math.min(mags.length - 1, target + slop);
            for (int i = lo; i <= hi; i++) {
                if (mags[i] >= thresh) return true;
            }
        }
        return false;
    }

    /** Power-vs-time at {@code freqHz}, one sample per frame. */
    private double[] envelopeOverTime(float[][] snapshot, double freqHz) {
        double[] env = new double[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            env[i] = Math.sqrt(Math.max(0, Goertzel.power(snapshot[i], sampleRate, freqHz)));
        }
        return env;
    }

    private static double mean(double[] a) {
        double s = 0;
        for (double v : a) s += v;
        return a.length == 0 ? 0 : s / a.length;
    }

    private static double stddev(double[] a, double mean) {
        double s = 0;
        for (double v : a) s += (v - mean) * (v - mean);
        return a.length == 0 ? 0 : Math.sqrt(s / a.length);
    }

    /** Fraction of frames whose envelope exceeds the mean — rough "on" duty. */
    private static double duty(double[] env, double mean) {
        if (env.length == 0) return 0;
        int on = 0;
        for (double v : env) if (v > mean) on++;
        return (double) on / env.length;
    }

    /** Longest continuous "on" run in seconds (envelope above mean). */
    private double longestBurstSec(double[] env, double mean) {
        int best = 0, cur = 0;
        for (double v : env) {
            if (v > mean) { cur++; if (cur > best) best = cur; }
            else          { cur = 0; }
        }
        return best * (frameSize / (double) sampleRate);
    }

    private double peakHz(int bin, double hzPerBin) { return bin * hzPerBin; }
}
