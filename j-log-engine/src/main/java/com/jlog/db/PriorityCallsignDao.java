package com.jlog.db;

import com.jlog.model.PriorityCallsign;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * DAO for the priority-callsign watch list. Lives in the existing
 * config.db so the operator's watch list backs up alongside their
 * macros and DX-network profiles.
 */
public class PriorityCallsignDao {

    private static final Logger log = LoggerFactory.getLogger(PriorityCallsignDao.class);
    private static final PriorityCallsignDao INSTANCE = new PriorityCallsignDao();

    public static PriorityCallsignDao getInstance() { return INSTANCE; }
    private PriorityCallsignDao() {}

    private Connection conn() {
        return DatabaseManager.getInstance().getConfigConnection();
    }

    public List<PriorityCallsign> getAll() {
        List<PriorityCallsign> out = new ArrayList<>();
        String sql = "SELECT id, callsign, note, muted, audible, banner " +
                     "FROM priority_callsigns ORDER BY callsign";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(map(rs));
        } catch (SQLException ex) {
            log.warn("Failed to load priority callsigns: {}", ex.getMessage());
        }
        return out;
    }

    /**
     * Returns the set of un-muted callsigns (upper-cased) — used by the
     * alert service for fast O(1) matching against incoming spots
     * without pulling note/banner/audible columns for each lookup.
     */
    public Set<String> getActiveCallsigns() {
        Set<String> out = new HashSet<>();
        String sql = "SELECT callsign FROM priority_callsigns WHERE muted=0";
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString(1).toUpperCase(Locale.ROOT));
        } catch (SQLException ex) {
            log.warn("Failed to load active priority callsigns: {}", ex.getMessage());
        }
        return out;
    }

    /**
     * Find a single row by callsign. Case-insensitive. Returns null if
     * not on the list — used by the alert service when it needs the
     * audible / banner flags after a callsign match.
     */
    public PriorityCallsign findByCallsign(String callsign) {
        if (callsign == null) return null;
        String sql = "SELECT id, callsign, note, muted, audible, banner " +
                     "FROM priority_callsigns WHERE UPPER(callsign)=UPPER(?)";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, callsign.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException ex) {
            log.warn("Lookup failed for {}: {}", callsign, ex.getMessage());
        }
        return null;
    }

    public long insert(PriorityCallsign p) {
        String sql = "INSERT OR IGNORE INTO priority_callsigns(callsign, note, muted, audible, banner) " +
                     "VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getCallsign().trim().toUpperCase(Locale.ROOT));
            ps.setString(2, p.getNote());
            ps.setInt   (3, p.isMuted()   ? 1 : 0);
            ps.setInt   (4, p.isAudible() ? 1 : 0);
            ps.setInt   (5, p.isBanner()  ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    p.setId(id);
                    return id;
                }
            }
        } catch (SQLException ex) {
            log.warn("Insert failed: {}", ex.getMessage());
        }
        return -1;
    }

    public boolean update(PriorityCallsign p) {
        String sql = "UPDATE priority_callsigns SET note=?, muted=?, audible=?, banner=? WHERE id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getNote());
            ps.setInt   (2, p.isMuted()   ? 1 : 0);
            ps.setInt   (3, p.isAudible() ? 1 : 0);
            ps.setInt   (4, p.isBanner()  ? 1 : 0);
            ps.setLong  (5, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            log.warn("Update failed: {}", ex.getMessage());
            return false;
        }
    }

    public boolean delete(long id) {
        try (PreparedStatement ps = conn().prepareStatement(
                "DELETE FROM priority_callsigns WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            log.warn("Delete failed: {}", ex.getMessage());
            return false;
        }
    }

    private static PriorityCallsign map(ResultSet rs) throws SQLException {
        PriorityCallsign p = new PriorityCallsign();
        p.setId(rs.getLong("id"));
        p.setCallsign(rs.getString("callsign"));
        p.setNote(rs.getString("note"));
        p.setMuted(rs.getInt("muted") != 0);
        p.setAudible(rs.getInt("audible") != 0);
        p.setBanner(rs.getInt("banner") != 0);
        return p;
    }
}
