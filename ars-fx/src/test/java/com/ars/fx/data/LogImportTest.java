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

    @Test void submodesComeInAsTheModeOperatorsKnow() {
        assertEquals("FT4", LogImport.modeOf("MFSK", "FT4"), "WSJT-X's FT4 used to come in as MFSK");
        assertEquals("JS8", LogImport.modeOf("MFSK", "JS8"));
        assertEquals("USB", LogImport.modeOf("SSB", "USB"));
        assertEquals("PSK31", LogImport.modeOf("PSK", "PSK31"));
        assertEquals("DOMINOEX", LogImport.modeOf("DOMINO", "DOMINOEX"));
        assertEquals("FT8", LogImport.modeOf("FT8", null));
        assertEquals("SSB", LogImport.modeOf("SSB", ""));
        assertEquals("OLIVIA", LogImport.modeOf("OLIVIA", "OLIVIA 8/250"), "a mode that is not only a family keeps its name");
        assertEquals("FT4", LogImport.modeOf(null, "FT4"));
    }

    @Test void exportWritesValidAdifModesAndImportReadsThemBack() {
        String[][] cases = {{"FT4", "MFSK", "FT4"}, {"JS8", "MFSK", "JS8"}, {"Q65", "MFSK", "Q65"}, {"USB", "SSB", "USB"},
                {"PSK31", "PSK", "PSK31"}, {"DOMINOEX", "DOMINO", "DOMINOEX"}, {"FT8", "FT8", null}, {"CW", "CW", null}, {"RTTY", "RTTY", null}};
        for (String[] c : cases) {
            String[] m = LogExport.mode(c[0]);
            assertEquals(c[1], m[0], c[0] + " MODE");
            assertEquals(c[2], m[1], c[0] + " SUBMODE");
            assertEquals(c[0], LogImport.modeOf(m[0], m[1]), c[0] + " round trip");
        }
    }

    @Test void lengthSkipsEmbeddedAngleBrackets() {
        List<Map<String, String>> r = LogImport.parse("<EOH><CALL:4>N0AB<COMMENT:9>a <b> c d!<EOR>");
        assertEquals(1, r.size());
        assertEquals("a <b> c d", r.get(0).get("COMMENT"));
    }
}
