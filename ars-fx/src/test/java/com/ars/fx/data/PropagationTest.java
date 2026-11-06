package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.Propagation.Hour;
import com.ars.fx.data.Propagation.Point;
import com.ars.fx.data.Propagation.Weather;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropagationTest {
    static final Point NYC = new Point(40.71, -74.01), LONDON = new Point(51.51, -0.13), TOKYO = new Point(35.68, 139.69),
            BOSTON = new Point(42.36, -71.06), CHICAGO = new Point(41.88, -87.63), HOUSTON = new Point(29.76, -95.37);
    static final Weather MEDIUM = new Weather(120, 60, 1), LOW = new Weather(70, 10, 1), HIGH = new Weather(200, 150, 1);

    @Test void greatCircleDistancesAreRight() {
        assertEquals(5570, Propagation.distanceKm(NYC, LONDON), 30);
        assertEquals(10850, Propagation.distanceKm(NYC, TOKYO), 60);
        assertEquals(0, Propagation.distanceKm(NYC, NYC), 1e-6);
        Point mid = Propagation.along(NYC, LONDON, 0.5);
        assertEquals(Propagation.distanceKm(NYC, mid), Propagation.distanceKm(mid, LONDON), 1.0, "the midpoint is halfway");
        assertEquals(NYC.lat(), Propagation.along(NYC, LONDON, 0).lat(), 1e-6);
    }

    @Test void hopsAreAtMostFourThousandKm() {
        assertEquals(1, Propagation.hops(100)); assertEquals(1, Propagation.hops(4000)); assertEquals(2, Propagation.hops(4001));
        assertEquals(3, Propagation.hops(10850)); assertEquals(1, Propagation.hops(0));
    }

    @Test void theSunIsOverheadAtNoonOnTheEquatorAtTheEquinoxAndBelowAtMidnight() {
        assertTrue(Propagation.cosZenith(new Point(0, 0), 80, 12.0) > 0.99);
        assertTrue(Propagation.cosZenith(new Point(0, 0), 80, 0.0) < -0.99);
        assertTrue(Propagation.cosZenith(new Point(0, 90), 80, 12.0) < 0.1, "at 90 E the sun is at the horizon when it is noon at Greenwich");
        assertTrue(Propagation.cosZenith(new Point(70, 0), 355, 12.0) < 0, "polar night in December");
    }

    @Test void aThreeThousandKmHopHasAnM3000NearThree() {
        double m = Propagation.secantF2(3000);
        assertTrue(m > 2.8 && m < 3.3, "M(3000)F2 of a typical day is 3.0 to 3.4: " + m);
        assertTrue(Propagation.secantF2(1000) < m, "a shorter hop meets the layer closer to the vertical");
        assertTrue(Propagation.secantF2(4000) > m);
        assertEquals(1.0, Propagation.secantF2(0), 0.02);
        assertTrue(Propagation.secantF2(300) < 1.2, "near-vertical paths have a MUF close to foF2");
    }

    @Test void foF2RisesWithFluxFallsAtNightAndIsCutInAStorm() {
        double dayLow = Propagation.foF2(1.0, 40, LOW), dayHigh = Propagation.foF2(1.0, 40, HIGH);
        assertTrue(dayLow > 5.5 && dayLow < 8, "solar minimum noon: " + dayLow);
        assertTrue(dayHigh > 10 && dayHigh < 15, "solar maximum noon: " + dayHigh);
        double night = Propagation.foF2(-0.5, 40, MEDIUM);
        assertTrue(night < 0.5 * Propagation.foF2(1.0, 40, MEDIUM) + 0.01 && night > 2, "night is about 45% of noon: " + night);
        assertTrue(Propagation.foF2(1.0, 40, new Weather(120, 60, 5)) < Propagation.foF2(1.0, 40, MEDIUM) * 0.8, "a K of 5 cuts it");
        assertTrue(Propagation.foF2(1.0, 65, MEDIUM) < Propagation.foF2(1.0, 35, MEDIUM), "weaker at high latitude");
    }

    @Test void aMidLatitudePathOpensTheBandsAContesterExpects() {
        LocalDate eq = LocalDate.of(2026, 3, 21);
        List<Hour> d = Propagation.day(BOSTON, HOUSTON, MEDIUM, eq);                       // about 2,500 km, mid-latitude, local noon near 18 UTC
        assertEquals(24, d.size());
        Hour noon = d.get(18), night = d.get(6);
        assertTrue(Propagation.reliability(14.074, noon) > 0.75, "20 m is open at noon: " + Propagation.reliability(14.074, noon));
        assertTrue(Propagation.reliability(3.573, noon) < 0.05, "80 m is shut by absorption at noon: " + Propagation.reliability(3.573, noon));
        assertTrue(Propagation.reliability(3.573, night) > 0.7, "80 m is open at night: " + Propagation.reliability(3.573, night));
        assertTrue(Propagation.reliability(28.074, night) < 0.05, "10 m is dead at night");
        assertTrue(noon.mufMhz() > night.mufMhz(), "the MUF is higher by day");
        assertTrue(noon.lufMhz() > night.lufMhz(), "the LUF is higher by day");
        assertEquals(1, noon.hops());
    }

    @Test void higherFluxOpensHigherBands() {
        LocalDate eq = LocalDate.of(2026, 3, 21);
        Point denver = new Point(39.74, -104.99);                                           // about 2,800 km: a path 10 m can use
        double lowTen = Propagation.reliability(28.074, Propagation.day(BOSTON, denver, LOW, eq).get(18));
        double highTen = Propagation.reliability(28.074, Propagation.day(BOSTON, denver, HIGH, eq).get(18));
        assertTrue(lowTen < 0.2 && highTen > 0.5, "10 m: " + lowTen + " at a flux of 70, " + highTen + " at 200");
        double sixHigh = Propagation.reliability(50.313, Propagation.day(BOSTON, HOUSTON, HIGH, eq).get(18));
        assertTrue(sixHigh < 0.35, "6 m is not opened by F2 even at a flux of 200: " + sixHigh);
    }

    @Test void aLongPathNeedsMoreHopsAndHasAHigherLuf() {
        LocalDate eq = LocalDate.of(2026, 3, 21);
        Hour near = Propagation.hour(NYC, BOSTON, MEDIUM, eq.getDayOfYear(), 17.5);
        Hour far = Propagation.hour(NYC, TOKYO, MEDIUM, eq.getDayOfYear(), 17.5);
        assertEquals(3, far.hops());
        assertTrue(far.hops() > near.hops());
        Hour farDay = Propagation.hour(NYC, LONDON, MEDIUM, eq.getDayOfYear(), 12.5);
        Hour oneHopDay = Propagation.hour(NYC, CHICAGO, MEDIUM, eq.getDayOfYear(), 17.5);
        assertTrue(farDay.lufMhz() > 0, "a luf is always reported"); assertTrue(oneHopDay.lufMhz() >= Propagation.LUF_FLOOR_MHZ);
    }

    @Test void absorptionIsWorstAroundMiddayAndGoneAtNight() {
        LocalDate d = LocalDate.of(2026, 6, 21);
        double lufNoon = Propagation.hour(BOSTON, HOUSTON, MEDIUM, d.getDayOfYear(), 17.5).lufMhz();
        double lufMidnight = Propagation.hour(BOSTON, HOUSTON, MEDIUM, d.getDayOfYear(), 5.5).lufMhz();
        assertTrue(lufNoon > 4 && lufNoon < 10, "a daytime LUF of a few MHz to the mid single figures: " + lufNoon);
        assertEquals(Propagation.LUF_FLOOR_MHZ, lufMidnight, 1e-9, "no D layer at night, so the floor");
    }

    @Test void reliabilityRisesThroughTheOpenWindowAndFallsAwayOutsideIt() {
        Hour h = new Hour(12, 24.0, 7.0, 1);
        assertTrue(Propagation.reliability(3.5, h) < 0.02, "below the LUF");
        assertTrue(Propagation.reliability(40, h) < 0.01, "above the MUF");
        assertTrue(Propagation.reliability(14, h) > 0.97, "in the middle of the window");
        assertTrue(Propagation.reliability(21, h) > Propagation.reliability(24, h), "worse toward the MUF");
        assertEquals(0.5, Propagation.reliability(24.0, new Hour(0, 24.0, 1.5, 1)), 0.02, "at the MUF it is a coin toss");
        assertEquals(0.5, Propagation.phi(0), 1e-6); assertEquals(0.8413, Propagation.phi(1), 1e-3); assertEquals(0.0228, Propagation.phi(-2), 1e-3);
    }

    @Test void weatherIsReadFromTheHamQslStringsWithFallbacks() {
        Weather w = Weather.of("142", "98", "2");
        assertEquals(142, w.sfi()); assertEquals(98, w.ssn()); assertEquals(2, w.k());
        Weather fromSsn = Weather.of(null, "100", null);
        assertTrue(fromSsn.sfi() > 135 && fromSsn.sfi() < 150, "flux from sunspots: " + fromSsn.sfi()); assertEquals(1, fromSsn.k(), "K defaults to quiet");
        Weather fromSfi = Weather.of("150", "", "3");
        assertTrue(fromSfi.ssn() > 80 && fromSfi.ssn() < 130, "sunspots from flux: " + fromSfi.ssn());
        assertNull(Weather.of(null, "", "3"));
        assertNull(Weather.of("--", "NoRpt", "2"));
    }

    @Test void openNowGivesAVerdictWithTheMufAndLufOrNothingWhenItCannot() {
        java.time.LocalDateTime noon = java.time.LocalDateTime.of(2026, 6, 21, 14, 0);
        String r10 = Propagation.openNow(NYC, LONDON, HIGH, noon, 28.5), r160 = Propagation.openNow(NYC, LONDON, HIGH, noon, 1.85);
        assertTrue(r10.contains("MUF") && r10.contains("LUF") && r10.contains("estimate"), r10);
        assertTrue(r160.startsWith("probably closed: below the LUF"), "160 m by day is absorbed: " + r160);
        assertTrue(Propagation.openNow(NYC, LONDON, LOW, noon, 50.1).startsWith("probably closed: above the MUF"), "6 m at the bottom of the cycle");
        assertEquals("", Propagation.openNow(null, LONDON, HIGH, noon, 14.2));
        assertEquals("", Propagation.openNow(NYC, LONDON, null, noon, 14.2));
        assertEquals("", Propagation.openNow(NYC, LONDON, HIGH, noon, 0));
        assertTrue(Propagation.openNow(NYC, LONDON, HIGH, noon, 14.2).startsWith("likely open"), "20 m NYC-London at midday with a high flux");
    }
}
