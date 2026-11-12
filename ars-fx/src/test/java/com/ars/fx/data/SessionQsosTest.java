package com.ars.fx.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessionQsosTest {
    @BeforeEach @AfterEach void clean() { SessionQsos.setListener(null); SessionQsos.clear(); }

    @Test void aLoggedQsoIsPlacedFromItsGridWhenItHasOne() {
        SessionQsos.logged("ja1abc", "PM95", "20m", "SSB");
        assertEquals(1, SessionQsos.count());
        SessionQsos.Qso q = SessionQsos.all().get(0);
        assertEquals("JA1ABC", q.call());
        assertTrue(q.placed());
        assertEquals(35.5, q.lat(), 1.0);             // PM95 is in the Tokyo area
        assertEquals(139.0, q.lon(), 2.0);
    }

    @Test void withoutAGridItIsPlacedFromTheCallsignPrefix() {
        SessionQsos.logged("VK2XYZ", "", "15m", "CW");
        SessionQsos.Qso q = SessionQsos.all().get(0);
        assertTrue(q.placed(), "an Australian prefix should place");
        assertTrue(q.lat() < 0, "VK is in the southern hemisphere");
    }

    @Test void aCallThatCannotBePlacedIsCountedButNotDrawn() {
        SessionQsos.logged("Q", null, "20m", "SSB");           // too short / not a call
        SessionQsos.logged("W1AW", null, "20m", "SSB");
        assertEquals(2, SessionQsos.count());
        assertEquals(1, SessionQsos.placedCount());
    }

    @Test void blankCallsAreIgnoredAndClearEmptiesTheListAndTellsTheListener() {
        int[] told = {0};
        SessionQsos.setListener(() -> told[0]++);
        SessionQsos.logged("  ", null, "20m", "SSB");
        assertEquals(0, SessionQsos.count());
        assertEquals(0, told[0]);
        SessionQsos.logged("K1ABC", null, "40m", "CW");
        assertEquals(1, told[0]);
        SessionQsos.clear();
        assertEquals(0, SessionQsos.count());
        assertEquals(2, told[0]);
    }
}
