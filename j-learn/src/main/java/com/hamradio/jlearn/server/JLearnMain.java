package com.hamradio.jlearn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.Locale;

/**
 * J-Learn entry point. Starts the embedded Jetty server on the configured
 * port (default 8082) and opens the UI in the user's default browser.
 * No JavaFX, no desktop window — J-Learn is purely a web app accessible at
 * http://localhost:8082/, embeddable in J-Hub via iframe, or visitable
 * directly from any browser on the LAN.
 *
 * Args:
 *   --no-browser        skip the browser-open (used when J-Hub embeds the
 *                       UI in an iframe so the user doesn't get a redundant
 *                       tab); {@code --launched-by-hub} is accepted as a
 *                       synonym for consistency with the rest of the suite.
 *
 * System properties:
 *   -Djlearn.port=NNNN  override the default port (8082); also set via
 *                       {@code port} in {@code ~/.j-learn/settings.json}.
 */
public final class JLearnMain {

    private static final Logger log = LoggerFactory.getLogger(JLearnMain.class);

    private JLearnMain() {}

    public static void main(String[] args) throws Exception {
        boolean openBrowser = true;
        for (String a : args) {
            if ("--no-browser".equals(a) || "--launched-by-hub".equals(a)) openBrowser = false;
        }

        Settings settings = Settings.load();
        int port = Integer.getInteger("jlearn.port", settings.port);

        ContentResolver content = new ContentResolver();
        content.seedIfEmpty();

        JLearnServer server = new JLearnServer(port, content);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down J-Learn.");
            server.stop();
        }, "jlearn-shutdown"));

        server.start();
        String url = "http://localhost:" + port + "/";
        log.info("J-Learn ready at {}  (content: {})", url, content.contentDir());

        if (openBrowser) openBrowser(url);

        server.join();
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
