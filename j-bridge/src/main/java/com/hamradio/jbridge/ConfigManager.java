package com.hamradio.jbridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * ConfigManager — reads and writes {@code j-bridge-config.json}.
 *
 * Uses the same Gson version as j-hub (2.10.1) for consistency.
 * Config file lives in the current working directory alongside j-hub's hub.json.
 *
 * Thread-safe singleton.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final ConfigManager INSTANCE = new ConfigManager();
    public  static ConfigManager getInstance() { return INSTANCE; }
    private ConfigManager() {}

    private static final String CONFIG_FILE = "j-bridge-config.json";
    private static final Gson   PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Config values ──────────────────────────────────────────────────────────

    // Hub
    private String hubAddress = "localhost";
    private int    hubPort    = 8080;

    // WSJT-X UDP
    private int    wsjtxUdpPort    = 2237;
    private String wsjtxBindAddress = "0.0.0.0";

    // Display
    private int      decodeHistoryLength = 200;
    private boolean  autoScroll          = true;
    private boolean  showCQOnly          = false;
    private int      minimumSnr          = -20;
    private String[] bandFilters         = {"160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"};

    // ── Load / Save ────────────────────────────────────────────────────────────

    public synchronized void load() {
        Path path = Paths.get(CONFIG_FILE);
        if (!Files.exists(path)) {
            log.info("No config file found — creating {}", CONFIG_FILE);
            save();
            return;
        }
        try (Reader reader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            if (root.has("hub")) {
                JsonObject hub = root.getAsJsonObject("hub");
                if (hub.has("address")) hubAddress = hub.get("address").getAsString();
                if (hub.has("port"))    hubPort    = hub.get("port").getAsInt();
            }
            if (root.has("wsjtx")) {
                JsonObject wx = root.getAsJsonObject("wsjtx");
                if (wx.has("udpPort"))     wsjtxUdpPort     = wx.get("udpPort").getAsInt();
                if (wx.has("bindAddress")) wsjtxBindAddress = wx.get("bindAddress").getAsString();
            }
            if (root.has("display")) {
                JsonObject d = root.getAsJsonObject("display");
                if (d.has("decodeHistoryLength")) decodeHistoryLength = d.get("decodeHistoryLength").getAsInt();
                if (d.has("autoScroll"))          autoScroll          = d.get("autoScroll").getAsBoolean();
                if (d.has("showCQOnly"))          showCQOnly          = d.get("showCQOnly").getAsBoolean();
                if (d.has("minimumSnr"))          minimumSnr          = d.get("minimumSnr").getAsInt();
                if (d.has("bandFilters"))         bandFilters         = PRETTY_GSON.fromJson(d.get("bandFilters"), String[].class);
            }
            log.info("Config loaded from {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to read config, using defaults: {}", e.getMessage());
        }
    }

    public synchronized void save() {
        try {
            JsonObject root = new JsonObject();

            JsonObject hub = new JsonObject();
            hub.addProperty("address", hubAddress);
            hub.addProperty("port",    hubPort);
            root.add("hub", hub);

            JsonObject wx = new JsonObject();
            wx.addProperty("udpPort",     wsjtxUdpPort);
            wx.addProperty("bindAddress", wsjtxBindAddress);
            root.add("wsjtx", wx);

            JsonObject d = new JsonObject();
            d.addProperty("decodeHistoryLength", decodeHistoryLength);
            d.addProperty("autoScroll",          autoScroll);
            d.addProperty("showCQOnly",          showCQOnly);
            d.addProperty("minimumSnr",          minimumSnr);
            d.add("bandFilters", PRETTY_GSON.toJsonTree(bandFilters));
            root.add("display", d);

            Path path = Paths.get(CONFIG_FILE);
            Files.writeString(path, PRETTY_GSON.toJson(root), StandardCharsets.UTF_8);
            log.info("Config saved to {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.warn("Failed to save config: {}", e.getMessage());
        }
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public synchronized String getHubAddress()            { return hubAddress; }
    public synchronized void   setHubAddress(String v)    { hubAddress = v; }

    public synchronized int    getHubPort()               { return hubPort; }
    public synchronized void   setHubPort(int v)          { hubPort = v; }

    public synchronized int    getWsjtxUdpPort()          { return wsjtxUdpPort; }
    public synchronized void   setWsjtxUdpPort(int v)     { wsjtxUdpPort = v; }

    public synchronized String getWsjtxBindAddress()      { return wsjtxBindAddress; }
    public synchronized void   setWsjtxBindAddress(String v){ wsjtxBindAddress = v; }

    public synchronized int    getDecodeHistoryLength()   { return decodeHistoryLength; }
    public synchronized void   setDecodeHistoryLength(int v){ decodeHistoryLength = v; }

    public synchronized boolean isAutoScroll()            { return autoScroll; }
    public synchronized void    setAutoScroll(boolean v)  { autoScroll = v; }

    public synchronized boolean isShowCQOnly()            { return showCQOnly; }
    public synchronized void    setShowCQOnly(boolean v)  { showCQOnly = v; }

    public synchronized int    getMinimumSnr()            { return minimumSnr; }
    public synchronized void   setMinimumSnr(int v)       { minimumSnr = v; }

    public synchronized String[] getBandFilters()         { return bandFilters; }
    public synchronized void     setBandFilters(String[] v){ bandFilters = v; }

    /** Full WebSocket URI for j-hub, e.g. "ws://localhost:8080" */
    public synchronized String getHubUri() {
        return "ws://" + hubAddress + ":" + hubPort;
    }
}
