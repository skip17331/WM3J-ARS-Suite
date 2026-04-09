package com.hamclock.astronomy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamclock.service.config.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Settings serialization / deserialization.
 */
class SettingsSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void settings_defaultValues_areReasonable() {
        Settings s = new Settings();
        assertFalse(s.getCallsign().isBlank(), "Default callsign should not be blank");
        assertTrue(s.getQthLat() >= -90 && s.getQthLat() <= 90, "QTH lat should be valid");
        assertTrue(s.getQthLon() >= -180 && s.getQthLon() <= 180, "QTH lon should be valid");
        assertTrue(s.getWebServerPort() > 0 && s.getWebServerPort() < 65536, "Port should be valid");
    }

    @Test
    void settings_roundTripJson_preservesValues() throws Exception {
        Settings original = new Settings();
        original.setCallsign("VK2XYZ");
        original.setQthLat(-33.87);
        original.setQthLon(151.21);
        original.setShowGrayline(false);
        original.setShowDxSpots(true);
        original.setDxMaxAgeMinutes(45);
        original.setRotorEnabled(true);
        original.setArduinoIp("10.0.0.50");
        original.setArduinoPort(4533);

        String json = mapper.writeValueAsString(original);
        Settings restored = mapper.readValue(json, Settings.class);

        assertEquals(original.getCallsign(),      restored.getCallsign());
        assertEquals(original.getQthLat(),        restored.getQthLat(),  0.001);
        assertEquals(original.getQthLon(),        restored.getQthLon(),  0.001);
        assertEquals(original.isShowGrayline(),   restored.isShowGrayline());
        assertEquals(original.isShowDxSpots(),    restored.isShowDxSpots());
        assertEquals(original.getDxMaxAgeMinutes(), restored.getDxMaxAgeMinutes());
        assertEquals(original.isRotorEnabled(),   restored.isRotorEnabled());
        assertEquals(original.getArduinoIp(),     restored.getArduinoIp());
        assertEquals(original.getArduinoPort(),   restored.getArduinoPort());
    }

    @Test
    void settings_json_isReadableString() throws Exception {
        Settings s = new Settings();
        s.setCallsign("W1AW");
        String json = mapper.writeValueAsString(s);

        assertTrue(json.contains("W1AW"), "JSON should contain callsign");
        assertTrue(json.contains("qthLat"), "JSON should contain field names");
        assertTrue(json.startsWith("{"), "JSON should be a JSON object");
    }

    @Test
    void settings_deserialize_ignoresUnknownFields() throws Exception {
        // Settings with an extra unknown field should not throw
        String json = """
            {
              "callsign": "G0ABC",
              "unknownFutureField": "someValue",
              "qthLat": 51.5
            }
            """;
        Settings s = mapper.readValue(json, Settings.class);
        assertEquals("G0ABC", s.getCallsign());
        assertEquals(51.5, s.getQthLat(), 0.001);
    }

    @Test
    void settings_nullApiKeys_returnEmptyString() {
        Settings s = new Settings();
        s.setNoaaApiKey(null);
        s.setOpenWeatherApiKey(null);

        assertEquals("", s.getNoaaApiKey(), "Null NOAA key should return empty string");
        assertEquals("", s.getOpenWeatherApiKey(), "Null OWM key should return empty string");
    }

    @Test
    void settings_dxBandFilter_defaultIsALL() {
        Settings s = new Settings();
        assertEquals("ALL", s.getDxBandFilter(), "Default DX band filter should be ALL");
    }

    @Test
    void settings_arduinoProtocol_defaultIsHTTP() {
        Settings s = new Settings();
        assertEquals("HTTP", s.getArduinoProtocol(), "Default Arduino protocol should be HTTP");
    }

    @Test
    void settings_allBooleans_haveReasonableDefaults() {
        Settings s = new Settings();
        assertTrue(s.isShowWorldMap(),     "World map should be visible by default");
        assertTrue(s.isShowGrayline(),     "Grayline should be visible by default");
        assertTrue(s.isShowDxSpots(),      "DX spots should be visible by default");
        assertTrue(s.isShowSolarData(),    "Solar data should be visible by default");
        assertTrue(s.isShowRotorMap(),     "Rotor map should be visible by default");
        assertTrue(s.isShowLocalTime(),    "Local time should be visible by default");
        assertTrue(s.isShowUtcTime(),      "UTC time should be visible by default");
        assertFalse(s.isRotorEnabled(),    "Rotor should be disabled by default");
        assertTrue(s.isUseMockData(),      "Mock data should be default (no API keys required)");
    }
}
