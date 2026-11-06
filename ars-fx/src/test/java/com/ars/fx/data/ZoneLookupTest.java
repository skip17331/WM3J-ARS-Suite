package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZoneLookupTest {
    @Test void gridCentreOfFourAndSixCharacterSquares() {
        double[] c = ZoneLookup.gridCentre("FN31");
        assertEquals(41.5, c[0], 1e-9); assertEquals(-73.0, c[1], 1e-9);
        double[] d = ZoneLookup.gridCentre("FN31pr");
        assertEquals(41.73, d[0], 0.01); assertEquals(-72.71, d[1], 0.01);
        assertNull(ZoneLookup.gridCentre("ZZ99")); assertNull(ZoneLookup.gridCentre("FN3")); assertNull(ZoneLookup.gridCentre(null));
    }

    @Test void outlinesNameTheZoneOfKnownPlaces() {
        assertEquals(5, ZoneLookup.cqAt(42.36, -71.06));    // Boston
        assertEquals(4, ZoneLookup.cqAt(41.88, -87.63));    // Chicago
        assertEquals(3, ZoneLookup.cqAt(37.77, -122.42));   // San Francisco
        assertEquals(14, ZoneLookup.cqAt(51.5, -0.12));     // London
        assertEquals(25, ZoneLookup.cqAt(35.68, 139.69));   // Tokyo
        assertEquals(30, ZoneLookup.cqAt(-33.87, 151.21));  // Sydney
        assertEquals(29, ZoneLookup.cqAt(-31.95, 115.86));  // Perth
        assertEquals(11, ZoneLookup.cqAt(-23.55, -46.63));  // Sao Paulo
    }

    @Test void gridSettlesTheZoneWhereTheCountryHasSeveral() {
        assertEquals(3, ZoneLookup.cq("W6XYZ", "CM87"));    // the callsign alone says 5
        assertEquals(5, ZoneLookup.cq("W1AW", "FN31"));
        assertEquals(4, ZoneLookup.cq("K0ABC", "EN34"));
        assertEquals(30, ZoneLookup.cq("VK2ABC", "QF56"));
        assertEquals(29, ZoneLookup.cq("VK6ABC", "OF78"));
        assertEquals(3, ZoneLookup.cq("VE7ABC", "CN89"));
    }

    @Test void withoutAUsableGridTheCountryDefaultStands() {
        int deflt = ZoneLookup.zones("W6XYZ", null)[0];
        assertEquals(deflt, ZoneLookup.cq("W6XYZ", ""));
        assertEquals(deflt, ZoneLookup.cq("W6XYZ", "garbage"));
        assertTrue(ZoneLookup.zones("W6XYZ", null)[1] > 0);
    }

    @Test void aGridOutsideTheCountrysZonesIsIgnored() {
        // a single-zone country keeps its zone whatever grid is typed: Germany is zone 14
        assertEquals(14, ZoneLookup.cq("DL1ABC", "JO62"));
        assertEquals(14, ZoneLookup.cq("DL1ABC", "CM87"));
    }
}
