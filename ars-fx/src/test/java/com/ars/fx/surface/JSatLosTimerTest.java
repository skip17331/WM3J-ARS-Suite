package com.ars.fx.surface;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** J-Sat's LOS IN timer: counts down from AOS to LOS, blank outside the pass. */
class JSatLosTimerTest {
    private static final long AOS = 1_000_000, LOS = AOS + 10 * 60 + 51;     // a 10 min 51 s pass

    @Test
    void countsDownFromAosToLos() {
        assertEquals("10:51", JSatView.losTimer(AOS, LOS, AOS));
        assertEquals("07:42", JSatView.losTimer(AOS, LOS, LOS - 7 * 60 - 42));
        assertEquals("00:01", JSatView.losTimer(AOS, LOS, LOS - 1));
    }

    @Test
    void blankBeforeAosAfterLosOrWithoutAPass() {
        assertEquals("--:--", JSatView.losTimer(AOS, LOS, AOS - 1));
        assertEquals("--:--", JSatView.losTimer(AOS, LOS, LOS));
        assertEquals("--:--", JSatView.losTimer(-1, -1, AOS));
    }

    @Test
    void longPassesShowHours() {
        assertEquals("1:02:05", JSatView.losTimer(AOS, AOS + 3725, AOS));
    }
}
