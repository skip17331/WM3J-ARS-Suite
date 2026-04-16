package com.hamradio.modem.dsp;

public final class WindowFunctions {
    private WindowFunctions() {}

    public static void applyHamming(float[] samples) {
        int n = samples.length;
        if (n < 2) return;
        for (int i = 0; i < n; i++) {
            double w = 0.54 - 0.46 * Math.cos((2.0 * Math.PI * i) / (n - 1));
            samples[i] *= (float) w;
        }
    }
}
