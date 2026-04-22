package com.hamradio.jhub.callsign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Imports the FCC Universal Licensing System (ULS) amateur radio database
 * into the local callsign SQLite file.
 *
 * Download free data from the FCC:
 *   https://www.fcc.gov/uls/index.php?job=transaction&page=weekly-monthly
 *   → "Amateur" → "Complete" → l_amat.zip
 *
 * Extract the ZIP, then POST {"ulsDir":"/path/to/extracted"} to
 * /api/callsign/db/import/fcc.
 *
 * Relevant files in the extracted directory:
 *   HD.dat — license header (status, service code, dates)
 *   EN.dat — entity/licensee (name, address, email)
 *   AM.dat — amateur specifics (operator class)
 *
 * All files are pipe-delimited (|), one record per line, ISO-8859-1 encoding.
 */
public class FccUlsImporter {

    private static final Logger log = LoggerFactory.getLogger(FccUlsImporter.class);

    public enum Status { IDLE, RUNNING, DONE, FAILED }

    private volatile Status status      = Status.IDLE;
    private volatile String statusMsg   = "Not started";
    private final AtomicLong imported   = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final FccUlsImporter INSTANCE = new FccUlsImporter();
    public static FccUlsImporter getInstance() { return INSTANCE; }
    private FccUlsImporter() {}

    public Status getStatus()      { return status; }
    public String getStatusMsg()   { return statusMsg; }
    public long   getImported()    { return imported.get(); }
    public boolean isRunning()     { return running.get(); }

    /**
     * Kicks off a background import thread.
     * Returns false immediately if an import is already in progress.
     */
    public boolean start(String ulsDir, String dbPath) {
        if (!running.compareAndSet(false, true)) return false;
        Thread t = new Thread(() -> doImport(ulsDir, dbPath), "jhub-fcc-import");
        t.setDaemon(true);
        t.start();
        return true;
    }

    // ── Import pipeline ────────────────────────────────────────────────────────

