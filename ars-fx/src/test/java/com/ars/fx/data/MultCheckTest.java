package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.StationContext;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MultCheckTest {
    static ContestPlugin ss, wwCw;
    static final StationContext CTX = StationContext.of("WM3J", "FN20", "EPA");

    static ContestPlugin load(String file) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", file).toFile(), ContestPlugin.class);
    }
    @BeforeAll static void plugins() throws Exception { ss = load("arrl_sweepstakes_ssb.json"); wwCw = load("cq_ww_cw.json"); }

    /** A QSO with the field of this id set (through the plugin's own slot mapping). */
    static QsoRecord q(ContestPlugin p, String call, String band, String mode, String fieldId, String value) {
        QsoRecord r = MultCheck.candidateOf(p, call, band, mode);
        r.setRstReceived("59");
        String slot = p.fieldSlotColumn(fieldId);
        if (slot != null) switch (slot) {
            case "field1" -> r.setContestField1(value);
            case "field2" -> r.setContestField2(value);
            case "field3" -> r.setContestField3(value);
            case "field4" -> r.setContestField4(value);
            case "field5" -> r.setContestField5(value);
            default -> {}
        }
        return r;
    }

    @Test void sweepstakesNewSectionIsAMultAndARepeatSectionIsNot() {
        List<QsoRecord> log = new ArrayList<>(List.of(q(ss, "K1ABC", "20m", "SSB", "section", "CT")));
        assertTrue(MultCheck.isNewMult(ss, log, q(ss, "K2XYZ", "20m", "SSB", "section", "ENY"), CTX));
        assertFalse(MultCheck.isNewMult(ss, log, q(ss, "K1DEF", "20m", "SSB", "section", "CT"), CTX), "CT is already worked");
    }

    @Test void sweepstakesSectionsCountOnceAcrossBands() {
        List<QsoRecord> log = List.of(q(ss, "K1ABC", "20m", "SSB", "section", "CT"));
        assertFalse(MultCheck.isNewMult(ss, log, q(ss, "K1DEF", "40m", "SSB", "section", "CT"), CTX), "sections are not per band");
    }

    @Test void aDupeNeverAddsAMultiplier() {
        List<QsoRecord> log = List.of(q(ss, "K1ABC", "20m", "SSB", "section", "CT"));
        // the same station again, claiming a new section: it is a dupe (SS is dupe once per contest)
        assertFalse(MultCheck.isNewMult(ss, log, q(ss, "K1ABC", "20m", "SSB", "section", "ENY"), CTX));
    }

    @Test void cqWwZonesAndCountriesCountPerBand() {
        List<QsoRecord> log = List.of(q(wwCw, "DL1AAA", "20m", "CW", "cq_zone", "14"));
        assertTrue(MultCheck.isNewMult(wwCw, log, q(wwCw, "JA1AAA", "20m", "CW", "cq_zone", "25"), CTX), "a new zone and a new country");
        assertTrue(MultCheck.isNewMult(wwCw, log, q(wwCw, "DL1BBB", "40m", "CW", "cq_zone", "14"), CTX), "the same zone and country on a new band counts again");
        assertFalse(MultCheck.isNewMult(wwCw, log, q(wwCw, "DL1BBB", "20m", "CW", "cq_zone", "14"), CTX), "same zone, country and band");
    }

    @Test void anEmptyCandidateOrContestIsNotAMultiplier() {
        assertFalse(MultCheck.isNewMult(ss, List.of(), null, CTX));
        assertFalse(MultCheck.isNewMult(null, List.of(), q(ss, "K1ABC", "20m", "SSB", "section", "CT"), CTX));
        assertFalse(MultCheck.isNewMult(ss, List.of(), MultCheck.candidateOf(ss, " ", "20m", "SSB"), CTX));
    }

    @Test void contestsWhoseMultiplierIsInTheCallCanFlagSpotsBeforeAnyExchange() throws Exception {
        assertTrue(MultCheck.multFromCallAlone(wwCw, CTX), "CQ WW countries come from the callsign");
        assertFalse(MultCheck.multFromCallAlone(ss, CTX), "Sweepstakes sections are only known from the exchange");
        assertTrue(MultCheck.multFromCallAlone(load("all_asian_dx_cw.json"), CTX), "All Asian DX multipliers come from the callsign");
        assertFalse(MultCheck.multFromCallAlone(load("arrl_dx_cw_us.json"), CTX), "ARRL DX takes its multiplier from a typed field");
        assertFalse(MultCheck.multFromCallAlone(load("cq_wpx_cw.json"), CTX), "WPX takes its prefix from the prefix field of the exchange, not from the call");
    }
}
