package com.jlog.db;

import com.jlog.model.Qtc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for {@link Qtc} records in the shared contest database. Used by
 * the j-wae app (and any future WAE-aware contest tools). Lives in the engine
 * module so that j-log and j-wae can share the same physical database.
 */
public class QtcDao {

    private static final Logger log = LoggerFactory.getLogger(QtcDao.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final QtcDao INSTANCE = new QtcDao();

    public static QtcDao getInstance() { return INSTANCE; }

    private Connection conn() { return DatabaseManager.getInstance().getContestConnection(); }

    public long insert(Qtc q) throws SQLException {
        String sql = """
            INSERT INTO qtc(contest_id,direction,peer_call,qtc_time_utc,qtc_call,qtc_serial,qso_id)
            VALUES(?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getContestId());
            ps.setString(2, q.getDirection());
            ps.setString(3, q.getPeerCall() == null ? "" : q.getPeerCall().toUpperCase());
            ps.setString(4, q.getQtcTimeUtc() != null ? q.getQtcTimeUtc().format(FMT) : null);
            ps.setString(5, q.getQtcCall() == null ? "" : q.getQtcCall().toUpperCase());
            ps.setString(6, q.getQtcSerial());
            if (q.getQsoId() != null) ps.setLong(7, q.getQsoId());
            else                      ps.setNull(7, Types.INTEGER);
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { long id = gk.getLong(1); q.setId(id); return id; }
        }
        return -1;
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement("DELETE FROM qtc WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Number of QTCs already exchanged with a peer (in either direction) —
     *  the WAE rule is "max 10 per QSO" keyed on the other station. */
    public int countWithPeer(String contestId, String peerCall) throws SQLException {
        String sql = "SELECT COUNT(*) FROM qtc WHERE contest_id=? AND peer_call=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, peerCall.toUpperCase());
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** True if a QTC referencing the given (qtcCall, qtcSerial) has already
     *  been sent to this peer. WAE forbids repeating a QTC to the same peer. */
    public boolean alreadySentTo(String contestId, String peerCall, String qtcCall, String qtcSerial) throws SQLException {
        String sql = "SELECT COUNT(*) FROM qtc WHERE contest_id=? AND peer_call=? "
                   + "AND direction='SENT' AND qtc_call=? AND qtc_serial=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, contestId);
            ps.setString(2, peerCall.toUpperCase());
            ps.setString(3, qtcCall.toUpperCase());
            ps.setString(4, qtcSerial);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    public List<Qtc> fetchByContest(String contestId) throws SQLException {
        List<Qtc> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM qtc WHERE contest_id=? ORDER BY id")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countByContest(String contestId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM qtc WHERE contest_id=?")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Qtc map(ResultSet rs) throws SQLException {
        Qtc q = new Qtc();
        q.setId         (rs.getLong  ("id"));
        q.setContestId  (rs.getString("contest_id"));
        q.setDirection  (rs.getString("direction"));
        q.setPeerCall   (rs.getString("peer_call"));
        String t = rs.getString("qtc_time_utc");
        if (t != null) q.setQtcTimeUtc(LocalDateTime.parse(t, FMT));
        q.setQtcCall    (rs.getString("qtc_call"));
        q.setQtcSerial  (rs.getString("qtc_serial"));
        long qsoId      = rs.getLong("qso_id");
        if (!rs.wasNull()) q.setQsoId(qsoId);
        return q;
    }
}
