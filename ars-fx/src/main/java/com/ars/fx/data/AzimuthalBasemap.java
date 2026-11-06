package com.ars.fx.data;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * The azimuthal J-Map's world map, centred on the station. The bundled picture ({@code gcm-gray.jpg}) is one azimuthal-equidistant map of the
 * whole globe (its edge is the antipode), but it was drawn around a fixed point, {@link #SRC_LAT} / {@link #SRC_LON} on the US east coast. The map
 * on screen is centred on the operator's own station, so for anyone else the continents sat in the wrong place against the spots, the beam and the
 * rings. This turns the picture around: each pixel of a map centred on the station is looked up in the picture, so the coastlines and borders fall
 * where the projection puts them for any station. The picture's centre was found by matching its land and sea to the satellite image (95% agreement,
 * the rest being borders, ice and lakes).
 */
public final class AzimuthalBasemap {
    private AzimuthalBasemap() {}

    /** Where the bundled picture is centred (found by matching it to the satellite image of the same globe). */
    public static final double SRC_LAT = 40.0, SRC_LON = -77.0;

    /** A square azimuthal picture whose disk fills it: the middle is the centre, the edge is 180 degrees away. */
    public record Source(int size, int[] argb) {}

    /** The bundled picture, or null if it cannot be read. */
    public static Source loadSource() {
        try (InputStream in = AzimuthalBasemap.class.getResourceAsStream("/com/ars/fx/map/gcm-gray.jpg")) {
            if (in == null) return null;
            BufferedImage b = ImageIO.read(in);
            if (b == null || b.getWidth() != b.getHeight()) return null;
            int n = b.getWidth();
            int[] px = new int[n * n];
            b.getRGB(0, 0, n, n, px, 0, n);
            return new Source(n, px);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * An {@code size} x {@code size} ARGB picture of the world centred on {@code qLat, qLon}, north up, the same projection and extent as the source
     * (the edge of the disk is 180 degrees away). Pixels outside the disk are transparent.
     */
    public static int[] render(Source src, double qLat, double qLon, int size) {
        int[] out = new int[size * size];
        double c = (size - 1) / 2.0, r = size / 2.0 - 0.5;
        double sc = (src.size() - 1) / 2.0, sr = src.size() / 2.0 - 0.5;
        double p0 = Math.toRadians(qLat), l0 = Math.toRadians(qLon), ps = Math.toRadians(SRC_LAT), ls = Math.toRadians(SRC_LON);
        double sinP0 = Math.sin(p0), cosP0 = Math.cos(p0), sinPs = Math.sin(ps), cosPs = Math.cos(ps);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = x - c, dy = y - c, dist = Math.hypot(dx, dy);
                if (dist > r) continue;
                double rho = dist / r * Math.PI, theta = Math.atan2(dx, -dy);                       // out from the station: distance and bearing
                double sinRho = Math.sin(rho), cosRho = Math.cos(rho);
                double lat = Math.asin(clamp(sinP0 * cosRho + cosP0 * sinRho * Math.cos(theta)));
                double lon = l0 + Math.atan2(Math.sin(theta) * sinRho * cosP0, cosRho - sinP0 * Math.sin(lat));
                double sinLat = Math.sin(lat), cosLat = Math.cos(lat), dl = lon - ls;               // that point, seen from the picture's centre
                double cosD = clamp(sinPs * sinLat + cosPs * cosLat * Math.cos(dl));
                double d = Math.min(Math.acos(cosD), Math.PI - 0.003);                              // never the very rim, which is background
                double b = Math.atan2(Math.sin(dl) * cosLat, cosPs * sinLat - sinPs * cosLat * Math.cos(dl));
                double sx = sc + sr * (d / Math.PI) * Math.sin(b), sy = sc - sr * (d / Math.PI) * Math.cos(b);
                out[y * size + x] = bilinear(src, sx, sy) | 0xFF000000;
            }
        }
        return out;
    }

    private static double clamp(double v) { return Math.max(-1, Math.min(1, v)); }

    private static int bilinear(Source s, double x, double y) {
        int n = s.size();
        x = Math.max(0, Math.min(n - 1.001, x)); y = Math.max(0, Math.min(n - 1.001, y));
        int x0 = (int) x, y0 = (int) y; double fx = x - x0, fy = y - y0;
        int a = s.argb()[y0 * n + x0], b = s.argb()[y0 * n + x0 + 1], c = s.argb()[(y0 + 1) * n + x0], d = s.argb()[(y0 + 1) * n + x0 + 1];
        int rgb = 0;
        for (int sh = 0; sh <= 16; sh += 8) {
            double top = ((a >> sh) & 0xFF) * (1 - fx) + ((b >> sh) & 0xFF) * fx, bot = ((c >> sh) & 0xFF) * (1 - fx) + ((d >> sh) & 0xFF) * fx;
            rgb |= ((int) Math.round(top * (1 - fy) + bot * fy) & 0xFF) << sh;
        }
        return rgb;
    }
}
