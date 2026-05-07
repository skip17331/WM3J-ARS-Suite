package com.morsetrainer.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Minimal stdout logger with levels. Avoids dragging in SLF4J for a desktop app. */
public final class Logger {

    public enum Level { TRACE, DEBUG, INFO, WARN, ERROR }
    private static volatile Level minLevel = Level.INFO;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private Logger() {}

    public static void setLevel(Level l) { minLevel = l; }

    public static void trace(String fmt, Object... args) { log(Level.TRACE, fmt, args); }
    public static void debug(String fmt, Object... args) { log(Level.DEBUG, fmt, args); }
    public static void info (String fmt, Object... args) { log(Level.INFO,  fmt, args); }
    public static void warn (String fmt, Object... args) { log(Level.WARN,  fmt, args); }
    public static void error(String fmt, Object... args) { log(Level.ERROR, fmt, args); }

    private static void log(Level l, String fmt, Object... args) {
        if (l.ordinal() < minLevel.ordinal()) return;
        String msg = (args == null || args.length == 0) ? fmt : String.format(fmt, args);
        System.out.printf("[%s] %-5s %s%n", LocalDateTime.now().format(TS), l, msg);
    }
}
