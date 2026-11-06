package com.jlog.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CabrilloExporterQtcDirectionTest {
    @Test void bothSpellingsOfTheDirectionAreRecognised() {
        assertTrue(CabrilloExporter.isReceived("RX"));
        assertTrue(CabrilloExporter.isReceived("RECEIVED"), "the QTC pane saves RECEIVED, not RX");
        assertTrue(CabrilloExporter.isReceived("received"));
        assertFalse(CabrilloExporter.isReceived("TX"));
        assertFalse(CabrilloExporter.isReceived("SENT"));
        assertFalse(CabrilloExporter.isReceived(null));
    }
}
