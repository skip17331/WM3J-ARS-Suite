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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Builds a fake 12-QSO ARRL November Sweepstakes (SSB) contest log and
 * runs {@link CabrilloExporter} against it. Prints the generated Cabrillo
 * file to stdout for visual inspection (run with
 * {@code mvn test -Dtest=CabrilloExporterSsSsbTest}) and asserts the
 * structural invariants the contest sponsor checks first.
 *
 * SS Phone exchange:
 *   Sent: NR PREC CALL CHK SECT — e.g. "1 B WM3J 83 MDC"
 *   Rcvd: same — e.g. "47 A K1AR 56 CT"
 *
 * Per the plugin's entryFields declaration order (after the special ids
 * callsign/serial_rcvd/serial_sent/band consume no slot):
 *   precedence → field1, check → field2, section → field3.
 */
class CabrilloExporterSsSsbTest {

    @BeforeAll
    static void initDb(@TempDir Path tmpHome) throws Exception {
        // Redirect ~/.j-log so the real DBs are untouched.
        System.setProperty("user.home", tmpHome.toString());
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
    void exportsTwelveQsoSsSsbLogToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("ARRL_SS_SSB");
        assertNotNull(plugin, "ARRL_SS_SSB plugin must be loaded");

        // Station + Cabrillo categories + SS sent constants.
        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        // grid setter via the generic db key (no public setter on AppConfig).
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");
        cfg.setSsPrecedence("B");      // single-op high power
        cfg.setSsCheck("83");          // year first licensed
        cfg.setSsSection("MDC");       // Maryland / DC (FM19 area)

        // 12 representative QSOs spanning bands, precedences, sections.
        // Times are over the 30-hour SS window starting 2026-11-21 21:00Z.
        Object[][] data = {
            // dx-call, prec,  chk,  sect,  serialRcvd, serialSent, band, mode, freq,    minutes-offset
            {"K1AR",   "A",   "56",  "CT",   "47",       "1",  "20m", "SSB", "14245",   0},
            {"W4PA",   "B",   "84",  "TN",   "112",      "2",  "20m", "SSB", "14245",   3},
            {"VE3EJ",  "U",   "73",  "ONS",  "8",        "3",  "20m", "SSB", "14245",   7},
            {"N6MJ",   "U",   "92",  "SCV",  "201",      "4",  "20m", "SSB", "14245",  15},
            {"K3ZO",   "Q",   "55",  "MDC",  "33",       "5",  "40m", "SSB",  "7245",  85},
            {"NN3W",   "A",   "78",  "MDC",  "44",       "6",  "40m", "SSB",  "7245", 120},
            {"WX4G",   "S",   "20",  "GA",   "12",       "7",  "40m", "SSB",  "7245", 175},
            {"W6YX",   "M",   "62",  "SCV",  "5",        "8",  "80m", "SSB",  "3845", 360},
            {"K0PC",   "B",   "78",  "MN",   "298",      "9",  "80m", "SSB",  "3845", 420},
            {"K2NV",   "A",   "65",  "WNY",  "180",     "10",  "80m", "SSB",  "3845", 485},
            {"K7RL",   "B",   "70",  "WWA",  "356",     "11",  "15m", "SSB", "21345", 1020},
            {"VE7CC",  "U",   "60",  "BC",   "44",      "12",  "15m", "SSB", "21345", 1050},
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
            q.setPoints(2);                         // SS: 2 pts per QSO
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(12, ContestQsoDao.getInstance().fetchByContest("ARRL_SS_SSB").size());

        // Export.
        Path out = dir.resolve("WM3J-arrl-ss-ssb.cbr");
        CabrilloExporter.export(plugin, out);

        // Echo to stdout for visual inspection of the generated file.
        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        // Structural assertions — what the sponsor's robot checks first.
        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim(),
            "first line must be Cabrillo 3.0 marker");
        assertTrue(cabrillo.contains("CONTEST: ARRL-SS-SSB"),
            "header must declare CONTEST: ARRL-SS-SSB");
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"));
        assertTrue(cabrillo.contains("END-OF-LOG:"),
            "log must terminate with END-OF-LOG:");

        long qsoLineCount = lines.stream().filter(l -> l.startsWith("QSO:")).count();
        assertEquals(12, qsoLineCount, "every inserted non-dupe QSO must produce a QSO: line");

        // Spot-check one QSO line: K1AR is the first row. Expected sent exchange
        // is "1 B WM3J 83 MDC" (per cabrilloSent order with station constants),
        // received is "47 A K1AR 56 CT".
        String k1ar = lines.stream()
            .filter(l -> l.startsWith("QSO:") && l.contains("K1AR"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("K1AR QSO line missing"));
        assertTrue(k1ar.contains("WM3J"),  "K1AR line must show our callsign");
        assertTrue(k1ar.contains(" 1 ") || k1ar.contains(" 1 B "),
            "K1AR line must contain sent serial 1");
        assertTrue(k1ar.contains(" B "),   "K1AR line must contain sent precedence B");
        assertTrue(k1ar.contains(" 83 "),  "K1AR line must contain sent check 83");
        assertTrue(k1ar.contains(" MDC "), "K1AR line must contain sent section MDC");
        assertTrue(k1ar.contains(" 47 "),  "K1AR line must contain rcvd serial 47");
        assertTrue(k1ar.contains(" A "),   "K1AR line must contain rcvd precedence A");
        assertTrue(k1ar.contains(" 56 "),  "K1AR line must contain rcvd check 56");
        assertTrue(k1ar.contains(" CT"),   "K1AR line must contain rcvd section CT");

        // No empty exchange fields on any QSO line — a regression indicator.
        for (String l : lines) {
            if (!l.startsWith("QSO:")) continue;
            assertFalse(l.contains("  ?  ") || l.contains(" ?? "),
                "QSO line should not contain placeholder ? tokens: " + l);
        }

        // QSO lines must appear in chronological order — ARRL Cabrillo spec
        // requires it, and the underlying fetchByContest sorts DESC for the
        // cockpit UI. Catches regression of the sort flip in CabrilloExporter.
        // The serial-sent column is monotonic and easier to compare than the
        // date/time fields, so use it as a stand-in for chronology.
        List<Integer> sentSerials = lines.stream()
            .filter(l -> l.startsWith("QSO:"))
            .map(l -> {
                // QSO line layout: "QSO: <freq> <mode> <date> <time> <mycall> <serial_sent> ..."
                // Tokens 0-6 are fixed; serial_sent is token 6.
                String[] tok = l.trim().split("\\s+");
                return Integer.parseInt(tok[6]);
            })
            .toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), sentSerials,
            "QSO lines must be in chronological (ascending serial) order");
    }
}
