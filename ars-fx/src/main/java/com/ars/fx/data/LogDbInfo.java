package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Which database file and table J-Log is writing to, in plain terms, so the UI can show it.
 * Two stores: the normal log ({@code j-log.db}, table {@code qso}) and the contest log
 * ({@code contest.db}, table {@code contest_qso}, rows keyed by contest id).
 */
public final class LogDbInfo {
    private LogDbInfo() {}

    public static final String NORMAL_TABLE = "qso";
    public static final String CONTEST_TABLE = "contest_qso";

    public static Path dataDir()     { return Paths.get(System.getProperty("user.home"), ".j-log"); }
    public static Path normalPath()  { return JLogDb.path(); }
    public static Path contestPath() { return dataDir().resolve("contest.db"); }

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

    /** One line for the contest log, naming the active contest when there is one. */
    public static String contestLine() {
        ContestState.ensureLoaded();
        var p = ContestState.plugin();
        return pretty(contestPath()) + " · table " + CONTEST_TABLE + (p == null ? " · no contest selected" : " · " + p.getContestName());
    }

    /** Open a folder in the desktop file manager (best effort). */
    public static boolean openFolder(Path dir) {
        try {
            Files.createDirectories(dir);
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(dir.toFile()); return true;
            }
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] cmd = os.contains("win") ? new String[]{"explorer", dir.toString()}
                         : os.contains("mac") ? new String[]{"open", dir.toString()}
                         : new String[]{"xdg-open", dir.toString()};
            new ProcessBuilder(cmd).start(); return true;
        } catch (Throwable t) { return false; }
    }
}
