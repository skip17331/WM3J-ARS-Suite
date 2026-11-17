package com.hamradio.jbridge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CallsignParserTest {

    @Test
    void senderOfACqIsTheCaller() {
        assertEquals("W3ABC", CallsignParser.extractSenderCallsign("CQ W3ABC FM19"));
        assertEquals("W3ABC", CallsignParser.extractSenderCallsign("CQ DX W3ABC FM19"));
        assertEquals("W3ABC", CallsignParser.extractSenderCallsign("CQ NA W3ABC FM19"));
    }

    @Test
    void senderOfADirectedMessageIsTheSecondCall() {
        // "<called> <sender> <report>": WSJT-X puts the station being called first.
        assertEquals("K3XYZ", CallsignParser.extractSenderCallsign("W3ABC K3XYZ -10"));
        assertEquals("K3XYZ", CallsignParser.extractSenderCallsign("W3ABC K3XYZ RR73"));
        assertEquals("K3XYZ", CallsignParser.extractSenderCallsign("W3ABC K3XYZ R-08"));
        assertEquals("ZS6AA", CallsignParser.extractSenderCallsign("N0CALL ZS6AA -12"));
    }

    @Test
    void tuAndDeMessagesAgreeWithTheDxExtractor() {
        assertEquals("W3ABC", CallsignParser.extractSenderCallsign("TU; K3XYZ W3ABC -08"));
        assertEquals("W3ABC", CallsignParser.extractSenderCallsign("DE W3ABC FM19"));
    }

    @Test
    void portableSuffixIsDroppedAndNothingFoundGivesNull() {
        assertEquals("K3XYZ", CallsignParser.extractSenderCallsign("W3ABC K3XYZ/P -10"));
        assertNull(CallsignParser.extractSenderCallsign("73"));
        assertNull(CallsignParser.extractSenderCallsign(""));
        assertNull(CallsignParser.extractSenderCallsign(null));
    }

    @Test
    void callsThatStartWithADigitAreFound() {
        // these all came back null: the pattern required a letter first (3Y0J, 5B4AIF, 9A1A, 2E0ABC) and one digit (A61BK)
        assertEquals("3Y0J", CallsignParser.extractDxCallsign("CQ 3Y0J JD15"));
        assertEquals("5B4AIF", CallsignParser.extractDxCallsign("CQ 5B4AIF KM64"));
        assertEquals("2E0ABC", CallsignParser.extractDxCallsign("CQ 2E0ABC IO91"));
        assertEquals("A61BK", CallsignParser.extractDxCallsign("CQ A61BK LL75"));
        assertEquals("9A1A", CallsignParser.extractSenderCallsign("W1AW 9A1A -10"), "it was credited to W1AW");
        assertEquals("4X1AB", CallsignParser.extractSenderCallsign("K1ABC 4X1AB R-05"));
    }

    @Test
    void ft8HashedCallsInBracketsCount() {
        assertEquals("K1ABC", CallsignParser.extractSenderCallsign("W3ABC <K1ABC/P> -12"));
        assertEquals("PJ4/K1ABC", CallsignParser.extractDxCallsign("CQ <PJ4/K1ABC>"));
        assertNull(CallsignParser.extractSenderCallsign("CQ <...>"), "an unresolved hash is no call");
    }

    @Test
    void theWiderPatternStillRejectsWhatIsNotACall() {
        for (String t : new String[]{"FN20", "JD15", "FN20AB", "-10", "R-08", "+05", "RR73", "73", "FT8", "POTA", "DX", "TEST", "12345"})
            org.junit.jupiter.api.Assertions.assertFalse(CallsignParser.isCallsign(t), t);
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("CQ POTA W3ABC FN20"));
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("CQ TEST W3ABC FN20"));
    }

    @Test
    void dxExtractorIsUnchanged() {
        // the existing behaviour other code relies on
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("W3ABC K3XYZ -10"));
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("CQ W3ABC FM19"));
    }
}
