package com.ars.fx.data;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * What is coming up: contests and DXpeditions. Each kind is one iCalendar feed:
 * <ul>
 *   <li><b>DXpeditions</b> - NG3K's Announced DX Operations list as an iCal feed (on by default).</li>
 *   <li><b>Contests</b> - an iCal link the operator supplies (Setup on the Info page). The WA7BNM Contest
 *       Calendar's terms allow its iCal feed for personal calendar use only and ask for written permission for automated
 *       access, so this app ships no built-in address for it.</li>
 * </ul>
 * Feeds are fetched politely: cached on disk for six hours, a manual refresh never goes to the network more than once
 * every fifteen minutes, one request per feed, and a failed fetch falls back to the cache.
 */
public final class UpcomingEvents {
    private UpcomingEvents() {}

    public enum Kind {
        CONTEST("Contests"), DXPEDITION("DXpeditions");
        public final String label;
        Kind(String label) { this.label = label; }
    }

    public record Event(Kind kind, String title, LocalDate start, LocalDate end, String time, String detail, String url, String location) {}

    /** Where a kind's events come from. */
    public record Source(Kind kind, String key, String url, String label, String creditUrl) {
        public boolean configured() { return url != null && !url.isBlank(); }
    }

    public record Loaded(Source source, List<Event> events, Instant fetched, boolean fromCache, String error) {}

    public static final String DX_DEFAULT = "https://www.danplanet.com/dxcal.ics";
    // ---- test hooks (the fetching and caching live in FeedFetcher) ------------------------------------
    static void setDirForTest(Path p) { FeedFetcher.setDirForTest(p); }
    static void setHttpForTest(Function<String, String> f) { FeedFetcher.setHttpForTest(f); }

    // ---- sources -------------------------------------------------------------------------------------
    public static List<Source> sources() {
        List<Source> out = new ArrayList<>();
        if (HubConfig.getBool("info.dx.enabled", true))
            out.add(new Source(Kind.DXPEDITION, "dx", HubConfig.get("info.dx.url", DX_DEFAULT).trim().isEmpty() ? DX_DEFAULT : HubConfig.get("info.dx.url", DX_DEFAULT).trim(),
                    "NG3K Announced DX Operations", "https://www.ng3k.com/Misc/adxo.html"));
        out.add(new Source(Kind.CONTEST, "contests", HubConfig.get("info.contests.url", "").trim(), "WA7BNM Contest Calendar", "https://www.contestcalendar.com/"));
        return out;
    }

    public static boolean validUrl(String u) { return FeedFetcher.validUrl(u); }

    // ---- loading -------------------------------------------------------------------------------------
    /** Events for one source: from the cache while it is fresh, else fetched. {@code force} is a manual refresh. */
    public static Loaded load(Source src, boolean force, Instant now) {
        if (!src.configured()) return new Loaded(src, List.of(), null, false, null);
        FeedFetcher.Fetched f = FeedFetcher.get(src.key(), src.url(), force, now);
        if (f.body() == null) return new Loaded(src, List.of(), null, false, f.error());
        return new Loaded(src, tag(src, f.body()), f.fetched(), f.fromCache(), f.error());
    }

    static List<Event> tag(Source src, String ics) {
        List<Event> out = new ArrayList<>();
        for (IcalFeed.Ev e : IcalFeed.parse(ics)) {
            String time = e.startTime() == null ? "" : String.format("%02d%02dZ", e.startTime().getHour(), e.startTime().getMinute())
                    + (e.endTime() == null ? "" : "–" + String.format("%02d%02dZ", e.endTime().getHour(), e.endTime().getMinute()));
            out.add(new Event(src.kind(), e.summary(), e.start(), e.end(), time, e.description(), e.url(), e.location()));
        }
        return out;
    }

    // ---- what the page shows ---------------------------------------------------------------------------
    /** Events that overlap {@code [today, today + days]} (all later ones when days <= 0), of the wanted kinds, matching the text, soonest first. */
    public static List<Event> upcoming(List<Event> all, LocalDate today, int days, Set<Kind> kinds, String text) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        LocalDate until = days > 0 ? today.plusDays(days) : LocalDate.MAX;
        List<Event> out = new ArrayList<>();
        for (Event e : all) {
            if (!kinds.contains(e.kind())) continue;
            if (e.end().isBefore(today) || e.start().isAfter(until)) continue;
            if (!q.isEmpty() && !(e.title() + " " + e.detail() + " " + e.location()).toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparing((Event e) -> e.start()).thenComparing(Event::title));
        return out;
    }

}
