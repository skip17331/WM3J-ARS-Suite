package com.morsetrainer.hardware;

import com.morsetrainer.decoder.KeyEvent;

import java.util.function.Consumer;

/**
 * A source of KeyEvents. Implemented by the keyboard listener, Arduino USB
 * keyer, and Pi Zero wireless keyer. Lets the rest of the app work with any
 * input transparently.
 */
public interface KeyEventSource extends AutoCloseable {
    /** Begin streaming events to {@code consumer}. */
    void start(Consumer<KeyEvent> consumer);

    /** Stop streaming. Safe to call multiple times. */
    void stop();

    /** Human-readable name (for UI). */
    String name();

    /** Whether the source successfully connected to its underlying transport. */
    boolean isConnected();

    @Override default void close() { stop(); }
}
