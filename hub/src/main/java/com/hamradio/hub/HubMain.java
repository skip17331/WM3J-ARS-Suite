package com.hamradio.hub;

import com.hamradio.hub.model.HubConfig;
import com.hamradio.hub.ui.HubStatusWindow;
import javafx.application.Application;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.time.Instant;

/**
 * HubMain — application entry point.
 *
 * Bootstraps every component in dependency order:
 *   1. ConfigManager  – reads (or creates) hub.json
 *   2. StateCache     – empty in-memory state store
 *   3. SpotEnricher   – loads the DXCC prefix table
 *   4. MessageRouter  – wires apps together
 *   5. HubServer      – opens the WebSocket listener
 *   6. WebConfigServer– opens the HTTP config UI
 *   7. ClusterManager – connects to the DX cluster
 *   8. HubStatusWindow– launches the JavaFX tray window
 *
 * On JVM shutdown (Ctrl-C, window close, tray Exit) a shutdown hook
 * tears everything down gracefully.
 */
public class HubMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(HubMain.class);

    // Singleton references kept for the shutdown hook
    private static HubServer      hubServer;
    private static WebConfigServer webConfigServer;
    private static ClusterManager  clusterManager;
    private static HubDiscovery    hubDiscovery;

    // Uptime reference
    public static final Instant START_TIME = Instant.now();

    // ---------------------------------------------------------------
    // JavaFX Application entry (JavaFX calls launch → start)
    // ---------------------------------------------------------------

    @Override
    public void start(javafx.stage.Stage primaryStage) {
        try {
            bootstrap();
            // Hand the primary stage to the status window
            HubStatusWindow window = new HubStatusWindow(primaryStage, hubServer);
            window.show();
        } catch (Exception e) {
            log.error("Fatal startup error", e);
            Platform.exit();
        }
    }

    // ---------------------------------------------------------------
    // Bootstrap sequence
    // ---------------------------------------------------------------

    private static void bootstrap() throws Exception {
        log.info("=== j-Hub starting  [WM3j ARS Suite] ===");

        // 1. Configuration
        ConfigManager config = ConfigManager.getInstance();
        config.load();
        log.info("Config loaded — WS port {}, web port {}",
                config.getHub().websocketPort, config.getHub().webConfigPort);

        // 2. State cache
        StateCache cache = StateCache.getInstance();

        // 3. Spot enricher (loads DXCC table from classpath)
        SpotEnricher.getInstance().init();

        // 4. Message router
        MessageRouter router = MessageRouter.getInstance();

        // 5. WebSocket server
        int wsPort = config.getHub().websocketPort;
        hubServer = new HubServer(new InetSocketAddress(wsPort), router, cache);
        hubServer.start();
        router.setHubServer(hubServer); // wire immediately so publishSpot/publishRawLine work from first connect
        log.info("WebSocket server listening on port {}", wsPort);

        // 6. HTTP config UI
        int webPort = config.getHub().webConfigPort;
        webConfigServer = new WebConfigServer(webPort, router, cache);
        webConfigServer.start();
        log.info("Web config UI at http://localhost:{}", webPort);

        // 7. DX cluster (non-fatal if it fails to connect initially)
        clusterManager = ClusterManager.getInstance();
        clusterManager.setRouter(router);
        clusterManager.connect();

        // 8. UDP discovery beacon so apps like HamLog can auto-connect
        hubDiscovery = new HubDiscovery();
        hubDiscovery.start();

        // 9. Auto-launch configured apps (hamclock, hamlog)
        AppLauncher launcher = AppLauncher.getInstance();
        HubConfig.AppsSection apps = config.getApps();
        if (apps != null) {
            autoLaunch(launcher, "hamclock", apps.hamclock);
            autoLaunch(launcher, "hamlog",   apps.hamlog);
        }

        // 10. Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(HubMain::shutdown, "shutdown-hook"));

        log.info("=== j-Hub ready ===");
    }

    // ---------------------------------------------------------------
    // Auto-launch helper
    // ---------------------------------------------------------------

    private static void autoLaunch(AppLauncher launcher, String name,
                                   HubConfig.AppLaunchEntry entry) {
        if (entry == null || !entry.autoLaunch) return;
        if (entry.command == null || entry.command.isBlank()) {
            log.warn("Auto-launch enabled for '{}' but no command is configured — skipping", name);
            return;
        }
        String err = launcher.launch(name, entry.command);
        if (err != null) {
            log.error("Auto-launch of '{}' failed: {}", name, err);
        }
    }

    // ---------------------------------------------------------------
    // Graceful shutdown
    // ---------------------------------------------------------------

    private static void shutdown() {
        log.info("Shutdown initiated …");
        try { AppLauncher.getInstance().stopAll();                        }  catch (Exception e) { log.warn("App launcher shutdown error", e); }
        try { if (hubDiscovery    != null) hubDiscovery.stop();          }  catch (Exception e) { log.warn("Discovery shutdown error", e); }
        try { if (clusterManager  != null) clusterManager.disconnect(); }  catch (Exception e) { log.warn("Cluster shutdown error", e); }
        try { if (webConfigServer != null) webConfigServer.stop();      }  catch (Exception e) { log.warn("Web server shutdown error", e); }
        try { if (hubServer       != null) hubServer.stop(1000);        }  catch (Exception e) { log.warn("WS server shutdown error", e); }
        log.info("Hub stopped.");
    }

    // ---------------------------------------------------------------
    // JavaFX stop — called after Platform.exit(); drives JVM exit
    // ---------------------------------------------------------------

    @Override
    public void stop() {
        // Non-daemon threads (Jetty, WebSocket) would keep the JVM alive forever.
        // System.exit triggers the shutdown hook which cleans them up gracefully.
        System.exit(0);
    }

    // ---------------------------------------------------------------
    // Program entry point
    // ---------------------------------------------------------------

    public static void main(String[] args) {
        // JavaFX Application.launch bootstraps the FX toolkit and then
        // calls start().  All heavy lifting happens inside start().
        launch(args);
    }
}
