package com.ars.fx.data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/** The time between contacts, for the contest score bar: how long since the last QSO and how long the gap before it was. No JavaFX, so it can be tested. */
public final class QsoGaps {
    private QsoGaps() {}

    /** "1:05", or "1:02:05" from an hour; "-" when there is nothing to show. */
    public static String fmt(Duration d) {
        if (d == null || d.isNegative()) return "-";
        long s = d.getSeconds();
        return s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
    }

    /** Time since the newest of {@code times}, or null when there are none (nulls are ignored). */
    public static Duration sinceLast(List<LocalDateTime> times, LocalDateTime now) {
        LocalDateTime last = newest(times, null);
        return last == null ? null : Duration.between(last, now);
    }

    /** The gap between the newest two of {@code times}, or null with fewer than two. */
    public static Duration lastGap(List<LocalDateTime> times) {
        LocalDateTime a = newest(times, null);
        LocalDateTime b = a == null ? null : newest(times, a);
        return b == null ? null : Duration.between(b, a);
    }

    /** The newest time strictly before {@code below} (any when below is null). */
    private static LocalDateTime newest(List<LocalDateTime> times, LocalDateTime below) {
        LocalDateTime best = null;
        for (LocalDateTime t : times) if (t != null && (below == null || t.isBefore(below)) && (best == null || t.isAfter(best))) best = t;
        return best;
    }
}
