package com.ars.fx.surface;

import com.ars.fx.data.ContestLogs;
import com.jlog.model.QsoRecord;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** The log browser's contest view: the Log list wording, the Band / Mode filter and the search. */
class LogBrowserContestTest {
    private static ContestLogs.Info info(String name, String file, int n, boolean current, boolean archived) {
        return new ContestLogs.Info(Path.of(file), "CQWW_SSB", name, "2026-11-28", n, current, false, archived);
    }

    @Test
    void logListLabels() {
        assertEquals("CQ WW SSB · nov.db · 412 QSOs", LogBrowser.sourceLabel(info("CQ WW SSB", "nov.db", 412, false, false)));
        assertEquals("CQ WW SSB · nov.db · 1 QSO (in use)", LogBrowser.sourceLabel(info("CQ WW SSB", "nov.db", 1, true, false)));
        assertEquals("CQ WW SSB · nov.db · 0 QSOs (archived)", LogBrowser.sourceLabel(info("CQ WW SSB", "nov.db", 0, false, true)));
        assertEquals("old.db · old.db · 3 QSOs", LogBrowser.sourceLabel(info("", "old.db", 3, false, false)), "no contest name: the file name stands in");
    }

    @Test
    void bandAndModeFilter() {
        assertTrue(LogBrowser.pick(null, "All bands", "20m"));
        assertTrue(LogBrowser.pick("All bands", "All bands", "20m"));
        assertTrue(LogBrowser.pick("20m", "All bands", "20M"), "not case sensitive");
        assertFalse(LogBrowser.pick("40m", "All bands", "20m"));
        assertFalse(LogBrowser.pick("40m", "All bands", null), "a QSO with no band is not on 40m");
        assertTrue(LogBrowser.pick("All modes", "All modes", null));
    }

    @Test
    void searchFindsCallSerialExchangeOperatorAndNotes() {
        QsoRecord q = new QsoRecord();
        q.setCallsign("dl1aa"); q.setSerialSent("12"); q.setSerialReceived("101");
        q.setContestField1("14"); q.setContestField2("DL");
        q.setOperator("W1OP"); q.setNotes("big signal");
        String h = LogBrowser.contestHaystack(q);
        for (String needle : new String[]{"dl1aa", "101", "14 dl", "w1op", "big signal"}) assertTrue(h.contains(needle), needle + " in: " + h);
        assertFalse(h.contains("zz9"));
        assertDoesNotThrow(() -> LogBrowser.contestHaystack(new QsoRecord()), "a QSO with nothing filled in");
    }
}
