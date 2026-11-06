package com.ars.fx.net;

/** What one station is doing, as the others see it: which operator, which contest, and where its rig is. {@code seenMs} is when we last heard it. */
public record NetStatus(String station, String op, String contest, String band, String mode, long freqHz, long seenMs, long skewMs, boolean run) {
    /** A status with a clock reading but no Run / S&P (what an older build sends). */
    public NetStatus(String station, String op, String contest, String band, String mode, long freqHz, long seenMs, long skewMs) {
        this(station, op, contest, band, mode, freqHz, seenMs, skewMs, false);
    }
    /** A station's status with no clock information (what older builds send, or a status made locally). */
    public NetStatus(String station, String op, String contest, String band, String mode, long freqHz, long seenMs) {
        this(station, op, contest, band, mode, freqHz, seenMs, 0, false);
    }
    /** The other station's clock minus ours, when that station last reported. Positive: its clock is ahead. */
    public String label() {
        String o = op == null || op.isBlank() ? "" : " (" + op + ")";
        return station + o;
    }
}
