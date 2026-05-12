package com.jlog.export;

import com.jlog.db.DatabaseManager;
import com.jlog.db.QsoDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(List.of("Missing CALL in record"), r.failures);
    }
}
