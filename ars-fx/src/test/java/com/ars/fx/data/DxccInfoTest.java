package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DxccInfoTest {
    @Test
    void resolvesCountryAndContinent() {
        DxccInfo.Info de = DxccInfo.of("DL1ABC");
        assertNotNull(de);
        assertEquals("Germany", de.country());
        assertEquals("EU", de.continent());
        assertTrue(de.dxcc() > 0);
        assertFalse(Double.isNaN(de.lat()));

        DxccInfo.Info us = DxccInfo.of("W3LPL");
        assertEquals("United States", us.country());
        assertEquals("NA", us.continent());
    }

    @Test
    void unresolvableOrMaritimeGivesNull() {
        assertNull(DxccInfo.of(null));
        assertNull(DxccInfo.of(""));
        assertNull(DxccInfo.of("W3ABC/MM"));
    }

    @Test
    void countryNameMapsAnEntityIdAndPassesUnknownsThrough() {
        DxccInfo.Info de = DxccInfo.of("DL1ABC");
        assertEquals("Germany", DxccInfo.countryName(String.valueOf(de.dxcc())));
        assertEquals("nowhere", DxccInfo.countryName("nowhere"));
    }

    @Test
    void greatCircleDistanceAndBearing() {
        // London → Paris is about 344 km, heading south-east (~149°)
        assertEquals(344, DxccInfo.distanceKm(51.5074, -0.1278, 48.8566, 2.3522), 3);
        assertEquals(149, DxccInfo.bearingDeg(51.5074, -0.1278, 48.8566, 2.3522), 2);
        // due north and due east
        assertEquals(0, DxccInfo.bearingDeg(10, 20, 30, 20), 0.01);
        assertEquals(90, DxccInfo.bearingDeg(0, 0, 0, 10), 0.01);
        assertEquals(0, DxccInfo.distanceKm(40, -75, 40, -75), 1e-9);
    }
}
