package com.ars.fx.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Ham radio headlines for the Info page: a catalog of feeds the operator can switch on, plus feeds of their own. Only the
 * headline, source and date are kept and shown, each linking to the source's own page. The catalog was chosen by testing
 * the candidate feeds (alive, recent, on topic); feeds that block programs, or stopped posting, are left out.
 */
public final class NewsFeeds {
    private NewsFeeds() {}

    public enum Group {
        OFFICIAL("Societies and news"), BLOGS("Blogs and topics");
        public final String label;
        Group(String label) { this.label = label; }
    }

    public record Feed(String id, String name, String url, String site, Group group, boolean defaultOn) {}
    public record Headline(String source, String title, String link, Instant published) {}
    public record Result(List<Headline> headlines, Map<String, String> problems, Instant fetched) {}

    public static final List<Feed> CATALOG = List.of(
        new Feed("arrl",     "ARRL",                  "https://www.arrl.org/arrl.rss",                          "https://www.arrl.org/",           Group.OFFICIAL, true),
        new Feed("arnews",   "Amateur Radio Newsline","https://www.arnewsline.org/news?format=rss",              "https://www.arnewsline.org/",     Group.OFFICIAL, true),
        new Feed("amsat",    "AMSAT",                 "https://www.amsat.org/feed",                             "https://www.amsat.org/",          Group.OFFICIAL, true),
        new Feed("rsgb",     "RSGB",                  "https://rsgb.org/main/feed/",                            "https://rsgb.org/",               Group.OFFICIAL, true),
        new Feed("hamweekly","Ham Weekly",            "https://daily.hamweekly.com/atom.xml",                   "https://daily.hamweekly.com/",    Group.OFFICIAL, true),
        new Feed("amateurradio","AmateurRadio.com",   "https://feeds.feedburner.com/amateurradiocom",           "https://www.amateurradio.com/",   Group.OFFICIAL, true),
        new Feed("dxzone",   "The DXZone",            "https://feeds.feedburner.com/dxzone/xml",                "https://www.dxzone.com/",         Group.OFFICIAL, true),
        new Feed("wwff",     "WWFF",                  "https://wwff.co/feed",                                   "https://wwff.co/",                Group.BLOGS, false),
        new Feed("qrper",    "QRPer",                 "https://qrper.com/feed",                                 "https://qrper.com/",              Group.BLOGS, false),
        new Feed("kb6nu",    "KB6NU",                 "https://www.kb6nu.com/feed",                             "https://www.kb6nu.com/",          Group.BLOGS, false),
        new Feed("onallbands","OnAllBands (DX Engineering)","https://feeds.feedburner.com/OnAllBands",         "https://www.onallbands.com/",     Group.BLOGS, false),
        new Feed("swling",   "SWLing Post",           "https://swling.com/blog/feed",                           "https://swling.com/blog/",        Group.BLOGS, false),
        new Feed("amateurlogic","AmateurLogic.TV",    "https://www.amateurlogic.tv/blog/?feed=rss2",            "https://www.amateurlogic.tv/",    Group.BLOGS, false),
        new Feed("soldersmoke","SolderSmoke",         "https://feeds.feedburner.com/SoldersmokeDailyNews",      "https://soldersmoke.blogspot.com/", Group.BLOGS, false),
        new Feed("machamradio","Mac Ham Radio",       "https://machamradio.com/feed",                           "https://machamradio.com/",        Group.BLOGS, false),
        new Feed("hamradiodx","Ham Radio DX",         "https://hamradiodx.net/feed",                            "https://hamradiodx.net/",         Group.BLOGS, false));

    private static final String ON_KEY = "info.news.on";            // comma separated catalog ids; unset = the defaults
    private static final String CUSTOM_KEY = "info.news.custom";    // the operator's own feeds, one https:// address per line
    public static final int PER_FEED = 12, TOTAL = 120;

