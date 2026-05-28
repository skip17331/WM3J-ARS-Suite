package com.jlog.export;

import com.jlog.db.ContestQsoDao;
import com.jlog.db.DatabaseManager;
import com.jlog.db.ContestQtcDao;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Smoke test for the Cabrillo exporter across every bundled contest plugin.
 *
 * For each plugin: wipe contest_qso, populate AppConfig with dummy values
 * for any constant exchange fields (so the sponsor's robot would see filled
 * tokens), insert 3 dummy QSOs whose field1..5 slots are filled per the
 * plugin's own slot mapping, export to a temp .cbr, and assert the
 * structural invariants the sponsor's robot checks first:
 *
 *   1. START-OF-LOG: 3.0 ... END-OF-LOG: envelope
 *   2. CONTEST: &lt;id-with-dashes&gt; header
 *   3. CALLSIGN: WM3J header
 *   4. Exactly 3 QSO: lines (matches insert count)
 *   5. Each QSO line is well-formed (starts with QSO:, contains mycall +
 *      dxcall, has at least 9 whitespace-separated tokens so there's at
 *      least 1 sent + 1 rcvd exchange token beyond the 7 fixed positions)
 *   6. QSO lines appear in chronological order (dxcalls K1A, K2A, K3A
 *      in insertion order) — pins the 1f59d2c chronological-sort fix
 *
 * Does NOT validate per-sponsor format quirks (token order, padding,
 * specific category headers) — that's what the family deep-dive tests
 * (like CabrilloExporterSsSsbTest) are for. This is the cheap broad
 * net that catches crashes, slot-mapping bugs, blank exchanges, and
 * order regressions across the whole 86-plugin catalog in one CI run.
 */
class CabrilloExporterAllPluginsSmokeTest {

    /** Special entry-field ids that consume no field1..5 slot. Mirror of the
     *  switch statement in {@link ContestPlugin#fieldSlotColumn(String)}. */
    private static final Set<String> SPECIAL_NON_SLOT_IDS = Set.of(
        "callsign", "serial_sent", "serial_rcvd", "band", "mode",
        "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent"
    );

    // JUnit 5's @MethodSource (allPluginIds) is invoked during test
    // discovery, BEFORE @BeforeAll. PluginLoader uses java.util.prefs to
    // resolve the user-installed-plugins path, which throws NPE on a null
    // user.home. Set user.home and bootstrap the loader here so the source
    // sees a populated catalog before the test parameter list is built.
    static {
        try {
            Path tmpHome = Files.createTempDirectory("jhub-cabrillo-smoke-");
            tmpHome.toFile().deleteOnExit();
            System.setProperty("user.home", tmpHome.toString());
            // Order matters:
            //   AppConfig.load() must run first — its prefs field is lazy
            //     and the PluginLoader catalog filter calls
            //     AppConfig.getHiddenContestIds() which dereferences it.
            //   DatabaseManager.initAll() before PluginLoader.init() —
            //     loadUserDir consults DatabaseManager.getDataDir().
            AppConfig.getInstance().load();
            DatabaseManager.getInstance().initAll();
            PluginLoader.getInstance().init();
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap test fixtures", e);
        }
    }

    @BeforeAll
    static void initSuite() {
        // Bootstrap happened in the static initializer above so the
        // @MethodSource catalog is populated. Nothing to do per-suite.

        AppConfig cfg = AppConfig.getInstance();
        cfg.setStationCallsign("WM3J");
        DatabaseManager.getInstance().setConfig("station.grid", "FM19");
        DatabaseManager.getInstance().setConfig("cab.operator", "SINGLE-OP");
        DatabaseManager.getInstance().setConfig("cab.band",     "ALL");
        // Sweepstakes sent constants — only used by ARRL SS plugins, harmless
        // for everything else (resolveField checks isConstant first).
        cfg.setSsPrecedence("B");
        cfg.setSsCheck("83");
        cfg.setSsSection("MDC");
    }

    /** Provide every loaded plugin's contestId as a separate test invocation. */
    static Stream<String> allPluginIds() {
        return PluginLoader.getInstance().getAvailablePlugins().stream()
            .map(ContestPlugin::getContestId)
            .sorted();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allPluginIds")
    void cabrilloExportIsStructurallyValid(String contestId, @TempDir Path dir) throws Exception {
        ContestPlugin plugin = PluginLoader.getInstance().getById(contestId);
        assertNotNull(plugin, "plugin not loaded: " + contestId);

        // Wipe contest_qso + contest_qtc so each plugin starts clean.
        try (Statement st = DatabaseManager.getInstance().getContestConnection().createStatement()) {
            st.executeUpdate("DELETE FROM contest_qso");
            st.executeUpdate("DELETE FROM contest_qtc");
        }
        // Populate AppConfig for every constant entry field this plugin
        // declares — resolveField pulls these via cfg.getContestConstant.
        AppConfig cfg = AppConfig.getInstance();
        if (plugin.getEntryFields() != null) {
            for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
                if (fd.isConstant() && fd.getId() != null) {
                    cfg.setContestConstant(contestId, fd.getId(), "X");
                }
            }
        }

        // Insert 3 QSOs with distinct callsigns (avoid dupe filtering).
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0);
        String[] dxCalls = { "K1A", "K2A", "K3A" };
        for (int i = 0; i < 3; i++) {
            QsoRecord q = buildDummyQso(plugin, dxCalls[i], i + 1, base.plusMinutes(i));
            ContestQsoDao.getInstance().insert(q);
        }
        assertEquals(3, ContestQsoDao.getInstance().fetchByContest(contestId).size(),
            "all 3 dummy QSOs must persist for " + contestId);

