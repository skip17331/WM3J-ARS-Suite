package com.wm3j.jmap.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads J-Map settings.
 *
 * Local mode  (hub reachable): fetch from J-Hub HTTP /api/jmap, fall back to ~/.j-map/settings.json
 * Remote mode (hub not reachable): load /config/jmap_config.json, fall back to ~/.j-map/settings.json
 *
 * Settings are no longer pushed back to port 8082 — J-Hub is the sole config authority.
 * Live updates arrive via WebSocket as JMAP_CONFIG messages.
 */
public class SettingsLoader {

    private static final Logger log = LoggerFactory.getLogger(SettingsLoader.class);

    private static final String SETTINGS_DIR  = System.getProperty("user.home") + File.separator + ".j-map";
    private static final String SETTINGS_FILE = SETTINGS_DIR + File.separator + "settings.json";
    private static final String REMOTE_CONFIG = "/config/jmap_config.json";

    private static volatile String jhubApiUrl = "http://localhost:8081/api/jmap";

    /** Call before loadOrDefaults() to point at a remote J-Hub. */
    public static void setJHubHost(String host, int webPort) {
        jhubApiUrl = "http://" + host + ":" + webPort + "/api/jmap";
        log.info("J-Hub API URL set to {}", jhubApiUrl);
    }

    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Load settings.
     *
     * @param hubReachable true if J-Hub WS port was reachable at startup (local mode)
     */
    public static Settings loadOrDefaults(boolean hubReachable) {
        if (hubReachable) {
            // Local mode: try J-Hub HTTP API first
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(jhubApiUrl).openConnection();
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(2000);
                if (conn.getResponseCode() == 200) {
                    byte[] body = conn.getInputStream().readAllBytes();
                    if (body.length > 2) {
                        Settings s = mapper.readValue(body, Settings.class);
                        log.info("Settings loaded from J-Hub (local mode)");
                        return s;
                    }
                }
            } catch (Exception e) {
                log.debug("J-Hub HTTP not available for initial load: {}", e.getMessage());
            }
        } else {
            // Remote mode: try /config/jmap_config.json
            Path remotePath = Path.of(REMOTE_CONFIG);
            if (Files.exists(remotePath)) {
                try {
                    Settings s = mapper.readValue(remotePath.toFile(), Settings.class);
                    log.info("Settings loaded from remote config: {}", REMOTE_CONFIG);
                    return s;
                } catch (IOException e) {
                    log.warn("Failed to load remote config {}: {}", REMOTE_CONFIG, e.getMessage());
                }
            } else {
                log.debug("Remote config not found at {}", REMOTE_CONFIG);
            }
        }

        // Fall back to local settings.json
        Path localPath = Paths.get(SETTINGS_FILE);
        if (Files.exists(localPath)) {
            try {
                Settings s = mapper.readValue(localPath.toFile(), Settings.class);
                log.info("Settings loaded from {}", SETTINGS_FILE);
                return s;
            } catch (IOException e) {
                log.warn("Failed to load settings, using defaults: {}", e.getMessage());
            }
        } else {
            log.info("No settings file found, using defaults");
        }
        return new Settings();
    }

    /** Backward-compat overload — assumes local mode. */
    public static Settings loadOrDefaults() {
        return loadOrDefaults(true);
    }

    public static void save(Settings settings) {
        try {
            Files.createDirectories(Paths.get(SETTINGS_DIR));
            mapper.writeValue(new File(SETTINGS_FILE), settings);
            log.debug("Settings saved locally to {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to save settings locally: {}", e.getMessage());
        }
        // No push to port 8082 — J-Hub is the sole configuration authority.
        // Live updates flow from J-Hub → J-Map via WebSocket (JMAP_CONFIG).
    }

    public static String getSettingsFilePath() {
        return SETTINGS_FILE;
    }

    /**
     * Quick read of just the J-Hub bootstrap fields (host, ws port, web port)
     * from the local settings file. Used at startup before any network call,
     * so we know where to look for J-Hub. Returns a defaults-only Settings
     * if the file is missing — never blocks, never throws.
     */
    public static Settings peekBootstrap() {
        Path local = Paths.get(SETTINGS_FILE);
        if (Files.exists(local)) {
            try {
                return mapper.readValue(local.toFile(), Settings.class);
            } catch (Exception e) {
                log.debug("peekBootstrap: failed to read {}: {}", SETTINGS_FILE, e.getMessage());
            }
        }
        return new Settings();
    }

    /** Persist just the bootstrap fields back to local settings.json. */
    public static void saveBootstrap(String host, int wsPort, int webPort) {
        try {
            Files.createDirectories(Paths.get(SETTINGS_DIR));
            Path local = Paths.get(SETTINGS_FILE);
            Settings s = Files.exists(local)
                ? mapper.readValue(local.toFile(), Settings.class)
                : new Settings();
            s.setJhubHost(host);
            s.setJhubWsPort(wsPort);
            s.setJhubWebPort(webPort);
            mapper.writeValue(local.toFile(), s);
            log.info("Bootstrap saved: jhub={}:{}/{}", host, wsPort, webPort);
        } catch (Exception e) {
            log.warn("saveBootstrap failed: {}", e.getMessage());
        }
    }
}
