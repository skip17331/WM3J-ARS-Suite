package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Exporting a contest log from the log browser: ADIF with the contest fields, or CSV. */
class LogExportContestTest {

    private static QsoRecord qso(String call, String band, String mode, String khz, String snSent, String snRcvd, String f1, String f2, int pts, boolean dupe) {
        QsoRecord q = new QsoRecord();
        q.setCallsign(call); q.setBand(band); q.setMode(mode); q.setFrequency(khz);
        q.setSerialSent(snSent); q.setSerialReceived(snRcvd);
        q.setContestField1(f1); q.setContestField2(f2);
        q.setRstSent("59"); q.setRstReceived("57");
        q.setPoints(pts); q.setDupe(dupe);
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 28, 14, 30, 5));
        return q;
    }

    private final List<QsoRecord> rows = List.of(
            qso("DL1AA", "20m", "USB", "14250", "12", "101", "14", "DL", 3, false),
            qso("K1BB", "40m", "CW", null, "13", "102", "05", "", 0, true));

    @Test
    void exchangeJoinsTheFilledFields() {
        assertEquals("14 DL", LogExport.exchange(rows.get(0)));
        assertEquals("05", LogExport.exchange(rows.get(1)));
        assertEquals("", LogExport.exchange(new QsoRecord()));
    }

    @Test
    void adifCarriesTheContestFields() {
        String s = LogExport.toAdifContest(rows, "CQ-WW-SSB");
        assertEquals(2, s.split("<EOR>", -1).length - 1);
        assertTrue(s.indexOf("<CALL:5>DL1AA") < s.indexOf("<CALL:4>K1BB"), "the order of the view is kept");
        assertTrue(s.contains("<QSO_DATE:8>20261128"));
        assertTrue(s.contains("<TIME_ON:6>143005"));
        assertTrue(s.contains("<BAND:3>20m"));
        assertTrue(s.contains("<MODE:3>SSB") && s.contains("<SUBMODE:3>USB"), "USB is SSB with a submode");
        assertTrue(s.contains("<FREQ:6>14.250"), "stored in kHz, written in MHz");
        assertTrue(s.contains("<STX:2>12") && s.contains("<SRX:3>101"));
        assertTrue(s.contains("<SRX_STRING:5>14 DL"));
        assertTrue(s.contains("<CONTEST_ID:9>CQ-WW-SSB"));
        assertTrue(s.contains("<RST_SENT:2>59") && s.contains("<RST_RCVD:2>57"));
    }

    @Test
    void aQsoWithNoFrequencyOrContestIdLeavesThoseOut() {
        String s = LogExport.toAdifContest(List.of(rows.get(1)), null);
        assertFalse(s.contains("<FREQ"), "no frequency was logged");
        assertFalse(s.contains("<CONTEST_ID"));
        assertTrue(s.contains("<CALL:4>K1BB"));
    }

    @Test
    void csvHasPointsAndDupeAndKilohertz() {
        List<String> lines = LogExport.toCsvContest(rows).lines().toList();
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).startsWith("Call,Date,Time,Band,Mode,Freq_kHz"));
        assertTrue(lines.get(1).startsWith("DL1AA,20261128,143005,20m,USB,14250,59,57,12,101,14 DL,3,,"), lines.get(1));
        assertTrue(lines.get(2).contains(",05,0,Y,"), "points 0 and the dupe flag: " + lines.get(2));
    }

    @Test
    void fileFormatFollowsTheExtension(@TempDir Path dir) throws Exception {
        LogExport.Result a = LogExport.exportContest(dir.resolve("a.adi"), rows, "CQ-WW-SSB");
        assertTrue(a.ok()); assertEquals(2, a.count());
        assertTrue(Files.readString(dir.resolve("a.adi")).contains("<EOR>"));
        LogExport.Result c = LogExport.exportContest(dir.resolve("deep/er/b.CSV"), rows, null);
        assertTrue(c.ok(), "the folder is created: " + c.error());
        assertTrue(Files.readString(dir.resolve("deep/er/b.CSV")).startsWith("Call,"));
        LogExport.Result none = LogExport.exportContest(dir.resolve("none.adi"), List.of(), null);
        assertTrue(none.ok()); assertEquals(0, none.count());
        Path blocker = dir.resolve("file"); Files.writeString(blocker, "x");
        assertFalse(LogExport.exportContest(blocker.resolve("x.adi"), rows, null).ok(), "a failed write is reported, not thrown");
    }
}
