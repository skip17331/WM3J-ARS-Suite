package com.jlog.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

/**
 * Central database manager.
 * Manages three SQLite databases:
 *   1. j-log.db        — QSO log (normal)
 *   2. contest.db       — QSO log (contest)
 *   3. config.db        — Application config, macros, etc.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final DatabaseManager INSTANCE = new DatabaseManager();

    private Connection logConn;
    private Connection contestConn;
    private Connection configConn;

    private Path dataDir;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() { return INSTANCE; }

    public void initAll() throws Exception {
        dataDir = Paths.get(System.getProperty("user.home"), ".j-log");
        dataDir.toFile().mkdirs();

        logConn     = openDb(dataDir.resolve("j-log.db").toString());
        contestConn = openDb(dataDir.resolve("contest.db").toString());
        configConn  = openDb(dataDir.resolve("config.db").toString());

        applyLogSchema();
        applyContestSchema();
        applyConfigSchema();
        log.info("Databases initialised in {}", dataDir);
    }

    private Connection openDb(String path) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys=ON");
        }
        return c;
    }

    // ---------------------------------------------------------------
    // Schema creation
    // ---------------------------------------------------------------

    private void applyLogSchema() throws SQLException {
        try (Statement st = logConn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS qso (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    callsign      TEXT NOT NULL,
                    datetime_utc  TEXT NOT NULL,
                    band          TEXT,
                    mode          TEXT,
                    frequency     TEXT,
                    power_watts   INTEGER,
                    rst_sent      TEXT,
                    rst_received  TEXT,
                    country       TEXT,
                    operator_name TEXT,
                    state         TEXT,
                    county        TEXT,
                    notes         TEXT,
                    qsl_sent      INTEGER DEFAULT 0,
                    qsl_received  INTEGER DEFAULT 0,
                    created_at    TEXT DEFAULT (datetime('now'))
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qso_call ON qso(callsign)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qso_dt   ON qso(datetime_utc)");
        }
    }

    private void applyContestSchema() throws SQLException {
        try (Statement st = contestConn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_qso (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    contest_id      TEXT NOT NULL,
                    callsign        TEXT NOT NULL,
                    datetime_utc    TEXT NOT NULL,
                    band            TEXT,
                    mode            TEXT,
                    frequency       TEXT,
                    operator        TEXT,
                    serial_sent     TEXT,
                    serial_received TEXT,
                    exchange        TEXT,
                    field1          TEXT,
                    field2          TEXT,
                    field3          TEXT,
                    field4          TEXT,
                    field5          TEXT,
                    points          INTEGER DEFAULT 0,
                    is_dupe         INTEGER DEFAULT 0,
                    rst_sent        TEXT,
                    rst_received    TEXT,
                    notes           TEXT,
                    created_at      TEXT DEFAULT (datetime('now'))
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cqso_call ON contest_qso(callsign)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cqso_cid  ON contest_qso(contest_id)");
        }
    }

    private void applyConfigSchema() throws SQLException {
        try (Statement st = configConn.createStatement()) {
            // Key-value config store
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS config (
                    key   TEXT PRIMARY KEY,
                    value TEXT
                )
                """);

            // Macros
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS macro (
                    id    INTEGER PRIMARY KEY AUTOINCREMENT,
                    name  TEXT NOT NULL,
                    fkey  INTEGER DEFAULT 0,
                    json  TEXT NOT NULL
                )
                """);

        }
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    public Connection getLogConnection()     { return logConn;     }
    public Connection getContestConnection() { return contestConn; }
    public Connection getConfigConnection()  { return configConn;  }
    public Path       getDataDir()           { return dataDir;      }

    public void closeAll() {
        closeQuietly(logConn);
        closeQuietly(contestConn);
        closeQuietly(configConn);
    }

    private void closeQuietly(Connection c) {
        try { if (c != null && !c.isClosed()) c.close(); }
        catch (SQLException ex) { log.warn("Error closing connection", ex); }
    }

    /** Read a config value. Returns defaultValue if not found. */
    public String getConfig(String key, String defaultValue) {
        try (PreparedStatement ps = configConn.prepareStatement(
                "SELECT value FROM config WHERE key=?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        } catch (SQLException ex) {
            log.warn("getConfig error for key={}", key, ex);
        }
        return defaultValue;
    }

    /** Write a config value. */
    public void setConfig(String key, String value) {
        try (PreparedStatement ps = configConn.prepareStatement(
                "INSERT OR REPLACE INTO config(key,value) VALUES(?,?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException ex) {
            log.error("setConfig error for key={}", key, ex);
        }
    }
}
