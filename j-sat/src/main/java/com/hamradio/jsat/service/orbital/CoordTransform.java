package com.hamradio.jsat.service.orbital;

import java.time.Instant;

/**
 * Coordinate transformations: ECI ↔ geodetic, observer AZ/EL, range-rate.
 *
 * All angles in radians unless a method name explicitly states degrees.
 * Distance in km, velocity in km/s.
 */
public final class CoordTransform {

    private static final double XKMPER  = 6378.135;  // Earth equatorial radius (km)
    private static final double F       = 1.0 / 298.257; // Earth flattening
    private static final double OMEGA_E = 7.2921150e-5;  // Earth rotation rate (rad/s)
    private static final double TWOPI   = 2.0 * Math.PI;

    private CoordTransform() {}

    /**
     * Convert ECI position [x,y,z] (km) at a given UTC instant to
     * geodetic latitude (deg), longitude (deg), altitude (km).
     */
    public static double[] eciToGeodetic(double[] eci, Instant when) {
        double theta = gmst(when);  // Greenwich Mean Sidereal Time (radians)

        double x = eci[0], y = eci[1], z = eci[2];

        // Longitude: atan2(y,x) - GMST
        double lon = Math.atan2(y, x) - theta;
        // Normalize to [-π, π]
        while (lon >  Math.PI) lon -= TWOPI;
        while (lon < -Math.PI) lon += TWOPI;

        // Latitude: iterative (Bowring's method, 3 iterations sufficient)
        double r = Math.sqrt(x * x + y * y);
        double lat = Math.atan2(z, r);  // initial spherical approximation
        for (int i = 0; i < 5; i++) {
            double sinLat = Math.sin(lat);
            double N = XKMPER / Math.sqrt(1.0 - 2 * F * sinLat * sinLat + F * F * sinLat * sinLat);
            lat = Math.atan2(z + F * (2 - F) * N * sinLat, r);
        }

        // Altitude
        double sinLat = Math.sin(lat);
        double N = XKMPER / Math.sqrt(1.0 - 2 * F * sinLat * sinLat + F * F * sinLat * sinLat);
        double cosLat = Math.cos(lat);
        double alt = (r / cosLat) - N;
        if (Math.abs(cosLat) < 1e-10) alt = Math.abs(z) / Math.abs(sinLat) - N * (1 - F * F);

        return new double[] { Math.toDegrees(lat), Math.toDegrees(lon), alt };
    }

    /**
     * Compute observer AZ/EL/range given:
     *   satEci  – satellite ECI position [x,y,z] (km)
     *   satVeci – satellite ECI velocity [vx,vy,vz] (km/s)
     *   obsLat  – observer geodetic latitude (deg)
     *   obsLon  – observer longitude (deg)
     *   obsAlt  – observer altitude (km, typically 0)
     *   when    – UTC instant
     *
     * Returns [azimuthDeg, elevationDeg, rangeKm, rangeRateKmSec].
     */
    public static double[] satAzElRange(double[] satEci, double[] satVeci,
                                        double obsLat, double obsLon, double obsAlt,
                                        Instant when) {
        double theta = gmst(when);

        double latR = Math.toRadians(obsLat);
        double lonR = Math.toRadians(obsLon);

        // Observer ECEF position
        double sinLat = Math.sin(latR);
        double cosLat = Math.cos(latR);
        double N = XKMPER / Math.sqrt(1 - 2 * F * sinLat * sinLat + F * F * sinLat * sinLat);
        double rObs = (N + obsAlt);
        double obsX_ecef = rObs * cosLat * Math.cos(lonR);
        double obsY_ecef = rObs * cosLat * Math.sin(lonR);
        double obsZ_ecef = (N * (1 - F) * (1 - F) + obsAlt) * sinLat;

        // Observer ECI (rotate by GMST)
        double obsX = obsX_ecef * Math.cos(theta) - obsY_ecef * Math.sin(theta);
        double obsY = obsX_ecef * Math.sin(theta) + obsY_ecef * Math.cos(theta);
        double obsZ = obsZ_ecef;

        // Observer ECI velocity (due to Earth rotation)
        double obsVx = -OMEGA_E * obsY;
        double obsVy =  OMEGA_E * obsX;
        double obsVz = 0.0;

        // Range vector (ECI)
        double rx = satEci[0] - obsX;
        double ry = satEci[1] - obsY;
        double rz = satEci[2] - obsZ;
        double range = Math.sqrt(rx*rx + ry*ry + rz*rz);

        // Range rate
        double vrx = satVeci[0] - obsVx;
        double vry = satVeci[1] - obsVy;
        double vrz = satVeci[2] - obsVz;
        double rangeRate = (rx*vrx + ry*vry + rz*vrz) / range;

        // Transform range vector to South-East-Zenith topocentric frame
        double sinLon = Math.sin(lonR + theta);
        double cosLon = Math.cos(lonR + theta);

        // SEZ components
        double S =  sinLat * cosLon * rx + sinLat * sinLon * ry - cosLat * rz;
        double E = -sinLon * rx           + cosLon * ry;
        double Z =  cosLat * cosLon * rx + cosLat * sinLon * ry + sinLat * rz;

        double elevation = Math.asin(Z / range);
        double azimuth   = Math.atan2(-E, S) + Math.PI;  // measured N through E

        return new double[] {
            Math.toDegrees(azimuth),
            Math.toDegrees(elevation),
            range,
            rangeRate
        };
    }

