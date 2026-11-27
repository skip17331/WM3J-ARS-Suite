package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** J-Map's North American states / provinces layer. */
class RegionLayersTest {
    static String st(double lat, double lon) { var r = RegionLayers.at(RegionLayers.states(), lat, lon); return r == null ? null : r.code(); }

    @Test void statesAndProvinces() {
        assertEquals(66, RegionLayers.states().size());
        assertEquals("WA", st(47.61, -122.33));
        assertEquals("ON", st(45.4, -75.7));
        assertEquals("QC", st(48.5, -72.0));
        assertEquals("TX", st(31.0, -100.0));
        assertEquals("DC", st(38.9, -77.03));
    }

    @Test void neighboursNeverShareAColour() {
        var m = new java.util.HashMap<String, Integer>();
        for (var r : RegionLayers.states()) m.put(r.code(), r.shape().colour());
        for (String[] p : new String[][]{{"WA", "OR"}, {"OR", "CA"}, {"TX", "OK"}, {"NY", "PA"}, {"ON", "QC"}, {"MB", "SK"}, {"WA", "BC"}, {"ND", "MB"}})
            assertNotEquals(m.get(p[0]), m.get(p[1]), p[0] + "/" + p[1]);
    }

    @Test void labelsAreInsideTheirRegion() {
        for (var layer : List.of(RegionLayers.states()))
            for (var r : layer) assertEquals(r.code(), RegionLayers.at(List.of(r), r.labelLat(), r.labelLon()) == null ? null : r.code(), "label of " + r.code());
    }
}
