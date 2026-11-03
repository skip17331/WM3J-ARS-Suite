package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Suite-wide station + hardware settings, edited on the J-Hub config pages and
 * persisted to {@code ~/.j-hub/config.json}. Flat key/value store
 * (e.g. {@code station.call}, {@code rig.baud}, {@code rotor.startDaemon}).
 */
public final class HubConfig {
    private HubConfig() {}

    private static JsonObject root;
    private static final java.util.List<java.util.function.Consumer<String>> LISTENERS =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Be told when a setting changes: the key, or {@code "*"} after {@link #reload()} replaced them all.
     *  Runs on the thread that made the change. */
    public static void addChangeListener(java.util.function.Consumer<String> l) { LISTENERS.add(l); }
    private static void changed(String key) { for (var l : LISTENERS) { try { l.accept(key); } catch (RuntimeException ignored) { } } }

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-hub", "config.json"); }

    private static synchronized void ensure() {
        if (root != null) return;
        root = new JsonObject();
        try {
            Path p = file();
            if (Files.exists(p)) root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        } catch (Exception e) { root = new JsonObject(); }
    }

    /** Drop the in-memory cache so the next read re-loads {@code config.json} from disk. The store is a
     *  per-process cache (loaded once), so the background hub calls this when a client edits config in
     *  another JVM and nudges a reconcile — see {@code HubServer.reconcile()}. */
    public static synchronized void reload() { root = null; ensure(); changed("*"); }

    public static synchronized String get(String key, String def) {
        ensure();
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : def;
    }
    public static synchronized boolean getBool(String key, boolean def) {
        ensure();
        try { return root.has(key) ? root.get(key).getAsBoolean() : def; } catch (Exception e) { return def; }
    }
    public static synchronized void set(String key, String val) { ensure(); root.addProperty(key, val == null ? "" : val); save(); changed(key); }
    /** Drop a key entirely, so readers fall back to their defaults. */
    public static synchronized void remove(String key) { ensure(); root.remove(key); save(); changed(key); }
    public static synchronized void setBool(String key, boolean val) { ensure(); root.addProperty(key, val); save(); changed(key); }

    // convenience accessors for the most-shared identity fields
    /** Your callsign from J-Hub ▸ Station; empty until set. */
    public static String call() { return get("station.call", "").trim(); }
    /** Your grid square from J-Hub ▸ Station; empty until set. */
    public static String grid() { return get("station.grid", "").trim(); }

    /** This station's {lat, lon}: the latitude / longitude from J-Hub ▸ Station, else the centre of the grid square;
     *  null when neither is set (nothing here ever substitutes somebody else's location). */
    public static double[] location() {
        try {
            String la = get("station.lat", "").trim(), lo = get("station.lon", "").trim();
            if (!la.isEmpty() && !lo.isEmpty()) {
                double a = Double.parseDouble(la), b = Double.parseDouble(lo);
                if (a >= -90 && a <= 90 && b >= -180 && b <= 360) return new double[]{a, b};
            }
        } catch (NumberFormatException ignored) { /* fall back to the grid */ }
        String g = grid();
        return g.isEmpty() ? null : com.jlog.util.Maidenhead.center(g);
    }
    public static boolean hasLocation() { return location() != null; }
    /** Latitude for maths that needs a number: 0 until a location is set — check {@link #hasLocation()} before showing a result. */
    public static double lat() { double[] l = location(); return l == null ? 0 : l[0]; }
    public static double lon() { double[] l = location(); return l == null ? 0 : l[1]; }
    public static final String LOCATION_HINT = "Set your location (grid square or latitude / longitude) in J-Hub ▸ Station.";

    private static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, root.toString());
        } catch (Exception ignored) { /* best-effort */ }
    }
}