    /**
     * Greenwich Mean Sidereal Time in radians at a given UTC instant.
     * Uses the simple formula accurate to ~0.1" over ±50 years from J2000.
     */
    public static double gmst(Instant when) {
        // Julian date
        double jd = when.getEpochSecond() / 86400.0 + 2440587.5
                    + when.getNano() / 86400e9;
        double T  = (jd - 2451545.0) / 36525.0;  // Julian centuries since J2000

        // GMST in seconds of time (IAU 1982)
        double gmstSec = 67310.54841
                       + (8640184.812866 + (0.093104 - 6.2e-6 * T) * T) * T
                       + 86400.0 * (jd - Math.floor(jd));

        double gmstRad = (gmstSec / 240.0) * (Math.PI / 180.0);  // deg → rad (÷15·÷180·π = ÷240·π/180)
        // Simpler: degrees = gmstSec / 240; radians = degrees * PI/180
        double deg = (gmstSec / 240.0) % 360.0;
        return Math.toRadians(deg);
    }

    /**
     * Compute footprint radius in degrees of great-circle arc.
     * altKm = satellite altitude above Earth surface.
     */
    public static double footprintRadiusDeg(double altKm) {
        double earthRad = XKMPER;
        double rhoRad   = Math.acos(earthRad / (earthRad + altKm));
        return Math.toDegrees(rhoRad);
    }

    /**
     * Return true if the satellite (ECI) is in sunlight.
     * Uses a simplified cylindrical shadow model.
     */
    public static boolean inSunlight(double[] satEci, Instant when) {
        double[] sunEci = sunEciKm(when);
        double sunDist = Math.sqrt(sunEci[0]*sunEci[0] + sunEci[1]*sunEci[1] + sunEci[2]*sunEci[2]);
        double sunUx = sunEci[0]/sunDist, sunUy = sunEci[1]/sunDist, sunUz = sunEci[2]/sunDist;

        double satR = Math.sqrt(satEci[0]*satEci[0] + satEci[1]*satEci[1] + satEci[2]*satEci[2]);

        // Projection of sat position along anti-sun direction
        double dot = -(satEci[0]*sunUx + satEci[1]*sunUy + satEci[2]*sunUz);
        if (dot < 0) return true; // Sun side, always illuminated

        // Perpendicular distance from shadow axis
        double px = satEci[0] + dot*sunUx;
        double py = satEci[1] + dot*sunUy;
        double pz = satEci[2] + dot*sunUz;
        double perp = Math.sqrt(px*px + py*py + pz*pz);

        return perp > XKMPER;
    }

    /**
     * Approximate Sun position in ECI (km) using a simplified model.
     */
    public static double[] sunEciKm(Instant when) {
        double jd  = when.getEpochSecond() / 86400.0 + 2440587.5;
        double T   = (jd - 2451545.0) / 36525.0;
        double L0  = 280.46646 + 36000.76983 * T;         // mean longitude (deg)
        double M   = Math.toRadians(357.52911 + 35999.05029 * T);  // mean anomaly
        double C   = (1.914602 - 0.004817*T) * Math.sin(M)
                   + 0.019993 * Math.sin(2*M);
        double slon = Math.toRadians(L0 + C);              // sun longitude (rad)
        double eps  = Math.toRadians(23.439 - 0.0000004 * T); // obliquity

        double AU = 1.496e8; // km
        double dist = (1.000001018 * (1 - 0.01671123 * Math.cos(M))) * AU;

        double x = dist * Math.cos(slon);
        double y = dist * Math.cos(eps) * Math.sin(slon);
        double z = dist * Math.sin(eps) * Math.sin(slon);

        // Rotate from ecliptic ECI to equatorial ECI (already included via eps above)
        return new double[]{ x, y, z };
    }
}
