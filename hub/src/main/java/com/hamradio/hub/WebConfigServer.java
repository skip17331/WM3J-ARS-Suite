package com.hamradio.hub;

import com.google.gson.JsonObject;
import com.hamradio.hub.model.HubConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * WebConfigServer — embedded Jetty HTTP server that serves:
 *
 *   GET  /            → index.html (config UI single-page app)
 *   GET  /api/config  → current JSON configuration
 *   POST /api/config  → save new configuration
 *   GET  /api/status  → live hub status (connected apps, rig, cluster)
 *   GET  /api/spots   → recent spot cache
 *
 * Static assets (index.html, config.js, config.css) are served from
 * the JAR's /web/ resource directory.
 */
public class WebConfigServer {

    private static final Logger log = LoggerFactory.getLogger(WebConfigServer.class);

    private final int    port;
    private final MessageRouter router;
    private final StateCache    cache;
    private       Server server;

    public WebConfigServer(int port, MessageRouter router, StateCache cache) {
        this.port   = port;
        this.router = router;
        this.cache  = cache;
    }

    // ---------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------

    public void start() throws Exception {
        server = new Server(port);

        // API servlets
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ctx.addServlet(new ServletHolder(new ConfigApiServlet()),    "/api/config");
        ctx.addServlet(new ServletHolder(new StatusApiServlet()),   "/api/status");
        ctx.addServlet(new ServletHolder(new SpotsApiServlet()),    "/api/spots");
        ctx.addServlet(new ServletHolder(new ClusterApiServlet()),  "/api/cluster/*");
        ctx.addServlet(new ServletHolder(new HamClockApiServlet()), "/api/hamclock");
        ctx.addServlet(new ServletHolder(new AppsApiServlet()),     "/api/apps/*");
        ctx.addServlet(new ServletHolder(new StaticServlet()),      "/*");

        server.setHandler(ctx);
        server.start();
        log.info("Web config server started on port {}", port);
    }

    public void stop() throws Exception {
        if (server != null) server.stop();
    }

    // ---------------------------------------------------------------
    // /api/config — GET returns config JSON, POST saves it
    // ---------------------------------------------------------------

