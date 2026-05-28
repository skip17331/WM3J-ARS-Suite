package com.hamradio.jhub.callsign;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the 2026-05-28 CSV charset auto-detection fix. Before the fix,
 * CsvImporter opened the file with {@code InputStreamReader(stream, UTF_8)},
 * which defaults to {@code CodingErrorAction.REPLACE} — non-UTF-8 bytes
 * (typical of HamCall, Buckmaster, and other Windows-native callsign DBs
 * exported as cp1252) were silently substituted with U+FFFD. The import
 * "succeeded" but stored mangled city / name fields. Now we sniff the
 * first 64 KB and fall back to Windows-1252 transparently.
 *
 * Tests run the importer end-to-end against a temp SQLite DB to exercise
 * the same path production uses.
 */
class CsvImporterCharsetTest {

    /**
     * Reset the singleton between tests. Without this, test N+1's
     * waitForStatus returns immediately on test N's stale DONE before
     * test N+1's daemon thread has even started writing its db.
     */
    @BeforeEach
    void resetSingleton() throws Exception {
        Field statusField = CsvImporter.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(CsvImporter.getInstance(), CsvImporter.Status.IDLE);

        Field runningField = CsvImporter.class.getDeclaredField("running");
        runningField.setAccessible(true);
        ((AtomicBoolean) runningField.get(CsvImporter.getInstance())).set(false);
    }

    @Test
    void cp1252CsvImportsWithCorrectAccentedCharacters(@TempDir Path dir) throws Exception {
        // Tiny CSV with a header + one row containing "München" in city. In
        // cp1252 'ü' is the single byte 0xFC; in UTF-8 it is 0xC3 0xBC. A
        // strict-UTF-8 reader would see 0xFC, fail to decode it as a UTF-8
        // start byte, and (with the prior REPLACE default) substitute U+FFFD
        // — the stored city would be "M�nchen".
        String csvText = "callsign,city\nDL5XYZ,München\n";
        byte[] cp1252Bytes = csvText.getBytes(Charset.forName("windows-1252"));
        // Confirm the test setup actually produced a cp1252 byte sequence.
        assertTrue(containsByte(cp1252Bytes, (byte) 0xFC),
            "test setup must produce the cp1252 byte 0xFC for 'ü'");
        assertFalse(containsSequence(cp1252Bytes, new byte[]{(byte) 0xC3, (byte) 0xBC}),
            "test setup must NOT contain the UTF-8 byte sequence for 'ü'");

        Path csv = dir.resolve("hamcall-cp1252.csv");
        Files.write(csv, cp1252Bytes);

        Path db = dir.resolve("callsigns.db");
        boolean started = CsvImporter.getInstance().start(csv.toString(), db.toString());
        assertTrue(started, "import must start");
        waitForStatus(CsvImporter.Status.DONE, CsvImporter.Status.FAILED);
        assertEquals(CsvImporter.Status.DONE, CsvImporter.getInstance().getStatus(),
            "import must complete: " + CsvImporter.getInstance().getStatusMsg());

        // Verify the row landed with the correct character — not a U+FFFD.
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
             PreparedStatement ps = c.prepareStatement("SELECT city FROM callsigns WHERE callsign=?")) {
            ps.setString(1, "DL5XYZ");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "imported row must exist");
                String city = rs.getString("city");
                assertEquals("München", city,
                    "cp1252 'ü' must round-trip as U+00FC, not as U+FFFD replacement");
            }
        }
    }

    @Test
    void utf8CsvStillImportsCorrectly(@TempDir Path dir) throws Exception {
        // Same logical content but encoded as UTF-8 — the sniff should
        // pick UTF-8 and read normally.
        String csvText = "callsign,city\nDL5XYZ,München\n";
        Path csv = dir.resolve("modern-utf8.csv");
        Files.write(csv, csvText.getBytes(StandardCharsets.UTF_8));

        Path db = dir.resolve("callsigns.db");
        boolean started = CsvImporter.getInstance().start(csv.toString(), db.toString());
        assertTrue(started);
        waitForStatus(CsvImporter.Status.DONE, CsvImporter.Status.FAILED);
        assertEquals(CsvImporter.Status.DONE, CsvImporter.getInstance().getStatus(),
            "UTF-8 import must succeed: " + CsvImporter.getInstance().getStatusMsg());

        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db);
             PreparedStatement ps = c.prepareStatement("SELECT city FROM callsigns WHERE callsign=?")) {
            ps.setString(1, "DL5XYZ");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("München", rs.getString("city"));
            }
        }
    }

    private static void waitForStatus(CsvImporter.Status... terminal) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < deadline) {
            CsvImporter.Status s = CsvImporter.getInstance().getStatus();
            for (CsvImporter.Status t : terminal) {
                if (s == t) return;
            }
            Thread.sleep(25);
        }
        throw new AssertionError("CSV import did not finish within 5s; status="
            + CsvImporter.getInstance().getStatus()
            + " msg=" + CsvImporter.getInstance().getStatusMsg());
    }

    private static boolean containsByte(byte[] haystack, byte needle) {
        for (byte b : haystack) if (b == needle) return true;
        return false;
    }

    private static boolean containsSequence(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
