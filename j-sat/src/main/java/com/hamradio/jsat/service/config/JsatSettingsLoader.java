package com.hamradio.jsat.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

/**
 * Loads and saves JsatSettings from ~/.j-sat/settings.json.
 * Also syncs with J-Hub REST API if available.
 */
public class JsatSettingsLoader {

    private static final Logger log = LoggerFactory.getLogger(JsatSettingsLoader.class);
    private static final Path   SETTINGS_FILE = Path.of(System.getProperty("user.home"), ".j-sat", "settings.json");
    private static final ObjectMapper MAPPER  = new ObjectMapper();

    private static volatile String hubApiUrl = "http://localhost:8081/api/jsat";

    public static void setHubHost(String host, int webPort) {
        hubApiUrl = "http://" + host + ":" + webPort + "/api/jsat";
    }

    public static JsatSettings loadOrDefaults() {
        // Try J-Hub first
        JsatSettings fromHub = loadFromHub();
        if (fromHub != null) return fromHub;
        // Fall back to local file
        return loadFromDisk();
    }

    public static void save(JsatSettings settings) {
        saveToDisk(settings);
        pushToHub(settings);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private static JsatSettings loadFromHub() {
        try {
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(1500)).build();
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(hubApiUrl)).GET().build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && !resp.body().isBlank()) {
                JsatSettings s = MAPPER.readValue(resp.body(), JsatSettings.class);
                log.info("Settings loaded from J-Hub");
                return s;
            }
        } catch (Exception e) {
            log.debug("Hub settings unavailable ({}), using local", e.getMessage());
        }
        return null;
    }

    private static JsatSettings loadFromDisk() {
        if (!Files.exists(SETTINGS_FILE)) {
            log.info("No settings file found, using defaults");
            return new JsatSettings();
        }
        try {
            JsatSettings s = MAPPER.readValue(SETTINGS_FILE.toFile(), JsatSettings.class);
            log.info("Settings loaded from {}", SETTINGS_FILE);
            return s;
        } catch (IOException e) {
            log.warn("Failed to load settings: {}, using defaults", e.getMessage());
            return new JsatSettings();
        }
    }

    private static void saveToDisk(JsatSettings settings) {
        try {
            Files.createDirectories(SETTINGS_FILE.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(SETTINGS_FILE.toFile(), settings);
        } catch (IOException e) {
            log.warn("Failed to save settings: {}", e.getMessage());
        }
    }

    private static void pushToHub(JsatSettings settings) {
        try {
            String json = MAPPER.writeValueAsString(settings);
            HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(hubApiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
            http.send(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.debug("Could not push settings to hub: {}", e.getMessage());
        }
    }
}
