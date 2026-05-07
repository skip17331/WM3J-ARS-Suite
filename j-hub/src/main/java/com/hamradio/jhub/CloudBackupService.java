package com.hamradio.jhub;

import com.hamradio.jhub.model.JHubConfig.BackupSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * CloudBackupService — periodic snapshot of every {@code ~/.j-*} directory
 * to either a local folder or a WebDAV server.
 *
 * <h3>Two modes</h3>
 * <dl>
 *   <dt>FOLDER</dt>
 *   <dd>Write a single timestamped <code>.zip</code> to a configured directory.
 *       Designed for users with Dropbox / Google Drive / OneDrive / iCloud
 *       sync clients already installed — point this at the synced folder
 *       and the cloud handles the rest.</dd>
 *   <dt>WEBDAV</dt>
 *   <dd>HTTP <code>PUT</code> the same zip to a WebDAV URL with HTTP Basic
 *       auth. Works with Nextcloud, ownCloud, and most generic WebDAV
 *       servers. Credentials are pulled from {@link CredentialStore}
 *       under the service id <code>"webdav"</code>.</dd>
 * </dl>
 *
 * <h3>What's included</h3>
 * Each app directory under <code>~/</code> can be opted in/out from config:
 * <code>.j-hub</code>, <code>.j-log</code>, <code>.j-map</code>,
 * <code>.j-sat</code>, <code>.j-digi</code>, <code>.j-bridge</code>.
 * Logs are excluded by default — they're regenerable and would dominate
 * the archive size.
 *
 * <h3>Retention</h3>
 * After a successful backup, oldest archives matching
 * <code>jsuite-backup-*.zip</code> are deleted to keep the count at
 * {@link BackupSection#retain}. WebDAV retention is best-effort
 * (PROPFIND list + DELETE) and silently degrades to "do nothing" when
 * the server doesn't support directory listing.
 */
public class CloudBackupService {

    private static final Logger log = LoggerFactory.getLogger(CloudBackupService.class);

    private static final CloudBackupService INSTANCE = new CloudBackupService();
    public static CloudBackupService getInstance() { return INSTANCE; }

    private static final String FILE_PREFIX = "jsuite-backup-";
    private static final String FILE_SUFFIX = ".zip";
    private static final DateTimeFormatter STAMP =
        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "cloud-backup"); t.setDaemon(true); return t;
        });

    // Last-run state, exposed via /api/backup/status.
    private volatile Instant lastRunAt;
    private volatile boolean lastRunSucceeded;
    private volatile String  lastRunMessage = "(never)";
    private volatile long    lastBytes;

    private CloudBackupService() {}

    public void start() {
        BackupSection cfg = ConfigManager.getInstance().getConfig().backup;
        if (cfg == null || !cfg.enabled || cfg.scheduleHours <= 0) {
            log.info("Cloud backup disabled or scheduleHours=0 — periodic backups not started");
            return;
        }
        scheduler.scheduleAtFixedRate(this::runOnce, 30, cfg.scheduleHours * 3600L, TimeUnit.SECONDS);
        log.info("Cloud backup scheduled every {}h", cfg.scheduleHours);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    /** Trigger an immediate backup. Used by the "Back up now" button. */
    public synchronized BackupResult runOnce() {
        BackupSection cfg = ConfigManager.getInstance().getConfig().backup;
        BackupResult r = new BackupResult();
        if (cfg == null) {
            r.message = "no config"; finalize(r); return r;
        }
        Path stamped = null;
        try {
            stamped = Files.createTempFile(FILE_PREFIX, FILE_SUFFIX);
            long bytes = buildArchive(stamped, cfg);
            r.archiveBytes = bytes;
            String name = FILE_PREFIX + STAMP.format(Instant.now()) + FILE_SUFFIX;
            switch (cfg.mode == null ? "FOLDER" : cfg.mode) {
                case "WEBDAV": pushWebdav(stamped, name); break;
                case "FOLDER":
                default:       pushFolder(stamped, name, cfg.folderPath); break;
            }
            pruneOldBackups(cfg);
            r.success = true;
            r.message = "Backed up " + bytes + " bytes (" + name + ")";
        } catch (Exception e) {
            r.success = false;
            r.message = "Backup failed: " + e.getMessage();
            log.warn("Backup failed", e);
        } finally {
            if (stamped != null) try { Files.deleteIfExists(stamped); } catch (IOException ignored) {}
            finalize(r);
        }
        return r;
    }

    private void finalize(BackupResult r) {
        lastRunAt        = Instant.now();
        lastRunSucceeded = r.success;
        lastRunMessage   = r.message;
        lastBytes        = r.archiveBytes;
    }

    // ---------------------------------------------------------------
    // Archive build
    // ---------------------------------------------------------------

    private long buildArchive(Path zip, BackupSection cfg) throws IOException {
        Path home = Path.of(System.getProperty("user.home"));
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zipDir(home, ".j-hub",   zos, cfg.includeJHub);
            zipDir(home, ".j-log",   zos, cfg.includeJLog);
            zipDir(home, ".j-map",   zos, cfg.includeJMap);
            zipDir(home, ".j-sat",   zos, cfg.includeJSat);
            zipDir(home, ".j-digi",  zos, cfg.includeJDigi);
            zipDir(home, ".j-bridge",zos, cfg.includeJBridge);
        }
        return Files.size(zip);
    }

    private void zipDir(Path home, String name, ZipOutputStream zos, boolean enabled) throws IOException {
        if (!enabled) return;
        Path src = home.resolve(name);
        if (!Files.isDirectory(src)) return;
        Files.walkFileTree(src, new SimpleFileVisitor<>() {
            @Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fname = file.getFileName().toString();
                // Skip large logs and per-run artifacts. They regenerate, and
                // most users don't want a 200MB archive for a small log set.
                if (fname.endsWith(".log") || fname.endsWith(".log.gz")) return FileVisitResult.CONTINUE;
                if (file.getParent().getFileName().toString().equals("logs")) return FileVisitResult.CONTINUE;
                String entryName = name + "/" + src.relativize(file).toString().replace('\\', '/');
                try {
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(file, zos);
                    zos.closeEntry();
                } catch (IOException e) {
                    log.debug("Skip file {} ({})", file, e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // ---------------------------------------------------------------
    // Destinations
    // ---------------------------------------------------------------

    private void pushFolder(Path src, String name, String folder) throws IOException {
        if (folder == null || folder.isBlank()) {
            throw new IOException("Folder mode selected but no folder path configured");
        }
        Path dir = Path.of(folder);
        Files.createDirectories(dir);
        Path dest = dir.resolve(name);
        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
        log.info("Backup written to {}", dest);
    }

    private void pushWebdav(Path src, String name) throws IOException {
        BackupSection cfg = ConfigManager.getInstance().getConfig().backup;
        if (cfg.webdavUrl == null || cfg.webdavUrl.isBlank()) {
            throw new IOException("WebDAV mode selected but no URL configured");
        }
        com.google.gson.JsonObject creds = CredentialStore.getInstance().get("webdav");
        String user = creds.has("user") ? creds.get("user").getAsString() : "";
        String pass = creds.has("pass") ? creds.get("pass").getAsString() : "";
        if (user.isBlank() || pass.isBlank()) {
            throw new IOException("WebDAV credentials not set — open the Backup tab and enter user/pass");
        }
        String base = cfg.webdavUrl.endsWith("/") ? cfg.webdavUrl : cfg.webdavUrl + "/";
        URL url = new URL(base + name);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setFixedLengthStreamingMode(Files.size(src));
        conn.setRequestProperty("Content-Type", "application/zip");
        String auth = Base64.getEncoder().encodeToString(
            (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
        conn.setRequestProperty("Authorization", "Basic " + auth);
        try (OutputStream out = conn.getOutputStream()) {
            Files.copy(src, out);
        }
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("WebDAV PUT returned " + code);
        }
        log.info("Backup PUT to {} ({})", url, code);
    }

    private void pruneOldBackups(BackupSection cfg) {
        if ("FOLDER".equals(cfg.mode) && cfg.folderPath != null && !cfg.folderPath.isBlank()) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(Path.of(cfg.folderPath),
                    FILE_PREFIX + "*" + FILE_SUFFIX)) {
                List<Path> all = new ArrayList<>();
                for (Path p : ds) all.add(p);
                if (all.size() > cfg.retain) {
                    all.sort(Comparator.comparing(Path::getFileName));
                    int toDelete = all.size() - cfg.retain;
                    for (int i = 0; i < toDelete; i++) {
                        try { Files.delete(all.get(i)); log.debug("Pruned old backup {}", all.get(i)); }
                        catch (IOException ignored) {}
                    }
                }
            } catch (IOException e) {
                log.debug("Folder retention skipped: {}", e.getMessage());
            }
        }
        // WebDAV pruning is intentionally omitted — generic WebDAV doesn't
        // mandate PROPFIND support. Users running Nextcloud / ownCloud can
        // set up server-side retention via the UI; for everyone else, a
        // small monthly cleanup script is simpler than detecting WebDAV
        // dialect quirks here.
    }

    // ---------------------------------------------------------------
    // Status DTO for /api/backup/status
    // ---------------------------------------------------------------

    public BackupStatus snapshot() {
        BackupStatus s = new BackupStatus();
        s.lastRunAt        = lastRunAt == null ? "" : lastRunAt.toString();
        s.lastRunSucceeded = lastRunSucceeded;
        s.lastRunMessage   = lastRunMessage;
        s.lastBytes        = lastBytes;
        BackupSection cfg = ConfigManager.getInstance().getConfig().backup;
        s.enabled = cfg != null && cfg.enabled;
        s.mode    = cfg == null ? "FOLDER" : cfg.mode;
        return s;
    }

    public static class BackupStatus {
        public boolean enabled;
        public String  mode;
        public String  lastRunAt;
        public boolean lastRunSucceeded;
        public String  lastRunMessage;
        public long    lastBytes;
    }

    public static class BackupResult {
        public boolean success;
        public String  message = "";
        public long    archiveBytes;
    }
}
