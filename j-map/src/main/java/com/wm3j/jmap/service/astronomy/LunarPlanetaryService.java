package com.wm3j.jmap.service.astronomy;

import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Computes lunar position, phase, and planetary positions using Meeus low-precision formulae.
 * Good to approximately 1° — sufficient for rise/set determination and map placement.
 */
public class LunarPlanetaryService {

    private final SolarPositionService solar;

    public LunarPlanetaryService(SolarPositionService solar) {
        this.solar = solar;
    }

    public LunarData compute(ZonedDateTime utc, double obsLat, double obsLon) {
        ZonedDateTime z = utc.withZoneSameInstant(ZoneOffset.UTC);
        double jd = toJulianDay(z);
        double d  = jd - 2451545.0;

        // Sun longitude for phase calculation
        SolarPosition sunPos = solar.computePosition(z);
        double sunLon = sunPos.getSubsolarLon() + 180.0; // approximate ecliptic lon of sun

        // GMST in degrees
        double T = d / 36525.0;
        double gmst = 280.46061837 + 360.98564736629 * d
                    + 0.000387933 * T * T - T * T * T / 38710000.0;
        gmst = norm360(gmst);

        // Obliquity
        double eps = 23.439291111 - 0.013004167 * T;
        double epsRad = Math.toRadians(eps);

        // Moon orbital elements (Meeus Chapter 47, low precision)
        double L = norm360(218.316 + 13.176396 * d);  // mean longitude
        double M = norm360(134.963 + 13.064993 * d);  // mean anomaly
        double F = norm360( 93.272 + 13.229350 * d);  // argument of latitude
        double Msun = norm360(357.529 + 0.98560028 * d);

        double lam = L + 6.289 * Math.sin(Math.toRadians(M))
                       - 1.274 * Math.sin(Math.toRadians(2 * L - M))
                       + 0.658 * Math.sin(Math.toRadians(2 * L))
                       - 0.186 * Math.sin(Math.toRadians(Msun));
        double beta = 5.128 * Math.sin(Math.toRadians(F))
                    - 0.280 * Math.sin(Math.toRadians(M + F))
                    - 0.272 * Math.sin(Math.toRadians(M - F));

        lam  = norm360(lam);
        double lamRad  = Math.toRadians(lam);
        double betaRad = Math.toRadians(beta);

        // Ecliptic → equatorial
        double[] moonEq = eclipticToEquatorial(lamRad, betaRad, epsRad);
        double moonRA  = moonEq[0];  // degrees
        double moonDec = moonEq[1];  // degrees

        // Equatorial → horizontal for observer
        double lstDeg = norm360(gmst + obsLon);
        double ha = norm360(lstDeg - moonRA);
        double[] horiz = equatorialToHorizontal(ha, moonDec, obsLat);
        double moonAz  = horiz[0];
        double moonAlt = horiz[1];

        // Sublunar point
        double subLat = moonDec;
        double subLon = -(gmst - moonRA);
        subLon = norm180(subLon);

        // Phase
        double phaseAngle = norm360(lam - sunLon);
        double illumination = (1.0 - Math.cos(Math.toRadians(phaseAngle))) / 2.0;
        String phaseName = phaseName(phaseAngle);

        LunarData data = new LunarData();
        data.setAzimuth(moonAz);
        data.setAltitude(moonAlt);
        data.setIllumination(illumination);
        data.setPhaseAngle(phaseAngle);
        data.setPhaseName(phaseName);
        data.setSublunarLat(subLat);
        data.setSublunarLon(subLon);
        data.setPlanets(computePlanets(d, gmst, epsRad, sunPos, obsLat, obsLon));

        return data;
    }

