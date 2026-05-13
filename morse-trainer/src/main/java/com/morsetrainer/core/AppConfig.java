package com.morsetrainer.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Central application configuration. Loaded from JSON at startup; persisted on demand.
 * All numeric fields have sensible defaults so the app runs without a config file.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppConfig {

    // --- Audio ---
    public int toneFrequencyHz = 650;
    public int sampleRateHz    = 44_100;
    public double toneVolume   = 0.7;          // 0.0 .. 1.0
    public int rampMillis      = 5;            // attack/release envelope to avoid clicks

    // --- Speed ---
    public int wpm             = 18;           // character WPM
    public int farnsworthWpm   = 18;           // effective overall WPM (>= wpm means no Farnsworth)

    // --- Decoder ---
    public double ditDahThresholdRatio    = 2.0;   // anything > ditLen * this is treated as a dah
    public double charGapThresholdRatio   = 2.5;   // gap > ditLen * this ends a character
    public double wordGapThresholdRatio   = 6.0;   // gap > ditLen * this ends a word
    public boolean adaptiveDecoder        = true;  // adapts dit length from incoming stream

    // --- Hardware ---
    public String arduinoSerialPort       = "";       // e.g. "COM3" or "/dev/ttyUSB0"
    public int    arduinoBaudRate         = 115_200;
    public int    pizeroUdpPort           = 51_234;
    public String pizeroBleCharacteristic = "";       // optional BLE characteristic UUID

    // --- Training ---
    public String kochOrder = "KMURESNAPTLWI.JZ=FOY,VG5/Q92H38B?47C1D6X-0";
    public int defaultKochLevel = 2;            // start with K, M
    public int letterSessionCount = 50;
    public int groupMinSize = 2;
    public int groupMaxSize = 5;

    // --- User station (for the QSO simulator's send-and-receive mode) ---
    public String userCallsign = "";    // your call (e.g. "WM3J")
    public String userName     = "";    // first name to send during QSOs
    public String userQth      = "";    // QTH to send during QSOs

    // --- Persistence ---
    public String sessionLogDir = "logs";
    public boolean autoExportSessions = true;

    // --- Localisation ---
    public String language = "en";  // en | es | de | fr | it | pt

    // ------------------------------------------------------------------

    private static AppConfig instance;
    public static synchronized AppConfig get() {
        if (instance == null) {
            instance = load(Paths.get("config", "app-config.json"));
        }
        return instance;
    }

    /** Replace the active config (for tests / runtime UI changes). */
    public static synchronized void set(AppConfig cfg) { instance = cfg; }

    public static AppConfig load(Path path) {
        try {
            File f = path.toFile();
            if (f.exists()) {
                return new ObjectMapper().readValue(f, AppConfig.class);
            }
        } catch (IOException e) {
            Logger.warn("Failed to load config %s: %s", path, e.getMessage());
        }
        return new AppConfig();
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(path.toFile(), this);
        } catch (IOException e) {
            Logger.warn("Failed to save config %s: %s", path, e.getMessage());
        }
    }

    /** Standard PARIS-based dit length in milliseconds for a given WPM. */
    public static double ditMillis(int wpm) {
        if (wpm <= 0) wpm = 1;
        return 1200.0 / wpm;
    }
}
