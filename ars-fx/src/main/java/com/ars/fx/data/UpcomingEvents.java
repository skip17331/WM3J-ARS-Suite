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
 *   <li><b>Contests</b> - the WA7BNM Contest Calendar's RSS feed, used with the owner's permission on the condition
 *       that it is credited (the tile names it and links to the site); fetched at most every 12 hours. The operator can
 *       replace it with an iCal link in Setup on the Info page.</li>
 * </ul>
 * Feeds are fetched politely: cached on disk for six hours (twelve for the contest calendar), a manual refresh never goes to the network more than once
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

    public static final String CONTEST_DEFAULT = "https://www.contestcalendar.com/calendar.rss";
    static final java.time.Duration CONTEST_KEEP = java.time.Duration.ofHours(12);
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
        out.add(new Source(Kind.CONTEST, "contests", contestUrl(), "WA7BNM Contest Calendar", "https://www.contestcalendar.com/"));
        return out;
    }

    private static String contestUrl() {
        String u = HubConfig.get("info.contests.url", "").trim();
        return u.isEmpty() ? CONTEST_DEFAULT : u;
    }

    public static boolean validUrl(String u) { return FeedFetcher.validUrl(u); }

    // ---- loading -------------------------------------------------------------------------------------
    /** Events for one source: from the cache while it is fresh, else fetched. {@code force} is a manual refresh. */
    public static Loaded load(Source src, boolean force, Instant now) {
        if (!src.configured()) return new Loaded(src, List.of(), null, false, null);
        FeedFetcher.Fetched f = FeedFetcher.get(src.key(), src.url(), force, now, src.kind() == Kind.CONTEST ? CONTEST_KEEP : FeedFetcher.FRESH);
        if (f.body() == null) return new Loaded(src, List.of(), null, false, f.error());
        return new Loaded(src, tag(src, f.body()), f.fetched(), f.fromCache(), f.error());
    }

    static List<Event> tag(Source src, String ics) {
        if (ics.contains("<rss")) return rss(src, ics, LocalDate.now(java.time.ZoneOffset.UTC));
        List<Event> out = new ArrayList<>();
        for (IcalFeed.Ev e : IcalFeed.parse(ics)) {
            String time = e.startTime() == null ? "" : String.format("%02d%02dZ", e.startTime().getHour(), e.startTime().getMinute())
                    + (e.endTime() == null ? "" : "–" + String.format("%02d%02dZ", e.endTime().getHour(), e.endTime().getMinute()));
            out.add(new Event(src.kind(), e.summary(), e.start(), e.end(), time, e.description(), e.url(), e.location()));
        }
        return out;
    }

    // ---- WA7BNM RSS: <description> is "0000Z, Nov 14 to 2359Z, Nov 15", "0100Z-0300Z, Nov 9" or several of those joined by " and " ----
    private static final java.util.regex.Pattern ITEM = java.util.regex.Pattern.compile("<item>(.*?)</item>", java.util.regex.Pattern.DOTALL);
    private static final java.util.regex.Pattern STAMP = java.util.regex.Pattern.compile("(\\d{4})Z?(?:-(\\d{4})Z)?,\\s*([A-Z][a-z]{2})\\s+(\\d{1,2})");
    private static final java.util.regex.Pattern DAY = java.util.regex.Pattern.compile("(\\d{4})Z,\\s*([A-Z][a-z]{2})\\s+(\\d{1,2})");

    static List<Event> rss(Source src, String xml, LocalDate today) {
        List<Event> out = new ArrayList<>();
        java.util.regex.Matcher im = ITEM.matcher(xml);
        while (im.find()) {
            String b = im.group(1);
            String title = FeedParser.clean(tagText(b, "title")), link = FeedParser.clean(tagText(b, "link")), d = FeedParser.clean(tagText(b, "description"));
            if (title.isEmpty()) continue;
            LocalDate start = null, end = null; String time = "";
            for (String part : d.split("\\s+and\\s+")) {
                LocalDate s0 = null, e0 = null; String t0 = "";
                if (part.contains(" to ")) {
                    String[] ab = part.split("\\s+to\\s+", 2);
                    java.util.regex.Matcher a = DAY.matcher(ab[0]), z = DAY.matcher(ab[1]);
                    if (a.find() && z.find()) {
                        s0 = day(a.group(2), a.group(3), today); e0 = day(z.group(2), z.group(3), today);
                        t0 = a.group(1) + "Z–" + z.group(1) + "Z";
                    }
                } else {
                    java.util.regex.Matcher m = STAMP.matcher(part);
                    if (m.find()) {
                        s0 = e0 = day(m.group(3), m.group(4), today);
                        t0 = m.group(1) + "Z" + (m.group(2) == null ? "" : "–" + m.group(2) + "Z");
                    }
                }
                if (s0 == null) continue;
                if (e0.isBefore(s0)) e0 = e0.plusYears(1);
                if (start == null) { start = s0; time = t0; }
                end = e0;
            }
            if (start == null) continue;
            out.add(new Event(src.kind(), title, start, end, time, "", FeedParser.webLink(link) ? link : "", ""));
        }
        return out;
    }

    private static String tagText(String block, String tag) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", java.util.regex.Pattern.DOTALL).matcher(block);
        return m.find() ? m.group(1) : "";
    }

    /** The feed gives no year: take the nearest one to today (a December feed lists January contests). */
    private static LocalDate day(String mon, String dom, LocalDate today) {
        int m = java.time.Month.from(java.time.format.DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH).parse(mon)).getValue();
        LocalDate d = LocalDate.of(today.getYear(), m, Integer.parseInt(dom));
        if (d.isBefore(today.minusMonths(6))) d = d.plusYears(1);
        else if (d.isAfter(today.plusMonths(6))) d = d.minusYears(1);
        return d;
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
