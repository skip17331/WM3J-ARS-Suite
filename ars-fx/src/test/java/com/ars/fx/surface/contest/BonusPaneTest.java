package com.ars.fx.surface.contest;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BonusPaneTest {
    static ContestPlugin load(String f) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f).toFile(), ContestPlugin.class);
    }
    static QsoRecord qso(String call, String mode, boolean dupe) {
        QsoRecord q = new QsoRecord(); q.setCallsign(call); q.setMode(mode); q.setDupe(dupe); return q;
    }

    @Test void aPartyWithBonusStationsListsThemWithPointsAndScope() throws Exception {
        List<BonusPane.Bonus> b = BonusPane.bonuses(load("mo_qso_party.json"));
        assertEquals(2, b.size());
        assertTrue(b.stream().anyMatch(x -> x.call().equals("W0MA") && x.points() == 100 && x.scope().equals("once")));
        List<BonusPane.Bonus> wa = BonusPane.bonuses(load("wa_qso_party.json"));
        assertEquals("once per mode", wa.get(0).scope());
        assertEquals("each QSO", BonusPane.bonuses(load("sc_qso_party.json")).get(0).scope());
    }

    @Test void partiesWithoutBonusesAndOtherContestsHaveNone() throws Exception {
        assertFalse(BonusPane.has(load("vt_qso_party.json")));
        assertFalse(BonusPane.has(load("arrl_sweepstakes_ssb.json")));
        assertFalse(BonusPane.has(null));
        assertTrue(BonusPane.has(load("mo_qso_party.json")));
    }

    @Test void workedMeansAQsoWithTheBaseCallInAModeIgnoringDupesAndSuffixes() throws Exception {
        List<BonusPane.Bonus> b = BonusPane.bonuses(load("mo_qso_party.json"));
        Map<String, Set<String>> w = BonusPane.worked(b, List.of(
                qso("W0MA", "CW", false),
                qso("W0MA/CLAY", "SSB", false),            // a county-expedition suffix is still W0MA
                qso("K0GQ", "SSB", true),                  // a dupe does not count
                qso("W9ZZZ", "CW", false)));
        assertEquals(Set.of("CW", "PH"), w.get("W0MA"));
        assertNull(w.get("K0GQ"));
        assertFalse(w.containsKey("W9ZZZ"));
    }
}
