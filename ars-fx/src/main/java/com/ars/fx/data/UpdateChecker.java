package com.ars.fx.data;

import com.ars.fx.AppVersion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Looks for a newer published release of the suite on GitHub. Read-only and anonymous: one
 * {@code GET /releases/latest} (which excludes drafts and pre-releases), no account, nothing about the
 * user is sent. The result is only reported — applying an update is a separate, per-OS step.
 *
 * <p>Settings (HubConfig): {@code update.check} (default on), {@code update.skipped} (a tag the user
 * chose to ignore), {@code update.lastCheck} (epoch ms of the last attempt).
 */
public final class UpdateChecker {
    private UpdateChecker() {}

    public static final String LATEST_URL = "https://api.github.com/repos/skip17331/WM3J-ARS-Suite/releases/latest";

    /** One downloadable file attached to a release. */
    /** {@code digest} is GitHub's "sha256:<hex>" for the file ("" if the release predates digests). */
    public record Asset(String name, String url, long size, String digest) {}

    /** A published release: {@code tag} like {@code v1.12.0}, {@code url} its GitHub page, {@code notes} its markdown body. */
    public record Release(String tag, String name, String url, String notes, List<Asset> assets) {
        /** The version without the leading {@code v}. */
        public String version() { return tag.startsWith("v") || tag.startsWith("V") ? tag.substring(1) : tag; }
    }

    // ---- parsing / platform matching (pure; unit-tested) ------------------------------------------

    /** Parse a GitHub "release" JSON object. Throws {@link IllegalArgumentException} if it has no tag. */
    public static Release parse(String json) {
        JsonObject o;
        try { o = JsonParser.parseString(json).getAsJsonObject(); }
        catch (RuntimeException e) { throw new IllegalArgumentException("not a release object", e); }
        String tag = str(o, "tag_name");
        if (tag.isEmpty()) throw new IllegalArgumentException("release has no tag_name");
        List<Asset> assets = new ArrayList<>();
        JsonElement arr = o.get("assets");
        if (arr != null && arr.isJsonArray()) {
            for (JsonElement e : (JsonArray) arr) {
                if (!e.isJsonObject()) continue;
                JsonObject a = e.getAsJsonObject();
                String name = str(a, "name"), url = str(a, "browser_download_url");
                if (name.isEmpty() || url.isEmpty()) continue;
                long size = a.has("size") && a.get("size").isJsonPrimitive() ? a.get("size").getAsLong() : 0;
                assets.add(new Asset(name, url, size, str(a, "digest")));
            }
        }
        return new Release(tag, str(o, "name"), str(o, "html_url"), str(o, "body"), List.copyOf(assets));
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }

    /** The release-asset platform key for an OS / CPU, e.g. {@code windows-x86_64}, {@code macos-aarch64}, {@code linux-aarch64}. */
    public static String platformKey(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(java.util.Locale.ROOT);
        String ar = osArch == null ? "" : osArch.toLowerCase(java.util.Locale.ROOT);
        boolean arm = ar.contains("aarch64") || ar.contains("arm64");
        if (os.contains("win")) return "windows-x86_64";
        if (os.contains("mac") || os.contains("darwin")) return arm ? "macos-aarch64" : "macos-x86_64";
        return arm ? "linux-aarch64" : "linux-x86_64";
    }

    /** The jar for {@code platformKey} among a release's assets ({@code ars-fx-<ver>-<platform>.jar}), or null. */
    public static Asset assetFor(Release r, String platformKey) {
        String suffix = "-" + platformKey + ".jar";
        for (Asset a : r.assets()) if (a.name().startsWith("ars-fx-") && a.name().endsWith(suffix)) return a;
        return null;
    }

    /** The asset for the machine we are running on. */
    public static Asset assetForThisMachine(Release r) {
        return assetFor(r, platformKey(System.getProperty("os.name"), System.getProperty("os.arch")));
    }

    /** True if {@code r} should be offered: newer than this build and not a version the user chose to skip. */
    static boolean shouldOffer(Release r, String installed, String skippedTag) {
        return AppVersion.isNewer(r.tag(), installed) && !r.tag().equals(skippedTag);
    }

    // ---- network ----------------------------------------------------------------------------------

    /** Fetch the latest published release. */
    public static Release fetchLatest() throws IOException, InterruptedException {
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(LATEST_URL)).timeout(Duration.ofSeconds(20))
                .header("Accept", "application/vnd.github+json").header("User-Agent", "WM3J-ARS-Suite/" + AppVersion.current())
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException("GitHub returned HTTP " + r.statusCode());
        try { return parse(r.body()); }
        catch (IllegalArgumentException e) { throw new IOException("unexpected release data: " + e.getMessage(), e); }
    }

    // ---- state + schedule -------------------------------------------------------------------------

    private static final List<Consumer<Release>> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Release available;
    private static ScheduledExecutorService scheduler;
    private static final long DAY_MS = TimeUnit.HOURS.toMillis(24);

    /** Called (on a background thread) when a newer release is found. Also called at once if one is already known. */
    public static void addListener(Consumer<Release> l) {
        LISTENERS.add(l);
        Release r = available;
        if (r != null) l.accept(r);
    }

    /** The newer release found by the last check, or null. */
    public static Release available() { return available; }

    public static boolean enabled() { return HubConfig.getBool("update.check", true); }

    /** Check once, shortly after start-up, then daily. Does nothing if the user turned update checks off. Safe to call twice. */
    public static synchronized void start() {
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "update-check"); t.setDaemon(true); return t; });
        // Re-check at most once a day, even across restarts: launching the app often must not hit GitHub every time.
        long last = parseLong(HubConfig.get("update.lastCheck", "0"));
        long wait = Math.max(15_000, DAY_MS - (System.currentTimeMillis() - last));
        scheduler.scheduleWithFixedDelay(() -> { if (enabled()) { try { check(false); } catch (Exception ignored) { } } },
                wait, DAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Check now. {@code manual} (a "Check now" click) also offers a version the user previously skipped.
     * Returns the newer release if there is one, otherwise null. Throws if GitHub can't be reached.
     */
    public static Release check(boolean manual) throws IOException, InterruptedException {
        Release latest = fetchLatest();
        HubConfig.set("update.lastCheck", Long.toString(System.currentTimeMillis()));
        String skipped = manual ? "" : HubConfig.get("update.skipped", "");
        if (!shouldOffer(latest, AppVersion.current(), skipped)) { if (manual) available = null; return null; }
        available = latest;
        for (Consumer<Release> l : LISTENERS) l.accept(latest);
        return latest;
    }

    /** "Skip this version": stop offering this tag (a newer one is still offered). */
    public static void skip(Release r) {
        HubConfig.set("update.skipped", r.tag());
        if (available != null && available.tag().equals(r.tag())) available = null;
    }

    private static long parseLong(String s) { try { return Long.parseLong(s.trim()); } catch (RuntimeException e) { return 0; } }
}
