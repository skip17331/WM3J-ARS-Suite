package com.morsetrainer.decoder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MorseCodeTest {

    @Test
    void roundTripBasicLetters() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String pattern = MorseCode.forChar(c);
            assertNotNull(pattern, "Missing mapping for " + c);
            Character back = MorseCode.forPattern(pattern);
            assertNotNull(back);
            assertEquals(c, back);
        }
    }

    @Test
    void digitsMapped() {
        for (char c = '0'; c <= '9'; c++) {
            assertNotNull(MorseCode.forChar(c), "Missing digit " + c);
        }
    }

    @Test
    void unknownPatternReturnsNull() {
        assertNull(MorseCode.forPattern("........"));
    }

    @Test
    void unknownCharReturnsNull() {
        assertNull(MorseCode.forChar('§'));
    }

    @Test
    void caseInsensitive() {
        assertEquals(MorseCode.forChar('A'), MorseCode.forChar('a'));
    }
}
