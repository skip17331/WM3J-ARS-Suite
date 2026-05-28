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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the contest-side ADIF export path
 * ({@code AdifExporter.exportContestAdif}). Prints the file for human
 * inspection AND asserts the three fixes shipped on the
 * fix/contest-adif-export-medium branch:
 *
 *   1. QSO records appear in chronological order (was reverse — DAO's
 *      UI sort leaked through, same root cause as the Cabrillo fix in
 *      1f59d2c).
 *   2. RST_SENT / RST_RCVD default by mode when not captured by the
 *      contest cockpit ("59" for phone modes, "599" for CW + digital).
 *      LoTW rejects records missing RST.
 *   3. STX_STRING / SRX_STRING carry the full Cabrillo-style exchange
 *      strings so downstream loggers see the exchange context even
 *      without a per-plugin semantic ADIF tag map.
 *
 * Reuses the same 12-QSO ARRL November Sweepstakes (SSB) scenario from
 * CabrilloExporterSsSsbTest so Cabrillo and ADIF can be compared
 * side-by-side.
 */
class AdifContestExportSsSsbTest {

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
    void dumpsArrlSsSsbContestAdifForHumanInspection(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_SS_SSB");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        cfg.setSsPrecedence("B");
        cfg.setSsCheck("83");
        cfg.setSsSection("MDC");

        // Same 12-QSO SS Phone scenario as CabrilloExporterSsSsbTest.
        // Slot mapping: precedence=field1, check=field2, section=field3.
        Object[][] data = {
            {"K1AR",   "A",  "56", "CT",   "47",  "1", "20m", "SSB", "14245",   0},
            {"W4PA",   "B",  "84", "TN",  "112",  "2", "20m", "SSB", "14245",   3},
            {"VE3EJ",  "U",  "73", "ONS",   "8",  "3", "20m", "SSB", "14245",   7},
            {"N6MJ",   "U",  "92", "SCV", "201",  "4", "20m", "SSB", "14245",  15},
            {"K3ZO",   "Q",  "55", "MDC",  "33",  "5", "40m", "SSB",  "7245",  85},
            {"NN3W",   "A",  "78", "MDC",  "44",  "6", "40m", "SSB",  "7245", 120},
            {"WX4G",   "S",  "20", "GA",   "12",  "7", "40m", "SSB",  "7245", 175},
            {"W6YX",   "M",  "62", "SCV",   "5",  "8", "80m", "SSB",  "3845", 360},
            {"K0PC",   "B",  "78", "MN",  "298",  "9", "80m", "SSB",  "3845", 420},
            {"K2NV",   "A",  "65", "WNY", "180", "10", "80m", "SSB",  "3845", 485},
            {"K7RL",   "B",  "70", "WWA", "356", "11", "15m", "SSB", "21345",1020},
            {"VE7CC",  "U",  "60", "BC",   "44", "12", "15m", "SSB", "21345",1050},
        };
        LocalDateTime base = LocalDateTime.of(2026, 11, 21, 21, 0);
        for (Object[] row : data) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign((String) row[0]);
            q.setContestField1((String) row[1]);   // precedence
            q.setContestField2((String) row[2]);   // check
            q.setContestField3((String) row[3]);   // section
            q.setSerialReceived((String) row[4]);
            q.setSerialSent((String) row[5]);
            q.setBand((String) row[6]);
            q.setMode((String) row[7]);
            q.setFrequency((String) row[8]);
            q.setDateTimeUtc(base.plusMinutes((Integer) row[9]));
            q.setPoints(2);
            ContestQsoDao.getInstance().insert(q);
        }

        Path out = dir.resolve("WM3J-arrl-ss-ssb.adi");
        AdifExporter.exportContestAdif(plugin.getContestId(), out);

        String adif = Files.readString(out);
        System.out.println("\n=== Generated contest ADIF (" + out.getFileName() + ") ===");
        System.out.println(adif);
        System.out.println("=== end ===\n");

        // --- Fix #1: chronological order ---
        // Extract callsign from each record in file order.
        // Records are "<CALL:N>XXXXX ..." lines separated by <EOR>.
        Pattern callPat = Pattern.compile("<CALL:(\\d+)>([^ ]+)");
        Matcher m = callPat.matcher(adif);
        java.util.List<String> callOrder = new java.util.ArrayList<>();
        while (m.find()) callOrder.add(m.group(2));
        assertEquals(12, callOrder.size(), "all 12 QSO records must be present");
        assertEquals("K1AR",  callOrder.get(0),  "first record must be the first-inserted K1AR (chronological)");
        assertEquals("VE7CC", callOrder.get(11), "last record must be the last-inserted VE7CC");

        // --- Fix #2: RST defaulting ---
        // Test data left RST unset; mode is SSB so contest export should
        // default both to "59". Should appear in every record.
        long rstSentCount = countMatches(adif, "<RST_SENT:2>59 ");
        long rstRcvdCount = countMatches(adif, "<RST_RCVD:2>59 ");
        assertEquals(12, rstSentCount, "RST_SENT:59 expected on every SSB record (default for blank)");
        assertEquals(12, rstRcvdCount, "RST_RCVD:59 expected on every SSB record (default for blank)");

        // --- Fix #3: STX_STRING / SRX_STRING ---
        // First QSO is K1AR. Sent exchange per the SS plugin
        // cabrilloSent = [serial_sent, prec_sent, mycall, check_sent, sect_sent]
        // → "1 B WM3J 83 MDC". Received cabrilloRcvd
        // = [serial_rcvd, precedence, callsign, check, section]
        // → "47 A K1AR 56 CT".
        String k1arRecord = extractRecordContaining(adif, "K1AR");
        assertTrue(k1arRecord.contains("<STX_STRING:15>1 B WM3J 83 MDC"),
            "K1AR record must include STX_STRING for sent exchange:\n" + k1arRecord);
        assertTrue(k1arRecord.contains("<SRX_STRING:15>47 A K1AR 56 CT"),
            "K1AR record must include SRX_STRING for received exchange:\n" + k1arRecord);
    }

    private static long countMatches(String haystack, String needle) {
        long count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) { count++; idx += needle.length(); }
        return count;
    }

    private static String extractRecordContaining(String adif, String callsign) {
        for (String chunk : adif.split("(?i)<EOR>")) {
            if (chunk.contains(callsign)) return chunk.trim();
        }
        throw new AssertionError("no record found containing " + callsign + " in:\n" + adif);
    }
}
