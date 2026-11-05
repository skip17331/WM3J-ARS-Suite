package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.ContestClock.Phase;
import com.ars.fx.data.ContestClock.Window;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContestClockTest {
    static LocalDateTime t(String s) { return ContestClock.parse(s); }
    static final Window W = new Window(t("2026-11-28 00:00"), t("2026-11-30 00:00"));      // 48 hours

    @Test void parsingAcceptsTheUsualShapesAndRejectsNonsense() {
        assertEquals(LocalDateTime.of(2026, 11, 28, 0, 0), t("2026-11-28 00:00"));
        assertEquals(LocalDateTime.of(2026, 11, 28, 21, 30), t("2026-11-28T21:30:00"));
        assertNull(t(""));
        assertNull(t("tomorrow"));
        assertNull(t(null));
    }

    @Test void statusBeforeDuringAndAfter() {
        assertEquals(Phase.UNSET, ContestClock.status(null, t("2026-11-28 00:00")).phase());
        assertEquals(Phase.UNSET, ContestClock.status(new Window(null, null), t("2026-11-28 00:00")).phase());
        var before = ContestClock.status(W, t("2026-11-25 20:50"));
        assertEquals(Phase.BEFORE, before.phase());
        assertEquals("starts in 2d 03:10", before.text());
        var during = ContestClock.status(W, t("2026-11-29 12:28"));
        assertEquals(Phase.RUNNING, during.phase());
        assertEquals("11:32 left", during.text());
        assertEquals(Phase.AFTER, ContestClock.status(W, t("2026-11-30 00:00")).phase());
        assertEquals("ended", ContestClock.status(W, t("2026-12-01 00:00")).text());
    }

    @Test void anEndBeforeTheStartIsNotAWindow() {
        assertFalse(new Window(t("2026-11-30 00:00"), t("2026-11-28 00:00")).valid());
        assertEquals(Phase.UNSET, ContestClock.status(new Window(t("2026-11-30 00:00"), t("2026-11-28 00:00")), t("2026-11-29 00:00")).phase());
    }

    @Test void offTimeCountsOnlyGapsOfAtLeastTheMinimum() {
        // QSOs at 00:10, 00:20, 01:00 (a 40 min gap), 01:10, then nothing until 02:00 "now" (a 50 min gap)
        List<LocalDateTime> q = List.of(t("2026-11-28 00:10"), t("2026-11-28 00:20"), t("2026-11-28 01:00"), t("2026-11-28 01:10"));
        Duration off = ContestClock.offTime(q, W, t("2026-11-28 02:00"), Duration.ofMinutes(30));
        assertEquals(Duration.ofMinutes(40 + 50), off, "the 10-minute gaps are not off time, the 40 and 50 minute ones are");
    }

    @Test void theWaitForTheFirstQsoCountsToo() {
        Duration off = ContestClock.offTime(List.of(t("2026-11-28 01:00")), W, t("2026-11-28 01:05"), Duration.ofMinutes(30));
        assertEquals(Duration.ofMinutes(60), off);
    }

    @Test void nothingIsOffBeforeTheStartOrWithoutAWindow() {
        assertEquals(Duration.ZERO, ContestClock.offTime(List.of(), W, t("2026-11-27 12:00"), Duration.ofMinutes(30)));
        assertEquals(Duration.ZERO, ContestClock.offTime(List.of(), null, t("2026-11-28 12:00"), Duration.ofMinutes(30)));
    }

    @Test void afterTheEndOnlyTheWindowCounts() {
        Duration off = ContestClock.offTime(List.of(), W, t("2026-12-05 00:00"), Duration.ofMinutes(30));
        assertEquals(Duration.ofHours(48), off, "no QSOs at all: the whole contest was off");
    }
}
