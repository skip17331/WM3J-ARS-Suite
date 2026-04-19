package com.hamradio.jhub;

import com.google.gson.JsonObject;
import com.hamradio.jhub.model.JHubConfig;
import com.hamradio.jhub.ui.JHubStatusWindow;
import com.hamradio.jhub.ui.SplashJHub;
import javafx.application.Application;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;

/**
 * JHubMain — application entry point.
 *
 * Bootstraps every component in dependency order:
 *   1. ConfigManager    – reads (or creates) j-hub.json
 *   2. StateCache       – empty in-memory state store
 *   3. SpotEnricher     – loads the DXCC prefix table
 *   4. MessageRouter    – wires apps together
 *   5. JHubServer       – opens the WebSocket listener
 *   6. WebConfigServer  – opens the HTTP config UI
 *   7. ClusterManager   – connects to the DX cluster
 *   8. JHubDiscovery    – broadcasts UDP beacon for auto-discovery
 *   9. AppLauncher      – auto-launches j-map, j-log, j-bridge, j-digi
 *  10. JHubStatusWindow – launches the JavaFX status window
 *
 * On JVM shutdown (Ctrl-C, window close, tray Exit) a shutdown hook
 * tears everything down gracefully, broadcasting a SHUTDOWN command to
 * all connected apps before force-killing child processes.
 *
 * A splash screen is ALWAYS shown before bootstrap begins.
 */
