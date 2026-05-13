package com.hamradio.jsat.service.orbital;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class LunarMathTest {

    /**
     * Sanity-check moon distance: the Moon's geocentric distance varies
     * between perigee (~356,500 km) and apogee (~406,700 km). Sample
     * across several dates and confirm every reading falls inside that
     * window. This is a physics-based bound, not a single reference
     * value, so it's robust to truncated-series accuracy.
     */
    @Test
    void moonDistanceInPhysicalRange() {
        for (int month = 1; month <= 12; month++) {
            Instant when = ZonedDateTime.of(2024, month, 15, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
            double[] eci = LunarMath.moonEciKm(when);
            double dist = Math.sqrt(eci[0]*eci[0] + eci[1]*eci[1] + eci[2]*eci[2]);
            assertTrue(dist > 350_000 && dist < 410_000,
                    "moon distance out of physical range at month " + month + ": " + dist);
        }
    }

    /**
     * Doppler shift on 1296 MHz with a range-rate of +1 km/s (Moon
     * receding) should be roughly −8.6 kHz on the round-trip return.
     */
    @Test
    void dopplerShift1296() {
        double shift = LunarMath.dopplerShiftHz(1_296_000_000.0, 1.0);
        // -2 × 1000 / c × 1.296e9 ≈ -8.65 kHz
        assertEquals(-8650, shift, 50.0);
    }

    /**
     * Doppler magnitude scales linearly with frequency — 432 MHz should
     * see roughly 1/3 the shift of 1296 MHz for the same range-rate.
     */
    @Test
    void dopplerScalesWithFrequency() {
        double s432  = LunarMath.dopplerShiftHz(432_000_000.0, 1.0);
        double s1296 = LunarMath.dopplerShiftHz(1_296_000_000.0, 1.0);
        assertEquals(s1296 / 3.0, s432, 10.0);
    }

    /**
     * Range-rate must be non-zero at typical observation times — the
     * Moon is always moving relative to a ground observer because of
     * Earth rotation plus its own orbital velocity.
     */
    @Test
    void moonHasMeasurableRangeRate() {
        Instant when = ZonedDateTime.of(2026, 6, 15, 22, 0, 0, 0, ZoneOffset.UTC).toInstant();
        // From a mid-latitude QTH (FM19 ≈ 39N 77W)
        double[] aer = LunarMath.moonAzElRange(39.0, -77.0, 0.0, when);
        // Range-rate magnitude is typically 0.1 – 1 km/s for a ground observer
        assertTrue(Math.abs(aer[3]) > 0.05 && Math.abs(aer[3]) < 2.0,
                "moon range-rate out of expected band: " + aer[3]);
    }

    /**
     * Optical libration magnitude stays within physical bounds (~±8°).
     * Spot-check three different dates so we don't accidentally pass on
     * a single coincidental value.
     */
    @Test
    void librationStaysInBounds() {
        for (int year = 2024; year <= 2027; year++) {
            Instant when = ZonedDateTime.of(year, 7, 4, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
            double mag = LunarMath.librationMagnitudeDeg(when);
            assertTrue(mag >= 0 && mag <= 10.0,
                    "libration magnitude out of bounds at " + when + ": " + mag);
        }
    }

    /**
     * Common-window finder returns null when neither station can see
     * the moon (antipodal pair at moon-nadir).
     */
    @Test
    void noCommonWindowAtAntipodes() {
        Instant when = Instant.parse("2026-06-15T12:00:00Z");
        // Two stations on opposite sides of the Earth — they cannot both see
        // the Moon simultaneously by definition.
        LunarMath.MoonWindow w = LunarMath.nextCommonWindow(
            40.0, 0.0, -40.0, 180.0,
            when, java.time.Duration.ofHours(2), 5.0);
        assertNull(w, "antipodal stations cannot share a Moon window");
    }

    /**
     * Moon az/el changes over time — the Moon doesn't stand still.
     * Catches a class of bugs where the position calculator gets cached.
     */
    @Test
    void moonMovesOverTime() {
        double lat = 39.0, lon = -77.0;
        Instant t1 = Instant.parse("2026-06-15T22:00:00Z");
        Instant t2 = t1.plusSeconds(3600);
        double[] a1 = LunarMath.moonAzElRange(lat, lon, 0, t1);
        double[] a2 = LunarMath.moonAzElRange(lat, lon, 0, t2);
        // The Moon should have moved by at least 10° in azimuth in an hour
        // (it tracks roughly 14° per hour at modest elevation).
        double daz = Math.abs(a2[0] - a1[0]);
        // Wrap-around handling
        if (daz > 180) daz = 360 - daz;
        assertTrue(daz > 5.0, "moon azimuth barely moved in 1h: " + daz);
    }
}
