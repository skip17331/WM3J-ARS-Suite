package com.jlog.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A QSO or QTC with no logged frequency gets its own band's Cabrillo value, not 14000. */
class CabrilloFreqFallbackTest {
    @Test
    void aLoggedFrequencyIsKept() {
        assertEquals("7025", CabrilloExporter.cabFreq("7025", "40m"));
        assertEquals("14245", CabrilloExporter.cabFreq(" 14245 ", "20m"));
    }

    @Test
    void noFrequencyFallsBackToTheBand() {
        assertEquals("7000",  CabrilloExporter.cabFreq(null, "40m"));
        assertEquals("3500",  CabrilloExporter.cabFreq("", "80m"));
        assertEquals("21000", CabrilloExporter.cabFreq("  ", "15m"));
        assertEquals("1800",  CabrilloExporter.cabFreq(null, "160m"));
        assertEquals("144",   CabrilloExporter.cabFreq(null, "2m"));
        assertEquals("432",   CabrilloExporter.cabFreq(null, "70cm"));
    }

    @Test
    void everyHfBandHasItsOwnValueAndOnlyTwentyIs14000() {
        String[] hf = {"160m", "80m", "40m", "30m", "20m", "17m", "15m", "12m", "10m"};
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String b : hf) {
            String v = CabrilloExporter.cabBand(b);
            assertEquals(v.equals("14000"), b.equals("20m"), b + " -> " + v);
            seen.add(v);
        }
        assertEquals(hf.length, seen.size(), "no two bands share a value");
    }

    @Test
    void bandNamesAreForgivingAndUnknownOnesAreKept() {
        assertEquals("7000", CabrilloExporter.cabBand(" 40M "));
        assertEquals("??",   CabrilloExporter.cabBand(null));
        assertEquals("??",   CabrilloExporter.cabBand(""));
        assertEquals("60m",  CabrilloExporter.cabBand("60m"));
    }

    @Test void bandWithoutAFrequencyUsesTheCabrilloDesignator() {
        assertEquals("14000", CabrilloExporter.cabFreq(null, "20m"));
        assertEquals("14025", CabrilloExporter.cabFreq("14025", "20m"), "a logged frequency wins");
        String[][] vhf = {{"6m","50"},{"4m","70"},{"2m","144"},{"1.25m","222"},{"70cm","432"},{"33cm","902"},
                          {"23cm","1.2G"},{"13cm","2.3G"},{"9cm","3.4G"},{"6cm","5.7G"},{"3cm","10G"}};
        for (String[] b : vhf) assertEquals(b[1], CabrilloExporter.cabFreq("", b[0]), b[0]);
    }
}
