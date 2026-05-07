package com.hamradio.jhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UploadStateDao — reads {@code ~/.j-log/j-log.db} and tracks which QSOs
 * have already been pushed to each upload service.
 *
 * <p>Why here and not in j-log-engine: the upload services live in
 * J-Hub (so they keep running even when j-log isn't open), so the
 * upload-tracking table needs to be reachable from J-Hub. We share
 * the j-log SQLite file but write our tracking rows to a separate
 * <code>upload_state</code> table that j-log doesn't touch.
 *
 * <h3>Schema</h3>
 * <pre>
 *   CREATE TABLE IF NOT EXISTS upload_state (
 *     qso_id      INTEGER NOT NULL,
 *     service_id  TEXT    NOT NULL,
 *     uploaded_at TEXT    NOT NULL,
 *     status      TEXT    NOT NULL DEFAULT 'OK',
 *     PRIMARY KEY (qso_id, service_id)
 *   );
 * </pre>
 */
public class UploadStateDao {

    private static final Logger log = LoggerFactory.getLogger(UploadStateDao.class);

    private static final UploadStateDao INSTANCE = new UploadStateDao();
    public static UploadStateDao getInstance() { return INSTANCE; }
    private UploadStateDao() {}

    private static Path jlogDb() {
        return Path.of(System.getProperty("user.home"), ".j-log", "j-log.db");
    }

    private Connection open() throws SQLException {
        Path db = jlogDb();
        if (!Files.exists(db)) {
            throw new SQLException("j-log database not found at " + db
                + " — run j-log at least once before uploading");
        }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
        try (Statement st = c.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS upload_state ("
              + "  qso_id      INTEGER NOT NULL,"
              + "  service_id  TEXT    NOT NULL,"
              + "  uploaded_at TEXT    NOT NULL,"
              + "  status      TEXT    NOT NULL DEFAULT 'OK',"
              + "  PRIMARY KEY (qso_id, service_id)"
              + ")");
        }
        return c;
    }

    /** Returns every QSO that has not been confirmed for the given service. */
    public List<LogUploader.PendingQso> pendingFor(String serviceId) {
        List<LogUploader.PendingQso> out = new ArrayList<>();
        try (Connection c = open()) {
            Set<Long> confirmed = new HashSet<>();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT qso_id FROM upload_state WHERE service_id = ?")) {
                ps.setString(1, serviceId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) confirmed.add(rs.getLong(1));
                }
            }
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT id, callsign, datetime_utc, band, mode, frequency, " +
                     "       rst_sent, rst_received, country, operator_name, state, " +
                     "       county, notes, sig, sig_info FROM qso ORDER BY id")) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    if (confirmed.contains(id)) continue;
                    out.add(new LogUploader.PendingQso(id, toAdif(rs)));
                }
            }
        } catch (SQLException e) {
            log.warn("pendingFor({}) failed: {}", serviceId, e.getMessage());
        }
        return out;
    }

    /** Inserts confirmation rows so these QSOs are skipped next pass. */
    public void markConfirmed(String serviceId, List<Long> qsoIds) {
        if (qsoIds == null || qsoIds.isEmpty()) return;
        String now = java.time.Instant.now().toString();
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT OR REPLACE INTO upload_state (qso_id, service_id, uploaded_at, status) " +
                 "VALUES (?, ?, ?, 'OK')")) {
            c.setAutoCommit(false);
            for (Long id : qsoIds) {
                ps.setLong(1, id);
                ps.setString(2, serviceId);
                ps.setString(3, now);
                ps.addBatch();
            }
            ps.executeBatch();
            c.commit();
        } catch (SQLException e) {
            log.warn("markConfirmed({}, {} rows) failed: {}",
                serviceId, qsoIds.size(), e.getMessage());
        }
    }

    /** Total QSO count in the j-log database. Used by /api/uploaders. */
    public int totalQsos() {
        try (Connection c = open();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM qso")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return -1;
        }
    }

    /** How many QSOs have been confirmed uploaded to the given service. */
    public int uploadedFor(String serviceId) {
        try (Connection c = open();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT COUNT(*) FROM upload_state WHERE service_id = ? AND status = 'OK'")) {
            ps.setString(1, serviceId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    // -----------------------------------------------------------
    // Inline ADIF formatter — j-hub doesn't depend on j-log-engine
    // -----------------------------------------------------------

    private static String toAdif(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder(256);
        appendField(sb, "CALL",         rs.getString("callsign"));
        // datetime_utc is "YYYY-MM-DDTHH:MM:SSZ" (ISO-8601 from j-log)
        String dt = rs.getString("datetime_utc");
        if (dt != null && dt.length() >= 16) {
            String date = dt.substring(0, 10).replace("-", "");
            String time = dt.length() >= 19 ? dt.substring(11, 19).replace(":", "") : "000000";
            appendField(sb, "QSO_DATE", date);
            appendField(sb, "TIME_ON",  time);
        }
        appendField(sb, "BAND",         rs.getString("band"));
        appendField(sb, "MODE",         normalizeMode(rs.getString("mode")));
        appendField(sb, "FREQ",         rs.getString("frequency"));
        appendField(sb, "RST_SENT",     rs.getString("rst_sent"));
        appendField(sb, "RST_RCVD",     rs.getString("rst_received"));
        appendField(sb, "COUNTRY",      rs.getString("country"));
        appendField(sb, "NAME",         rs.getString("operator_name"));
        appendField(sb, "STATE",        rs.getString("state"));
        appendField(sb, "CNTY",         rs.getString("county"));
        appendField(sb, "COMMENT",      rs.getString("notes"));
        appendField(sb, "SIG",          rs.getString("sig"));
        appendField(sb, "SIG_INFO",     rs.getString("sig_info"));
        // Station fields pulled from j-hub config so uploads don't depend
        // on j-log being open.
        var st = ConfigManager.getInstance().getStation();
        appendField(sb, "STATION_CALLSIGN", st == null ? null : st.callsign);
        appendField(sb, "MY_GRIDSQUARE",    st == null ? null : st.gridSquare);
        sb.append("<EOR>\n");
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, String name, String value) {
        if (value == null || value.isBlank()) return;
        sb.append('<').append(name).append(':').append(value.length()).append('>')
          .append(value).append(' ');
    }

    private static String normalizeMode(String m) {
        if (m == null) return null;
        String u = m.toUpperCase();
        if ("PHONE".equals(u) || "USB".equals(u) || "LSB".equals(u)) return "SSB";
        if ("DIGITAL".equals(u)) return "DIGITALVOICE";
        return u;
    }
}
