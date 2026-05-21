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

    /** True when running on Windows — selects {@code cmd}/{@code start.bat}
     *  over {@code bash}/{@code start.sh} for the local J-Hub auto-start. */
    private static final boolean IS_WINDOWS =
        System.getProperty("os.name", "").toLowerCase().contains("win");

    /** Path to the j-hub start script — {@code start.bat} on Windows,
     *  {@code start.sh} elsewhere. Root resolved in order:
     *  (1) {@code $ARS_SUITE_HOME} if the env var is set,
     *  (2) {@code $HOME/ARS_Suite} otherwise. */
    private static final String JHUB_START    = resolveJHubStart();

    private static String resolveJHubStart() {
        String root = System.getenv("ARS_SUITE_HOME");
        if (root == null || root.isBlank()) {
            root = System.getProperty("user.home", "") + java.io.File.separator + "ARS_Suite";
        }
        String script = IS_WINDOWS ? "start.bat" : "start.sh";
        return java.nio.file.Path.of(root, "j-hub", script).toString();
    }

    private ServiceRegistry serviceRegistry;
    private boolean         launchedByHub = false;
    private String          hubHost       = "localhost";
    private int             hubWsPort     = 8080;
    private int             hubWebPort    = 8081;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        CrashHandler.install("J-Map");
        com.wm3j.jmap.i18n.I18n.load("en"); // base; JMAP_CONFIG / station.language may swap
        log.info("=== J-Map starting [WM3J ARS Suite] — headless mode ===");

        List<String> raw = getParameters().getRaw();
        launchedByHub = raw.contains("--launched-by-hub");

        // Bootstrap precedence: CLI flag > local settings.json > defaults.
        // We always read the local file first, then let CLI args win.
        Settings boot = SettingsLoader.peekBootstrap();
        hubHost    = boot.getJhubHost();
        hubWsPort  = boot.getJhubWsPort();
        hubWebPort = boot.getJhubWebPort();

        boolean cliOverride = false;
        int hubIdx = raw.indexOf("--hub");
        if (hubIdx >= 0 && hubIdx + 1 < raw.size()) {
            hubHost = raw.get(hubIdx + 1);
            cliOverride = true;
        }
        int wsIdx  = raw.indexOf("--hub-ws-port");
        if (wsIdx >= 0 && wsIdx + 1 < raw.size()) {
            try { hubWsPort = Integer.parseInt(raw.get(wsIdx + 1)); cliOverride = true; }
            catch (NumberFormatException ignored) {}
        }
        int webIdx = raw.indexOf("--hub-web-port");
        if (webIdx >= 0 && webIdx + 1 < raw.size()) {
            try { hubWebPort = Integer.parseInt(raw.get(webIdx + 1)); cliOverride = true; }
            catch (NumberFormatException ignored) {}
        }
        if (cliOverride) {
            SettingsLoader.saveBootstrap(hubHost, hubWsPort, hubWebPort);
        }
        log.info("J-Hub bootstrap: {}:{} (web {})", hubHost, hubWsPort, hubWebPort);

        boolean hubIsLocal = "localhost".equals(hubHost) || "127.0.0.1".equals(hubHost);

        // Auto-start J-Hub if local and not already running
        if (hubIsLocal && !launchedByHub) {
            ensureJHubRunning(hubWsPort);
        }

        // Detect mode — quick TCP probe of the WS port
        boolean hubReachable = isPortOpen(hubHost, hubWsPort, 1500);
        if (hubReachable) {
            log.info("J-Hub reachable at {}:{} — LOCAL mode: config via WebSocket",
                hubHost, hubWsPort);
        } else {
            log.info("J-Hub not reachable at {}:{} — REMOTE mode: config from /config/jmap_config.json",
                hubHost, hubWsPort);
        }

        // Load initial settings — always tell SettingsLoader where J-Hub lives
        // (not just when remote) so the HTTP fetch URL respects custom ports.
        SettingsLoader.setJHubHost(hubHost, hubWebPort);
        Settings settings = SettingsLoader.loadOrDefaults(hubReachable);
        // If J-Hub returned settings that don't carry our bootstrap fields,
        // restore them from boot so we don't drop the remote address.
        if ("localhost".equals(settings.getJhubHost()) && !"localhost".equals(hubHost)) {
            settings.setJhubHost(hubHost);
            settings.setJhubWsPort(hubWsPort);
            settings.setJhubWebPort(hubWebPort);
        }
        log.info("Settings loaded: callsign={}, lat={}, lon={}",
            settings.getCallsign(), settings.getQthLat(), settings.getQthLon());

        serviceRegistry = new ServiceRegistry(settings);
        serviceRegistry.setJHubHost(hubHost);

        // Always pin the hub host on the WebSocket client — UDP discovery would
        // otherwise replace our remote address with whatever beacon arrives last.
        serviceRegistry.dxClusterClient.setHubHost(hubHost, hubWsPort);
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

    private static void ensureJHubRunning(int wsPort) {
        if (isPortOpen("localhost", wsPort, 500)) return;
        log.info("J-Hub not detected — starting J-Hub...");
        try {
            ProcessBuilder pb = IS_WINDOWS
                ? new ProcessBuilder("cmd", "/c", JHUB_START, "--no-splash")
                : new ProcessBuilder("bash", JHUB_START, "--no-splash");
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (isPortOpen("localhost", wsPort, 200)) {
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
