package com.wm3j.jmap.astronomy;

import com.wm3j.jmap.service.astronomy.SolarPosition;
import com.wm3j.jmap.service.astronomy.SolarPositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the NOAA Solar Position Algorithm implementation.
 * Reference values from NOAA Solar Calculator and JPL Horizons.
 */
class SolarPositionServiceTest {

    private SolarPositionService service;

    @BeforeEach
    void setUp() {
        service = new SolarPositionService();
    }

    @Test
    void subsolarPoint_atVernalEquinox_isNearEquator() {
        // Vernal equinox 2024: March 20, 03:06 UTC
        ZonedDateTime equinox = ZonedDateTime.of(2024, 3, 20, 3, 6, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(equinox);

        // At the equinox, declination should be very close to 0°
        assertEquals(0.0, pos.getDeclination(), 0.5,
            "Declination at vernal equinox should be near 0°");
    }

    @Test
    void subsolarPoint_atSummerSolstice_isNearTropicOfCancer() {
        // Summer solstice 2024: June 20, 20:51 UTC
        ZonedDateTime solstice = ZonedDateTime.of(2024, 6, 20, 20, 51, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(solstice);

        // At summer solstice, declination should be ~+23.44°
        assertEquals(23.44, pos.getDeclination(), 0.2,
            "Declination at summer solstice should be near +23.44°");
    }

    @Test
    void subsolarPoint_atWinterSolstice_isNearTropicOfCapricorn() {
        // Winter solstice 2024: Dec 21, 09:20 UTC
        ZonedDateTime solstice = ZonedDateTime.of(2024, 12, 21, 9, 20, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(solstice);

        // At winter solstice, declination should be ~-23.44°
        assertEquals(-23.44, pos.getDeclination(), 0.2,
            "Declination at winter solstice should be near -23.44°");
    }

    @Test
    void subsolarLongitude_atNoon_isNearPrimeMeridian() {
        // At solar noon on the prime meridian, subsolar longitude ≈ 0°
        ZonedDateTime noon = ZonedDateTime.of(2024, 6, 20, 11, 51, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(noon);

        // Subsolar longitude should be close to 0° at approximate solar noon UTC
        assertTrue(Math.abs(pos.getSubsolarLon()) < 30,
            "Subsolar longitude should be near 0° around noon UTC, got: " + pos.getSubsolarLon());
    }

    @Test
    void subsolarLongitude_changesWith_time() {
        ZonedDateTime t1 = ZonedDateTime.of(2024, 6, 20, 0, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime t2 = ZonedDateTime.of(2024, 6, 20, 6, 0, 0, 0, ZoneOffset.UTC);

        SolarPosition pos1 = service.computePosition(t1);
        SolarPosition pos2 = service.computePosition(t2);

        // 6 hours = 90° of Earth rotation
        double lonDiff = Math.abs(pos2.getSubsolarLon() - pos1.getSubsolarLon());
        // Normalize to 0-180 range
        if (lonDiff > 180) lonDiff = 360 - lonDiff;

        assertEquals(90.0, lonDiff, 3.0,
            "6 hours should correspond to ~90° change in subsolar longitude");
    }

    @Test
    void zenithAngle_atSubsolarPoint_isZero() {
        ZonedDateTime dt = ZonedDateTime.of(2024, 6, 20, 12, 0, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(dt);

        double zenith = service.computeZenithAngle(
            pos.getSubsolarLat(), pos.getSubsolarLon(), dt);

        assertEquals(0.0, zenith, 2.0,
            "Zenith angle at subsolar point should be ~0°");
    }

    @Test
    void zenithAngle_atAntipodalPoint_isNear180() {
        ZonedDateTime dt = ZonedDateTime.of(2024, 6, 20, 12, 0, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(dt);

        // Antipodal point
        double antiLat = -pos.getSubsolarLat();
        double antiLon = pos.getSubsolarLon() + 180;
        if (antiLon > 180) antiLon -= 360;

        double zenith = service.computeZenithAngle(antiLat, antiLon, dt);
        assertTrue(zenith > 150,
            "Zenith angle at antipodal point should be > 150°, got: " + zenith);
    }

    @ParameterizedTest(name = "month={0}")
    @CsvSource({
        "1,  -24.0, -20.0",  // January: sun near -23.44°
        "4,   8.0,  13.0",   // April: sun moving north
        "7,  20.0,  24.5",   // July: near +23.44°
        "10, -10.0,  -5.0",  // October: sun moving south
    })
    void declination_isWithinExpectedRange(int month, double minDec, double maxDec) {
        ZonedDateTime dt = ZonedDateTime.of(2024, month, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(dt);
        assertTrue(pos.getDeclination() >= minDec && pos.getDeclination() <= maxDec,
            String.format("Month %d declination %.2f not in [%.1f, %.1f]",
                month, pos.getDeclination(), minDec, maxDec));
    }

    @Test
    void julianDay_J2000epoch_isCorrect() {
        // J2000.0 = 2000-01-01 12:00 UTC = JD 2451545.0
        ZonedDateTime j2000 = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        SolarPosition pos = service.computePosition(j2000);
        assertEquals(2451545.0, pos.getJulianDay(), 0.01,
            "JD at J2000.0 epoch should be 2451545.0");
    }

    @Test
    void position_isConsistent_overShortInterval() {
        // Two positions 1 second apart should differ by tiny amount
        ZonedDateTime t1 = ZonedDateTime.of(2024, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC);
        ZonedDateTime t2 = t1.plusSeconds(1);

        SolarPosition p1 = service.computePosition(t1);
        SolarPosition p2 = service.computePosition(t2);

        double lonDiff = Math.abs(p2.getSubsolarLon() - p1.getSubsolarLon());
        // 1 second = 360/86400 ≈ 0.00417° of longitude
        assertTrue(lonDiff < 0.01, "1-second longitude change should be tiny: " + lonDiff);
    }
}
