package com.ars.fx.data;

import com.jlog.db.DatabaseManager;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

/**
 * Named contest log databases. Each contest log is its own SQLite file under {@code ~/.j-log/contest-logs/}
 * (the original {@code ~/.j-log/contest.db} stays available as a database too). A small {@code ars_log_meta}
 * table inside each file records which contest it is for and when it was started, so the file can be moved
 * around without losing that. Archiving moves a file into {@code ~/.j-log/archive/<Contest>_<Year>/}, which
 * takes it out of the list offered when entering contest mode.
 */
public final class ContestLogs {
    private ContestLogs() {}

    /** One database file. {@code started} is a date (yyyy-MM-dd) or blank when unknown. */
    public record Info(Path path, String contestId, String contestName, String started, int qsos,
                       boolean current, boolean legacy, boolean archived) {
        public String fileName() { return path.getFileName().toString(); }
    }

    private static final String K_CURRENT = "contest.db.current", K_FOR = "contest.dbfor.";
    private static volatile boolean confirmed;

    public static Path dataDir()     { return LogDbInfo.dataDir(); }
    public static Path legacyPath()  { return dataDir().resolve("contest.db"); }
    public static Path logsDir()     { return dataDir().resolve("contest-logs"); }
    public static Path archiveRoot() { return dataDir().resolve("archive"); }

    /** The database the contest log is writing to right now. */
    public static Path current() {
        try { Path p = DatabaseManager.getInstance().getContestDbPath(); if (p != null) return p; } catch (Throwable ignored) {}
        return legacyPath();
    }

    // ── "which database this session" confirmation ──────────────────────────
    /** True once the operator has chosen (or created) a database since entering contest mode. */
    public static boolean confirmed() { return confirmed; }
    public static void resetConfirmation() { confirmed = false; }

    /** Called once when the engine comes up: reopen the database used last time, if it still exists. */
    public static void applyCurrent() {
        String c = HubConfig.get(K_CURRENT, "");
        if (c.isBlank()) return;
        try {
            Path p = Paths.get(c);
            if (Files.isRegularFile(p) && !p.toAbsolutePath().equals(current().toAbsolutePath()))
                DatabaseManager.getInstance().useContestDb(p);
        } catch (Exception e) { System.err.println("[contest] could not reopen " + c + ": " + e.getMessage()); }
    }

    /** The database last used for this contest, if it is still around and not archived. */
    public static Path lastFor(String contestId) {
        String s = HubConfig.get(K_FOR + contestId, "");
        if (s.isBlank()) return null;
        Path p = Paths.get(s);
        return Files.isRegularFile(p) ? p : null;
    }

    // ── listing ─────────────────────────────────────────────────────────────
    /** Databases offered when entering contest mode, newest first. */
    public static List<Info> usable() {
        List<Info> out = new ArrayList<>();
        Path cur = current().toAbsolutePath();
        if (Files.isRegularFile(legacyPath())) {
            Info i = inspect(legacyPath(), cur, true, false);
            if (i.qsos() > 0 || !i.contestId().isBlank()) out.add(i);   // an empty, never-named contest.db is just the idle default
        }
        for (Path p : dbFiles(logsDir())) out.add(inspect(p, cur, false, false));
        out.sort((a, b) -> b.started().compareTo(a.started()));
        return out;
    }

    public static List<Info> archived() {
        List<Info> out = new ArrayList<>();
        Path cur = current().toAbsolutePath();
        if (Files.isDirectory(archiveRoot())) {
            try (Stream<Path> dirs = Files.list(archiveRoot())) {
                for (Path d : (Iterable<Path>) dirs.filter(Files::isDirectory).sorted()::iterator)
                    for (Path p : dbFiles(d)) out.add(inspect(p, cur, false, true));
            } catch (IOException ignored) {}
        }
        out.sort((a, b) -> b.started().compareTo(a.started()));
        return out;
    }

