package com.hamradio.modem.model;

import java.time.Instant;

public class DecodeMessage {
    private final ModeType mode;
    private final String text;
    private final long frequencyHz;
    private final double offsetHz;
    private final double snr;
    private final double confidence;
    private final Instant timestamp;

    public DecodeMessage(ModeType mode, String text, long frequencyHz, double offsetHz, double snr, double confidence) {
        this.mode = mode;
        this.text = text;
        this.frequencyHz = frequencyHz;
        this.offsetHz = offsetHz;
        this.snr = snr;
        this.confidence = confidence;
        this.timestamp = Instant.now();
    }

    public ModeType getMode() { return mode; }
    public String getText() { return text; }
    public long getFrequencyHz() { return frequencyHz; }
    public double getOffsetHz() { return offsetHz; }
    public double getSnr() { return snr; }
    public double getConfidence() { return confidence; }
    public Instant getTimestamp() { return timestamp; }
}
