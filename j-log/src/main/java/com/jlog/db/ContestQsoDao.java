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
                operator,serial_sent,serial_received,exchange,field1,field2,field3,field4,field5,
                points,is_dupe,rst_sent,rst_received,notes)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
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
            ps.setString(10, qso.getExchange());
            ps.setString(11, qso.getContestField1());
            ps.setString(12, qso.getContestField2());
            ps.setString(13, qso.getContestField3());
            ps.setString(14, qso.getContestField4());
            ps.setString(15, qso.getContestField5());
            ps.setInt   (16, qso.getPoints());
            ps.setInt   (17, qso.isDupe() ? 1 : 0);
            ps.setString(18, qso.getRstSent());
            ps.setString(19, qso.getRstReceived());
            ps.setString(20, qso.getNotes());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { long id = gk.getLong(1); qso.setId(id); return id; }
        }
        return -1;
    }

    public void update(QsoRecord qso) throws SQLException {
        String sql = """
            UPDATE contest_qso SET callsign=?,datetime_utc=?,band=?,mode=?,frequency=?,
                operator=?,serial_sent=?,serial_received=?,exchange=?,
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
            ps.setString(9,  qso.getExchange());
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
            ps.setLong  (20, qso.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM contest_qso WHERE id=?")) {
            ps.setLong(1, id); ps.executeUpdate();
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

    public int totalPointsByContest(String contestId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COALESCE(SUM(points),0) FROM contest_qso WHERE contest_id=? AND is_dupe=0")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Get all unique values of field1 (e.g. sections) for multiplier counting. */
    public List<String> distinctField1(String contestId) throws SQLException {
        return distinctFieldByColumn(contestId, "field1");
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
        q.setExchange       (rs.getString("exchange"));
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
