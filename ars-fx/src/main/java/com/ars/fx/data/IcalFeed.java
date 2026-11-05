package com.ars.fx.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Just enough iCalendar (RFC 5545) to read an events feed: the VEVENTs' SUMMARY, DESCRIPTION, LOCATION, URL and
 * start / end. Repeat rules and alarms are ignored. All-day events (VALUE=DATE) have an exclusive end in iCalendar;
 * it is turned into an inclusive last day here. Times are taken as UTC (a trailing Z, or a TZID we do not interpret).
 */
public final class IcalFeed {
    private IcalFeed() {}

    public record Ev(String summary, String description, String location, String url,
                     LocalDate start, LocalDate end, LocalTime startTime, LocalTime endTime) {}

    public static List<Ev> parse(String ics) {
        List<Ev> out = new ArrayList<>();
        if (ics == null || ics.isBlank()) return out;
        String summary = null, desc = "", loc = "", url = "";
        LocalDate s = null, e = null; LocalTime st = null, et = null;
        boolean inEvent = false, allDay = false;
        for (String line : unfold(ics)) {
            if (line.equalsIgnoreCase("BEGIN:VEVENT")) {
                inEvent = true; summary = null; desc = ""; loc = ""; url = ""; s = null; e = null; st = null; et = null; allDay = false;
                continue;
            }
            if (line.equalsIgnoreCase("END:VEVENT")) {
                if (inEvent && summary != null && s != null) {
                    LocalDate last = e == null ? s : e;
                    if (e != null && allDay) last = e.minusDays(1);          // DTEND of a date-only event is the day after
                    if (last.isBefore(s)) last = s;
                    out.add(new Ev(summary, desc, loc, url, s, last, st, et));
                }
                inEvent = false;
                continue;
            }
            if (!inEvent) continue;
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String head = line.substring(0, colon), val = line.substring(colon + 1);
            String name = head.split(";", 2)[0].toUpperCase(Locale.ROOT);
            switch (name) {
                case "SUMMARY" -> summary = unescape(val);
                case "DESCRIPTION" -> desc = unescape(val);
                case "LOCATION" -> loc = unescape(val);
                case "URL" -> url = val.trim();
                case "DTSTART" -> { allDay = isDateOnly(head, val); s = date(val); st = allDay ? null : time(val); }
                case "DTEND" -> { e = date(val); et = isDateOnly(head, val) ? null : time(val); }
                default -> {}
            }
        }
        return out;
    }

    private static boolean isDateOnly(String head, String val) {
        return head.toUpperCase(Locale.ROOT).contains("VALUE=DATE") && !head.toUpperCase(Locale.ROOT).contains("VALUE=DATE-TIME")
                || val.trim().length() == 8;
    }
    private static LocalDate date(String v) {
        String t = v.trim();
        try { return t.length() >= 8 ? LocalDate.of(Integer.parseInt(t.substring(0, 4)), Integer.parseInt(t.substring(4, 6)), Integer.parseInt(t.substring(6, 8))) : null; }
        catch (RuntimeException ex) { return null; }
    }
    private static LocalTime time(String v) {
        String t = v.trim();
        int T = t.indexOf('T');
        if (T < 0 || t.length() < T + 5) return null;
        try { return LocalTime.of(Integer.parseInt(t.substring(T + 1, T + 3)), Integer.parseInt(t.substring(T + 3, T + 5))); }
        catch (RuntimeException ex) { return null; }
    }

    /** Join folded lines (a line that starts with a space or tab continues the one before). */
    static List<String> unfold(String ics) {
        List<String> out = new ArrayList<>();
        for (String raw : ics.split("\\r?\\n|\\r")) {
            if ((raw.startsWith(" ") || raw.startsWith("\t")) && !out.isEmpty()) out.set(out.size() - 1, out.get(out.size() - 1) + raw.substring(1));
            else out.add(raw);
        }
        return out;
    }

    static String unescape(String v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            if (c == '\\' && i + 1 < v.length()) {
                char n = v.charAt(++i);
                sb.append(n == 'n' || n == 'N' ? '\n' : n);
            } else sb.append(c);
        }
        return sb.toString().trim();
    }

    /** The UTC instant of a timed start, for sorting; midnight for an all-day event. */
    public static LocalDateTime when(Ev e) { return LocalDateTime.of(e.start(), e.startTime() == null ? LocalTime.MIDNIGHT : e.startTime()); }
}
