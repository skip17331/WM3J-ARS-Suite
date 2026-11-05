package com.jlog.db;

import com.jlog.model.QsoRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

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

    // ---- network mode: identity, change events ------------------------------------------------------------

    /** What changed in the contest log through this station's own logging (not a QSO that arrived from another station). */
    public record Change(Kind kind, String uid, String contestId) { public enum Kind { ADD, EDIT, DELETE } }
    /** One QSO's version, for comparing logs: its uid, when it last changed, who logged it, and whether it is a deletion. */
    public record Meta(String uid, long ts, String origin, boolean deleted) {}

    private final List<Consumer<Change>> listeners = new CopyOnWriteArrayList<>();
    private volatile String stationId = "";

    /** Tells the log which station this is; QSOs logged here carry the name as their origin. */
    public void setStationId(String id) { stationId = id == null ? "" : id; }
    public void addListener(Consumer<Change> l) { if (l != null) listeners.add(l); }
    public void removeListener(Consumer<Change> l) { listeners.remove(l); }
    private void fire(Change.Kind k, String uid, String contestId) {
        for (Consumer<Change> l : listeners) { try { l.accept(new Change(k, uid, contestId)); } catch (RuntimeException e) { log.warn("contest log listener failed: {}", e.getMessage()); } }
    }

    public long insert(QsoRecord qso) throws SQLException {
        if (qso.getUid() == null || qso.getUid().isBlank()) qso.setUid(UUID.randomUUID().toString());
        if (qso.getOrigin() == null || qso.getOrigin().isBlank()) qso.setOrigin(stationId);
        if (qso.getUpdatedMs() <= 0) qso.setUpdatedMs(System.currentTimeMillis());
        long id = insertRow(qso, qso.getPoints(), qso.isDupe());
        fire(Change.Kind.ADD, qso.getUid(), qso.getContestId());
        return id;
    }

    private long insertRow(QsoRecord qso, int points, boolean dupe) throws SQLException {
        String sql = """
            INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode,frequency,
                operator,serial_sent,serial_received,field1,field2,field3,field4,field5,
                points,is_dupe,rst_sent,rst_received,notes,uid,origin,updated_ms)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            ps.setInt   (15, points);
            ps.setInt   (16, dupe ? 1 : 0);
            ps.setString(17, qso.getRstSent());
            ps.setString(18, qso.getRstReceived());
            ps.setString(19, qso.getNotes());
            ps.setString(20, qso.getUid());
            ps.setString(21, qso.getOrigin() == null ? "" : qso.getOrigin());
            ps.setLong  (22, qso.getUpdatedMs());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { long id = gk.getLong(1); qso.setId(id); return id; }
        }
        return -1;
    }

    /** An edit made at this station: stamps the change time and tells the listeners. */
    public void update(QsoRecord qso) throws SQLException {
        qso.setUpdatedMs(System.currentTimeMillis());
        if (qso.getUid() == null || qso.getUid().isBlank()) { QsoRecord cur = findById(qso.getId()); if (cur != null) qso.setUid(cur.getUid()); }
        updateRow(qso, qso.getPoints(), qso.isDupe());
        fire(Change.Kind.EDIT, qso.getUid(), qso.getContestId());
    }

    private void updateRow(QsoRecord qso, int points, boolean dupe) throws SQLException {
        String sql = """
            UPDATE contest_qso SET callsign=?,datetime_utc=?,band=?,mode=?,frequency=?,
                operator=?,serial_sent=?,serial_received=?,
                field1=?,field2=?,field3=?,field4=?,field5=?,
                points=?,is_dupe=?,rst_sent=?,rst_received=?,notes=?,updated_ms=?
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
            ps.setInt   (14, points);
            ps.setInt   (15, dupe ? 1 : 0);
            ps.setString(16, qso.getRstSent());
            ps.setString(17, qso.getRstReceived());
            ps.setString(18, qso.getNotes());
            ps.setLong  (19, qso.getUpdatedMs());
            ps.setLong  (20, qso.getId());
            ps.executeUpdate();
        }
    }

    /** Erases a QSO at this station; a tombstone is left so the deletion reaches the other stations. */
    public void delete(long id) throws SQLException {
        QsoRecord cur = findById(id);
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM contest_qso WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
        }
        if (cur != null && cur.getUid() != null) {
            writeTombstone(cur.getUid(), cur.getContestId(), System.currentTimeMillis());
            fire(Change.Kind.DELETE, cur.getUid(), cur.getContestId());
        }
    }

    /**
     * Sets the points and dupe flag worked out from the whole log. This is not an edit: it does not change the QSO's version and is not
     * sent to other stations (each station works these out for itself, from its own position and the merged log).
     */
    public void updateDerived(long id, int points, boolean dupe) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("UPDATE contest_qso SET points=?, is_dupe=? WHERE id=?")) {
            ps.setInt(1, points); ps.setInt(2, dupe ? 1 : 0); ps.setLong(3, id); ps.executeUpdate();
        }
    }

    // ---- network mode: applying and comparing other stations' QSOs ----------------------------------------------

    public QsoRecord findById(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM contest_qso WHERE id=?")) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    public QsoRecord findByUid(String uid) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("SELECT * FROM contest_qso WHERE uid=?")) {
            ps.setString(1, uid);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? map(rs) : null;
        }
    }

    /** The version of a QSO as this station holds it - live or deleted - or null if it has never heard of it. */
    public Meta metaOf(String uid) throws SQLException {
        QsoRecord q = findByUid(uid);
        if (q != null) return new Meta(uid, q.getUpdatedMs(), q.getOrigin() == null ? "" : q.getOrigin(), false);
        try (PreparedStatement ps = conn().prepareStatement("SELECT deleted_ms FROM contest_qso_deleted WHERE uid=?")) {
            ps.setString(1, uid);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? new Meta(uid, rs.getLong(1), "", true) : null;
        }
    }

    /** Every QSO of the contest, and every deletion, as versions: what two stations compare to find what each lacks. */
    public List<Meta> inventory(String contestId) throws SQLException {
        List<Meta> out = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement("SELECT uid, updated_ms, origin FROM contest_qso WHERE contest_id=? AND uid IS NOT NULL")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(new Meta(rs.getString(1), rs.getLong(2), rs.getString(3) == null ? "" : rs.getString(3), false));
        }
        try (PreparedStatement ps = conn().prepareStatement("SELECT uid, deleted_ms FROM contest_qso_deleted WHERE contest_id=?")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) out.add(new Meta(rs.getString(1), rs.getLong(2), "", true));
        }
        return out;
    }

    /**
     * Stores a QSO that arrived from another station, as it is (uid, origin and change time kept). Replaces the one with the same uid, or adds it,
     * and forgets any deletion of it. Quiet: listeners are not told, so it is never sent back. Points and the dupe flag are left for
     * this station to work out.
     */
    public void applyReplica(QsoRecord q) throws SQLException {
        QsoRecord cur = findByUid(q.getUid());
        if (cur != null) {
            q.setId(cur.getId());
            updateRow(q, cur.getPoints(), cur.isDupe());
            try (PreparedStatement ps = conn().prepareStatement("UPDATE contest_qso SET origin=?, contest_id=? WHERE id=?")) {
                ps.setString(1, q.getOrigin() == null ? "" : q.getOrigin()); ps.setString(2, q.getContestId()); ps.setLong(3, cur.getId()); ps.executeUpdate();
            }
        } else insertRow(q, 0, false);
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM contest_qso_deleted WHERE uid=?")) { ps.setString(1, q.getUid()); ps.executeUpdate(); }
    }

    /** A deletion that arrived from another station: removes the QSO if we have it and remembers the deletion. Quiet. */
    public void applyReplicaDelete(String uid, String contestId, long ts) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM contest_qso WHERE uid=?")) { ps.setString(1, uid); ps.executeUpdate(); }
        writeTombstone(uid, contestId, ts);
    }

    private void writeTombstone(String uid, String contestId, long ts) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("INSERT OR REPLACE INTO contest_qso_deleted(uid, contest_id, deleted_ms) VALUES(?,?,?)")) {
            ps.setString(1, uid); ps.setString(2, contestId); ps.setLong(3, ts); ps.executeUpdate();
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
        try { q.setUid(rs.getString("uid")); q.setOrigin(rs.getString("origin")); q.setUpdatedMs(rs.getLong("updated_ms")); }
        catch (SQLException noNetworkColumns) { /* a database not yet upgraded */ }
        return q;
    }
}
