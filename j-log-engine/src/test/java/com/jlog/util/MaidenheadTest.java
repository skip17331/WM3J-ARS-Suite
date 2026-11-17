package com.jlog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    void eightAndTenCharacterGridsHaveACentre() {
        // used to be null: a station that entered its precise grid had no location
        double[] six = Maidenhead.center("FN20xr");
        double[] eight = Maidenhead.center("FN20xr45");
        double[] ten = Maidenhead.center("fn20xr45ab");
        assertNotNull(eight);
        assertNotNull(ten);
        // inside the 6-character square: lon 2/24 deg wide, lat 1/24 deg high
        assertEquals(six[0], eight[0], 1.0 / 48);
        assertEquals(six[1], eight[1], 2.0 / 48);
        // FN20xr45: lon -76 + 23*(2/24) + 4*(2/240) + half of 2/240; lat 40 + 17/24 + 5/240 + half of 1/240
        assertEquals(-76 + 23 * (2.0 / 24) + 4 * (2.0 / 240) + 1.0 / 240, eight[1], 1e-9);
        assertEquals(40 + 17 / 24.0 + 5 / 240.0 + 0.5 / 240, eight[0], 1e-9);
        assertEquals(eight[0], ten[0], 1.0 / 480);
        assertEquals(eight[1], ten[1], 1.0 / 240);
    }

    @Test
    void notGrids() {
        assertNull(Maidenhead.center("FN20xr4"));
        assertNull(Maidenhead.center("FN20xr45zz"));     // 10th-place letters run A-X
        assertNull(Maidenhead.center("FN20"  + "ZZ"));
    }

    @Test
    void invalidGridDistanceIsNegativeOne() {
        assertEquals(-1.0, Maidenhead.distanceKm("ZZ99", "FN31"), 1e-9);
        assertEquals(-1.0, Maidenhead.distanceKm("FN31", null), 1e-9);
    }
}