    private static List<Path> dbFiles(Path dir) {
        List<Path> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> s = Files.list(dir)) {
            s.filter(p -> Files.isRegularFile(p) && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".db")).sorted().forEach(out::add);
        } catch (IOException ignored) {}
        return out;
    }

    /** Read a file's metadata and QSO count with a short-lived connection. */
    public static Info inspect(Path p, Path currentPath, boolean legacy, boolean archived) {
        String id = "", name = "", started = "";
        int n = 0;
        try (Connection c = Sqlite.open(p, 3000)) {
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT key, value FROM ars_log_meta")) {
                while (rs.next()) switch (rs.getString(1)) {
                    case "contest_id"   -> id = nz(rs.getString(2));
                    case "contest_name" -> name = nz(rs.getString(2));
                    case "started_utc"  -> started = nz(rs.getString(2));
                    default -> { }
                }
            } catch (Exception noMeta) { /* an older file without the meta table */ }
            try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*), MIN(datetime_utc) FROM contest_qso")) {
                if (rs.next()) { n = rs.getInt(1); if (started.isBlank()) started = nz(rs.getString(2)); }
            } catch (Exception ignored) {}
            if (id.isBlank()) {
                try (Statement st = c.createStatement();
                     ResultSet rs = st.executeQuery("SELECT contest_id FROM contest_qso GROUP BY contest_id ORDER BY COUNT(*) DESC LIMIT 1")) {
                    if (rs.next()) id = nz(rs.getString(1));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        if (name.isBlank() && !id.isBlank()) name = contestNameOf(id);
        if (started.isBlank()) {
            try { started = Files.readAttributes(p, BasicFileAttributes.class).creationTime().toInstant().toString(); } catch (Exception ignored) {}
        }
        started = started.length() >= 10 ? started.substring(0, 10) : started;
        return new Info(p, id, name, started, n, p.toAbsolutePath().equals(currentPath), legacy, archived);
    }

    private static String contestNameOf(String id) {
        try { ContestPlugin pl = PluginLoader.getInstance().getById(id); if (pl != null) return pl.getContestName(); } catch (Exception ignored) {}
        return id;
    }
    private static String nz(String s) { return s == null ? "" : s.trim(); }

    // ── switching / creating ────────────────────────────────────────────────
    /** Use an existing database for {@code contestId} and remember it. */
    public static synchronized void use(Path p, String contestId) throws Exception {
        DatabaseManager.getInstance().useContestDb(p);
        remember(p, contestId);
    }

    /** Create a new named database for this contest and switch to it. */
    public static synchronized Path create(ContestPlugin plugin, String rawName) throws Exception {
        String file = sanitizeFile(rawName);
        Path target = logsDir().resolve(file);
        if (Files.exists(target) || existsInArchive(file)) throw new IllegalStateException("A database named " + file + " already exists.");
        Files.createDirectories(logsDir());
        DatabaseManager dm = DatabaseManager.getInstance();
        dm.useContestDb(target);
        try (Statement st = dm.getContestConnection().createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS ars_log_meta (key TEXT PRIMARY KEY, value TEXT)");
        }
        putMeta(dm, "contest_id", plugin.getContestId());
        putMeta(dm, "contest_name", plugin.getContestName());
        putMeta(dm, "started_utc", Instant.now().toString());
        remember(target, plugin.getContestId());
        return target;
    }

    private static void putMeta(DatabaseManager dm, String k, String v) throws Exception {
        try (PreparedStatement ps = dm.getContestConnection().prepareStatement("INSERT OR REPLACE INTO ars_log_meta(key, value) VALUES (?, ?)")) {
            ps.setString(1, k); ps.setString(2, v); ps.executeUpdate();
        }
    }

    private static void remember(Path p, String contestId) {
        HubConfig.set(K_CURRENT, p.toAbsolutePath().toString());
        if (contestId != null && !contestId.isBlank()) HubConfig.set(K_FOR + contestId, p.toAbsolutePath().toString());
        confirmed = true;
        ContestState.recompute();
    }

    // ── archive / restore ───────────────────────────────────────────────────
    /** Move a database into {@code archive/<Contest>_<Year>/}. If it is the one in use, the engine is switched off it first. */
    public static synchronized Path archive(Path p) throws Exception {
        Info info = inspect(p, current().toAbsolutePath(), false, false);
        String year = info.started().length() >= 4 ? info.started().substring(0, 4) : String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear());
        String contest = !info.contestName().isBlank() ? info.contestName() : info.fileName().replaceAll("(?i)\\.db$", "");
        Path dir = archiveRoot().resolve(sanitizeDir(contest) + "_" + year);
        Files.createDirectories(dir);

        DatabaseManager dm = DatabaseManager.getInstance();
        boolean wasCurrent = p.toAbsolutePath().equals(current().toAbsolutePath());
        Path scratch = null;
        if (wasCurrent) { scratch = Files.createTempFile("ars-contest-", ".db"); dm.useContestDb(scratch); }   // releases p

        Path dest = uniqueIn(dir, p.getFileName().toString());
        moveWithSidecars(p, dest);

        if (wasCurrent) {
            dm.useContestDb(legacyPath());      // a valid connection must always exist; this recreates an empty contest.db if that was moved
            HubConfig.remove(K_CURRENT);
            confirmed = false;
            ContestState.recompute();
            for (String suffix : new String[]{"", "-wal", "-shm"}) try { Files.deleteIfExists(Paths.get(scratch + suffix)); } catch (Exception ignored) {}
        }
        return dest;
    }

    /** Move an archived database back into the usable list. */
    public static synchronized Path restore(Path archivedFile) throws Exception {
        Files.createDirectories(logsDir());
        Path dest = uniqueIn(logsDir(), archivedFile.getFileName().toString());
        moveWithSidecars(archivedFile, dest);
        try { Path dir = archivedFile.getParent(); if (dir != null && !dir.equals(archiveRoot())) Files.deleteIfExists(dir); } catch (Exception ignored) {}  // only if now empty
        return dest;
    }

    private static void moveWithSidecars(Path from, Path to) throws IOException {
        Files.move(from, to);
        for (String suffix : new String[]{"-wal", "-shm"}) {
            Path side = Paths.get(from + suffix);
            if (Files.exists(side)) Files.move(side, Paths.get(to + suffix), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path uniqueIn(Path dir, String fileName) {
        Path p = dir.resolve(fileName);
        String base = fileName.replaceAll("(?i)\\.db$", "");
        for (int i = 2; Files.exists(p); i++) p = dir.resolve(base + "-" + i + ".db");
        return p;
    }

    private static boolean existsInArchive(String fileName) {
        for (Info i : archived()) if (i.fileName().equalsIgnoreCase(fileName)) return true;
        return false;
    }

    // ── naming ──────────────────────────────────────────────────────────────
    /** File name used for a typed or suggested name: letters, digits, '_' and '-' only, ending in ".db". */
    public static String sanitizeFile(String raw) {
        String s = raw == null ? "" : raw.trim().replaceAll("(?i)\\.db$", "").replaceAll("[^A-Za-z0-9_\\-]", "_");
        s = s.replaceAll("^_+|_+$", "");
        if (s.isBlank()) throw new IllegalArgumentException("Give the database a name.");
        return s + ".db";
    }

    private static String sanitizeDir(String s) {
        String d = s.replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return d.isBlank() ? "Contest" : d;
    }

    private static final Set<String> FILLER = Set.of("ARRL", "THE", "OF", "AND", "CONTEST", "INTERNATIONAL");
    private static final String[] MONTHS = {"JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"};

    /** Default name for a new database for this contest, e.g. {@code NovSwpSSB2026.db}. Never collides with an existing file. */
    public static String suggestName(ContestPlugin plugin) {
        StringBuilder sb = new StringBuilder();
        for (String w : plugin.getContestName().split("[^A-Za-z0-9]+")) {
            if (w.isBlank() || FILLER.contains(w.toUpperCase(Locale.ROOT))) continue;
            sb.append(abbreviate(w));
        }
        if (sb.length() == 0) sb.append(plugin.getContestId().replace("_", ""));
        String base = sb + String.valueOf(LocalDate.now(ZoneOffset.UTC).getYear());
        String name = base + ".db";
        for (int i = 2; Files.exists(logsDir().resolve(name)) || existsInArchive(name); i++) name = base + "-" + i + ".db";
        return name;
    }

    /** "November" → Nov, "Sweepstakes" → Swp, "SSB"/"7QP"/"CW" stay as written. */
    private static String abbreviate(String w) {
        String up = w.toUpperCase(Locale.ROOT);
        for (String m : MONTHS) if (up.equals(m)) return w.substring(0, 1).toUpperCase(Locale.ROOT) + w.substring(1, 3).toLowerCase(Locale.ROOT);
        if (w.length() <= 3 || w.equals(up) || w.chars().anyMatch(Character::isDigit)) return w;   // short words, ALL-CAPS (SSB, CW, DX), 7QP
        StringBuilder r = new StringBuilder().append(Character.toUpperCase(w.charAt(0)));
        for (int i = 1; i < w.length() && r.length() < 3; i++) {
            char ch = Character.toLowerCase(w.charAt(i));
            if ("aeiou".indexOf(ch) < 0) r.append(ch);
        }
        return r.toString();
    }
}
