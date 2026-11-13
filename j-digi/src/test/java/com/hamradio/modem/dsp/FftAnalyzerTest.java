package com.hamradio.modem.dsp;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The fast FFT must give the numbers the direct sum gives. */
class FftAnalyzerTest {

    /** The reference: the direct sum over the same windowed input. */
    private static double[] direct(float[] input) {
        int n = input.length;
        float[] w = input.clone();
        WindowFunctions.applyHamming(w);
        double[] mags = new double[n / 2];
        for (int k = 0; k < mags.length; k++) {
            double re = 0, im = 0, scale = -2.0 * Math.PI * k / n;
            for (int i = 0; i < n; i++) { re += w[i] * Math.cos(scale * i); im += w[i] * Math.sin(scale * i); }
            mags[k] = Math.sqrt(re * re + im * im);
        }
        return mags;
    }

    @Test
    void fastMatchesDirectOnNoiseAndTones() {
        int n = 1024;
        FftAnalyzer fft = new FftAnalyzer(n, 8000f);
        Random rnd = new Random(42);
        float[] x = new float[n];
        for (int i = 0; i < n; i++) x[i] = (float) (0.3 * Math.sin(2 * Math.PI * 700 * i / 8000.0) + 0.1 * rnd.nextGaussian());
        double[] want = direct(x), got = fft.analyze(x).magnitudes();
        for (int k = 0; k < want.length; k++) assertEquals(want[k], got[k], 1e-6 * Math.max(1, want[k]), "bin " + k);
        assertEquals(700.0, fft.analyze(x).peakFrequencyHz(), 8000.0 / n);
    }

    @Test
    void otherSizesStillWork() {
        FftAnalyzer fft = new FftAnalyzer(600, 8000f);     // not a power of two: the direct sum
        float[] x = new float[600];
        for (int i = 0; i < x.length; i++) x[i] = (float) Math.sin(2 * Math.PI * 1000 * i / 8000.0);
        assertEquals(1000.0, fft.analyze(x).peakFrequencyHz(), 8000.0 / 600 * 1.5);
    }
}
