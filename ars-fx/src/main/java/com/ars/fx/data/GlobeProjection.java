package com.ars.fx.data;

/**
 * The maths behind the 3D globe, kept free of JavaFX so it can be tested. The globe is a sphere of radius {@code radiusPx} centred in a
 * {@code w x h} view, turned so that {@code (lat0, lon0)} faces the viewer, seen by a perspective camera (JavaFX's default 30 degree vertical
 * field of view, eye on the axis at the distance that makes the plane through the centre 1:1 with pixels). Positions are the same ones the
 * JavaFX {@code Sphere} uses, so an overlay drawn from {@link #project} lines up with the textured sphere underneath.
 */
public final class GlobeProjection {
    public static final double EARTH_KM = 6371.0, FOV_DEG = 30;

    /** A point on the screen; {@code visible} is false when the globe is in the way. */
    public record Pt(double x, double y, boolean visible) {}

    private final double r, w, h, e;
    private final double cLon, sLon, cLat, sLat;

    public GlobeProjection(double lat0, double lon0, double radiusPx, double w, double h) {
        this.r = radiusPx; this.w = w; this.h = h;
        this.e = eyeDistance(h);
        cLon = Math.cos(Math.toRadians(lon0)); sLon = Math.sin(Math.toRadians(lon0));
        cLat = Math.cos(Math.toRadians(lat0)); sLat = Math.sin(Math.toRadians(lat0));
    }

    /** Distance from the eye to the centre of the globe, in pixels, for a view {@code h} high. */
    public static double eyeDistance(double h) { return (h / 2) / Math.tan(Math.toRadians(FOV_DEG / 2)); }

    /** The largest globe radius that keeps the eye outside it: past this the camera is inside the Earth. */
    public static double maxRadius(double h) { return 0.85 * eyeDistance(h); }

    /** Distance from the eye to the centre of the globe, in pixels. */
    public double eyeDistance() { return e; }

    /**
     * A height above the surface, in km, at which something sits {@code factor} Earth radii from the centre, brought in so it never
     * passes the camera: the real sun and a stationary orbit are too far to draw to scale.
     */
    public double orbitAltKm(double factor) {
        double f = Math.max(1, Math.min(factor, 0.85 * e / r));
        return (f - 1) * EARTH_KM;
    }

    /** Where the globe's edge appears, as a radius in pixels (a little under {@code radiusPx}: the eye sees less than half the sphere). */
    public double silhouetteRadius() { return e * r / Math.sqrt(Math.max(1e-9, e * e - r * r)); }

    /** Screen position of a point at the given height above the surface. */
    public Pt project(double lat, double lon, double altKm) {
        double rad = (1 + altKm / EARTH_KM) * r;
        double la = Math.toRadians(lat), lo = Math.toRadians(lon), c = Math.cos(la);
        double x = c * Math.sin(lo), y = -Math.sin(la), z = -c * Math.cos(lo);         // JavaFX Sphere: longitude 0 faces the camera
        double x1 = x * cLon + z * sLon, z1 = -x * sLon + z * cLon;                    // turn about the vertical axis by lon0
        double y2 = y * cLat - z1 * sLat, z2 = y * sLat + z1 * cLat;                   // then tilt about the horizontal axis by lat0
        double X = x1 * rad, Y = y2 * rad, Z = z2 * rad;
        double k = e / (e + Z);
        return new Pt(w / 2 + X * k, h / 2 + Y * k, !hidden(X, Y, Z));
    }

    /** True when the straight line from the eye to the point passes through the globe first. */
    private boolean hidden(double X, double Y, double Z) {
        double dx = X, dy = Y, dz = Z + e;                                              // eye is at (0, 0, -e)
        double len2 = dx * dx + dy * dy + dz * dz;
        double t = Math.max(0, Math.min(1, e * dz / len2));                             // closest approach of the segment to the centre
        double px = t * dx, py = t * dy, pz = -e + t * dz;
        return px * px + py * py + pz * pz < r * r * 0.999 && t < 0.9999;
    }

    /** The point {@code angDeg} of arc from (lat, lon) along the given compass bearing, as {lat, lon}. */
    public static double[] destination(double lat, double lon, double bearingDeg, double angDeg) {
        double p1 = Math.toRadians(lat), l1 = Math.toRadians(lon), b = Math.toRadians(bearingDeg), d = Math.toRadians(angDeg);
        double p2 = Math.asin(Math.sin(p1) * Math.cos(d) + Math.cos(p1) * Math.sin(d) * Math.cos(b));
        double l2 = l1 + Math.atan2(Math.sin(b) * Math.sin(d) * Math.cos(p1), Math.cos(d) - Math.sin(p1) * Math.sin(p2));
        return new double[]{Math.toDegrees(p2), ((Math.toDegrees(l2) + 540) % 360) - 180};
    }

    /** A circle of {@code angDeg} radius about (lat, lon) as {lat, lon} pairs, {@code n + 1} of them with the last equal to the first. */
    public static double[][] circle(double lat, double lon, double angDeg, int n) {
        double[][] out = new double[n + 1][];
        for (int i = 0; i < n; i++) out[i] = destination(lat, lon, 360.0 * i / n, angDeg);
        out[n] = out[0];
        return out;
    }
}
