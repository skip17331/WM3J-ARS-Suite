package com.ars.fx.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The log browser's "Export…": only the rows handed over are written, in that order, as ADIF or CSV by extension. */
class LogExportFilteredTest {

    private static JLogDb.QsoFull qso(long id, String when, String call, String band, String mode, String notes) {
        return new JLogDb.QsoFull(id, when, call, "14.074", band, mode, "59", "57", "Ann", "Boston", "FN42", "MA", "",
                100, notes, new JLogDb.Refs("", "", "K-0039", 5, 8, false, true, false));
    }

    private final List<JLogDb.QsoFull> rows = List.of(
            qso(2, "2026-11-10 14:30:00", "W1AW", "20m", "FT8", "tnx, 73"),
            qso(1, "2026-11-09 09:05:00", "K1ABC", "40m", "CW", ""));

    @Test
    void adifHoldsOnlyTheGivenRowsInOrder(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("out.adi");
        LogExport.Result r = LogExport.exportFiltered(f, rows);
        assertTrue(r.ok());
        assertEquals(2, r.count());
        String s = Files.readString(f);
        assertEquals(2, s.split("<EOR>", -1).length - 1);
        assertTrue(s.indexOf("<CALL:4>W1AW") < s.indexOf("<CALL:5>K1ABC"), "order of the view is kept");
        assertTrue(s.contains("<QSO_DATE:8>20261110"));
        assertTrue(s.contains("<POTA_REF:6>K-0039"));
        assertTrue(s.contains("<LOTW_QSL_RCVD:1>Y"));
        assertTrue(s.contains("<GRIDSQUARE:4>FN42"));
        assertTrue(s.contains("<QTH:6>Boston"));
    }

    @Test
    void csvByExtensionQuotesCommas(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("out.CSV");
        assertTrue(LogExport.exportFiltered(f, rows).ok());
        List<String> lines = Files.readAllLines(f);
        assertEquals(3, lines.size());                       // header + 2 rows
        assertTrue(lines.get(0).startsWith("Call,Date,Time"));
        assertTrue(lines.get(1).startsWith("W1AW,20261110,143000,20m,FT8"));
        assertTrue(lines.get(1).contains("\"tnx, 73\""));
    }

    @Test
    void emptyViewWritesAnEmptyDocument(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("none.adi");
        LogExport.Result r = LogExport.exportFiltered(f, List.of());
        assertTrue(r.ok());
        assertEquals(0, r.count());
        assertFalse(Files.readString(f).contains("<EOR>"));
    }

    @Test
    void createsMissingFolderAndReportsFailure(@TempDir Path dir) throws Exception {
        assertTrue(LogExport.exportFiltered(dir.resolve("a/b/c.adi"), rows).ok());
        Path blocker = dir.resolve("file");
        Files.writeString(blocker, "x");
        LogExport.Result bad = LogExport.exportFiltered(blocker.resolve("x.adi"), rows);   // parent is a file
        assertFalse(bad.ok());
        assertNotNull(bad.error());
    }

    @Test
    void fromFullCopiesEveryField() {
        JLogDb.QsoExport e = LogExport.fromFull(rows.get(0));
        assertEquals("W1AW", e.call());
        assertEquals("Boston", e.qth());
        assertEquals("Ann", e.name());
        assertEquals("FN42", e.grid());
        assertEquals(100, e.powerW());
        assertEquals("K-0039", e.refs().pota());
    }
}
