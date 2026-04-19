package com.wm3j.jmap.app;

import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.config.SettingsLoader;
import com.wm3j.jmap.ui.main.MainWindow;
import com.wm3j.jmap.web.SetupWebServer;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JMapApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JMapApp.class);

    private static final String JHUB_START = "/home/mike/ARS_Suite/j-hub/start.sh";
    private static final int    JHUB_PORT  = 8080;

    private SetupWebServer  webServer;
    private ServiceRegistry serviceRegistry;
    private boolean         launchedByHub = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        log.info("=== j-Map starting  [WM3j ARS Suite] ===");

        launchedByHub = getParameters().getRaw().contains("--launched-by-hub");
        if (!launchedByHub) {
            ensureJHubRunning();
        }

        Settings settings = SettingsLoader.loadOrDefaults();
        log.info("Settings loaded: callsign={}, qthLat={}, qthLon={}",
            settings.getCallsign(), settings.getQthLat(), settings.getQthLon());

        serviceRegistry = new ServiceRegistry(settings);
        serviceRegistry.start();

        webServer = new SetupWebServer(settings, serviceRegistry, settings.getWebServerPort());
        webServer.start();
        log.info("Settings API available at: http://localhost:{}/api/settings", settings.getWebServerPort());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SplashScreen.applyIcon(primaryStage);
        if (launchedByHub) {
            MainWindow mainWindow = new MainWindow(primaryStage, serviceRegistry);
            mainWindow.show();
            log.info("Main display started (hub-launched, no splash)");
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
        log.info("j-Map shutting down...");
        try {
            if (webServer != null) webServer.stop();
        } catch (Exception e) {
            log.warn("Web server stop error: {}", e.getMessage());
        }
        try {
            if (serviceRegistry != null) serviceRegistry.stop();
        } catch (Exception e) {
            log.warn("Service registry stop error: {}", e.getMessage());
        }
        try {
            if (serviceRegistry != null) SettingsLoader.save(serviceRegistry.getSettings());
        } catch (Exception e) {
            log.warn("Settings save error: {}", e.getMessage());
        }
        log.info("Shutdown complete");
        System.exit(0);
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
            s.connect(new java.net.InetSocketAddress("localhost", port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
