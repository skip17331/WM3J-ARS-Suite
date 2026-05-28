package com.jlog.app;

import com.jlog.cluster.HubDiscoveryListener;
import com.jlog.cluster.HubEngine;
import com.jlog.cluster.JLogUiPresence;
import com.jlog.db.DatabaseManager;
import com.jlog.i18n.I18n;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import com.jlog.util.LoggingConfigurator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class JLogApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JLogApp.class);

    /** Path to the j-hub start script. Resolved in order:
     *  (1) {@code $ARS_SUITE_HOME/j-hub/start.sh} if env var is set,
     *  (2) {@code $HOME/ARS_Suite/j-hub/start.sh} otherwise. */
    private static final String JHUB_START = resolveJHubStart();
    private static final int    JHUB_PORT  = 8080;

    private static String resolveJHubStart() {
        String root = System.getenv("ARS_SUITE_HOME");
        if (root == null || root.isBlank()) {
            root = System.getProperty("user.home", "") + "/ARS_Suite";
        }
        return root + "/j-hub/start.sh";
    }

    private boolean engineOnly    = false;
    private boolean launchedByHub = false;

    @Override
    public void init() throws Exception {
        CrashHandler.install("J-Log");
        engineOnly    = getParameters().getRaw().contains("--engine-only");
        launchedByHub = getParameters().getRaw().contains("--launched-by-hub");

        AppConfig.getInstance().load();
        LoggingConfigurator.configure(AppConfig.getInstance().isDebugMode());
        I18n.load(resolveLanguage());
        DatabaseManager.getInstance().initAll();
        PluginLoader.getInstance().init();

        if (!engineOnly && !launchedByHub) {
            ensureJHubRunning();
        }

        log.info("j-Log initialised — version 1.0.0{}{}",
            engineOnly    ? " [engine-only]"     : "",
            launchedByHub ? " [launched-by-hub]" : "");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (engineOnly) {
            // No UI — stay alive and serve as the logging engine
            Platform.setImplicitExit(false);
            HubEngine engine = HubEngine.getInstance();
            engine.setOnShutdown(() -> Platform.exit());

            HubDiscoveryListener discovery = new HubDiscoveryListener();
            discovery.setOnHubFound(url -> {
                if (!engine.isConnected()) {
                    Thread t = new Thread(() -> engine.connect(url), "engine-reconnect");
                    t.setDaemon(true);
                    t.start();
                }
            });
            discovery.start();

            Thread t = new Thread(() -> engine.connect("ws://localhost:" + JHUB_PORT), "engine-connect");
            t.setDaemon(true);
            t.start();
            return;
        }

        applyIcon(primaryStage);
        // Respond to hub SHUTDOWN broadcasts (e.g. "Save Data & Restart J-Log"
        // from the web UI) by closing the UI. The stop() hook then flushes
        // the WebSocket + closes all SQLite connections cleanly.
        HubEngine.getInstance().setOnShutdown(() -> Platform.exit());
        new SplashScreen(primaryStage, launchedByHub).show();
    }

    @Override
    public void stop() {
        log.info("j-Log shutting down");
        JLogUiPresence.getInstance().disconnect();
        HubEngine.getInstance().disconnect();
        DatabaseManager.getInstance().closeAll();
    }

    // ── Icon helpers ─────────────────────────────────────────────────────────

    private static Image appIcon;

    public static Image getAppIcon() {
        if (appIcon == null) {
            var stream = JLogApp.class.getResourceAsStream("/com/jlog/icons/icon.png");
            if (stream != null) appIcon = new Image(stream);
        }
        return appIcon;
    }

    public static void applyIcon(Stage stage) {
        Image icon = getAppIcon();
        if (icon != null) {
            stage.getIcons().clear();
            stage.getIcons().add(icon);
        }
    }

    // ── Theme helper ─────────────────────────────────────────────────────────

    public static void applyTheme(Scene scene) {
        AppConfig cfg = AppConfig.getInstance();
        scene.getStylesheets().clear();
        String base = Objects.requireNonNull(
            JLogApp.class.getResource("/com/jlog/css/base.css")).toExternalForm();
        java.net.URL themeFile = JLogApp.class.getResource(
            "/com/jlog/css/" + cfg.getTheme() + ".css");
        scene.getStylesheets().add(base);
        if (themeFile != null) scene.getStylesheets().add(themeFile.toExternalForm());
        FontScaler.apply(scene);
        // Density preset (compact / comfortable / spacious) multiplies the
        // user's base font size before it lands on the scene root.
        double scale = switch (cfg.getDensity()) {
            case "compact"  -> 0.92;
            case "spacious" -> 1.12;
            default          -> 1.0;
        };
        int px = (int) Math.round(cfg.getFontSize() * scale);
        scene.getRoot().setStyle("-fx-font-size: " + px + "px;");
    }

    // ── j-Hub auto-start ─────────────────────────────────────────────────────

    private static void ensureJHubRunning() {
        if (isPortOpen(JHUB_PORT, 500)) return;
        log.info("j-Hub not detected — starting j-Hub...");
        try {
            new ProcessBuilder("bash", JHUB_START, "--no-splash")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (isPortOpen(JHUB_PORT, 200)) {
                    log.info("j-Hub ready");
                    return;
                }
            }
            log.warn("j-Hub did not become available within 10 seconds");
        } catch (Exception e) {
            log.error("Failed to start j-Hub: {}", e.getMessage());
        }
    }

    private static boolean isPortOpen(int port, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            // 127.0.0.1 not "localhost" — see project_hamlib_loopback_ipv6_ipv4_bug
            s.connect(new java.net.InetSocketAddress("127.0.0.1", port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Prefer the language set in j-hub.json (the suite-wide dropdown in the
     * j-Hub config UI) over the local j-log Preferences value. Falls back to
     * AppConfig if j-hub.json is missing or unreadable — keeps j-log runnable
     * standalone.
     */
    private static String resolveLanguage() {
        String home   = System.getProperty("user.home", "");
        java.nio.file.Path[] candidates = new java.nio.file.Path[] {
            java.nio.file.Paths.get(home, "ARS_Suite", "j-hub", "j-hub.json"),
            java.nio.file.Paths.get(".", "j-hub.json"),
        };
        for (java.nio.file.Path p : candidates) {
            if (!java.nio.file.Files.isReadable(p)) continue;
            try {
                com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(p.toFile());
                String lang = root.path("station").path("language").asText("");
                if (!lang.isBlank()) return lang;
            } catch (Exception ignored) {}
        }
        return AppConfig.getInstance().getLanguage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
