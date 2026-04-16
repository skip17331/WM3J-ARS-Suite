package com.wm3j.jmap.app;

import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.config.SettingsLoader;
import com.wm3j.jmap.ui.main.MainWindow;
import com.wm3j.jmap.web.SetupWebServer;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * J-Map - Main Application Entry Point
 *
 * Cross-platform JavaFX desktop amateur radio operator dashboard.
 * Provides world map, grayline, DX spots, solar/propagation data,
 * aurora/weather/tropo overlays, and great-circle rotor map.
 *
 * Setup Page accessible at: http://localhost:8080/setup
 */
public class JMapApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JMapApp.class);

    private SetupWebServer webServer;
    private ServiceRegistry serviceRegistry;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        log.info("=== j-Map starting  [WM3j ARS Suite] ===");

        // Load persisted settings
        Settings settings = SettingsLoader.loadOrDefaults();
        log.info("Settings loaded: callsign={}, qthLat={}, qthLon={}",
            settings.getCallsign(), settings.getQthLat(), settings.getQthLon());

        // Initialize service registry (wires up all providers)
        serviceRegistry = new ServiceRegistry(settings);
        serviceRegistry.start();

        // Start embedded web server for settings API (used by Hub unified UI)
        webServer = new SetupWebServer(settings, serviceRegistry, settings.getWebServerPort());
        webServer.start();
        log.info("Settings API available at: http://localhost:{}/api/settings", settings.getWebServerPort());
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        MainWindow mainWindow = new MainWindow(primaryStage, serviceRegistry);
        mainWindow.show();
        log.info("Main display started");
    }

    @Override
    public void stop() throws Exception {
        log.info("j-Map shutting down...");
        if (webServer != null) webServer.stop();
        if (serviceRegistry != null) serviceRegistry.stop();
        SettingsLoader.save(serviceRegistry.getSettings());
        log.info("Shutdown complete");
    }
}
