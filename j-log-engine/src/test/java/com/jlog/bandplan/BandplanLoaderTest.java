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
        Bandplan r2 = BandplanLoader.getInstance().region("IARU-R2");
        Bandplan us = BandplanLoader.getInstance().country("US");
        assertNotNull(r2);
        assertNotNull(us);
        assertTrue(r2.bands().size() >= 13,
            "IARU R2 plan should cover all HF + V/UHF bands: " + r2.bands().size());
        assertTrue(us.bands().size() >= 5,
            "US overlay should cover at least the main HF subbands: " + us.bands().size());
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
