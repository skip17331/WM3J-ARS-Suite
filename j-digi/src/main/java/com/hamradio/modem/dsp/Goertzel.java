package com.hamradio.modem.dsp;

public final class Goertzel {
    private Goertzel() {}

    public static double power(float[] samples, float sampleRate, double targetHz) {
        int n = samples.length;
        if (n == 0 || sampleRate <= 0) return 0.0;
        double k = Math.round((n * targetHz) / sampleRate);
        double omega = 2.0 * Math.PI * k / n;
        double coeff = 2.0 * Math.cos(omega);
        double q0 = 0.0;
        double q1 = 0.0;
        double q2 = 0.0;
        for (float sample : samples) {
            q0 = coeff * q1 - q2 + sample;
            q2 = q1;
            q1 = q0;
        }
        return q1 * q1 + q2 * q2 - coeff * q1 * q2;
    }
}
