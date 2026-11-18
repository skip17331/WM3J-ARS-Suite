package com.ars.fx.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapes for J-Map's azimuthal (great-circle) map, without the antipode wedge (punch list 5-7).
 *
 * <p>The map is centred on the station; the far side of the world, around the antipode, is cut off at the disk's edge. Points beyond
 * {@code maxDeg} are held at that distance on their own bearing. Near the antipode the bearing swings round quickly, so two such points
 * joined by a straight line cut a chord, a "wedge", right across the disk; and a coastline that goes round the antipode (Asia, for a
 * station in Argentina) was filled on the wrong side. Here two held points are joined by an arc along the edge, outside the visible
 * disk, and when the arcs went round the wrong way (which flips every point inside the edge) the whole edge circle is added once more:
 * the even-odd fill then puts land where land is, which is checked at the one point known for sure, the station itself.
 *
 * <p>Output is in degrees of arc from the station: x east, y down the screen (north up). No JavaFX, so it can be tested.
 */
public final class AzimuthalPath {
    private AzimuthalPath() {}

    /** Degrees between steps when a long edge is split so it bends like a great circle, and when an arc follows the edge. */
    static final double STEP = 1.0, ARC_STEP = 3.0;

    /** One polygon's rings (outline and holes), projected; {@code stationInside} says whether the station is on this polygon. */
    public static List<double[]> polygon(List<double[]> rings, double qLat, double qLon, double maxDeg, boolean stationInside) {
        List<double[]> out = new ArrayList<>();
        int winding = 0;
        for (double[] ll : rings) {
            double[] xy = project(ll, qLat, qLon, maxDeg);
            out.add(xy);
            winding += windingAtCentre(xy);
        }
        if ((Math.abs(winding) % 2 == 1) != stationInside) out.add(circle(maxDeg));
        return out;
    }

    /** An open line (a border) projected; held points are joined along the edge, so no chord crosses the disk. */
    public static double[] line(double[] ll, double qLat, double qLon, double maxDeg) { return project(ll, qLat, qLon, maxDeg); }

    /** One point: {x, y, distance in degrees}. */
    public static double[] point(double lat, double lon, double qLat, double qLon) {
        double qLatR = Math.toRadians(qLat), latR = Math.toRadians(lat), dLon = Math.toRadians(lon - qLon);
        double cosD = Math.sin(qLatR) * Math.sin(latR) + Math.cos(qLatR) * Math.cos(latR) * Math.cos(dLon);
        double d = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cosD))));
        double brg = Math.atan2(Math.sin(dLon) * Math.cos(latR), Math.cos(qLatR) * Math.sin(latR) - Math.sin(qLatR) * Math.cos(latR) * Math.cos(dLon));
        return new double[]{ Math.sin(brg) * d, -Math.cos(brg) * d, d };
    }

    static double[] project(double[] ll, double qLat, double qLon, double maxDeg) {
        int n = ll.length / 2;
        double[] out = new double[Math.max(16, n * 4)];
        int len = 0;
        double plat = 0, plon = 0, pAng = 0;
        boolean pHeld = false;
        for (int i = 0; i < n; i++) {
            double lat = ll[i * 2 + 1], lon = ll[i * 2];
            int sub = 1;
            if (i > 0) sub = (int) Math.min(40, Math.max(1, Math.ceil(Math.max(Math.abs(lat - plat), Math.abs(lon - plon)) / STEP)));
            for (int k = 1; k <= sub; k++) {
                double f = (double) k / sub, la = i == 0 ? lat : plat + (lat - plat) * f, lo = i == 0 ? lon : plon + (lon - plon) * f;
                double[] p = point(la, lo, qLat, qLon);
                double ang = Math.atan2(p[1], p[0]);
                boolean held = p[2] > maxDeg;
                if (held) { p[0] = Math.cos(ang) * maxDeg; p[1] = Math.sin(ang) * maxDeg; }
                if (held && pHeld && (i > 0 || k > 1)) {                 // follow the edge from the last held point to this one
                    double d = wrap(ang - pAng);
                    int steps = (int) Math.ceil(Math.abs(Math.toDegrees(d)) / ARC_STEP);
                    for (int s = 1; s < steps; s++) {
                        double a = pAng + d * s / steps;
                        out = put(out, len, Math.cos(a) * maxDeg, Math.sin(a) * maxDeg); len += 2;
                    }
                }
                out = put(out, len, p[0], p[1]); len += 2;
                pAng = ang; pHeld = held;
            }
            plat = lat; plon = lon;
        }
        return java.util.Arrays.copyOf(out, len);
    }

    private static double[] put(double[] a, int len, double x, double y) {
        if (len + 2 > a.length) a = java.util.Arrays.copyOf(a, a.length * 2);
        a[len] = x; a[len + 1] = y;
        return a;
    }

    private static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a < -Math.PI) a += 2 * Math.PI;
        return a;
    }

    /** How many times the closed path goes round the station (the origin). */
    static int windingAtCentre(double[] xy) {
        int n = xy.length / 2;
        if (n < 3) return 0;
        double sum = 0, prev = Math.atan2(xy[2 * n - 1], xy[2 * n - 2]);
        for (int i = 0; i < n; i++) {
            double a = Math.atan2(xy[i * 2 + 1], xy[i * 2]);
            sum += wrap(a - prev);
            prev = a;
        }
        return (int) Math.round(sum / (2 * Math.PI));
    }

    /** The edge circle. */
    static double[] circle(double r) {
        int n = 120;
        double[] c = new double[n * 2];
        for (int i = 0; i < n; i++) { double a = 2 * Math.PI * i / n; c[i * 2] = Math.cos(a) * r; c[i * 2 + 1] = Math.sin(a) * r; }
        return c;
    }

    /** Even-odd test of a point against projected subpaths (for tests and hit-testing). */
    public static boolean filled(List<double[]> paths, double x, double y) {
        boolean in = false;
        for (double[] xy : paths) {
            int n = xy.length / 2;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = xy[i * 2], yi = xy[i * 2 + 1], xj = xy[j * 2], yj = xy[j * 2 + 1];
                if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) in = !in;
            }
        }
        return in;
    }
}
