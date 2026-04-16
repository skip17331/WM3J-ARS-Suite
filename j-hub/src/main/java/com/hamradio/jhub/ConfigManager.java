package com.hamradio.jhub;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hamradio.jhub.model.JHubConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * ConfigManager — reads and writes the j-hub.json configuration file.
 *
 * The config file is stored in the current working directory as "j-hub.json".
 * On first run the file is created from built-in defaults.
 *
 * All access is through the singleton, which is safe to call from any thread.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    // Singleton
    private static final ConfigManager INSTANCE = new ConfigManager();
    public static ConfigManager getInstance() { return INSTANCE; }
    private ConfigManager() {}

    private static final String CONFIG_FILE = "j-hub.json";

    // Shared Gson instance (pretty-print for the file; compact for WS messages)
    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson COMPACT_GSON = new Gson();

    /** Gson for serializing WebSocket messages (compact). */
    public static Gson gson() { return COMPACT_GSON; }

    private JHubConfig config;

    // ---------------------------------------------------------------
    // Load / Save
    // ---------------------------------------------------------------

    /**
     * Load config from j-hub.json, creating it with defaults if absent.
     */
    public synchronized void load() throws IOException {
        Path path = Paths.get(CONFIG_FILE);
        if (!Files.exists(path)) {
            log.info("No config file found — creating default j-hub.json");
            config = JHubConfig.defaults();
            save();
        } else {
            try (Reader reader = new FileReader(path.toFile(), StandardCharsets.UTF_8)) {
                config = PRETTY_GSON.fromJson(reader, JHubConfig.class);
                if (config == null) {
                    log.warn("j-hub.json was empty — using defaults");
                    config = JHubConfig.defaults();
                }
            }
        }
        log.info("Configuration loaded from {}", path.toAbsolutePath());
    }

    /**
     * Write current config back to j-hub.json.
     */
    public synchronized void save() throws IOException {
        Path path = Paths.get(CONFIG_FILE);
        String json = PRETTY_GSON.toJson(config);
        Files.writeString(path, json, StandardCharsets.UTF_8);
        log.info("Configuration saved to {}", path.toAbsolutePath());
    }

    // ---------------------------------------------------------------
    // Accessors
    // ---------------------------------------------------------------

    public synchronized JHubConfig getConfig() { return config; }

    public synchronized JHubConfig.JHubSection      getJHub()      { return config.jHub;       }
    public synchronized JHubConfig.StationSection   getStation()   { return config.station;    }
    public synchronized JHubConfig.ClusterSection   getCluster()   { return config.cluster;    }
    public synchronized JHubConfig.LoggerSection    getLogger()    { return config.logger;     }
    public synchronized JHubConfig.InfoScreenSection getInfoScreen() { return config.infoScreen; }
    public synchronized JHubConfig.AppsSection       getApps()      { return config.apps;       }

    /**
     * Replace the entire config object (used by the web UI save endpoint).
     * Immediately persists to disk.
     */
    public synchronized void updateConfig(JHubConfig newConfig) throws IOException {
        this.config = newConfig;
        save();
    }

    /**
     * Return the current config as a pretty JSON string (for the web UI).
     */
    public synchronized String toJson() {
        return PRETTY_GSON.toJson(config);
    }

    /**
     * Parse a JSON string into a JHubConfig object (for the web UI save).
     */
    public JHubConfig fromJson(String json) {
        return PRETTY_GSON.fromJson(json, JHubConfig.class);
    }
}
