package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfirmSyncTest {
    private String oldHome;

    @BeforeEach void home(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home");
        System.setProperty("user.home", tmp.toString());
        HubConfig.reload();
        ConfirmSync.setHttpForTest(null);
        JLogDb.insertAt("2026-01-02 15:30:00", "K1ABC", "14.250", "20m", "SSB", "59", "59", "", "", "", "", "", "", 0, JLogDb.Refs.NONE);
        JLogDb.insertAt("2026-01-03 10:00:00", "JA1XYZ", "7.030", "40m", "CW", "599", "599", "", "", "", "", "", "", 0, JLogDb.Refs.NONE);
        JLogDb.insertAt("2026-01-04 11:00:00", "DL1ABC", "14.200", "20m", "USB", "59", "59", "", "", "", "", "", "", 0, JLogDb.Refs.NONE);   // logged as USB
    }
    @AfterEach void restore() { ConfirmSync.setHttpForTest(null); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    private static String f(String name, String value) { return "<" + name + ":" + value.length() + ">" + value + " "; }
    private static String rec(String call, String date, String time, String band, String mode) {
        return f("CALL", call) + f("QSO_DATE", date) + f("TIME_ON", time) + f("BAND", band) + f("MODE", mode) + f("QSL_RCVD", "Y") + "<EOR>\n";
    }
    private static JLogDb.QsoExport find(String call) {
        return JLogDb.allForExport().stream().filter(q -> q.call().equalsIgnoreCase(call)).findFirst().orElseThrow();
    }

    static String lotwFile() {
        return "ARRL Logbook of the World Status Report\n" + f("APP_LoTW_LASTQSL", "2026-02-01 12:00:00") + "<EOH>\n"
                + rec("K1ABC", "20260102", "153000", "20m", "SSB")
                + rec("JA1XYZ", "20260103", "100000", "40m", "CW")
                + rec("DL1ABC", "20260104", "110000", "20m", "SSB")        // the log says USB
                + rec("W9ZZZ", "20260105", "120000", "20m", "SSB");        // not in the log
    }

    @Test void lotwConfirmationsAreTickedAsLotwNeverAsPaperQsl() {
        ConfirmSync.Result r = ConfirmSync.applyLotw(lotwFile());
        assertTrue(r.ok());
        assertEquals(4, r.received());
        assertEquals(3, r.matched(), "the USB QSO matches the SSB confirmation; W9ZZZ is not in the log");
        assertTrue(find("K1ABC").refs().lotw());
        assertFalse(find("K1ABC").refs().qsl(), "QSL_RCVD in a LoTW file means LoTW, not a paper card");
        assertFalse(find("K1ABC").refs().eqsl());
        assertTrue(find("DL1ABC").refs().lotw());
        assertEquals("2026-02-01 12:00:00", HubConfig.get("confirm.lotw.since", ""), "the next ask is only for what arrives after this");
        assertTrue(r.message().contains("1 are for contacts that are not in the log"));
    }

    @Test void eqslConfirmationsAreTickedAsEqsl() {
        String adif = "eQSL inbox\n<EOH>\n" + rec("JA1XYZ", "20260103", "100000", "40m", "CW");
        ConfirmSync.Result r = ConfirmSync.applyEqsl(adif, "202611051200");
        assertTrue(r.ok()); assertEquals(1, r.matched());
        assertTrue(find("JA1XYZ").refs().eqsl());
        assertFalse(find("JA1XYZ").refs().lotw());
        assertEquals("202611051200", HubConfig.get("confirm.eqsl.since", ""));
    }

    @Test void theLotwRequestCarriesTheLoginAndOnlyAsksForNewConfirmationsAfterTheFirstTime() {
        UploadConfig.setField("lotw", "weblogin", "WM3J");
        UploadConfig.setField("lotw", "webpass", "s3cret&pw");
        List<String> urls = new ArrayList<>();
        ConfirmSync.setHttpForTest(u -> { urls.add(u); return lotwFile(); });
        assertTrue(ConfirmSync.lotw().ok());
        assertTrue(urls.get(0).contains("login=WM3J") && urls.get(0).contains("qso_query=1") && urls.get(0).contains("qso_qsl=yes"));
        assertTrue(urls.get(0).contains("password=s3cret%26pw"), "special characters are encoded");
        assertFalse(urls.get(0).contains("qso_qslsince"), "the first ask is for everything");
        HubConfig.set("confirm.lotw.at", "0");                                   // pretend the quarter hour has passed
        assertTrue(ConfirmSync.lotw().ok());
        assertTrue(urls.get(1).contains("qso_qslsince=2026-02-01+12%3A00%3A00"));
    }

    @Test void askingAgainStraightAwayIsRefusedPolitely() {
        UploadConfig.setField("lotw", "weblogin", "WM3J"); UploadConfig.setField("lotw", "webpass", "pw");
        ConfirmSync.setHttpForTest(u -> lotwFile());
        assertTrue(ConfirmSync.lotw().ok());
        ConfirmSync.Result again = ConfirmSync.lotw();
        assertFalse(again.ok());
        assertTrue(again.message().contains("try again in"));
    }

    @Test void missingLoginsSayWhatToEnter() {
        assertFalse(ConfirmSync.lotw().ok());
        assertTrue(ConfirmSync.lotw().message().contains("LoTW website login"));
        assertFalse(ConfirmSync.eqsl().ok());
    }

    @Test void aBadLoginPageIsReportedNotImported() {
        UploadConfig.setField("lotw", "weblogin", "WM3J"); UploadConfig.setField("lotw", "webpass", "wrong");
        ConfirmSync.setHttpForTest(u -> "<html><body><p>Username/password incorrect</p></body></html>");
        ConfirmSync.Result r = ConfirmSync.lotw();
        assertFalse(r.ok());
        assertTrue(r.message().contains("Username/password incorrect"));
        assertFalse(find("K1ABC").refs().lotw());
    }

    @Test void aPasswordNeverAppearsInAnErrorMessage() {
        UploadConfig.setField("lotw", "weblogin", "WM3J"); UploadConfig.setField("lotw", "webpass", "hunter2");
        ConfirmSync.setHttpForTest(u -> { throw new RuntimeException("failed for " + u); });
        String msg = ConfirmSync.lotw().message();
        assertFalse(msg.contains("hunter2"), msg);
    }

    @Test void eqslFollowsTheLinkToTheFileItMade() {
        UploadConfig.setField("eqsl", "user", "WM3J"); UploadConfig.setField("eqsl", "password", "pw");
        List<String> urls = new ArrayList<>();
        ConfirmSync.setHttpForTest(u -> {
            urls.add(u);
            return u.contains("DownloadInBox") ? "<html>Your ADIF file: <A HREF=\"../downloadedfiles/WM3J.adi\">download</A></html>"
                    : "eQSL\n<EOH>\n" + rec("JA1XYZ", "20260103", "100000", "40m", "CW");
        });
        ConfirmSync.Result r = ConfirmSync.eqsl();
        assertTrue(r.ok(), r.message());
        assertEquals("https://www.eqsl.cc/downloadedfiles/WM3J.adi", urls.get(1));
        assertTrue(find("JA1XYZ").refs().eqsl());
    }
}
