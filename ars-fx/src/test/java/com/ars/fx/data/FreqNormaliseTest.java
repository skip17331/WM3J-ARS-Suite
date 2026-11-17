package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Frequencies as operators type them into the log, turned into the MHz the log and ADIF keep. */
class FreqNormaliseTest {

    private static void is(String expected, String typed) { assertEquals(expected, Bands.normaliseMhz(typed), typed); }

    @Test void megahertzIsKept() {
        is("14.074", "14.074");
        is("14.074", " 14.0740 ");
        is("1.84", "1.840");
        is("144.174", "144.174");
        is("1296.1", "1296.1");           // 23 cm in MHz
        is("10368.1", "10368.1");         // 3 cm in MHz: not mistaken for 10.368 MHz
    }

    @Test void kilohertzBecomesMegahertz() {
        // all of these used to be read as MHz: no band, and an ADIF FREQ that LoTW rejects
        is("14.025", "14025");
        is("7.0745", "7074.5");
        is("1.83", "1830");
        is("3.5", "3500");                // 80 m, not 3.5 GHz
        is("50.125", "50125");
        is("144.174", "144174");
    }

    @Test void decimalCommaAndSpotStyle() {
        is("14.025", "14,025");           // used to become 14,025 MHz
        is("7.074", "7,074");
        is("14.025", "14.025.0");
        is("14.0255", "14.025.5");
    }

    @Test void theBandFollows() {
        assertEquals("20m", Bands.ofMhz("14025"));
        assertEquals("20m", Bands.ofMhz("14,025"));
        assertEquals("6m", Bands.ofMhz("50125"));
        assertEquals("40m", Bands.ofMhz("7.074"));
        assertNull(Bands.ofMhz("abc"));
    }

    @Test void notAFrequencyIsLeftAlone() {
        is("abc", " abc ");
        is("", "  ");
        assertNull(Bands.normaliseMhz(null));
        is("99999", "99999");             // neither 99.999 GHz nor 99.999 MHz is a band: unchanged
    }

    @Test void adifExportWritesMegahertz() {
        JLogDb.QsoExport q = new JLogDb.QsoExport("K1ABC", "2026-11-17 12:00:00", "20m", "CW", "14025", 0, "599", "599",
                null, null, null, null, null, null, JLogDb.Refs.NONE);
        String adif = LogExport.toAdifRecord(q);
        org.junit.jupiter.api.Assertions.assertTrue(adif.contains("<FREQ:6>14.025"), adif);
    }
}
