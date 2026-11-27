package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Which database file and table J-Log is writing to, in plain terms, so the UI can show it.
 * The log database ({@code j-log.db}, table {@code qso}).
 */
public final class LogDbInfo {
    private LogDbInfo() {}

    public static final String NORMAL_TABLE = "qso";

    public static Path dataDir()     { return Paths.get(System.getProperty("user.home"), ".j-log"); }
    public static Path normalPath()  { return JLogDb.path(); }

    /** Path with the home directory shown as {@code ~}. */
    public static String pretty(Path p) {
        String s = p.toString(), home = System.getProperty("user.home");
        return home != null && s.startsWith(home) ? "~" + s.substring(home.length()) : s;
    }

    /** "12.4 KB" style size of a file, or "not created yet". */
    public static String size(Path p) {
        try {
            if (!Files.exists(p)) return "not created yet";
            long b = Files.size(p);
            return b < 1024 ? b + " B" : b < 1024 * 1024 ? String.format("%.1f KB", b / 1024.0) : String.format("%.1f MB", b / 1048576.0);
        } catch (Exception e) { return "?"; }
    }

    /** One line for the normal log, e.g. "~/.j-log/j-log.db · table qso". */
    public static String normalLine()  { return pretty(normalPath()) + " · table " + NORMAL_TABLE; }

    /** Open a folder in the desktop file manager (best effort). */
    public static boolean openFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            LinkOpener.openFile(dir, null);   // off the UI thread: AWT Desktop can hang JavaFX on Linux
            return true;
        } catch (Throwable t) { return false; }
    }
}
