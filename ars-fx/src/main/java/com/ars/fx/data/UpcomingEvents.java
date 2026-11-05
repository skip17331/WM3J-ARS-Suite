package com.ars.fx.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

/**
 * What is coming up: contests, DXpeditions, hamfests. Each kind is one iCalendar feed:
 * <ul>
 *   <li><b>DXpeditions</b> - NG3K's Announced DX Operations list as an iCal feed (on by default).</li>
 *   <li><b>Contests</b> and <b>hamfests</b> - an iCal link the operator supplies (Setup on the Info page). The WA7BNM Contest
 *       Calendar's terms allow its iCal feed for personal calendar use only and ask for written permission for automated
 *       access, so this app ships no built-in address for it.</li>
 * </ul>
 * Feeds are fetched politely: cached on disk for six hours, a manual refresh never goes to the network more than once
 * every fifteen minutes, one request per feed, and a failed fetch falls back to the cache.
 */
public final class UpcomingEvents {
    private UpcomingEvents() {}

    public enum Kind {
        CONTEST("Contests"), DXPEDITION("DXpeditions"), HAMFEST("Hamfests");
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
    public static final Duration FRESH = Duration.ofHours(6);
    public static final Duration MIN_REFETCH = Duration.ofMinutes(15);
    private static final int MAX_BYTES = 5_000_000;

    // ---- test hooks ---------------------------------------------------------------------------------
    private static Path dirOverride;
    private static Function<String, String> http;                    // url -> body, for tests
    static void setDirForTest(Path p) { dirOverride = p; }
    static void setHttpForTest(Function<String, String> f) { http = f; }
    static Path cacheDir() { return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-hub", "info"); }

    // ---- sources -------------------------------------------------------------------------------------
    public static List<Source> sources() {
        List<Source> out = new ArrayList<>();
        if (HubConfig.getBool("info.dx.enabled", true))
            out.add(new Source(Kind.DXPEDITION, "dx", HubConfig.get("info.dx.url", DX_DEFAULT).trim().isEmpty() ? DX_DEFAULT : HubConfig.get("info.dx.url", DX_DEFAULT).trim(),
                    "NG3K Announced DX Operations", "https://www.ng3k.com/Misc/adxo.html"));
        out.add(new Source(Kind.CONTEST, "contests", HubConfig.get("info.contests.url", "").trim(), "WA7BNM Contest Calendar", "https://www.contestcalendar.com/"));
        out.add(new Source(Kind.HAMFEST, "hamfests", HubConfig.get("info.hamfests.url", "").trim(), "Hamfest calendar", ""));
        return out;
    }

    /** Only http(s) feeds: a feed address is never a local file or another scheme. */
    public static boolean validUrl(String u) {
        if (u == null) return false;
        String t = u.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("https://") || t.startsWith("http://");
    }

    // ---- loading -------------------------------------------------------------------------------------
    /** Events for one source: from the cache while it is fresh, else fetched. {@code force} is a manual refresh. */
    public static Loaded load(Source src, boolean force, Instant now) {
        if (!src.configured()) return new Loaded(src, List.of(), null, false, null);
        if (!validUrl(src.url())) return new Loaded(src, List.of(), null, false, "Not a web address (use https://...)");
        Path body = cacheDir().resolve(src.key() + ".ics"), stamp = cacheDir().resolve(src.key() + ".stamp");
        Instant cachedAt = readStamp(stamp, body, src.url());
        Duration limit = force ? MIN_REFETCH : FRESH;
        if (cachedAt != null && Duration.between(cachedAt, now).compareTo(limit) < 0)
            return new Loaded(src, tag(src, read(body)), cachedAt, true, null);
        try {
            String text = fetch(src.url());
            Files.createDirectories(cacheDir());
            Files.writeString(body, text, StandardCharsets.UTF_8);
            Files.writeString(stamp, now.toString() + "\n" + src.url(), StandardCharsets.UTF_8);
            return new Loaded(src, tag(src, text), now, false, null);
        } catch (Exception e) {
            String why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (cachedAt != null) return new Loaded(src, tag(src, read(body)), cachedAt, true, "Could not refresh (" + why + ") - showing the saved copy");
            return new Loaded(src, List.of(), null, false, "Could not load (" + why + ")");
        }
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        if (http != null) return http.apply(url);
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
                .header("User-Agent", "WM3J-ARS-Suite/" + AppVersionHolder.version() + " (amateur radio logger)").GET().build();
        HttpResponse<byte[]> rs = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
        if (rs.statusCode() != 200) throw new IOException("HTTP " + rs.statusCode());
        if (rs.body().length > MAX_BYTES) throw new IOException("feed too large");
        return new String(rs.body(), StandardCharsets.UTF_8);
    }
    /** Keeps this class free of a hard dependency on the UI-side version class. */
    private static final class AppVersionHolder { static String version() { try { return com.ars.fx.AppVersion.current(); } catch (Throwable t) { return "dev"; } } }

    private static Instant readStamp(Path stamp, Path body, String url) {
        try {
            if (!Files.exists(stamp) || !Files.exists(body)) return null;
            String[] p = Files.readString(stamp, StandardCharsets.UTF_8).split("\\R", 2);
            if (p.length < 2 || !p[1].trim().equals(url)) return null;          // the feed address changed: the cache is not for it
            return Instant.parse(p[0].trim());
        } catch (Exception e) { return null; }
    }
    private static String read(Path p) { try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { return ""; } }

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
