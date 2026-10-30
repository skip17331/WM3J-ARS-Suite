package com.ars.fx.data;

import com.ars.fx.sat.TleSet;

import java.io.InputStream;
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
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Orbital elements (TLEs) for J-Sat: loaded from the cache {@code ~/.j-sat/tles.txt} (or the bundled snapshot
 * when there is none), and refreshed from Celestrak and AMSAT. A refresh merges into what is already held,
 * keeping the newest epoch per satellite, so satellites a source doesn't carry keep their older elements.
 */
public final class TleStore {
    private TleStore() {}

    public record Result(boolean ok, int count, String message) {}

    /** {label, url} — fetched in order; every source that answers is merged. */
    public static final String[][] SOURCES = {
            {"Celestrak amateur",  "https://celestrak.org/NORAD/elements/gp.php?GROUP=amateur&FORMAT=TLE"},
            {"Celestrak stations", "https://celestrak.org/NORAD/elements/gp.php?GROUP=stations&FORMAT=TLE"},
            {"AMSAT nasabare",     "https://amsat.org/tle/current/nasabare.txt"}};

    private static final String K_LAST = "sat.tle.lastFetchMs", K_AUTO = "sat.tle.autoRefresh";
    /** Picks Celestrak has already said it does not track (decayed / unknown) — not asked again until "Refresh now". */
    private static final java.util.Set<Integer> NOT_TRACKED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static boolean isNotTracked(int norad) { return NOT_TRACKED.contains(norad); }
    private static final long DAY_MS = 24L * 3600_000;

    private static volatile Map<Integer, TleSet> current;
    private static volatile boolean refreshing;
    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    public static Path cacheFile() { return Paths.get(System.getProperty("user.home"), ".j-sat", "tles.txt"); }

    public static void addListener(Runnable r) { if (r != null) listeners.add(r); }
    public static void removeListener(Runnable r) { listeners.remove(r); }

    public static Map<Integer, TleSet> current() {
        Map<Integer, TleSet> c = current;
        if (c == null) { c = load(); current = c; }
        return c;
    }

    /** Re-read the cache from disk (e.g. another process refreshed it). */
    public static void reload() { current = load(); }

    public static boolean refreshing() { return refreshing; }
    public static boolean autoRefresh() { return HubConfig.getBool(K_AUTO, true); }
    public static void setAutoRefresh(boolean on) { HubConfig.setBool(K_AUTO, on); }

    /** When the elements were last downloaded, or null if only the bundled snapshot is in use. */
    public static Instant lastFetch() {
        try { long ms = Long.parseLong(HubConfig.get(K_LAST, "0")); if (ms > 0) return Instant.ofEpochMilli(ms); }
        catch (Exception ignored) {}
        try { if (Files.isReadable(cacheFile())) return Files.getLastModifiedTime(cacheFile()).toInstant(); }
        catch (Exception ignored) {}
        return null;
    }

    /** Oldest / median element age in days across everything held. */
    public static double[] ageStats() {
        List<Double> ages = new ArrayList<>();
        for (TleSet t : current().values()) ages.add(t.ageInDays());
        if (ages.isEmpty()) return new double[]{0, 0};
        Collections.sort(ages);
        return new double[]{ages.get(ages.size() - 1), ages.get(ages.size() / 2)};
    }

    /** Kick off a background refresh when auto-refresh is on and the last download is over a day old. */
    public static void autoRefreshIfDue() {
        if (!autoRefresh() || refreshing) return;
        Instant last = lastFetch();
        if (last != null && System.currentTimeMillis() - last.toEpochMilli() < DAY_MS) return;
        refreshAsync(null);
    }

    public static void refreshAsync(Consumer<Result> done) {
        Thread t = new Thread(() -> { Result r = refreshNow(); if (done != null) done.accept(r); }, "tle-refresh");
        t.setDaemon(true); t.start();
    }

    /** Download and merge now (blocking — call off the FX thread). The cache is only rewritten when something was fetched. */
    public static synchronized Result refreshNow() {
        refreshing = true;
        NOT_TRACKED.clear();                 // an explicit refresh asks again
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            Map<Integer, TleSet> merged = new HashMap<>(current());
            int fetched = 0, newer = 0;
            List<String> notes = new ArrayList<>();
            for (String[] src : SOURCES) {
                try {
                    HttpRequest req = HttpRequest.newBuilder(URI.create(src[1])).timeout(Duration.ofSeconds(30))
                            .header("User-Agent", "WM3J-ARS-Suite").GET().build();
                    HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                    if (resp.statusCode() != 200) { notes.add(src[0] + ": HTTP " + resp.statusCode()); continue; }
                    Map<Integer, TleSet> got = parse(resp.body().lines().toList());
                    if (got.isEmpty()) { notes.add(src[0] + ": no elements in reply"); continue; }
                    int upd = 0;
                    for (Map.Entry<Integer, TleSet> e : got.entrySet()) {
                        TleSet old = merged.get(e.getKey());
                        if (old == null || e.getValue().epochToInstant().isAfter(old.epochToInstant())) { merged.put(e.getKey(), e.getValue()); upd++; }
                    }
                    fetched += got.size(); newer += upd;
                    notes.add(src[0] + ": " + got.size() + " (" + upd + " newer)");
                } catch (Exception e) {
                    notes.add(src[0] + ": " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
                }
            }
            // Picked satellites the group lists don't carry: ask Celestrak for each by NORAD id.
            int[] byId = fetchMissing(http, merged);
            if (byId[0] > 0) { fetched += byId[0]; newer += byId[0]; notes.add("by NORAD id: " + byId[0] + " found" + (byId[1] > 0 ? ", " + byId[1] + " not tracked" : "")); }
            else if (byId[1] > 0) notes.add(byId[1] + " selected not tracked by Celestrak (decayed or unknown)");
            if (fetched == 0) return new Result(false, 0, "Could not download elements — " + String.join("; ", notes));
            write(merged);
            current = merged;
            HubConfig.set(K_LAST, String.valueOf(System.currentTimeMillis()));
            for (Runnable r : listeners) { try { r.run(); } catch (Exception ignored) {} }
            return new Result(true, merged.size(), merged.size() + " satellites held, " + newer + " updated — " + String.join("; ", notes));
        } catch (Exception e) {
            return new Result(false, 0, "Refresh failed: " + e.getMessage());
        } finally { refreshing = false; }
    }

