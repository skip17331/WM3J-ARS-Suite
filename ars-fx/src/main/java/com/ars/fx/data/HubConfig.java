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

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-hub", "config.json"); }

    private static synchronized void ensure() {
        if (root != null) return;
        root = new JsonObject();
        try {
            Path p = file();
            if (Files.exists(p)) root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        } catch (Exception e) { root = new JsonObject(); }
    }

    public static synchronized String get(String key, String def) {
        ensure();
        return root.has(key) && !root.get(key).isJsonNull() ? root.get(key).getAsString() : def;
    }
    public static synchronized boolean getBool(String key, boolean def) {
        ensure();
        try { return root.has(key) ? root.get(key).getAsBoolean() : def; } catch (Exception e) { return def; }
    }
    public static synchronized void set(String key, String val) { ensure(); root.addProperty(key, val == null ? "" : val); save(); }
    public static synchronized void setBool(String key, boolean val) { ensure(); root.addProperty(key, val); save(); }

    // convenience accessors for the most-shared identity fields
    public static String call() { return get("station.call", "WM3J"); }
    public static String grid() { return get("station.grid", "FM19"); }

    private static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, root.toString());
        } catch (Exception ignored) { /* best-effort */ }
    }
}
