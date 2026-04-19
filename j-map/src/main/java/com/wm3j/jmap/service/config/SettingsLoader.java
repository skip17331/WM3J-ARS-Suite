package com.wm3j.jmap.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SettingsLoader {

    private static final Logger log = LoggerFactory.getLogger(SettingsLoader.class);
    private static final String SETTINGS_DIR  = System.getProperty("user.home") + File.separator + ".j-map";
    private static final String SETTINGS_FILE = SETTINGS_DIR + File.separator + "settings.json";
    private static volatile String jhubApiUrl = "http://localhost:8081/api/jmap";

    /** Call before loadOrDefaults() to point at a remote j-hub. */
    public static void setJHubHost(String host, int webPort) {
        jhubApiUrl = "http://" + host + ":" + webPort + "/api/jmap";
        log.info("J-Hub API URL set to {}", jhubApiUrl);
    }

    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public static Settings loadOrDefaults() {
        // Try j-hub first
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(jhubApiUrl).openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(2000);
            if (conn.getResponseCode() == 200) {
                byte[] body = conn.getInputStream().readAllBytes();
                if (body.length > 2) {
                    Settings s = mapper.readValue(body, Settings.class);
                    log.info("Settings loaded from j-hub");
                    return s;
                }
            }
        } catch (Exception e) {
            log.debug("j-hub not available for settings load: {}", e.getMessage());
        }

        // Fall back to local file
        Path path = Paths.get(SETTINGS_FILE);
        if (Files.exists(path)) {
            try {
                Settings s = mapper.readValue(path.toFile(), Settings.class);
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

    public static void save(Settings settings) {
        // Save to local file (always)
        try {
            Files.createDirectories(Paths.get(SETTINGS_DIR));
            mapper.writeValue(new File(SETTINGS_FILE), settings);
            log.info("Settings saved to {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to save settings locally: {}", e.getMessage());
        }

        // Also push to j-hub so the UI stays in sync
        try {
            byte[] json = mapper.writeValueAsBytes(settings);
            HttpURLConnection conn = (HttpURLConnection) new URL(jhubApiUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(2000);
            conn.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = conn.getOutputStream()) {
                os.write(json);
            }
            conn.getResponseCode(); // consume
            log.debug("Settings pushed to j-hub");
        } catch (Exception e) {
            log.debug("Could not push settings to j-hub: {}", e.getMessage());
        }
    }

    public static String getSettingsFilePath() {
        return SETTINGS_FILE;
    }
}