    /** Fetch just the picked satellites that have no elements (Celestrak single-satellite query). Blocking. */
    public static synchronized Result fetchMissingNow() {
        refreshing = true;
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
            Map<Integer, TleSet> merged = new HashMap<>(current());
            int[] r = fetchMissing(http, merged);
            if (r[0] > 0) {
                write(merged); current = merged;
                for (Runnable l : listeners) { try { l.run(); } catch (Exception ignored) {} }
            }
            return new Result(r[0] > 0, r[0], r[0] + " found, " + r[1] + " not tracked by Celestrak");
        } catch (Exception e) {
            return new Result(false, 0, "Lookup failed: " + e.getMessage());
        } finally { refreshing = false; }
    }

    public static void fetchMissingAsync(Consumer<Result> done) {
        Thread t = new Thread(() -> { Result r = fetchMissingNow(); if (done != null) done.accept(r); }, "tle-lookup");
        t.setDaemon(true); t.start();
    }

    /** @return {found, notTracked}; found elements are merged into {@code merged}. */
    private static int[] fetchMissing(HttpClient http, Map<Integer, TleSet> merged) {
        int found = 0, none = 0, tried = 0;
        for (int id : SatSelection.selected()) {
            if (id <= 0 || merged.containsKey(id) || NOT_TRACKED.contains(id)) continue;
            if (tried++ >= 30) break;                       // be polite to Celestrak
            try {
                HttpRequest req = HttpRequest.newBuilder(URI.create("https://celestrak.org/NORAD/elements/gp.php?CATNR=" + id + "&FORMAT=TLE"))
                        .timeout(Duration.ofSeconds(20)).header("User-Agent", "WM3J-ARS-Suite").GET().build();
                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                Map<Integer, TleSet> got = resp.statusCode() == 200 ? parse(resp.body().lines().toList()) : Map.of();
                if (got.containsKey(id)) { merged.put(id, got.get(id)); found++; }
                else { none++; if (resp.statusCode() == 200) NOT_TRACKED.add(id); }   // a clean "no data" answer, not a network error
            } catch (Exception e) { none++; }
        }
        return new int[]{found, none};
    }

    // ── parsing / persistence ────────────────────────────────────────────────
    private static Map<Integer, TleSet> load() {
        List<String> lines = List.of();
        try {
            if (Files.isReadable(cacheFile())) lines = Files.readAllLines(cacheFile(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        Map<Integer, TleSet> m = parse(lines);
        if (m.isEmpty()) {
            try (InputStream in = TleStore.class.getResourceAsStream("/com/ars/fx/sat/tles-fallback.txt")) {
                if (in != null) m = parse(new String(in.readAllBytes(), StandardCharsets.UTF_8).lines().toList());
            } catch (Exception ignored) {}
        }
        return m;
    }

    public static Map<Integer, TleSet> parse(List<String> lines) {
        Map<Integer, TleSet> map = new HashMap<>();
        int i = 0;
        while (i + 2 < lines.size()) {
            String name = lines.get(i).trim();
            String l1 = lines.get(i + 1).trim(), l2 = lines.get(i + 2).trim();
            if (l1.startsWith("1 ") && l2.startsWith("2 ")) {
                try { TleSet t = new TleSet(name, l1, l2); map.put(Integer.parseInt(t.noradId), t); } catch (Exception ignored) {}
                i += 3;
            } else { i += 1; }
        }
        return map;
    }

    private static void write(Map<Integer, TleSet> m) throws Exception {
        List<TleSet> sorted = new ArrayList<>(m.values());
        sorted.sort(Comparator.comparing(t -> t.name));
        StringBuilder sb = new StringBuilder();
        for (TleSet t : sorted) sb.append(t.name).append('\n').append(t.line1).append('\n').append(t.line2).append('\n');
        Path f = cacheFile();
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling("tles.txt.tmp");
        Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
        Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
