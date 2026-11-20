package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** J-Map's Sweepstakes sections and North American states / provinces layers. */
class RegionLayersTest {
    static String sec(double lat, double lon) { var r = RegionLayers.at(RegionLayers.sections(), lat, lon); return r == null ? null : r.code(); }
    static String st(double lat, double lon) { var r = RegionLayers.at(RegionLayers.states(), lat, lon); return r == null ? null : r.code(); }

    @Test void thereAre85Sections() {
        assertEquals(85, RegionLayers.sections().size());
        for (var r : RegionLayers.sections()) assertTrue(r.shape().colour() >= 1 && r.shape().colour() <= 7, r.code());
    }

    @Test void placesLandInTheRightSection() {
        assertEquals("WWA", sec(47.61, -122.33), "Seattle");
        assertEquals("EWA", sec(47.66, -117.43), "Spokane");
        assertEquals("EMA", sec(42.36, -71.06), "Boston");
        assertEquals("WMA", sec(42.10, -72.59), "Springfield MA");
        assertEquals("LAX", sec(34.05, -118.24), "Los Angeles");
        assertEquals("SFL", sec(25.76, -80.19), "Miami");
        assertEquals("STX", sec(29.42, -98.49), "San Antonio");
        assertEquals("GH", sec(43.65, -79.38), "Toronto");
        assertEquals("AK", sec(61.2, -149.9), "Anchorage");
        assertEquals("PAC", sec(21.45, -158.0), "Oahu");
        assertEquals("PR", sec(18.4, -66.1), "San Juan");
        assertEquals("WY", sec(43.0, -107.5), "Wyoming");
        assertEquals("BC", sec(50.0, -123.0), "BC");
        assertNull(sec(40, -40), "mid-Atlantic");
    }

    @Test void statesAndProvinces() {
        assertEquals(66, RegionLayers.states().size());
        assertEquals("WA", st(47.61, -122.33));
        assertEquals("ON", st(45.4, -75.7));
        assertEquals("QC", st(48.5, -72.0));
        assertEquals("TX", st(31.0, -100.0));
        assertEquals("DC", st(38.9, -77.03));
    }

    @Test void neighboursNeverShareAColour() {
        String[][] secs = {{"WWA", "EWA"}, {"EWA", "ID"}, {"EMA", "WMA"}, {"NNJ", "SNJ"}, {"ENY", "NNY"}, {"WY", "CO"}, {"ONN", "ONE"}, {"BC", "AB"}};
        var m = new java.util.HashMap<String, Integer>();
        for (var r : RegionLayers.sections()) m.put(r.code(), r.shape().colour());
        for (String[] p : secs) assertNotEquals(m.get(p[0]), m.get(p[1]), p[0] + "/" + p[1]);
        m.clear();
        for (var r : RegionLayers.states()) m.put(r.code(), r.shape().colour());
        for (String[] p : new String[][]{{"WA", "OR"}, {"OR", "CA"}, {"TX", "OK"}, {"NY", "PA"}, {"ON", "QC"}, {"MB", "SK"}, {"WA", "BC"}, {"ND", "MB"}})
            assertNotEquals(m.get(p[0]), m.get(p[1]), p[0] + "/" + p[1]);
    }

    @Test void labelsAreInsideTheirRegion() {
        for (var layer : List.of(RegionLayers.sections(), RegionLayers.states()))
            for (var r : layer) assertEquals(r.code(), RegionLayers.at(List.of(r), r.labelLat(), r.labelLon()) == null ? null : r.code(), "label of " + r.code());
    }
}
