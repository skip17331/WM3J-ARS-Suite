package com.hamradio.jhub;

import java.util.Locale;

/**
 * Single source of truth for runtime OS detection.
 *
 * <p>{@code os.name} is fixed for the life of the JVM, so it is read once and
 * cached. Every place that decides between Windows and *nix launch mechanics
 * (e.g. {@code cmd /c} vs {@code bash -c}, {@code .bat} vs {@code .sh}) must
 * call these methods instead of re-inlining the
 * {@code System.getProperty("os.name")...contains("win")} idiom — a missed
 * copy of that idiom is exactly how a new launch path (morse-trainer) once
 * shipped Linux-only on Windows.
 */
public final class Platform {

    private static final String OS =
        System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final boolean WINDOWS = OS.contains("win");
    private static final boolean MAC     = OS.contains("mac") || OS.contains("darwin");

    private Platform() {}

    /** True when running on any Windows. */
    public static boolean isWindows() { return WINDOWS; }

    /** True when running on macOS. */
    public static boolean isMac() { return MAC; }
}
