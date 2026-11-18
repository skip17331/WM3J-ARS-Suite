package com.ars.fx.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A satellite's ground track (one orbit of sub-satellite points, 91 SGP4 propagations) barely moves between the service's 2 s reports, so each
 * track is kept for a few seconds and reused. The track is centred on the moment it was computed; kept this short the satellite's dot stays on
 * its line (10 s is about 70 km of orbit, under a degree on the world map).
 */
final class GroundTrackCache {
    private record Entry(long atMs, Object source, double[][] track) {}
    private final Map<Integer, Entry> entries = new HashMap<>();
    private final long maxAgeMs;

    GroundTrackCache(long maxAgeMs) { this.maxAgeMs = maxAgeMs; }

    /**
     * The cached track for {@code norad}, or a new one from {@code compute} when there is none, it is older than the limit, or {@code source}
     * (the element set it came from) is a different object: new elements mean a new orbit.
     */
    synchronized double[][] get(int norad, Object source, long nowMs, Supplier<double[][]> compute) {
        Entry e = entries.get(norad);
        if (e != null && e.source() == source && nowMs - e.atMs() >= 0 && nowMs - e.atMs() < maxAgeMs) return e.track();
        double[][] t = compute.get();
        entries.put(norad, new Entry(nowMs, source, t));
        return t;
    }

    synchronized void clear() { entries.clear(); }
}
