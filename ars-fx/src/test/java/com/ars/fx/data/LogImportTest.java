package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LogImportTest {
    @Test void parsesHeaderlessAndHeaderedRecords() {
        String adif = "Some header text\n<ADIF_VER:5>3.1.4\n<EOH>\n"
                + "<CALL:5>W1AW <QSO_DATE:8>20260102 <TIME_ON:4>1530 <BAND:3>20m <MODE:3>SSB <SUBMODE:3>USB <FREQ:6>14.250 <NAME:2>Hi<EOR>\n"
                + "<call:4>K1AB<qso_date:8>20260103<time_on:6>010203<mode:3>CW<eor>\n";
        List<Map<String, String>> r = LogImport.parse(adif);
        assertEquals(2, r.size());
        assertEquals("W1AW", r.get(0).get("CALL"));
        assertEquals("USB", r.get(0).get("SUBMODE"));
        assertEquals("14.250", r.get(0).get("FREQ"));
        assertEquals("K1AB", r.get(1).get("CALL"));
        assertEquals("010203", r.get(1).get("TIME_ON"));
    }

    @Test void lengthSkipsEmbeddedAngleBrackets() {
        List<Map<String, String>> r = LogImport.parse("<EOH><CALL:4>N0AB<COMMENT:9>a <b> c d!<EOR>");
        assertEquals(1, r.size());
        assertEquals("a <b> c d", r.get(0).get("COMMENT"));
    }
}
