package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.BandMapModel.Kind;
import com.ars.fx.data.BandMapModel.Mark;
import com.ars.fx.data.BandMapModel.Placed;
import java.util.List;
import org.junit.jupiter.api.Test;

class BandMapModelTest {
    static final long NOW = 1_000_000_000L;
    static Mark m(String call, long hz, long ageMin, Kind k) { return new Mark(call, hz, "CW", "20m", NOW - ageMin * 60_000L, k); }

    @Test void edgesComeFromTheSharedBandPlan() {
        long[] e = BandMapModel.edges(14_074_000L);
        assertNotNull(e);
        assertEquals(14_000_000L, e[0]);
        assertTrue(e[1] >= 14_349_000L && e[1] <= 14_350_000L, "20 m ends at 14.350: " + e[1]);
        long[] t = BandMapModel.edges(10_130_000L);
        assertEquals(10_100_000L, t[0]); assertTrue(t[1] >= 10_149_000L && t[1] <= 10_150_000L);
        assertNull(BandMapModel.edges(14_500_000L), "not in an amateur band");
    }

    @Test void spotsFadeFromFreshToOneThirdOverAnHour() {
        assertEquals(1.0, BandMapModel.alpha(0));
        assertEquals(1.0, BandMapModel.alpha(10 * 60_000L));
        assertEquals(BandMapModel.MIN_ALPHA, BandMapModel.alpha(60 * 60_000L));
        assertEquals(BandMapModel.MIN_ALPHA, BandMapModel.alpha(5 * 3_600_000L));
        double mid = BandMapModel.alpha(35 * 60_000L);
        assertTrue(mid < 1.0 && mid > BandMapModel.MIN_ALPHA);
    }

    @Test void selectKeepsOneSpotPerCallInRangeAndNotTooOld() {
        List<Mark> in = List.of(
                m("K1ABC", 14_025_000L, 20, Kind.NORMAL),
                m("k1abc", 14_025_100L, 3, Kind.NORMAL),            // the same station spotted again, later: keep this one
                m("W1AW", 14_030_000L, 90, Kind.NORMAL),            // too old
                m("N1MM", 14_100_000L, 5, Kind.NORMAL),
                m("DL1XYZ", 7_010_000L, 1, Kind.NORMAL));            // another band
        List<Mark> out = BandMapModel.select(in, 14_000_000L, 14_350_000L, NOW, 50);
        assertEquals(2, out.size());
        assertEquals(14_025_100L, out.stream().filter(x -> x.call().equalsIgnoreCase("K1ABC")).findFirst().get().freqHz());
        assertTrue(out.stream().noneMatch(x -> x.call().equals("W1AW") || x.call().equals("DL1XYZ")));
    }

    @Test void whenThereAreTooManyTheNeededOnesAndTheNewestSurvive() {
        List<Mark> in = List.of(
                m("OLD1", 14_010_000L, 50, Kind.NORMAL), m("OLD2", 14_020_000L, 40, Kind.NORMAL),
                m("NEW1", 14_030_000L, 1, Kind.NORMAL), m("NEW2", 14_040_000L, 2, Kind.NORMAL),
                m("NEED", 14_050_000L, 55, Kind.NEEDED));
        List<Mark> out = BandMapModel.select(in, 14_000_000L, 14_350_000L, NOW, 3);
        assertEquals(3, out.size());
        assertTrue(out.stream().anyMatch(x -> x.call().equals("NEED")), "a needed spot is never dropped for a newer ordinary one");
        assertTrue(out.stream().anyMatch(x -> x.call().equals("NEW1")) && out.stream().anyMatch(x -> x.call().equals("NEW2")));
    }

    @Test void labelsSitAtTheirFrequencyWhenThereIsRoom() {
        List<Placed> p = BandMapModel.layout(List.of(m("A1A", 14_000_000L, 0, Kind.NORMAL), m("B1B", 14_175_000L, 0, Kind.NORMAL), m("C1C", 14_350_000L, 0, Kind.NORMAL)),
                14_000_000L, 14_350_000L, 700, 16, NOW);
        assertEquals(0.0, p.get(0).trueY(), 1e-9);
        assertEquals(350.0, p.get(1).trueY(), 1e-9);
        assertEquals(350.0, p.get(1).labelY(), 1e-9, "no neighbour near, so the label is not moved");
        assertEquals(700.0, p.get(2).trueY(), 1e-9);
    }

