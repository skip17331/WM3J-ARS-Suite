package com.hamradio.jsat.app;

import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.service.config.JsatSettingsLoader;
import com.hamradio.jsat.ui.main.MainWindow;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * J-Sat — Amateur Satellite Tracking Application
 * WM3J ARS Suite
 *
 * Launch flags:
 *   --hub <hostname>       connect to j-hub on a remote host
 *   --launched-by-hub      skip auto-start of j-hub
 */
public class JSatApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JSatApp.class);

    private static final int JHUB_WS_PORT  = 8080;
    private static final int JHUB_WEB_PORT = 8081;

    private ServiceRegistry serviceRegistry;
    private boolean launchedByHub = false;
    private String  hubHost       = "localhost";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        log.info("=== J-Sat starting [WM3J ARS Suite] ===");

        List<String> raw = getParameters().getRaw();
        launchedByHub = raw.contains("--launched-by-hub");

        int hubIdx = raw.indexOf("--hub");
        if (hubIdx >= 0 && hubIdx + 1 < raw.size()) {
            hubHost = raw.get(hubIdx + 1);
            log.info("Hub host: {}", hubHost);
        }

        boolean hubIsLocal = "localhost".equals(hubHost) || "127.0.0.1".equals(hubHost);
        if (hubIsLocal && !launchedByHub) {
            ensureJHubRunning();
        }

        if (!hubIsLocal) {
            JsatSettingsLoader.setHubHost(hubHost, JHUB_WEB_PORT);
        }

        JsatSettings settings = JsatSettingsLoader.loadOrDefaults();
        settings.hubHost   = hubHost;
        settings.hubWsPort = JHUB_WS_PORT;

        serviceRegistry = new ServiceRegistry(settings);
        serviceRegistry.start();

        log.info("Services started. Station: {} ({}, {})",
            settings.callsign, settings.qthLat, settings.qthLon);
    }

    @Override
    public void start(Stage primaryStage) {
        MainWindow win = new MainWindow(primaryStage, serviceRegistry);
        win.show();
        log.info("J-Sat UI started");
    }

    @Override
    public void stop() {
        log.info("J-Sat shutting down...");
        if (serviceRegistry != null) {
            JsatSettingsLoader.save(serviceRegistry.getSettings());
            serviceRegistry.stop();
        }
        log.info("Shutdown complete");
        System.exit(0);
    }

    // ── J-Hub auto-start ──────────────────────────────────────────────────────

    private static void ensureJHubRunning() {
        if (isPortOpen("localhost", JHUB_WS_PORT, 500)) return;
        log.info("J-Hub not detected — starting...");
        try {
            new ProcessBuilder("bash", "/home/mike/ARS_Suite/j-hub/start.sh", "--no-splash")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
            for (int i = 0; i < 20; i++) {
                Thread.sleep(500);
                if (isPortOpen("localhost", JHUB_WS_PORT, 200)) { log.info("J-Hub ready"); return; }
            }
            log.warn("J-Hub did not start in time");
        } catch (Exception e) {
            log.error("Failed to start J-Hub: {}", e.getMessage());
        }
    }

    private static boolean isPortOpen(String host, int port, int timeoutMs) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) { return false; }
    }
}
