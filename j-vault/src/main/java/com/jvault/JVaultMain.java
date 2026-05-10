package com.jvault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

/**
 * JVaultMain — application entry point.
 *
 * Starts the embedded Jetty web server (default port 8083) and opens the UI
 * in the user's default browser. The actual UI lives entirely in the
 * browser at http://localhost:8083 — there is no native window.
 *
 * Args:
 *   --no-browser   skip the browser-open (used when J-Hub embeds the UI in
 *                  an iframe, so the user doesn't get a redundant tab)
 *
 * System properties:
 *   -Djvault.port=NNNN   override the default port (8083)
 */
public class JVaultMain {

    private static final Logger log = LoggerFactory.getLogger(JVaultMain.class);

    public static void main(String[] args) throws Exception {
        boolean openBrowser = true;
        for (String a : args) {
            if ("--no-browser".equals(a) || "--launched-by-hub".equals(a)) openBrowser = false;
        }

        int port = Integer.getInteger("jvault.port", 8083);

        JVaultServer server = new JVaultServer(port);
        try {
            server.start();
        } catch (Exception e) {
            log.error("J-Vault failed to start web server: {}", e.getMessage(), e);
            System.err.println("J-Vault failed to start: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Touch the DAO once to trigger schema creation + migration.
        try {
            InventoryDao.getInstance().listTypes();
        } catch (Exception e) {
            log.warn("Initial DAO touch failed: {}", e.getMessage());
        }

        String url = "http://localhost:" + port + "/";
        log.info("J-Vault running on {}", url);
        System.out.println("J-Vault running on " + url);

        if (openBrowser) openBrowser(url);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down J-Vault");
            try { server.stop(); } catch (Exception ignored) {}
        }, "jvault-shutdown"));

        // Keep the main thread alive — Jetty runs on its own threads, but the
        // JVM exits if main returns and no non-daemon threads remain.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException ignored) {}
    }

    /** Open a URL in the system default browser. Tries AWT {@code Desktop}
     *  first (works on all three platforms when a display is up) and falls
     *  back to a per-OS shell command for headless or stripped-down JDKs. */
    private static void openBrowser(String url) {
        try {
            URI uri = URI.create(url);
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String[] cmd = os.contains("win") ? new String[]{"cmd", "/c", "start", "", url}
                         : os.contains("mac") ? new String[]{"open", url}
                         :                      new String[]{"xdg-open", url};
            new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            log.warn("openBrowser failed: {}", e.getMessage());
        }
    }
}
