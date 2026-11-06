package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.MultiplierLists;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Scores the bundled Ontario and Quebec QSO party definitions against the 2026 rules: Ontario phone and CW are 2 points
 * each and the five club / RAC stations are 10; Quebec phone is 1 and CW 2; in both, multipliers count once per band
 * (not per mode), an in-province entrant also gets provinces / states / DX, and an out-of-province entrant gets only the
 * counties (Ontario) or regions (Quebec).
 */
class OntarioQuebecQsoPartyScoreTest {

    private static ContestPlugin load(String id) throws Exception {
        try (InputStream in = OntarioQuebecQsoPartyScoreTest.class.getResourceAsStream("/com/jlog/plugins/" + id + ".json")) {
            assertNotNull(in, id);
            return new ObjectMapper().readValue(in, ContestPlugin.class);
        }
    }

    private static QsoRecord q(String call, String band, String mode, String rcvd) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode); r.setContestField1(rcvd);
        return r;
    }

    /** Stores each QSO's points the way the log does, then scores the lot. */
    private static ContestScore score(ContestPlugin p, String sentQth, QsoRecord... qsos) {
        StationContext ctx = StationContext.of("W1AW", "FN31", sentQth);
        List<QsoRecord> l = new ArrayList<>();
        for (QsoRecord r : qsos) { r.setPoints(ContestScorer.points(p, r, ctx)); l.add(r); }
        return ContestScorer.score(p, l, ctx, "field1", List.of(), 0);
    }

    @Test void multiplierListsAreTheOfficialSizes() throws Exception {
        assertEquals(50, MultiplierLists.load(load("on_qso_party").getMultiplierList()).size());
        assertEquals(43, MultiplierLists.load(load("bc_qso_party").getMultiplierList()).size());
        assertEquals(17, MultiplierLists.load(load("qc_qso_party").getMultiplierList()).size());
    }

    @Test void ontario_inProvince() throws Exception {
        ContestScore s = score(load("on_qso_party"), "OTT",
            q("K1ABC", "20m", "SSB", "PA"),      // phone 2, mult PA / 20m
            q("VA3CCO", "20m", "CW", "TOR"),     // club station 10, mult TOR / 20m
            q("G3ABC", "20m", "CW", "DX"),       // 2, DX entity England / 20m
            q("DL1ABC", "20m", "SSB", "DX"),     // 2, DX entity Germany / 20m (each DXCC country counts)
            q("K1XYZ", "40m", "CW", "PA"));      // 2, PA again but on a new band = new mult
        assertEquals(5, s.qsoCount());
        assertEquals(18, s.points());
        assertEquals(5, s.mults());
        assertEquals(90, s.score());
    }

    @Test void ontario_outOfProvince_onlyOntarioCountiesCount() throws Exception {
        ContestScore s = score(load("on_qso_party"), "PA",
            q("VE3ABC", "20m", "SSB", "OTT"),    // 2, OTT / 20m
            q("VE3ABC", "20m", "CW", "OTT"),     // 2, same band: no new mult (not per mode)
            q("VE3XYZ", "40m", "SSB", "TOR"),    // 2, TOR / 40m
            q("W1AW", "20m", "SSB", "MA"),       // not Ontario: no points
            q("VE3RHQ", "40m", "CW", "TOR"));    // RAC Ontario station 10, TOR / 40m already
        assertEquals(16, s.points());
        assertEquals(2, s.mults());
        assertEquals(32, s.score());
    }

    @Test void quebec_inProvince() throws Exception {
        ContestScore s = score(load("qc_qso_party"), "MTL",
            q("W1AW", "20m", "SSB", "PA"),       // phone 1, PA / 20m
            q("VE3ABC", "20m", "CW", "ON"),      // CW 2, ON / 20m
            q("VE2ABC", "20m", "SSB", "QUE"),    // 1, QUE / 20m
            q("VE2ABC", "20m", "CW", "QUE"),     // 2, same band: no new mult
            q("VE2XYZ", "40m", "SSB", "QUE"),    // 1, QUE / 40m
            q("G3ABC", "20m", "SSB", "DX"));     // 1, one DX multiplier in all
        assertEquals(8, s.points());
        assertEquals(5, s.mults());
        assertEquals(40, s.score());
    }

    @Test void quebec_outOfProvince_onlyQuebecRegionsCount() throws Exception {
        ContestScore s = score(load("qc_qso_party"), "ON",
            q("VE2ABC", "20m", "SSB", "MTL"),    // 1, MTL / 20m
            q("VE2ABC", "20m", "CW", "MTL"),     // 2, same band: no new mult
            q("VE2XYZ", "40m", "SSB", "QUE"),    // 1, QUE / 40m
            q("W1AW", "20m", "SSB", "MA"));      // not Quebec: no points
        assertEquals(4, s.points());
        assertEquals(2, s.mults());
        assertEquals(8, s.score());
    }
}
