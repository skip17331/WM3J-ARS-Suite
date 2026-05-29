package com.jlog.db;

import com.jlog.model.QsoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for the normal QSO log (j-log.db → qso table).
 */
public class QsoDao {

    private static final Logger log = LoggerFactory.getLogger(QsoDao.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final QsoDao INSTANCE = new QsoDao();

    public static QsoDao getInstance() { return INSTANCE; }

    private Connection conn() {
        return DatabaseManager.getInstance().getLogConnection();
    }

    /** Insert a new QSO, returns auto-generated id. */
    public long insert(QsoRecord qso) throws SQLException {
        String sql = """
            INSERT INTO qso(callsign,datetime_utc,band,mode,frequency,power_watts,
                rst_sent,rst_received,country,operator_name,state,county,notes,
                qsl_sent,qsl_received,sig,sig_info)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  qso.getCallsign());
            ps.setString(2,  qso.getDateTimeUtc() != null ? qso.getDateTimeUtc().format(FMT) : FMT.format(LocalDateTime.now()));
            ps.setString(3,  qso.getBand());
            ps.setString(4,  qso.getMode());
            ps.setString(5,  qso.getFrequency());
            ps.setInt   (6,  qso.getPowerWatts());
            ps.setString(7,  qso.getRstSent());
            ps.setString(8,  qso.getRstReceived());
            ps.setString(9,  qso.getCountry());
            ps.setString(10, qso.getOperatorName());
            ps.setString(11, qso.getState());
            ps.setString(12, qso.getCounty());
            ps.setString(13, qso.getNotes());
            ps.setInt   (14, qso.isQslSent()     ? 1 : 0);
            ps.setInt   (15, qso.isQslReceived()  ? 1 : 0);
            ps.setString(16, qso.getSig());
            ps.setString(17, qso.getSigInfo());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) {
                long id = gk.getLong(1);
                qso.setId(id);
                log.debug("Inserted QSO id={} call={}", id, qso.getCallsign());
                return id;
            }
        }
        return -1;
    }

    /** Update an existing QSO. */
    public void update(QsoRecord qso) throws SQLException {
        String sql = """
            UPDATE qso SET callsign=?,datetime_utc=?,band=?,mode=?,frequency=?,
                power_watts=?,rst_sent=?,rst_received=?,country=?,operator_name=?,
                state=?,county=?,notes=?,qsl_sent=?,qsl_received=?,sig=?,sig_info=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,  qso.getCallsign());
            ps.setString(2,  qso.getDateTimeUtc().format(FMT));
            ps.setString(3,  qso.getBand());
            ps.setString(4,  qso.getMode());
            ps.setString(5,  qso.getFrequency());
            ps.setInt   (6,  qso.getPowerWatts());
            ps.setString(7,  qso.getRstSent());
            ps.setString(8,  qso.getRstReceived());
            ps.setString(9,  qso.getCountry());
            ps.setString(10, qso.getOperatorName());
            ps.setString(11, qso.getState());
            ps.setString(12, qso.getCounty());
            ps.setString(13, qso.getNotes());
            ps.setInt   (14, qso.isQslSent()     ? 1 : 0);
            ps.setInt   (15, qso.isQslReceived()  ? 1 : 0);
            ps.setString(16, qso.getSig());
            ps.setString(17, qso.getSigInfo());
            ps.setLong  (18, qso.getId());
            ps.executeUpdate();
        }
    }

    /** Delete a QSO by id. */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM qso WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Fetch last N QSOs ordered newest-first. */
    public List<QsoRecord> fetchLast(int limit) throws SQLException {
        return query("SELECT * FROM qso ORDER BY datetime_utc DESC LIMIT " + limit);
    }

    /** Fetch page of QSOs (offset-based). */
    public List<QsoRecord> fetchPage(int offset, int size) throws SQLException {
        return query("SELECT * FROM qso ORDER BY datetime_utc DESC LIMIT " + size + " OFFSET " + offset);
    }

    /** Total QSO count. */
    public int count() throws SQLException {
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM qso")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Delete every row matching the (callsign, band, mode, date) dupe key.
     *  When {@code date} is null, falls back to the legacy (call, band, mode)
     *  match — used for records that didn't carry a date so the operator
     *  can still wipe them out by callsign/band/mode. Returns the number
     *  of rows removed. */
    public int deleteByKey(String callsign, String band, String mode,
                           java.time.LocalDate date) throws SQLException {
        String sql = (date == null)
            ? "DELETE FROM qso WHERE callsign=? AND band=? AND mode=?"
            : "DELETE FROM qso WHERE callsign=? AND band=? AND mode=? "
            + "AND substr(datetime_utc, 1, 10)=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, callsign == null ? "" : callsign.toUpperCase());
            ps.setString(2, band == null ? "" : band);
            ps.setString(3, mode == null ? "" : mode);
            if (date != null) ps.setString(4, date.toString());
            return ps.executeUpdate();
        }
    }

    /** Check for duplicate by the dupe key (callsign + band + mode + date).
     *  Same call on different UTC days is NOT a duplicate (per the operator
     *  rule "if dates are different it's not a dupe"). Same call+band+mode
     *  on the same UTC day IS a duplicate. When {@code date} is null, falls
     *  back to legacy (call, band, mode) match. */
    public boolean isDuplicate(String callsign, String band, String mode,
                               java.time.LocalDate date) throws SQLException {
        String sql = (date == null)
            ? "SELECT COUNT(*) FROM qso WHERE callsign=? AND band=? AND mode=?"
            : "SELECT COUNT(*) FROM qso WHERE callsign=? AND band=? AND mode=? "
            + "AND substr(datetime_utc, 1, 10)=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, callsign == null ? "" : callsign.toUpperCase());
            ps.setString(2, band == null ? "" : band);
            ps.setString(3, mode == null ? "" : mode);
            if (date != null) ps.setString(4, date.toString());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Fetch all QSOs for ADIF/CSV export. */
    public List<QsoRecord> fetchAll() throws SQLException {
        return query("SELECT * FROM qso ORDER BY datetime_utc ASC");
    }

    /** Rows that match the dupe key (callsign + band + mode + date) in
     *  insertion order. Used by AdifImporter's REVIEW dupe-mode so the
     *  operator's review dialog can show the EXISTING row(s) next to the
     *  incoming row before deciding Keep / Overwrite / Skip. When
     *  {@code date} is null, falls back to legacy (call, band, mode) match. */
    public List<QsoRecord> findByDupeKey(String callsign, String band, String mode,
                                         java.time.LocalDate date) throws SQLException {
        String sql = (date == null)
            ? "SELECT * FROM qso WHERE callsign=? AND band=? AND mode=? ORDER BY id ASC"
            : "SELECT * FROM qso WHERE callsign=? AND band=? AND mode=? "
            + "AND substr(datetime_utc, 1, 10)=? ORDER BY id ASC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, callsign == null ? "" : callsign.toUpperCase().trim());
            ps.setString(2, band == null ? "" : band);
            ps.setString(3, mode == null ? "" : mode);
            if (date != null) ps.setString(4, date.toString());
            try (ResultSet rs = ps.executeQuery()) {
                List<QsoRecord> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    /** All QSOs with this callsign (any band, any mode), newest-first.
     *  Used by the cockpit's live "Previous QSOs with &lt;CALL&gt;" panel —
     *  populates when the operator types into the callsign entry field. */
    public List<QsoRecord> findByCallsign(String callsign) throws SQLException {
        if (callsign == null || callsign.isBlank()) return List.of();
        String sql = "SELECT * FROM qso WHERE callsign=? ORDER BY datetime_utc DESC";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, callsign.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                List<QsoRecord> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    private List<QsoRecord> query(String sql) throws SQLException {
        List<QsoRecord> list = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private QsoRecord map(ResultSet rs) throws SQLException {
        QsoRecord q = new QsoRecord();
        q.setId           (rs.getLong  ("id"));
        q.setCallsign     (rs.getString("callsign"));
        String dtStr = rs.getString("datetime_utc");
        if (dtStr != null) q.setDateTimeUtc(LocalDateTime.parse(dtStr, FMT));
        q.setBand         (rs.getString("band"));
        q.setMode         (rs.getString("mode"));
        q.setFrequency    (rs.getString("frequency"));
        q.setPowerWatts   (rs.getInt   ("power_watts"));
        q.setRstSent      (rs.getString("rst_sent"));
        q.setRstReceived  (rs.getString("rst_received"));
        q.setCountry      (rs.getString("country"));
        q.setOperatorName (rs.getString("operator_name"));
        q.setState        (rs.getString("state"));
        q.setCounty       (rs.getString("county"));
        q.setNotes        (rs.getString("notes"));
        q.setQslSent      (rs.getInt   ("qsl_sent")     == 1);
        q.setQslReceived  (rs.getInt   ("qsl_received") == 1);
        q.setSig          (rs.getString("sig"));
        q.setSigInfo      (rs.getString("sig_info"));
        return q;
    }
}
