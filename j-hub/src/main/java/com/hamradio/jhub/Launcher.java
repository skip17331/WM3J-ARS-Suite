package com.hamradio.jhub;

/**
 * Plain entrypoint that delegates to the JavaFX Application. Having a separate
 * non-Application class as the Main-Class makes the shaded jar runnable
 * without --module-path tricks (the JVM launcher otherwise demands
 * javafx.graphics as a resolved named module).
 */
public final class Launcher {
    public static void main(String[] args) {
        JHubMain.main(args);
    }
}
