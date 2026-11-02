package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class GeoTest {

    /** Real spots the old prefix table dropped, which took them off J-Map altogether. */
    @Test
    void placesCallsOutsideThePrefixTable() {
        for (String call : new String[]{"5H3MB", "3V8LL", "SQ8LUN", "PR2AR"}) {
            assertNotNull(Geo.of(call), call);
        }
    }

    @Test
    void placesDxccEntitiesWhereThePrefixTableGuessedWrong() {
        double[] hawaii = Geo.of("KH6ABC");
        assertNotNull(hawaii);
        assertEquals(-157, hawaii[1], 10);          // not the "K" mainland fallback
    }

    @Test
    void keepsUsCallAreas() {
        assertEquals(-120.0, Geo.of("W6ABC")[1], 0.01);
        assertEquals(-77.5, Geo.of("W3LPL-#")[1], 0.01);
    }

    @Test
    void unknownCallsStayUnplaced() {
        assertNull(Geo.of(""));
        assertNull(Geo.of(null));
    }

    /** These entities have only a map label to work from; check it lands in the right part of the world. */
    @Test
    void placesEntitiesKnownOnlyFromTheMapOutlines() {
        double[] tanzania = Geo.of("5H3MB");
        assertEquals(-6, tanzania[0], 5); assertEquals(35, tanzania[1], 5);
        double[] tunisia = Geo.of("3V8LL");
        assertEquals(34, tunisia[0], 5); assertEquals(9, tunisia[1], 5);
        assertEquals("Tanzania", DxccInfo.of("5H3MB").country());
    }
}
