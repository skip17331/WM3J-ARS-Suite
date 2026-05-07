package com.morsetrainer.analytics;

/** Mutable per-character running statistics for the receiving side. */
public class CharStats {
    public final char character;
    public int presented = 0;
    public int correct   = 0;
    public int errors    = 0;
    public long totalResponseMillis = 0;
    public long slowestResponseMillis = 0;

    public CharStats(char c) { this.character = c; }

    public double accuracy() { return presented == 0 ? 0.0 : (double) correct / presented; }
    public double errorRate() { return presented == 0 ? 0.0 : (double) errors / presented; }
    public double avgResponseMillis() { return correct == 0 ? 0.0 : (double) totalResponseMillis / correct; }

    public void recordCorrect(long responseMillis) {
        presented++; correct++;
        totalResponseMillis += responseMillis;
        if (responseMillis > slowestResponseMillis) slowestResponseMillis = responseMillis;
    }

    public void recordError() { presented++; errors++; }
}
