package com.morsetrainer.decoder;

/**
 * A single Morse element after timing analysis: dit, dah, or a gap.
 * Durations are in milliseconds.
 */
public record DecodedElement(Kind kind, long durationMillis, long startTime) {
    public enum Kind { DIT, DAH, INTRA_GAP, CHAR_GAP, WORD_GAP }

    public boolean isMark() { return kind == Kind.DIT || kind == Kind.DAH; }
    public boolean isGap()  { return !isMark(); }
}
