package com.ars.fx.data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Scores the propagation estimate ({@link Propagation}) against what the DX cluster hears. Every spot is evidence that a path was open:
 * somebody at the spotter's end heard the station at the spot's end, on that frequency, at that moment. So for each spot of 1,000 km or
 * more the estimate is asked about the path spotter-to-station then, and its answer is tallied - likely open, marginal, or probably
 * closed (a miss). The tallies are kept per band and carried over between sessions, so after a few weeks they say how far to trust the
 * estimate and on which bands it is weakest.
 *
 * <p>What it can and cannot say: spots only ever show paths that were open, so it measures misses (closed paths that were heard), never
 * false alarms (open paths that were not). A high "likely open" share is necessary for a good estimate, not proof of one. Spotters are
 * not spread evenly, so the share is also biased toward the busy parts of the world.
 */
public final class PropagationCheck {
    private PropagationCheck() {}

    public static final String KEY = "propcheck.tally";
    static final double MIN_KM = 1000;

    /** Counts for one band. */
    public record Tally(int open, int marginal, int closed) {
        public int n() { return open + marginal + closed; }
        Tally add(Propagation.Verdict v) {
            return new Tally(open + (v == Propagation.Verdict.OPEN ? 1 : 0), marginal + (v == Propagation.Verdict.MARGINAL ? 1 : 0), closed + (v == Propagation.Verdict.CLOSED ? 1 : 0));
        }
    }

    private static final TreeMap<String, Tally> TALLY = new TreeMap<>();
    private static boolean loaded;
    private static int unsaved;
    private static long lastSpotMs = System.currentTimeMillis();

    // ---- recording ---------------------------------------------------------------------------------------------------

    /** Asks the estimate about one heard path and tallies the answer. Returns the verdict, or null if the path was not counted (too short, no weather, no band). */
    public static synchronized Propagation.Verdict observe(Propagation.Point spotter, Propagation.Point station, long freqHz, LocalDateTime utc, Propagation.Weather w) {
        if (spotter == null || station == null || w == null || freqHz <= 0) return null;
        if (Propagation.distanceKm(spotter, station) < MIN_KM) return null;            // short paths are ground wave / NVIS, which the model does not claim to cover
        String band = Bands.of(freqHz);
        if (band == null || band.isEmpty()) return null;
        Propagation.Verdict v = Propagation.verdictNow(spotter, station, w, utc, freqHz / 1e6);
        if (v == null) return null;
        load();
        TALLY.merge(band, new Tally(0, 0, 0).add(v), (a, b) -> new Tally(a.open() + b.open(), a.marginal() + b.marginal(), a.closed() + b.closed()));
        if (++unsaved >= 25) save();
        return v;
    }

