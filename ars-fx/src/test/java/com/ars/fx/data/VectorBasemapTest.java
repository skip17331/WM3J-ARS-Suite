package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VectorBasemapTest {
    @Test void landLoadsAndSpansTheWorld() {
        var rs = VectorBasemap.land().rings();
        assertTrue(rs.length > 1000, "rings: " + rs.length);
        double minLat = 90, maxLat = -90, minLon = 180, maxLon = -180;
        for (var r : rs) { minLat = Math.min(minLat, r.minLat()); maxLat = Math.max(maxLat, r.maxLat()); minLon = Math.min(minLon, r.minLon()); maxLon = Math.max(maxLon, r.maxLon()); }
        assertTrue(minLat < -85 && maxLat > 80 && minLon < -170 && maxLon > 170);
    }
    @Test void bordersLoad() {
        assertTrue(VectorBasemap.borders().rings().length > 100);
    }
    @Test void statesAndLabelsLoad() {
        assertTrue(VectorBasemap.states().rings().length > 300);
        var ls = MapLabels.countries();
        assertTrue(ls.size() > 200);
        var us = ls.stream().filter(l -> l.name().startsWith("United States")).findFirst().orElseThrow();
        assertTrue(us.lon() < -90 && us.lon() > -110 && us.lat() > 35 && us.lat() < 45, us.toString());
        assertEquals(1.0, MapLabels.zoomOf(360), 1e-9);
        assertEquals(3.0, MapLabels.zoomOf(90), 1e-9);
    }
    /** Point-in-ring even-odd: London is land, the mid-Atlantic is not. */
    @Test void londonIsLandAndMidAtlanticIsSea() {
        assertTrue(inside(51.5, -0.1));
        assertFalse(inside(30, -40));
        assertTrue(inside(39, -77));
        assertFalse(inside(-30, -120));
    }
    private static boolean inside(double lat, double lon) {
        boolean in = false;
        for (var r : VectorBasemap.land().rings()) {
            if (lon < r.minLon() || lon > r.maxLon() || lat < r.minLat() || lat > r.maxLat()) continue;
            double[] p = r.ll(); int n = r.points(); boolean ri = false;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = p[i*2], yi = p[i*2+1], xj = p[j*2], yj = p[j*2+1];
                if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) ri = !ri;
            }
            if (ri) in = !in;
        }
        return in;
    }
}
