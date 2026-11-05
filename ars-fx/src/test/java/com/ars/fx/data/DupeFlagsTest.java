package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DupeFlagsTest {
    static ContestPlugin ss, ww;

    static ContestPlugin load(String f) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f).toFile(), ContestPlugin.class);
    }
    @BeforeAll static void plugins() throws Exception { ss = load("arrl_sweepstakes_ssb.json"); ww = load("cq_ww_cw.json"); }

    static QsoRecord q(ContestPlugin p, String call, String band, String time, boolean dupe) {
        QsoRecord r = MultCheck.candidateOf(p, call, band, "SSB");
        r.setDateTimeUtc(LocalDateTime.parse("2026-11-28T" + time + ":00"));
        r.setDupe(dupe);
        return r;
    }

    @Test void theSecondContactWithAStationIsTheDupe() {
        QsoRecord a = q(ss, "K1ABC", "20m", "00:10", false), b = q(ss, "K1ABC", "40m", "00:20", false), c = q(ss, "K2XYZ", "20m", "00:30", false);
        List<QsoRecord> changed = DupeFlags.recompute(ss, new ArrayList<>(List.of(c, b, a)));      // out of order on purpose
        assertEquals(List.of(b), changed);
        assertFalse(a.isDupe()); assertTrue(b.isDupe()); assertFalse(c.isDupe());
    }

    @Test void erasingTheFirstContactFreesTheLaterOne() {
        QsoRecord a = q(ss, "K1ABC", "20m", "00:10", false), b = q(ss, "K1ABC", "40m", "00:20", true);
        List<QsoRecord> afterErase = new ArrayList<>(List.of(b));                    // a was erased
        List<QsoRecord> changed = DupeFlags.recompute(ss, afterErase);
        assertEquals(List.of(b), changed);
        assertFalse(b.isDupe(), "now it is the first contact with the station");
    }

    @Test void nothingChangesWhenTheFlagsAreAlreadyRight() {
        QsoRecord a = q(ss, "K1ABC", "20m", "00:10", false), b = q(ss, "K1ABC", "40m", "00:20", true);
        assertTrue(DupeFlags.recompute(ss, new ArrayList<>(List.of(a, b))).isEmpty());
    }

    @Test void perBandContestsOnlyDupeTheSameBand() {
        QsoRecord a = q(ww, "DL1AAA", "20m", "00:10", false), b = q(ww, "DL1AAA", "40m", "00:20", false), c = q(ww, "DL1AAA", "20m", "00:30", false);
        DupeFlags.recompute(ww, new ArrayList<>(List.of(a, b, c)));
        assertFalse(a.isDupe()); assertFalse(b.isDupe(), "a new band is a new contact");
        assertTrue(c.isDupe(), "20 m again");
    }

    @Test void editingAnEarlierQsoCanTurnALaterOneIntoADupe() {
        QsoRecord a = q(ww, "DL1AAA", "40m", "00:10", false), b = q(ww, "DL1AAA", "20m", "00:20", false);
        assertTrue(DupeFlags.recompute(ww, new ArrayList<>(List.of(a, b))).isEmpty());
        a.setBand("20m");                                                              // the operator corrects the band of the first QSO
        List<QsoRecord> changed = DupeFlags.recompute(ww, new ArrayList<>(List.of(a, b)));
        assertEquals(List.of(b), changed);
        assertTrue(b.isDupe());
    }
}
