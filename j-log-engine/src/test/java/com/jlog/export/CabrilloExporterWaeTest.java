package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.ContestQtcDao;
import com.jlog.db.DatabaseManager;
import com.jlog.model.QsoRecord;
import com.jlog.model.QtcRecord;
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
 * Realistic Cabrillo export check for WAE-DC SSB — exercises the
 * exporter's WAE-specific QTC subsystem ({@link CabrilloExporter}
 * appends one {@code QTC:} line per traffic-list entry after the
 * QSO block per WAE-DC §7).
 *
 * Plugin spec:
 *   cabrilloSent: ["rst_sent", "serial_sent"]   — special, no slots
 *   cabrilloRcvd: ["rst_rcvd", "serial_rcvd"]   — special, no slots
 *
 * QTC format (sponsor: DARC):
 *   QTC: <qrg> <mode> <date> <time> <call-rx> <ser>/<size> <call-tx>
 *        <qso-time> <qso-call> <qso-ser>
 *
 * Scenario: WM3J (non-EU, US side) works 8 EU stations, then sends a
 * 5-entry QTC group to DL5XX and receives a 3-entry QTC group from
 * G3ABC. Both QTC groups must appear after all QSO: lines and before
 * END-OF-LOG:, with correct N/M numbering and TX/RX direction.
 */
class CabrilloExporterWaeTest {

    @BeforeAll
    static void initDb(@TempDir Path tmpHome) throws Exception {
        System.setProperty("user.home", tmpHome.toString());
        AppConfig.getInstance().load();
        DatabaseManager.getInstance().initAll();
        PluginLoader.getInstance().init();
    }

    @BeforeEach
    void wipeContestTables() throws Exception {
        try (Statement st = DatabaseManager.getInstance().getContestConnection().createStatement()) {
            st.executeUpdate("DELETE FROM contest_qso");
            st.executeUpdate("DELETE FROM contest_qtc");
        }
    }

    @Test
    void exportsWaeLogWithQtcGroupsToWellFormedCabrillo(@TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById("WAE_SSB");
        assertNotNull(plugin, "WAE_SSB plugin must be loaded");

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");

        // 8 QSOs working EU. WM3J sends serial 001..008.
        String[] euCalls = {"DL5XX", "G3ABC", "F5RAB", "OH2BH", "OK1AA", "ON4UN", "S52BT", "OE1ZZ"};
        LocalDateTime base = LocalDateTime.of(2026, 8, 8, 0, 0);   // WAE-DC SSB 2nd full Aug weekend
        for (int i = 0; i < euCalls.length; i++) {
            QsoRecord q = new QsoRecord();
            q.setContestId(plugin.getContestId());
            q.setCallsign(euCalls[i]);
            q.setBand("20m");
            q.setFrequency("14245");
            q.setMode("SSB");
            q.setRstSent("59");
            q.setRstReceived("59");
            q.setSerialSent(String.format("%03d", i + 1));
            q.setSerialReceived(String.format("%03d", 100 + i));
            q.setDateTimeUtc(base.plusMinutes(i * 5));
            q.setPoints(1);                     // WAE: 1 pt per QSO
            ContestQsoDao.getInstance().insert(q);
        }

        // 5-entry QTC group SENT (TX) to DL5XX at 0100Z — we replay 5 earlier QSOs.
        LocalDateTime qtcTxAt = base.plusHours(1);
        for (int n = 1; n <= 5; n++) {
            QtcRecord r = new QtcRecord();
            r.setContestId(plugin.getContestId());
            r.setPartnerCall("DL5XX");
            r.setDirection("TX");
            r.setSeriesNo(n);
            r.setSeriesSize(5);
            r.setQtcQrg("14245");
            r.setQtcBand("20m");
            r.setQtcMode("SSB");
            r.setQtcDateTimeUtc(qtcTxAt);
            // Each QTC entry replays one earlier QSO: time + call + serial.
            r.setQsoTime(String.format("%04d", n * 5));         // "0005", "0010", ...
            r.setQsoCall(euCalls[n - 1]);
            r.setQsoSerial(String.format("%03d", n));
            ContestQtcDao.getInstance().insert(r);
        }

        // 3-entry QTC group RECEIVED (RX) from G3ABC at 0200Z.
        LocalDateTime qtcRxAt = base.plusHours(2);
        for (int n = 1; n <= 3; n++) {
            QtcRecord r = new QtcRecord();
            r.setContestId(plugin.getContestId());
            r.setPartnerCall("G3ABC");
            r.setDirection("RX");
            r.setSeriesNo(n);
            r.setSeriesSize(3);
            r.setQtcQrg("14245");
            r.setQtcBand("20m");
            r.setQtcMode("SSB");
            r.setQtcDateTimeUtc(qtcRxAt);
            r.setQsoTime(String.format("%04d", 30 + n * 5));    // "0035", "0040", "0045"
            r.setQsoCall("PA3" + (char) ('A' + n - 1) + "BC");  // fake EU calls G3ABC's log
            r.setQsoSerial(String.format("%03d", 200 + n));
            ContestQtcDao.getInstance().insert(r);
        }

        Path out = dir.resolve("WM3J-wae-ssb.cbr");
        CabrilloExporter.export(plugin, out);

        String cabrillo = Files.readString(out);
        System.out.println("\n=== Generated Cabrillo (" + out.getFileName() + ") ===");
        System.out.println(cabrillo);
        System.out.println("=== end ===\n");

        List<String> lines = cabrillo.lines().toList();
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim());
        assertTrue(cabrillo.contains("CONTEST: WAE-SSB"),
            "header must declare CONTEST: WAE-SSB");
        assertTrue(cabrillo.contains("END-OF-LOG:"));

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        List<String> qtcLines = lines.stream().filter(l -> l.startsWith("QTC:")).toList();
        assertEquals(8, qsoLines.size(), "all 8 QSOs must produce QSO: lines");
        assertEquals(8, qtcLines.size(), "QTC group of 5 + group of 3 = 8 QTC: lines");