        // Export.
        Path out = dir.resolve(contestId + ".cbr");
        try {
            CabrilloExporter.export(plugin, out);
        } catch (Exception e) {
            fail("CabrilloExporter threw on " + contestId + ": " + e.getMessage(), e);
        }
        String cabrillo = Files.readString(out);
        List<String> lines = cabrillo.lines().toList();

        // --- Structural assertions ---
        assertTrue(lines.size() > 5, "Cabrillo file suspiciously short for " + contestId
            + ":\n" + cabrillo);
        assertEquals("START-OF-LOG: 3.0", lines.get(0).trim(),
            "first line must be Cabrillo 3.0 marker for " + contestId);

        // Plugins may declare a sponsor-accepted Cabrillo contest name
        // (e.g. ARRL_DX_SSB_US/DX both emit "ARRL-DX-SSB"). Honor it.
        String expectedHeaderValue = (plugin.getCabrilloContestName() != null
                                      && !plugin.getCabrilloContestName().isBlank())
            ? plugin.getCabrilloContestName()
            : contestId.replace("_", "-");
        String expectedContestHeader = "CONTEST: " + expectedHeaderValue;
        assertTrue(cabrillo.contains(expectedContestHeader),
            "missing '" + expectedContestHeader + "' header for " + contestId);
        assertTrue(cabrillo.contains("CALLSIGN: WM3J"),
            "missing CALLSIGN: WM3J header for " + contestId);
        assertTrue(cabrillo.contains("END-OF-LOG:"),
            "missing END-OF-LOG: terminator for " + contestId);

        List<String> qsoLines = lines.stream().filter(l -> l.startsWith("QSO:")).toList();
        assertEquals(3, qsoLines.size(),
            "expected 3 QSO: lines for " + contestId + " but got " + qsoLines.size()
            + "\n--- Cabrillo dump ---\n" + cabrillo);

        // Per-QSO checks.
        for (int i = 0; i < 3; i++) {
            String line = qsoLines.get(i);
            String dx = dxCalls[i];
            assertTrue(line.contains("WM3J"),
                "QSO line for " + contestId + " missing our callsign:\n" + line);
            assertTrue(line.contains(dx),
                "QSO line " + i + " for " + contestId + " missing dxcall " + dx + ":\n" + line);

            String[] tokens = line.trim().split("\\s+");
            // QSO: freq mode date time mycall <sent...> dxcall <rcvd...>
            // 7 fixed tokens; we want at least 1 sent + 1 rcvd token beyond that.
            assertTrue(tokens.length >= 9,
                "QSO line for " + contestId + " has only " + tokens.length
                + " tokens (need >= 9 for non-empty exchanges):\n" + line);

            // No literal "null" placeholders bleeding through resolveField.
            assertFalse(line.matches(".*\\bnull\\b.*"),
                "QSO line for " + contestId + " contains literal 'null':\n" + line);
        }

        // Chronological ordering — dxcalls must appear in K1A, K2A, K3A order.
        for (int i = 0; i < 3; i++) {
            assertTrue(qsoLines.get(i).contains(dxCalls[i]),
                "chronological order broken for " + contestId
                + ": QSO line " + i + " should mention " + dxCalls[i] + " but is:\n"
                + qsoLines.get(i));
        }
    }

    /** Build a dummy QSO with field1..5 slots populated per the plugin's
     *  own slot mapping. The slot value is "X<slot>" so a regression that
     *  swaps two fields is visible in the Cabrillo dump. */
    private static QsoRecord buildDummyQso(ContestPlugin plugin, String dxCall,
                                            int serial, LocalDateTime when) {
        QsoRecord q = new QsoRecord();
        q.setContestId(plugin.getContestId());
        q.setCallsign(dxCall);
        q.setDateTimeUtc(when);
        q.setBand("20m");
        q.setMode("SSB");
        q.setFrequency("14250");
        q.setSerialSent(Integer.toString(serial));
        q.setSerialReceived(Integer.toString(serial + 10));
        q.setRstSent("59");
        q.setRstReceived("59");
        q.setPoints(1);

        // Fill every slot the plugin declares with "X<slot>" so blank exchanges
        // and slot-swap regressions are visible. The slot assignment must
        // mirror ContestPlugin.fieldSlotColumn exactly.
        int slot = 0;
        Set<String> assigned = new HashSet<>();
        if (plugin.getEntryFields() != null) {
            for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
                if (fd.isConstant()) continue;
                String id = fd.getId();
                if (id == null || SPECIAL_NON_SLOT_IDS.contains(id)) continue;
                if (slot >= 5) break;
                String value = "X" + (slot + 1);
                switch (slot) {
                    case 0 -> q.setContestField1(value);
                    case 1 -> q.setContestField2(value);
                    case 2 -> q.setContestField3(value);
                    case 3 -> q.setContestField4(value);
                    case 4 -> q.setContestField5(value);
                }
                assigned.add(id);
                slot++;
            }
        }
        return q;
    }
}
