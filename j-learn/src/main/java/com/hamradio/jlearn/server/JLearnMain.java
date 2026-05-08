package com.hamradio.jlearn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * J-Learn entry point. Starts the embedded Jetty server on the configured
 * port (default 8082) and blocks until SIGINT / SIGTERM. No JavaFX, no
 * desktop window — J-Learn is purely a web app accessible at
 * http://localhost:8082/, embeddable in J-Hub via iframe, or visitable
 * directly from any browser on the LAN.
 *
 * Override the port with {@code -Djlearn.port=NNNN} or by setting
 * {@code port} in {@code ~/.j-learn/settings.json}.
 */
public final class JLearnMain {

    private static final Logger log = LoggerFactory.getLogger(JLearnMain.class);

    private JLearnMain() {}

    public static void main(String[] args) throws Exception {
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
        log.info("J-Learn ready at http://localhost:{}/  (content: {})",
                 port, content.contentDir());
        server.join();
    }
}
