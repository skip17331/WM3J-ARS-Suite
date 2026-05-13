package com.hamradio.jsat.service.orbital;

import java.time.Instant;

/**
 * LunarMath — low-precision lunar ephemeris for EME (Moon-bounce) operators.
 *
 * <p>EME doesn't need arcsecond accuracy — operators are pointing dishes
 * and Yagi arrays, and the Moon is half a degree wide. What they need is:
 * <ul>
 *   <li>Where the Moon is right now (az/el) so they can point at it.</li>
 *   <li>Range-rate so they can correct Doppler before TX.</li>
 *   <li>Optical libration so they know whether a JT65 window or a Q65
 *       window makes more sense for the path.</li>
 *   <li>Moon rise/set times so they can plan a sked.</li>
 * </ul>
 *
 * <p>Math comes from Meeus, <em>Astronomical Algorithms</em> chapter 47
 * (low-precision lunar position) and chapter 53 (libration). Accuracy
 * runs ~10 arc-minutes in position and ~0.1° in libration — well inside
 * the half-degree the Moon subtends and well inside the half-dB EME
 * Doppler matters at.
 *
 * <p>All angles in degrees in the public surface; internal calculations
 * use radians.
 */
public final class LunarMath {

    /** Speed of light, m/s — used by {@link #dopplerShiftHz}. */
    public static final double C_MS = 299_792_458.0;

    private LunarMath() {}

    // ---------------------------------------------------------------
    // Moon position (geocentric ECI)
    // ---------------------------------------------------------------

    /**
     * Compute the Moon's geocentric ECI position in km at the given UTC
     * instant. Returns {x, y, z}, equatorial frame of date.
     *
     * <p>Low-precision Meeus chapter 47 (truncated ELP-2000). Accurate
     * to ~10 arc-minutes in position and ~30 km in distance — fine for
     * EME pointing.
     */
    public static double[] moonEciKm(Instant when) {
        double T = julianCenturiesJ2000(when);

        // Mean longitude of the Moon (degrees)
        double L = 218.3164477 + 481267.88123421*T - 0.0015786*T*T + T*T*T/538841.0;
        // Mean elongation of the Moon from the Sun
        double D = 297.8501921 + 445267.1114034*T - 0.0018819*T*T + T*T*T/545868.0;
        // Sun's mean anomaly
        double M = 357.5291092 + 35999.0502909*T - 0.0001536*T*T + T*T*T/24490000.0;
        // Moon's mean anomaly
        double Mp = 134.9633964 + 477198.8675055*T + 0.0087414*T*T + T*T*T/69699.0;
        // Moon's argument of latitude
        double F  = 93.2720950 + 483202.0175233*T - 0.0036539*T*T - T*T*T/3526000.0;

        double Lr  = Math.toRadians(norm(L));
        double Dr  = Math.toRadians(norm(D));
        double Mr  = Math.toRadians(norm(M));
        double Mpr = Math.toRadians(norm(Mp));
        double Fr  = Math.toRadians(norm(F));

        // Truncated ELP-2000 — biggest periodic terms for longitude (deg),
        // latitude (deg), and distance from Earth centre (km).
        double lon = norm(L)
            + 6.289 * Math.sin(Mpr)
            - 1.274 * Math.sin(2*Dr - Mpr)
            + 0.658 * Math.sin(2*Dr)
            - 0.186 * Math.sin(Mr)
            - 0.059 * Math.sin(2*Mpr - 2*Dr)
            - 0.057 * Math.sin(Mpr + Mr - 2*Dr)
            + 0.053 * Math.sin(Mpr + 2*Dr)
            + 0.046 * Math.sin(2*Dr - Mr)
            + 0.041 * Math.sin(Mpr - Mr)
            - 0.035 * Math.sin(Dr)
            - 0.031 * Math.sin(Mpr + Mr);

        double lat =
              5.128 * Math.sin(Fr)
            + 0.281 * Math.sin(Mpr + Fr)
            + 0.278 * Math.sin(Mpr - Fr)
            + 0.173 * Math.sin(2*Dr - Fr)
            + 0.055 * Math.sin(2*Dr + Fr - Mpr)
            + 0.046 * Math.sin(2*Dr - Fr - Mpr)
            + 0.033 * Math.sin(2*Dr + Fr)
            + 0.017 * Math.sin(2*Mpr + Fr);

        // Distance in km (geocentric)
        double dist = 385000.56
            - 20905.355 * Math.cos(Mpr)
            -  3699.111 * Math.cos(2*Dr - Mpr)
            -  2955.968 * Math.cos(2*Dr)
            -   569.925 * Math.cos(2*Mpr)
            +    48.888 * Math.cos(Mr)
            +   246.158 * Math.cos(2*Dr - 2*Mpr)
            -   152.138 * Math.cos(2*Dr - Mr - Mpr)
            -   170.733 * Math.cos(2*Dr + Mpr)
            -   204.586 * Math.cos(2*Dr - Mr)
            -   129.620 * Math.cos(Mpr - Mr);

        // Geocentric ecliptic → equatorial (ECI, equator of date)
        double lonRad = Math.toRadians(lon);
        double latRad = Math.toRadians(lat);
        double eps    = obliquityRad(T);

        double cosLat = Math.cos(latRad);
        double xEcl = dist * cosLat * Math.cos(lonRad);
        double yEcl = dist * cosLat * Math.sin(lonRad);
        double zEcl = dist * Math.sin(latRad);

        double cosE = Math.cos(eps), sinE = Math.sin(eps);
        return new double[] {
            xEcl,
            yEcl * cosE - zEcl * sinE,
            yEcl * sinE + zEcl * cosE
        };
    }

