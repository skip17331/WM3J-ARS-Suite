package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GlobeProjectionTest {
    private static final double W = 800, H = 600;

    @Test void theCentreOfTheViewIsTheMiddleOfTheScreen() {
        GlobeProjection p = new GlobeProjection(40, -77, 250, W, H);
        GlobeProjection.Pt c = p.project(40, -77, 0);
        assertEquals(W / 2, c.x(), 1e-6); assertEquals(H / 2, c.y(), 1e-6); assertTrue(c.visible());
    }

    @Test void northIsUpAndEastIsRight() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, W, H);
        assertTrue(p.project(20, 0, 0).y() < H / 2, "north of centre is above it");
        assertTrue(p.project(-20, 0, 0).y() > H / 2);
        assertTrue(p.project(0, 20, 0).x() > W / 2, "east of centre is right of it");
        assertTrue(p.project(0, -20, 0).x() < W / 2);
    }

    @Test void turningTheGlobeBringsThatPlaceToTheMiddle() {
        for (double[] at : new double[][]{{51.5, -0.1}, {-33.9, 18.4}, {35.7, 139.7}, {-10, -170}, {70, 100}}) {
            GlobeProjection.Pt c = new GlobeProjection(at[0], at[1], 250, W, H).project(at[0], at[1], 0);
            assertEquals(W / 2, c.x(), 1e-6, "x at " + at[0] + "," + at[1]); assertEquals(H / 2, c.y(), 1e-6);
        }
    }

    @Test void theFarSideIsHiddenAndTheNearSideIsNot() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, W, H);
        assertFalse(p.project(0, 180, 0).visible());
        assertFalse(p.project(0, 120, 0).visible());
        assertTrue(p.project(0, 60, 0).visible());
        assertTrue(p.project(0, 0, 0).visible());
    }

    @Test void aSatelliteJustBehindTheLimbIsHiddenButOneHighAboveItIsSeen() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, W, H);
        assertFalse(p.project(0, 100, 400).visible(), "low and over the back");
        assertTrue(p.project(0, 80, 400).visible());
        assertTrue(p.project(0, 100, 35786).visible(), "geostationary height clears the limb");
    }

    @Test void aHigherPointSitsFartherFromTheCentre() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, W, H);
        double low = Math.hypot(p.project(0, 30, 0).x() - W / 2, p.project(0, 30, 0).y() - H / 2);
        double high = Math.hypot(p.project(0, 30, 2000).x() - W / 2, p.project(0, 30, 2000).y() - H / 2);
        assertTrue(high > low);
    }

    @Test void theEdgeOfTheGlobeProjectsToTheSilhouette() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, W, H);
        // the limb is where the line from the eye just grazes the sphere: 90 degrees minus the eye's half angle from the middle
        double half = Math.toDegrees(Math.acos(250 / p.eyeDistance()));
        GlobeProjection.Pt edge = p.project(0, half - 0.01, 0);
        assertTrue(edge.visible());
        assertEquals(p.silhouetteRadius(), edge.x() - W / 2, 0.5);
        assertFalse(p.project(0, half + 4, 0).visible());
    }

    @Test void aCircleKeepsItsRadiusAndCloses() {
        double[][] c = GlobeProjection.circle(10, 20, 15, 72);
        assertEquals(73, c.length);
        assertArrayEquals(c[0], c[72], 1e-9);
        for (double[] pt : c) {
            double d = Math.toDegrees(Math.acos(Math.sin(Math.toRadians(10)) * Math.sin(Math.toRadians(pt[0]))
                    + Math.cos(Math.toRadians(10)) * Math.cos(Math.toRadians(pt[0])) * Math.cos(Math.toRadians(pt[1] - 20))));
            assertEquals(15, d, 1e-6);
        }
    }

    @Test void theLargestRadiusKeepsTheEyeOutsideTheGlobe() {
        double h = 600;
        assertTrue(GlobeProjection.maxRadius(h) < GlobeProjection.eyeDistance(h));
        GlobeProjection p = new GlobeProjection(0, 0, GlobeProjection.maxRadius(h), 800, h);
        assertTrue(p.silhouetteRadius() > 0 && Double.isFinite(p.silhouetteRadius()));
        assertTrue(p.project(0, 0, 0).visible());
    }

    @Test void anOrbitHeightIsBroughtInSoItNeverPassesTheCamera() {
        GlobeProjection small = new GlobeProjection(0, 0, 200, 800, 600), big = new GlobeProjection(0, 0, GlobeProjection.maxRadius(600), 800, 600);
        assertTrue(small.orbitAltKm(2.2) > 7000, "room to spare: the full height");
        assertTrue(big.orbitAltKm(2.2) < small.orbitAltKm(2.2), "zoomed right in: closer");
        assertTrue(big.orbitAltKm(2.2) >= 0);
        double rad = (1 + big.orbitAltKm(2.2) / GlobeProjection.EARTH_KM) * GlobeProjection.maxRadius(600);
        assertTrue(rad < big.eyeDistance(), "still in front of the eye");
        assertTrue(big.project(0, 0, big.orbitAltKm(2.2)).visible());
    }

    @Test void theSunOverTheNearSideShowsAndOverTheFarSideIsHidden() {
        GlobeProjection p = new GlobeProjection(0, 0, 250, 800, 600);
        double alt = p.orbitAltKm(2.2);
        assertTrue(p.project(-17, 30, alt).visible());
        assertFalse(p.project(-17, 170, alt).visible());
    }
}