    @Test void crowdedLabelsAreSpreadApartKeepingTheirOrder() {
        List<Mark> marks = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) marks.add(m("S" + i, 14_100_000L + i * 200L, 0, Kind.NORMAL));      // eight spots within 1.4 kHz
        List<Placed> p = BandMapModel.layout(marks, 14_000_000L, 14_350_000L, 700, 16, NOW);
        for (int i = 1; i < p.size(); i++) {
            assertTrue(p.get(i).labelY() - p.get(i - 1).labelY() >= 16 - 1e-9, "labels keep their distance: " + p);
            assertTrue(p.get(i).mark().freqHz() >= p.get(i - 1).mark().freqHz(), "and their order");
        }
    }

    @Test void aCrowdAtTheTopOfTheBandIsPulledBackInsideTheRuler() {
        List<Mark> marks = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) marks.add(m("T" + i, 14_349_000L + i * 100L, 0, Kind.NORMAL));
        List<Placed> p = BandMapModel.layout(marks, 14_000_000L, 14_350_000L, 400, 20, NOW);
        assertTrue(p.get(p.size() - 1).labelY() <= 400, "the last label stays on the ruler");
        for (int i = 1; i < p.size(); i++) assertTrue(p.get(i).labelY() - p.get(i - 1).labelY() >= 20 - 1e-9);
        assertTrue(p.get(0).labelY() >= 0);
    }

    @Test void layoutOfNothingIsNothing() {
        assertTrue(BandMapModel.layout(List.of(), 14_000_000L, 14_350_000L, 500, 16, NOW).isEmpty());
        assertTrue(BandMapModel.select(List.of(), 14_000_000L, 14_350_000L, NOW, 10).isEmpty());
    }

    @Test void ticksAreSpacedForTheRulerHeight() {
        assertEquals(25_000L, BandMapModel.tickStepHz(14_000_000L, 14_350_000L, 600, 40), "25 kHz is 43 px on a 350 kHz band 600 px tall");
        assertEquals(1_000L, BandMapModel.tickStepHz(14_070_000L, 14_080_000L, 600, 40), "a 10 kHz window gets a tick every kHz");
        assertEquals(14_025_000L, BandMapModel.firstTick(14_000_001L, 25_000L));
        assertEquals(14_000_000L, BandMapModel.firstTick(14_000_000L, 25_000L));
    }

    @Test void theBusiestBandIsTheOneWithTheMostSpots() {
        List<Mark> marks = List.of(m("A1A", 14_025_000L, 0, Kind.NORMAL), m("B1B", 14_030_000L, 0, Kind.NORMAL), m("C1C", 7_010_000L, 0, Kind.NORMAL), m("D1D", 99_000L, 0, Kind.NORMAL));
        assertEquals("20m", BandMapModel.busiestBand(marks));
        assertNull(BandMapModel.busiestBand(List.of()));
    }

    @Test void segmentsJoinRunsOfTheSameActivityAndSkipGaps() {
        java.util.function.LongFunction<String> plan = hz -> hz < 14_070_000L ? "CW" : hz < 14_150_000L ? "DATA" : hz < 14_160_000L ? "" : "PHONE";
        List<BandMapModel.Segment> sg = BandMapModel.segments(14_000_000L, 14_350_000L, plan);
        assertEquals(List.of("CW", "DATA", "PHONE"), sg.stream().map(BandMapModel.Segment::activity).toList());
        assertEquals(14_000_000L, sg.get(0).loHz());
        assertEquals(14_350_000L, sg.get(2).hiHz());
        assertTrue(Math.abs(sg.get(0).hiHz() - 14_070_000L) <= 1_000, "the CW edge is found to within a step");
        assertEquals(List.of(), BandMapModel.segments(14_000_000L, 14_000_000L, plan), "an empty range has no segments");
        assertEquals(List.of(), BandMapModel.segments(1, 1000, hz -> ""), "a plan that says nothing gives none");
    }

    @Test void aClickOnTheRulerIsAFrequencyToTheNearest100HzInsideTheRange() {
        assertEquals(14_000_000L, BandMapModel.freqAt(10, 10, 300, 14_000_000L, 14_350_000L));
        assertEquals(14_350_000L, BandMapModel.freqAt(310, 10, 300, 14_000_000L, 14_350_000L));
        assertEquals(14_175_000L, BandMapModel.freqAt(160, 10, 300, 14_000_000L, 14_350_000L));
        assertEquals(14_000_000L, BandMapModel.freqAt(-50, 10, 300, 14_000_000L, 14_350_000L), "above the ruler stays in range");
        assertEquals(14_350_000L, BandMapModel.freqAt(900, 10, 300, 14_000_000L, 14_350_000L));
        assertEquals(0L, BandMapModel.freqAt(160, 10, 300, 14_000_000L, 14_350_000L) % 100);
    }

    @Test void aMarkFromTheClusterIsNotLocalUnlessSaidSo() {
        assertFalse(m("K1ABC", 14_025_000L, 1, Kind.NORMAL).local());
        assertTrue(new Mark("K1ABC", 14_025_000L, "CW", "20m", NOW, Kind.NORMAL, true).local());
    }
}
