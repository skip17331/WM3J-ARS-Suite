package com.hamradio.jlearn.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Embedded Jetty for the standalone J-Learn web app.
 *
 * Endpoints:
 *   GET  /                          → index.html
 *   GET  /api/jlearn/manifest       → parsed manifest as JSON array
 *   GET  /api/jlearn/content?id=X-Y → raw markdown for a single section
 *   GET  /api/health                → liveness probe
 *
 * Static assets live on the classpath under {@code /web/}; markdown
 * content lives at {@code ~/.j-learn/content/} (preferred) with a
 * jar-bundled fallback at {@code /content/}.
 */
public final class JLearnServer {

    private static final Logger log = LoggerFactory.getLogger(JLearnServer.class);

    /** Matches a row in manifest.md: {@code | NN-NN | title | path | level |}. */
    private static final Pattern ROW = Pattern.compile(
            "^\\|\\s*(\\d{2}-\\d{2})\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([^|]+?)\\s*\\|\\s*([a-z]+)\\s*\\|");

    private final int port;
    private final ContentResolver content;
    /** True when launched by J-Hub — in that case J-Hub owns our lifecycle
     *  and we skip the browser-driven auto-shutdown. */
    private final boolean hubManaged;
    private Server server;
    private HeartbeatWatchdog watchdog;

    public JLearnServer(int port, ContentResolver content) {
        this(port, content, false);
    }

    public JLearnServer(int port, ContentResolver content, boolean hubManaged) {
        this.port = port;
        this.content = content;
        this.hubManaged = hubManaged;
    }

    public void start() throws Exception {
        server = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");

        ctx.addServlet(new ServletHolder(new JLearnApiServlet(content)), "/api/jlearn/*");
        ctx.addServlet(new ServletHolder(new HealthServlet()),           "/api/health");

        // Browser-presence tracking: heartbeat pings keep the JVM alive when
        // launched standalone; explicit /api/close beacon shuts it down on
        // tab close. Disabled when J-Hub is managing our lifecycle.
        if (!hubManaged) {
            watchdog = new HeartbeatWatchdog();
            ctx.addServlet(new ServletHolder(new HeartbeatServlet(watchdog)), "/api/heartbeat");
            ctx.addServlet(new ServletHolder(new CloseServlet()),             "/api/close");
        }

        ctx.addServlet(new ServletHolder(new StaticServlet()),           "/*");

        server.setHandler(ctx);
        server.start();
        log.info("J-Learn web UI listening on http://localhost:{}", port);

        if (watchdog != null) watchdog.start();
    }

    public void stop() {
        if (server != null) {
            try { server.stop(); } catch (Exception ignore) {}
        }
    }

    public void join() throws InterruptedException {
        if (server != null) server.join();
    }

    // ----- helpers --------------------------------------------------------

