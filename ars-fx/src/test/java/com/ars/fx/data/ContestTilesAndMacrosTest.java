package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ContestTilesAndMacrosTest {
    private static QsoRecord q(String call, String band, boolean dupe) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setDupe(dupe);
        return r;
    }

    // ---- tiles ------------------------------------------------------------------------------------------
    @Test void qsosAndMultsAreCountedByBandInBandOrder() {
        List<QsoRecord> qsos = List.of(q("K1ABC", "20m", false), q("K2ABC", "20m", false), q("K3ABC", "80m", false), q("K4ABC", "20m", true));
        ContestScore s = ContestScore.perBand(3, 3, 3, 9, Map.of("20m", List.of("A", "B"), "80m", List.of("A")));
        List<String[]> t = ContestTiles.compute(List.of("qsosByBand", "multsByBand"), qsos, s, Set.of());
        assertEquals("QSOS BY BAND", t.get(0)[0]);
        assertEquals("80:1  20:2", t.get(0)[1], "dupes are not counted; 80 m comes before 20 m");
        assertEquals("80:1  20:2", t.get(1)[1]);
    }

    @Test void countriesAndPrefixesAreDistinct() {
        List<QsoRecord> qsos = List.of(q("W1AW", "20m", false), q("K1ABC", "20m", false), q("DL1ABC", "20m", false), q("DL2XYZ", "40m", false), q("G3ABC", "20m", true));
        List<String[]> t = ContestTiles.compute(List.of("countries", "prefixes", "dupes"), qsos, ContestScore.of(4, 4, 0, 0), Set.of());
        assertEquals("2", t.get(0)[1], "USA and Germany; the dupe's England is not counted");
        assertEquals("4", t.get(1)[1], "W1, K1, DL1, DL2");
        assertEquals("1", t.get(2)[1]);
    }

    @Test void zonesStatesAndSectionsReadTheScore() {
        ContestScore z = ContestScore.zones(5, 5, 3, 15, List.of("03", "05", "14"));
        List<String[]> t = ContestTiles.compute(List.of("zones", "states", "sections", "mults"), List.of(), z, Set.of("OH", "PA"));
        assertEquals("3 / 40", t.get(0)[1]);
        assertEquals("2", t.get(1)[1]);
        assertEquals("2", t.get(2)[1]);
        assertEquals("2", t.get(3)[1]);
    }

    @Test void aTileWithNothingYetShowsADashAndAnUnknownKindStillTakesItsPlace() {
        List<String[]> t = ContestTiles.compute(List.of("qsosByBand", "zones", "countries", "nonsense"), List.of(), ContestScore.of(0, 0, 0, 0), Set.of());
        assertEquals(4, t.size(), "the number of tiles never changes");
        assertEquals("-", t.get(0)[1]);
        assertEquals("-", t.get(1)[1]);
        assertEquals("-", t.get(2)[1]);
        assertEquals("?", t.get(3)[1]);
        assertTrue(ContestTiles.compute(null, List.of(), null, null).isEmpty());
    }

    // ---- macros -----------------------------------------------------------------------------------------
    @Test void theStandardSetHasEightMessagesAndTheGivenCq() {
        List<MacroConfig.Macro> m = ContestMacros.standard("CQ FD {MYCALL}");
        assertEquals(8, m.size());
        assertEquals("CQ FD {MYCALL}", m.get(0).text());
        assertEquals("{EXCH}", m.get(1).text());
        assertEquals("{CALL} {EXCH}", m.get(3).text());
        assertEquals(ContestMacros.DEFAULT_CQ, ContestMacros.standard("").get(0).text());
    }

    @Test void reportsAreCutAndOtherTokensKept() {
        assertEquals("5NN 05", ContestMacros.cut("599 05"));
        assertEquals("1D ORG", ContestMacros.cut("1d  org"));
        assertEquals("59 OH", ContestMacros.cut("59 OH"), "phone reports are not cut");
        assertEquals("5NN 123", ContestMacros.cut("599 123"));
    }

    @Test void exchAndNrAreFilledInAndStationTagsStillWork() {
        MacroConfig.Expanded x = MacroConfig.expand("{call} {EXCH} #{NR}", "k1abc", Map.of("{EXCH}", "5NN 05", "{NR}", "17"));
        assertEquals("K1ABC 5NN 05 #17", x.text());
        assertTrue(x.ok());
        assertEquals("K1ABC", MacroConfig.expand("{CALL}", "k1abc").text(), "the old two-argument form is unchanged");
    }

    // ---- the layout ---------------------------------------------------------------------------------------
    @Test void aLayoutReadsTilesAndMacroSettingsAndTheOperatorsFileWins() {
        ContestLayout base = ContestLayout.of(Map.of("tiles", List.of("zones", "dupes"), "macroSet", "contest", "cq", "CQ FD {MYCALL}"));
        assertEquals(List.of("zones", "dupes"), base.tiles());
        assertEquals(8, base.contestMacros().size());
        assertEquals("CQ FD {MYCALL}", base.contestMacros().get(0).text());

        ContestLayout own = ContestLayout.of(Map.of("macros", List.of(Map.of("label", "CQ", "text", "CQ SS"), Map.of("label", "bad"), Map.of("label", "TU", "text", "TU"))));
        assertEquals(2, own.macros().size(), "an entry without text is dropped");
        ContestLayout both = base.over(own);
        assertEquals(2, both.contestMacros().size(), "the operator's own list replaces the standard set");
        assertEquals(List.of("zones", "dupes"), both.tiles(), "and the tiles stay");
        assertTrue(ContestLayout.NONE.contestMacros().isEmpty());
    }

    @Test void aLongMacroListIsCutAtEight() {
        List<Map<String, String>> l = new ArrayList<>();
        for (int i = 0; i < 12; i++) l.add(Map.of("label", "M" + i, "text", "T" + i));
        assertEquals(8, ContestLayout.of(Map.of("macros", l)).macros().size());
    }

    // ---- every shipped definition -------------------------------------------------------------------------
    @Test void everyShippedLayoutIsConsistentWithItsDefinition() throws IOException {
        Path dir = Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins");
        int withTiles = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                ContestPlugin p = new ObjectMapper().readValue(f.toFile(), ContestPlugin.class);
                ContestLayout l = ContestLayout.of(p.getLayout());
                String id = f.getFileName().toString();
                for (String k : l.tiles()) assertTrue(ContestTiles.KINDS.contains(k), id + ": unknown tile " + k);
                if (!l.tiles().isEmpty()) withTiles++;
                List<String> ids = p.getEntryFields().stream().map(ContestPlugin.FieldDef::getId).toList();
                for (String fo : l.fieldOrder()) assertTrue(ids.contains(fo), id + ": fieldOrder names a field it does not have: " + fo);
                assertTrue(l.macroSet().isEmpty() || l.macroSet().equals("contest"), id + ": macroSet " + l.macroSet());
                assertTrue(l.macros().size() <= MacroConfig.count(), id);
                if (!l.macroSet().isEmpty()) assertEquals(8, l.contestMacros().size(), id);
            }
        }
        assertTrue(withTiles >= 60, "the CQ, ARRL, Field Day and state party contests have tiles: " + withTiles);
    }
}
