package com.ars.fx.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Launch descriptor for running a single module (J-Map or J-Sat) on its own —
 * e.g. on a second monitor, a laptop, or a shack Raspberry Pi — driven by a
 * small JSON file rather than the full dock app.
 *
 * <pre>
 * {
 *   "module": "map",                         // "map" or "sat"
 *   "title":  "J-Map · shack Pi",            // optional window title
 *   "window": { "width": 1400, "height": 900, "maximized": false },
 *   "dock":   false,                          // show the module dock? default false (clean solo frame)
 *   "remote": "ws://192.168.1.50:8090",       // station J-Hub WebSocket; omit/empty = run fully local
 *   "station": { "call": "WM3J", "grid": "FM19", "lat": 39.7456, "lon": -76.96 }
 * }
 * </pre>
 *
 * <p>When {@code remote} is set the module talks to the station's J-Hub over the
 * WebSocket (live spots / rig / rotor in, tune + rotate commands out); when it is
 * empty the module behaves exactly like the in-dock build, using local daemons.
 */
public final class SoloConfig {

    public String module = "map";
    public String title;
    public Window window = new Window();
    public boolean dock = false;
    public String remote;                 // ws:// url of the station J-Hub, or null/blank for local
    public Station station;               // optional QTH/call overrides applied at startup

    public static final class Window {
        public double width = 1360;
        public double height = 880;
        public boolean maximized = false;
    }
    public static final class Station {
        public String call;
        public String grid;
        public Double lat;
        public Double lon;
    }

    public boolean isRemote() { return remote != null && !remote.isBlank(); }
    public String windowTitle() {
        if (title != null && !title.isBlank()) return title;
        return ("sat".equals(module) ? "J-Sat" : "J-Map") + (isRemote() ? " · remote" : "");
    }

    /** Load + validate a launch file; throws IllegalArgumentException with a clear message on bad input. */
    public static SoloConfig load(Path file) {
        String json;
        try { json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8); }
        catch (Exception e) { throw new IllegalArgumentException("cannot read launch file " + file + ": " + e.getMessage()); }

        SoloConfig cfg;
        try { cfg = new Gson().fromJson(json, SoloConfig.class); }
        catch (Exception e) { throw new IllegalArgumentException("invalid JSON in " + file + ": " + e.getMessage()); }
        if (cfg == null) throw new IllegalArgumentException("empty launch file " + file);
        if (cfg.window == null) cfg.window = new Window();

        cfg.module = cfg.module == null ? "" : cfg.module.trim().toLowerCase();
        if (!cfg.module.equals("map") && !cfg.module.equals("sat"))
            throw new IllegalArgumentException("\"module\" must be \"map\" or \"sat\" (got \"" + cfg.module + "\")");
        return cfg;
    }

    /** Apply this config's station overrides into HubConfig so the module's backends pick them up. */
    public void applyStation() {
        if (station == null) return;
        if (station.call != null && !station.call.isBlank()) HubConfig.set("station.call", station.call.trim().toUpperCase());
        if (station.grid != null && !station.grid.isBlank()) HubConfig.set("station.grid", station.grid.trim());
        if (station.lat != null) HubConfig.set("station.lat", String.valueOf(station.lat));
        if (station.lon != null) HubConfig.set("station.lon", String.valueOf(station.lon));
    }

    /** Write a commented sample launch file (used by the --write-sample helper). */
    public static String sample(String module) {
        JsonObject o = new JsonObject();
        o.addProperty("module", module);
        o.addProperty("title", ("sat".equals(module) ? "J-Sat" : "J-Map") + " · shack Pi");
        JsonObject w = new JsonObject(); w.addProperty("width", 1400); w.addProperty("height", 900); w.addProperty("maximized", false);
        o.add("window", w);
        o.addProperty("dock", false);
        o.addProperty("remote", "ws://192.168.1.50:8090");
        JsonObject s = new JsonObject(); s.addProperty("call", "WM3J"); s.addProperty("grid", "FM19");
        s.addProperty("lat", 39.7456); s.addProperty("lon", -76.96);
        o.add("station", s);
        return new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(o);
    }

    /** Pretty-print (debug). */
    @Override public String toString() {
        return "SoloConfig{module=" + module + ", remote=" + remote + ", dock=" + dock + "}";
    }
}
