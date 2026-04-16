package com.hamradio.digitalbridge;

import com.hamradio.digitalbridge.ui.MainWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DigitalBridgeMain — JavaFX Application entry point for Digital Bridge.
 *
 * Startup sequence (mirrors j-log HamLogApp pattern):
 *   1. LoggingConfigurator — set up SLF4J/Logback
 *   2. ConfigManager.load()— read digitalbridge-config.json
 *   3. Build services      — HubClient, WsjtxUdpListener, MessagePublisher
 *   4. Build MainWindow    — wire all callbacks
 *   5. hub.connect()       — attempt j-hub connection (non-blocking, auto-reconnect)
 *   6. udp.start()         — begin WSJT-X UDP listen (non-blocking, works without hub)
 *
 * The app is fully functional locally even when j-hub is offline.
 * WSJT-X decodes appear in the table regardless of hub connectivity.
 *
 * IMPORTANT: j-hub shuts down when the last app disconnects
 * (HubServer.onClose → System.exit). Digital Bridge must maintain
 * its WebSocket connection while running and disconnect cleanly on exit.
 */
public class DigitalBridgeMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(DigitalBridgeMain.class);

    // Keep service references for clean shutdown in stop()
    private HubClient        hubClient;
    private WsjtxUdpListener udpListener;

    // ── JavaFX init (background thread, before start()) ──────────────────────

    @Override
    public void init() {
        // Logging first so everything below is captured
        ConfigManager cfg = ConfigManager.getInstance();
        LoggingConfigurator.configure(false); // start INFO; user can enable debug later
        cfg.load();
        log.info("=== Digital Bridge v1.0.0 starting  [WM3j ARS Suite] ===");
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

            // Build and show main window (wires all callbacks)
            MainWindow mainWindow = new MainWindow(hubClient, udpListener, publisher, primaryStage);
            mainWindow.show();

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
        log.info("Digital Bridge shutting down");
        if (udpListener != null) udpListener.stop();
        if (hubClient   != null) hubClient.disconnect();
        log.info("Digital Bridge stopped");
    }

    // ── Main ─────────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        launch(args);
    }
}
