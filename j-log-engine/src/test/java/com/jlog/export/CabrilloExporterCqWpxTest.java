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
 * Realistic Cabrillo export check for CQ WPX SSB. Exchange is RS +
 * serial (e.g. "59 001"). Multiplier is the distinct WPX prefix across
 * the entire contest (not per-band like CQ WW). Points by continent ×
 * band group: HF (20/15/10) vs LF (160/80/40) and same-continent vs
 * different-continent.
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "serial_sent"]   — both special, no slots
 *   cabrilloRcvd: ["rst_rcvd", "serial_rcvd"]   — both special, no slots
 *
 * Slot mapping is trivial — every exchange field is in the
 * special-non-slot set. No field1..5 needed.
 *
 * Expected QSO line shape (sponsor: CQ):
 *   QSO: <freq> <mode> <date> <time> <mycall> 59 <ourSer> <dxcall> 59 <theirSer>
 */
class CabrilloExporterCqWpxTest {

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
    void exportsTwelveQsoCqWpxSsbLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("CQ_WPX_SSB");
        assertNotNull(plugin, "CQ_WPX_SSB plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 12 QSOs with distinct WPX prefixes so the contest-wide mult
        // grows linearly. Prefixes: DL5, G3, F5, OH2, JA1, VK3, ZL2,
        // PY2, LU5, OH1, S52, K1.
        // dxCall  band  freq    pts (HF=3 DX-EU/3 DX-other/1 NA-NA, LF doubles)
        // Per CQ WPX:
        //   28/21/14 same-continent = 1 pt, different-continent = 3 pts
        //   7/3.5/1.8 same-continent = 2 pts, different-continent = 6 pts
        //   NA → NA = 1 pt (HF) / 2 pts (LF)
        Object[][] data = {
            // dxCall    band   freq    serRcvd  pts  minute
            {"DL5XX",  "20m", "14245",  "045",    3,    0},
            {"G3ABC",  "20m", "14245",  "112",    3,    3},
            {"F5RAB",  "20m", "14245",  "008",    3,    7},
            {"OH2BH",  "20m", "14245",  "234",    3,   12},
            {"JA1ZZ",  "15m", "21245",  "067",    3,  120},
            {"VK3XY",  "15m", "21245",  "199",    3,  140},
            {"ZL2AA",  "15m", "21245",  "088",    3,  155},
            {"PY2BB",  "10m", "28425",  "022",    3,  280},
            {"LU5DX",  "10m", "28425",  "311",    3,  295},
            {"OH1BB",  "10m", "28425",  "501",    3,  310},     // OH1 vs OH2 = distinct prefix
            {"S52BT",  "40m", "7245",  "144",    6,   720},     // LF, different continent
            {"K1AR",   "40m", "7245",  "298",    2,   750},     // LF, same continent
        };

        LocalDateTime base = LocalDateTime.of(2026, 3, 28, 0, 0);    // CQ WPX SSB last full Mar weekend
        for (int i = 0; i < data.length; i++) {
            Object[] row = data[i];
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode("SSB");
            q.setRstSent("59");
            q.setRstReceived("59");
            q.setSerialSent(String.format("%03d", i + 1));
            q.setSerialReceived((String) row[3]);
            q.setDateTimeUtc(base.plusMinutes((Integer) row[5]));
            q.setPoints((Integer) row[4]);
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(12, ContestQsoDao.getInstance().fetchByContest("CQ_WPX_SSB").size());

        Path out = dir.resolve("WM3J-cq-wpx-ssb.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: CQ-WPX-SSB"),
            "header must declare CONTEST: CQ-WPX-SSB");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(12, qsoLines.size(), "every inserted QSO must produce a QSO: line");

        // First QSO: WM3J 59 001 DL5XX 59 045.
        String first = qsoLines.get(0);
        assertTrue(first.contains("WM3J"));
        assertTrue(first.contains("DL5XX"));
        assertTrue(first.matches(".*WM3J\\s+59\\s+001\\s+.*"),
            "first QSO sent half must be '59 001': " + first);
        assertTrue(first.matches(".*DL5XX\\s+59\\s+045\\b.*"),
            "first QSO rcvd half must be '59 045': " + first);

        // Sent serials must increment 1..12 in chronological order — pins
        // both the chronological-sort fix and the controller-side serial
        // assignment. Captures the 3rd whitespace-separated token after
        // mycall, which is the sent serial.
        for (int i = 0; i < 12; i++) {
            String l = qsoLines.get(i);
            String[] tok = l.trim().split("\\s+");
            // Layout: QSO: freq mode date time mycall <rst_sent> <serial_sent> dxcall ...
            // mycall is token[5], rst_sent token[6], serial_sent token[7]
            assertEquals(String.format("%03d", i + 1), tok[7],
                "QSO " + (i+1) + " serial_sent should be " + String.format("%03d", i+1)
                + " but row is:\n" + l);
        }

        // Chronological order verified by call ordering too.
        assertTrue(qsoLines.get(0).contains("DL5XX"));
        assertTrue(qsoLines.get(11).contains("K1AR"));
    }
}
