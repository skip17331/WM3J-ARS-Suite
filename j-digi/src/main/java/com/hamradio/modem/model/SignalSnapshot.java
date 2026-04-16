package com.hamradio.modem.model;

public class SignalSnapshot {
    private final float[] samples;
    private final double[] magnitudes;
    private final double rms;
    private final double peakFrequencyHz;
    private final double sampleRate;

    public SignalSnapshot(float[] samples, double[] magnitudes, double rms, double peakFrequencyHz, double sampleRate) {
        this.samples = samples;
        this.magnitudes = magnitudes;
        this.rms = rms;
        this.peakFrequencyHz = peakFrequencyHz;
        this.sampleRate = sampleRate;
    }

    public float[] getSamples() { return samples; }
    public double[] getMagnitudes() { return magnitudes; }
    public double getRms() { return rms; }
    public double getPeakFrequencyHz() { return peakFrequencyHz; }
    public double getSampleRate() { return sampleRate; }
}
