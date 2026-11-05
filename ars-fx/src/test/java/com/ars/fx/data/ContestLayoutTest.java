package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContestLayoutTest {
    @BeforeEach void dir(@TempDir Path tmp) { ContestLayout.setDirForTest(tmp); }
    @AfterEach void reset() { ContestLayout.setDirForTest(null); }

    static ContestPlugin load(String f) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f).toFile(), ContestPlugin.class);
    }

    @Test void aMapBecomesALayoutAndWrongTypesAreIgnored() {
        ContestLayout l = ContestLayout.of(Map.of("note", " hello ", "tabs", List.of("B", "A"), "hideTabs", "not a list", "fieldOrder", List.of("x", 3, "y"), "defaultMode", "ssb"));
        assertEquals("hello", l.note());
        assertEquals(List.of("B", "A"), l.tabs());
        assertTrue(l.hideTabs().isEmpty());
        assertEquals(List.of("x", "y"), l.fieldOrder());
        assertEquals("SSB", l.modeOrNull());
        assertSame(ContestLayout.NONE, ContestLayout.of(null));
    }

    @Test void orderingPutsListedFirstHidesAndKeepsTheRest() {
        ContestLayout l = new ContestLayout("", List.of("sections worked", "STATISTICS"), List.of("dupe checker"), List.of("section", "callsign"), "");
        assertEquals(List.of("Sections Worked", "Statistics", "Map"), l.orderTabs(List.of("Dupe Checker", "Statistics", "Map", "Sections Worked")));
        assertEquals(List.of("section", "callsign", "check", "precedence"), l.orderFields(List.of("callsign", "check", "section", "precedence")));
        assertEquals(List.of("a", "b"), ContestLayout.NONE.orderFields(List.of("a", "b")), "no layout, no change");
    }

    @Test void theOperatorsPartsReplaceTheBuiltInOnesPartByPart() {
        ContestLayout built = new ContestLayout("built-in note", List.of("A"), List.of("H"), List.of("f1"), "CW");
        ContestLayout mine = new ContestLayout("", List.of("B"), List.of(), List.of(), "SSB");
        ContestLayout m = built.over(mine);
        assertEquals("built-in note", m.note());
        assertEquals(List.of("B"), m.tabs());
        assertEquals(List.of("H"), m.hideTabs());
        assertEquals(List.of("f1"), m.fieldOrder());
        assertEquals("SSB", m.modeOrNull());
    }

    @Test void theShippedSweepstakesLayoutLoadsAndPutsSectionsFirst() throws Exception {
        ContestPlugin ss = load("arrl_sweepstakes_ssb.json");
        ContestLayout l = ContestLayout.resolve(ss);
        assertFalse(l.note().isEmpty(), "the built-in note");
        assertEquals("Sections Worked", l.orderTabs(List.of("Statistics", "Sections Worked")).get(0));
        ContestLayout ww = ContestLayout.resolve(load("cq_ww_cw.json"));
        assertEquals("Multipliers count once per band.", ww.note(), "CQ WW carries only the note its own definition supports");
        assertTrue(ww.tabs().isEmpty() && ww.hideTabs().isEmpty() && ww.fieldOrder().isEmpty(), "no ordering: the standard page");
    }

    @Test void theOperatorsFileIsLayeredOverTheBuiltInOne(@TempDir Path tmp) throws Exception {
        ContestLayout.setDirForTest(tmp);
        ContestPlugin ss = load("arrl_sweepstakes_ssb.json");
        Files.writeString(ContestLayout.userFile(ss.getContestId()), "{\"layout\": {\"note\": \"my own reminder\", \"hideTabs\": [\"Statistics\"]}}");
        ContestLayout l = ContestLayout.resolve(ss);
        assertEquals("my own reminder", l.note());
        assertEquals(List.of("Statistics"), l.hideTabs());
        assertEquals("Sections Worked", l.orderTabs(List.of("Statistics", "Sections Worked")).get(0), "the built-in tab order still applies");
        // the keys may also sit at the top level of the file
        Files.writeString(ContestLayout.userFile(ss.getContestId()), "{\"note\": \"flat\"}");
        assertEquals("flat", ContestLayout.resolve(ss).note());
    }

    @Test void aBrokenFileChangesNothing(@TempDir Path tmp) throws Exception {
        ContestLayout.setDirForTest(tmp);
        ContestPlugin ss = load("arrl_sweepstakes_ssb.json");
        Files.writeString(ContestLayout.userFile(ss.getContestId()), "{ this is not json");
        assertEquals(ContestLayout.of(ss.getLayout()), ContestLayout.resolve(ss));
    }

    @Test void theStarterFileHoldsTheCurrentLayoutAndIsNotOverwritten(@TempDir Path tmp) throws Exception {
        ContestLayout.setDirForTest(tmp);
        ContestPlugin ss = load("arrl_sweepstakes_ssb.json");
        Path f = ContestLayout.ensureUserFile(ss, List.of("Statistics", "Sections Worked"), List.of("callsign", "section"));
        String text = Files.readString(f);
        assertTrue(text.contains("\"layout\"") && text.contains("Sections Worked") && text.contains("fieldOrder") && text.contains("_help"));
        Files.writeString(f, "{\"layout\": {\"note\": \"edited\"}}");
        ContestLayout.ensureUserFile(ss, List.of(), List.of());
        assertEquals("edited", ContestLayout.resolve(ss).note(), "an existing file is left alone");
    }
}
