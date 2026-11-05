package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.AwardLevels.Level;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class AwardLevelsTest {
    private static JLogDb.QsoExport q(String call, String band, String mode, String state, String grid, boolean confirmed) {
        return new JLogDb.QsoExport(call, "2026-01-02 15:30:00", band, mode, "14.2", 0, "59", "59", null, null, state, null, null, grid,
                new JLogDb.Refs("", "", "", 0, 0, false, confirmed, false));
    }
    private static Level find(List<Level> l, String group, String name) {
        return l.stream().filter(x -> x.group().equals(group) && x.name().equals(name)).findFirst().orElseThrow();
    }

    @Test void challengeCountsEntityBandSlotsOnlyOnTheTenChallengeBands() {
        var s = Awards.compute(List.of(
                q("W1AW", "20m", "SSB", "CT", "", true),
                q("W1AW", "40m", "SSB", "CT", "", false),
                q("W1AW", "60m", "SSB", "CT", "", true),        // 60 m does not count for the Challenge
                q("DL1ABC", "20m", "FT8", "", "", false)));
        Level c = find(AwardLevels.compute(s), "DXCC Challenge", "Entity x band slots, 160 m to 6 m (no 60 m)");
        assertEquals(3, c.worked());       // W1AW 20 + 40, DL1ABC 20
        assertEquals(1, c.confirmed());    // only W1AW 20 m
        assertEquals(1000, c.target());
    }

    @Test void fiveBandDxccIsPerBandAndNeedsAllFive() {
        var s = Awards.compute(List.of(q("W1AW", "20m", "SSB", "CT", "", true), q("DL1ABC", "20m", "SSB", "", "", false)));
        var l = AwardLevels.compute(s);
        Level b20 = find(l, "5-Band DXCC", "DXCC on 20m");
        assertEquals(2, b20.worked()); assertEquals(1, b20.confirmed()); assertEquals(100, b20.target());
        assertEquals(0, find(l, "5-Band DXCC", "DXCC on 80m").worked());
        assertEquals(0, find(l, "5-Band DXCC", "Bands completed (of 5)").worked());
    }

    @Test void modeEndorsementsUseCwPhoneDigital() {
        var s = Awards.compute(List.of(q("W1AW", "20m", "CW", "CT", "", true), q("DL1ABC", "20m", "FT8", "", "", true)));
        var l = AwardLevels.compute(s);
        assertEquals(2, find(l, "DXCC by mode", "Mixed").confirmed());
        assertEquals(1, find(l, "DXCC by mode", "CW").confirmed());
        assertEquals(1, find(l, "DXCC by mode", "Digital").confirmed());
        assertEquals(0, find(l, "DXCC by mode", "Phone").worked());
    }

    @Test void fiveBandWasCountsStatesPerBand() {
        List<JLogDb.QsoExport> rows = new ArrayList<>();
        rows.add(q("W1AW", "20m", "SSB", "CT", "", true));
        rows.add(q("K1ABC", "20m", "SSB", "ME", "", false));
        rows.add(q("K2ABC", "40m", "SSB", "NY", "", true));
        rows.add(q("VE3AA", "20m", "SSB", "ON", "", true));      // not a state
        var l = AwardLevels.compute(Awards.compute(rows));
        Level b20 = find(l, "5-Band WAS", "States on 20m");
        assertEquals(2, b20.worked()); assertEquals(1, b20.confirmed()); assertEquals(50, b20.target());
        assertEquals(1, find(l, "5-Band WAS", "States on 40m").confirmed());
    }

    @Test void vuccCountsGridsPerBandAndTheirConfirmation() {
        var s = Awards.compute(List.of(
                q("W1AW", "6m", "SSB", "CT", "FN31", true),
                q("K1ABC", "6m", "SSB", "ME", "FN54", false),
                q("K2ABC", "2m", "SSB", "NY", "FN30", true),
                q("W3ABC", "20m", "SSB", "PA", "FM19", true)));   // HF does not count
        var l = AwardLevels.compute(s);
        Level six = find(l, "VUCC", "Grid squares on 6m");
        assertEquals(2, six.worked()); assertEquals(1, six.confirmed()); assertEquals(100, six.target());
        assertEquals(1, find(l, "VUCC", "Grid squares on 2m").worked());
        assertEquals(0, find(l, "VUCC", "Grid squares on 70cm").worked());
    }

    @Test void emptyLogGivesEveryLevelAtZero() {
        for (Level l : AwardLevels.compute(Awards.compute(List.of()))) { assertEquals(0, l.worked()); assertFalse(l.done()); }
    }
}
