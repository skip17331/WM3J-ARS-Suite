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
 * Realistic Cabrillo export check for Wisconsin QSO Party — representative
 * of the state-QSO-party family (one of the largest plugin groups, ~50
 * plugins).
 *
 * Exchange shape:
 *   WI sends 3-letter county code (DAN, MIL, WAU, …)
 *   Non-WI sends state / province / country
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "state_prov_sent"]  → field2
 *   cabrilloRcvd: ["rst_rcvd", "state_prov_rcvd"]  → field1
 *
 * Slot mapping: state_prov_rcvd → field1, state_prov_sent → field2.
 *
 * Scenario: WM3J in PA (non-WI) works 10 WI stations across different
 * counties spanning the high bands and CW + SSB + Digital modes.
 */
class CabrilloExporterWiQsoPartyTest {

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
    void exportsWisconsinQsoPartyLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("WI_QSO_PARTY");
        assertNotNull(plugin, "WI_QSO_PARTY plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 10 QSOs to WI stations across 10 different WI counties.
        // dxCall   band   freq    mode    countyRcvd   pts  minute
        Object[][] data = {
            {"W9RE",  "20m", "14245", "SSB",  "DAN",  2,    0},   // Dane
            {"K9NW",  "20m", "14245", "SSB",  "MIL",  2,    3},   // Milwaukee
            {"K9OM",  "20m", "14045", "CW",   "WAU",  3,   30},   // Waukesha (CW = 3 pts)
            {"WB9Z",  "20m", "14045", "CW",   "WIN",  3,   45},   // Winnebago
            {"N9NB",  "40m", "7245", "SSB",   "BRO",  2,  120},   // Brown
            {"AA9A",  "40m", "7045", "CW",    "ROC",  3,  140},   // Rock
            {"K0PC",  "80m", "3845", "SSB",   "DAN",  2,  400},   // Dane (different op, same county — re-mult? No, mult is per county once)
            {"N9SE",  "15m", "21080", "RTTY", "EAU",  3,  520},   // Eau Claire (RTTY = 3 pts)
            {"K9YY",  "15m", "21245", "SSB",  "LAC",  2,  540},   // La Crosse
            {"W9XT",  "10m", "28080", "RTTY", "OUT",  3,  690},   // Outagamie
        };

        LocalDateTime base = LocalDateTime.of(2026, 3, 14, 18, 0);   // WI QSO Party Sat 1800Z
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode((String) row[3]);
            q.setRstSent("59");
            q.setRstReceived("59");
            q.setContestField1((String) row[4]);   // state_prov_rcvd (WI county)
            q.setContestField2("PA");               // state_prov_sent (operator's state)
            q.setDateTimeUtc(base.plusMinutes((Integer) row[6]));
            q.setPoints((Integer) row[5]);
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(10, ContestQsoDao.getInstance().fetchByContest("WI_QSO_PARTY").size());

        Path out = dir.resolve("WM3J-wi-qso-party.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: WI-QSO-PARTY"),
            "header must declare CONTEST: WI-QSO-PARTY");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(10, qsoLines.size());

        // Spot-check first: WM3J 59 PA W9RE 59 DAN
        String first = qsoLines.get(0);
        assertTrue(first.matches(".*WM3J\\s+59\\s+PA\\s+.*"),
            "first sent half '59 PA': " + first);
        assertTrue(first.matches(".*W9RE\\s+59\\s+DAN\\b.*"),
            "first rcvd half '59 DAN': " + first);

        // Mode column translation: CW → CW, SSB → PH, RTTY → RY.
        long cw = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("CW")).count();
        long ph = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("PH")).count();
        long ry = qsoLines.stream().filter(l -> l.trim().split("\\s+")[2].equals("RY")).count();
        assertEquals(3, cw, "expected 3 CW QSO lines");
        assertEquals(5, ph, "expected 5 PH (SSB) QSO lines");
        assertEquals(2, ry, "expected 2 RY (RTTY) QSO lines");

        // Chronological.
        assertTrue(qsoLines.get(0).contains("W9RE"));
        assertTrue(qsoLines.get(9).contains("W9XT"));
    }
}
