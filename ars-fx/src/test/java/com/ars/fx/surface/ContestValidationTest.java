package com.ars.fx.surface;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import com.ars.fx.surface.ContestValidation.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContestValidationTest {
    static final Map<String, ContestPlugin> byId = new HashMap<>();

    @BeforeAll
    static void load() throws Exception {
        Path dir = Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins");
        ObjectMapper mapper = new ObjectMapper();
        try (var files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                ContestPlugin p = mapper.readValue(f.toFile(), ContestPlugin.class);
                byId.put(p.getContestId(), p);
            }
        }
    }

    static FieldDef field(ContestPlugin p, String id) {
        return p.getEntryFields().stream().filter(f -> id.equals(f.getId())).findFirst().orElseThrow();
    }
    static Level lv(String contest, String fieldId, String value, boolean complete) {
        ContestPlugin p = byId.get(contest);
        return ContestValidation.check(p, field(p, fieldId), value, complete).level();
    }

    @Test
    void bogusSweepstakesSectionIsRefused() {
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "section", "EMD", true));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "section", "EMD", false), "refused as soon as it can never become valid");
        assertEquals(Level.HARD, lv("ARRL_SS_CW", "section", "ZZ", true));
    }

    @Test
    void realSectionsPassAndPartialOnesAreAllowedWhileTyping() {
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "section", "EMA", true));
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "section", "ema", true));
        assertEquals(Level.PARTIAL, lv("ARRL_SS_SSB", "section", "E", false));
        assertEquals(Level.PARTIAL, lv("ARRL_SS_SSB", "section", "EM", false));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "section", "EM", true), "an incomplete section cannot be logged");
    }

    @Test
    void precedenceAndCheck() {
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "precedence", "A", true));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "precedence", "X", true));
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "check", "77", true));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "check", "7", true));
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "check", "7", false), "one digit is fine while typing");
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "check", "7A", false));
    }

    @Test
    void serialMustBeNumeric() {
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "serial_rcvd", "1A", false));
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "serial_rcvd", "12", true));
    }

    @Test
    void cqZoneRange() {
        assertEquals(Level.OK, lv("CQ_WW_CW", "cq_zone", "5", true));
        assertEquals(Level.OK, lv("CQ_WW_CW", "cq_zone", "05", true));
        assertEquals(Level.HARD, lv("CQ_WW_CW", "cq_zone", "41", false));
        assertEquals(Level.HARD, lv("CQ_WW_CW", "cq_zone", "0", true));
    }

    @Test
    void callsigns() {
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "callsign", "W1AW", true));
        assertEquals(Level.OK, lv("ARRL_SS_SSB", "callsign", "VE3/W1AW", true));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "callsign", "W1@W", false));
        assertEquals(Level.HARD, lv("ARRL_SS_SSB", "callsign", "WWWW", true), "no digit");
    }

    @Test
    void qsoPartyStateOnlyWarns() {
        String qp = byId.keySet().stream().filter(k -> k.endsWith("_QSO_PARTY")).sorted().findFirst().orElseThrow();
        ContestPlugin p = byId.get(qp);
        FieldDef f = p.getEntryFields().stream().filter(x -> "state_prov_rcvd".equals(x.getId())).findFirst().orElse(null);
        if (f == null) return;
        assertEquals(Level.SOFT, ContestValidation.check(p, f, "QQQ", true).level());
        assertEquals(Level.OK, ContestValidation.check(p, f, "OH", true).level());
        assertEquals(Level.OK, ContestValidation.check(p, f, "DX", true).level());
    }

    @Test
    void everyValueTheDefinitionListsIsAccepted() {
        List<String> rejected = new ArrayList<>();
        for (ContestPlugin p : byId.values())
            for (FieldDef fd : p.getEntryFields()) {
                if (fd.getEntryRow() == 1 || "band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
                List<String> vals = new ArrayList<>();
                if (fd.getOptions() != null) vals.addAll(fd.getOptions());
                var mm = p.getMultiplierModel();
                if (mm != null && fd.getId().equals(mm.getField()) && mm.getValidValues() != null) vals.addAll(mm.getValidValues());
                for (String v : vals)
                    if (ContestValidation.check(p, fd, v, true).level() != Level.OK) rejected.add(p.getContestId() + ":" + fd.getId() + "=" + v);
            }
        assertTrue(rejected.isEmpty(), "valid values rejected: " + rejected);
    }
}
