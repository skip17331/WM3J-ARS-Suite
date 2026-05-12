package com.hamradio.jsat.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises TleSet parsing against a known ISS (ZARYA) two-line element
 * set. Values are deterministic — the columns the parser reads have
 * fixed positions defined by the TLE format spec, so a hand-checked
 * fixture is the cleanest unit-test bed.
 */
class TleSetTest {

    // Real ISS TLE epoch 2024-day-001 (cosmetic — not used by SGP4 in this test).
    private static final String ISS_L1 =
        "1 25544U 98067A   24001.50000000  .00012345  00000-0  22456-3 0  9991";
    private static final String ISS_L2 =
        "2 25544  51.6400 124.6789 0006789  12.3456 347.8901 15.49012345456789";

    @Test
    void parsesNoradId() {
        TleSet t = new TleSet("ISS (ZARYA)", ISS_L1, ISS_L2);
        assertEquals("25544", t.noradId);
    }

    @Test
    void parsesOrbitalElements() {
        TleSet t = new TleSet("ISS (ZARYA)", ISS_L1, ISS_L2);
        assertEquals(51.6400,    t.inclination,  1e-4);
        assertEquals(124.6789,   t.raan,         1e-4);
        assertEquals(0.0006789,  t.eccentricity, 1e-7);
        assertEquals(12.3456,    t.argPerigee,   1e-4);
        assertEquals(347.8901,   t.meanAnomaly,  1e-4);
        assertEquals(15.49012345, t.meanMotion,  1e-7);
    }

    @Test
    void parsesEpochYearAndDay() {
        TleSet t = new TleSet("ISS (ZARYA)", ISS_L1, ISS_L2);
        assertEquals(2024, t.epochYear);
        assertEquals(1.50000000, t.epochDay, 1e-7);
    }

    @Test
    void parsesBstarExponentialNotation() {
        TleSet t = new TleSet("ISS (ZARYA)", ISS_L1, ISS_L2);
        // "22456-3" → 0.22456 × 10⁻³  = 0.00022456
        assertEquals(0.00022456, t.bstar, 1e-9);
    }

    @Test
    void freshnessGreenWhenJustParsed() {
        // Construct a TLE whose epoch is "today" — needs current year + day-of-year.
        Instant now = Instant.now();
        int year = now.atZone(java.time.ZoneOffset.UTC).getYear();
        int doy  = now.atZone(java.time.ZoneOffset.UTC).getDayOfYear();
        String epochField = String.format("%02d%03d.00000000",
            year - 2000, Math.max(1, doy));
        // Build minimal valid line 1 — only the cols our parser reads matter.
        String line1 = "1 25544U 98067A   " + epochField
                     + "  .00000000  00000-0  00000-0 0  9999";
        String line2 = "2 25544  51.0000 000.0000 0000000   0.0000   0.0000 15.50000000 00001";
        TleSet t = new TleSet("FRESH", line1, line2);
        assertEquals(TleSet.TleFreshness.GREEN, t.freshness(),
            "TLE for today should be GREEN");
    }

    @Test
    void freshnessRedWhenAncient() {
        // Year 2020 day 001
        String line1 = "1 25544U 98067A   20001.00000000  .00000000  00000-0  00000-0 0  9999";
        String line2 = "2 25544  51.0000 000.0000 0000000   0.0000   0.0000 15.50000000 00001";
        TleSet t = new TleSet("ANCIENT", line1, line2);
        assertEquals(TleSet.TleFreshness.RED, t.freshness());
        assertTrue(t.ageInDays() > 7, "age must be > 7 days for RED");
    }
}
