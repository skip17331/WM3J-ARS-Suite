package com.jlog.db;

import com.jlog.model.QtcRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access object for WAE-DC QTC traffic (contest.db → contest_qtc).
 * Each stored row = one transferred QTC = one point (Rule §7).
 */
public class ContestQtcDao {

    private static final Logger log = LoggerFactory.getLogger(ContestQtcDao.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ContestQtcDao INSTANCE = new ContestQtcDao();

    public static ContestQtcDao getInstance() { return INSTANCE; }

    private Connection conn() {
        return DatabaseManager.getInstance().getContestConnection();
    }

    public long insert(QtcRecord r) throws SQLException {
        String sql = """
            INSERT INTO contest_qtc(contest_id,partner_call,direction,series_no,series_size,
                qtc_qrg,qtc_band,qtc_mode,qtc_datetime,qso_time,qso_call,qso_serial)
            VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
            """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1,  r.getContestId());
            ps.setString(2,  r.getPartnerCall());
            ps.setString(3,  r.getDirection());
            ps.setInt   (4,  r.getSeriesNo());
            ps.setInt   (5,  r.getSeriesSize());
            ps.setString(6,  r.getQtcQrg());
            ps.setString(7,  r.getQtcBand());
            ps.setString(8,  r.getQtcMode());
            ps.setString(9,  r.getQtcDateTimeUtc() != null
                    ? r.getQtcDateTimeUtc().format(FMT) : FMT.format(LocalDateTime.now()));
            ps.setString(10, r.getQsoTime());
            ps.setString(11, r.getQsoCall());
            ps.setString(12, r.getQsoSerial());
            ps.executeUpdate();
            ResultSet gk = ps.getGeneratedKeys();
            if (gk.next()) { long id = gk.getLong(1); r.setId(id); return id; }
        }
        return -1;
    }

    /** Delete one saved series (every QTC of it). Returns how many QTCs went; each was a point. */
    public int deleteSeries(String contestId, String direction, int seriesNo) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM contest_qtc WHERE contest_id=? AND direction=? AND series_no=?")) {
            ps.setString(1, contestId); ps.setString(2, direction); ps.setInt(3, seriesNo);
            return ps.executeUpdate();
        }
    }

    /**
     * Replace a saved series with corrected rows, all or nothing: the old series is deleted and the new rows inserted in one
     * transaction, so a failure leaves the original as it was.
     */
    public void replaceSeries(String contestId, String direction, int seriesNo, List<QtcRecord> rows) throws SQLException {
        Connection c = conn();
        boolean auto = c.getAutoCommit();
        c.setAutoCommit(false);
        try {
            deleteSeries(contestId, direction, seriesNo);
            for (QtcRecord r : rows) insert(r);
            c.commit();
        } catch (SQLException | RuntimeException e) {
            c.rollback();
            throw e;
        } finally {
            c.setAutoCommit(auto);
        }
    }

    /** Total QTC points for the contest = one per transferred QTC (Rule §7). */
    public int totalQtcPointsByContest(String contestId) throws SQLException {
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT COUNT(*) FROM contest_qtc WHERE contest_id=?")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countByContest(String contestId) throws SQLException {
        return totalQtcPointsByContest(contestId);
    }

    public List<QtcRecord> fetchByContest(String contestId) throws SQLException {
        List<QtcRecord> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(
                "SELECT * FROM contest_qtc WHERE contest_id=? ORDER BY id")) {
            ps.setString(1, contestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                QtcRecord r = new QtcRecord();
                r.setId(rs.getLong("id"));
                r.setContestId(rs.getString("contest_id"));
                r.setPartnerCall(rs.getString("partner_call"));
                r.setDirection(rs.getString("direction"));
                r.setSeriesNo(rs.getInt("series_no"));
                r.setSeriesSize(rs.getInt("series_size"));
                r.setQtcQrg(rs.getString("qtc_qrg"));
                r.setQtcBand(rs.getString("qtc_band"));
                r.setQtcMode(rs.getString("qtc_mode"));
                String dt = rs.getString("qtc_datetime");
                if (dt != null && !dt.isBlank()) {
                    try { r.setQtcDateTimeUtc(LocalDateTime.parse(dt, FMT)); }
                    catch (Exception ignore) {}
                }
                r.setQsoTime(rs.getString("qso_time"));
                r.setQsoCall(rs.getString("qso_call"));
                r.setQsoSerial(rs.getString("qso_serial"));
                list.add(r);
            }
        }
        return list;
    }
}
