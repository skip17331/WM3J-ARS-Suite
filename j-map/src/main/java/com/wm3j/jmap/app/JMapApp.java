package com.wm3j.jmap.app;

import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.config.SettingsLoader;
import com.wm3j.jmap.ui.main.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * J-Map entry point.
 *
 * Headless configuration — no local web server.
 * All settings come from J-Hub (port 8081/8080) and are delivered via WebSocket.
 *
 * Mode detection:
 *   Local  — J-Hub WS reachable at startup → config via WebSocket (JMAP_CONFIG messages)
 *   Remote — J-Hub not reachable           → config from /config/jmap_config.json
 */
public class JMapApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JMapApp.class);

    /** Path to the j-hub start script. Resolved in order:
     *  (1) {@code $ARS_SUITE_HOME/j-hub/start.sh} if env var is set,
     *  (2) {@code $HOME/ARS_Suite/j-hub/start.sh} otherwise. */
    private static final String JHUB_START    = resolveJHubStart();
    private static final int    JHUB_WS_PORT  = 8080;
    private static final int    JHUB_WEB_PORT = 8081;

    private static String resolveJHubStart() {
        String root = System.getenv("ARS_SUITE_HOME");
        if (root == null || root.isBlank()) {
            root = System.getProperty("user.home", "") + "/ARS_Suite";
        }
        return root + "/j-hub/start.sh";
    }

    private ServiceRegistry serviceRegistry;
    private boolean         launchedByHub = false;
    private String          hubHost       = "localhost";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        CrashHandler.install("J-Map");
        log.info("=== J-Map starting [WM3J ARS Suite] — headless mode ===");

        List<String> raw = getParameters().getRaw();
        launchedByHub = raw.contains("--launched-by-hub");

        int hubIdx = raw.indexOf("--hub");
        if (hubIdx >= 0 && hubIdx + 1 < raw.size()) {
            hubHost = raw.get(hubIdx + 1);
            log.info("Hub host overridden to: {}", hubHost);
        }

        boolean hubIsLocal = "localhost".equals(hubHost) || "127.0.0.1".equals(hubHost);

        // Auto-start J-Hub if local and not already running
        if (hubIsLocal && !launchedByHub) {
            ensureJHubRunning();
        }

        // Detect mode
        boolean hubReachable = isPortOpen(hubHost, JHUB_WS_PORT, 1000);
        if (hubReachable) {
            log.info("J-Hub reachable — LOCAL mode: config via WebSocket");
        } else {
            log.info("J-Hub not reachable — REMOTE mode: config from /config/jmap_config.json");
        }

        // Load initial settings
        if (!hubIsLocal) {
            SettingsLoader.setJHubHost(hubHost, JHUB_WEB_PORT);
        }
        Settings settings = SettingsLoader.loadOrDefaults(hubReachable);
        log.info("Settings loaded: callsign={}, lat={}, lon={}",
            settings.getCallsign(), settings.getQthLat(), settings.getQthLon());

        serviceRegistry = new ServiceRegistry(settings);
        serviceRegistry.setJHubHost(hubHost);

        if (!hubIsLocal) {
            serviceRegistry.dxClusterClient.setHubHost(hubHost, JHUB_WS_PORT);
        }
        serviceRegistry.start();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SplashScreen.applyIcon(primaryStage);
        if (launchedByHub) {
            MainWindow mainWindow = new MainWindow(primaryStage, serviceRegistry);
            mainWindow.show();
            log.info("Main display started (hub-launched)");
        } else {
            new SplashScreen(() -> {
                MainWindow mainWindow = new MainWindow(primaryStage, serviceRegistry);
                mainWindow.show();
                log.info("Main display started");
            }).show();
        }
    }

    @Override
    public void stop() {
        log.info("J-Map shutting down...");
        try {
            if (serviceRegistry != null) serviceRegistry.stop();
        } catch (Exception e) {
            log.warn("Service registry stop error: {}", e.getMessage());
        }
        log.info("Shutdown complete");
        System.exit(0);
    }

    // ── J-Hub auto-start ─────────────────────────────────────────────────────

    private static void ensureJHubRunning() {
        if (isPortOpen("localhost", JHUB_WS_PORT, 500)) return;
        log.info("J-Hub not detected — starting J-Hub...");
        try {
            new ProcessBuilder("bash", JHUB_START, "--no-splash")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (isPortOpen("localhost", JHUB_WS_PORT, 200)) {
                    log.info("J-Hub ready");
                    return;
                }
            }
            log.warn("J-Hub did not become available within 10 seconds");
        } catch (Exception e) {
            log.error("Failed to start J-Hub: {}", e.getMessage());
        }
    }

    static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