public class JHubMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(JHubMain.class);

    // Singleton references kept for the shutdown hook
    private static JHubServer            jHubServer;
    private static WebConfigServer       webConfigServer;
    private static ClusterManager        clusterManager;
    private static JHubDiscovery         jHubDiscovery;
    private static HamlibRigController   rigController;

    // Uptime reference
    public static final Instant START_TIME = Instant.now();

    // ---------------------------------------------------------------
    // JavaFX Application entry (JavaFX calls launch → start)
    // ---------------------------------------------------------------

    @Override
    public void start(javafx.stage.Stage primaryStage) {
        // Splash always shows before bootstrap
        new SplashJHub(() -> {
            try {
                bootstrap();
                JHubStatusWindow window = new JHubStatusWindow(primaryStage, jHubServer);
                window.show();
            } catch (Exception e) {
                log.error("Fatal startup error", e);
                Platform.exit();
            }
        }).show();
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
                config.getJHub().websocketPort, config.getJHub().webConfigPort);

        // 2. State cache
        StateCache cache = StateCache.getInstance();

        // 3. Spot enricher (loads DXCC table from classpath)
        SpotEnricher.getInstance().init();

        // 4. Message router
        MessageRouter router = MessageRouter.getInstance();

        // 5. WebSocket server
        int wsPort = config.getJHub().websocketPort;
        jHubServer = new JHubServer(new InetSocketAddress(wsPort), router, cache);
        jHubServer.start();
        router.setJHubServer(jHubServer);
        log.info("WebSocket server listening on port {}", wsPort);

        // 6. HTTP config UI
        int webPort = config.getJHub().webConfigPort;
        webConfigServer = new WebConfigServer(webPort, router, cache);
        webConfigServer.start();
        log.info("Web config UI at http://localhost:{}", webPort);

        // 7. DX cluster (only if autoConnect is enabled in config)
        clusterManager = ClusterManager.getInstance();
        clusterManager.setRouter(router);
        if (config.getCluster().autoConnect) {
            clusterManager.connect();
        } else {
            log.info("Cluster autoConnect disabled — use Web Config UI to connect");
        }

        // 8. UDP discovery beacon so apps can auto-connect
        jHubDiscovery = new JHubDiscovery();
        jHubDiscovery.start();

        // 9. Auto-launch configured companion apps
        //    j-log and j-bridge receive --launched-by-hub so they skip their splash screens.
        AppLauncher launcher = AppLauncher.getInstance();
        JHubConfig.AppsSection apps = config.getApps();
        if (apps != null) {
            autoLaunch(launcher, "jMap",     apps.jMap,    false);
            autoLaunch(launcher, "j-log",    apps.jLog,    true);
            autoLaunch(launcher, "j-bridge", apps.jBridge, true);
            autoLaunch(launcher, "j-digi",   apps.jDigi,   false);
        }

        // 10. Hamlib rig controller (only when backend = HAMLIB)
        rigController = HamlibRigController.getInstance();
        rigController.setRouter(router);
        if ("HAMLIB".equals(config.getConfig().rig.backend)) {
            rigController.start(config.getConfig().rig);
        } else {
            log.info("Rig backend is '{}' — Hamlib controller not started",
                    config.getConfig().rig.backend);
        }

        // 11. Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(JHubMain::shutdown, "shutdown-hook"));

        log.info("=== j-Hub ready ===");
    }

    // ---------------------------------------------------------------
    // Auto-launch helper
    // ---------------------------------------------------------------

    /**
     * Launch a named app using the given command string.
     * When {@code passHubFlag} is true, appends {@code --launched-by-hub} to the command
     * so that J-Log and J-Bridge know to suppress their own splash screens.
     *
     * @return null on success, or an error message string on failure
     */
    private static void autoLaunch(AppLauncher launcher, String name,
                                   JHubConfig.AppLaunchEntry entry,
                                   boolean passHubFlag) {
        if (entry == null || !entry.autoLaunch) return;
        if (entry.command == null || entry.command.isBlank()) {
            log.warn("Auto-launch enabled for '{}' but no command is configured — skipping", name);
            return;
        }
        String cmd = passHubFlag ? entry.command + " --launched-by-hub" : entry.command;
        String err = launcher.launch(name, cmd);
        if (err != null) {
            log.error("Auto-launch of '{}' failed: {}", name, err);
        }
    }

    // ---------------------------------------------------------------
    // Graceful shutdown — called by JVM shutdown hook
    // ---------------------------------------------------------------

    private static void shutdown() {
        log.info("Shutdown initiated ...");

        // 1. Broadcast SHUTDOWN command to all connected WebSocket clients first
        //    so they can exit cleanly before we force-kill their processes.
        try {
            if (jHubServer != null) {
                JsonObject msg = new JsonObject();
                msg.addProperty("type",    "SHUTDOWN");
                msg.addProperty("command", "shutdown");
                msg.addProperty("source",  "j-hub");
                jHubServer.broadcastToAll(msg.toString());
                log.info("SHUTDOWN broadcast sent to all connected apps — waiting 750 ms");
                Thread.sleep(750);
            }
        } catch (Exception e) { log.warn("Shutdown broadcast error", e); }

        // 2. Force-kill all child processes (any that did not self-terminate)
        try { if (rigController     != null) rigController.stop();           } catch (Exception e) { log.warn("Rig controller shutdown error", e); }
        try { AppLauncher.getInstance().stopAll();                          } catch (Exception e) { log.warn("App launcher shutdown error", e); }
        try { if (jHubDiscovery   != null) jHubDiscovery.stop();           } catch (Exception e) { log.warn("Discovery shutdown error", e); }
        try { if (clusterManager  != null) clusterManager.disconnect();    } catch (Exception e) { log.warn("Cluster shutdown error", e); }
        try { if (webConfigServer != null) webConfigServer.stop();         } catch (Exception e) { log.warn("Web server shutdown error", e); }
        try { if (jHubServer      != null) jHubServer.stop(1000);          } catch (Exception e) { log.warn("WS server shutdown error", e); }
        log.info("J-Hub stopped.");
    }

    // ---------------------------------------------------------------
    // JavaFX stop — called after Platform.exit()
    // ---------------------------------------------------------------

    @Override
    public void stop() {
        System.exit(0);
    }

    // ---------------------------------------------------------------
    // Program entry point
    // ---------------------------------------------------------------

    public static void main(String[] args) {
        launch(args);
    }
}