    /** Spots as they arrive (the cluster listener): each one not seen before is checked. */
    public static synchronized int spots(List<ClusterClient.Spot> spots) {
        long newest = lastSpotMs;
        List<ClusterClient.Spot> fresh = new ArrayList<>();
        for (ClusterClient.Spot s : spots) { newest = Math.max(newest, s.arrivalMs()); if (s.arrivalMs() > lastSpotMs) fresh.add(s); }
        lastSpotMs = newest;
        if (fresh.isEmpty()) return 0;
        SolarData.Snapshot sd = SolarData.getInstance().snapshot();
        if (sd == null) return 0;
        Propagation.Weather w = Propagation.Weather.of(sd.sfi(), sd.sunspots(), sd.kIndex());
        if (w == null) return 0;
        int n = 0;
        for (ClusterClient.Spot s : fresh) {
            double[] a = Geo.of(spotterBase(s.de())), b = Geo.of(s.call());
            if (a == null || b == null) continue;
            LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochMilli(s.arrivalMs()), ZoneOffset.UTC);
            if (observe(new Propagation.Point(a[0], a[1]), new Propagation.Point(b[0], b[1]), s.freqHz(), t, w) != null) n++;
        }
        return n;
    }

    /** The spotter's callsign without a skimmer's "-#" or an SSID. */
    static String spotterBase(String de) { return de == null ? "" : de.trim().replaceAll("-.*$", ""); }

    /** Starts watching the cluster. Call once at start-up. */
    public static void init() {
        ClusterClient.getInstance().addListener("prop-check", cc -> { try { spots(cc.spots()); } catch (Throwable ignored) { } });
    }

    // ---- reading -----------------------------------------------------------------------------------------------------

    public static synchronized Map<String, Tally> byBand() { load(); return new TreeMap<>(TALLY); }

    public static synchronized Tally total() {
        load();
        int o = 0, m = 0, c = 0;
        for (Tally t : TALLY.values()) { o += t.open(); m += t.marginal(); c += t.closed(); }
        return new Tally(o, m, c);
    }

    /** One line for the panel: "Estimate check: 412 heard paths - 71% likely open, 18% marginal, 11% probably closed". */
    public static String summary() {
        Tally t = total();
        if (t.n() == 0) return "Estimate check: waiting for DX cluster spots of 1,000 km or more to score the estimate against.";
        return String.format(Locale.US, "Estimate check: %,d heard paths · %d%% rated likely open, %d%% marginal, %d%% probably closed (a miss)",
                t.n(), pct(t.open(), t.n()), pct(t.marginal(), t.n()), pct(t.closed(), t.n()));
    }

    /** The same by band, weakest first, for a tip: "10m: 41% misses of 120". */
    public static synchronized String weakest() {
        load();
        List<Map.Entry<String, Tally>> l = new ArrayList<>(TALLY.entrySet());
        l.removeIf(e -> e.getValue().n() < 20);
        l.sort((a, b) -> Double.compare((double) b.getValue().closed() / b.getValue().n(), (double) a.getValue().closed() / a.getValue().n()));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Tally> e : l.subList(0, Math.min(3, l.size())))
            sb.append(sb.length() == 0 ? "" : "; ").append(e.getKey()).append(": ").append(pct(e.getValue().closed(), e.getValue().n())).append("% misses of ").append(e.getValue().n());
        return sb.length() == 0 ? "Not enough heard paths on any band yet (20 or more are needed)." : "Weakest bands: " + sb;
    }

    private static int pct(int part, int whole) { return whole == 0 ? 0 : (int) Math.round(100.0 * part / whole); }

    // ---- keeping ------------------------------------------------------------------------------------------------------

    /** Start the count again. */
    public static synchronized void reset() { TALLY.clear(); unsaved = 0; loaded = true; HubConfig.set(KEY, ""); }

    static String encode() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Tally> e : TALLY.entrySet())
            sb.append(sb.length() == 0 ? "" : ";").append(e.getKey()).append(':').append(e.getValue().open()).append(',').append(e.getValue().marginal()).append(',').append(e.getValue().closed());
        return sb.toString();
    }

    static synchronized void decode(String s) {
        TALLY.clear(); loaded = true;
        if (s == null) return;
        for (String part : s.split(";")) {
            String[] kv = part.split(":");
            if (kv.length != 2) continue;
            String[] n = kv[1].split(",");
            if (n.length != 3) continue;
            try { TALLY.put(kv[0], new Tally(Math.max(0, Integer.parseInt(n[0])), Math.max(0, Integer.parseInt(n[1])), Math.max(0, Integer.parseInt(n[2])))); }
            catch (NumberFormatException ignored) { /* a bad entry is skipped */ }
        }
    }

    private static void load() { if (!loaded) { decode(HubConfig.get(KEY, "")); loaded = true; } }
    private static void save() { HubConfig.set(KEY, encode()); unsaved = 0; }
    /** Writes the tallies now (the app calls this on a clean exit). */
    public static synchronized void flush() { if (loaded && unsaved > 0) save(); }

    /** Tests: forget what was loaded so the next read starts from the config again. */
    static synchronized void forgetForTest() { TALLY.clear(); loaded = false; unsaved = 0; lastSpotMs = System.currentTimeMillis(); }
}
