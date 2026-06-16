package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal read/write access to the J-Log normal-log SQLite database
 * (~/.j-log/j-log.db, table `qso`) — schema-compatible with the J-Log module
 * (j-log-engine DatabaseManager/QsoDao) so the mini-logger and the full J-Log
 * share one log. Self-contained: opens a short-lived WAL connection per call
 * (busy-timeout handles contention with a running J-Log).
 */
public final class JLogDb {
    private JLogDb() {}

    static {
        // Explicit load — JDBC service-loader auto-registration is unreliable
        // when sqlite-jdbc sits on the classpath under a module-path launch.
        try { Class.forName("org.sqlite.JDBC"); } catch (Throwable ignored) {}
    }

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String DDL = """
        CREATE TABLE IF NOT EXISTS qso (
            id            INTEGER PRIMARY KEY AUTOINCREMENT,
            callsign      TEXT NOT NULL,
            datetime_utc  TEXT NOT NULL,
            band          TEXT, mode TEXT, frequency TEXT, power_watts INTEGER,
            rst_sent      TEXT, rst_received TEXT, country TEXT, operator_name TEXT,
            state         TEXT, county TEXT, notes TEXT,
            qsl_sent      INTEGER DEFAULT 0, qsl_received INTEGER DEFAULT 0,
            sig           TEXT, sig_info TEXT,
            created_at    TEXT DEFAULT (datetime('now')))""";

    /** A logged QSO row (the columns the mini-logger displays). */
    public record Qso(long id, String utc, String callsign, String freq, String band, String mode, String rstRcvd) {}

    private static Path dbPath() { return Paths.get(System.getProperty("user.home"), ".j-log", "j-log.db"); }

