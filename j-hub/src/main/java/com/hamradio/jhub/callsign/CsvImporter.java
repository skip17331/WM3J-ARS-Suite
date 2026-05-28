package com.hamradio.jhub.callsign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generic CSV importer for the local callsign database.
 *
 * Accepts a CSV file in UTF-8 or Windows-1252 (auto-detected by sniffing
 * the first 64 KB; HamCall, Buckmaster, and other Windows-native callsign
 * databases ship as cp1252). Header row required. Column names are matched
 * case-insensitively; unrecognized columns are silently ignored.
 *
 * Recognized column names:
 *   callsign, name, first_name (or fname), last_name (or lname),
 *   addr1 (or address, street), city, state, zip (or postal_code),
 *   country, lat (or latitude), lon (or longitude, lng),
 *   grid (or gridsquare, maidenhead),
 *   license_class (or class, oper_class),
 *   license_expires (or expires, expiry),
 *   email
 *
 * Use this to import from HamCall, Buckmaster, or any other tabular source
 * you have converted to CSV.  POST {"csvPath":"/path/to/file.csv"} to
 * /api/callsign/db/import/csv.
 */
public class CsvImporter {

    private static final Logger log = LoggerFactory.getLogger(CsvImporter.class);

    public enum Status { IDLE, RUNNING, DONE, FAILED }

    private volatile Status status      = Status.IDLE;
    private volatile String statusMsg   = "Not started";
    private final AtomicLong imported   = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private static final CsvImporter INSTANCE = new CsvImporter();
    public static CsvImporter getInstance() { return INSTANCE; }
    private CsvImporter() {}

    public Status  getStatus()    { return status; }
    public String  getStatusMsg() { return statusMsg; }
    public long    getImported()  { return imported.get(); }
    public boolean isRunning()    { return running.get(); }

    /** Starts an async import. Returns false if one is already running. */
    public boolean start(String csvPath, String dbPath) {
        if (!running.compareAndSet(false, true)) return false;
        Thread t = new Thread(() -> doImport(csvPath, dbPath), "jhub-csv-import");
        t.setDaemon(true);
        t.start();
        return true;
    }

    // ── Import ─────────────────────────────────────────────────────────────────

    private void doImport(String csvPath, String dbPath) {
        imported.set(0);
        status = Status.RUNNING;
        try {
            Path file = Path.of(csvPath);
            if (!Files.exists(file)) throw new IOException("CSV file not found: " + csvPath);

            log.info("CSV import starting from {}", csvPath);
            statusMsg = "Reading CSV header…";

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
                FccUlsImporter.initSchema(conn);
                conn.setAutoCommit(false);
                streamCsv(conn, file);
                conn.commit();
            }

            statusMsg = String.format("CSV import complete — %,d records", imported.get());
            status = Status.DONE;
            log.info("CSV import done: {} records", imported.get());

        } catch (Exception e) {
            statusMsg = "Import failed: " + e.getMessage();
            status = Status.FAILED;
            log.error("CSV import failed", e);
        } finally {
            running.set(false);
        }
    }

    private void streamCsv(Connection conn, Path file) throws Exception {
        String sql = "INSERT OR REPLACE INTO callsigns "
            + "(callsign,name,first_name,last_name,addr1,city,state,zip,country,"
            + " lat,lon,grid,license_class,license_expires,email,source,imported_at) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        String now = Instant.now().toString();
        int batch = 0;

        Charset csvCharset = detectCharset(file);
        if (csvCharset != StandardCharsets.UTF_8) {
            log.info("CSV file {} not valid UTF-8; decoding as {}", file, csvCharset);
        }
        try (BufferedReader br = Files.newBufferedReader(file, csvCharset);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String headerLine = br.readLine();
            if (headerLine == null) throw new IOException("CSV file is empty");

            String[] headers = splitCsv(headerLine);
            Map<String, Integer> colIdx = buildColMap(headers);

            if (!colIdx.containsKey("callsign"))
                throw new IOException("CSV must have a 'callsign' column");

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = splitCsv(line);

                String call = get(cols, colIdx, "callsign").toUpperCase();
                if (call.isEmpty()) continue;

                ps.setString(1,  call);
                ps.setString(2,  get(cols, colIdx, "name"));
                ps.setString(3,  getAny(cols, colIdx, "first_name", "fname"));
                ps.setString(4,  getAny(cols, colIdx, "last_name",  "lname"));
                ps.setString(5,  getAny(cols, colIdx, "addr1", "address", "street"));
                ps.setString(6,  get(cols, colIdx, "city"));
                ps.setString(7,  get(cols, colIdx, "state"));
                ps.setString(8,  getAny(cols, colIdx, "zip", "postal_code"));
                ps.setString(9,  get(cols, colIdx, "country"));
                ps.setDouble(10, getDouble(cols, colIdx, "lat", "latitude"));
                ps.setDouble(11, getDouble(cols, colIdx, "lon", "longitude", "lng"));
                ps.setString(12, getAny(cols, colIdx, "grid", "gridsquare", "maidenhead"));
                ps.setString(13, getAny(cols, colIdx, "license_class", "class", "oper_class"));
                ps.setString(14, getAny(cols, colIdx, "license_expires", "expires", "expiry"));
                ps.setString(15, get(cols, colIdx, "email"));
                ps.setString(16, "csv");
                ps.setString(17, now);
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

    // ── CSV / column helpers ───────────────────────────────────────────────────

    /** Splits a CSV line respecting double-quoted fields. */
    static String[] splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // doubled quote inside quoted field → literal quote
                if (inQuote && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"'); i++;
                } else {
                    inQuote = !inQuote;
                }
            } else if (c == ',' && !inQuote) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    private static Map<String, Integer> buildColMap(String[] headers) {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            m.put(h, i);
        }
        return m;
    }

    private static String get(String[] cols, Map<String, Integer> idx, String key) {
        Integer i = idx.get(key);
        if (i == null || i >= cols.length) return "";
        return cols[i].trim();
    }

    private static String getAny(String[] cols, Map<String, Integer> idx, String... keys) {
        for (String k : keys) {
            String v = get(cols, idx, k);
            if (!v.isEmpty()) return v;
        }
        return "";
    }

    private static double getDouble(String[] cols, Map<String, Integer> idx, String... keys) {
        String v = getAny(cols, idx, keys);
        if (v.isEmpty()) return 0.0;
        try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0.0; }
    }

    /**
     * Sniff the first 64 KB of the file to decide between UTF-8 (modern
     * exports) and Windows-1252 (HamCall, Buckmaster, other Windows-native
     * callsign DBs). Streaming the rest with the wrong charset would either
     * substitute U+FFFD for every cp1252 byte (the prior strict-UTF-8 +
     * REPLACE default; silently mangled "Müller" / "São Paulo" / etc.) or
     * mojibake every UTF-8 multibyte sequence. The sniff window is large
     * enough that any non-ASCII byte in a typical callsign-DB layout
     * (city, name) will appear before the cutoff.
     */
    private static Charset detectCharset(Path file) throws IOException {
        byte[] sniff;
        try (InputStream in = Files.newInputStream(file)) {
            sniff = in.readNBytes(64 * 1024);
        }
        try {
            StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(sniff));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException notUtf8) {
            return Charset.forName("windows-1252");
        }
    }
}
