package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.DatabaseManager;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Cabrillo header carries the host's score and program name, dupes stay in the log, and a multi-op entry lists its operators. */
class CabrilloHeaderAndDupesTest {

    @BeforeAll
    static void initDb(@TempDir Path tmpHome) throws Exception {
        System.setProperty("user.home", tmpHome.toString());
        AppConfig.getInstance().load();
        DatabaseManager.getInstance().initAll();
        PluginLoader.getInstance().init();
    }

    @AfterAll
    static void closeDb() {
        com.jlog.db.BackupService.getInstance().stop();
        DatabaseManager.getInstance().closeAll();
    }

    @BeforeEach
    void wipe() throws Exception {
        try (Statement st = DatabaseManager.getInstance().getContestConnection().createStatement()) {
            st.executeUpdate("DELETE FROM contest_qso");
        }
        AppConfig.getInstance().setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
    }

    private static void qso(ContestPlugin p, String call, int minute, boolean dupe, String op) throws Exception {
        QsoRecord q = new QsoRecord();
        q.setContestId(p.getContestId());
        q.setCallsign(call); q.setBand("20m"); q.setFrequency("14025"); q.setMode("CW");
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 28, 12, minute));
        q.setRstSent("599"); q.setRstReceived("599");
        q.setContestField1("14"); q.setContestField2("5");
        q.setPoints(dupe ? 0 : 3);
        q.setDupe(dupe);
        q.setOperator(op);
        ContestQsoDao.getInstance().insert(q);
    }

    private static List<String> qsoLines(String cab) { return cab.lines().filter(l -> l.startsWith("QSO:")).toList(); }

    @Test void dupesStayInTheLog(@TempDir Path dir) throws Exception {
        ContestPlugin p = PluginLoader.getInstance().getById("CQ_WW_CW");
        assertNotNull(p);
        qso(p, "DL1ABC", 0, false, null);
        qso(p, "DL1ABC", 5, true, null);                      // the same station again, marked a dupe
        qso(p, "G3XYZ", 9, false, null);
        Path out = dir.resolve("a.cbr");
        CabrilloExporter.export(p, out);
        List<String> l = qsoLines(Files.readString(out));
        assertEquals(3, l.size(), "a dupe used to be left out, losing its points if the flag was wrong: " + l);
        assertEquals(2, l.stream().filter(x -> x.contains("DL1ABC")).count());
    }

    @Test void theHostsScoreAndNameGoInTheHeader(@TempDir Path dir) throws Exception {
        ContestPlugin p = PluginLoader.getInstance().getById("CQ_WW_CW");
        qso(p, "DL1ABC", 0, false, null);
        Path out = dir.resolve("b.cbr");
        CabrilloExporter.export(p, out, new CabrilloExporter.Options("WM3J ARS Suite 1.13.18", 4242));
        String cab = Files.readString(out);
        assertTrue(cab.contains("CREATED-BY: WM3J ARS Suite 1.13.18"), cab);
        assertTrue(cab.contains("CLAIMED-SCORE: 4242"), "the score the contest page shows, not a rough sum: " + cab);
        assertFalse(cab.contains("j-Log v1.5.0"));
    }

    @Test void withoutOptionsItStillWritesAHeader(@TempDir Path dir) throws Exception {
        ContestPlugin p = PluginLoader.getInstance().getById("CQ_WW_CW");
        qso(p, "DL1ABC", 0, false, null);
        Path out = dir.resolve("c.cbr");
        CabrilloExporter.export(p, out);
        String cab = Files.readString(out);
        assertTrue(cab.contains("CREATED-BY: WM3J ARS Suite"), cab);
        assertTrue(cab.contains("CLAIMED-SCORE: "));
    }

    @Test void aMultiOpEntryListsItsOperators(@TempDir Path dir) throws Exception {
        ContestPlugin p = PluginLoader.getInstance().getById("CQ_WW_CW");
        DatabaseManager.getInstance().setConfig("cab.operator", "MULTI-OP");
        qso(p, "DL1ABC", 0, false, "k3abc");
        qso(p, "G3XYZ", 3, false, "N3XYZ");
        qso(p, "F5AA", 6, false, "K3ABC");
        Path out = dir.resolve("d.cbr");
        CabrilloExporter.export(p, out);
        assertTrue(Files.readString(out).contains("OPERATORS: K3ABC N3XYZ"), Files.readString(out));
    }

    @Test void aSingleOpEntryHasNoOperatorsLine(@TempDir Path dir) throws Exception {
        ContestPlugin p = PluginLoader.getInstance().getById("CQ_WW_CW");
        qso(p, "DL1ABC", 0, false, "K3ABC");
        Path out = dir.resolve("e.cbr");
        CabrilloExporter.export(p, out);
        assertFalse(Files.readString(out).contains("OPERATORS:"));
    }
}
