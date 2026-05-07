package com.morsetrainer;

/**
 * Plain entrypoint that delegates to the JavaFX Application. Having a separate
 * non-Application class as the Main-Class makes the shaded jar runnable
 * without --module-path tricks.
 */
public final class Main {
    public static void main(String[] args) {
        com.morsetrainer.ui.MorseTrainerApp.main(args);
    }
}
