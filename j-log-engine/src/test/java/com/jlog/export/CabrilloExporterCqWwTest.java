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
 * Realistic Cabrillo export check for CQ WorldWide DX SSB. Both sides
 * exchange RS + CQ Zone (e.g. "59 05"). Multipliers are per-band zone +
 * per-band DXCC country. Asymmetric scoring: same-country = 0 pts,
 * same-continent = 1 pt, different-continent = 3 pts (US ↔ EU/JA).
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "zone_sent"]   — zone_sent = field2 (operator's zone)
 *   cabrilloRcvd: ["rst_rcvd", "cq_zone"]     — cq_zone = field1 (their zone)
 *
 * Slot mapping: cq_zone → field1, zone_sent → field2.
 *
 * Expected QSO line shape (sponsor: CQ):
 *   QSO: <freq> <mode> <date> <time> <mycall> 59 05 <dxcall> 59 <theirZone>
 */
class CabrilloExporterCqWwTest {

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
    void exportsTwelveQsoCqWwSsbLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("CQ_WW_SSB");
        assertNotNull(plugin, "CQ_WW_SSB plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 12 QSOs spanning bands + zones + continents.
        // dxCall  band   freq    zoneR  zoneSent  pts  min-offset
        Object[][] data = {
            {"DL5XX",  "20m", "14245", "14", "05",  3,    0},   // EU
            {"G3ABC",  "20m", "14245", "14", "05",  3,    3},   // EU
            {"OH2BH",  "20m", "14245", "15", "05",  3,    8},   // EU (zone 15)
            {"JA1ZZ",  "15m", "21245", "25", "05",  3,  120},   // JA
            {"VK3XY",  "15m", "21245", "30", "05",  3,  140},   // OC
            {"BV2A",   "15m", "21245", "24", "05",  3,  155},   // Taiwan
            {"PY2BB",  "10m", "28425", "11", "05",  3,  280},   // SA
            {"LU5DX",  "10m", "28425", "13", "05",  3,  300},   // SA
            {"K1AR",   "20m", "14245", "05", "05",  0,  420},   // US-US: 0 pts
            {"VE3CC",  "20m", "14245", "04", "05",  2,  450},   // NA (Canada)
            {"ZS6XX",  "40m", "7245",  "38", "05",  3,  720},   // AF
            {"S52BT",  "40m", "7245",  "15", "05",  3,  750},   // EU (Slovenia)
        };

        LocalDateTime base = LocalDateTime.of(2026, 10, 24, 0, 0);   // CQ WW SSB last full Oct weekend
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode("SSB");
            q.setContestField1((String) row[3]);   // cq_zone (theirs)
            q.setContestField2((String) row[4]);   // zone_sent (ours)
            q.setRstSent("59");
            q.setRstReceived("59");
            q.setDateTimeUtc(base.plusMinutes((Integer) row[6]));
            q.setPoints((Integer) row[5]);
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(12, ContestQsoDao.getInstance().fetchByContest("CQ_WW_SSB").size());

        Path out = dir.resolve("WM3J-cq-ww-ssb.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: CQ-WW-SSB"),
            "header must declare CONTEST: CQ-WW-SSB");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(12, qsoLines.size(), "every inserted QSO must produce a QSO: line");

        // Spot-check first QSO: WM3J 59 05 DL5XX 59 14.
        String first = qsoLines.get(0);
        assertTrue(first.contains("WM3J"));
        assertTrue(first.contains("DL5XX"));
        assertTrue(first.matches(".*WM3J\\s+59\\s+05\\s+.*"),
            "first QSO sent half must be '59 05' (RST + our zone): " + first);
        assertTrue(first.matches(".*DL5XX\\s+59\\s+14\\b.*"),
            "first QSO rcvd half must be '59 14' (RST + their zone): " + first);

        // No-serial assertion: CQ WW has no serial number in exchange.
        for (String l : qsoLines) {
            // After mycall should come "59 <zone>", not "<serial> 59 ..."
            assertTrue(l.matches(".*WM3J\\s+59\\s+\\d{1,2}\\s+.*"),
                "QSO line must show '59 <zone>' after mycall, no serial: " + l);
        }

        // Chronological order.
        assertTrue(qsoLines.get(0).contains("DL5XX"),
            "first QSO line should be first-inserted DL5XX: " + qsoLines.get(0));
        assertTrue(qsoLines.get(11).contains("S52BT"),
            "last QSO line should be last-inserted S52BT: " + qsoLines.get(11));
    }
}
