package com.ars.fx.data;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * Real solar disk image (NASA SDO/HMI visible-light continuum — the frame that
 * shows sunspots) for the J-Map "Solar &amp; space weather" drawer.
 *
 * <p>Warm-starts from the shared on-disk cache {@code ~/.j-map/cache/sun-hmiic.jpg}
 * (the same file the j-map module maintains), then refreshes from
 * {@code sdo.gsfc.nasa.gov} on a background thread when the cache is missing or
 * stale. Offline → the cached/synthetic image stands. Building a JavaFX
 * {@link Image} off the FX thread is fine; the UI swap happens via the listener
 * on the FX thread.
 */
public final class SunImageService {

    private static final SunImageService INSTANCE = new SunImageService();
    public static SunImageService getInstance() { return INSTANCE; }
    private SunImageService() {}

    /** 512×512 visible-light continuum from NASA SDO/HMI (shows sunspots). */
    private static final String IMAGE_URL = "https://sdo.gsfc.nasa.gov/assets/img/latest/latest_512_HMIIC.jpg";
    private static final Path CACHE = Paths.get(System.getProperty("user.home"), ".j-map", "cache", "sun-hmiic.jpg");
    private static final long MAX_AGE_MS = 30 * 60 * 1000;   // refresh if cache older than 30 min

    private volatile Image image;
    private volatile Runnable listener = () -> {};
    private volatile boolean started;

    public Image image() { return image; }
    public void setListener(Runnable r) { this.listener = r == null ? () -> {} : r; }

    /** Begin warm-start + background refresh (idempotent). */
    public synchronized void start() {
        if (started) return;
        started = true;
        Thread t = new Thread(this::load, "sun-image");
        t.setDaemon(true);
        t.start();
    }

    private void load() {
        // 1) warm-start from the shared on-disk cache
        try {
            if (Files.exists(CACHE) && Files.size(CACHE) > 1024) {
                Image cached = new Image(CACHE.toUri().toString(), false);
                if (!cached.isError()) { image = cached; fire(); }
            }
        } catch (Throwable ignored) {}

        // 2) refresh from NASA when missing/stale (offline → keep what we have)
        boolean stale = true;
        try { stale = !Files.exists(CACHE) || (System.currentTimeMillis() - Files.getLastModifiedTime(CACHE).toMillis()) > MAX_AGE_MS; }
        catch (Exception ignored) {}
        if (image != null && !stale) return;

        try {
            HttpClient http = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest req = HttpRequest.newBuilder(URI.create(IMAGE_URL))
                    .timeout(Duration.ofSeconds(12))
                    .header("User-Agent", "ARS-Suite/ars-fx").build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            byte[] b = resp.body();
            if (resp.statusCode() == 200 && b != null && b.length > 1024) {
                Image img = new Image(new ByteArrayInputStream(b), 0, 0, true, true);
                if (!img.isError()) {
                    // atomic cache write so a partial download can't poison warm-start
                    Files.createDirectories(CACHE.getParent());
                    Path tmp = CACHE.resolveSibling("sun-hmiic.jpg.tmp");
                    Files.write(tmp, b);
                    Files.move(tmp, CACHE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    image = img; fire();
                }
            }
        } catch (Throwable ignored) { /* offline / blocked → cached or synthetic stands */ }
    }

    private void fire() {
        Runnable l = listener;
        try { javafx.application.Platform.runLater(l); } catch (IllegalStateException noFx) { /* toolkit not up (tests) */ }
    }
}
