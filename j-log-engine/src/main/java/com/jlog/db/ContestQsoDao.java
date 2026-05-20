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
 * Data access object for contest QSOs (contest.db → contest_qso).
 */
public class ContestQsoDao {

    private static final Logger log = LoggerFactory.getLogger(ContestQsoDao.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ContestQsoDao INSTANCE = new ContestQsoDao();

    public static ContestQsoDao getInstance() { return INSTANCE; }

    private Connection conn() {
        return DatabaseManager.getInstance().getContestConnection();
    }

    public long insert(QsoRecord qso) throws SQLException {
        String sql = """
            INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode,frequency,
                operator,serial_sent,serial_received,field1,field2,field3,field4,field5,
                points,is_dupe,rst_sent,rst_received,notes)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  qso.getContestId());
            ps.setString(2,  qso.getCallsign());
            ps.setString(3,  qso.getDateTimeUtc() != null ? qso.getDateTimeUtc().format(FMT) : FMT.format(LocalDateTime.now()));
            ps.setString(4,  qso.getBand());
            ps.setString(5,  qso.getMode());
            ps.setString(6,  qso.getFrequency());
            ps.setString(7,  qso.getOperator());
            ps.setString(8,  qso.getSerialSent());
            ps.setString(9,  qso.getSerialReceived());
            ps.setString(10, qso.getContestField1());
            ps.setString(11, qso.getContestField2());
            ps.setString(12, qso.getContestField3());
            ps.setString(13, qso.getContestField4());
            ps.setString(14, qso.getContestField5());
            ps.setInt   (15, qso.getPoints());
            ps.setInt   (16, qso.isDupe() ? 1 : 0);
            ps.setString(17, qso.getRstSent());
            ps.setString(18, qso.getRstReceived());
            ps.setString(19, qso.getNotes());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { long id = gk.getLong(1); qso.setId(id); return id; }
        }
        return -1;
    }

    public void update(QsoRecord qso) throws SQLException {
        String sql = """
            UPDATE contest_qso SET callsign=?,datetime_utc=?,band=?,mode=?,frequency=?,
                operator=?,serial_sent=?,serial_received=?,
                field1=?,field2=?,field3=?,field4=?,field5=?,
                points=?,is_dupe=?,rst_sent=?,rst_received=?,notes=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1,  qso.getCallsign());
            ps.setString(2,  qso.getDateTimeUtc().format(FMT));
            ps.setString(3,  qso.getBand());
            ps.setString(4,  qso.getMode());
            ps.setString(5,  qso.getFrequency());
            ps.setString(6,  qso.getOperator());
            ps.setString(7,  qso.getSerialSent());
            ps.setString(8,  qso.getSerialReceived());
            ps.setString(9,  qso.getContestField1());
            ps.setString(10, qso.getContestField2());
            ps.setString(11, qso.getContestField3());
            ps.setString(12, qso.getContestField4());
            ps.setString(13, qso.getContestField5());
            ps.setInt   (14, qso.getPoints());
            ps.setInt   (15, qso.isDupe() ? 1 : 0);
            ps.setString(16, qso.getRstSent());
            ps.setString(17, qso.getRstReceived());
            ps.setString(18, qso.getNotes());
            ps.setLong  (19, qso.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM contest_qso WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
    }

    /**
     * Natural-key upsert for network sync. Two stations logging the same call
     * in the same contest at the same second is essentially impossible in one
     * operation, so {@code (contest_id, callsign, datetime_utc)} is a reliable
     * identity. Inserts when no match; updates when found. Returns true if a
     * new row was inserted.
     */
    public boolean upsertByNaturalKey(QsoRecord q) throws SQLException {
        String call = q.getCallsign() == null ? "" : q.getCallsign().toUpperCase();
        String dt   = q.getDateTimeUtc() == null ? null : q.getDateTimeUtc().format(FMT);
        if (dt == null) return false;

        Long existingId = null;
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT id FROM contest_qso WHERE contest_id=? AND callsign=? AND datetime_utc=?")) {
            ps.setString(1, q.getContestId());
            ps.setString(2, call);
            ps.setString(3, dt);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) existingId = rs.getLong(1);
        }

