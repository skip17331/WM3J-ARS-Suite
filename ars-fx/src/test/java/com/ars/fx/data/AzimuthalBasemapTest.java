package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AzimuthalBasemapTest {
    static AzimuthalBasemap.Source src;
    static BufferedImage marble;

    @BeforeAll static void load() throws Exception {
        src = AzimuthalBasemap.loadSource();
        try (InputStream in = AzimuthalBasemapTest.class.getResourceAsStream("/com/ars/fx/map/bluemarble.jpg")) { marble = ImageIO.read(in); }
    }

    /** Land by the satellite image: not ocean-blue. */
    static boolean marbleLand(double lat, double lon) {
        int x = (int) Math.max(0, Math.min(marble.getWidth() - 1, (lon + 180) / 360 * (marble.getWidth() - 1)));
        int y = (int) Math.max(0, Math.min(marble.getHeight() - 1, (90 - lat) / 180 * (marble.getHeight() - 1)));
        int p = marble.getRGB(x, y), r = (p >> 16) & 0xFF, g = (p >> 8) & 0xFF, b = p & 0xFF;
        boolean ocean = b > r + 15 && b > g - 5 && b < 140;
        return !ocean || r > 150;
    }

    /** The share of the disk where the picture's land / sea agrees with the satellite image, for a map centred on (qLat, qLon). */
    static double agreement(double qLat, double qLon) {
        int size = 384;
        int[] px = AzimuthalBasemap.render(src, qLat, qLon, size);
        double c = (size - 1) / 2.0, r = size / 2.0 - 0.5, p0 = Math.toRadians(qLat), l0 = Math.toRadians(qLon);
        int same = 0, n = 0;
        for (int y = 2; y < size; y += 3) for (int x = 2; x < size; x += 3) {
            double dx = x - c, dy = y - c, dist = Math.hypot(dx, dy);
            if (dist > r - 3) continue;
            double rho = dist / r * Math.PI, th = Math.atan2(dx, -dy);
            double lat = Math.asin(Math.sin(p0) * Math.cos(rho) + Math.cos(p0) * Math.sin(rho) * Math.cos(th));
            double lon = l0 + Math.atan2(Math.sin(th) * Math.sin(rho) * Math.cos(p0), Math.cos(rho) - Math.sin(p0) * Math.sin(lat));
            double lond = ((Math.toDegrees(lon) + 540) % 360) - 180;
            boolean pictureLand = ((px[y * size + x] >> 8) & 0xFF) > 235;
            if (pictureLand == marbleLand(Math.toDegrees(lat), lond)) same++;
            n++;
        }
        return (double) same / n;
    }

    @Test void theBundledPictureLoadsAsASquare() {
        assertNotNull(src);
        assertEquals(768, src.size());
        assertEquals(768 * 768, src.argb().length);
    }

    @Test void aMapCentredOnThePicturesOwnCentreIsThePicture() {
        int size = src.size();
        int[] out = AzimuthalBasemap.render(src, AzimuthalBasemap.SRC_LAT, AzimuthalBasemap.SRC_LON, size);
        long diff = 0; int n = 0;
        for (int y = 20; y < size - 20; y += 5) for (int x = 20; x < size - 20; x += 5) {
            double dx = x - (size - 1) / 2.0, dy = y - (size - 1) / 2.0;
            if (Math.hypot(dx, dy) > size / 2.0 - 30) continue;
            diff += Math.abs(((out[y * size + x] >> 8) & 0xFF) - ((src.argb()[y * size + x] >> 8) & 0xFF)); n++;
        }
        assertTrue((double) diff / n < 4.0, "mean grey difference " + (double) diff / n);
    }

    @Test void landAndSeaFallWhereTheGlobeHasThemForStationsAroundTheWorld() {
        String[] names = {"New England", "Los Angeles", "Tokyo", "London", "Sydney", "Cape Town", "Buenos Aires"};
        double[][] at = {{41.7, -72.7}, {34.0, -118.2}, {35.7, 139.7}, {51.5, -0.1}, {-33.9, 151.2}, {-33.9, 18.4}, {-34.6, -58.4}};
        for (int i = 0; i < at.length; i++) {
            double a = agreement(at[i][0], at[i][1]);
            assertTrue(a > 0.88, names[i] + ": land and sea agree with the satellite image only " + Math.round(a * 100) + "%");
        }
    }

    @Test void theOldFixedPictureWouldHaveBeenWrongAwayFromItsCentre() {
        // what the screen used to draw for every station is this picture as it is; seen from Los Angeles it does not agree with the globe
        int size = src.size();
        double c = (size - 1) / 2.0, r = size / 2.0 - 0.5, p0 = Math.toRadians(34.0), l0 = Math.toRadians(-118.2);
        int same = 0, n = 0;
        for (int y = 2; y < size; y += 6) for (int x = 2; x < size; x += 6) {
            double dx = x - c, dy = y - c, dist = Math.hypot(dx, dy);
            if (dist > r - 3) continue;
            double rho = dist / r * Math.PI, th = Math.atan2(dx, -dy);
            double lat = Math.asin(Math.sin(p0) * Math.cos(rho) + Math.cos(p0) * Math.sin(rho) * Math.cos(th));
            double lon = l0 + Math.atan2(Math.sin(th) * Math.sin(rho) * Math.cos(p0), Math.cos(rho) - Math.sin(p0) * Math.sin(lat));
            boolean pictureLand = ((src.argb()[y * size + x] >> 8) & 0xFF) > 235;
            if (pictureLand == marbleLand(Math.toDegrees(lat), ((Math.toDegrees(lon) + 540) % 360) - 180)) same++;
            n++;
        }
        assertTrue((double) same / n < 0.80, "the unrotated picture agrees " + Math.round(100.0 * same / n) + "% from Los Angeles");
    }

    @Test void theOutsideOfTheDiskIsTransparentAndTheInsideOpaque() {
        int size = 100;
        int[] out = AzimuthalBasemap.render(src, 10, 20, size);
        assertEquals(0, out[0], "a corner is outside the disk");
        assertEquals(0xFF, (out[50 * size + 50] >>> 24), "the middle is drawn");
    }
}
