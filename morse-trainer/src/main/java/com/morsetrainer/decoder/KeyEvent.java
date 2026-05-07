package com.morsetrainer.decoder;

/**
 * A single keying event with a timestamp in milliseconds.
 * Down = key pressed (audio on), Up = key released (audio off).
 */
public record KeyEvent(Type type, long timestampMillis) {
    public enum Type { DOWN, UP }

    public static KeyEvent down(long t) { return new KeyEvent(Type.DOWN, t); }
    public static KeyEvent up(long t)   { return new KeyEvent(Type.UP, t); }
}