    private List<PlanetData> computePlanets(double d, double gmst, double epsRad,
                                             SolarPosition sunPos, double obsLat, double obsLon) {
        // Earth's heliocentric longitude ≈ sunPos ecliptic lon + 180
        double earthLon = norm360(sunPos.getSubsolarLon() + 180.0);

        // [name, L0, dL/day]
        double[][] planets = {
            // Venus
            {181.979, 1.602130},
            // Mars
            {355.433, 0.524071},
            // Jupiter
            { 34.351, 0.083056},
            // Saturn
            { 50.077, 0.033459},
        };
        String[] names    = {"Venus", "Mars", "Jupiter", "Saturn"};

        List<PlanetData> result = new ArrayList<>();

        for (int i = 0; i < planets.length; i++) {
            double planetLon = norm360(planets[i][0] + planets[i][1] * d);

            // Geocentric ecliptic longitude (simplified for ecliptic lat ≈ 0)
            double dLon = Math.toRadians(earthLon - planetLon);
            double geocLon = norm360(Math.toDegrees(
                Math.atan2(Math.sin(dLon), Math.cos(dLon))) + 180.0);

            // Ecliptic → equatorial (beta ≈ 0 for planets)
            double[] eq = eclipticToEquatorial(Math.toRadians(geocLon), 0.0, epsRad);
            double ra  = eq[0];
            double dec = eq[1];

            double lstDeg = norm360(gmst + obsLon);
            double ha = norm360(lstDeg - ra);
            double[] horiz = equatorialToHorizontal(ha, dec, obsLat);

            result.add(new PlanetData(names[i], horiz[0], horiz[1]));
        }
        return result;
    }

    /**
     * Convert ecliptic coordinates to equatorial.
     * @return [RA degrees, Dec degrees]
     */
    private double[] eclipticToEquatorial(double lamRad, double betaRad, double epsRad) {
        double sinBeta = Math.sin(betaRad);
        double cosBeta = Math.cos(betaRad);
        double sinLam  = Math.sin(lamRad);
        double cosLam  = Math.cos(lamRad);
        double sinEps  = Math.sin(epsRad);
        double cosEps  = Math.cos(epsRad);

        double decRad = Math.asin(sinBeta * cosEps + cosBeta * sinEps * sinLam);
        double ra = Math.toDegrees(Math.atan2(
            cosBeta * cosEps * sinLam - sinBeta * sinEps,
            cosBeta * cosLam));
        ra = norm360(ra);
        return new double[]{ra, Math.toDegrees(decRad)};
    }

    /**
     * Convert equatorial (HA, Dec) to horizontal (Az, Alt) for observer.
     * @return [azimuth degrees 0=N, altitude degrees]
     */
    private double[] equatorialToHorizontal(double haDeg, double decDeg, double latDeg) {
        double haRad  = Math.toRadians(haDeg);
        double decRad = Math.toRadians(decDeg);
        double latRad = Math.toRadians(latDeg);

        double sinAlt = Math.sin(decRad) * Math.sin(latRad)
                      + Math.cos(decRad) * Math.cos(latRad) * Math.cos(haRad);
        sinAlt = Math.max(-1.0, Math.min(1.0, sinAlt));
        double altRad = Math.asin(sinAlt);

        double cosAz = (Math.sin(decRad) - Math.sin(latRad) * sinAlt)
                     / (Math.cos(latRad) * Math.cos(altRad));
        cosAz = Math.max(-1.0, Math.min(1.0, cosAz));
        double az = Math.toDegrees(Math.acos(cosAz));
        if (Math.sin(haRad) > 0) az = 360.0 - az;

        return new double[]{az, Math.toDegrees(altRad)};
    }

    private String phaseName(double phaseAngle) {
        if      (phaseAngle <  22.5 || phaseAngle >= 337.5) return "New";
        else if (phaseAngle <  67.5) return "Waxing Crescent";
        else if (phaseAngle < 112.5) return "First Quarter";
        else if (phaseAngle < 157.5) return "Waxing Gibbous";
        else if (phaseAngle < 202.5) return "Full";
        else if (phaseAngle < 247.5) return "Waning Gibbous";
        else if (phaseAngle < 292.5) return "Last Quarter";
        else                         return "Waning Crescent";
    }

    private double toJulianDay(ZonedDateTime utc) {
        int y = utc.getYear();
        int m = utc.getMonthValue();
        int day = utc.getDayOfMonth();
        double h = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0;
        if (m <= 2) { y -= 1; m += 12; }
        int A = y / 100;
        int B = 2 - A + A / 4;
        return Math.floor(365.25 * (y + 4716))
             + Math.floor(30.6001 * (m + 1))
             + day + h / 24.0 + B - 1524.5;
    }

    private double norm360(double v) {
        v = v % 360.0;
        if (v < 0) v += 360.0;
        return v;
    }

    private double norm180(double v) {
        v = v % 360.0;
        if (v > 180)  v -= 360.0;
        if (v < -180) v += 360.0;
        return v;
    }
}