    private static void json(HttpServletResponse res, String body) throws IOException {
        res.setContentType("application/json; charset=utf-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        try (PrintWriter w = res.getWriter()) { w.write(body); }
    }

    // ----- /api/health ---------------------------------------------------

    private static class HealthServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, "{\"ok\":true,\"app\":\"j-learn\",\"version\":\"1.1.0\"}");
        }
    }

    // ----- /api/jlearn ---------------------------------------------------

    private static class JLearnApiServlet extends HttpServlet {
        private final ContentResolver content;
        JLearnApiServlet(ContentResolver content) { this.content = content; }

        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if ("/manifest".equals(path)) { writeManifest(res);            return; }
            if ("/content".equals(path))  { writeContent(req, res);        return; }
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json(res, "{\"error\":\"unknown jlearn endpoint\"}");
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            res.setHeader("Access-Control-Allow-Origin",  "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type");
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private void writeManifest(HttpServletResponse res) throws IOException {
            String md = content.readManifest();
            if (md == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"manifest.md not found\"}");
                return;
            }
            JsonArray entries = new JsonArray();
            for (String line : md.split("\\R")) {
                Matcher m = ROW.matcher(line);
                if (!m.find()) continue;
                JsonObject row = new JsonObject();
                String id = m.group(1);
                row.addProperty("id",      id);
                row.addProperty("title",   m.group(2));
                row.addProperty("path",    m.group(3));
                row.addProperty("level",   m.group(4));
                row.addProperty("chapter", id.substring(0, 2));
                row.addProperty("section", id.substring(3, 5));
                entries.add(row);
            }
            json(res, entries.toString());
        }

        private void writeContent(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String id = req.getParameter("id");
            if (id == null || !id.matches("\\d{2}-\\d{2}")) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"id query parameter required (NN-NN)\"}");
                return;
            }
            String md = content.readManifest();
            String relPath = null;
            if (md != null) {
                for (String line : md.split("\\R")) {
                    Matcher m = ROW.matcher(line);
                    if (m.find() && id.equals(m.group(1))) { relPath = m.group(3); break; }
                }
            }
            if (relPath == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"unknown id " + id + "\"}");
                return;
            }
            String body = content.readSection(relPath);
            if (body == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"content file missing: " + relPath + "\"}");
                return;
            }
            res.setContentType("text/markdown; charset=utf-8");
            res.setHeader("Access-Control-Allow-Origin", "*");
            try (PrintWriter w = res.getWriter()) { w.write(body); }
        }
    }

    // ----- static files --------------------------------------------------

    private static class StaticServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) path = "/index.html";

            try (InputStream is = getClass().getResourceAsStream("/web" + path)) {
                if (is == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }
                if      (path.endsWith(".html")) res.setContentType("text/html; charset=utf-8");
                else if (path.endsWith(".js"))   res.setContentType("application/javascript; charset=utf-8");
                else if (path.endsWith(".css"))  res.setContentType("text/css; charset=utf-8");
                else if (path.endsWith(".json")) res.setContentType("application/json; charset=utf-8");
                else if (path.endsWith(".md"))   res.setContentType("text/markdown; charset=utf-8");
                else if (path.endsWith(".png"))  res.setContentType("image/png");
                else if (path.endsWith(".svg"))  res.setContentType("image/svg+xml");
                else                             res.setContentType("application/octet-stream");
                byte[] bytes = is.readAllBytes();
                res.setContentLength(bytes.length);
                res.getOutputStream().write(bytes);
            }
        }
    }

    // For tests / parent-process control.
    @SuppressWarnings("unused")
    int port() { return port; }

    // ----- Browser-presence watchdog -------------------------------------
    //
    // The web UI POSTs /api/heartbeat every few seconds while it's open
    // and POSTs /api/close as a sendBeacon when the tab/window closes.
    // If we go STALE_MS without a ping (after the first one has been
    // received), assume the browser is gone and exit the JVM.
    //
    // Initial grace: the watchdog waits for the first ping before it starts
    // policing — that way the JVM stays alive in the gap between server
    // start and the user actually opening the browser.

    private static class HeartbeatWatchdog {
        // Tunings: ping every ~4s from the browser, give up after ~12s.
        private static final long STALE_MS = 12_000L;

        private final AtomicLong lastPing = new AtomicLong(0L);

        void heartbeat() {
            lastPing.set(System.currentTimeMillis());
        }

        void start() {
            Thread t = new Thread(this::loop, "jlearn-heartbeat-watchdog");
            t.setDaemon(true);
            t.start();
        }

        private void loop() {
            try {
                while (true) {
                    Thread.sleep(2_000L);
                    long last = lastPing.get();
                    if (last == 0L) continue; // haven't seen the browser yet
                    long age = System.currentTimeMillis() - last;
                    if (age > STALE_MS) {
                        log.info("Browser heartbeat stale ({} ms) — shutting down J-Learn", age);
                        System.exit(0);
                    }
                }
            } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
    }

    private static class HeartbeatServlet extends HttpServlet {
        private final HeartbeatWatchdog watchdog;
        HeartbeatServlet(HeartbeatWatchdog w) { this.watchdog = w; }
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            watchdog.heartbeat();
            json(res, "{\"ok\":true}");
        }
        // Allow GET too — easier to test, and sendBeacon falls back to POST anyway.
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            watchdog.heartbeat();
            json(res, "{\"ok\":true}");
        }
    }

    private static class CloseServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) {
            log.info("Received /api/close from browser — shutting down J-Learn");
            // Send the response on a separate thread so the browser sees a
            // clean shutdown rather than a hang/ECONNRESET.
            new Thread(() -> {
                try { Thread.sleep(150L); } catch (InterruptedException ignored) {}
                System.exit(0);
            }, "jlearn-close-exit").start();
            try {
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
            } catch (Exception ignored) {}
        }
    }

    static {
        // Suppress noisy charset detection.
        System.setProperty("file.encoding", StandardCharsets.UTF_8.name());
    }
}