    /**
     * Moon az/el/range/range-rate for a ground observer.
     * <p>Range-rate is positive when the Moon is moving away from the
     * observer (typical sign convention for Doppler). Units: degrees,
     * degrees, km, km/s.
     */
    public static double[] moonAzElRange(double obsLatDeg, double obsLonDeg, double obsAltKm,
                                          Instant when) {
        double[] m0 = moonEciKm(when);
        // Numerical derivative for range-rate — sample 2 seconds apart.
        // The Moon moves ~1 km/s, this is plenty precise for EME Doppler.
        Instant later = when.plusSeconds(2);
        double[] m1 = moonEciKm(later);
        double[] velKmS = {
            (m1[0] - m0[0]) / 2.0,
            (m1[1] - m0[1]) / 2.0,
            (m1[2] - m0[2]) / 2.0
        };
        return CoordTransform.satAzElRange(m0, velKmS, obsLatDeg, obsLonDeg, obsAltKm, when);
    }

    // ---------------------------------------------------------------
    // EME Doppler
    // ---------------------------------------------------------------

    /**
     * Doppler frequency shift in Hz seen on the receive side of an EME
     * round-trip. The factor of 2 covers TX → Moon → RX — the radio
     * wave Doppler-shifts twice along the path.
     *
     * <p>positive rangeRateKmS = receding → negative shift.
     */
    public static double dopplerShiftHz(double rxFreqHz, double rangeRateKmS) {
        return -2.0 * (rangeRateKmS * 1000.0) / C_MS * rxFreqHz;
    }

    // ---------------------------------------------------------------
    // Optical libration (longitude / latitude)
    // ---------------------------------------------------------------

