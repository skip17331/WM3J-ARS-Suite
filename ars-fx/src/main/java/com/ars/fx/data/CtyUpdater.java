package com.ars.fx.data;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Keeps the contest country files ({@code cty.dat}) current. The file changes a few times a month, and the copy in the jar
 * is only as new as the app release, so once a week the current file is fetched from country-files.com (a stable address:
 * {@value #URL}) into {@code ~/.j-hub/cty/}. A download is used only if it parses as a whole country file and is not much
 * smaller than what is already in use; anything else is discarded and the old copy stays. Nothing about the user is sent.
 * Switched off with {@code cty.update} (J-Hub ▸ Station ▸ Updates).
 */
public final class CtyUpdater {
    private CtyUpdater() {}

    public static final String URL = "https://www.country-files.com/cty/cty.dat";
    public static final Duration EVERY = Duration.ofDays(7);
    private static final int MAX_BYTES = 2_000_000;

    /** Start-up: refresh in the background when the saved copy is missing or a week old. Never blocks, never throws. */
    public static void start() {
        if (!HubConfig.getBool("cty.update", true)) return;
        Thread t = new Thread(() -> { try { refreshIfStale(Instant.now(), CtyUpdater::download); } catch (Throwable ignored) { } }, "cty-update");
        t.setDaemon(true);
        t.start();
    }

    /** True when a new copy was saved and is now in use. {@code fetch} returns the file's bytes (a test passes its own). */
    static boolean refreshIfStale(Instant now, Supplier<byte[]> fetch) throws IOException {
        Path f = CtyData.userFile();
        if (Files.isRegularFile(f)) {
            Instant saved = Files.getLastModifiedTime(f).toInstant();
            if (Duration.between(saved, now).compareTo(EVERY) < 0) return false;
        }
        byte[] body = fetch.get();
        if (body == null || body.length == 0 || body.length > MAX_BYTES) return false;
        CtyData fresh = CtyData.parse(new String(body, StandardCharsets.ISO_8859_1));
        CtyData inUse = CtyData.current();
        if (!CtyData.plausible(fresh) || fresh.size() < inUse.size() * 0.9) return false;      // truncated, wrong page, or a regression
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling("cty.dat.part");
        Files.write(tmp, body);
        try { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(tmp, f, StandardCopyOption.REPLACE_EXISTING); }
        CtyData.reload();
        return true;
    }

    private static byte[] download() {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest rq = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(40))
                    .header("User-Agent", "WM3J-ARS-Suite/" + version() + " (amateur radio logger)").GET().build();
            HttpResponse<byte[]> rs = c.send(rq, HttpResponse.BodyHandlers.ofByteArray());
            return rs.statusCode() == 200 ? rs.body() : null;
        } catch (IOException | InterruptedException | RuntimeException e) { return null; }
    }
    private static String version() { try { return com.ars.fx.AppVersion.current(); } catch (Throwable t) { return "dev"; } }
}
