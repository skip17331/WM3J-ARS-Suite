package com.ars.fx;

/**
 * Launch entry point for the packaged (shaded) jar. Deliberately NOT an
 * {@link javafx.application.Application} subclass: when JavaFX is on the plain
 * classpath of a fat jar, a main class that extends Application trips the
 * "JavaFX runtime components are missing" guard. Bootstrapping through a normal
 * class that simply calls {@link Launcher#main} sidesteps that, so
 * {@code java -jar ars-fx-*.jar} works on the Pi without a module path.
 */
public final class Main {
    private Main() {}
    public static void main(String[] args) { Launcher.main(args); }
}
