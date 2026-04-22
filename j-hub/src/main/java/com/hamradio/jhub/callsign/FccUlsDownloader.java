package com.hamradio.jhub.callsign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Full automated pipeline: download FCC ULS ZIP → extract dat files →
 * import into SQLite → run schema migrations + VACUUM/ANALYZE.
 *
 * Triggered by POST /api/callsign/db/download/fcc.
 * Progress is reported via GET /api/callsign/db/import/progress.
 *
 * Default source URL (publicly available, no auth required):
 *   https://data.fcc.gov/download/pub/uls/complete/l_amat.zip
 *
 * The URL is also configurable via callsignLookup.fccUlsUrl in j-hub.json
 * so you can point it at a mirror or a locally cached copy.
 */
public class FccUlsDownloader {

    private static final Logger log = LoggerFactory.getLogger(FccUlsDownloader.class);

    public static final String DEFAULT_URL =
        "https://data.fcc.gov/download/pub/uls/complete/l_amat.zip";

    public enum Phase { IDLE, DOWNLOADING, EXTRACTING, IMPORTING, MAINTAINING, DONE, FAILED }

    private volatile Phase   phase         = Phase.IDLE;
    private volatile String  statusMsg     = "Not started";
    private final AtomicLong bytesReceived = new AtomicLong(0);
    private final AtomicLong bytesTotal    = new AtomicLong(-1);
    private final AtomicBoolean running    = new AtomicBoolean(false);

    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    private static final FccUlsDownloader INSTANCE = new FccUlsDownloader();
    public static FccUlsDownloader getInstance() { return INSTANCE; }
    private FccUlsDownloader() {}

    public Phase   getPhase()         { return phase; }
    public String  getStatusMsg()     { return statusMsg; }
    public long    getBytesReceived() { return bytesReceived.get(); }
    public long    getBytesTotal()    { return bytesTotal.get(); }
    public boolean isRunning()        { return running.get(); }

    /**
     * Starts the full pipeline in a background thread.
     * Returns false immediately if the pipeline is already running.
     *
     * @param dbPath     path to the SQLite file to write (created if absent)
     * @param urlOverride optional URL override; null/blank uses DEFAULT_URL
     */
    public boolean start(String dbPath, String urlOverride) {
        if (!running.compareAndSet(false, true)) return false;
        String url = (urlOverride != null && !urlOverride.isBlank()) ? urlOverride : DEFAULT_URL;
        Thread t = new Thread(() -> doPipeline(dbPath, url), "jhub-fcc-download");
        t.setDaemon(true);
        t.start();
        return true;
    }

    // ── Pipeline ───────────────────────────────────────────────────────────────

    private void doPipeline(String dbPath, String url) {
        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("jhub-fcc-");

            // ── 1. Download ────────────────────────────────────────────────────
            phase = Phase.DOWNLOADING;
            bytesReceived.set(0);
            bytesTotal.set(-1);
            statusMsg = "Connecting to FCC…";
            log.info("FCC ULS download started from {}", url);
            Path zipFile = tempDir.resolve("l_amat.zip");
            download(url, zipFile);

            // ── 2. Extract ─────────────────────────────────────────────────────
            phase = Phase.EXTRACTING;
            statusMsg = "Extracting archive…";
            log.info("Extracting FCC ULS ZIP");
            extract(zipFile, tempDir);
            Files.deleteIfExists(zipFile);  // free disk space before import

            // ── 3. Import (FccUlsImporter runs maintenance itself at the end) ──
            phase = Phase.IMPORTING;
            statusMsg = "Starting database import…";
            FccUlsImporter importer = FccUlsImporter.getInstance();
            if (!importer.start(tempDir.toString(), dbPath))
                throw new IOException("An FCC import is already running — try again later");

            // Mirror the importer's progress messages while we wait
            while (importer.isRunning()) {
                statusMsg = importer.getStatusMsg();
                Thread.sleep(400);
            }
            if (importer.getStatus() == FccUlsImporter.Status.FAILED)
                throw new IOException("Import failed: " + importer.getStatusMsg());

            // ── Done ───────────────────────────────────────────────────────────
            phase = Phase.DONE;
            statusMsg = String.format("Done — %,d records imported and indexed",
                importer.getImported());
            log.info(statusMsg);

        } catch (Exception e) {
            phase = Phase.FAILED;
            statusMsg = "Failed: " + e.getMessage();
            log.error("FCC ULS pipeline failed", e);
        } finally {
            running.set(false);
            if (tempDir != null) deleteQuietly(tempDir);
        }
    }

    // ── Download ───────────────────────────────────────────────────────────────

    private void download(String url, Path dest) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Hub/1.0 WM3J-ARS")
            .timeout(Duration.ofMinutes(30))   // large file — FCC servers can be slow
            .GET().build();

        HttpResponse<InputStream> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200)
            throw new IOException("FCC server returned HTTP " + resp.statusCode());

        resp.headers().firstValueAsLong("content-length").ifPresent(bytesTotal::set);

        byte[] buf = new byte[65_536];
        try (InputStream  in  = resp.body();
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(dest))) {
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                long got   = bytesReceived.addAndGet(n);
                long total = bytesTotal.get();
                if (total > 0) {
                    statusMsg = String.format("Downloading… %.1f / %.1f MB (%.0f%%)",
                        got / 1_048_576.0, total / 1_048_576.0, got * 100.0 / total);
                } else {
                    statusMsg = String.format("Downloading… %.1f MB", got / 1_048_576.0);
                }
            }
        }
        log.info("Download complete: {} MB", bytesReceived.get() / 1_048_576);
    }

    // ── Extract ────────────────────────────────────────────────────────────────

    private void extract(Path zipFile, Path destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(Files.newInputStream(zipFile)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // FCC ZIP may have entries like "l_amat/HD.dat" or just "HD.dat"
                String name = Path.of(entry.getName()).getFileName().toString();
                if ("HD.dat".equals(name) || "EN.dat".equals(name) || "AM.dat".equals(name)) {
                    Files.copy(zis, destDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    statusMsg = "Extracted " + name;
                    log.debug("Extracted {}", name);
                }
                zis.closeEntry();
            }
        }
        // Verify we got what we need
        for (String required : new String[]{"HD.dat", "EN.dat"}) {
            if (!Files.exists(destDir.resolve(required)))
                throw new IOException(required + " not found in the ZIP — wrong file?");
        }
    }

    // ── Cleanup ────────────────────────────────────────────────────────────────

    private static void deleteQuietly(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
