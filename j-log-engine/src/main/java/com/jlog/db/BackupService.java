package com.jlog.db;

import com.jlog.util.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically snapshots the log / contest / config SQLite databases to
 * {@code ~/.j-log/backups/YYYYMMDD-HHmmss/}. Cadence and retention count
 * are configurable via {@link AppConfig}. Old snapshot directories beyond
 * the retention count are pruned after each successful backup.
 *
 * Started automatically from {@link DatabaseManager#initAll()} when
 * {@code backup.enabled} is true (default). A backup is also taken on the
 * first run shortly after init — new installations get a baseline snapshot
 * even before the first interval elapses.
 */
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final BackupService INSTANCE = new BackupService();
    public  static BackupService getInstance() { return INSTANCE; }
    private BackupService() {}

    private ScheduledExecutorService scheduler;

    /** Start the scheduled backup thread. Idempotent — calling twice is a no-op. */
    public synchronized void start() {
        if (scheduler != null) return;
        AppConfig cfg = AppConfig.getInstance();
        if (!cfg.isBackupEnabled()) { log.info("DB backup disabled by config"); return; }

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "db-backup");
            t.setDaemon(true);
            return t;
        });
        int intervalHours = Math.max(1, cfg.getBackupIntervalHours());
        // First run after 60s gives the app time to fully init; then every interval.
        scheduler.scheduleAtFixedRate(this::runBackup, 60, (long) intervalHours * 3600, TimeUnit.SECONDS);
        log.info("DB backup scheduled every {}h (retain last {})",
            intervalHours, cfg.getBackupKeepCount());
    }

    public synchronized void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    /** Run a single backup pass immediately. Exposed so the user can trigger
     *  "Backup now" from a setup/menu command later. */
    public synchronized void runBackupNow() { runBackup(); }

    // -----------------------------------------------------------------
    // Backup pass
    // -----------------------------------------------------------------

    private void runBackup() {
        try {
            Path dataDir = DatabaseManager.getInstance().getDataDir();
            if (dataDir == null) return;
            Path backupRoot = dataDir.resolve("backups");
            Files.createDirectories(backupRoot);

            String stamp = LocalDateTime.now().format(STAMP);
            Path dest = backupRoot.resolve(stamp);
            Files.createDirectories(dest);

            int copied = 0;
            for (String dbName : new String[]{"j-log.db", "contest.db", "config.db"}) {
                Path src = dataDir.resolve(dbName);
                if (!Files.exists(src)) continue;
                Files.copy(src, dest.resolve(dbName), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
            if (copied == 0) {
                // Nothing copied — likely pre-init call. Remove the empty dir.
                Files.deleteIfExists(dest);
                return;
            }
            log.info("DB backup: wrote {} files to {}", copied, dest);
            pruneOldBackups(backupRoot, AppConfig.getInstance().getBackupKeepCount());
        } catch (Exception e) {
            log.warn("DB backup failed: {}", e.getMessage());
        }
    }

    private void pruneOldBackups(Path backupRoot, int keep) {
        if (keep < 1) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(backupRoot)) {
            List<Path> dirs = new ArrayList<>();
            for (Path p : ds) if (Files.isDirectory(p)) dirs.add(p);
            // Newest-first by mtime.
            dirs.sort((a, b) -> {
                try {
                    return Files.readAttributes(b, BasicFileAttributes.class).lastModifiedTime()
                        .compareTo(Files.readAttributes(a, BasicFileAttributes.class).lastModifiedTime());
                } catch (IOException e) { return 0; }
            });
            for (int i = keep; i < dirs.size(); i++) deleteRecursively(dirs.get(i));
        } catch (IOException e) {
            log.debug("prune scan failed: {}", e.getMessage());
        }
    }

    private void deleteRecursively(Path dir) {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                    Files.deleteIfExists(f); return FileVisitResult.CONTINUE;
                }
                @Override public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                    Files.deleteIfExists(d); return FileVisitResult.CONTINUE;
                }
            });
            log.debug("pruned old backup: {}", dir);
        } catch (IOException e) {
            log.warn("failed to prune {}: {}", dir, e.getMessage());
        }
    }
}
