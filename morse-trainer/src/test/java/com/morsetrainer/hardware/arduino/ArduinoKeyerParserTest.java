package com.morsetrainer.hardware.arduino;

import com.morsetrainer.core.AppConfig;
import com.morsetrainer.decoder.KeyEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArduinoKeyerParserTest {

    @BeforeAll
    static void fixWpm() {
        // Pin WPM so ELEM duration math is deterministic: 1200/20 = 60ms dit.
        AppConfig.get().wpm = 20;
    }

    @Test
    void parsesTimestampedDown() {
        List<KeyEvent> evs = ArduinoKeyer.parse("DOWN 12345");
        assertEquals(1, evs.size());
        assertEquals(KeyEvent.Type.DOWN, evs.get(0).type());
        assertEquals(12345L, evs.get(0).timestampMillis());
    }

    @Test
    void parsesTimestampedUp() {
        List<KeyEvent> evs = ArduinoKeyer.parse("UP 99999");
        assertEquals(1, evs.size());
        assertEquals(KeyEvent.Type.UP, evs.get(0).type());
        assertEquals(99999L, evs.get(0).timestampMillis());
    }

    @Test
    void parsesSimpleTokens() {
        assertEquals(KeyEvent.Type.DOWN, ArduinoKeyer.parse("DITDOWN").get(0).type());
        assertEquals(KeyEvent.Type.DOWN, ArduinoKeyer.parse("DAHDOWN").get(0).type());
        assertEquals(KeyEvent.Type.UP,   ArduinoKeyer.parse("DITUP").get(0).type());
        assertEquals(KeyEvent.Type.UP,   ArduinoKeyer.parse("DAHUP").get(0).type());
    }

    @Test
    void parsesElemDitAsDownUpPair() {
        List<KeyEvent> evs = ArduinoKeyer.parse("ELEM DIT 1000");
        assertEquals(2, evs.size());
        assertEquals(KeyEvent.Type.DOWN, evs.get(0).type());
        assertEquals(1000L, evs.get(0).timestampMillis());
        assertEquals(KeyEvent.Type.UP, evs.get(1).type());
        // 1200/20 = 60ms dit
        assertEquals(1060L, evs.get(1).timestampMillis());
    }

    @Test
    void parsesElemDahAsLongerPair() {
        List<KeyEvent> evs = ArduinoKeyer.parse("ELEM DAH 2000");
        assertEquals(2, evs.size());
        assertEquals(2000L, evs.get(0).timestampMillis());
        // 3 * 60 = 180ms dah
        assertEquals(2180L, evs.get(1).timestampMillis());
    }

    @Test
    void elemMalformedReturnsEmpty() {
        assertTrue(ArduinoKeyer.parse("ELEM").isEmpty());
        assertTrue(ArduinoKeyer.parse("ELEM DIT").isEmpty());
        assertTrue(ArduinoKeyer.parse("ELEM DIT abc").isEmpty());
    }

    @Test
    void statusPacketsProduceNoEvents() {
        assertTrue(ArduinoKeyer.parse("READY 0 18").isEmpty());
        assertTrue(ArduinoKeyer.parse("PING 12345").isEmpty());
        assertTrue(ArduinoKeyer.parse("OK WPM 22").isEmpty());
        assertTrue(ArduinoKeyer.parse("ERR UNKNOWN FOO").isEmpty());
    }

    @Test
    void unknownAndEmptyReturnEmpty() {
        assertTrue(ArduinoKeyer.parse("WAT").isEmpty());
        assertTrue(ArduinoKeyer.parse("").isEmpty());
    }
}
