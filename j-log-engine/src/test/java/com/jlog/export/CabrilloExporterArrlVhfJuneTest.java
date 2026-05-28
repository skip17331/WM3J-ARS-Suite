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
 * Realistic Cabrillo export check for ARRL June VHF — representative
 * of the VHF/UHF/grid family (also covers Jan VHF + Sep VHF + 222 & Up
 * + 10 GHz & Up + EME with minor exchange differences).
 *
 * Exchange shape:
 *   Both sides send 4-char Maidenhead grid (e.g. FN10, FM19). No RST,
 *   no serial. That's the entire exchange.
 *
 * Plugin spec:
 *   cabrilloSent: ["grid_sent"]   → field2 = grid_sent (operator's grid)
 *   cabrilloRcvd: ["grid_rcvd"]   → field1 = grid_rcvd
 *
 * Slot mapping: grid_rcvd → field1, grid_sent → field2.
 *
 * Expected QSO line shape (sponsor: ARRL VHF):
 *   QSO: <freq-khz> <mode> <date> <time> <mycall> <ourGrid> <dxcall> <theirGrid>
 *
 * Note: VHF Cabrillo uses freq in kHz (e.g. 50125 for 6m) but spec
 * also accepts the band tag.
 */
class CabrilloExporterArrlVhfJuneTest {

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
    void exportsArrlJuneVhfLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_VHF_JUNE");
        assertNotNull(plugin, "ARRL_VHF_JUNE plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 10 QSOs across 6m + 2m + 70cm + 23cm + 10GHz. Different grids
        // for multiplier diversity.
        // dxCall    band    freq    mode    grid    pts  minute
        Object[][] data = {
            {"W3SO",  "6m",   "50125",  "SSB", "FN00",  1,    0},
            {"K3IPM", "6m",   "50125",  "SSB", "FM18",  1,    5},
            {"N3RG",  "6m",   "50313",  "FT8", "FM19",  1,   20},
            {"K1TR",  "6m",   "50313",  "FT8", "FN42",  1,   40},
            {"W1QA",  "2m",   "144250", "SSB", "FN42",  1,   90},
            {"W3GAD", "2m",   "144175", "CW",  "FM19",  1,  150},
            {"W3IP",  "70cm", "432100", "SSB", "FM19",  2,  210},
            {"N3HBX", "70cm", "432100", "SSB", "FM19",  2,  240},
            {"WA3EHD","23cm", "1296100","SSB", "FM19",  3,  300},
            {"K1RZ",  "10GHz","10368100","SSB","FM19",  8,  720},
        };

        LocalDateTime base = LocalDateTime.of(2026, 6, 13, 18, 0);   // June VHF 2nd full Jun weekend Sat 1800Z
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setBand((String) row[1]);
            q.setFrequency((String) row[2]);
            q.setMode((String) row[3]);
            q.setContestField1((String) row[4]);   // grid_rcvd
            q.setContestField2("FM19");             // grid_sent (operator's grid)
            q.setDateTimeUtc(base.plusMinutes((Integer) row[6]));
            q.setPoints((Integer) row[5]);
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(10, ContestQsoDao.getInstance().fetchByContest("ARRL_VHF_JUNE").size());

        Path out = dir.resolve("WM3J-arrl-vhf-june.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: ARRL-VHF-JUNE"),
            "header must declare CONTEST: ARRL-VHF-JUNE");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(10, qsoLines.size());

        // Every QSO sent half must be just "FM19" (no RST, no serial).
        // Expected layout: QSO: freq mode date time mycall FM19 dxcall theirGrid
        // Token index: 0=QSO: 1=freq 2=mode 3=date 4=time 5=mycall 6=sent-grid 7=dxcall 8=rcvd-grid
        for (String l : qsoLines) {
            String[] tok = l.trim().split("\\s+");
            assertEquals(9, tok.length,
                "VHF QSO line should have exactly 9 tokens (no RST/serial): " + l);
            assertEquals("FM19", tok[6],
                "token 6 must be our sent grid FM19: " + l);
            assertTrue(tok[8].matches("[A-R]{2}\\d{2}"),
                "token 8 must be a valid 4-char grid: " + l);
        }

        // First QSO spot-check: WM3J FM19 W3SO FN00 on 6m.
        String first = qsoLines.get(0);
        assertTrue(first.contains("W3SO"));
        assertTrue(first.matches(".*WM3J\\s+FM19\\s+W3SO\\s+FN00\\s*$"),
            "first QSO exact layout 'mycall FM19 dxcall grid': " + first);

        // Chronological.
        assertTrue(qsoLines.get(0).contains("W3SO"));
        assertTrue(qsoLines.get(9).contains("K1RZ"));
    }
}
