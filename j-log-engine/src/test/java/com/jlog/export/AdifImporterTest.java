package com.jlog.export;

import com.jlog.db.DatabaseManager;
import com.jlog.db.QsoDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end check that AdifImporter's three DupeMode branches each touch
 * exactly one counter and leave the DB in the expected state. Specifically
 * pins the 2026-05-12 OVERWRITE fix: re-importing the same ADIF in OVERWRITE
 * mode must NOT accumulate dupes (the pre-fix bug counted as "overwritten"
 * but actually appended a fresh row).
 */
class AdifImporterTest {

    private static final String SAMPLE_ADIF = """
            Test header
            <ADIF_VER:5>3.1.4
            <EOH>
            <CALL:5>K1ABC<BAND:3>20m<MODE:2>CW<QSO_DATE:8>20260101<TIME_ON:4>1200<EOR>
            <CALL:5>JA1XY<BAND:3>15m<MODE:3>SSB<QSO_DATE:8>20260101<TIME_ON:4>1230<EOR>
            <CALL:4>DL5Z<BAND:3>40m<MODE:3>FT8<QSO_DATE:8>20260101<TIME_ON:4>1245<EOR>
            """;

    @BeforeAll
    static void initDb(@TempDir Path tmpHome) throws Exception {
        // Redirect ~/.j-log into a JUnit temp dir so the user's real log is
        // untouched. user.home is read in DatabaseManager.initAll().
        System.setProperty("user.home", tmpHome.toString());
        DatabaseManager.getInstance().initAll();
    }

    @BeforeEach
    void wipeQso() throws Exception {
        // Each test starts with an empty qso table.
        try (Statement st = DatabaseManager.getInstance().getLogConnection().createStatement()) {
            st.executeUpdate("DELETE FROM qso");
        }
    }

    private static Path writeAdif(Path dir, String name) throws IOException {
        Path p = dir.resolve(name);
        Files.writeString(p, SAMPLE_ADIF);
        return p;
    }

