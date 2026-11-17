package com.jlog.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CallsignRegionTest {

    @Test void normaliseKeepsTheDocumentedCases() {
        assertEquals("W1AW", CallsignRegion.normalise("W1AW/P"));
        assertEquals("W1AW", CallsignRegion.normalise("w1aw/7"));
        assertEquals("VE3", CallsignRegion.normalise("VE3/W1AW"));
        assertEquals("JA", CallsignRegion.normalise("W1AW/JA"));
        assertEquals("K1ABC", CallsignRegion.normalise("K1ABC"));
        assertEquals("", CallsignRegion.normalise(null));
    }

    @Test void aPrefixMayStartWithADigit() {
        // W1AW/5B used to stay W1AW (US): the right side was not seen as a prefix because it starts with a digit
        assertEquals("5B", CallsignRegion.normalise("W1AW/5B"));
        assertEquals("3D2", CallsignRegion.normalise("K1ABC/3D2"));
        assertEquals("9A", CallsignRegion.normalise("9A/DL1ABC"));
        assertEquals(CallsignRegion.Region.DX, CallsignRegion.classify("W1AW/5B"));
        assertEquals(CallsignRegion.Region.US, CallsignRegion.classify("W1AW/P"));
    }
}
