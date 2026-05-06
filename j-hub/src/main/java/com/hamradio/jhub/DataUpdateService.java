package com.hamradio.jhub;

import com.hamradio.jhub.model.DataUpdateStatus;
import com.hamradio.jhub.model.DataUpdateStatus.FileStatus;
import com.hamradio.jhub.model.JHubConfig.AutoUpdateSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * DataUpdateService — keeps CTY.DAT (DXCC entity database) and MASTER.SCP
 * (Super Check Partial) current via scheduled HTTPS fetches.
 *
 * <p>Uses {@link HttpClient} from {@code java.net.http} — no third-party HTTP
 * deps, per the suite's "Java-only + cross-platform" architecture rule.
 *
 * <p>Each fetch:
 * <ol>
 *   <li>GETs the URL (no conditional headers — the validator below catches
 *       partial responses, and weekly bandwidth for ~1 MB is negligible).
 *   <li>Validates the response: 200 OK + body size ≥ configured minimum +
 *       basic format sanity (see {@link #validateCty} / {@link #validateScp}).
 *   <li>Atomically replaces the working file. The previous version is rotated
 *       to {@code <name>.bak} (single-cycle backup) before the swap so a bad
 *       day's data can be restored manually.
 *   <li>Broadcasts a {@link DataUpdateStatus} via MessageRouter.
 * </ol>
 *
 * <p>Failures (network, validation, I/O) log + leave the working file
 * untouched + retry on the next scheduled tick. Operators can force an
 * immediate retry via {@link #updateNow()}.
 */
public class DataUpdateService {

    private static final Logger log = LoggerFactory.getLogger(DataUpdateService.class);

    private static final DataUpdateService INSTANCE = new DataUpdateService();
    public  static DataUpdateService getInstance() { return INSTANCE; }
    private DataUpdateService() {}

    private MessageRouter router;

    private volatile AutoUpdateSection cfg = new AutoUpdateSection();
    private volatile DataUpdateStatus  status = new DataUpdateStatus();
    private volatile Path              dataDir;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "data-update");
                t.setDaemon(true);
                return t;
            });

    private ScheduledFuture<?> scheduledTask;

    /** HttpClient is stateless and thread-safe; keep one per service. */
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // ── Lifecycle ────────────────────────────────────────────────────

    public void setRouter(MessageRouter r) { this.router = r; }

    public void start(AutoUpdateSection s) {
        this.cfg = s != null ? s : new AutoUpdateSection();
        this.dataDir = resolveDataDir(cfg.dataDir);
        try { Files.createDirectories(dataDir); }
        catch (IOException e) {
            log.warn("Could not create data dir {}: {}", dataDir, e.getMessage());
        }

        // Pre-populate the status with whatever's already on disk so the
        // first /api/data response makes sense even before the first fetch.
        status.cty.url       = cfg.ctyUrl;
        status.cty.localPath = dataDir.resolve("cty.dat").toString();
        status.scp.url       = cfg.scpUrl;
        status.scp.localPath = dataDir.resolve("MASTER.SCP").toString();
        seedSizeFromDisk(status.cty);
        seedSizeFromDisk(status.scp);

        if (!cfg.enabled) {
            log.info("DataUpdateService: auto-update disabled in config");
            return;
        }

        // Initial fetch a few seconds after startup so we don't compete with
        // app launch I/O; subsequent ticks at the configured interval.
        long initialDelaySec = 30;
        long periodSec       = Math.max(1, cfg.intervalDays) * 86_400L;
        scheduledTask = scheduler.scheduleAtFixedRate(
                this::runCycle, initialDelaySec, periodSec, TimeUnit.SECONDS);
        log.info("DataUpdateService scheduled — first run in {}s, then every {} day(s)",
                initialDelaySec, cfg.intervalDays);
    }

    public void stop() {
        if (scheduledTask != null) { scheduledTask.cancel(false); scheduledTask = null; }
        log.info("DataUpdateService stopped");
    }

    public void restart(AutoUpdateSection s) {
        stop();
        start(s);
    }

    /** Force an immediate fetch on the worker thread. Returns immediately. */
    public void updateNow() {
        scheduler.execute(this::runCycle);
    }

    public DataUpdateStatus getStatus() { return status; }

    // ── Fetch cycle ──────────────────────────────────────────────────

    private void runCycle() {
        log.info("DataUpdateService: starting fetch cycle");
        boolean ctyOk = fetchAndStore(
                cfg.ctyUrl, dataDir.resolve("cty.dat"),
                cfg.minCtySize, DataUpdateService::validateCty, status.cty);
        boolean scpOk = fetchAndStore(
                cfg.scpUrl, dataDir.resolve("MASTER.SCP"),
                cfg.minScpSize, DataUpdateService::validateScp, status.scp);
        log.info("DataUpdateService: cycle done — cty={} scp={}", ctyOk, scpOk);
        if (router != null) router.publishDataUpdateStatus(status);
    }

    /**
     * Fetch a URL into {@code target}, applying size + format validation.
     * Returns true on success. On any failure: leaves the existing target
     * file untouched and records the error in {@code st}.
     */
    private boolean fetchAndStore(String url, Path target, int minSize,
                                  java.util.function.Predicate<byte[]> validator,
                                  FileStatus st) {
        st.url           = url;
        st.localPath     = target.toString();
        st.lastAttempted = Instant.now().toString();
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "ARS-Suite/1.0 (j-hub)")
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            st.lastHttpStatus = resp.statusCode();

            if (resp.statusCode() != 200) {
                st.lastError = "HTTP " + resp.statusCode();
                st.validated = false;
                log.warn("Fetch {} failed — HTTP {}", url, resp.statusCode());
                return false;
            }
            byte[] body = resp.body();
            if (body.length < minSize) {
                st.lastError = "size " + body.length + " < min " + minSize;
                st.validated = false;
                log.warn("Fetch {} failed validation — size {} bytes (need ≥ {})",
                        url, body.length, minSize);
                return false;
            }
            if (!validator.test(body)) {
                st.lastError = "format sanity check failed";
                st.validated = false;
                log.warn("Fetch {} failed format sanity", url);
                return false;
            }

            // Validation passed — rotate the existing file to .bak then write
            // the new bytes via a temp file + atomic move so a crash mid-write
            // can't leave a half-file behind.
            rotateBackup(target);
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, body);
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            st.sizeBytes   = body.length;
            st.lastUpdated = Instant.now().toString();
            st.lastError   = "";
            st.validated   = true;
            log.info("Updated {} ({} bytes)", target.getFileName(), body.length);
            return true;

        } catch (java.net.http.HttpConnectTimeoutException | java.net.UnknownHostException offline) {
            // Graceful offline handling per spec — log and let the next tick retry.
            st.lastError = "offline: " + offline.getClass().getSimpleName();
            st.validated = false;
            log.info("Fetch {} skipped — offline ({})", url, offline.getClass().getSimpleName());
            return false;
        } catch (Exception e) {
            st.lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            st.validated = false;
            log.warn("Fetch {} failed: {}", url, e.getMessage());
            return false;
        }
    }

    /** Rotate the existing target file to {@code <name>.bak} (single cycle). */
    private static void rotateBackup(Path target) {
        try {
            if (Files.exists(target)) {
                Path bak = target.resolveSibling(target.getFileName() + ".bak");
                Files.move(target, bak, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Backup rotation for {} failed: {}", target, e.getMessage());
        }
    }

    /** Populate FileStatus.sizeBytes from disk so the first GET /api/data is honest. */
    private static void seedSizeFromDisk(FileStatus st) {
        try {
            Path p = Paths.get(st.localPath);
            if (Files.exists(p)) st.sizeBytes = Files.size(p);
        } catch (IOException ignored) {}
    }

    // ── Validators ───────────────────────────────────────────────────
    // package-private so tests can exercise them directly without HTTP

    /** CTY.DAT lines start with a country name followed by a colon. The first
     *  non-blank line in a real CTY.DAT always contains a colon — easy guard
     *  against an HTML error page sneaking in. */
    static boolean validateCty(byte[] body) {
        String head = new String(body, 0, Math.min(body.length, 512), StandardCharsets.US_ASCII);
        for (String line : head.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            return line.contains(":");
        }
        return false;
    }

    /** MASTER.SCP starts with a comment block ("# ...") then one callsign per
     *  line (uppercase A-Z, 0-9, /). Look for at least one well-formed call in
     *  the first 1 KB. */
    static boolean validateScp(byte[] body) {
        String head = new String(body, 0, Math.min(body.length, 1024), StandardCharsets.US_ASCII);
        for (String line : head.split("\\r?\\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (t.matches("[A-Z0-9/]{3,}")) return true;
        }
        return false;
    }

    // ── Path helpers ─────────────────────────────────────────────────

    private static Path resolveDataDir(String configured) {
        if (configured != null && !configured.isBlank()) return Paths.get(configured);
        return Paths.get(System.getProperty("user.home"), ".j-hub", "data");
    }
}
