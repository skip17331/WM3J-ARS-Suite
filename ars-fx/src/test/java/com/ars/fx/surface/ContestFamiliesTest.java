package com.ars.fx.surface;

import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The one generic contest page has to work for every contest definition we ship: this is the guard for new ones. */
class ContestFamiliesTest {
    static List<ContestPlugin> all = new ArrayList<>();
    static java.util.Map<String, ContestPlugin> byId = new java.util.HashMap<>();

    /** Read the shipped definitions straight from the engine's resources (the loader needs the user's ~/.j-log). */
    @BeforeAll
    static void load() throws Exception {
        Path dir = Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins");
        ObjectMapper mapper = new ObjectMapper();
        try (var files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                ContestPlugin p = mapper.readValue(f.toFile(), ContestPlugin.class);
                all.add(p); byId.put(p.getContestId(), p);
            }
        }
    }

    @Test
    void shippedContestsAreLoaded() {
        assertTrue(all.size() >= 80, "expected the bundled contest definitions, got " + all.size());
    }

    @Test
    void everySentFieldIsOneThePageCanFillOrAsk() {
        List<String> bad = new ArrayList<>();
        for (ContestPlugin p : all)
            for (FieldDef fd : SentExchange.sentFields(p))
                if (!SentExchange.known(fd.getId())) bad.add(p.getContestId() + ":" + fd.getId());
        assertTrue(bad.isEmpty(), "sent fields the page does not know how to fill: " + bad);
    }

    @Test
    void everyContestHasACallsignField() {
        for (ContestPlugin p : all)
            assertTrue(p.getEntryFields().stream().anyMatch(f -> "callsign".equals(f.getId())), p.getContestId() + " has no callsign field");
    }

    @Test
    void modeFollowsTheContestName() {
        assertEquals("SSB", SentExchange.contestMode(byId.get("ARRL_SS_SSB")));
        assertEquals("CW", SentExchange.contestMode(byId.get("ARRL_SS_CW")));
        assertEquals("CW", SentExchange.contestMode(byId.get("CQ_WW_CW")));
        assertEquals("SSB", SentExchange.contestMode(byId.get("CQ_WW_SSB")));
        assertEquals("RTTY", SentExchange.contestMode(byId.get("CQ_WW_RTTY")));
    }

    @Test
    void mixedModeContestsHaveNoFixedMode() {
        ContestPlugin p = byId.get("ARRL_FIELD_DAY");
        assertNotNull(p);
        assertNull(SentExchange.contestMode(p));
    }

    @Test
    void phoneReportIs59AndOthersAre599() {
        FieldDef rst = new FieldDef(); rst.setId("rst_sent");
        assertEquals("59", SentExchange.value(rst, "SSB"));
        assertEquals("59", SentExchange.value(rst, "USB"));
        assertEquals("599", SentExchange.value(rst, "CW"));
        assertEquals("599", SentExchange.value(rst, "RTTY"));
    }
}