        if (existingId == null) {
            insert(q);
            return true;
        }
        q.setId(existingId);
        update(q);
        return false;
    }

    /**
     * Wipe every QSO for the given contest. Used by File → New Database to
     * start a fresh log for the active event after the operator has exported
     * (Cabrillo/ADIF) and backed up the DB. Returns the number of rows
     * removed so the caller can log/show it.
     *
     * <p>QTC rows in {@code contest_qtc} are joined via QSO id, so wipe
     * those first to avoid orphan rows for plugins that use the QTC table
     * (WAE-DC family).
     */
    public int deleteAllForContest(String contestId) throws SQLException {
        if (contestId == null) return 0;
        try (PreparedStatement qtc = conn().prepareStatement(
                "DELETE FROM contest_qtc WHERE qso_id IN "
              + "(SELECT id FROM contest_qso WHERE contest_id=?)")) {
            qtc.setString(1, contestId);
            qtc.executeUpdate();
        }
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM contest_qso WHERE contest_id=?")) {
            ps.setString(1, contestId);
            return ps.executeUpdate();
        }
    }

    /**
     * Delete by the same natural key upsert uses. Silently no-ops if there is
     * no match — idempotent so repeated broadcasts don't fail.
     */
    public int deleteByNaturalKey(String contestId, String callsign,
                                  java.time.LocalDateTime datetimeUtc) throws SQLException {
        if (datetimeUtc == null) return 0;
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM contest_qso WHERE contest_id=? AND callsign=? AND datetime_utc=?")) {
            ps.setString(1, contestId);
            ps.setString(2, callsign == null ? "" : callsign.toUpperCase());
            ps.setString(3, datetimeUtc.format(FMT));
            return ps.executeUpdate();
        }
    }

    /** Check if callsign is already worked this contest on same band+mode. */
    public boolean isDuplicate(String contestId, String callsign, String band, String mode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? AND band=? AND mode=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ps.setString(3, band);
            ps.setString(4, mode);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Rover-aware dupe: matches on callsign + band + grid (column-whitelisted).
     *  If the same rover reports a new grid, it is NOT a dupe (VHF contests). */
    public boolean isDuplicateBandGrid(String contestId, String callsign,
                                       String band, String gridColumn, String grid) throws SQLException {
        if (!gridColumn.matches("field[1-5]")) return false;
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? "
                   + "AND band=? AND " + gridColumn + "=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ps.setString(3, band);
            ps.setString(4, grid);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Band-only dupe (mode-agnostic). Used by VHF contests where cross-mode
     *  isn't allowed but dupe is tracked per band regardless of mode. */
    public boolean isDuplicatePerBand(String contestId, String callsign, String band) throws SQLException {
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? AND band=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ps.setString(3, band);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Check if callsign is already worked this contest on ANY band/mode (ARRL Sweepstakes). */
    public boolean isDuplicateContestWide(String contestId, String callsign) throws SQLException {
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Check if callsign is already worked this contest on same mode (band-agnostic). */
    public boolean isDuplicatePerMode(String contestId, String callsign, String mode) throws SQLException {
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? AND mode=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ps.setString(3, mode);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** QSO-party / rover-aware dupe: matches on callsign + band + mode +
     *  a whitelisted QTH column (the received county/state field). A
     *  mobile or rover that moves and sends a NEW county/QTH is therefore
     *  NOT a duplicate and may be worked again for QSO + multiplier
     *  credit (MNQP "county line", VTQP straddling mobiles). When the QTH
     *  value is blank the check degrades to plain (call, band, mode). */
    public boolean isDuplicateBandModeField(String contestId, String callsign,
                                            String band, String mode,
                                            String qthColumn, String qth) throws SQLException {
        if (qthColumn == null || !qthColumn.matches("field[1-5]")
                || qth == null || qth.isBlank()) {
            return isDuplicate(contestId, callsign, band, mode);
        }
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND callsign=? "
                   + "AND band=? AND mode=? AND " + qthColumn + "=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ps.setString(3, band);
            ps.setString(4, mode);
            ps.setString(5, qth);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    /** Partial callsign match for dupe checker pane. */
    public List<String> partialMatch(String contestId, String partial) throws SQLException {
        List<String> results = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT DISTINCT callsign FROM contest_qso WHERE contest_id=? AND callsign LIKE ? LIMIT 20")) {
            ps.setString(1, contestId);
            ps.setString(2, partial.toUpperCase() + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) results.add(rs.getString(1));
        }
        return results;
    }

    public List<QsoRecord> fetchByContest(String contestId) throws SQLException {
        List<QsoRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM contest_qso WHERE contest_id=? ORDER BY datetime_utc DESC")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countByContest(String contestId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND is_dupe=0")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Highest serial_sent value previously used in this contest; 0 if none.
     *  Non-numeric serials are ignored so contests that reuse the column for
     *  text values (rare) don't crash the count. */
    public int maxSerialSent(String contestId) throws SQLException {
        int max = 0;
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT serial_sent FROM contest_qso WHERE contest_id=? AND serial_sent IS NOT NULL")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString(1);
                if (s == null || s.isBlank()) continue;
                try { max = Math.max(max, Integer.parseInt(s.trim())); }
                catch (NumberFormatException ignored) {}
            }
        }
        return max;
    }

    public int totalPointsByContest(String contestId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COALESCE(SUM(points),0) FROM contest_qso WHERE contest_id=? AND is_dupe=0")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Get distinct values of any field column (field1–field5) for multiplier counting.
     * The column name is validated against a whitelist to prevent SQL injection.
     */
    public List<String> distinctFieldByColumn(String contestId, String column) throws SQLException {
        if (!column.matches("field[1-5]")) return List.of();
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM contest_qso WHERE contest_id=? AND "
                   + column + " IS NOT NULL AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(rs.getString(1));
        }
        return list;
    }

    /** Distinct multiplier values for a given band (mode-agnostic). */
    public List<String> distinctFieldByColumnAndBand(String contestId, String column, String band) throws SQLException {
        if (!column.matches("field[1-5]")) return List.of();
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM contest_qso WHERE contest_id=? AND band=? AND "
                   + column + " IS NOT NULL AND " + column + " <> '' AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, band);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isBlank()) list.add(v);
            }
        }
        return list;
    }

    /** Distinct multiplier values for a given mode (band-agnostic). */
    public List<String> distinctFieldByColumnAndMode(String contestId, String column, String mode) throws SQLException {
        if (!column.matches("field[1-5]")) return List.of();
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT " + column + " FROM contest_qso WHERE contest_id=? AND mode=? AND "
                   + column + " IS NOT NULL AND " + column + " <> '' AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, mode);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String v = rs.getString(1);
                if (v != null && !v.isBlank()) list.add(v);
            }
        }
        return list;
    }

    /** Total points for one mode. */
    public int pointsByMode(String contestId, String mode) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COALESCE(SUM(points),0) FROM contest_qso WHERE contest_id=? AND mode=? AND is_dupe=0")) {
            ps.setString(1, contestId);
            ps.setString(2, mode);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** First time a particular multiplier value was worked on a mode. */
    public LocalDateTime firstWorkedAt(String contestId, String column, String value, String mode) throws SQLException {
        if (!column.matches("field[1-5]")) return null;
        String sql = "SELECT MIN(datetime_utc) FROM contest_qso WHERE contest_id=? AND mode=? AND "
                   + column + "=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, mode);
            ps.setString(3, value);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String s = rs.getString(1);
                if (s != null) return LocalDateTime.parse(s, FMT);
            }
        }
        return null;
    }

    /** Number of QSOs holding a particular multiplier value on a mode. */
    public int countQsosForMultValue(String contestId, String column, String value, String mode) throws SQLException {
        if (!column.matches("field[1-5]")) return 0;
        String sql = "SELECT COUNT(*) FROM contest_qso WHERE contest_id=? AND mode=? AND "
                   + column + "=? AND is_dupe=0";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, mode);
            ps.setString(3, value);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Returns all prior QSOs with this callsign in this contest (used for worked-before indicator). */
    public List<QsoRecord> findByCallsign(String contestId, String callsign) throws SQLException {
        List<QsoRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM contest_qso WHERE contest_id=? AND callsign=? ORDER BY datetime_utc";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, callsign.toUpperCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    private QsoRecord map(ResultSet rs) throws SQLException {
        QsoRecord q = new QsoRecord();
        q.setId             (rs.getLong  ("id"));
        q.setContestId      (rs.getString("contest_id"));
        q.setCallsign       (rs.getString("callsign"));
        String dtStr = rs.getString("datetime_utc");
        if (dtStr != null) q.setDateTimeUtc(LocalDateTime.parse(dtStr, FMT));
        q.setBand           (rs.getString("band"));
        q.setMode           (rs.getString("mode"));
        q.setFrequency      (rs.getString("frequency"));
        q.setOperator       (rs.getString("operator"));
        q.setSerialSent     (rs.getString("serial_sent"));
        q.setSerialReceived (rs.getString("serial_received"));
        q.setContestField1  (rs.getString("field1"));
        q.setContestField2  (rs.getString("field2"));
        q.setContestField3  (rs.getString("field3"));
        q.setContestField4  (rs.getString("field4"));
        q.setContestField5  (rs.getString("field5"));
        q.setPoints         (rs.getInt   ("points"));
        q.setDupe           (rs.getInt   ("is_dupe") == 1);
        q.setRstSent        (rs.getString("rst_sent"));
        q.setRstReceived    (rs.getString("rst_received"));
        q.setNotes          (rs.getString("notes"));
        return q;
    }
}
