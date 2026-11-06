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
    public record Qso(long id, String utc, String callsign, String freq, String band, String mode, String rstRcvd, String name) {}

    /** Every loggable field for one QSO (for editing). qth=country, grid=sig_info, name=operator_name. */
    public record QsoFull(long id, String datetimeUtc, String call, String freq, String band, String mode,
                          String rstSent, String rstRcvd, String name, String qth, String grid,
                          String state, String county, int powerW, String notes, Refs refs) {}

    /** What award tracking needs beyond the basic QSO: island / summit / park references, zones, and which
     *  confirmations (paper QSL, LoTW, eQSL) have come back. {@link #NONE} = nothing recorded. */
    public record Refs(String iota, String sota, String pota, int cqz, int ituz, boolean qsl, boolean lotw, boolean eqsl) {
        public static final Refs NONE = new Refs("", "", "", 0, 0, false, false, false);
        public boolean confirmed() { return qsl || lotw || eqsl; }
        /** Same refs with the confirmations of {@code o} added (never takes one away). */
        public Refs mergedWith(Refs o) {
            return new Refs(blank(iota) ? o.iota : iota, blank(sota) ? o.sota : sota, blank(pota) ? o.pota : pota,
                    cqz > 0 ? cqz : o.cqz, ituz > 0 ? ituz : o.ituz, qsl || o.qsl, lotw || o.lotw, eqsl || o.eqsl);
        }
        private static boolean blank(String s) { return s == null || s.isBlank(); }
    }

    private static final String REF_COLS = "iota,sota_ref,pota_ref,cqz,ituz,qsl_received,lotw_rcvd,eqsl_rcvd";
    private static Refs refsAt(ResultSet rs, int from) throws SQLException {
        return new Refs(nvl(rs.getString(from)), nvl(rs.getString(from + 1)), nvl(rs.getString(from + 2)), rs.getInt(from + 3),
                rs.getInt(from + 4), rs.getInt(from + 5) != 0, rs.getInt(from + 6) != 0, rs.getInt(from + 7) != 0);
    }
    private static String nvl(String s) { return s == null ? "" : s; }

    /** Prior-QSO summary for the worked-before / dupe indicator. */
    public record Prior(int count, String lastDate, java.util.List<String> bands) {}

    /** The normal-log database file (shared with the full J-Log). */
    public static Path path() { return dbPath(); }
    private static Path dbPath() { return Paths.get(System.getProperty("user.home"), ".j-log", "j-log.db"); }

    private static boolean adopted;

    private static Connection open() throws Exception {
        Path p = dbPath();
        Files.createDirectories(p.getParent());
        if (!adopted) { adopted = true; Sqlite.adoptLegacy(p, "?busyTimeout=5000", "qso"); }   // data an earlier build put in a wrongly named file
        Connection c = Sqlite.open(p, 5000);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute(DDL);
            addColumns(st);
        }
        return c;
    }

    /** Award-tracking columns added after the first release; additive, so an older J-Log opening the file is unaffected. */
    private static final String[][] EXTRA_COLUMNS = {
        {"iota", "TEXT"}, {"sota_ref", "TEXT"}, {"pota_ref", "TEXT"},
        {"cqz", "INTEGER DEFAULT 0"}, {"ituz", "INTEGER DEFAULT 0"},
        {"lotw_rcvd", "INTEGER DEFAULT 0"}, {"eqsl_rcvd", "INTEGER DEFAULT 0"}};

    private static void addColumns(Statement st) throws SQLException {
        java.util.Set<String> have = new java.util.HashSet<>();
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(qso)")) { while (rs.next()) have.add(rs.getString("name").toLowerCase()); }
        for (String[] col : EXTRA_COLUMNS)
            if (!have.contains(col[0])) st.executeUpdate("ALTER TABLE qso ADD COLUMN " + col[0] + " " + col[1]);
    }

    /** True if the log DB can be opened (driver present, path writable). */
    public static boolean available() {
        try (Connection c = open()) { return true; } catch (Throwable t) { return false; }
    }

    /** Most recent QSOs, newest first. */
    public static List<Qso> recent(int limit) {
        List<Qso> out = new ArrayList<>();
        String sql = "SELECT id,datetime_utc,callsign,frequency,band,mode,rst_received,operator_name FROM qso ORDER BY datetime_utc DESC LIMIT " + limit;
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                out.add(new Qso(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)));
        } catch (Throwable t) { /* DB absent / driver missing → empty */ }
        return out;
    }

    /** Every QSO with all loggable fields, newest first — for the log browser. */
    public static List<QsoFull> all() {
        List<QsoFull> out = new ArrayList<>();
        String sql = "SELECT id,datetime_utc,callsign,frequency,band,mode,rst_sent,rst_received,operator_name,"
                + "country,sig_info,state,county,power_watts,notes," + REF_COLS + " FROM qso ORDER BY datetime_utc DESC, id DESC";
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next())
                out.add(new QsoFull(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getInt(14), rs.getString(15), refsAt(rs, 16)));
        } catch (Throwable t) { /* DB absent → empty */ }
        return out;
    }

    /** Full QSO by id (for the edit form), or null. */
    public static QsoFull byId(long id) {
        String sql = "SELECT id,datetime_utc,callsign,frequency,band,mode,rst_sent,rst_received,operator_name,"
                + "country,sig_info,state,county,power_watts,notes," + REF_COLS + " FROM qso WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new QsoFull(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getString(10), rs.getString(11), rs.getString(12), rs.getString(13), rs.getInt(14), rs.getString(15), refsAt(rs, 16));
            }
        } catch (Throwable t) { /* not found */ }
        return null;
    }

    /** The newest row's id (the QSO just inserted, in a single-operator log), or 0. */
    public static long maxId() {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("SELECT MAX(id) FROM qso"); java.sql.ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        } catch (Throwable t) { return 0; }
    }

    /** Delete a logged QSO. */
    public static boolean delete(long id) {
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement("DELETE FROM qso WHERE id=?")) {
            ps.setLong(1, id); return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }

    /** Update an existing QSO (same field/column mapping as insert). */
    public static boolean update(long id, String datetimeUtc, String call, String freq, String band, String mode,
                                 String rstSent, String rstRcvd, String name, String qth, String grid,
                                 String comment, String state, String county, int powerW) {
        QsoFull cur = byId(id);
        return update(id, datetimeUtc, call, freq, band, mode, rstSent, rstRcvd, name, qth, grid, comment, state, county, powerW,
                cur == null ? Refs.NONE : cur.refs());   // keeps the references and confirmations the caller does not know about
    }
    public static boolean update(long id, String datetimeUtc, String call, String freq, String band, String mode,
                                 String rstSent, String rstRcvd, String name, String qth, String grid,
                                 String comment, String state, String county, int powerW, Refs r) {
        if (call == null || call.isBlank()) return false;
        if (r == null) r = Refs.NONE;
        String sql = "UPDATE qso SET callsign=?,datetime_utc=?,band=?,mode=?,frequency=?,power_watts=?,"
                + "rst_sent=?,rst_received=?,country=?,operator_name=?,state=?,county=?,notes=?,sig_info=?,"
                + "iota=?,sota_ref=?,pota_ref=?,cqz=?,ituz=?,qsl_received=?,lotw_rcvd=?,eqsl_rcvd=? WHERE id=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.toUpperCase().trim());
            ps.setString(2, (datetimeUtc == null || datetimeUtc.isBlank()) ? LocalDateTime.now(ZoneOffset.UTC).format(FMT) : datetimeUtc.trim());
            ps.setString(3, nz(band)); ps.setString(4, nz(mode)); ps.setString(5, nz(freq));
            ps.setInt(6, Math.max(0, powerW));
            ps.setString(7, nz(rstSent)); ps.setString(8, nz(rstRcvd));
            ps.setString(9, nz(qth)); ps.setString(10, nz(name)); ps.setString(11, nz(state)); ps.setString(12, nz(county));
            ps.setString(13, nz(comment)); ps.setString(14, nz(grid));
            ps.setString(15, refNorm(r.iota())); ps.setString(16, refNorm(r.sota())); ps.setString(17, refNorm(r.pota()));
            ps.setInt(18, Math.max(0, r.cqz())); ps.setInt(19, Math.max(0, r.ituz()));
            ps.setInt(20, r.qsl() ? 1 : 0); ps.setInt(21, r.lotw() ? 1 : 0); ps.setInt(22, r.eqsl() ? 1 : 0);
            ps.setLong(23, id);
            return ps.executeUpdate() > 0;
        } catch (Throwable t) { return false; }
    }

    /** Prior QSOs with a call: count, most-recent date, and the bands worked. */
    public static Prior priorWith(String call) {
        if (call == null || call.isBlank()) return new Prior(0, null, java.util.List.of());
        String c = call.trim().toUpperCase();
        int count = 0; String last = null; java.util.LinkedHashSet<String> bands = new java.util.LinkedHashSet<>();
        String sql = "SELECT band,datetime_utc FROM qso WHERE UPPER(callsign)=? ORDER BY datetime_utc DESC";
        try (Connection conn = open(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    count++;
                    String dt = rs.getString(2); if (last == null && dt != null && dt.length() >= 10) last = dt.substring(0, 10);
                    String band = rs.getString(1); if (band != null && !band.isBlank()) bands.add(band);
                }
            }
        } catch (Throwable t) { /* none */ }
        return new Prior(count, last, new ArrayList<>(bands));
    }
    public static String dateOf(String dt) { return (dt != null && dt.length() >= 10) ? dt.substring(0, 10) : "—"; }

    /** A full QSO row for export (note: qth is stored in country, grid in sig_info). */
    public record QsoExport(String call, String datetimeUtc, String band, String mode, String freq,
                            int powerW, String rstSent, String rstRcvd, String qth, String name,
                            String state, String county, String notes, String grid, Refs refs) {}

    /** Every QSO with all loggable fields, oldest first — for ADIF / CSV export. */
    public static List<QsoExport> allForExport() {
        List<QsoExport> out = new ArrayList<>();
        String sql = "SELECT callsign,datetime_utc,band,mode,frequency,power_watts,rst_sent,rst_received,"
                + "country,operator_name,state,county,notes,sig_info," + REF_COLS + " FROM qso ORDER BY datetime_utc ASC";
        try (Connection c = open(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(new QsoExport(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                    rs.getString(5), rs.getInt(6), rs.getString(7), rs.getString(8), rs.getString(9), rs.getString(10),
                    rs.getString(11), rs.getString(12), rs.getString(13), rs.getString(14), refsAt(rs, 15)));
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

    /** Every distinct (call, band, mode) already in the log, for the cluster's new-entity / band / mode flags. */
    public static java.util.List<SpotNeeds.Slot> workedSlots() {
        java.util.List<SpotNeeds.Slot> out = new java.util.ArrayList<>();
        try (Connection c = open(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT DISTINCT callsign, band, mode FROM qso")) {
            while (rs.next()) out.add(new SpotNeeds.Slot(rs.getString(1), rs.getString(2), rs.getString(3)));
        } catch (Throwable t) { /* DB absent: nothing worked yet */ }
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
    /** Full insert with state / county / power (qso table columns), timestamped now (UTC). */
    public static boolean insert(String call, String freq, String band, String mode,
                                 String rstSent, String rstRcvd, String name, String qth,
                                 String grid, String comment, String state, String county, int powerW) {
        return insertAt(null, call, freq, band, mode, rstSent, rstRcvd, name, qth, grid, comment, state, county, powerW);
    }
    /** Full insert with an explicit UTC datetime ("yyyy-MM-dd HH:mm:ss"); null/blank → now. */
    public static boolean insertAt(String datetimeUtc, String call, String freq, String band, String mode,
                                   String rstSent, String rstRcvd, String name, String qth,
                                   String grid, String comment, String state, String county, int powerW) {
        return insertAt(datetimeUtc, call, freq, band, mode, rstSent, rstRcvd, name, qth, grid, comment, state, county, powerW, Refs.NONE);
    }
    /** Full insert including island / summit / park references, zones and confirmations. */
    public static boolean insertAt(String datetimeUtc, String call, String freq, String band, String mode,
                                   String rstSent, String rstRcvd, String name, String qth,
                                   String grid, String comment, String state, String county, int powerW, Refs r) {
        if (call == null || call.isBlank()) return false;
        if (r == null) r = Refs.NONE;
        String sql = "INSERT INTO qso(callsign,datetime_utc,band,mode,frequency,power_watts,"
                + "rst_sent,rst_received,country,operator_name,state,county,notes,"
                + "qsl_sent,qsl_received,sig,sig_info,iota,sota_ref,pota_ref,cqz,ituz,lotw_rcvd,eqsl_rcvd) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.toUpperCase().trim());
            ps.setString(2, (datetimeUtc == null || datetimeUtc.isBlank()) ? LocalDateTime.now(ZoneOffset.UTC).format(FMT) : datetimeUtc.trim());
            ps.setString(3, nz(band)); ps.setString(4, nz(mode)); ps.setString(5, nz(freq));
            ps.setInt(6, Math.max(0, powerW));
            ps.setString(7, nz(rstSent)); ps.setString(8, nz(rstRcvd));
            ps.setString(9, nz(qth)); ps.setString(10, nz(name)); ps.setString(11, nz(state)); ps.setString(12, nz(county));
            ps.setString(13, nz(comment)); ps.setInt(14, 0); ps.setInt(15, r.qsl() ? 1 : 0);
            ps.setString(16, null); ps.setString(17, nz(grid));
            ps.setString(18, refNorm(r.iota())); ps.setString(19, refNorm(r.sota())); ps.setString(20, refNorm(r.pota()));
            ps.setInt(21, Math.max(0, r.cqz())); ps.setInt(22, Math.max(0, r.ituz()));
            ps.setInt(23, r.lotw() ? 1 : 0); ps.setInt(24, r.eqsl() ? 1 : 0);
            ps.executeUpdate();
            return true;
        } catch (Throwable t) { t.printStackTrace(); return false; }
    }

    /** Reference text as stored: trimmed and upper-case, null when empty. */
    static String refNorm(String s) { return (s == null || s.isBlank()) ? null : s.trim().toUpperCase(java.util.Locale.ROOT); }

    /** Adds confirmations and references from an imported record to the QSO already in the log (same call, band, mode and
     *  minute). Never removes anything. Returns true when a matching QSO was found. */
    public static boolean mergeRefs(String call, String datetimeUtc, String band, String mode, Refs add) {
        if (call == null || datetimeUtc == null || add == null) return false;
        String minute = datetimeUtc.substring(0, Math.min(16, datetimeUtc.length()));
        String sql = "SELECT id FROM qso WHERE UPPER(callsign)=? AND substr(datetime_utc,1,16)=? AND band IS ? AND UPPER(IFNULL(mode,''))=?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.trim().toUpperCase(java.util.Locale.ROOT)); ps.setString(2, minute); ps.setString(3, nz(band));
            ps.setString(4, mode == null ? "" : mode.trim().toUpperCase(java.util.Locale.ROOT));
            long id = -1;
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) id = rs.getLong(1); }
            if (id < 0) return false;
            return mergeInto(id, add);
        } catch (Throwable t) { return false; }
    }

    /** Like {@link #mergeRefs} but ignoring the mode (a confirmation says SSB where the log says USB): used only when the QSO
     *  at that call, minute and band is unique. */
    public static boolean mergeRefsAnyMode(String call, String datetimeUtc, String band, Refs add) {
        if (call == null || datetimeUtc == null || add == null) return false;
        String minute = datetimeUtc.substring(0, Math.min(16, datetimeUtc.length()));
        String sql = "SELECT id FROM qso WHERE UPPER(callsign)=? AND substr(datetime_utc,1,16)=? AND band IS ?";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.trim().toUpperCase(java.util.Locale.ROOT)); ps.setString(2, minute); ps.setString(3, nz(band));
            long id = -1; int n = 0;
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) { id = rs.getLong(1); n++; } }
            return n == 1 && mergeInto(id, add);
        } catch (Throwable t) { return false; }
    }

    private static boolean mergeInto(long id, Refs add) {
        QsoFull cur = byId(id);
        if (cur == null) return false;
        Refs m = cur.refs().mergedWith(add);
        if (m.equals(cur.refs())) return true;
        return update(id, cur.datetimeUtc(), cur.call(), cur.freq(), cur.band(), cur.mode(), cur.rstSent(), cur.rstRcvd(),
                cur.name(), cur.qth(), cur.grid(), cur.notes(), cur.state(), cur.county(), cur.powerW(), m);
    }

    /** True when a QSO with this callsign, band and mode is already logged within 3 minutes of {@code datetimeUtc}
     *  ("yyyy-MM-dd HH:mm:ss"): used so a QSO WSJT-X reports twice is not logged twice. */
    public static boolean hasQso(String call, String band, String mode, String datetimeUtc) {
        String sql = "SELECT COUNT(*) FROM qso WHERE callsign=? AND band IS ? AND mode IS ? AND "
                + "ABS(strftime('%s', datetime_utc) - strftime('%s', ?)) <= 180";
        try (Connection c = open(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, call.toUpperCase().trim()); ps.setString(2, nz(band)); ps.setString(3, nz(mode)); ps.setString(4, datetimeUtc);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; }
        } catch (Throwable t) { return false; }
    }

    private static String nz(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    public static String hhmm(String dt) { return (dt != null && dt.length() >= 16) ? dt.substring(11, 16) : "—"; }

    /** Amateur band from a frequency in MHz (e.g. "14.074" → "20m"), from the shared band plan; null if unreadable or out of band. */
    public static String bandFor(String freqMhz) { return Bands.ofMhz(freqMhz); }
}
