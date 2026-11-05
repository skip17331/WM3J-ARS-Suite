package com.ars.fx.data;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * When a contest runs (start and end, in UTC, typed by the operator because the dates change every year) and what that means
 * right now: time to the start, time left, and off time - the stretches without a QSO that count against the contest's
 * limit on how long you may be off the air.
 */
public final class ContestClock {
    private ContestClock() {}

    public enum Phase { UNSET, BEFORE, RUNNING, AFTER }
    public record Window(LocalDateTime start, LocalDateTime end) {
        public boolean valid() { return start != null && end != null && end.isAfter(start); }
    }
    public record Status(Phase phase, Duration toGo, String text) {}

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static String key(String contestId, String part) { return "contest.window." + contestId + "." + part; }

    public static Window window(String contestId) {
        return new Window(parse(HubConfig.get(key(contestId, "start"), "")), parse(HubConfig.get(key(contestId, "end"), "")));
    }
    public static void setWindow(String contestId, Window w) {
        HubConfig.set(key(contestId, "start"), w == null || w.start() == null ? "" : FMT.format(w.start()));
        HubConfig.set(key(contestId, "end"), w == null || w.end() == null ? "" : FMT.format(w.end()));
    }

    /** "2026-11-28 00:00" (a "T" or seconds are fine) or null. */
    public static LocalDateTime parse(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim().replace('T', ' ');
        try { return LocalDateTime.parse(t.length() == 16 ? t : t.substring(0, Math.min(16, t.length())), FMT); }
        catch (DateTimeParseException e) { return null; }
    }

    public static Status status(Window w, LocalDateTime now) {
        if (w == null || !w.valid()) return new Status(Phase.UNSET, Duration.ZERO, "");
        if (now.isBefore(w.start())) { Duration d = Duration.between(now, w.start()); return new Status(Phase.BEFORE, d, "starts in " + span(d)); }
        if (now.isBefore(w.end())) { Duration d = Duration.between(now, w.end()); return new Status(Phase.RUNNING, d, span(d) + " left"); }
        return new Status(Phase.AFTER, Duration.ZERO, "ended");
    }

    /** "2d 03:10", "11:32" (hours and minutes). */
    public static String span(Duration d) {
        long m = Math.max(0, d.toMinutes());
        long days = m / 1440, h = (m % 1440) / 60, mm = m % 60;
        return days > 0 ? String.format("%dd %02d:%02d", days, h, mm) : String.format("%d:%02d", h, mm);
    }

    /**
     * Off time so far: the sum of the gaps between QSOs (and from the start to the first, and from the last to {@code now} or the end)
     * that are at least {@code minGap} long, inside the window. Contests count an off period from 30 minutes (60 in some).
     */
    public static Duration offTime(List<LocalDateTime> qsoTimes, Window w, LocalDateTime now, Duration minGap) {
        if (w == null || !w.valid() || now.isBefore(w.start())) return Duration.ZERO;
        LocalDateTime upto = now.isBefore(w.end()) ? now : w.end();
        List<LocalDateTime> t = new ArrayList<>();
        for (LocalDateTime q : qsoTimes) if (q != null && !q.isBefore(w.start()) && !q.isAfter(upto)) t.add(q);
        Collections.sort(t);
        Duration off = Duration.ZERO;
        LocalDateTime prev = w.start();
        for (LocalDateTime q : t) { off = off.plus(gap(prev, q, minGap)); prev = q; }
        return off.plus(gap(prev, upto, minGap));
    }
    private static Duration gap(LocalDateTime a, LocalDateTime b, Duration minGap) {
        Duration d = Duration.between(a, b);
        return d.compareTo(minGap) >= 0 ? d : Duration.ZERO;
    }
}
