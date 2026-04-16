package com.wm3j.jmap.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads and saves Settings to a JSON file in the user's home directory.
 */
public class SettingsLoader {

    private static final Logger log = LoggerFactory.getLogger(SettingsLoader.class);
    private static final String SETTINGS_DIR = System.getProperty("user.home") + File.separator + ".j-map";
    private static final String SETTINGS_FILE = SETTINGS_DIR + File.separator + "settings.json";

    private static final ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    public static Settings loadOrDefaults() {
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
        try {
            Files.createDirectories(Paths.get(SETTINGS_DIR));
            mapper.writeValue(new File(SETTINGS_FILE), settings);
            log.info("Settings saved to {}", SETTINGS_FILE);
        } catch (IOException e) {
            log.error("Failed to save settings: {}", e.getMessage());
        }
    }

    public static String getSettingsFilePath() {
        return SETTINGS_FILE;
    }
}
