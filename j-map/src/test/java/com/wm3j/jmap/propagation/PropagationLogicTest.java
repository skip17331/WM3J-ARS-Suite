package com.wm3j.jmap.propagation;

import com.wm3j.jmap.service.propagation.MockPropagationProvider;
import com.wm3j.jmap.service.propagation.PropagationData;
import com.wm3j.jmap.service.propagation.PropagationData.BandCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for propagation data logic.
 */
class PropagationLogicTest {

    @Test
    void bandCondition_aboveMUF_isClosed() {
        PropagationData data = new PropagationData();
        data.setMuf(14.0);  // MUF = 14 MHz

        // 10m (28 MHz) is far above MUF - should be CLOSED
        data.setBandCondition("10m", BandCondition.CLOSED);
        assertEquals(BandCondition.CLOSED, data.getBandConditions().get("10m"));
    }

    @Test
    void bandCondition_colorHex_isNotNull() {
        for (BandCondition bc : BandCondition.values()) {
            assertNotNull(bc.colorHex, "Color hex should not be null for " + bc);
            assertTrue(bc.colorHex.startsWith("#"), "Color should be hex: " + bc.colorHex);
        }
    }

    @Test
    void bandCondition_label_isNotNull() {
        for (BandCondition bc : BandCondition.values()) {
            assertNotNull(bc.label, "Label should not be null for " + bc);
            assertFalse(bc.label.isBlank(), "Label should not be blank for " + bc);
        }
    }

    @Test
    void propagationData_defaultBands_allPresent() {
        PropagationData data = new PropagationData();
        String[] expectedBands = {"80m", "60m", "40m", "30m", "20m", "17m", "15m", "12m", "10m", "6m"};
        for (String band : expectedBands) {
            assertTrue(data.getBandConditions().containsKey(band),
                "Default bands should include " + band);
        }
    }

    @Test
    void mockProvider_fetchReturnsValidData() throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();
        PropagationData data = provider.fetch();

        assertNotNull(data, "Fetch should return non-null data");
        assertTrue(data.getMuf() > 0, "MUF should be positive");
        assertTrue(data.getFot() > 0, "FOT should be positive");
        assertTrue(data.getFot() < data.getMuf(), "FOT should be less than MUF");
        assertFalse(data.getBandConditions().isEmpty(), "Band conditions should not be empty");
    }

    @Test
    void mockProvider_muf_isSensibleRange() throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();

        for (int i = 0; i < 5; i++) {
            PropagationData data = provider.fetch();
            assertTrue(data.getMuf() >= 3.0 && data.getMuf() <= 35.0,
                "MUF should be in [3, 35] MHz, got: " + data.getMuf());
        }
    }

    @Test
    void mockProvider_fot_isBelowMUF() throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();
        PropagationData data = provider.fetch();

        assertTrue(data.getFot() <= data.getMuf(),
            "FOT (" + data.getFot() + ") should not exceed MUF (" + data.getMuf() + ")");
    }

    @Test
    void mockProvider_lastUpdated_isSetAfterFetch() throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();
        assertNull(provider.getLastUpdated(), "lastUpdated should be null before first fetch");

        provider.fetch();
        assertNotNull(provider.getLastUpdated(), "lastUpdated should be set after fetch");
    }

    @Test
    void mockProvider_isStale_beforeFetch() {
        MockPropagationProvider provider = new MockPropagationProvider();
        assertTrue(provider.isStale(java.time.Duration.ofMinutes(10)),
            "Provider should be stale before first fetch");
    }

    @Test
    void mockProvider_isNotStale_immediatelyAfterFetch() throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();
        provider.fetch();
        assertFalse(provider.isStale(java.time.Duration.ofMinutes(10)),
            "Provider should not be stale immediately after fetch");
    }

    @Test
    void propagationData_setBandCondition_overridesDefault() {
        PropagationData data = new PropagationData();
        data.setBandCondition("20m", BandCondition.EXCELLENT);
        assertEquals(BandCondition.EXCELLENT, data.getBandConditions().get("20m"),
            "setBandCondition should override the default");
    }

    @ParameterizedTest
    @ValueSource(strings = {"80m", "40m", "20m", "15m", "10m"})
    void mockProvider_majorBands_haveConditions(String band) throws Exception {
        MockPropagationProvider provider = new MockPropagationProvider();
        PropagationData data = provider.fetch();
        assertTrue(data.getBandConditions().containsKey(band),
            "Mock provider should have condition for " + band);
        assertNotNull(data.getBandConditions().get(band),
            "Condition for " + band + " should not be null");
    }
}
