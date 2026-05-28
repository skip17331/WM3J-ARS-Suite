package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.DatabaseManager;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Realistic Cabrillo export check for ARRL Field Day. Both sides exchange
 * CLASS + SECTION (e.g. "2A MDC", "1B CT", "3F STX"). No serial number.
 *
 * Plugin spec:
 *   cabrilloSent: ["class_sent", "sect_sent"]   — both AppConfig constants
 *   cabrilloRcvd: ["class_rcvd", "sect_rcvd"]   — both per-QSO
 *
 * Slot mapping: class_rcvd → field1, sect_rcvd → field2
 *
 * Expected QSO line shape (sponsor: ARRL):
 *   QSO: <freq> <mode> <date> <time> <mycall> <classS> <sectS> <dxcall> <classR> <sectR>
 *
 * Mode coverage matters here — FD scores per-mode-category (CW=2pts,
 * digital=2pts, phone=1pt) and the Cabrillo mode column must be one
 * of CW / PH / DG. Test exercises all three so mode-token translation
 * is visible in the dump.
 */
class CabrilloExporterFieldDayTest {

    @BeforeAll
    static void initDb(@TempDir Path tmpHome) throws Exception {
        System.setProperty("user.home", tmpHome.toString());
        AppConfig.getInstance().load();
        DatabaseManager.getInstance().initAll();
        PluginLoader.getInstance().init();
    }

    @BeforeEach
    void wipeContestQso() throws Exception {
        try (Statement st = DatabaseManager.getInstance().getContestConnection().createStatement()) {
            st.executeUpdate("DELETE FROM contest_qso");
        }
    }

    @Test
    void exportsTwelveQsoFieldDayLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_FIELD_DAY");
        assertNotNull(plugin, "ARRL_FIELD_DAY plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");
        // Operator's Field Day class + section — constants emitted in
        // every QSO's sent half via AppConfig.getContestConstant().
        cfg.setContestConstant("ARRL_FIELD_DAY", "class_sent", "1D");   // 1 tx, home-station / commercial power
        cfg.setContestConstant("ARRL_FIELD_DAY", "sect_sent",  "MDC");

        // 12 QSOs across CW + SSB + FT8 (digital) on a mix of bands,
        // working club, battery, home, and DX stations.
        // dxCall  band  freq    mode    classR  sectR  minute-offset  pts
        Object[][] data = {
            {"W1AW",  "20m", "14045", "CW",  "5A", "CT",   0, 2},
            {"K1KI",  "20m", "14045", "CW",  "2B", "EMA",  3, 2},
            {"K8AB",  "40m", "7060",  "CW",  "3A", "OH",  85, 2},
            {"VE3CC", "40m", "7245",  "SSB", "1D", "ONS", 130, 1},
            {"K0FX",  "20m", "14245", "SSB", "1B", "MN",  175, 1},
            {"N6TR",  "15m", "21045", "CW",  "1E", "OR",  240, 2},
            {"AA6YQ", "15m", "21245", "SSB", "3F", "EB",  280, 1},
            {"DL5XX", "10m", "28425", "SSB", "2A", "DX",  320, 1},
            {"W6YX",  "20m", "14074", "FT8", "4F", "SCV", 480, 2},
            {"K3LR",  "80m", "3550",  "CW",  "9A", "WPA", 720, 2},
            {"NN3W",  "80m", "3845",  "SSB", "1D", "MDC", 780, 1},
            {"JA1ZZ", "20m", "14074", "FT8", "1D", "DX",  900, 2},
        };

        LocalDateTime base = LocalDateTime.of(2026, 6, 27, 18, 0);    // FD Saturday 1800Z start
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode((String) row[3]);
            q.setContestField1((String) row[4]);   // class_rcvd
            q.setContestField2((String) row[5]);   // sect_rcvd
            q.setDateTimeUtc(base.plusMinutes((Integer) row[6]));
            q.setPoints((Integer) row[7]);
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(12, ContestQsoDao.getInstance().fetchByContest("ARRL_FIELD_DAY").size());

        Path out = dir.resolve("WM3J-arrl-field-day.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: ARRL-FIELD-DAY"),
            "header must declare CONTEST: ARRL-FIELD-DAY");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(12, qsoLines.size(), "every inserted QSO must produce a QSO: line");

        // Spot-check the first QSO: W1AW on 20m CW. Sent "1D MDC", rcvd "5A CT".
        String first = qsoLines.get(0);
        assertTrue(first.contains("WM3J"));
        assertTrue(first.contains("W1AW"));
        assertTrue(first.contains(" 1D ")  && first.contains(" MDC "),
            "first QSO sent half must show our 1D MDC: " + first);
        assertTrue(first.contains(" 5A ")  && first.contains(" CT"),
            "first QSO rcvd half must show 5A CT: " + first);

        // Mode-column translation: CW QSOs should show "CW", SSB → "PH", FT8 → "DG".
        // The Cabrillo mode token is the 3rd whitespace-separated field after "QSO:".
        // Counts: 5 CW, 4 SSB, 2 FT8, 1 ... actually 5 CW, 4 SSB, 2 FT8 = 11. Let me recount.
        long cwLines = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("CW")).count();
        long phLines = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("PH")).count();
        long dgLines = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("DG")).count();
        assertEquals(5, cwLines, "expected 5 CW QSO lines (mode-col token 'CW')");
        assertEquals(5, phLines, "expected 5 PH QSO lines (mode-col token 'PH')");
        assertEquals(2, dgLines, "expected 2 DG QSO lines (FT8 → 'DG')");

        // Chronological order — first inserted W1AW first, last inserted JA1ZZ last.
        assertTrue(qsoLines.get(0).contains("W1AW"),
            "first QSO line should be first-inserted W1AW: " + qsoLines.get(0));
        assertTrue(qsoLines.get(11).contains("JA1ZZ"),
            "last QSO line should be last-inserted JA1ZZ: " + qsoLines.get(11));

        // No serial number leak. Field Day has no serial; if one bled through
        // we'd see digits immediately after WM3J.
        for (String l : qsoLines) {
            assertTrue(l.matches(".*WM3J\\s+\\d[A-Z]\\s+.*"),
                "QSO line must show class (e.g. 1D, 2A, 3F) immediately after WM3J, no serial: " + l);
        }
    }
}
