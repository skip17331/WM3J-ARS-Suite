package com.morsetrainer.hardware;

import com.morsetrainer.decoder.KeyEvent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

import java.util.function.Consumer;

/**
 * Bridges JavaFX keyboard events to KeyEvents. Treats key-press as DOWN and
 * key-release as UP. Filters auto-repeat (key-press fires repeatedly while
 * held on most platforms — we suppress until a release).
 */
public class KeyboardKeyer implements KeyEventSource {

    private final Scene scene;
    private final KeyCode key;
    private Consumer<KeyEvent> sink;
    private volatile boolean down = false;

    public KeyboardKeyer(Scene scene, KeyCode key) {
        this.scene = scene;
        this.key = key;
    }

    @Override
    public void start(Consumer<KeyEvent> consumer) {
        this.sink = consumer;
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == key && !down) {
                down = true;
                sink.accept(KeyEvent.down(System.currentTimeMillis()));
            }
        });
        scene.setOnKeyReleased(e -> {
            if (e.getCode() == key && down) {
                down = false;
                sink.accept(KeyEvent.up(System.currentTimeMillis()));
            }
        });
    }

    @Override
    public void stop() {
        scene.setOnKeyPressed(null);
        scene.setOnKeyReleased(null);
    }

    @Override public String name() { return "Keyboard (" + key + ")"; }
    @Override public boolean isConnected() { return true; }
}
