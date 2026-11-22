package com.ars.fx.shell;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IdTimerTest {
    @Test void countsDownFromTheLastId() {
        assertEquals(600, IdTimer.remaining(1_000_000, 1_000_000));
        assertEquals(540, IdTimer.remaining(1_060_000, 1_000_000));
        assertEquals(0, IdTimer.remaining(1_600_000, 1_000_000));
    }
    @Test void goesNegativeOnceOver() { assertEquals(-5, IdTimer.remaining(1_605_000, 1_000_000)); }
    @Test void aClockStepBackNeverAddsTime() { assertEquals(600, IdTimer.remaining(900_000, 1_000_000)); }
}
