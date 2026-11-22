package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class QsoGapsTest {
    static final LocalDateTime T = LocalDateTime.of(2026, 11, 22, 18, 0, 0);

    @Test
    void formatsMinutesAndHours() {
        assertEquals("0:05", QsoGaps.fmt(Duration.ofSeconds(5)));
        assertEquals("1:05", QsoGaps.fmt(Duration.ofSeconds(65)));
        assertEquals("1:02:05", QsoGaps.fmt(Duration.ofSeconds(3725)));
        assertEquals("-", QsoGaps.fmt(null));
    }

    @Test
    void sinceLastUsesTheNewestWhateverTheOrder() {
        List<LocalDateTime> t = Arrays.asList(T.minusMinutes(9), T.minusMinutes(2), null, T.minusMinutes(5));
        assertEquals(Duration.ofMinutes(2), QsoGaps.sinceLast(t, T));
        assertNull(QsoGaps.sinceLast(List.of(), T));
    }

    @Test
    void lastGapIsBetweenTheNewestTwo() {
        List<LocalDateTime> t = Arrays.asList(T.minusMinutes(9), T.minusMinutes(2), T.minusMinutes(5));
        assertEquals(Duration.ofMinutes(3), QsoGaps.lastGap(t));
        assertNull(QsoGaps.lastGap(List.of(T)));
    }
}
