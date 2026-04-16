package com.wm3j.jmap.rotor;

import com.wm3j.jmap.service.rotor.MockRotorProvider;
import com.wm3j.jmap.service.rotor.RotorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for rotor data parsing, long-path math, and mock provider behavior.
 */
class RotorDataTest {

    private MockRotorProvider mockProvider;

    @BeforeEach
    void setUp() {
        mockProvider = new MockRotorProvider();
    }

    // ── RotorData domain model ───────────────────────────────

    @Test
    void rotorData_longPath_is180DegreesFromShortPath() {
        RotorData data = new RotorData(90.0);
        assertEquals(270.0, data.getLongPathAzimuth(), 0.001,
            "Long path should be short path + 180°");
    }

    @Test
    void rotorData_longPath_wrapsAt360() {
        RotorData data = new RotorData(270.0);
        assertEquals(90.0, data.getLongPathAzimuth(), 0.001,
            "Long path should wrap correctly at 360°");
    }

    @Test
    void rotorData_longPath_northFacingIsNorth() {
        RotorData data = new RotorData(180.0); // facing South
        assertEquals(0.0, data.getLongPathAzimuth(), 0.001,
            "Long path of South-facing is North");
    }

    @Test
    void rotorData_longPath_zeroAzimuth_is180() {
        RotorData data = new RotorData(0.0);
        assertEquals(180.0, data.getLongPathAzimuth(), 0.001);
    }

    @ParameterizedTest(name = "az={0}")
    @ValueSource(doubles = {0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0, 359.9})
    void rotorData_longPath_alwaysInValidRange(double az) {
        RotorData data = new RotorData(az);
        double lp = data.getLongPathAzimuth();
        assertTrue(lp >= 0.0 && lp < 360.0,
            "Long path " + lp + " should be in [0, 360) for az=" + az);
    }

    @ParameterizedTest(name = "az={0}")
    @ValueSource(doubles = {0.0, 45.0, 90.0, 135.0, 180.0, 225.0, 270.0, 315.0, 359.9})
    void rotorData_shortPlusLong_is360(double az) {
        RotorData data = new RotorData(az);
        double sum = az + data.getLongPathAzimuth();
        // Sum should be 360 (or 180 if az=0 and lp=180)
        double expected = az == 0.0 ? 180.0 : 360.0;
        // Actually normalize: (az + lp) mod 360 should always equal 0 (or both are complements)
        double diff = Math.abs(az - data.getLongPathAzimuth());
        if (diff > 180) diff = 360 - diff;
        assertEquals(180.0, diff, 0.01,
            "Short and long path should differ by exactly 180°");
    }

    @Test
    void rotorData_constructor_setsTimestamp() {
        RotorData data = new RotorData(90.0);
        assertNotNull(data.getTimestamp(), "Timestamp should be set by constructor");
    }

    @Test
    void rotorData_defaultConstructor_isNotConnected() {
        RotorData data = new RotorData();
        assertFalse(data.isConnected(), "Default constructor should set connected=false");
    }

    @Test
    void rotorData_singleArgConstructor_isConnected() {
        RotorData data = new RotorData(90.0);
        assertTrue(data.isConnected(), "Single-arg constructor should set connected=true");
    }

    @Test
    void rotorData_twoArgConstructor_hasElevation() {
        RotorData data = new RotorData(90.0, 45.0);
        assertEquals(45.0, data.getElevation(), 0.001);
        assertTrue(data.isElevationSupported());
    }

    // ── MockRotorProvider behavior ───────────────────────────

    @Test
    void mockProvider_fetch_returnsData() throws Exception {
        RotorData data = mockProvider.fetch();
        assertNotNull(data, "Mock provider should return non-null data");
    }

    @Test
    void mockProvider_azimuth_inValidRange() throws Exception {
        for (int i = 0; i < 10; i++) {
            RotorData data = mockProvider.fetch();
            assertTrue(data.getAzimuth() >= 0 && data.getAzimuth() < 360,
                "Azimuth should be in [0, 360), got: " + data.getAzimuth());
        }
    }

    @Test
    void mockProvider_azimuth_changesOverTime() throws Exception {
        RotorData first = mockProvider.fetch();
        RotorData second = mockProvider.fetch();
        assertNotEquals(first.getAzimuth(), second.getAzimuth(),
            "Mock provider azimuth should change between calls");
    }

    @Test
    void mockProvider_isConnected_returnsFalse() throws Exception {
        // Mock provider indicates demo mode (not a real rotor)
        RotorData data = mockProvider.fetch();
        assertFalse(data.isConnected(),
            "Mock provider should return connected=false to indicate demo mode");
    }

    @Test
    void mockProvider_lastUpdated_isSetAfterFetch() throws Exception {
        assertNull(mockProvider.getLastUpdated(), "Should be null before first fetch");
        mockProvider.fetch();
        assertNotNull(mockProvider.getLastUpdated(), "Should be set after fetch");
    }

    @Test
    void mockProvider_getCached_returnsNullBeforeFetch() {
        assertNull(mockProvider.getCached(), "getCached() should return null before any fetch");
    }

    @Test
    void mockProvider_getCached_returnsDataAfterFetch() throws Exception {
        mockProvider.fetch();
        assertNotNull(mockProvider.getCached(), "getCached() should return data after fetch");
    }

    // ── Azimuth arithmetic ───────────────────────────────────

    @Test
    void azimuth_normalization_over360_wraps() {
        double az = 370.0;
        double normalized = az % 360.0;
        assertEquals(10.0, normalized, 0.001);
    }

    @Test
    void azimuth_normalization_negative_wraps() {
        double az = -10.0;
        double normalized = ((az % 360) + 360) % 360;
        assertEquals(350.0, normalized, 0.001);
    }

    @Test
    void toString_containsAzimuth() throws Exception {
        RotorData data = new RotorData(135.5, 22.3);
        String str = data.toString();
        assertTrue(str.contains("135.5"), "toString should contain azimuth");
        assertTrue(str.contains("22.3"), "toString should contain elevation");
    }
}
