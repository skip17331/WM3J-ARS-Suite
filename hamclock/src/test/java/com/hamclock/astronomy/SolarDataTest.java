package com.hamclock.astronomy;

import com.hamclock.service.solar.MockSolarDataProvider;
import com.hamclock.service.solar.SolarData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for solar/geomagnetic data parsing and domain model logic.
 */
class SolarDataTest {

    private MockSolarDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new MockSolarDataProvider();
    }

    // ── Domain model ─────────────────────────────────────────

    @ParameterizedTest(name = "kp={0} → {1}")
    @CsvSource({
        "0.0, QUIET",
        "1.5, QUIET",
        "2.9, QUIET",
        "3.0, MINOR",
        "4.5, MINOR",
        "5.0, MODERATE",
        "5.9, MODERATE",
        "6.0, STRONG",
        "6.9, STRONG",
        "7.0, SEVERE",
        "7.9, SEVERE",
        "8.0, EXTREME",
        "9.0, EXTREME",
    })
    void getKpLabel_correctClassification(double kp, String expectedLabel) {
        SolarData data = new SolarData();
        data.setKp(kp);
        assertEquals(expectedLabel, data.getKpLabel(),
            "Kp=" + kp + " should be classified as " + expectedLabel);
    }

    @ParameterizedTest(name = "sfi={0} → {1}")
    @CsvSource({
        "65,  VERY POOR",
        "79,  VERY POOR",
        "80,  POOR",
        "99,  POOR",
        "100, FAIR",
        "119, FAIR",
        "120, GOOD",
        "149, GOOD",
        "150, VERY GOOD",
        "199, VERY GOOD",
        "200, EXCELLENT",
        "250, EXCELLENT",
    })
    void getSfiQuality_correctClassification(double sfi, String expectedQuality) {
        SolarData data = new SolarData();
        data.setSfi(sfi);
        assertEquals(expectedQuality, data.getSfiQuality(),
            "SFI=" + sfi + " should be classified as " + expectedQuality);
    }

    @Test
    void solarData_constructorWithArgs_setsAllFields() {
        SolarData data = new SolarData(150.0, 3.5, 12, 120);
        assertEquals(150.0, data.getSfi(), 0.001);
        assertEquals(3.5,   data.getKp(),  0.001);
        assertEquals(12,    data.getAIndex());
        assertEquals(120,   data.getSunspotNumber());
    }

    @Test
    void solarData_fourArgConstructor_isFreshByDefault() {
        SolarData data = new SolarData(100, 2, 8, 80);
        assertTrue(data.isFresh(), "Data created via constructor should be fresh");
    }

    @Test
    void solarData_fourArgConstructor_hasObservationTime() {
        SolarData data = new SolarData(100, 2, 8, 80);
        assertNotNull(data.getObservationTime(), "Observation time should be set");
    }

    @Test
    void solarData_toString_containsKeyValues() {
        SolarData data = new SolarData(123.4, 2.5, 10, 75);
        data.setXrayClass("C3.2");
        String str = data.toString();
        assertTrue(str.contains("123"), "toString should contain SFI");
        assertTrue(str.contains("C3.2"), "toString should contain X-ray class");
    }

    // ── Mock provider ─────────────────────────────────────────

    @Test
    void mockProvider_fetch_returnsNonNull() throws Exception {
        SolarData data = provider.fetch();
        assertNotNull(data, "Mock provider should return non-null data");
    }

    @Test
    void mockProvider_sfi_inRealisticRange() throws Exception {
        SolarData data = provider.fetch();
        assertTrue(data.getSfi() >= 65 && data.getSfi() <= 250,
            "SFI should be in realistic range [65,250], got: " + data.getSfi());
    }

    @Test
    void mockProvider_kp_inValidRange() throws Exception {
        SolarData data = provider.fetch();
        assertTrue(data.getKp() >= 0 && data.getKp() <= 9,
            "Kp should be in [0,9], got: " + data.getKp());
    }

    @Test
    void mockProvider_aIndex_isNonNegative() throws Exception {
        SolarData data = provider.fetch();
        assertTrue(data.getAIndex() >= 0,
            "A-index should be non-negative, got: " + data.getAIndex());
    }

    @Test
    void mockProvider_sunspotNumber_isNonNegative() throws Exception {
        SolarData data = provider.fetch();
        assertTrue(data.getSunspotNumber() >= 0,
            "Sunspot number should be non-negative, got: " + data.getSunspotNumber());
    }

    @Test
    void mockProvider_xrayClass_isNotBlank() throws Exception {
        SolarData data = provider.fetch();
        assertNotNull(data.getXrayClass(), "X-ray class should not be null");
        assertFalse(data.getXrayClass().isBlank(), "X-ray class should not be blank");
    }

    @Test
    void mockProvider_xrayClass_startsWithValidLetter() throws Exception {
        SolarData data = provider.fetch();
        char firstChar = data.getXrayClass().charAt(0);
        assertTrue("ABCMX".indexOf(firstChar) >= 0,
            "X-ray class should start with A/B/C/M/X, got: " + data.getXrayClass());
    }

    @Test
    void mockProvider_isStale_afterFetch_withShortTimeout() throws Exception {
        provider.fetch();
        // Data fetched just now should NOT be stale with a 10-minute window
        assertFalse(provider.isStale(Duration.ofMinutes(10)),
            "Freshly fetched data should not be stale in 10 minutes");
    }

    @Test
    void mockProvider_isStale_withZeroDuration() throws Exception {
        provider.fetch();
        // Even freshly fetched data is stale with 0 duration (nothing is instant)
        assertTrue(provider.isStale(Duration.ZERO),
            "Any data is stale with zero max age");
    }

    @Test
    void mockProvider_getCached_beforeFetch_isNull() {
        assertNull(provider.getCached(),
            "getCached() before any fetch should return null");
    }

    @Test
    void mockProvider_getCached_afterFetch_matchesFetch() throws Exception {
        SolarData fetched = provider.fetch();
        SolarData cached = provider.getCached();
        assertNotNull(cached);
        // Should be the same instance (caching by reference)
        assertSame(fetched, cached,
            "getCached() should return the same instance returned by fetch()");
    }

    @Test
    void mockProvider_lastUpdated_nullBeforeFetch() {
        assertNull(provider.getLastUpdated(),
            "lastUpdated should be null before first fetch");
    }

    @Test
    void mockProvider_lastUpdated_setAfterFetch() throws Exception {
        provider.fetch();
        assertNotNull(provider.getLastUpdated(),
            "lastUpdated should be set after fetch");
    }

    // ── Derived fields ────────────────────────────────────────

    @Test
    void kpLabel_allEnumValues_nonNull() {
        SolarData data = new SolarData();
        double[] kpValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        for (double kp : kpValues) {
            data.setKp(kp);
            assertNotNull(data.getKpLabel(), "Kp=" + kp + " should have a non-null label");
            assertFalse(data.getKpLabel().isBlank(), "Kp=" + kp + " label should not be blank");
        }
    }

    @Test
    void sfiQuality_allRanges_nonNull() {
        SolarData data = new SolarData();
        double[] sfiValues = {65, 80, 100, 120, 150, 200};
        for (double sfi : sfiValues) {
            data.setSfi(sfi);
            assertNotNull(data.getSfiQuality(), "SFI=" + sfi + " should have a non-null quality");
        }
    }
}
