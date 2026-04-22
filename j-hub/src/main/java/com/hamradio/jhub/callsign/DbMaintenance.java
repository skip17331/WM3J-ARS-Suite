package com.hamradio.jhub.callsign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;

/**
 * SQLite schema migration and post-import housekeeping for the local callsign DB.
 *
 * Schema versions are tracked in a {@code schema_version} table so that columns
 * added in future releases are applied automatically on first run after upgrade.
 * Maintenance (ANALYZE + VACUUM) is run after every successful import to keep
 * query performance healthy and reclaim space from replaced rows.
 */
public class DbMaintenance {

    private static final Logger log = LoggerFactory.getLogger(DbMaintenance.class);

    static final int CURRENT_VERSION = 1;

    /**
     * Applies any pending schema migrations then runs ANALYZE + VACUUM.
     * Safe to call on an empty or partial DB.
     */
    public static void run(String dbPath) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            applyMigrations(conn);
            try (Statement st = conn.createStatement()) {
                log.debug("Running ANALYZE…");
                st.execute("ANALYZE");
                log.debug("Running VACUUM…");
                st.execute("VACUUM");
            }
            log.info("DB maintenance complete");
        }
    }

    // ── Migrations ─────────────────────────────────────────────────────────────

    static void applyMigrations(Connection conn) throws SQLException {
        // Ensure the version-tracking table exists
        try (Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS schema_version " +
                "(version INTEGER PRIMARY KEY, applied_at TEXT)");
        }

        int current = currentVersion(conn);
        if (current >= CURRENT_VERSION) return;

        // v1: baseline — ensure every expected column exists.
        // Handles DBs created by earlier code that lacked some columns.
        if (current < 1) {
            ensureColumn(conn, "callsigns", "lat",             "REAL    DEFAULT 0");
            ensureColumn(conn, "callsigns", "lon",             "REAL    DEFAULT 0");
            ensureColumn(conn, "callsigns", "grid",            "TEXT");
            ensureColumn(conn, "callsigns", "license_expires", "TEXT");
            ensureColumn(conn, "callsigns", "source",          "TEXT");
            ensureColumn(conn, "callsigns", "imported_at",     "TEXT");
            markVersion(conn, 1);
            log.info("Schema migrated to version 1");
        }

        // Future migrations go here — keep them cumulative, never destructive:
        // if (current < 2) { ensureColumn(conn, "callsigns", "lotw", "INTEGER DEFAULT 0"); markVersion(conn, 2); }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static int currentVersion(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT MAX(version) FROM schema_version")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }

    /** Adds {@code col} to {@code table} only if it does not already exist. */
    private static void ensureColumn(Connection conn, String table, String col, String typeDef)
            throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                if (col.equalsIgnoreCase(rs.getString("name"))) return;
            }
        }
        try (Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN " + col + " " + typeDef);
            log.debug("Added column {}.{}", table, col);
        }
    }

    private static void markVersion(Connection conn, int version) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO schema_version (version, applied_at) VALUES (?,?)")) {
            ps.setInt(1, version);
            ps.setString(2, Instant.now().toString());
            ps.execute();
        }
    }
}
