package com.ars.fx.surface;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Setup warns as an eQSL password with '#' is typed, not only after a failed download. */
class EqslHashNoteTest {
    @Test void aHashGetsTheNote() {
        String n = JHubSetupPages.eqslHashNote("abc#123");
        assertNotNull(n);
        assertTrue(n.contains("Get confirmations"));
    }

    @Test void otherPasswordsDoNot() {
        assertNull(JHubSetupPages.eqslHashNote("abc123!$"));
        assertNull(JHubSetupPages.eqslHashNote(""));
        assertNull(JHubSetupPages.eqslHashNote(null));
    }
}