    private void doImport(String ulsDir, String dbPath) {
        imported.set(0);
        status = Status.RUNNING;
        try {
            Path dir = Path.of(ulsDir);
            if (!Files.isDirectory(dir))
                throw new IOException("ULS directory not found: " + ulsDir);

            log.info("FCC ULS import starting from {}", ulsDir);

            // Phase 1 — collect unique_system_identifiers for active amateur licenses from HD.dat
            statusMsg = "Scanning HD.dat for active licenses…";
            Set<String> activeUsi = readActiveUsi(dir.resolve("HD.dat"));
            statusMsg = String.format("Found %,d active amateur licenses", activeUsi.size());
            log.info(statusMsg);

            // Phase 2 — build USI → operator class map from AM.dat
            statusMsg = "Reading AM.dat for operator class…";
            Map<String, String> usiClass = readOperatorClasses(dir.resolve("AM.dat"), activeUsi);

            // Phase 3 — stream EN.dat and bulk-insert into SQLite
            statusMsg = "Importing EN.dat into local database…";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                initSchema(conn);
                conn.setAutoCommit(false);
                streamEntities(conn, dir.resolve("EN.dat"), activeUsi, usiClass);
                conn.commit();
            }

            statusMsg = String.format("Imported %,d records — running maintenance…", imported.get());
            log.info("FCC ULS import done: {} records — running maintenance", imported.get());
            DbMaintenance.run(dbPath);

            statusMsg = String.format("FCC import complete — %,d records", imported.get());
            status = Status.DONE;
            log.info("FCC ULS import + maintenance complete");

        } catch (Exception e) {
            statusMsg = "Import failed: " + e.getMessage();
            status = Status.FAILED;
            log.error("FCC ULS import failed", e);
        } finally {
            running.set(false);
        }
    }

    /**
     * Reads HD.dat and returns USIs for active amateur-service licenses (HA / HV, status A).
     * Field layout: [0]=type [1]=USI [4]=call [5]=status [6]=service
     */
    private Set<String> readActiveUsi(Path hdPath) throws IOException {
        if (!Files.exists(hdPath)) throw new IOException("HD.dat not found at " + hdPath);
        Set<String> active = new HashSet<>(900_000);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(hdPath), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f.length < 7) continue;
                String svc = f[6].trim();
                String sta = f[5].trim();
                if (("HA".equals(svc) || "HV".equals(svc)) && "A".equals(sta))
                    active.add(f[1].trim());
            }
        }
        return active;
    }

    /**
     * Reads AM.dat and returns a map of USI → expanded license class for active USIs.
     * Field layout: [0]=type [1]=USI [4]=call [5]=class
     */
    private Map<String, String> readOperatorClasses(Path amPath, Set<String> activeUsi) throws IOException {
        Map<String, String> map = new HashMap<>(activeUsi.size());
        if (!Files.exists(amPath)) {
            log.warn("AM.dat not found — operator class will not be set");
            return map;
        }
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(Files.newInputStream(amPath), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f.length < 6) continue;
                String usi = f[1].trim();
                if (activeUsi.contains(usi))
                    map.put(usi, HamDbProvider.classExpand(f[5].trim()));
            }
        }
        return map;
    }

    /**
     * Streams EN.dat and bulk-inserts into the callsigns table.
     * Commits every 5,000 rows to keep memory low and give progress updates.
     * Field layout: [0]=EN [1]=USI [4]=call [7]=entity_name [8]=first [10]=last
     *               [14]=email [15]=addr1 [16]=city [17]=state [18]=zip
     */
    private void streamEntities(Connection conn, Path enPath,
                                Set<String> activeUsi, Map<String, String> usiClass) throws Exception {
        if (!Files.exists(enPath)) throw new IOException("EN.dat not found at " + enPath);

        String sql = "INSERT OR REPLACE INTO callsigns "
            + "(callsign,name,first_name,last_name,addr1,city,state,zip,country,"
            + " license_class,email,source,imported_at) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";

        String now = Instant.now().toString();
        int batch = 0;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             BufferedReader br = new BufferedReader(
                 new InputStreamReader(Files.newInputStream(enPath), StandardCharsets.ISO_8859_1))) {

            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\\|", -1);
                if (f.length < 19 || !"EN".equals(f[0].trim())) continue;
                String usi = f[1].trim();
                if (!activeUsi.contains(usi)) continue;

                String call = f[4].trim();
                if (call.isEmpty()) continue;

                String entityName = f[7].trim();
                String firstName  = f[8].trim();
                String lastName   = f[10].trim();
                String fullName   = entityName.isEmpty()
                    ? (firstName + " " + lastName).trim()
                    : entityName;

                ps.setString(1,  call);
                ps.setString(2,  fullName);
                ps.setString(3,  firstName);
                ps.setString(4,  lastName);
                ps.setString(5,  f[15].trim());                      // addr1
                ps.setString(6,  f[16].trim());                      // city
                ps.setString(7,  f[17].trim());                      // state
                ps.setString(8,  f[18].trim());                      // zip
                ps.setString(9,  "USA");
                ps.setString(10, usiClass.getOrDefault(usi, ""));
                ps.setString(11, f.length > 14 ? f[14].trim() : ""); // email
                ps.setString(12, "fcc");
                ps.setString(13, now);
                ps.addBatch();

                if (++batch % 5_000 == 0) {
                    ps.executeBatch();
                    conn.commit();
                    imported.set(batch);
                    statusMsg = String.format("Imported %,d records…", batch);
                }
            }
            ps.executeBatch();
            imported.set(batch);
        }
    }

    // ── Schema ─────────────────────────────────────────────────────────────────

    static void initSchema(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(
                "CREATE TABLE IF NOT EXISTS callsigns (" +
                "  callsign        TEXT PRIMARY KEY," +
                "  name            TEXT," +
                "  first_name      TEXT," +
                "  last_name       TEXT," +
                "  addr1           TEXT," +
                "  city            TEXT," +
                "  state           TEXT," +
                "  zip             TEXT," +
                "  country         TEXT," +
                "  lat             REAL DEFAULT 0," +
                "  lon             REAL DEFAULT 0," +
                "  grid            TEXT," +
                "  license_class   TEXT," +
                "  license_expires TEXT," +
                "  email           TEXT," +
                "  source          TEXT," +
                "  imported_at     TEXT" +
                ")");
            st.execute("CREATE INDEX IF NOT EXISTS idx_cs ON callsigns(callsign)");
        }
    }
}
