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
 * Realistic Cabrillo export check for ARRL RTTY Roundup. Single-mode
 * (RTTY) contest with mixed-shape exchange — W/VE stations send RST +
 * state/province; DX stations send RST + serial. The receiver records
 * whatever the other side sent in the same field.
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "field2"]   — field2 = state_prov_sent (PA)
 *   cabrilloRcvd: ["rst_rcvd", "field1"]   — field1 = state_prov_rcvd
 *                                            (state/prov OR serial)
 *
 * Slot mapping: state_prov_rcvd → field1, state_prov_sent → field2
 *
 * Expected QSO line shape:
 *   QSO: <freq> RY <date> <time> <mycall> 599 PA <dxcall> 599 <st-or-ser>
 */
class CabrilloExporterArrlRttyRuTest {

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
    void exportsRttyRoundupLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_RTTY_RU");
        assertNotNull(plugin, "ARRL_RTTY_RU plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 10 QSOs mixing W/VE (state/prov received) and DX (serial received).
        // dxCall  band  freq    rcvdToken  isDx  minute
        Object[][] data = {
            {"K1AR",  "20m", "14080",  "CT",  false,   0},
            {"W4PA",  "20m", "14080",  "TN",  false,   3},
            {"N6TR",  "20m", "14080",  "OR",  false,   7},
            {"VE3CC", "20m", "14080",  "ONS", false,  10},
            {"DL5XX", "20m", "14080",  "001",  true,  15},
            {"G3ABC", "20m", "14080",  "045",  true,  20},
            {"JA1ZZ", "15m", "21080",  "112",  true, 140},
            {"K0FX",  "15m", "21080",  "MN",  false, 165},
            {"K3LR",  "40m", "7080",  "WPA",  false, 480},
            {"VK3XY", "40m", "7080",  "201",   true, 510},
        };

        LocalDateTime base = LocalDateTime.of(2026, 1, 3, 18, 0);   // RTTY RU first Sat of Jan 1800Z
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode("RTTY");
            q.setRstSent("599");
            q.setRstReceived("599");
            q.setContestField1((String) row[3]);    // state_prov_rcvd OR serial
            q.setContestField2("PA");                // state_prov_sent (operator's state)
            q.setDateTimeUtc(base.plusMinutes((Integer) row[5]));
            q.setPoints(2);                          // RTTY RU: 2 pts per QSO
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(10, ContestQsoDao.getInstance().fetchByContest("ARRL_RTTY_RU").size());

        Path out = dir.resolve("WM3J-arrl-rtty-ru.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: ARRL-RTTY-RU"),
            "header must declare CONTEST: ARRL-RTTY-RU");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(10, qsoLines.size(), "every inserted QSO must produce a QSO: line");

        // Mode column for RTTY: should be "RY" (Cabrillo's RTTY token).
        // The mode is the third whitespace-separated token in a QSO line.
        for (String l : qsoLines) {
            String mode = l.trim().split("\\s+")[2];
            assertEquals("RY", mode,
                "RTTY mode should map to Cabrillo 'RY' token, got '" + mode + "' in: " + l);
        }

        // Sent exchange must be "599 PA" on every QSO.
        for (String l : qsoLines) {
            assertTrue(l.matches(".*WM3J\\s+599\\s+PA\\s+.*"),
                "every QSO sent half must be '599 PA': " + l);
        }

        // First QSO rcvd half: W/VE — "599 CT"
        String first = qsoLines.get(0);
        assertTrue(first.matches(".*K1AR\\s+599\\s+CT\\b.*"),
            "first QSO rcvd half must be '599 CT': " + first);

        // DX QSO rcvd half: serial e.g. "599 001"
        String dxLine = qsoLines.stream().filter(l -> l.contains("DL5XX")).findFirst()
            .orElseThrow(() -> new AssertionError("no DL5XX line in:\n" + cabrillo));
        assertTrue(dxLine.matches(".*DL5XX\\s+599\\s+001\\b.*"),
            "DX rcvd half must be '599 001' (serial from DX side): " + dxLine);

        // Chronological order.
        assertTrue(qsoLines.get(0).contains("K1AR"));
        assertTrue(qsoLines.get(9).contains("VK3XY"));
    }
}
