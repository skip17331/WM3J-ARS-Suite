package com.hamradio.modem.dsp;

public class FftAnalyzer {
    private final int size;
    private final float sampleRate;

    public FftAnalyzer(int size, float sampleRate) {
        this.size = size;
        this.sampleRate = sampleRate;
    }

    public SpectrumResult analyze(float[] input) {
        if (input.length != size) {
            throw new IllegalArgumentException("Expected " + size + " samples, got " + input.length);
        }
        float[] windowed = input.clone();
        WindowFunctions.applyHamming(windowed);

        int bins = size / 2;
        double[] mags = new double[bins];
        double peakMag = Double.NEGATIVE_INFINITY;
        int peakBin = 0;

        for (int k = 0; k < bins; k++) {
            double real = 0.0;
            double imag = 0.0;
            double angleScale = -2.0 * Math.PI * k / size;
            for (int n = 0; n < size; n++) {
                double angle = angleScale * n;
                real += windowed[n] * Math.cos(angle);
                imag += windowed[n] * Math.sin(angle);
            }
            double mag = Math.sqrt(real * real + imag * imag);
            mags[k] = mag;
            if (mag > peakMag) {
                peakMag = mag;
                peakBin = k;
            }
        }

        double peakFrequency = peakBin * sampleRate / size;
        double rms = rms(input);
        double avg = 0.0;
        for (double v : mags) avg += v;
        avg = mags.length == 0 ? 0.0 : avg / mags.length;
        double snr = avg > 0 ? 20.0 * Math.log10((peakMag + 1e-9) / (avg + 1e-9)) : 0.0;

        return new SpectrumResult(mags, rms, peakFrequency, snr);
    }

    private double rms(float[] samples) {
        double sumSq = 0.0;
        for (float sample : samples) sumSq += sample * sample;
        return Math.sqrt(sumSq / samples.length);
    }

    public record SpectrumResult(double[] magnitudes, double rms, double peakFrequencyHz, double snrDb) {}
}
