package com.hamradio.jsat.service.tle;

import com.hamradio.jsat.model.TleSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages TLE storage, caching, and auto-update scheduling.
 * TLEs are persisted to ~/.j-sat/tles.txt and refreshed daily.
 */
public class TleManager {

    private static final Logger log = LoggerFactory.getLogger(TleManager.class);
    private static final Path TLE_CACHE = Path.of(
        System.getProperty("user.home"), ".j-sat", "tles.txt");

    private final CelestrakTleProvider provider = new CelestrakTleProvider();
    private final Map<String, TleSet>  byName   = new ConcurrentHashMap<>();
    private final Map<String, TleSet>  byNorad  = new ConcurrentHashMap<>();
    private volatile Instant lastUpdated;

    /** Load TLEs from disk cache or fetch fresh if stale/absent. */
    public void initialize() {
        if (Files.exists(TLE_CACHE) && !isCacheStale()) {
            loadFromDisk();
        } else {
            updateNow();
        }
    }

    /** Force-refresh TLEs from Celestrak. */
    public void updateNow() {
        try {
            List<TleSet> fresh = provider.fetch();
            index(fresh);
            saveToDisk(fresh);
            lastUpdated = Instant.now();
            log.info("TLE update complete: {} satellites indexed", byName.size());
        } catch (Exception e) {
            log.error("TLE update failed: {}", e.getMessage());
            if (byName.isEmpty()) loadFromDisk(); // fall back to stale cache
        }
    }

    public TleSet findByName(String name) {
        if (name == null) return null;
        String key = name.trim().toUpperCase();
        // Exact match first
        TleSet t = byName.get(key);
        if (t != null) return t;
        // Partial match
        for (Map.Entry<String, TleSet> e : byName.entrySet()) {
            if (e.getKey().contains(key) || key.contains(e.getKey())) return e.getValue();
        }
        return null;
    }

    public TleSet findByNorad(String noradId) {
        return noradId != null ? byNorad.get(noradId.trim()) : null;
    }

    public TleSet findByNorad(int noradId) {
        return noradId > 0 ? byNorad.get(String.valueOf(noradId)) : null;
    }

    public Collection<TleSet> allTles() { return Collections.unmodifiableCollection(byName.values()); }

    public Instant getLastUpdated() { return lastUpdated; }

    public TleSet.TleFreshness overallFreshness() {
        if (byName.isEmpty() || lastUpdated == null) return TleSet.TleFreshness.RED;
        long daysSince = ChronoUnit.DAYS.between(lastUpdated, Instant.now());
        if (daysSince < 3)  return TleSet.TleFreshness.GREEN;
        if (daysSince < 7)  return TleSet.TleFreshness.YELLOW;
        return TleSet.TleFreshness.RED;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void index(List<TleSet> tles) {
        byName.clear();
        byNorad.clear();
        for (TleSet t : tles) {
            byName.put(t.name.toUpperCase(), t);
            byNorad.put(t.noradId, t);
        }
    }

    private boolean isCacheStale() {
        try {
            Instant modified = Files.getLastModifiedTime(TLE_CACHE).toInstant();
            return ChronoUnit.HOURS.between(modified, Instant.now()) >= 24;
        } catch (IOException e) {
            return true;
        }
    }

    private void loadFromDisk() {
        if (!Files.exists(TLE_CACHE)) return;
        try {
            String content = Files.readString(TLE_CACHE);
            List<TleSet> tles = CelestrakTleProvider.parseTles(content);
            index(tles);
            lastUpdated = Files.getLastModifiedTime(TLE_CACHE).toInstant();
            log.info("Loaded {} TLEs from disk cache", tles.size());
        } catch (IOException e) {
            log.warn("Could not load TLE cache: {}", e.getMessage());
        }
    }

    private void saveToDisk(List<TleSet> tles) {
        try {
            Files.createDirectories(TLE_CACHE.getParent());
            StringBuilder sb = new StringBuilder();
            for (TleSet t : tles) {
                sb.append(t.name).append('\n');
                sb.append(t.line1).append('\n');
                sb.append(t.line2).append('\n');
            }
            Files.writeString(TLE_CACHE, sb.toString());
        } catch (IOException e) {
            log.warn("Could not save TLE cache: {}", e.getMessage());
        }
    }
}
