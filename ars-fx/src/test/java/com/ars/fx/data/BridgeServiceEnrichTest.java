package com.ars.fx.data;

import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxStatus;
import com.jlog.util.Maidenhead;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BridgeServiceEnrichTest {
    private static final double[] FN20 = Maidenhead.center("FN20");       // eastern US

    private static WsjtxDecode decode(String message) {
        WsjtxDecode d = new WsjtxDecode();
        d.setMessage(message);
        return d;
    }

    @Test
    void gridInTheMessageGivesCountryDistanceAndBearing() {
        WsjtxDecode d = decode("CQ DL1ABC JO31");
        BridgeService.enrich(d, null, FN20);

        assertEquals("DL1ABC", d.getCallsign());
        assertEquals("Germany", d.getCountry());
        assertEquals("EU", d.getContinent());
        assertTrue(d.getDxcc() > 0);
        assertEquals(6300, d.getDistanceKm(), 500);           // eastern US to the Rhineland
        assertEquals(d.getDistanceKm() / 1.609344, d.getDistanceMi(), 0.01);
        assertEquals(50, d.getBearing(), 15);                 // north-east
        double[] jo31 = Maidenhead.center("JO31");
        assertEquals(jo31[0], d.getLat(), 1e-9);
        assertEquals(jo31[1], d.getLon(), 1e-9);
    }

    @Test
    void withoutAGridTheMiddleOfTheCountryIsUsed() {
        WsjtxDecode d = decode("CQ DX W3ABC");
        BridgeService.enrich(d, null, Maidenhead.center("JO31"));

        assertEquals("United States", d.getCountry());
        assertEquals("NA", d.getContinent());
        DxccInfo.Info us = DxccInfo.of("W3ABC");
        assertEquals(us.lat(), d.getLat(), 1e-9);
        assertTrue(d.getDistanceKm() > 5000);
    }

    @Test
    void rr73IsAReplyNotAGrid() {
        WsjtxDecode d = decode("DL1ABC W3XYZ RR73");
        BridgeService.enrich(d, null, Maidenhead.center("JO31"));
        assertEquals(DxccInfo.of("W3XYZ").lat(), d.getLat(), 1e-9);      // the country centre, not the "RR73" square
    }

    @Test
    void countryAndDistanceDescribeTheTransmittingStation() {
        // "<called> <sender> <report>": ZS6AA is sending, N0CALL is being called
        WsjtxDecode d = decode("N0CALL ZS6AA -12");
        BridgeService.enrich(d, null, FN20);
        assertEquals("N0CALL", d.getCallsign());                           // the existing "DX call" meaning is unchanged
        assertEquals("South Africa", d.getCountry());
        assertEquals("AF", d.getContinent());
        assertTrue(d.getDistanceKm() > 11000);
    }

    @Test
    void noStationPositionLeavesDistanceAndBearingAtZero() {
        WsjtxDecode d = decode("CQ DL1ABC JO31");
        BridgeService.enrich(d, null, null);
        assertEquals("Germany", d.getCountry());
        assertEquals(0.0, d.getDistanceKm());
        assertEquals(0.0, d.getBearing());
    }

    @Test
    void bandModeAndFrequencyComeFromTheLatestStatus() {
        WsjtxStatus st = new WsjtxStatus();
        st.setDialFrequency(14_074_000L);
        st.setMode("FT8");
        WsjtxDecode d = decode("CQ DL1ABC JO31");
        BridgeService.enrich(d, st, FN20);
        assertEquals(14_074_000L, d.getFrequency());
        assertEquals("FT8", d.getMode());
        assertEquals("20m", d.getBand());
    }

    @Test
    void aMessageWithNoCallsignIsLeftAlone() {
        WsjtxDecode d = decode("73");
        BridgeService.enrich(d, null, FN20);
        assertNull(d.getCallsign());
        assertNull(d.getCountry());
        assertEquals(0.0, d.getDistanceKm());
    }
}
