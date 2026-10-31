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
    void dxExtractorIsUnchanged() {
        // the existing behaviour other code relies on
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("W3ABC K3XYZ -10"));
        assertEquals("W3ABC", CallsignParser.extractDxCallsign("CQ W3ABC FM19"));
    }
}
