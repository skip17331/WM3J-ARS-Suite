package com.hamradio.modem.dsp;

public class FftAnalyzer {
    private final int size;
    private final float sampleRate;

    /** Tables for the fast path (power-of-two sizes); null for any other size, which keeps the direct sum. Read-only after construction,
     *  so one analyzer can be used from more than one thread. */
    private final double[] cos, sin;
    private final int[] rev;

    public FftAnalyzer(int size, float sampleRate) {
        this.size = size;
        this.sampleRate = sampleRate;
        if (size >= 4 && Integer.bitCount(size) == 1) {
            cos = new double[size / 2]; sin = new double[size / 2];
            for (int i = 0; i < size / 2; i++) { double a = -2.0 * Math.PI * i / size; cos[i] = Math.cos(a); sin[i] = Math.sin(a); }
            rev = new int[size];
            int bits = Integer.numberOfTrailingZeros(size);
            for (int i = 0; i < size; i++) rev[i] = Integer.reverse(i) >>> (32 - bits);
        } else { cos = null; sin = null; rev = null; }
    }

    /** Magnitudes of the first size/2 bins: a radix-2 FFT, the same numbers as the direct sum below at a small fraction of the work
     *  (the direct sum was about a million sine / cosine calls per audio frame, a third of a core for the audio thread alone). */
    private double[] magnitudesFast(float[] x) {
        int n = size;
        double[] re = new double[n], im = new double[n];
        for (int i = 0; i < n; i++) re[rev[i]] = x[i];
        for (int len = 2; len <= n; len <<= 1) {
            int half = len >> 1, step = n / len;
            for (int i = 0; i < n; i += len) {
                for (int j = 0, t = 0; j < half; j++, t += step) {
                    int a = i + j, b = a + half;
                    double wr = cos[t], wi = sin[t];
                    double xr = re[b] * wr - im[b] * wi, xi = re[b] * wi + im[b] * wr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr;        im[a] += xi;
                }
            }
        }
        double[] mags = new double[n / 2];
        for (int k = 0; k < mags.length; k++) mags[k] = Math.sqrt(re[k] * re[k] + im[k] * im[k]);
        return mags;
    }

    public SpectrumResult analyze(float[] input) {
        if (input.length != size) {
            throw new IllegalArgumentException("Expected " + size + " samples, got " + input.length);
        }
        float[] windowed = input.clone();
        WindowFunctions.applyHamming(windowed);

        int bins = size / 2;
        double[] mags = rev != null ? magnitudesFast(windowed) : magnitudesDirect(windowed, bins);
        double peakMag = Double.NEGATIVE_INFINITY;
        int peakBin = 0;
        for (int k = 0; k < bins; k++) if (mags[k] > peakMag) { peakMag = mags[k]; peakBin = k; }

        double peakFrequency = peakBin * sampleRate / size;
        double rms = rms(input);
        double avg = 0.0;
        for (double v : mags) avg += v;
        avg = mags.length == 0 ? 0.0 : avg / mags.length;
        double snr = avg > 0 ? 20.0 * Math.log10((peakMag + 1e-9) / (avg + 1e-9)) : 0.0;

        return new SpectrumResult(mags, rms, peakFrequency, snr);
    }

    private double[] magnitudesDirect(float[] windowed, int bins) {
        double[] mags = new double[bins];
        for (int k = 0; k < bins; k++) {
            double real = 0.0;
            double imag = 0.0;
            double angleScale = -2.0 * Math.PI * k / size;
            for (int n = 0; n < size; n++) {
                double angle = angleScale * n;
                real += windowed[n] * Math.cos(angle);
                imag += windowed[n] * Math.sin(angle);
            }
            mags[k] = Math.sqrt(real * real + imag * imag);
        }
        return mags;
    }

    private double rms(float[] samples) {
        double sumSq = 0.0;
        for (float sample : samples) sumSq += sample * sample;
        return Math.sqrt(sumSq / samples.length);
    }

    public record SpectrumResult(double[] magnitudes, double rms, double peakFrequencyHz, double snrDb) {}
}
