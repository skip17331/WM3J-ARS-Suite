package com.hamradio.jbridge;

import com.hamradio.jbridge.ui.MainWindow;
import com.hamradio.jbridge.ui.SplashJBridge;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * JBridgeMain — JavaFX Application entry point for J-Bridge.
 *
 * Startup sequence:
 *   1. LoggingConfigurator — set up SLF4J/Logback
 *   2. ConfigManager.load()— read j-bridge-config.json
 *   3. Build services      — HubClient, WsjtxUdpListener, MessagePublisher
 *   4. Splash / main window:
 *        • If launched directly (no --launched-by-hub arg): show splash,
 *          then open main window when splash completes.
 *        • If launched by J-Hub (--launched-by-hub present): open main window
 *          immediately (no splash).
 *   5. hub.connect()       — attempt j-hub connection (non-blocking, auto-reconnect)
 *   6. udp.start()         — begin WSJT-X UDP listen (non-blocking)
 *
 * The app is fully functional locally even when j-hub is offline.
 * WSJT-X decodes appear in the table regardless of hub connectivity.
 */
public class JBridgeMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(JBridgeMain.class);

    private static final String JHUB_START = "/home/mike/ARS_Suite/j-hub/start.sh";
    private static final int    JHUB_PORT  = 8080;

    // Set true when J-Hub passes --launched-by-hub on the command line
    private boolean launchedByHub = false;

    // Keep service references for clean shutdown in stop()
    private HubClient        hubClient;
    private WsjtxUdpListener udpListener;

    // ── JavaFX init (background thread, before start()) ──────────────────────

    @Override
    public void init() {
        ConfigManager cfg = ConfigManager.getInstance();
        LoggingConfigurator.configure(false); // start INFO; user can enable debug later
        cfg.load();
        launchedByHub = getParameters().getRaw().contains("--launched-by-hub");
        log.info("=== J-Bridge v1.0.0 starting  [WM3j ARS Suite] ===");
        if (launchedByHub) {
            log.info("Launched by J-Hub — splash suppressed");
        } else {
            ensureJHubRunning();
        }
    }

    // ── JavaFX start (FX Application Thread) ─────────────────────────────────

    @Override
    public void start(Stage primaryStage) {
        try {
            ConfigManager cfg = ConfigManager.getInstance();

            // Build services
            hubClient   = new HubClient();
            udpListener = new WsjtxUdpListener(cfg.getWsjtxUdpPort(), cfg.getWsjtxBindAddress());
            MessagePublisher publisher = new MessagePublisher(hubClient);

            // Show the main window (after splash if not hub-launched)
            Runnable showMain = () -> {
                MainWindow mainWindow = new MainWindow(hubClient, udpListener, publisher, primaryStage);
                mainWindow.show();
            };

            SplashJBridge.applyIcon(primaryStage);

            if (launchedByHub) {
                // Skip splash — show main window directly
                showMain.run();
            } else {
                // Show splash first; main window opens when splash completes
                new SplashJBridge(showMain).show();
            }

            // Connect to hub — non-blocking, hub discovery also starts inside connect()
            log.info("Connecting to j-Hub at {}", cfg.getHubUri());
            hubClient.connect(cfg.getHubUri());

            // Start UDP listener — works independently of hub
            log.info("Starting WSJT-X UDP listener on port {}", cfg.getWsjtxUdpPort());
            udpListener.start();

        } catch (Exception e) {
            log.error("Fatal startup error", e);
            Platform.exit();
        }
    }

    // ── JavaFX stop (called after Platform.exit()) ────────────────────────────

    @Override
    public void stop() {
        log.info("J-Bridge shutting down");
        if (udpListener != null) udpListener.stop();
        if (hubClient   != null) hubClient.disconnect();
        log.info("J-Bridge stopped");
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
                if (isPortOpen(JHUB_PORT, 200)) { log.info("j-Hub ready"); return; }
            }
            log.warn("j-Hub did not become available within 10 seconds");
        } catch (Exception e) {
            log.error("Failed to start j-Hub: {}", e.getMessage());
        }
    }

    private static boolean isPortOpen(int port, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new InetSocketAddress("localhost", port), timeoutMs);
            return true;
        } catch (Exception e) { return false; }
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
