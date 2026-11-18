package com.hamradio.modem;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Tests keep the modem's settings in their own node, on every OS (punch list 3-8). */
class ModemPrefsNodeTest {

    @Test void theTestRunUsesItsOwnNode() {
        // set by the surefire argLine in pom.xml; on Windows this is what keeps the tests out of the operator's registry settings
        assertEquals("ars-suite-tests/jdigi", System.getProperty("jdigi.prefs.node"));
        assertEquals("/ars-suite-tests/jdigi", ModemService.prefsNode().absolutePath());
    }

    @Test void withoutTheOverrideItIsThePackageNode() {
        String old = System.getProperty("jdigi.prefs.node");
        try {
            System.clearProperty("jdigi.prefs.node");
            assertEquals("/com/hamradio/modem", ModemService.prefsNode().absolutePath());
        } finally {
            if (old != null) System.setProperty("jdigi.prefs.node", old);
        }
    }
}
