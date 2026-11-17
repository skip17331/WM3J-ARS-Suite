package com.jlog.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CabrilloModeTest {

    @Test void cw() {
        for (String m : new String[]{"CW", "cw", "CW-R", "CWR"}) assertEquals("CW", CabrilloExporter.cabMode(m), m);
    }

    @Test void voice() {
        for (String m : new String[]{"SSB", "USB", "LSB", "AM", "FM", "PHONE", "DV"}) assertEquals("PH", CabrilloExporter.cabMode(m), m);
    }

    @Test void rtty() {
        for (String m : new String[]{"RTTY", "RTTY-R", "FSK", "FSK-R"}) assertEquals("RY", CabrilloExporter.cabMode(m), m);
    }

    @Test void everyOtherDataModeIsDgNotPhone() {
        // these used to fall through to PH
        for (String m : new String[]{"FT8", "FT4", "JS8", "PSK31", "PSK63", "OLIVIA", "MFSK16", "DOMINOEX", "Q65", "DIGI", "PKTUSB"})
            assertEquals("DG", CabrilloExporter.cabMode(m), m);
    }

    @Test void missing() {
        assertEquals("??", CabrilloExporter.cabMode(null));
        assertEquals("??", CabrilloExporter.cabMode(" "));
    }
}