    @Test
    void firstImportInsertsAll(@TempDir Path dir) throws Exception {
        Path adif = writeAdif(dir, "fresh.adi");
        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);
        assertEquals(3, r.imported);
        assertEquals(0, r.skipped);
        assertEquals(0, r.overwritten);
        assertEquals(3, QsoDao.getInstance().count());
    }

    @Test
    void skipModeLeavesDbUnchangedOnReimport(@TempDir Path dir) throws Exception {
        Path adif = writeAdif(dir, "skip.adi");
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);   // seed: 3 rows
        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);
        assertEquals(0, r.imported);
        assertEquals(3, r.skipped);
        assertEquals(0, r.overwritten);
        assertEquals(3, QsoDao.getInstance().count(), "SKIP must not insert new rows");
    }

    @Test
    void overwriteModeReplacesInPlaceNoDupes(@TempDir Path dir) throws Exception {
        Path adif = writeAdif(dir, "ow.adi");
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);   // seed: 3 rows
        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.OVERWRITE);
        assertEquals(0, r.imported, "OVERWRITE bumps r.overwritten, not r.imported");
        assertEquals(3, r.overwritten);
        assertEquals(0, r.skipped);
        assertEquals(3, QsoDao.getInstance().count(),
            "OVERWRITE must keep row count at 3 — the pre-fix bug made it 6");
    }

    @Test
    void overwriteCollapsesAccumulatedAppendDupes(@TempDir Path dir) throws Exception {
        Path adif = writeAdif(dir, "collapse.adi");
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.APPEND); // 3 rows
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.APPEND); // 6 rows
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.APPEND); // 9 rows
        assertEquals(9, QsoDao.getInstance().count());

        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.OVERWRITE);
        assertEquals(3, r.overwritten);
        assertEquals(3, QsoDao.getInstance().count(),
            "OVERWRITE must wipe all matching dupes and leave exactly one row per key");
    }

    @Test
    void appendModeAddsDupesAndCountsImports(@TempDir Path dir) throws Exception {
        Path adif = writeAdif(dir, "ap.adi");
        AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);   // seed: 3 rows
        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.APPEND);
        assertEquals(3, r.imported);
        assertEquals(0, r.skipped);
        assertEquals(0, r.overwritten);
        assertEquals(6, QsoDao.getInstance().count(), "APPEND legitimately duplicates");
    }

    @Test
    void missingCallRecordIsRecordedAsFailure(@TempDir Path dir) throws Exception {
        Path adif = dir.resolve("bad.adi");
        Files.writeString(adif, """
                <EOH>
                <BAND:3>20m<MODE:2>CW<EOR>
                """);
        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);
        assertEquals(0, r.imported);
        assertEquals(1, r.failed);
        assertEquals(1, r.failures.size());
        AdifImporter.RejectedRecord rej = r.failures.get(0);
        assertEquals(1, rej.recordNumber);
        assertEquals("Missing CALL in record", rej.reason);
        // Parsed fields preserved so the Rejected QSOs dialog can pre-populate
        // the editor with the BAND / MODE that were present.
        assertEquals("20m", rej.fields.get("BAND"));
        assertEquals("CW",  rej.fields.get("MODE"));
        // Partial QsoRecord has band/mode but blank callsign.
        assertEquals("20m", rej.partial.getBand());
        assertEquals("CW",  rej.partial.getMode());
        assertTrue(rej.partial.getCallsign() == null || rej.partial.getCallsign().isBlank());
    }

    /**
     * Full round-trip through the file format: insert records, export to a
     * fresh .adi via {@link AdifExporter}, wipe the table, re-import via
     * {@link AdifImporter}, and verify every row came back. Catches any
     * future regression where export and import disagree on the field set,
     * charset, or quoting — including the 2026-05-28 export-charset lock
     * (UTF-8) and the import cp1252 fallback combined.
     */
    @Test
    void roundTripExportImportPreservesAllRecordsIncludingNonAscii(@TempDir Path dir) throws Exception {
        // Seed three rows: one plain, one with a UTF-8 non-ASCII char in
        // notes, one with smart quotes in country name.
        com.jlog.model.QsoRecord a = new com.jlog.model.QsoRecord();
        a.setCallsign("K1ABC"); a.setBand("20m"); a.setMode("CW");
        a.setDateTimeUtc(java.time.LocalDateTime.of(2026, 1, 1, 12, 0));

        com.jlog.model.QsoRecord b = new com.jlog.model.QsoRecord();
        b.setCallsign("XE1AB"); b.setBand("15m"); b.setMode("SSB");
        b.setDateTimeUtc(java.time.LocalDateTime.of(2026, 1, 1, 12, 15));
        b.setNotes("Operator André, México");

        com.jlog.model.QsoRecord c = new com.jlog.model.QsoRecord();
        c.setCallsign("OH2XX"); c.setBand("40m"); c.setMode("FT8");
        c.setDateTimeUtc(java.time.LocalDateTime.of(2026, 1, 1, 12, 30));
        c.setCountry("Åland Islands");

        QsoDao.getInstance().insert(a);
        QsoDao.getInstance().insert(b);
        QsoDao.getInstance().insert(c);
        assertEquals(3, QsoDao.getInstance().count());

        Path out = dir.resolve("round-trip.adi");
        AdifExporter.exportAdif(out, java.util.List.of(a, b, c), false);

        try (Statement st = DatabaseManager.getInstance().getLogConnection().createStatement()) {
            st.executeUpdate("DELETE FROM qso");
        }
        assertEquals(0, QsoDao.getInstance().count());

        AdifImporter.Result r = AdifImporter.importAdif(out, AdifImporter.DupeMode.APPEND);
        assertEquals(3, r.imported, "round-trip must load every exported row");
        assertEquals(0, r.failed);
        assertEquals(3, QsoDao.getInstance().count());
    }

    /**
     * Pins the 2026-05-28 charset-fallback fix. HRD/Logger32/N1MM on Windows
     * write ADIF in Windows-1252; a strict UTF-8 read threw
     * {@code MalformedInputException: Input length = N} on any cp1252 byte
     * (smart quotes in comments, accented chars in names, etc.) and the
     * import silently bombed. importAdif() must transparently fall back to
     * cp1252 and load the rows.
     */
    @Test
    void cp1252AdifImportsWithoutCharsetError(@TempDir Path dir) throws Exception {
        // 0xE9 is "é" in cp1252; it's also the start byte of a 3-byte UTF-8
        // sequence, so a strict UTF-8 decoder throws MalformedInputException
        // when the following bytes aren't valid continuation bytes.
        String cp1252Text = """
                Operator: André
                <ADIF_VER:5>3.1.4
                <EOH>
                <CALL:5>K1ABC<BAND:3>20m<MODE:2>CW<QSO_DATE:8>20260101<TIME_ON:4>1200<COMMENT:14>QSO with André<EOR>
                """;
        Path adif = dir.resolve("cp1252.adi");
        Files.write(adif, cp1252Text.getBytes(Charset.forName("windows-1252")));

        AdifImporter.Result r = AdifImporter.importAdif(adif, AdifImporter.DupeMode.SKIP);
        assertEquals(1, r.imported, "cp1252 ADIF must import, not blow up");
        assertEquals(0, r.failed);
    }
}
