package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlaceLookupTest {
    /** A Nevada W7 sat on the Oregon / Idaho line (the W7 call-area centre); its callbook state or grid puts it in Nevada. */
    @Test
    void aCallbookStatePutsAStationInItsState() {
        double[] nv = PlaceLookup.pointOf(null, "NV");
        assertEquals(39.3, nv[0], 0.1);
        assertEquals(-116.6, nv[1], 0.1);
        assertEquals(nv[0], PlaceLookup.pointOf("", " nv ")[0], 0.001);
    }

    @Test
    void aGridBeatsTheState() {
        double[] g = PlaceLookup.pointOf("DM09fk", "NV");
        assertEquals(39.5, g[0], 0.5);
        assertEquals(-119.8, g[1], 0.5);
    }

    @Test
    void nothingUsableGivesNull() {
        assertNull(PlaceLookup.pointOf(null, null));
        assertNull(PlaceLookup.pointOf("x", "ZZ"));
    }

    @Test
    void usCallsAreTheOnesLookedUp() {
        assertTrue(Geo.isUs("WA7ABC"));
        assertTrue(Geo.isUs("N7ABC-#"));
        assertFalse(Geo.isUs("DL1ABC"));
        assertFalse(Geo.isUs(null));
    }
}
