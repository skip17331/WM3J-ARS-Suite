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

    /** WA7ABC / KB6ABC / N7ABC were all dropped at the generic US centre because only the first letter was looked at. */
    @Test
    void usCallAreaComesFromTheDigit() {
        assertEquals(-120.0, Geo.of("KB6ABC")[1], 0.01);
        assertEquals(-117.0, Geo.of("WA7ABC")[1], 0.01);
        assertEquals(-117.0, Geo.of("N7ABC")[1], 0.01);
        assertEquals(-72.0, Geo.of("AA1AA")[1], 0.01);
        assertEquals(-157, Geo.of("KH6ABC")[1], 2);   // a special prefix still wins over the digit
    }

    @Test
    void washingtonSectionsAreNotOnTheOregonIdahoLine() {
        assertEquals(47.5, Geo.stateCentre("WWA")[0], 0.1);
        assertEquals(-122.3, Geo.stateCentre("WWA")[1], 0.1);
        assertEquals(44.0, Geo.stateCentre("OR")[0], 0.1);
        assertNull(Geo.stateCentre("DX"));
    }

    /** Every Canadian call was put at one Canada-wide centre, in Saskatchewan. */
    @Test
    void canadianCallsComeFromTheirProvince() {
        assertEquals(-125.0, Geo.of("VE7ABC")[1], 1);         // British Columbia
        assertEquals(-125.0, Geo.of("VA7XY")[1], 1);
        assertEquals(-114.0, Geo.of("VE6AA")[1], 1);          // Alberta
        assertEquals(-106.0, Geo.of("VE5XX")[1], 1);          // Saskatchewan
        assertEquals(-85.0, Geo.of("VE3ABC")[1], 1);          // Ontario
        assertEquals(-135.0, Geo.of("VY1JA")[1], 1);          // Yukon
        assertEquals(-56.0, Geo.of("VO1AA")[1], 1);           // Newfoundland
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