    // ---- what is switched on -----------------------------------------------------------------------
    public static Set<String> enabledIds() {
        String v = HubConfig.get(ON_KEY, null);
        Set<String> out = new LinkedHashSet<>();
        if (v == null) { for (Feed f : CATALOG) if (f.defaultOn()) out.add(f.id()); return out; }
        for (String s : v.split(",")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }
    public static void setEnabled(Set<String> ids) { HubConfig.set(ON_KEY, String.join(",", ids)); }
    public static List<String> customUrls() {
        List<String> out = new ArrayList<>();
        for (String l : HubConfig.get(CUSTOM_KEY, "").split("\\R")) if (FeedFetcher.validUrl(l)) out.add(l.trim());
        return out;
    }
    public static void setCustom(String text) { HubConfig.set(CUSTOM_KEY, text == null ? "" : text.trim()); }

    /** Every feed that is switched on: catalog feeds plus the operator's own (named by their host). */
    public static List<Feed> active() {
        List<Feed> out = new ArrayList<>();
        Set<String> on = enabledIds();
        for (Feed f : CATALOG) if (on.contains(f.id())) out.add(f);
        int n = 0;
        for (String u : customUrls()) {
            String host = u.replaceFirst("(?i)^https?://", "").replaceAll("[/?#].*$", "").replaceFirst("^www\\.", "");
            out.add(new Feed("custom" + (n++), host, u, u, Group.BLOGS, false));
        }
        return out;
    }

    // ---- loading ------------------------------------------------------------------------------------
    /** Load every active feed (four at a time, 75 seconds at most); a slow or broken feed is reported, never fatal. */
    public static Result load(List<Feed> feeds, boolean force, Instant now) {
        Map<String, String> problems = new LinkedHashMap<>();
        List<Headline> all = new ArrayList<>();
        Instant newest = null;
        ExecutorService pool = Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r, "news-feed"); t.setDaemon(true); return t; });
        try {
            List<Callable<Object[]>> jobs = new ArrayList<>();
            for (Feed f : feeds) jobs.add(() -> new Object[]{f, FeedFetcher.get("news-" + f.id(), f.url(), force, now)});
            List<Future<Object[]>> futures = new ArrayList<>();
            for (Callable<Object[]> j : jobs) futures.add(pool.submit(j));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(75);
            for (int i = 0; i < futures.size(); i++) {
                Feed f = feeds.get(i);
                try {
                    Object[] r = futures.get(i).get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                    FeedFetcher.Fetched got = (FeedFetcher.Fetched) r[1];
                    if (got.body() == null) { problems.put(f.name(), got.error()); continue; }
                    if (got.error() != null) problems.put(f.name(), got.error());
                    if (newest == null || (got.fetched() != null && got.fetched().isAfter(newest))) newest = got.fetched();
                    List<FeedParser.Item> items = FeedParser.parse(got.body());
                    if (items.isEmpty()) problems.put(f.name(), "No headlines found in the feed");
                    int n = 0;
                    for (FeedParser.Item it : items) {
                        if (n++ >= PER_FEED) break;
                        all.add(new Headline(f.name(), it.title(), it.link(), it.published()));
                    }
                } catch (Exception e) {
                    problems.put(f.name(), "slow to answer - it will appear on the next refresh");   // the fetch carries on and is cached
                }
            }
        } finally {
            pool.shutdown();          // let a slow feed finish in the background: its copy is saved for next time
        }
        return new Result(all, problems, newest);
    }

    // ---- what the tile shows ---------------------------------------------------------------------------
    /** Newest first; optionally one source only and/or a search word in the headline; at most {@code limit}. Undated items sort last. */
    public static List<Headline> latest(List<Headline> all, String source, String text, int limit) {
        String q = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<Headline> out = new ArrayList<>();
        for (Headline h : all) {
            if (source != null && !source.isBlank() && !h.source().equals(source)) continue;
            if (!q.isEmpty() && !(h.title() + " " + h.source()).toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(h);
        }
        out.sort(Comparator.comparing(Headline::published, Comparator.nullsLast(Comparator.reverseOrder())));
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    /** "3h", "2d", "just now". */
    public static String age(Instant published, Instant now) {
        if (published == null) return "";
        long m = java.time.Duration.between(published, now).toMinutes();
        if (m < 2) return "just now";
        if (m < 60) return m + "m";
        if (m < 60 * 24) return (m / 60) + "h";
        if (m < 60 * 24 * 60) return (m / (60 * 24)) + "d";
        return (m / (60 * 24 * 30)) + "mo";
    }
}
