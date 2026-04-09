package com.hamclock.astronomy;

import com.hamclock.service.astronomy.GraylineService;
import com.hamclock.service.astronomy.NightMask;
import com.hamclock.service.astronomy.SolarPositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for great-circle and grayline math.
 */
class GreatCircleMathTest {

    private SolarPositionService solarService;
    private GraylineService graylineService;

    @BeforeEach
    void setUp() {
        solarService  = new SolarPositionService();
        graylineService = new GraylineService(solarService);
    }

    @Test
    void nightMask_atSummerSolsticeMidnight_northPoleIsDay() {
        // At summer solstice, local midnight at 0° longitude
        // The North Pole should be in daylight (midnight sun)
        ZonedDateTime dt = ZonedDateTime.of(2024, 6, 21, 0, 0, 0, 0, ZoneOffset.UTC);
        NightMask mask = graylineService.computeNightMask(dt);

        // North pole (lat=90) should not be in night at summer solstice
        assertFalse(mask.isNight(85.0, 0.0),
            "High Arctic should be in daylight at summer solstice midnight");
    }

    @Test
    void nightMask_atSummerSolsticeMidnight_southPoleIsNight() {
        // At summer solstice, South Pole should be in polar night
        ZonedDateTime dt = ZonedDateTime.of(2024, 6, 21, 0, 0, 0, 0, ZoneOffset.UTC);
        NightMask mask = graylineService.computeNightMask(dt);

        assertTrue(mask.isNight(-85.0, 0.0),
            "Antarctic should be in polar night at Northern summer solstice");
    }

    @Test
    void nightMask_subsolarPoint_isAlwaysDay() {
        ZonedDateTime dt = ZonedDateTime.of(2024, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC);
        NightMask mask = graylineService.computeNightMask(dt);
        var sun = mask.getSolarPosition();

        assertFalse(mask.isNight(sun.getSubsolarLat(), sun.getSubsolarLon()),
            "Subsolar point should always be in daylight");
    }

    @Test
    void nightMask_hasCorrectDimensions() {
        ZonedDateTime dt = ZonedDateTime.now(ZoneOffset.UTC);
        NightMask mask = graylineService.computeNightMask(dt);

        assertEquals(graylineService.getLonSteps(), mask.getLonSteps());
        assertEquals(graylineService.getLatSteps(), mask.getLatSteps());
        assertEquals(graylineService.getLonSteps(), mask.getMask().length);
        assertEquals(graylineService.getLatSteps(), mask.getMask()[0].length);
    }

    @Test
    void nightMask_approximatelyHalfEarthInDaylight() {
        ZonedDateTime dt = ZonedDateTime.of(2024, 3, 20, 12, 0, 0, 0, ZoneOffset.UTC);
        NightMask mask = graylineService.computeNightMask(dt);

        int dayCount  = 0;
        int nightCount = 0;
        boolean[][] m = mask.getMask();

        for (boolean[] col : m) {
            for (boolean night : col) {
                if (night) nightCount++; else dayCount++;
            }
        }

        int total = dayCount + nightCount;
        double nightFraction = (double) nightCount / total;

        // At/near equinox, approximately half should be night
        assertEquals(0.5, nightFraction, 0.1,
            "Near equinox, ~50% of points should be in nighttime, got: " + nightFraction);
    }

    // ── Great-circle distance calculations ──────────────────

    @Test
    void greatCircle_samePoint_isZero() {
        double dist = greatCircleDistanceDeg(51.5, -0.1, 51.5, -0.1);
        assertEquals(0.0, dist, 0.001);
    }

    @Test
    void greatCircle_antipodal_is180() {
        double dist = greatCircleDistanceDeg(0.0, 0.0, 0.0, 180.0);
        assertEquals(180.0, dist, 0.5);
    }

    @Test
    void greatCircle_quarterEarth() {
        // London to a point 90° away
        double dist = greatCircleDistanceDeg(0.0, 0.0, 90.0, 0.0);
        assertEquals(90.0, dist, 0.5);
    }

    @Test
    void bearing_northIs0() {
        // From equator going north
        double bearing = initialBearing(0, 0, 10, 0);
        assertEquals(0.0, bearing, 1.0);
    }

    @Test
    void bearing_eastIs90() {
        double bearing = initialBearing(0, 0, 0, 10);
        assertEquals(90.0, bearing, 1.0);
    }

    @Test
    void bearing_southIs180() {
        double bearing = initialBearing(10, 0, 0, 0);
        assertEquals(180.0, bearing, 1.0);
    }

    @Test
    void bearing_westIs270() {
        double bearing = initialBearing(0, 10, 0, 0);
        assertEquals(270.0, bearing, 1.0);
    }

    @Test
    void longPath_isShortPath_plus180() {
        double shortPath = 45.0;
        double longPath = (shortPath + 180.0) % 360.0;
        assertEquals(225.0, longPath, 0.001);
    }

    @Test
    void longPath_wraps_correctly() {
        double shortPath = 270.0;
        double longPath = (shortPath + 180.0) % 360.0;
        assertEquals(90.0, longPath, 0.001);
    }

    // ── Haversine helper ────────────────────────────────────

    private double greatCircleDistanceDeg(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.toDegrees(c);
    }

    private double initialBearing(double lat1, double lon1, double lat2, double lon2) {
        double lat1r = Math.toRadians(lat1);
        double lat2r = Math.toRadians(lat2);
        double dLon  = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(lat2r);
        double x = Math.cos(lat1r) * Math.sin(lat2r)
                 - Math.sin(lat1r) * Math.cos(lat2r) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }
}
