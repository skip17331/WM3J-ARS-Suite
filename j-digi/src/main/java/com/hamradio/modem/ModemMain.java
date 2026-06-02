package com.hamradio.modem;

import com.hamradio.modem.ui.ModemAppContext;
import com.hamradio.modem.ui.SplashScreen;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class ModemMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(ModemMain.class);

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

    /** App version, read from the jar manifest's Implementation-Version
     *  (set by ${project.version} at build time); "dev" when run unpackaged.
     *  Single source so the value can't drift from the pom. */
    public static String version() {
        String v = ModemMain.class.getPackage().getImplementationVersion();
        return (v != null && !v.isBlank()) ? v : "dev";
    }

    private ModemService modemService;
    private boolean      launchedByHub = false;

    @Override
    public void init() throws Exception {
        CrashHandler.install("J-Digi");
        launchedByHub = getParameters().getRaw().contains("--launched-by-hub");
        if (!launchedByHub) {
            ensureJHubRunning();
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("J-Digi");
        try {
            com.jlog.db.DatabaseManager.getInstance().initAll();
        } catch (Exception ignored) {}
        modemService = new ModemService();
        ModemAppContext context = new ModemAppContext(modemService);
        ModemAppContext.applyIcon(primaryStage);
        new SplashScreen(primaryStage, context, launchedByHub).show();
    }

    @Override
    public void stop() {
        if (modemService != null) {
            modemService.stopAudio();
            modemService.disconnectHub();
        }
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

    public static void main(String[] args) {
        launch(args);
    }
}