    private static Connection open() throws Exception {
        Path p = dbPath();
        Files.createDirectories(p.getParent());
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + p + "?busyTimeout=5000");
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute(DDL);
        }
        return c;
    }

    /** True if the log DB can be opened (driver present, path writable). */
    public static boolean available() {
        try (Connection c = open()) { return true; } catch (Throwable t) { return false; }
    }

    /** Most recent QSOs, newest first. */
    public static List<Qso> recent(int limit) {
        List<Qso> out = new ArrayList<>();
        String sql = "SELECT id,datetime_utc,callsign,frequency,band,mode,rst_received FROM qso ORDER BY datetime_utc DESC LIMIT " + limit;
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                out.add(new Qso(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7)));
        } catch (Throwable t) { /* DB absent / driver missing → empty */ }
        return out;
    }

    /** A full QSO row for export (note: qth is stored in country, grid in sig_info). */
    public record QsoExport(String call, String datetimeUtc, String band, String mode, String freq,
                            int powerW, String rstSent, String rstRcvd, String qth, String name,
                            String state, String county, String notes, String grid) {}

    /** Every QSO with all loggable fields, oldest first — for ADIF / CSV export. */
    public static List<QsoExport> allForExport() {
        List<QsoExport> out = new ArrayList<>();
        String sql = "SELECT callsign,datetime_utc,band,mode,frequency,power_watts,rst_sent,rst_received,"
                + "country,operator_name,state,county,notes,sig_info FROM qso ORDER BY datetime_utc ASC";
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(new QsoExport(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                    rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14)));
        } catch (Throwable t) { /* DB absent → empty */ }
        return out;
    }

    /** Set of worked "CALL|BAND" keys (uppercased) — for cluster "needed" (band-fill/ATNO) filtering. */
    public static java.util.Set<String> workedCallBands() {
        java.util.Set<String> out = new java.util.HashSet<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT callsign, band FROM qso")) {
            while (rs.next()) {
                String call = rs.getString(1), band = rs.getString(2);
                if (call != null) out.add(call.toUpperCase() + "|" + (band == null ? "" : band.toUpperCase()));
            }
        } catch (Throwable t) { /* DB absent → empty (everything counts as needed) */ }
        return out;
    }

    public static int count() {
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM qso")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Throwable t) { return 0; }
    }
    /** Distinct callsigns in the log (uppercased) — for award resolution. */
    public static java.util.List<String> distinctCalls() {
        java.util.List<String> out = new java.util.ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT callsign FROM qso WHERE callsign IS NOT NULL")) {
            while (rs.next()) { String s = rs.getString(1); if (s != null && !s.isBlank()) out.add(s.toUpperCase().trim()); }
        } catch (Throwable t) { /* empty */ }
        return out;
    }
    /** Distinct worked US/CA states (the qso.state column, uppercased). */
    public static java.util.Set<String> workedStates() {
        java.util.Set<String> out = new java.util.HashSet<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT state FROM qso WHERE state IS NOT NULL AND state<>''")) {
            while (rs.next()) { String s = rs.getString(1); if (s != null && !s.isBlank()) out.add(s.toUpperCase().trim()); }
        } catch (Throwable t) { /* empty */ }
        return out;
    }

    /** QSOs logged so far today (UTC). */
    public static int countToday() {
        String today = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM qso WHERE datetime_utc LIKE ?")) {
            ps.setString(1, today + "%");
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (Throwable t) { return 0; }
    }

    /** Insert a casual QSO. Returns true on success. Maps fields to the j-log
     *  qso schema: qth→country, name→operator_name, grid→sig_info, comment→notes. */
    public static boolean insert(String call, String freq, String band, String mode,
                                 String rstSent, String rstRcvd, String name, String qth,
                                 String grid, String comment) {
        return insert(call, freq, band, mode, rstSent, rstRcvd, name, qth, grid, comment, null, null, 0);
    }
    /** Full insert with state / county / power (qso table columns). */
    public static boolean insert(String call, String freq, String band, String mode,
                                 String rstSent, String rstRcvd, String name, String qth,
                                 String grid, String comment, String state, String county, int powerW) {
        if (call == null || call.isBlank()) return false;
        String sql = "INSERT INTO qso(callsign,datetime_utc,band,mode,frequency,power_watts,"
                + "rst_sent,rst_received,country,operator_name,state,county,notes,"
                + "qsl_sent,qsl_received,sig,sig_info) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.toUpperCase().trim());
            ps.setString(2, LocalDateTime.now(ZoneOffset.UTC).format(FMT));
            ps.setString(3, nz(band)); ps.setString(4, nz(mode)); ps.setString(5, nz(freq));
            ps.setInt(6, Math.max(0, powerW));
            ps.setString(7, nz(rstSent)); ps.setString(8, nz(rstRcvd));
            ps.setString(9, nz(qth)); ps.setString(10, nz(name)); ps.setString(11, nz(state)); ps.setString(12, nz(county));
            ps.setString(13, nz(comment)); ps.setInt(14, 0); ps.setInt(15, 0);
            ps.setString(16, null); ps.setString(17, nz(grid));
            ps.executeUpdate();
            return true;
        } catch (Throwable t) { t.printStackTrace(); return false; }
    }

    private static String nz(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    public static String hhmm(String dt) { return (dt != null && dt.length() >= 16) ? dt.substring(11, 16) : "—"; }

    /** Best-effort amateur band from a frequency in MHz (e.g. "14.074" → "20m"). */
    public static String bandFor(String freqMhz) {
        if (freqMhz == null) return null;
        double f;
        try { f = Double.parseDouble(freqMhz.replace(",", "").trim()); } catch (Exception e) { return null; }
        if (f >= 1.8 && f <= 2.0) return "160m";
        if (f >= 3.5 && f <= 4.0) return "80m";
        if (f >= 5.3 && f <= 5.45) return "60m";
        if (f >= 7.0 && f <= 7.3) return "40m";
        if (f >= 10.1 && f <= 10.15) return "30m";
        if (f >= 14.0 && f <= 14.35) return "20m";
        if (f >= 18.0 && f <= 18.2) return "17m";
        if (f >= 21.0 && f <= 21.45) return "15m";
        if (f >= 24.8 && f <= 25.0) return "12m";
        if (f >= 28.0 && f <= 29.7) return "10m";
        if (f >= 50.0 && f <= 54.0) return "6m";
        if (f >= 144.0 && f <= 148.0) return "2m";
        if (f >= 222.0 && f <= 225.0) return "1.25m";
        if (f >= 420.0 && f <= 450.0) return "70cm";
        return null;
    }
}
