package com.wm3j.jmap.service.astronomy;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;

/**
 * Computes solar position using the NOAA Solar Position Algorithm (SPA).
 * Pure calculation - no external API calls.
 *
 * References:
 *   Meeus, Jean. "Astronomical Algorithms", 2nd ed.
 *   NOAA SPA: https://www.nrel.gov/docs/fy08osti/34302.pdf
 */
public class SolarPositionService {

    /**
     * Computes the subsolar point (latitude/longitude of the Sun directly overhead)
     * and solar declination for the given UTC time.
     *
     * @param utc UTC date-time
     * @return SolarPosition with subsolar lat/lon, declination, right ascension, and hour angle
     */
    public SolarPosition computePosition(ZonedDateTime utc) {
        ZonedDateTime z = utc.withZoneSameInstant(ZoneOffset.UTC);
        double jd = toJulianDay(z);

        // Julian centuries from J2000.0
        double T = (jd - 2451545.0) / 36525.0;

        // Geometric mean longitude of the Sun (degrees)
        double L0 = 280.46646 + 36000.76983 * T + 0.0003032 * T * T;
        L0 = normalizeAngle(L0);

        // Mean anomaly of the Sun (degrees)
        double M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T;
        M = normalizeAngle(M);
        double Mrad = Math.toRadians(M);

        // Equation of center
        double C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * Math.sin(Mrad)
                 + (0.019993 - 0.000101 * T) * Math.sin(2 * Mrad)
                 + 0.000289 * Math.sin(3 * Mrad);

        // Sun's true longitude
        double sunLon = L0 + C;

        // Apparent longitude (correcting for aberration and nutation)
        double omega = 125.04 - 1934.136 * T;
        double apparentLon = sunLon - 0.00569 - 0.00478 * Math.sin(Math.toRadians(omega));

        // Obliquity of the ecliptic
        double epsilon0 = 23.439291111 - 0.013004167 * T - 0.000000164 * T * T + 0.000000504 * T * T * T;
        double epsilon = epsilon0 + 0.00256 * Math.cos(Math.toRadians(omega));

        double epsilonRad = Math.toRadians(epsilon);
        double appLonRad = Math.toRadians(apparentLon);

        // Declination (latitude of subsolar point)
        double declinationRad = Math.asin(Math.sin(epsilonRad) * Math.sin(appLonRad));
        double declination = Math.toDegrees(declinationRad);

        // Right Ascension
        double ra = Math.atan2(
            Math.cos(epsilonRad) * Math.sin(appLonRad),
            Math.cos(appLonRad)
        );
        ra = Math.toDegrees(ra);
        ra = normalizeAngle(ra);

        // Greenwich Mean Sidereal Time (GMST) in degrees
        double gmstDeg = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                       + 0.000387933 * T * T - T * T * T / 38710000.0;
        gmstDeg = normalizeAngle(gmstDeg);

        // Greenwich Hour Angle of the Sun (degrees → subsolar longitude)
        double gha = gmstDeg - ra;
        double subsolarLon = -normalizeAngle(gha); // Convert GHA to longitude (-180 to +180)
        if (subsolarLon > 180) subsolarLon -= 360;
        if (subsolarLon < -180) subsolarLon += 360;

        // Solar elevation angle at the subsolar point = 90° by definition
        // but we compute subsolar lat = declination

        return new SolarPosition(declination, subsolarLon, declination, ra, gha, jd);
    }

    /** Compute solar zenith angle at a given lat/lon for the given UTC time */
    public double computeZenithAngle(double lat, double lon, ZonedDateTime utc) {
        SolarPosition pos = computePosition(utc);

        // Hour angle
        double ha = pos.getGha() + lon;
        double haRad = Math.toRadians(ha);
        double latRad = Math.toRadians(lat);
        double decRad = Math.toRadians(pos.getDeclination());

        double cosZ = Math.sin(latRad) * Math.sin(decRad)
                    + Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad);
        cosZ = Math.max(-1.0, Math.min(1.0, cosZ));
        return Math.toDegrees(Math.acos(cosZ));
    }

    private double toJulianDay(ZonedDateTime utc) {
        // Algorithm from Meeus, "Astronomical Algorithms"
        int y = utc.getYear();
        int m = utc.getMonthValue();
        int d = utc.getDayOfMonth();
        double h = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0;

        if (m <= 2) { y -= 1; m += 12; }
        int A = y / 100;
        int B = 2 - A + A / 4;
        return Math.floor(365.25 * (y + 4716))
             + Math.floor(30.6001 * (m + 1))
             + d + h / 24.0 + B - 1524.5;
    }

    private double normalizeAngle(double deg) {
        deg = deg % 360.0;
        if (deg < 0) deg += 360.0;
        return deg;
    }
}
