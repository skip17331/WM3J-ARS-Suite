package com.hamradio.jsat.service.orbital;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CoordTransformTest {

    @Test
    void footprintRadiusForIssAltitude() {
        // ISS ~408 km altitude → ~19.7° great-circle radius.
        double deg = CoordTransform.footprintRadiusDeg(408);
        assertEquals(19.7, deg, 0.3);
    }

    @Test
    void footprintRadiusForGeoAltitude() {
        // GEO at 35786 km → ~81.3° (covers ~1/3 of Earth from the equator).
        double deg = CoordTransform.footprintRadiusDeg(35786);
        assertEquals(81.3, deg, 0.5);
    }

    @Test
    void footprintRadiusGrowsMonotonicallyWithAltitude() {
        double prev = 0;
        for (double alt : new double[]{200, 500, 1000, 5000, 20000, 35786}) {
            double r = CoordTransform.footprintRadiusDeg(alt);
            assertTrue(r > prev, "footprint must grow at altitude " + alt);
            prev = r;
        }
    }

    @Test
    void gmstAtJ2000IsApproximately18h41m() {
        // J2000.0 = 2000-01-01 12:00:00 UTC. GMST ≈ 18h 41m 50.5s ≈ 280.46° ≈ 4.895 rad.
        Instant j2000 = ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toInstant();
        double gmst = CoordTransform.gmst(j2000);
        assertEquals(Math.toRadians(280.46), gmst, Math.toRadians(0.5));
    }

    @Test
    void gmstWrapsThroughTwoPiOverADay() {
        Instant t0 = ZonedDateTime.of(2024, 6, 1,  0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        Instant t1 = t0.plusSeconds(12 * 3600); // 12h later
        double a = CoordTransform.gmst(t0);
        double b = CoordTransform.gmst(t1);
        // 12 sidereal hours ≈ π radians difference, give or take a wrap.
        double diff = Math.abs(((b - a) + 4 * Math.PI) % (2 * Math.PI) - Math.PI);
        assertTrue(diff < 0.1, "GMST should advance ≈π rad over 12 h; got diff " + diff);
    }

    @Test
    void azElForOverheadSatellite() {
        // Observer at equator/prime meridian. Place a satellite at GEO altitude
        // directly above (ECI ≈ ECEF when GMST = 0, but GMST is rarely 0; use a
        // contrived position that *will* be overhead given the chosen instant).
        Instant when = ZonedDateTime.of(2024, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();
        double theta = CoordTransform.gmst(when);
        double R = 6378.135 + 35786; // GEO
        // ECI x-axis points to RA=0 at when; we want sat above (lat 0, lon 0):
        // ECEF (R, 0, 0) → ECI = rot(theta) * (R, 0, 0)
        double[] satEci = { R * Math.cos(theta), R * Math.sin(theta), 0 };
        double[] satVel = { 0, 0, 0 }; // ignore rate for this geometric check

        double[] aer = CoordTransform.satAzElRange(satEci, satVel, 0.0, 0.0, 0.0, when);
        assertEquals(90.0, aer[1], 1.0, "elevation should be straight up");
    }
}