    /**
     * Optical libration in selenographic longitude and latitude (degrees).
     * <p>Returns {@code [longLib, latLib]}. Operators care about the
     * absolute libration rate (degrees per minute) — high libration
     * smears the Doppler return, favouring slower WSJT-X modes like Q65
     * over JT65. This implementation gives instantaneous libration good
     * to ~0.1°, which is enough to flag the "smeared" / "clean" verdict.
     */
    public static double[] libration(Instant when) {
        double T = julianCenturiesJ2000(when);

        // Moon's argument of latitude and mean anomaly (radians)
        double Fr  = Math.toRadians(norm(93.2720950 + 483202.0175233*T - 0.0036539*T*T));
        double Mpr = Math.toRadians(norm(134.9633964 + 477198.8675055*T));

        // Optical libration in longitude — dominated by the eccentricity
        // libration. First-order: 2e·sin(M'), with e ≈ 0.0549 the Moon's
        // orbital eccentricity. Peaks at ±6.29°.
        double E = 0.0549;
        double longLib = Math.toDegrees(2 * E * Math.sin(Mpr));

        // Optical libration in latitude — caused by the tilt of the
        // Moon's equator relative to its orbit. I_lunar = 6.687°.
        // sin(b) = sin(F) · sin(I_lunar). Peaks at ±6.687°.
        double Ilunar = Math.toRadians(6.687);
        double latLib = Math.toDegrees(Math.asin(Math.sin(Fr) * Math.sin(Ilunar)));

        return new double[] { longLib, latLib };
    }

    /**
     * Composite libration magnitude — distance from the (0,0) sub-Earth
     * point in degrees. ≤ ~8°. Used by the panel to colour-code "clean"
     * (≤2°) vs "smeared" (≥5°) Doppler-return conditions.
     */
    public static double librationMagnitudeDeg(Instant when) {
        double[] l = libration(when);
        return Math.sqrt(l[0]*l[0] + l[1]*l[1]);
    }

    // ---------------------------------------------------------------
    // Common-window prediction (two stations)
    // ---------------------------------------------------------------

    /**
     * Find the next common visibility window (both QTHs see the Moon
     * above the horizon) within the given lookahead. Returns null if no
     * window starts in that range.
     *
     * <p>Coarse-stepped at 10 minutes — enough to catch any window
     * lasting longer than that, and EME windows always last hours.
     */
    public static MoonWindow nextCommonWindow(double lat1, double lon1,
                                              double lat2, double lon2,
                                              Instant from, java.time.Duration lookahead,
                                              double minElevationDeg) {
        long step  = 10L * 60L;
        long end   = lookahead.toSeconds();
        Instant start = null;
        for (long t = 0; t <= end; t += step) {
            Instant when = from.plusSeconds(t);
            double el1 = moonAzElRange(lat1, lon1, 0, when)[1];
            double el2 = moonAzElRange(lat2, lon2, 0, when)[1];
            boolean both = el1 >= minElevationDeg && el2 >= minElevationDeg;
            if (both && start == null) start = when;
            if (!both && start != null) {
                return new MoonWindow(start, when);
            }
        }
        if (start != null) {
            return new MoonWindow(start, from.plusSeconds(end));
        }
        return null;
    }

    /** Container for a time window when both stations can see the Moon. */
    public static final class MoonWindow {
        public final Instant startUtc;
        public final Instant endUtc;
        public MoonWindow(Instant s, Instant e) { this.startUtc = s; this.endUtc = e; }
        public java.time.Duration duration() { return java.time.Duration.between(startUtc, endUtc); }
    }

    // ---------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------

    private static double julianCenturiesJ2000(Instant when) {
        double jd = when.getEpochSecond() / 86400.0 + 2440587.5 + when.getNano() / 86400e9;
        return (jd - 2451545.0) / 36525.0;
    }

    private static double obliquityRad(double T) {
        // Mean obliquity of the ecliptic — IAU 1980, sufficient for EME.
        double seconds = 84381.448 - 46.8150*T - 0.00059*T*T + 0.001813*T*T*T;
        return Math.toRadians(seconds / 3600.0);
    }

    private static double norm(double deg) {
        deg = deg % 360.0;
        if (deg < 0) deg += 360.0;
        return deg;
    }
}
