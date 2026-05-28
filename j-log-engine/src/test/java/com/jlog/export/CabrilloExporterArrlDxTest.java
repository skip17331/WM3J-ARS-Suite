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
 * Realistic Cabrillo export check for ARRL International DX, W/VE side
 * (SSB variant). Asymmetric contest: W/VE stations send signal report +
 * state/province; DX stations send signal report + power.
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "field3"]   — field3 = state_sent (PA)
 *   cabrilloRcvd: ["rst_rcvd", "field2"]   — field2 = power_rcvd (100)
 *
 * Slot mapping (per ContestPlugin.fieldSlotColumn order over entryFields):
 *   dxcc → field1, power_rcvd → field2, state_sent → field3
 *
 * Expected QSO line shape:
 *   QSO: 14245 PH 2026-02-21 1500 WM3J 59 PA <DXCALL> 59 <PWR>
 */
class CabrilloExporterArrlDxTest {

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
    void exportsTenQsoArrlDxSsbWveLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_DX_SSB_US");
        assertNotNull(plugin, "ARRL_DX_SSB_US plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid",   "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator",   "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",       "ALL");

        // Ten QSOs across bands, with realistic DX-side data.
        // dxCall   prec  band   freq   pwrRcvd  dxccCountry        minute-offset
        Object[][] data = {
            {"DL5XX",  "20m", "14245",  "1KW",   "Germany",         0},
            {"G3ABC",  "20m", "14245",  "400",   "England",         3},
            {"F5RAB",  "20m", "14245",  "100",   "France",          7},
            {"JA1ZZ",  "15m", "21245",  "100",   "Japan",          90},
            {"VK3XY",  "15m", "21245",  "1500",  "Australia",     140},
            {"ZL2AA",  "15m", "21245",  "300",   "New Zealand",   160},
            {"PY2BB",  "10m", "28425",  "100",   "Brazil",        320},
            {"LU5DX",  "10m", "28425",  "750",   "Argentina",     345},
            {"OH2BH",  "40m", "7245",  "1500",   "Finland",      1200},
            {"S52BT",  "40m", "7245",  "200",    "Slovenia",     1230},
        };

        LocalDateTime base = LocalDateTime.of(2026, 2, 21, 15, 0);
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode("SSB");
            q.setDateTimeUtc(base.plusMinutes((Integer) row[5]));
            q.setRstSent("59");
            q.setRstReceived("59");
            q.setContestField1((String) row[4]);   // dxcc — needed for scoring
            q.setContestField2((String) row[3]);   // power_rcvd
            q.setContestField3("PA");              // state_sent (operator's state)
            q.setPoints(3);                        // ARRL DX: 3 pts per QSO
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(10, ContestQsoDao.getInstance().fetchByContest("ARRL_DX_SSB_US").size());

        Path out = dir.resolve("WM3J-arrl-dx-ssb.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        // KNOWN-WRONG (2026-05-28 finding): CONTEST: ARRL-DX-SSB-US is our
        // internal split-side disambiguation; the ARRL robot expects
        // "ARRL-DX-SSB" regardless of W/VE-vs-DX side (the operator
        // category tells the sponsor which side). Pinned here as the
        // current behavior; the planned Cabrillo header-correctness pass
        // will flip this assertion to "ARRL-DX-SSB" once the exporter
        // honors a per-plugin cabrilloContestName override. Affects
        // ARRL_DX_*_US/DX (4 plugins) + likely ARRL_RRU_* (3 plugins).
        assertTrue(cabrillo.contains("CONTEST: ARRL-DX-SSB-US"),
            "header currently emits ARRL-DX-SSB-US (sponsor expects ARRL-DX-SSB)");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(10, qsoLines.size(), "every inserted QSO must produce a QSO: line");

        // Spot-check first QSO: WM3J 59 PA DL5XX 59 1KW (no serial in ARRL DX).
        String first = qsoLines.get(0);
        assertTrue(first.contains("WM3J"),  "WM3J in first QSO");
        assertTrue(first.contains("DL5XX"), "DL5XX in first QSO");
        assertTrue(first.contains(" 59 "),  "first QSO must contain RST tokens");
        assertTrue(first.contains(" PA "),  "first QSO must show our state PA in sent exchange");
        assertTrue(first.contains(" 1KW"),  "first QSO must show DL5XX's power 1KW in rcvd exchange");

        // Chronological order: first inserted DL5XX should be line 1, last
        // inserted S52BT should be line 10. Pins the 1f59d2c sort fix.
        assertTrue(qsoLines.get(0).contains("DL5XX"),
            "first QSO line should be the first-inserted DL5XX, got: " + qsoLines.get(0));
        assertTrue(qsoLines.get(9).contains("S52BT"),
            "last QSO line should be the last-inserted S52BT, got: " + qsoLines.get(9));

        // ARRL DX has no serial number in the exchange — spot-check we didn't
        // accidentally inject one. A bogus serial would appear right after
        // WM3J like "WM3J 1 59 PA ...".
        assertTrue(first.matches(".*WM3J\\s+59\\s+PA.*"),
            "sent exchange should be just '59 PA' after mycall — no serial: " + first);
    }
}
