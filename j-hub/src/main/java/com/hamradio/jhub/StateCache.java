package com.hamradio.jhub;

import com.hamradio.jhub.model.RigCapabilities;
import com.hamradio.jhub.model.RigStatus;
import com.hamradio.jhub.model.Spot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * StateCache — thread-safe in-memory store for j-hub state.
 *
 * Maintains:
 *   • Last known RIG_STATUS
 *   • Last N spots (ring buffer, default 50)
 *   • Last LOGGER_SESSION raw JSON
 *   • Rolling spot counter for spots-per-minute calculation
 *
 * Late-connecting apps receive a replay of this cache immediately after
 * registration so they start with full situational awareness.
 */
public class StateCache {

    // Singleton
    private static final StateCache INSTANCE = new StateCache();
    public static StateCache getInstance() { return INSTANCE; }
    private StateCache() {}

    // ---------------------------------------------------------------
    // Rig status
    // ---------------------------------------------------------------

    private volatile RigStatus lastRigStatus;

    public RigStatus getLastRigStatus() { return lastRigStatus; }

    public void setLastRigStatus(RigStatus status) {
        this.lastRigStatus = status;
    }

    // ---------------------------------------------------------------
    // Rig capabilities (RIG_CAPS) — one per connection, replayed to
    // late joiners so the Rig Control pane can gate controls immediately.
    // ---------------------------------------------------------------

    private volatile RigCapabilities lastRigCaps;

    public RigCapabilities getLastRigCaps() { return lastRigCaps; }

    public void setLastRigCaps(RigCapabilities caps) {
        this.lastRigCaps = caps;
    }

    // ---------------------------------------------------------------
    // Logger session (stored as raw JSON string)
    // ---------------------------------------------------------------

    private volatile String lastLoggerSession;

    public String getLastLoggerSession() { return lastLoggerSession; }

    public void setLastLoggerSession(String rawJson) {
        this.lastLoggerSession = rawJson;
    }

    // ---------------------------------------------------------------
    // Last selected spot (for replay to late joiners)
    // ---------------------------------------------------------------

    private volatile String lastSelectedSpot; // raw SPOT_SELECTED JSON

    public String getLastSelectedSpot() { return lastSelectedSpot; }

    public void setLastSelectedSpot(String rawJson) { this.lastSelectedSpot = rawJson; }

    // ---------------------------------------------------------------
    // Rotor status (raw JSON)
    // ---------------------------------------------------------------

    private volatile String lastRotorStatus;

    public String getLastRotorStatus() { return lastRotorStatus; }

    public void setLastRotorStatus(String rawJson) { this.lastRotorStatus = rawJson; }

    // ---------------------------------------------------------------
    // J-Map config (JMAP_CONFIG message, broadcast to j-map on connect)
    // ---------------------------------------------------------------

    private volatile String lastJMapConfig;

    public String getLastJMapConfig() { return lastJMapConfig; }

    public void setLastJMapConfig(String rawJson) { this.lastJMapConfig = rawJson; }

    // ---------------------------------------------------------------
    // Satellite state (from J-Sat)
    // ---------------------------------------------------------------

    private volatile String lastSatState;

    public void setLastSatState(String rawJson) { this.lastSatState = rawJson; }

    // ---------------------------------------------------------------
    // Recent spots (ring buffer)
    // ---------------------------------------------------------------

    private static final int DEFAULT_MAX_SPOTS = 50;
    private int maxSpots = DEFAULT_MAX_SPOTS;

    private final LinkedList<Spot> recentSpots = new LinkedList<>();
    private final ReadWriteLock spotLock = new ReentrantReadWriteLock();

    // Counters for spot-rate calculation
    private final AtomicLong totalSpots = new AtomicLong(0);
    private final LinkedList<Long> spotTimestamps = new LinkedList<>(); // epoch millis

    public void addSpot(Spot spot) {
        spotLock.writeLock().lock();
        try {
            recentSpots.addLast(spot);
            if (recentSpots.size() > maxSpots) {
                recentSpots.removeFirst();
            }
            long now = System.currentTimeMillis();
            spotTimestamps.addLast(now);
            // Keep only the last 60 seconds of timestamps
            long cutoff = now - 60_000;
            while (!spotTimestamps.isEmpty() && spotTimestamps.getFirst() < cutoff) {
                spotTimestamps.removeFirst();
            }
            totalSpots.incrementAndGet();
        } finally {
            spotLock.writeLock().unlock();
        }
    }

    // ---------------------------------------------------------------
    // Heard-By spots (PSK Reporter, WSPR) — raw JSON, oldest first
    // ---------------------------------------------------------------

    private static final int MAX_HEARD_BY = 100;
    private final LinkedList<String> recentHeardBy = new LinkedList<>();
    private final ReadWriteLock heardByLock = new ReentrantReadWriteLock();

    public void addHeardBySpot(String rawJson) {
        if (rawJson == null) return;
        heardByLock.writeLock().lock();
        try {
            recentHeardBy.addLast(rawJson);
            if (recentHeardBy.size() > MAX_HEARD_BY) recentHeardBy.removeFirst();
        } finally {
            heardByLock.writeLock().unlock();
        }
    }

    public List<String> getRecentHeardBy() {
        heardByLock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(recentHeardBy));
        } finally {
            heardByLock.readLock().unlock();
        }
    }

    /** Returns an unmodifiable snapshot of recent spots (oldest first). */
    public List<Spot> getRecentSpots() {
        spotLock.readLock().lock();
        try {
            return Collections.unmodifiableList(new ArrayList<>(recentSpots));
        } finally {
            spotLock.readLock().unlock();
        }
    }

    /**
     * Find the most recent spot for a given callsign (case-insensitive).
     * Returns null if not found in the ring buffer.
     */
    public Spot findSpotByCallsign(String callsign) {
        if (callsign == null) return null;
        spotLock.readLock().lock();
        try {
            Spot[] arr = recentSpots.toArray(new Spot[0]);
            for (int i = arr.length - 1; i >= 0; i--) {
                if (callsign.equalsIgnoreCase(arr[i].spotted)) return arr[i];
            }
            return null;
        } finally {
            spotLock.readLock().unlock();
        }
    }

    /**
     * Returns spots per minute calculated over the last 60 seconds.
     */
    public double getSpotsPerMinute() {
        spotLock.readLock().lock();
        try {
            long now = System.currentTimeMillis();
            long cutoff = now - 60_000;
            long count = spotTimestamps.stream().filter(t -> t >= cutoff).count();
            // count = spots in last minute
            return count;
        } finally {
            spotLock.readLock().unlock();
        }
    }

    public long getTotalSpots() { return totalSpots.get(); }

    public void setMaxSpots(int max) {
        spotLock.writeLock().lock();
        try {
            this.maxSpots = max;
        } finally {
            spotLock.writeLock().unlock();
        }
    }
}
