package com.wm3j.jmap.dx;

import com.wm3j.jmap.service.dx.DxSpot;
import com.wm3j.jmap.service.dx.MockDxSpotProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DX spot domain model, frequency-to-band conversion,
 * and mock provider behavior.
 */
class DxSpotTest {

    // ── Frequency-to-band mapping ────────────────────────────

    @ParameterizedTest(name = "{0} kHz → {1}")
    @CsvSource({
        "1840,   160m",
        "1900,   160m",
        "3525,   80m",
        "3790,   80m",
        "5357,   60m",
        "7025,   40m",
        "7074,   40m",
        "10115,  30m",
        "14025,  20m",
        "14225,  20m",
        "14280,  20m",
        "18100,  17m",
        "21025,  15m",
        "21340,  15m",
        "24915,  12m",
        "28025,  10m",
        "28500,  10m",
        "50125,  6m",
        "144200, 2m",
    })
    void frequencyToBand_correctMapping(double freqKhz, String expectedBand) {
        assertEquals(expectedBand, DxSpot.frequencyToBand(freqKhz),
            "Frequency " + freqKhz + " kHz should map to " + expectedBand);
    }

    @Test
    void dxSpot_constructor_setsBandAutomatically() {
        DxSpot spot = new DxSpot("W1AW", "VK2XYZ", 14225.0, Instant.now());
        assertEquals("20m", spot.getBand(),
            "Constructor should auto-detect band from frequency");
    }

    @Test
    void dxSpot_setFrequency_updatesBand() {
        DxSpot spot = new DxSpot("W1AW", "VK2XYZ", 14225.0, Instant.now());
        spot.setFrequencyKhz(7074.0);
        assertEquals("40m", spot.getBand(),
            "setFrequencyKhz should update band");
    }

    @Test
    void dxSpot_ageMinutes_freshSpotIsZero() {
        DxSpot spot = new DxSpot("W1AW", "VK2XYZ", 14225.0, Instant.now());
        assertTrue(spot.ageMinutes() < 1,
            "Fresh spot should be less than 1 minute old");
    }

    @Test
    void dxSpot_ageMinutes_oldSpotIsCorrect() {
        Instant thirtyMinsAgo = Instant.now().minusSeconds(30 * 60);
        DxSpot spot = new DxSpot("W1AW", "VK2XYZ", 14225.0, thirtyMinsAgo);
        long age = spot.ageMinutes();
        assertTrue(age >= 29 && age <= 31,
            "30-minute-old spot should report ~30 minutes, got: " + age);
    }

    // ── Band color validation ────────────────────────────────

    @ParameterizedTest(name = "band={0}")
    @CsvSource({
        "160m, #ff4444",
        "80m,  #ff8800",
        "40m,  #ffff00",
        "20m,  #00ff00",
        "10m,  #aa00ff",
        "6m,   #ff00ff",
    })
    void getBandColor_returnsCorrectColor(String band, String expectedColor) {
        DxSpot spot = new DxSpot();
        spot.setBand(band);
        assertEquals(expectedColor, spot.getBandColor(),
            "Band " + band + " should have color " + expectedColor);
    }

    @Test
    void getBandColor_unknownBand_returnsWhite() {
        DxSpot spot = new DxSpot();
        spot.setBand("QRP");
        assertEquals("#ffffff", spot.getBandColor(),
            "Unknown band should return white fallback");
    }

    // ── Mock provider ────────────────────────────────────────

    @Test
    void mockProvider_fetch_returnsNonEmptyList() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        assertNotNull(spots);
        assertFalse(spots.isEmpty(), "Mock provider should return some spots");
    }

    @Test
    void mockProvider_spots_haveValidCallsigns() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        for (DxSpot spot : spots) {
            assertNotNull(spot.getDxCallsign(), "DX callsign should not be null");
            assertFalse(spot.getDxCallsign().isBlank(), "DX callsign should not be blank");
            assertNotNull(spot.getSpotter(), "Spotter should not be null");
        }
    }

    @Test
    void mockProvider_spots_haveValidFrequencies() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        for (DxSpot spot : spots) {
            assertTrue(spot.getFrequencyKhz() >= 1800 && spot.getFrequencyKhz() <= 60000,
                "Frequency " + spot.getFrequencyKhz() + " kHz should be in HF/6m range");
        }
    }

    @Test
    void mockProvider_spots_haveValidBands() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        List<String> validBands = List.of(
            "160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m","UHF");
        for (DxSpot spot : spots) {
            assertTrue(validBands.contains(spot.getBand()),
                "Band " + spot.getBand() + " should be a valid amateur band");
        }
    }

    @Test
    void mockProvider_spots_haveTimestamps() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        for (DxSpot spot : spots) {
            assertNotNull(spot.getTimestamp(), "Spot should have a timestamp");
            // Should not be in the future
            assertTrue(!spot.getTimestamp().isAfter(Instant.now().plusSeconds(5)),
                "Spot timestamp should not be in the future");
        }
    }

    @Test
    void mockProvider_returnsReasonableCount() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        assertTrue(spots.size() >= 10 && spots.size() <= 100,
            "Mock provider should return 10-100 spots, got: " + spots.size());
    }

    @Test
    void mockProvider_getCached_afterFetch_isNotNull() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        provider.fetch();
        assertNotNull(provider.getCached(), "getCached() should return data after fetch");
    }

    @Test
    void dxSpot_toString_containsCallsign() {
        DxSpot spot = new DxSpot("W1AW", "VK2XYZ", 14225.0, Instant.now());
        String str = spot.toString();
        assertTrue(str.contains("VK2XYZ"), "toString should contain DX callsign");
        assertTrue(str.contains("W1AW"), "toString should contain spotter callsign");
    }

    @Test
    void dxSpot_bandColorIsValidHex() throws Exception {
        MockDxSpotProvider provider = new MockDxSpotProvider();
        List<DxSpot> spots = provider.fetch();
        for (DxSpot spot : spots) {
            String color = spot.getBandColor();
            assertNotNull(color, "Band color should not be null");
            assertTrue(color.matches("#[0-9a-fA-F]{6}"),
                "Band color should be a valid 6-digit hex: " + color);
        }
    }
}
