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
    /** QSO phrase translator — independent DB so power users can ship a
     *  shared translations.db between machines without touching the log. */
    private Connection translationsConn;

    private Path dataDir;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() { return INSTANCE; }

    public void initAll() throws Exception {
        dataDir = Paths.get(System.getProperty("user.home"), ".j-log");
        dataDir.toFile().mkdirs();

        logConn          = openDb(dataDir.resolve("j-log.db").toString());
        contestConn      = openDb(dataDir.resolve("contest.db").toString());
        configConn       = openDb(dataDir.resolve("config.db").toString());
        translationsConn = openDb(dataDir.resolve("translations.db").toString());

        applyLogSchema();
        applyContestSchema();
        applyConfigSchema();
        applyTranslationsSchema();
        log.info("Databases initialised in {}", dataDir);

        // Kick off scheduled backups. The service is a no-op if backups are
        // disabled in AppConfig; calling start() twice is safe.
        try { BackupService.getInstance().start(); }
        catch (Exception e) { log.warn("could not start backup service: {}", e.getMessage()); }
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
                    sig           TEXT,
                    sig_info      TEXT,
                    created_at    TEXT DEFAULT (datetime('now'))
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qso_call ON qso(callsign)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qso_dt   ON qso(datetime_utc)");
            // In-place upgrade for pre-sig databases. SQLite's PRAGMA
            // table_info is the portable way to detect an existing column.
            addColumnIfMissing("qso", "sig",      "TEXT");
            addColumnIfMissing("qso", "sig_info", "TEXT");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_qso_sig ON qso(sig_info)");
        }
    }

    private void addColumnIfMissing(String table, String column, String type) throws SQLException {
        try (Statement st = logConn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) return;
        }
        try (Statement st = logConn.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            log.info("Upgraded {}: added column {}", table, column);
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

            // WAE-DC QTC traffic (Rule §7). One row = one transferred QTC.
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS contest_qtc (
                    id            INTEGER PRIMARY KEY AUTOINCREMENT,
                    contest_id    TEXT NOT NULL,
                    partner_call  TEXT NOT NULL,
                    direction     TEXT,
                    series_no     INTEGER,
                    series_size   INTEGER,
                    qtc_qrg       TEXT,
                    qtc_band      TEXT,
                    qtc_mode      TEXT,
                    qtc_datetime  TEXT,
                    qso_time      TEXT,
                    qso_call      TEXT,
                    qso_serial    TEXT,
                    created_at    TEXT DEFAULT (datetime('now'))
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cqtc_cid ON contest_qtc(contest_id)");
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

            // Operator-curated priority-callsign watch list — fires a
            // banner + audible alert when a matching DX spot arrives.
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS priority_callsigns (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    callsign   TEXT NOT NULL UNIQUE,
                    note       TEXT,
                    muted      INTEGER NOT NULL DEFAULT 0,
                    audible    INTEGER NOT NULL DEFAULT 1,
                    banner     INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT DEFAULT (datetime('now'))
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_prio_call ON priority_callsigns(callsign)");
        }
    }

    /**
     * QSO phrase translator schema. Seeds the table with a starter set
     * the first time the database file is created; subsequent runs leave
     * operator edits alone.
     */
    private void applyTranslationsSchema() throws SQLException {
        try (Statement st = translationsConn.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS translations (
                    phrase_id  INTEGER PRIMARY KEY AUTOINCREMENT,
                    category   TEXT,
                    english    TEXT NOT NULL,
                    spanish    TEXT,
                    german     TEXT,
                    portuguese TEXT,
                    phonetic   TEXT
                )
                """);
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tr_english  ON translations(english)");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_tr_category ON translations(category)");

            // Seed only when empty so operator edits survive restarts.
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM translations")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seedTranslations();
                    log.info("Translations DB seeded with starter phrases");
                }
            }
        }
    }

    /** Built-in 20+ phrase QSO starter set. */
    private void seedTranslations() throws SQLException {
        String sql = "INSERT INTO translations(category,english,spanish,german,portuguese,phonetic) " +
                     "VALUES(?,?,?,?,?,?)";
        // category, english, spanish, german, portuguese, phonetic (Spanish)
        Object[][] rows = {
            {"greeting", "Good morning",        "Buenos días",            "Guten Morgen",         "Bom dia",                  "BWAY-nos DEE-as"},
            {"greeting", "Good afternoon",      "Buenas tardes",          "Guten Tag",            "Boa tarde",                "BWAY-nas TAR-des"},
            {"greeting", "Good evening",        "Buenas noches",          "Guten Abend",          "Boa noite",                "BWAY-nas NO-ches"},
            {"greeting", "Hello",               "Hola",                   "Hallo",                "Olá",                       "OH-lah"},
            {"signoff",  "Goodbye",             "Adiós",                  "Auf Wiedersehen",      "Adeus",                    "ah-dee-OS"},
            {"signoff",  "73",                  "73",                     "73",                   "73",                        "SET-en-ta y TRES"},
            {"qso",      "Thank you for the contact", "Gracias por el contacto", "Danke für den Kontakt", "Obrigado pelo contato", "GRA-see-as por el con-TAK-to"},
            {"qso",      "Your signal report is", "Tu reporte de señal es", "Dein Rapport ist",    "Seu relatório é",          "tu re-POR-te de se-NYAL es"},
            {"qso",      "Please repeat",       "Por favor repita",       "Bitte wiederholen",    "Por favor repita",         "por fa-VOR re-PEE-ta"},
            {"qso",      "Roger that",          "Recibido",               "Verstanden",           "Entendido",                "re-see-BEE-do"},
            {"qso",      "Copy that",           "Copiado",                "Verstanden",           "Copiado",                  "co-pee-AH-do"},
            {"qso",      "Calling CQ",          "Llamando CQ",            "Allgemeiner Anruf CQ", "Chamando CQ",              "ya-MAN-do CQ"},
            {"qso",      "Standing by",         "En escucha",             "Aufnahmebereit",        "Aguardando",               "en es-COO-cha"},
            {"qso",      "Please QSL",          "QSL por favor",          "QSL bitte",            "QSL por favor",            "QSL por fa-VOR"},
            {"qso",      "Confirm",             "Confirmar",              "Bestätigen",           "Confirmar",                "con-feer-MAR"},
            {"qth",      "What is your QTH?",   "¿Cuál es tu QTH?",       "Wie ist Ihr QTH?",     "Qual é o seu QTH?",        "kwal es tu Q-T-H"},
            {"qth",      "My location is",      "Mi ubicación es",        "Mein Standort ist",    "Minha localização é",      "mee oo-bee-ca-see-OWN es"},
            {"name",     "My name is",          "Mi nombre es",           "Mein Name ist",        "Meu nome é",               "mee NOM-bre es"},
            {"name",     "What is your name?",  "¿Cuál es tu nombre?",    "Wie heißen Sie?",      "Qual é o seu nome?",       "kwal es tu NOM-bre"},
            {"weather",  "The weather is good", "El clima está bien",     "Das Wetter ist gut",   "O tempo está bom",         "el CLEE-ma es-TA bee-EN"},
            {"weather",  "It is raining",       "Está lloviendo",         "Es regnet",            "Está chovendo",            "es-TA yoh-vee-EN-do"},
            {"station",  "What antenna are you using?", "¿Qué antena usas?", "Welche Antenne benutzen Sie?", "Que antena você está usando?", "ke an-TEN-a OO-sas"},
            {"station",  "How much power?",    "¿Cuánta potencia?",      "Wie viel Leistung?",   "Quanta potência?",         "KWAN-ta po-TEN-see-a"},
            {"contest",  "Please send again",  "Por favor envíe de nuevo","Bitte nochmal senden", "Por favor, envie novamente", "por fa-VOR en-VEE-ay de NWAY-vo"},
            {"contest",  "Contest exchange",   "Intercambio de concurso","Contestaustausch",      "Troca de concurso",        "een-ter-CAM-bee-o de con-COOR-so"}
        };
        try (PreparedStatement ps = translationsConn.prepareStatement(sql)) {
            for (Object[] row : rows) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.setString(5, (String) row[4]);
                ps.setString(6, (String) row[5]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    public Connection getLogConnection()         { return logConn;          }
    public Connection getContestConnection()     { return contestConn;      }
    public Connection getConfigConnection()      { return configConn;       }
    public Connection getTranslationsConnection(){ return translationsConn; }
    public Path       getDataDir()               { return dataDir;          }

    public void closeAll() {
        closeQuietly(logConn);
        closeQuietly(contestConn);
        closeQuietly(configConn);
        closeQuietly(translationsConn);
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
