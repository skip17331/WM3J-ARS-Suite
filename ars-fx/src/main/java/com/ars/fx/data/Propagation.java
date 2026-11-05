package com.ars.fx.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple HF propagation estimate for a path, from the space-weather figures the app already pulls (solar flux, sunspot number, K index),
 * the great-circle path and the time of day and year. Stage one of the plan: a MUF / LUF / band-open estimate, not VOACAP and not ITU-R P.533.
 *
 * <p>The model, in the order it is applied:
 * <ol>
 *   <li>The path is cut into hops of at most about 4,000 km (the longest single F2 hop). The sun's zenith angle is worked out where it
 *       matters: at the middle of the first and last hop for the F2 layer, and at the ends of every hop for D-layer absorption.</li>
 *   <li>foF2, the F2 layer's critical frequency, comes from the solar flux (about 6.5 MHz at noon for a flux of 70, rising by 5 MHz to a flux
 *       of 200), falls with the sun's zenith angle to 45% of the noon value at night, is trimmed at high latitude, and is cut when the
 *       K index is 4 or more (a disturbed ionosphere).</li>
 *   <li>The MUF of a hop is foF2 times the secant of the angle at which the ray meets the F2 layer (a thin layer 320 km up, a spherical
 *       Earth, a ray leaving at the lowest angle the hop length allows). A 3,000 km hop comes out near 3.0 times foF2, the M(3000)F2 of
 *       a typical day. The path MUF is the lower of the two ends: the F2 reflection points nearest the terminals set it, as in common practice
 *       (and ITU-R P.533, which uses control points near each end of a long path); the hops in between are assumed to find a usable layer.</li>
 *   <li>The D layer absorbs where the ray crosses it near the ends of each hop: for each hop the loss in dB is
 *       {@code C (1 + 0.0037 SSN) mean(cos(zenith)^1.3 at the two ends) sec(angle at 90 km) / (f + 0.8)^2},
 *       with C chosen so a 3,000 km noon hop at a sunspot number of 100 loses about 8 dB on 20 m and 29 dB on 40 m. The LUF is the frequency
 *       at which the total loss reaches {@link #ABSORPTION_LIMIT_DB}.</li>
 *   <li>Day-to-day variation is a spread of 13% on the MUF and 20% on the LUF (judgement, not measurement: tuned so daytime 80 m is not left open on a long path); the chance a frequency is open is the chance it is below
 *       the day's MUF and above the day's LUF.</li>
 * </ol>
 * What it leaves out: sporadic E and E-layer paths, the winter anomaly and the equatorial anomaly, grey-line enhancement, polar and
 * auroral paths, atmospheric noise and the operator's power and antennas. It is an estimate, and the app says so.
 */
public final class Propagation {
    private Propagation() {}

    public static final double EARTH_KM = 6371.0, F2_HEIGHT_KM = 320.0, D_HEIGHT_KM = 90.0, MAX_HOP_KM = 4000.0;
    /** Total D-layer loss, in dB, beyond which a frequency is below the LUF (a modest station: it cannot make up more). */
    public static final double ABSORPTION_LIMIT_DB = 35.0;
    static final double ABSORPTION_C = 334.0, GYRO_MHZ = 0.8, LUF_FLOOR_MHZ = 1.5;

    /** A place, in degrees. */
    public record Point(double lat, double lon) {}
    /** Solar flux (10.7 cm), sunspot number and K index. */
    public record Weather(double sfi, double ssn, double k) {
        /** From the HamQSL strings; either of flux and sunspots may stand in for the other. Null when neither is known. K defaults to 1 (quiet). */
        public static Weather of(String sfi, String ssn, String k) {
            double f = num(sfi), s = num(ssn), kk = num(k);
            if (Double.isNaN(f) && Double.isNaN(s)) return null;
            if (Double.isNaN(f)) f = 63.7 + 0.728 * s + 0.00089 * s * s;            // flux from sunspots (a standard fit)
            if (Double.isNaN(s)) {                                                       // sunspots from flux: the same fit, solved the other way
                double disc = 0.728 * 0.728 - 4 * 0.00089 * (63.7 - f);
                s = disc < 0 || f <= 63.7 ? 0 : (-0.728 + Math.sqrt(disc)) / (2 * 0.00089);
            }
            return new Weather(f, s, Double.isNaN(kk) ? 1 : kk);
        }
        private static double num(String v) { try { return v == null ? Double.NaN : Double.parseDouble(v.trim().replaceAll("[^0-9.\\-]", "")); } catch (Exception e) { return Double.NaN; } }
    }
    /** One hour of the estimate. */
    public record Hour(int utc, double mufMhz, double lufMhz, int hops) {}

    // ---- geometry ----------------------------------------------------------------------------------------------------

    /** Great-circle distance, in km. */
    public static double distanceKm(Point a, Point b) {
        double p1 = Math.toRadians(a.lat()), p2 = Math.toRadians(b.lat()), dl = Math.toRadians(b.lon() - a.lon());
        double c = Math.sin(p1) * Math.sin(p2) + Math.cos(p1) * Math.cos(p2) * Math.cos(dl);
        return EARTH_KM * Math.acos(Math.max(-1, Math.min(1, c)));
    }

    /** The point a fraction of the way along the great circle from {@code a} to {@code b}. */
    public static Point along(Point a, Point b, double f) {
        double p1 = Math.toRadians(a.lat()), l1 = Math.toRadians(a.lon()), p2 = Math.toRadians(b.lat()), l2 = Math.toRadians(b.lon());
        double d = distanceKm(a, b) / EARTH_KM;
        if (d < 1e-9) return a;
        double A = Math.sin((1 - f) * d) / Math.sin(d), B = Math.sin(f * d) / Math.sin(d);
        double x = A * Math.cos(p1) * Math.cos(l1) + B * Math.cos(p2) * Math.cos(l2);
        double y = A * Math.cos(p1) * Math.sin(l1) + B * Math.cos(p2) * Math.sin(l2);
        double z = A * Math.sin(p1) + B * Math.sin(p2);
        return new Point(Math.toDegrees(Math.atan2(z, Math.hypot(x, y))), Math.toDegrees(Math.atan2(y, x)));
    }

    /** Cosine of the sun's zenith angle at {@code p} on day {@code doy} of the year at {@code utcHours}. Negative at night. */
    public static double cosZenith(Point p, int doy, double utcHours) {
        double decl = Math.toRadians(23.44) * Math.sin(2 * Math.PI * (284 + doy) / 365.0);
        double hourAngle = Math.toRadians(15.0 * (utcHours + p.lon() / 15.0 - 12.0));
        double lat = Math.toRadians(p.lat());
        return Math.sin(lat) * Math.sin(decl) + Math.cos(lat) * Math.cos(decl) * Math.cos(hourAngle);
    }

    /** How many hops a path of {@code km} needs. */
    public static int hops(double km) { return Math.max(1, (int) Math.ceil(km / MAX_HOP_KM - 1e-9)); }

    /**
     * For a hop of {@code hopKm} on the ground, the angle (radians, from the vertical) at which the ray meets a thin layer at {@code layerKm},
     * for the lowest ray the hop allows. Spherical Earth.
     */
    static double incidence(double hopKm, double layerKm) {
        double r = EARTH_KM, alpha = hopKm / (2 * r);                           // half-hop, as an angle at the Earth's centre
        double lo = 0, hi = Math.PI / 2;                                        // the ray's elevation: g(elevation) = 90 - elevation - incidence falls from its maximum to 0
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2, phi = Math.asin(r * Math.cos(mid) / (r + layerKm)), g = Math.PI / 2 - mid - phi;
            if (g > alpha) lo = mid; else hi = mid;
        }
        double elev = (lo + hi) / 2;
        return Math.asin(r * Math.cos(elev) / (r + layerKm));
    }
    /** The elevation of the ray for a hop of {@code hopKm}, radians. */
    static double elevation(double hopKm) {
        double r = EARTH_KM, alpha = Math.min(hopKm / (2 * r), maxHalfHop());
        double lo = 0, hi = Math.PI / 2;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2, g = Math.PI / 2 - mid - Math.asin(r * Math.cos(mid) / (r + F2_HEIGHT_KM));
            if (g > alpha) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }
    private static double maxHalfHop() { return Math.PI / 2 - Math.asin(EARTH_KM / (EARTH_KM + F2_HEIGHT_KM)); }

    /** The secant of the incidence angle on the F2 layer for a hop of {@code hopKm}: the MUF is foF2 times this. 1 for a hop of nothing. */
    public static double secantF2(double hopKm) {
        double h = Math.min(hopKm, 2 * EARTH_KM * maxHalfHop());
        return 1.0 / Math.cos(incidence(Math.max(h, 1.0), F2_HEIGHT_KM));
    }
    /** The secant of the angle at which that same ray crosses the D layer (90 km). */
    static double secantD(double hopKm) {
        double elev = elevation(hopKm);
        return 1.0 / Math.cos(Math.asin(EARTH_KM * Math.cos(elev) / (EARTH_KM + D_HEIGHT_KM)));
    }

    // ---- the ionosphere ----------------------------------------------------------------------------------------------

    /** The F2 layer's critical frequency (MHz) at a place where the cosine of the sun's zenith angle is {@code cosChi}. */
    public static double foF2(double cosChi, double lat, Weather w) {
        double noon = 6.5 + 0.0385 * (w.sfi() - 70.0);
        double x = Math.max(cosChi, 0);
        double f = noon * (0.45 + 0.55 * Math.pow(x, 0.25));
        f *= Math.max(0.7, 1.0 - 0.004 * Math.max(0, Math.abs(lat) - 35.0));
        if (w.k() >= 5) f *= 0.75; else if (w.k() >= 4) f *= 0.9;
        return f;
    }

    /** The MUF and LUF of the path at {@code utcHours} on day {@code doy}. */
    public static Hour hour(Point tx, Point rx, Weather w, int doy, double utcHours) {
        double km = distanceKm(tx, rx);
        int n = hops(km);
        double hop = km / n;
        double muf = Double.MAX_VALUE, loss = 0;
        for (int i = 0; i < n; i++) {
            if (i == 0 || i == n - 1) {                                                // F2: the reflection points nearest each terminal limit the path
                Point mid = along(tx, rx, (i + 0.5) / n);
                muf = Math.min(muf, foF2(cosZenith(mid, doy, utcHours), mid.lat(), w) * secantF2(hop));
            }
            double xa = Math.max(cosZenith(along(tx, rx, (double) i / n), doy, utcHours), 0);          // D: where the ray crosses it, near the hop's two ends
            double xb = Math.max(cosZenith(along(tx, rx, (double) (i + 1) / n), doy, utcHours), 0);
            loss += 0.5 * (Math.pow(xa, 1.3) + Math.pow(xb, 1.3)) * secantD(hop);
        }
        double s = ABSORPTION_C * (1 + 0.0037 * w.ssn()) * loss;                       // dB x (f + 0.8)^2
        double luf = Math.max(LUF_FLOOR_MHZ, Math.sqrt(s / ABSORPTION_LIMIT_DB) - GYRO_MHZ);
        return new Hour((int) Math.floor(utcHours) % 24, muf, luf, n);
    }

    /** The 24 hours (UTC) of {@code date}. */
    public static List<Hour> day(Point tx, Point rx, Weather w, LocalDate date) {
        List<Hour> out = new ArrayList<>(24);
        for (int h = 0; h < 24; h++) out.add(hour(tx, rx, w, date.getDayOfYear(), h + 0.5));       // the middle of each hour
        return out;
    }

    /** The chance (0 to 1) that {@code fMhz} is usable in this hour: below that day's MUF and above that day's LUF. */
    public static double reliability(double fMhz, Hour h) {
        double pMuf = phi((h.mufMhz() - fMhz) / (0.13 * h.mufMhz()));
        double pLuf = phi((fMhz - h.lufMhz()) / (0.20 * h.lufMhz()));
        return pMuf * pLuf;
    }

    /** The standard normal distribution function (Abramowitz and Stegun 7.1.26, error under 1.5e-7). */
    static double phi(double z) {
        double t = 1.0 / (1.0 + 0.3275911 * Math.abs(z) / Math.sqrt(2));
        double y = 1 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t - 0.284496736) * t + 0.254829592) * t * Math.exp(-z * z / 2);
        return z >= 0 ? 0.5 * (1 + y) : 0.5 * (1 - y);
    }
}
