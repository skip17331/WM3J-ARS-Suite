package com.jlog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaidenheadTest {

    @Test
    void center4CharIsSquareCenter() {
        double[] c = Maidenhead.center("FN31");
        assertEquals(41.5,  c[0], 1e-9);   // latitude
        assertEquals(-73.0, c[1], 1e-9);   // longitude
    }

    @Test
    void center6CharIsSubsquareCenter() {
        // FN31pr ≈ W1AW; subsquare center, not the 4-char square center.
        double[] c = Maidenhead.center("FN31pr");
        assertEquals(41.729, c[0], 0.01);
        assertEquals(-72.708, c[1], 0.01);
    }

    @Test
    void caseInsensitiveAndTrimmed() {
        double[] a = Maidenhead.center("  fn31PR ");
        double[] b = Maidenhead.center("FN31pr");
        assertEquals(b[0], a[0], 1e-9);
        assertEquals(b[1], a[1], 1e-9);
    }

    @Test
    void invalidGridReturnsNull() {
        assertNull(Maidenhead.center("ZZ99"));   // Z out of A-R field range
        assertNull(Maidenhead.center("FOO"));
        assertNull(Maidenhead.center(null));
    }

    @Test
    void sameGridDistanceIsZero() {
        assertEquals(0.0, Maidenhead.distanceKm("FN31", "FN31"), 1e-6);
    }

    @Test
    void shortAndLongDistancesAreSane() {
        double near = Maidenhead.distanceKm("FN20", "FN31");      // ~2 grids apart
        assertTrue(near > 150 && near < 260, "got " + near);

        double far = Maidenhead.distanceKm("FN31", "JN58");       // NY ↔ Munich
        assertTrue(far > 5000 && far < 8000, "got " + far);
    }

    @Test
    void invalidGridDistanceIsNegativeOne() {
        assertEquals(-1.0, Maidenhead.distanceKm("ZZ99", "FN31"), 1e-9);
        assertEquals(-1.0, Maidenhead.distanceKm("FN31", null), 1e-9);
    }
}
