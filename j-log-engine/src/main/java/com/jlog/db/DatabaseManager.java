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
    /** The file the contest connection currently points at ({@code contest.db} unless switched). */
    private Path contestDbPath;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() { return INSTANCE; }

    public void initAll() throws Exception {
        dataDir = Paths.get(System.getProperty("user.home"), ".j-log");
        dataDir.toFile().mkdirs();

        logConn          = openDb(dataDir.resolve("j-log.db").toString());
        contestDbPath    = dataDir.resolve("contest.db");
        contestConn      = openDb(contestDbPath.toString());
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

    private boolean contestColumnExists(String column) throws SQLException {
        try (Statement st = contestConn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(contest_qso)")) {
            while (rs.next()) if (column.equalsIgnoreCase(rs.getString("name"))) return true;
        }
        return false;
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

            // One-time migration: older contest_qso carried a redundant `exchange`
            // blob (the structured field1..5 + rst/serial already hold the data,
            // and it never fed Cabrillo). SQLite has no DROP COLUMN before 3.35,
            // so rebuild the table without it.
            if (contestColumnExists("exchange")) {
                st.executeUpdate("""
                    CREATE TABLE contest_qso__new (
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
                st.executeUpdate("""
                    INSERT INTO contest_qso__new
                        (id,contest_id,callsign,datetime_utc,band,mode,frequency,operator,
                         serial_sent,serial_received,field1,field2,field3,field4,field5,
                         points,is_dupe,rst_sent,rst_received,notes,created_at)
                    SELECT id,contest_id,callsign,datetime_utc,band,mode,frequency,operator,
                         serial_sent,serial_received,field1,field2,field3,field4,field5,
                         points,is_dupe,rst_sent,rst_received,notes,created_at
                    FROM contest_qso
                    """);
                st.executeUpdate("DROP TABLE contest_qso");
                st.executeUpdate("ALTER TABLE contest_qso__new RENAME TO contest_qso");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cqso_call ON contest_qso(callsign)");
                st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_cqso_cid  ON contest_qso(contest_id)");
                log.info("Migrated contest_qso: dropped redundant `exchange` column");
            }

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

            // Per-language pronunciation guides: `phonetic` is the Spanish one (its original meaning); German and Portuguese
            // get their own columns. Added to existing databases without touching what is already there.
            for (String col : new String[]{"phonetic_de", "phonetic_pt"}) {
                boolean have = false;
                try (ResultSet cols = st.executeQuery("PRAGMA table_info(translations)")) {
                    while (cols.next()) if (col.equalsIgnoreCase(cols.getString("name"))) have = true;
                }
                if (!have) st.executeUpdate("ALTER TABLE translations ADD COLUMN " + col + " TEXT");
            }

            // Seed only when empty so operator edits survive restarts.
            try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM translations")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    seedTranslations();
                    log.info("Translations DB seeded with starter phrases");
                }
                backfillPhonetics();
            }
        }
    }

    /** Built-in 20+ phrase QSO starter set. */
    /** English-speaker pronunciation guides for the German and Portuguese starter phrases, keyed by the English phrase.
     *  Only fills a cell that is empty, so a guide the operator wrote or corrected is never overwritten. */
    private void backfillPhonetics() throws SQLException {
        String[][] g = {
            {"Good morning", "GOO-ten MOR-gen", "bohm JEE-ah"},
            {"Good afternoon", "GOO-ten TAHK", "BOH-ah TAR-jee"},
            {"Good evening", "GOO-ten AH-bent", "BOH-ah NOY-chee"},
            {"Hello", "HAH-loh", "oh-LAH"},
            {"Goodbye", "owf VEE-der-zayn", "ah-DAY-oosh"},
            {"73", "DRY-oont-ZEEP-tsikh", "seh-TEN-tah ee TRAYSH"},
            {"Thank you for the contact", "DAHN-keh fewr dayn kon-TAKT", "oh-bree-GAH-doo PAY-loo kon-TAH-too"},
            {"Your signal report is", "dine rah-PORT ist", "SAY-oo heh-lah-TOR-yoo eh"},
            {"Please repeat", "BIT-teh vee-der-HOH-len", "poor fah-VOR heh-PEE-tah"},
            {"Roger that", "fer-SHTAHN-den", "en-ten-DEE-doo"},
            {"Copy that", "fer-SHTAHN-den", "koh-pee-AH-doo"},
            {"Calling CQ", "AHL-geh-my-ner AHN-roof CQ", "shah-MAHN-doo CQ"},
            {"Standing by", "OWF-nah-meh-beh-RITE", "ah-gwar-DAHN-doo"},
            {"Please QSL", "Q-S-L BIT-teh", "Q-S-L poor fah-VOR"},
            {"Confirm", "beh-SHTAY-tih-gen", "kon-feer-MAR"},
            {"What is your QTH?", "vee ist eer Q-T-H", "kwahl eh oo SAY-oo Q-T-H"},
            {"My location is", "mine SHTAHNT-ort ist", "MEEN-yah loh-kah-lee-zah-SOWN eh"},
            {"My name is", "mine NAH-meh ist", "MAY-oo NOH-mee eh"},
            {"What is your name?", "vee HY-sen zee", "kwahl eh oo SAY-oo NOH-mee"},
            {"The weather is good", "dahs VET-ter ist goot", "oo TEM-poo ish-TAH bohm"},
            {"It is raining", "es RAYG-net", "ish-TAH shoh-VEN-doo"},
            {"What antenna are you using?", "VEL-kheh ahn-TEN-neh beh-NOOT-sen zee", "kee ahn-TEH-nah voh-SAY ish-TAH oo-ZAHN-doo"},
            {"How much power?", "vee feel LICE-toong", "KWAHN-tah poh-TEN-syah"},
            {"Please send again", "BIT-teh NOKH-mahl ZEN-den", "poor fah-VOR EN-vee-eh noh-vah-MEN-chee"},
            {"Contest exchange", "kon-TEST-ows-towsh", "TROH-kah jee kon-KOOR-soo"}};
        try (PreparedStatement ps = translationsConn.prepareStatement(
                "UPDATE translations SET phonetic_de = COALESCE(NULLIF(phonetic_de,''), ?), phonetic_pt = COALESCE(NULLIF(phonetic_pt,''), ?) WHERE english = ?")) {
            for (String[] r : g) { ps.setString(1, r[1]); ps.setString(2, r[2]); ps.setString(3, r[0]); ps.addBatch(); }
            ps.executeBatch();
        }
    }

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
    public synchronized Path getContestDbPath()  { return contestDbPath;    }

    /**
     * Point the contest log at a different SQLite file (created, with the contest schema, if it is new).
     * The previous contest connection is closed. The DAOs fetch the connection on every call, so they follow
     * the switch immediately. On failure the previous file stays in use.
     */
    public synchronized void useContestDb(Path file) throws Exception {
        java.nio.file.Files.createDirectories(file.toAbsolutePath().getParent());
        Connection next = openDb(file.toAbsolutePath().toString());
        Connection prev = contestConn;
        contestConn = next;
        try { applyContestSchema(); }
        catch (SQLException e) { contestConn = prev; closeQuietly(next); throw e; }
        contestDbPath = file.toAbsolutePath();
        if (prev != null && prev != next) closeQuietly(prev);
        log.info("Contest log now using {}", contestDbPath);
    }
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
