package com.hamradio.modem.dsp;

public final class Goertzel {
    private Goertzel() {}

    public static double power(float[] samples, float sampleRate, double targetHz) {
        if (samples == null) return 0.0;
        return power(samples, 0, samples.length, sampleRate, targetHz);
    }

    /** Power at targetHz computed over samples[offset, offset+length). */
    public static double power(float[] samples, int offset, int length,
                               float sampleRate, double targetHz) {
        if (samples == null || length <= 0 || sampleRate <= 0) return 0.0;
        if (offset < 0 || offset + length > samples.length) return 0.0;
        double k = Math.round((length * targetHz) / sampleRate);
        double omega = 2.0 * Math.PI * k / length;
        double coeff = 2.0 * Math.cos(omega);
        double q1 = 0.0;
        double q2 = 0.0;
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            double q0 = coeff * q1 - q2 + samples[i];
            q2 = q1;
            q1 = q0;
        }
        return q1 * q1 + q2 * q2 - coeff * q1 * q2;
    }
}