    private static class ConfigApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.getInstance().toJson());
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                HubConfig newCfg = ConfigManager.getInstance().fromJson(body);
                ConfigManager.getInstance().updateConfig(newCfg);

                // Soft-reconnect cluster with new settings (does not shut down executors)
                ClusterManager.getInstance().reconnect();

                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type");
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/cluster — connect / disconnect actions
    // ---------------------------------------------------------------

    private static class ClusterApiServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String action = req.getPathInfo(); // "/connect" or "/disconnect"
            if ("/connect".equals(action)) {
                // Accept cluster config in the body so Connect works without a separate Save
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.isBlank()) {
                        com.google.gson.JsonObject j =
                            com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        HubConfig.ClusterSection c = ConfigManager.getInstance().getCluster();
                        if (j.has("server"))        c.server        = j.get("server").getAsString();
                        if (j.has("port"))          c.port          = j.get("port").getAsInt();
                        if (j.has("loginCallsign")) c.loginCallsign = j.get("loginCallsign").getAsString();
                        ConfigManager.getInstance().save();
                    }
                } catch (Exception ignored) {}
                ClusterManager.getInstance().reconnect();
                json(res, "{\"status\":\"connecting\"}");
            } else if ("/disconnect".equals(action)) {
                ClusterManager.getInstance().softDisconnect();
                json(res, "{\"status\":\"disconnected\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    // ---------------------------------------------------------------
    // /api/status — live status snapshot
    // ---------------------------------------------------------------

    private class StatusApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JsonObject status = new JsonObject();

            // Hub uptime
            long uptimeSec = java.time.Duration.between(HubMain.START_TIME,
                java.time.Instant.now()).getSeconds();
            status.addProperty("uptimeSeconds", uptimeSec);
            status.addProperty("clusterConnected", ClusterManager.getInstance().isConnected());
            status.addProperty("spotsPerMinute", cache.getSpotsPerMinute());
            status.addProperty("totalSpots", cache.getTotalSpots());

            // Rig
            var rig = cache.getLastRigStatus();
            if (rig != null) {
                status.add("rig", ConfigManager.gson().toJsonTree(rig));
            }

            json(res, status.toString());
        }
    }

    // ---------------------------------------------------------------
    // /api/spots — recent spots
    // ---------------------------------------------------------------

    private class SpotsApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.gson().toJson(cache.getRecentSpots()));
        }
    }

    // ---------------------------------------------------------------
    // /api/hamclock — persist HamClock settings in hub.json
    // ---------------------------------------------------------------

    private static class HamClockApiServlet extends HttpServlet {
        /** GET — return stored HamClock settings (or empty object if none saved yet) */
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored =
                ConfigManager.getInstance().getConfig().hamclockSettings;
            json(res, stored != null ? stored.toString() : "{}");
        }

        /** POST — save HamClock settings into hub.json */
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().hamclockSettings = settings;
                cm.save();
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            res.setHeader("Access-Control-Allow-Origin", "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type");
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/apps — launch/kill/status for hamclock and hamlog
    // ---------------------------------------------------------------

    private static class AppsApiServlet extends HttpServlet {

        /** GET /api/apps/status — returns running state for each app */
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            AppLauncher al = AppLauncher.getInstance();
            JsonObject obj = new JsonObject();
            obj.addProperty("hamclock", al.isRunning("hamclock"));
            obj.addProperty("hamlog",   al.isRunning("hamlog"));
            json(res, obj.toString());
        }

        /**
         * POST /api/apps/launch/{name}  — start a named app
         * POST /api/apps/kill/{name}    — stop a named app
         */
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String pathInfo = req.getPathInfo(); // e.g. "/launch/hamclock"
            if (pathInfo == null) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            String[] parts = pathInfo.split("/");
            // parts: ["", "launch"|"kill", "hamclock"|"hamlog"]
            if (parts.length < 3) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            String op   = parts[1];
            String name = parts[2];

            if (!name.equals("hamclock") && !name.equals("hamlog")) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            AppLauncher al = AppLauncher.getInstance();
            if ("launch".equals(op)) {
                // Command may be supplied in the request body (takes priority over saved config)
                String command = null;
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.isBlank()) {
                        com.google.gson.JsonObject bodyJson =
                            com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        if (bodyJson.has("command")) {
                            command = bodyJson.get("command").getAsString().strip();
                        }
                    }
                } catch (Exception ignored) {}

                // Fall back to saved config if body had no command
                if (command == null || command.isBlank()) {
                    var entry = name.equals("hamclock")
                        ? ConfigManager.getInstance().getApps().hamclock
                        : ConfigManager.getInstance().getApps().hamlog;
                    if (entry != null) command = entry.command;
                }

                if (command == null || command.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"no command configured for " + name + "\"}");
                    return;
                }

                // Persist the command so it survives a restart
                var cfg = ConfigManager.getInstance().getApps();
                var entry = name.equals("hamclock") ? cfg.hamclock : cfg.hamlog;
                if (entry != null) {
                    entry.command = command;
                    try { ConfigManager.getInstance().save(); } catch (Exception ignored) {}
                }

                String err = al.launch(name, command);
                if (err == null) {
                    json(res, "{\"status\":\"launched\"}");
                } else {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    json(res, "{\"status\":\"error\",\"error\":" +
                        ConfigManager.gson().toJson(err) + "}");
                }
            } else if ("kill".equals(op)) {
                al.kill(name);
                json(res, "{\"status\":\"stopped\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }
    }

    // ---------------------------------------------------------------
    // Static files — served from classpath /web/
    // ---------------------------------------------------------------

    private static class StaticServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) path = "/index.html";

            InputStream is = getClass().getResourceAsStream("/web" + path);
            if (is == null) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Basic MIME type
            if (path.endsWith(".html")) res.setContentType("text/html; charset=utf-8");
            else if (path.endsWith(".js")) res.setContentType("application/javascript");
            else if (path.endsWith(".css")) res.setContentType("text/css");
            else res.setContentType("application/octet-stream");

            res.getOutputStream().write(is.readAllBytes());
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private static void json(HttpServletResponse res, String body) throws IOException {
        res.setContentType("application/json; charset=utf-8");
        res.setHeader("Access-Control-Allow-Origin", "*"); // allow local dev tools
        PrintWriter w = res.getWriter();
        w.print(body);
        w.flush();
    }
}
