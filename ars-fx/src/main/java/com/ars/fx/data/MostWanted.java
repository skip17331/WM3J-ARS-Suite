package com.ars.fx.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Club Log's DXCC Most Wanted ranking: how rare each entity is, worldwide, all bands and modes (1 = the most wanted).
 *
 * <p>Fetched with the operator's Club Log REST token ({@code GET https://clublog.org/api/v1/most-wanted}, "Authorization: Bearer
 * cl_..."; the reply maps place → DXCC entity number), kept in {@code ~/.j-hub/clublog/mostwanted.json} and refreshed at most once a
 * month, as Club Log asks ("Fetch it once a month"). A failed refresh keeps the copy already on disk. Without a token nothing is fetched
 * and {@link #rank} is 0 for every call. The token is a secret, so it lives with the upload logins ({@code upload.json}, owner-only).
 */
public final class MostWanted {
    private MostWanted() {}

    public static final String URL = "https://clublog.org/api/v1/most-wanted?mode=0&band=0&club=0";
    /** Refresh after this long (Club Log: once a month). */
    static final long MAX_AGE_MS = 30L * 24 * 3600 * 1000;

    private static volatile Map<Integer, Integer> rankByEntity = Map.of();
    private static volatile boolean loaded;

    public static String token() { return UploadConfig.field("clublog", "token").trim(); }
    public static void setToken(String t) { UploadConfig.setField("clublog", "token", t == null ? "" : t.trim()); }

    static Path file() { return Paths.get(System.getProperty("user.home"), ".j-hub", "clublog", "mostwanted.json"); }

    /** The reply {"1":"344","2":"123",...} (place → entity number, strings or numbers) as entity number → place. Bad entries are skipped. */
    static Map<Integer, Integer> parse(String json) {
        Map<Integer, Integer> out = new HashMap<>();
        JsonElement e = JsonParser.parseString(json);
        if (!e.isJsonObject()) return out;
        for (Map.Entry<String, JsonElement> m : e.getAsJsonObject().entrySet()) {
            try {
                int place = Integer.parseInt(m.getKey().trim());
                int entity = m.getValue().isJsonPrimitive() ? Integer.parseInt(m.getValue().getAsString().trim()) : -1;
                if (place > 0 && entity > 0) out.putIfAbsent(entity, place);
            } catch (NumberFormatException | UnsupportedOperationException | IllegalStateException ignored) { }
        }
        return out;
    }

    /** The Most Wanted place of this DXCC entity number, 0 when unknown. */
    public static int rankOfEntity(int dxcc) {
        ensureLoaded();
        Integer r = rankByEntity.get(dxcc);
        return r == null ? 0 : r;
    }

    /** The Most Wanted place of the entity this call is in, 0 when unknown or no list. */
    public static int rank(String call) {
        if (call == null || call.isBlank()) return 0;
        ensureLoaded();
        if (rankByEntity.isEmpty()) return 0;                     // no list: skip the DXCC lookup (called for every spot drawn)
        DxccInfo.Info i = DxccInfo.of(call);
        return i == null || i.dxcc() <= 0 ? 0 : rankOfEntity(i.dxcc());
    }

    /** How many entities the list holds (0 = no list yet). */
    public static int size() { ensureLoaded(); return rankByEntity.size(); }

    private static synchronized void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        try {
            Path f = file();
            if (Files.exists(f)) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                rankByEntity = Collections.unmodifiableMap(parse(o.get("reply").toString()));
            }
        } catch (Exception ignored) { /* no usable copy: no ranks */ }
    }

    /** The copy on disk is older than a month (or missing). */
    static boolean stale(long nowMs) {
        try {
            Path f = file();
            if (!Files.exists(f)) return true;
            long fetched = JsonParser.parseString(Files.readString(f)).getAsJsonObject().get("fetchedMs").getAsLong();
            return nowMs - fetched > MAX_AGE_MS;
        } catch (Exception e) { return true; }
    }

    /** At start-up: refresh in the background if a token is set and the copy is a month old. Never on the FX thread, never throws. */
    public static void init() {
        if (token().isEmpty() || !stale(System.currentTimeMillis())) return;
        Thread t = new Thread(() -> { try { refresh(); } catch (Exception ignored) { } }, "clublog-most-wanted");
        t.setDaemon(true); t.start();
    }

    /** Fetch now (the Setup button). Returns a short message for the screen. Keeps the old copy on any failure. */
    public static String refresh() {
        String tok = token();
        if (tok.isEmpty()) return "Set your Club Log token first (Club Log > Settings > API).";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(System.getProperty("clublog.mostwanted.url", URL)))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + tok)
                    .header("Accept", "application/json").header("User-Agent", "ARS-Suite").GET().build();
            HttpResponse<String> resp = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
                    .send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 401 || resp.statusCode() == 403) return "Club Log refused the token (HTTP " + resp.statusCode() + ").";
            if (resp.statusCode() == 429) return "Club Log says too many requests; try again tomorrow.";
            if (resp.statusCode() / 100 != 2) return "Club Log answered HTTP " + resp.statusCode() + ".";
            Map<Integer, Integer> m = parse(resp.body());
            if (m.size() < 100) return "Club Log's answer did not look like the Most Wanted list.";
            JsonObject o = new JsonObject();
            o.addProperty("fetchedMs", System.currentTimeMillis());
            o.add("reply", JsonParser.parseString(resp.body()));
            Path f = file();
            Files.createDirectories(f.getParent());
            Files.writeString(f, o.toString());
            rankByEntity = Collections.unmodifiableMap(m);
            loaded = true;
            return "Most Wanted list updated: " + m.size() + " entities.";
        } catch (Exception e) {
            return "Could not reach Club Log: " + e.getMessage();
        }
    }

    /** For tests: replace the ranks without touching disk. */
    static void setForTest(Map<Integer, Integer> m) { rankByEntity = m; loaded = true; }
}
