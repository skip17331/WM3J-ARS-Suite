package com.ars.fx.data;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;

/** Tiny append-only trace of callsign lookups and spot clicks, at {@code ~/.j-log/lookup-debug.log}. Never logs credentials. */
public final class DebugLog {
    private DebugLog() {}
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".j-log", "lookup-debug.log");

    public static synchronized void log(String msg) {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.exists(FILE) && Files.size(FILE) > 200_000) Files.delete(FILE);   // keep it small
            Files.writeString(FILE, LocalTime.now().withNano(0) + " " + msg + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) { /* diagnostics only */ }
    }
}
