package com.ars.fx.net;

/** What one station is doing, as the others see it: which operator, which contest, and where its rig is. {@code seenMs} is when we last heard it. */
public record NetStatus(String station, String op, String contest, String band, String mode, long freqHz, long seenMs) {
    public String label() {
        String o = op == null || op.isBlank() ? "" : " (" + op + ")";
        return station + o;
    }
}
