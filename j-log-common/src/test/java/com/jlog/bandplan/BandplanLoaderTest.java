package com.jlog.bandplan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BandplanLoaderTest {

    @BeforeEach
    void freshLoad() {
        BandplanLoader.getInstance().reset();
    }

    @Test
    void regionAndCountryBothPopulated() {
        Bandplan r1 = BandplanLoader.getInstance().region("IARU-R1");
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        Bandplan r3 = BandplanLoader.getInstance().region("IARU-R3");
        Bandplan us = BandplanLoader.getInstance().country("US");
        assertNotNull(r1);
        assertNotNull(r2);
        assertNotNull(r3);
        assertNotNull(us);
        for (Bandplan p : new Bandplan[]{r1, r2, r3}) {
            assertTrue(p.bands().size() >= 11,
                p.id() + " should cover at least 11 amateur bands: " + p.bands().size());
        }
        assertEquals(3, BandplanLoader.getInstance().regionIds().size());
        assertTrue(us.bands().size() >= 5,
            "US overlay should cover at least the main HF subbands: " + us.bands().size());
    }

    @Test
    void r1FortyEndsAt7200() {
        Bandplan r1 = BandplanLoader.getInstance().region("IARU-R1");
        BandRange forty = r1.band("40m");
        assertNotNull(forty);
        assertEquals(7_200_000L, forty.upperHz(),
            "R1 40m allocation is 7.0-7.2 MHz, not 7.3");
        // 7.250 should be out of band in R1.
        assertNull(r1.segmentAt(7_250_000L));
        // ...but in band in R2/R3.
        assertNotNull(BandplanLoader.getInstance().region("IARU-R2").segmentAt(7_250_000L));
        assertNotNull(BandplanLoader.getInstance().region("IARU-R3").segmentAt(7_250_000L));
    }

    @Test
    void r1TwoMeterEndsAt146() {
        Bandplan r1 = BandplanLoader.getInstance().region("IARU-R1");
        // R1 2m is 144-146 MHz; 147.000 is out of band.
        assertNull(r1.segmentAt(147_000_000L));
        // R2 / R3 2m goes to 148 MHz.
        assertNotNull(BandplanLoader.getInstance().region("IARU-R2").segmentAt(147_000_000L));
        assertNotNull(BandplanLoader.getInstance().region("IARU-R3").segmentAt(147_000_000L));
    }

    @Test
    void ft8DialIsDataInEveryRegion() {
        // 14.074 MHz is the global FT8 dial — should land in a DATA segment
        // regardless of region.
        for (String id : new String[]{"IARU-R1", "IARU-R2", "IARU-R3"}) {
            BandSegment s = BandplanLoader.getInstance().region(id).segmentAt(14_074_000L);
            assertNotNull(s, id + " is missing a 14.074 segment");
            assertEquals(Activity.DATA, s.activity(),
                id + " should classify 14.074 as DATA, got " + s);
        }
    }

    @Test
    void cwSpotFallsInCwSegment() {
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        BandSegment s = r2.segmentAt(14_060_000L);
        assertNotNull(s);
        assertEquals(Activity.CW, s.activity(), "14.060 MHz is the CW QRP calling frequency");
    }

    @Test
    void ft8DialIsADataSegment() {
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        BandSegment s = r2.segmentAt(14_074_000L);
        assertNotNull(s);
        assertEquals(Activity.DATA, s.activity());
        assertTrue(s.label().toLowerCase().contains("ft8") || s.label().toLowerCase().contains("digi"),
            "FT8 dial label should mention FT8 or digimodes: " + s.label());
    }

    @Test
    void ssbPortionOfTwentyIsPhone() {
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        BandSegment s = r2.segmentAt(14_200_000L);
        assertEquals(Activity.PHONE, s.activity());
    }

    @Test
    void outOfBandReturnsNull() {
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        assertNull(r2.bandAt(13_500_000L), "13.5 MHz isn't in any amateur band");
        assertNull(r2.segmentAt(13_500_000L));
    }

    @Test
    void usOverlayFlagsCwOnlyPortionOf20m() {
        Bandplan us = BandplanLoader.getInstance().country("US");
        BandSegment s = us.segmentAt(14_100_000L);
        assertNotNull(s);
        assertEquals(Activity.CW, s.activity(),
            "US Part 97: 14.0-14.150 is CW/data only — 14.100 must be CW");
    }

    @Test
    void describePrefersCountryOverlay() {
        BandplanLoader.Description d = BandplanLoader.getInstance()
            .describe(14_100_000L, "IARU-R2", "US");
        assertNotNull(d);
        assertEquals("20m", d.band);
        assertEquals(Activity.CW, d.activity);
        assertEquals("US", d.sourceId, "country overlay should win when it has a segment");
    }

    @Test
    void describeFallsBackToRegionWhenNoCountryOverlay() {
        // 145.250 MHz is unambiguously 2m FM-repeater-outputs in R2; the
        // US country overlay has no 2m segments, so describe() must fall
        // back to the IARU R2 plan.
        BandplanLoader.Description d = BandplanLoader.getInstance()
            .describe(145_250_000L, "IARU-R2", "US");
        assertNotNull(d);
        assertEquals("2m", d.band);
        assertEquals(Activity.PHONE, d.activity);
        assertEquals("IARU-R2", d.sourceId,
            "no US 2m overlay yet → should fall back to IARU R2");
    }

    @Test
    void describeReturnsNullForNonAmateurFrequency() {
        assertNull(BandplanLoader.getInstance().describe(11_000_000L),
            "11 MHz is broadcast, not amateur");
    }

    @Test
    void describeWithDefaultsWorks() {
        BandplanLoader.Description d = BandplanLoader.getInstance().describe(7_040_000L);
        assertNotNull(d);
        assertEquals("40m", d.band);
        assertEquals(Activity.CW, d.activity);
    }

    @Test
    void segmentToStringIncludesKhzAndActivity() {
        BandSegment s = new BandSegment(14_000_000L, 14_070_000L, Activity.CW, "CW");
        String str = s.toString();
        assertTrue(str.contains("CW"));
        assertTrue(str.contains("14000"));
    }

    @Test
    void bandSegmentRejectsLowerGreaterThanUpper() {
        assertThrows(IllegalArgumentException.class,
            () -> new BandSegment(14_070_000L, 14_000_000L, Activity.CW, "bad"));
    }
}
