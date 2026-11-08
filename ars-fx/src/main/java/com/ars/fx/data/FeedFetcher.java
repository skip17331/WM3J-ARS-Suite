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
import java.util.Locale;
import java.util.function.Function;

/**
 * Polite fetching of web feeds for the Info page (calendars and news): one request per feed, kept on disk for six hours,
 * a manual refresh never goes to the network more than once every fifteen minutes, and a failed fetch falls back to the
 * saved copy (a source may ask for a longer keep time, e.g. WA7BNM's twelve hours). Only http(s) addresses are fetched, at most 5 MB, with a User-Agent that names the app.
 */
public final class FeedFetcher {
    private FeedFetcher() {}

    /** {@code body} is null when nothing could be loaded (see {@code error}); {@code error} can accompany a saved copy. */
    public record Fetched(String body, Instant fetched, boolean fromCache, String error) {}

    public static final Duration FRESH = Duration.ofHours(6);
    public static final Duration MIN_REFETCH = Duration.ofMinutes(15);
    private static final int MAX_BYTES = 5_000_000;

    private static Path dirOverride;
    private static Function<String, String> http;                    // url -> body, for tests
    static void setDirForTest(Path p) { dirOverride = p; }
    static void setHttpForTest(Function<String, String> f) { http = f; }
    static Path cacheDir() { return dirOverride != null ? dirOverride : Paths.get(System.getProperty("user.home"), ".j-hub", "info"); }

    /** Only http(s) feeds: a feed address is never a local file or another scheme. */
    public static boolean validUrl(String u) {
        if (u == null) return false;
        String t = u.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("https://") || t.startsWith("http://");
    }

    /** The feed's text: from the cache while it is fresh, else fetched. {@code key} names the cache file. */
    public static Fetched get(String key, String url, boolean force, Instant now) { return get(key, url, force, now, FRESH); }

    /** As above, but a copy younger than {@code keep} is used without asking again (for things that rarely change). */
    public static Fetched get(String key, String url, boolean force, Instant now, Duration keep) {
        if (!validUrl(url)) return new Fetched(null, null, false, "Not a web address (use https://...)");
        Path body = cacheDir().resolve(key + ".feed"), stamp = cacheDir().resolve(key + ".stamp");
        Instant cachedAt = readStamp(stamp, body, url);
        Duration limit = force ? MIN_REFETCH : keep;
        if (cachedAt != null && Duration.between(cachedAt, now).compareTo(limit) < 0)
            return new Fetched(read(body), cachedAt, true, null);
        try {
            String text = fetch(url);
            Files.createDirectories(cacheDir());
            Files.writeString(body, text, StandardCharsets.UTF_8);
            Files.writeString(stamp, now.toString() + "\n" + url, StandardCharsets.UTF_8);
            return new Fetched(text, now, false, null);
        } catch (Exception e) {
            String why = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            if (cachedAt != null) return new Fetched(read(body), cachedAt, true, "Could not refresh (" + why + ") - showing the saved copy");
            return new Fetched(null, null, false, "Could not load (" + why + ")");
        }
    }

    private static String fetch(String url) throws IOException, InterruptedException {
        if (http != null) return http.apply(url);
        // Some feed hosts are slow to answer (a half-megabyte feed behind a busy CDN): allow more time, and try a failed connect once more.
        try { return fetchOnce(url); }
        catch (java.net.ConnectException | java.net.http.HttpConnectTimeoutException e) { return fetchOnce(url); }
    }
    private static String fetchOnce(String url) throws IOException, InterruptedException {
        HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest rq = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(40))
                .header("User-Agent", "WM3J-ARS-Suite/" + version() + " (amateur radio logger)")
                .header("Accept", "application/rss+xml, application/atom+xml, text/calendar, application/xml, text/xml;q=0.9, */*;q=0.5").GET().build();
        HttpResponse<byte[]> rs = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
        if (rs.statusCode() != 200) throw new IOException("HTTP " + rs.statusCode());
        if (rs.body().length > MAX_BYTES) throw new IOException("feed too large");
        return new String(rs.body(), StandardCharsets.UTF_8);
    }
    private static String version() { try { return com.ars.fx.AppVersion.current(); } catch (Throwable t) { return "dev"; } }

    private static Instant readStamp(Path stamp, Path body, String url) {
        try {
            if (!Files.exists(stamp) || !Files.exists(body)) return null;
            String[] p = Files.readString(stamp, StandardCharsets.UTF_8).split("\\R", 2);
            if (p.length < 2 || !p[1].trim().equals(url)) return null;          // the feed address changed: the cache is not for it
            return Instant.parse(p[0].trim());
        } catch (Exception e) { return null; }
    }
    private static String read(Path p) { try { return Files.readString(p, StandardCharsets.UTF_8); } catch (IOException e) { return ""; } }
}