        // Block ordering: every QSO: must appear before every QTC: in the file.
        int lastQsoIdx = -1, firstQtcIdx = Integer.MAX_VALUE;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith("QSO:")) lastQsoIdx = i;
            if (lines.get(i).startsWith("QTC:") && i < firstQtcIdx) firstQtcIdx = i;
        }
        assertTrue(lastQsoIdx < firstQtcIdx,
            "all QSO: lines must precede all QTC: lines (WAE-DC §7 ordering)");

        // Spot-check first TX QTC line: "QTC: 14245 PH ... DL5XX 1/5 WM3J ..."
        // ('PH' is the Cabrillo SSB mode token; DL5XX is the recipient,
        // WM3J is the sender.)
        String firstTxQtc = qtcLines.stream().filter(l -> l.contains("DL5XX")).findFirst()
            .orElseThrow(() -> new AssertionError("no TX QTC line for DL5XX in:\n" + cabrillo));
        assertTrue(firstTxQtc.contains(" 1/5 "),
            "first TX QTC must show 1/5 numbering: " + firstTxQtc);
        // recipient (DL5XX) precedes the N/M token; sender (WM3J) follows.
        assertTrue(firstTxQtc.matches(".*DL5XX\\s+1/5\\s+WM3J\\s+.*"),
            "TX QTC layout: recipient 1/5 sender ... : " + firstTxQtc);

        // Spot-check first RX QTC line: G3ABC is sender, WM3J is recipient.
        String firstRxQtc = qtcLines.stream().filter(l -> l.contains(" 1/3 ")).findFirst()
            .orElseThrow(() -> new AssertionError("no RX QTC line with 1/3 in:\n" + cabrillo));
        assertTrue(firstRxQtc.matches(".*WM3J\\s+1/3\\s+G3ABC\\s+.*"),
            "RX QTC layout: WM3J(recipient) 1/3 G3ABC(sender) ... : " + firstRxQtc);
    }
}
