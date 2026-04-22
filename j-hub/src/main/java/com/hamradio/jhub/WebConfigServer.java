package com.hamradio.jhub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hamradio.jhub.callsign.*;
import com.hamradio.jhub.model.JHubConfig;
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

/**
 * WebConfigServer — embedded Jetty HTTP server that serves:
 *
 *   GET  /             → index.html (config UI single-page app)
 *   GET  /api/config   → current JSON configuration
 *   POST /api/config   → save new configuration
 *   GET  /api/status   → live j-hub status (connected apps, rig, cluster)
 *   GET  /api/spots    → recent spot cache
 *   POST /api/cluster/* → connect / disconnect cluster
 *   GET|POST /api/jmap → J-Map settings passthrough
 *   GET  /api/apps/status          → running state for jMap, j-log, j-bridge
 *   POST /api/apps/launch/{name}   → start a named app
 *   POST /api/apps/kill/{name}     → stop a named app
 *
 * Valid app names: "jMap", "j-log", "j-bridge", "j-digi"
 *
 * Static assets (index.html, etc.) are served from the JAR's /web/ resource dir.
 */
public class WebConfigServer {

    private static final Logger log = LoggerFactory.getLogger(WebConfigServer.class);

    /** All app names recognised by the launcher API. */
    private static final java.util.Set<String> KNOWN_APPS =
            java.util.Set.of("jMap", "j-log", "j-bridge", "j-digi", "j-sat");

    private final int           port;
    private final MessageRouter router;
    private final StateCache    cache;
    private       Server        server;

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

        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        ctx.setContextPath("/");

        ctx.addServlet(new ServletHolder(new ConfigApiServlet()),    "/api/config");
        ctx.addServlet(new ServletHolder(new StatusApiServlet()),    "/api/status");
        ctx.addServlet(new ServletHolder(new SpotsApiServlet()),     "/api/spots");
        ctx.addServlet(new ServletHolder(new ClusterApiServlet()),   "/api/cluster/*");
        ctx.addServlet(new ServletHolder(new JMapApiServlet()),        "/api/jmap");
        ctx.addServlet(new ServletHolder(new JSatApiServlet()),        "/api/jsat");
        ctx.addServlet(new ServletHolder(new JSatTleStatusServlet()), "/api/jsat/tle-status");
        ctx.addServlet(new ServletHolder(new JLogApiServlet()),        "/api/jlog");
        ctx.addServlet(new ServletHolder(new JDigiApiServlet()),       "/api/jdigi");
        ctx.addServlet(new ServletHolder(new AppsApiServlet()),      "/api/apps/*");
        ctx.addServlet(new ServletHolder(new MacrosApiServlet()),    "/api/macros");
        ctx.addServlet(new ServletHolder(new RigApiServlet()),       "/api/rig/*");
        ctx.addServlet(new ServletHolder(new RotorApiServlet()),     "/api/rotor");
        ctx.addServlet(new ServletHolder(new AppearanceApiServlet()),"/api/appearance");
        ctx.addServlet(new ServletHolder(new JBridgeApiServlet()),   "/api/jbridge");
        ctx.addServlet(new ServletHolder(new DbApiServlet()),        "/api/db/*");
        ctx.addServlet(new ServletHolder(new JMapImageUploadServlet("world_map.jpg", "RELOAD_MAP_IMAGE")), "/api/jmap/map-image");
        ctx.addServlet(new ServletHolder(new JMapImageUploadServlet("gcm.jpg",       "RELOAD_GCM")),       "/api/jmap/gcm-image");
        ctx.addServlet(new ServletHolder(new WeatherApiServlet()),    "/api/weather");
        ctx.addServlet(new ServletHolder(new CallsignApiServlet()), "/api/callsign/*");
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
                JHubConfig newCfg = ConfigManager.getInstance().fromJson(body);
                if (newCfg == null) throw new IllegalArgumentException("Parsed config was null");
                ConfigManager.getInstance().updateConfig(newCfg);
                if (newCfg.cluster != null && newCfg.cluster.autoConnect) {
                    ClusterManager.getInstance().reconnect();
                } else {
                    ClusterManager.getInstance().softDisconnect();
                }
                HamlibRigController.getInstance().restart(newCfg.rig);
                HamlibRotorController.getInstance().restart(newCfg.rotor);
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                log.error("Config save failed", e);
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                com.google.gson.JsonObject err = new com.google.gson.JsonObject();
                err.addProperty("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                json(res, err.toString());
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/cluster — connect / disconnect actions
    // ---------------------------------------------------------------

    private static class ClusterApiServlet extends HttpServlet {

        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            if ("/networks".equals(req.getPathInfo())) {
                JHubConfig.ClusterSection c = ConfigManager.getInstance().getCluster();
                json(res, ConfigManager.gson().toJson(c.networks));
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String action = req.getPathInfo();
            if ("/connect".equals(action)) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.isBlank()) {
                        com.google.gson.JsonObject j =
                            com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        JHubConfig.ClusterSection c = ConfigManager.getInstance().getCluster();
                        if (j.has("networkName")) {
                            // Look up saved network by name and apply its settings
                            String name = j.get("networkName").getAsString();
                            if (c.networks != null) {
                                c.networks.stream()
                                    .filter(n -> n.name.equals(name))
                                    .findFirst()
                                    .ifPresent(net -> {
                                        c.server        = net.server;
                                        c.port          = net.port;
                                        c.loginCallsign = net.loginCallsign;
                                    });
                            }
                        } else {
                            if (j.has("server"))        c.server        = j.get("server").getAsString();
                            if (j.has("port"))          c.port          = j.get("port").getAsInt();
                            if (j.has("loginCallsign")) c.loginCallsign = j.get("loginCallsign").getAsString();
                        }
                        ConfigManager.getInstance().save();
                    }
                } catch (Exception ignored) {}
                ClusterManager.getInstance().reconnect();
                json(res, "{\"status\":\"connecting\"}");

            } else if ("/disconnect".equals(action)) {
                ClusterManager.getInstance().softDisconnect();
                json(res, "{\"status\":\"disconnected\"}");

            } else if ("/networks".equals(action)) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j =
                        com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    JHubConfig.DxNetwork net = new JHubConfig.DxNetwork();
                    net.name          = j.has("name")          ? j.get("name").getAsString()          : "";
                    net.server        = j.has("server")        ? j.get("server").getAsString()        : "";
                    net.port          = j.has("port")          ? j.get("port").getAsInt()             : 7373;
                    net.loginCallsign = j.has("loginCallsign") ? j.get("loginCallsign").getAsString() : "";
                    if (net.name.isBlank()) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
                    JHubConfig.ClusterSection c = ConfigManager.getInstance().getCluster();
                    if (c.networks == null) c.networks = new java.util.ArrayList<>();
                    c.networks.removeIf(n -> n.name.equals(net.name)); // upsert
                    c.networks.add(net);
                    ConfigManager.getInstance().save();
                    json(res, "{\"status\":\"saved\"}");
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }

            } else if ("/send".equals(action)) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j =
                        com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    String command = j.has("command") ? j.get("command").getAsString() : "";
                    if (command.isBlank()) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        json(res, "{\"error\":\"empty command\"}");
                        return;
                    }
                    ClusterManager.getInstance().sendRawCommand(command);
                    json(res, "{\"status\":\"sent\"}");
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }

            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
            if ("/networks".equals(req.getPathInfo())) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j =
                        com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    String name = j.get("name").getAsString();
                    JHubConfig.ClusterSection c = ConfigManager.getInstance().getCluster();
                    c.networks.removeIf(n -> n.name.equals(name));
                    ConfigManager.getInstance().save();
                    json(res, "{\"status\":\"deleted\"}");
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/status — live status snapshot
    // ---------------------------------------------------------------

    private class StatusApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JsonObject status = new JsonObject();

            long uptimeSec = java.time.Duration.between(JHubMain.START_TIME,
                java.time.Instant.now()).getSeconds();
            status.addProperty("uptimeSeconds",    uptimeSec);
            status.addProperty("clusterConnected", ClusterManager.getInstance().isConnected());
            status.addProperty("spotsPerMinute",   cache.getSpotsPerMinute());
            status.addProperty("totalSpots",       cache.getTotalSpots());

            var rig = cache.getLastRigStatus();
            if (rig != null) {
                status.add("rig", ConfigManager.gson().toJsonTree(rig));
            }

            // App running state — included so the web UI can show launch buttons
            AppLauncher al = AppLauncher.getInstance();
            JsonObject appsRunning = new JsonObject();
            appsRunning.addProperty("jMap",     al.isRunning("jMap"));
            appsRunning.addProperty("j-log",    al.isRunning("j-log"));
            appsRunning.addProperty("j-bridge", al.isRunning("j-bridge"));
            appsRunning.addProperty("j-digi",   al.isRunning("j-digi"));
            appsRunning.addProperty("j-sat",    al.isRunning("j-sat"));
            status.add("appsRunning", appsRunning);

            // Connected WebSocket sessions — authoritative for module status lights
            JHubServer jhubServer = router.getJHubServer();
            if (jhubServer != null) {
                com.google.gson.JsonArray connectedApps = new com.google.gson.JsonArray();
                for (JHubServer.AppSession s : jhubServer.getSessions().values()) {
                    if (s.registered && s.socket.isOpen()) {
                        JsonObject appObj = new JsonObject();
                        appObj.addProperty("appName", s.appName);
                        appObj.addProperty("connectedAt", s.connectedAt.toString());
                        connectedApps.add(appObj);
                    }
                }
                status.add("connectedApps", connectedApps);
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
    // /api/jmap — persist J-Map settings in j-hub.json
    // ---------------------------------------------------------------

    private static class JMapApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored =
                ConfigManager.getInstance().getConfig().jMapSettings;
            json(res, stored != null ? stored.toString() : "{}");
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().jMapSettings = settings;
                cm.save();

                // Broadcast JMAP_CONFIG via WebSocket so local j-map applies it immediately
                com.google.gson.JsonObject wsMsg = settings.deepCopy();
                wsMsg.addProperty("type", "JMAP_CONFIG");
                String wsJson = wsMsg.toString();
                StateCache.getInstance().setLastJMapConfig(wsJson);
                JHubServer jhub = MessageRouter.getInstance().getJHubServer();
                if (jhub != null) {
                    jhub.broadcastToAppName("j-map", wsJson);
                }

                // Write /config/jmap_config.json for remote J-Map instances
                try {
                    com.google.gson.JsonObject remote = new com.google.gson.JsonObject();
                    remote.addProperty("version", 1);
                    if (settings.has("tileProvider"))      remote.add("tileProvider",      settings.get("tileProvider"));
                    if (settings.has("noaaApiKey"))        remote.add("apiKey",             settings.get("noaaApiKey"));
                    if (settings.has("mapZoom"))           remote.add("zoom",               settings.get("mapZoom"));
                    if (settings.has("mapCenterLat") && settings.has("mapCenterLon")) {
                        com.google.gson.JsonObject center = new com.google.gson.JsonObject();
                        center.add("lat", settings.get("mapCenterLat"));
                        center.add("lon", settings.get("mapCenterLon"));
                        remote.add("center", center);
                    }
                    if (settings.has("showSatelliteTracking")) remote.add("satelliteTracking", settings.get("showSatelliteTracking"));
                    if (settings.has("tleSource"))         remote.add("tleSource",          settings.get("tleSource"));
                    if (settings.has("refreshSeconds"))    remote.add("refreshSeconds",      settings.get("refreshSeconds"));
                    java.nio.file.Path remoteFile = java.nio.file.Path.of("/config/jmap_config.json");
                    java.nio.file.Files.createDirectories(remoteFile.getParent());
                    java.nio.file.Files.writeString(remoteFile, ConfigManager.gson().toJson(remote));
                } catch (Exception e) {
                    log.debug("Could not write remote jmap config: {}", e.getMessage());
                }

                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/jsat — persist J-Sat settings in j-hub.json
    // ---------------------------------------------------------------

    private static class JSatApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored =
                ConfigManager.getInstance().getConfig().jSatSettings;
            if (stored == null || stored.size() == 0) {
                res.setStatus(HttpServletResponse.SC_NO_CONTENT);
                return;
            }
            json(res, stored.toString());
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().jSatSettings = settings;
                cm.save();
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/jsat/tle-status — proxy to J-Sat TLE API and return freshness data
    // ---------------------------------------------------------------

    private static class JSatTleStatusServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            int tleApiPort = 4540;
            try {
                com.google.gson.JsonObject jss = ConfigManager.getInstance().getConfig().jSatSettings;
                if (jss != null && jss.has("tleApiPort")) {
                    tleApiPort = jss.get("tleApiPort").getAsInt();
                }
            } catch (Exception ignored) {}

            try {
                java.net.URL url = new java.net.URL("http://localhost:" + tleApiPort + "/api/tle/all");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(3000);
                if (conn.getResponseCode() == 200) {
                    String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    json(res, body);
                } else {
                    json(res, "[]");
                }
            } catch (Exception e) {
                log.debug("J-Sat TLE API unreachable on port {}: {}", tleApiPort, e.getMessage());
                json(res, "[]");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/jlog — persist J-Log settings in j-hub.json; broadcast CONFIG_UPDATE
    // ---------------------------------------------------------------

    private class JLogApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored = ConfigManager.getInstance().getConfig().jLogSettings;
            json(res, stored != null ? stored.toString() : "{}");
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().jLogSettings = settings;
                cm.save();
                // Push CONFIG_UPDATE to j-log via WebSocket
                JHubServer server = router.getJHubServer();
                if (server != null && settings.has("fontSize")) {
                    com.google.gson.JsonObject upd = new com.google.gson.JsonObject();
                    upd.addProperty("type", "CONFIG_UPDATE");
                    upd.add("settings", settings);
                    server.broadcastToAppName("logging-engine", upd.toString());
                }
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/jdigi — persist J-Digi settings in j-hub.json; broadcast CONFIG_UPDATE
    // ---------------------------------------------------------------

    private class JDigiApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored = ConfigManager.getInstance().getConfig().jDigiSettings;
            json(res, stored != null ? stored.toString() : "{}");
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().jDigiSettings = settings;
                cm.save();
                // Push CONFIG_UPDATE to j-digi via WebSocket
                JHubServer server = router.getJHubServer();
                if (server != null && settings.has("fontSize")) {
                    com.google.gson.JsonObject upd = new com.google.gson.JsonObject();
                    upd.addProperty("type", "CONFIG_UPDATE");
                    upd.add("settings", settings);
                    server.broadcastToAppName("j-digi", upd.toString());
                }
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/apps — launch / kill / status for all managed apps
    //
    //   Valid names: jMap | j-log | j-bridge | j-digi
    // ---------------------------------------------------------------

    private static class AppsApiServlet extends HttpServlet {

        /** GET /api/apps/status — running state for each app */
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            AppLauncher al = AppLauncher.getInstance();
            JsonObject obj = new JsonObject();
            obj.addProperty("jMap",     al.isRunning("jMap"));
            obj.addProperty("j-log",    al.isRunning("j-log"));
            obj.addProperty("j-bridge", al.isRunning("j-bridge"));
            obj.addProperty("j-digi",   al.isRunning("j-digi"));
            json(res, obj.toString());
        }

        /**
         * POST /api/apps/launch/{name}  — start a named app
         * POST /api/apps/kill/{name}    — stop a named app
         */
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String pathInfo = req.getPathInfo(); // "/launch/jMap" etc.
            if (pathInfo == null) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }

            String[] parts = pathInfo.split("/");
            // parts[0]="" parts[1]="launch"|"kill" parts[2]="jMap"|"j-log"|"j-bridge"|"j-digi"
            if (parts.length < 3) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }

            String op   = parts[1];
            String name = parts[2];

            if (!KNOWN_APPS.contains(name)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"unknown app: " + name + "\"}");
                return;
            }

            AppLauncher al = AppLauncher.getInstance();

            if ("launch".equals(op)) {
                // Command may be overridden in the POST body
                String command = null;
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    if (!body.isBlank()) {
                        com.google.gson.JsonObject bodyJson =
                            com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        if (bodyJson.has("command"))
                            command = bodyJson.get("command").getAsString().strip();
                    }
                } catch (Exception ignored) {}

                // Fall back to saved config
                if (command == null || command.isBlank()) {
                    JHubConfig.AppLaunchEntry entry = entryFor(name);
                    if (entry != null) command = entry.command;
                }

                if (command == null || command.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"no command configured for " + name + "\"}");
                    return;
                }

                // Persist the command so it survives a restart
                JHubConfig.AppLaunchEntry entry = entryFor(name);
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

        /** Retrieve the AppLaunchEntry for a given app name. */
        private JHubConfig.AppLaunchEntry entryFor(String name) {
            JHubConfig.AppsSection apps = ConfigManager.getInstance().getApps();
            if (apps == null) return null;
            if ("jMap".equals(name))     return apps.jMap;
            if ("j-log".equals(name))    return apps.jLog;
            if ("j-bridge".equals(name)) return apps.jBridge;
            if ("j-digi".equals(name))   return apps.jDigi;
            if ("j-sat".equals(name))    return apps.jSat;
            return null;
        }
    }

    // ---------------------------------------------------------------
    // /api/macros — GET returns macro list, POST saves updated list
    // ---------------------------------------------------------------

    private static class MacrosApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JHubConfig.MacrosSection ms = ConfigManager.getInstance().getConfig().macros;
            json(res, ms == null || ms.list == null ? "[]" : ConfigManager.gson().toJson(ms.list));
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                java.lang.reflect.Type listType =
                    new com.google.gson.reflect.TypeToken<java.util.List<JHubConfig.MacroDefinition>>(){}.getType();
                java.util.List<JHubConfig.MacroDefinition> list =
                    ConfigManager.gson().fromJson(body, listType);
                ConfigManager cm = ConfigManager.getInstance();
                if (cm.getConfig().macros == null) cm.getConfig().macros = new JHubConfig.MacrosSection();
                cm.getConfig().macros.list = list;
                cm.save();
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/rig  — rig config + control actions
    //
    //   GET  /api/rig           → current rig config JSON
    //   GET  /api/rig/status    → { connected, running, frequency, mode }
    //   POST /api/rig           → save rig config; (re)starts controller
    //   POST /api/rig/ptt       → { "ptt": true|false }  key/un-key TX
    //   POST /api/rig/reconnect → force-close and reopen rigctld connection
    // ---------------------------------------------------------------

    private static class RigApiServlet extends HttpServlet {

        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if ("/status".equals(path)) {
                HamlibRigController ctrl = HamlibRigController.getInstance();
                com.google.gson.JsonObject status = new com.google.gson.JsonObject();
                status.addProperty("running",   ctrl.isRunning());
                status.addProperty("connected", ctrl.isConnected());
                status.addProperty("frequency", ctrl.getLastFreq());
                status.addProperty("mode",      ctrl.getLastMode());
                json(res, status.toString());
            } else {
                json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().rig));
            }
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();

            // ── PTT key/unkey ──────────────────────────────────────
            if ("/ptt".equals(path)) {
                HamlibRigController ctrl = HamlibRigController.getInstance();
                if (!ctrl.isRunning()) {
                    res.setStatus(HttpServletResponse.SC_CONFLICT);
                    json(res, "{\"error\":\"Hamlib controller is not running\"}");
                    return;
                }
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    boolean on = true; // default: key TX
                    if (!body.isBlank()) {
                        com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                        if (j.has("ptt")) on = j.get("ptt").getAsBoolean();
                    }
                    ctrl.setPtt(on);
                    json(res, "{\"status\":\"ptt " + (on ? "on" : "off") + "\"}");
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }
                return;
            }

            // ── Force reconnect ─────────────────────────────────────
            if ("/reconnect".equals(path)) {
                HamlibRigController.getInstance().reconnect();
                json(res, "{\"status\":\"reconnecting\"}");
                return;
            }

            // ── Save config (no path suffix) ────────────────────────
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.RigSection rig = ConfigManager.gson().fromJson(body, JHubConfig.RigSection.class);
                ConfigManager.getInstance().getConfig().rig = rig;
                ConfigManager.getInstance().save();
                // Apply immediately — restart controller if backend changed
                HamlibRigController.getInstance().restart(rig);
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/rotor — GET/POST rotor config
    // ---------------------------------------------------------------

    private static class RotorApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().rotor));
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.RotorSection rotor = ConfigManager.gson().fromJson(body, JHubConfig.RotorSection.class);
                ConfigManager.getInstance().getConfig().rotor = rotor;
                ConfigManager.getInstance().save();
                HamlibRotorController.getInstance().restart(rotor);
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/appearance — GET/POST appearance config
    // ---------------------------------------------------------------

    private static class AppearanceApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().appearance));
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.AppearanceSection ap = ConfigManager.gson().fromJson(body, JHubConfig.AppearanceSection.class);
                ConfigManager.getInstance().getConfig().appearance = ap;
                ConfigManager.getInstance().save();
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/jbridge — persist J-Bridge settings in j-hub.json
    // ---------------------------------------------------------------

    private static class JBridgeApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject stored = ConfigManager.getInstance().getConfig().jBridgeSettings;
            json(res, stored != null ? stored.toString() : "{}");
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject settings =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                ConfigManager cm = ConfigManager.getInstance();
                cm.getConfig().jBridgeSettings = settings;
                cm.save();
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/db  — log database management (files in ~/.j-log/)
    //
    //   GET  /api/db/list         → { databases: [...], active: "j-log.db" }
    //   GET  /api/db/active       → { active: "j-log.db" }
    //   POST /api/db/create       → { name: "mylog" }
    //   POST /api/db/select       → { name: "mylog.db" }
    //   POST /api/db/export/adif  → streams .adi file (requires J-Log connected)
    //   POST /api/db/export/csv   → streams .csv file (requires J-Log connected)
    //   DELETE /api/db/delete     → { name: "mylog.db" }
    // ---------------------------------------------------------------

    private class DbApiServlet extends HttpServlet {

        private final java.nio.file.Path LOG_DIR = java.nio.file.Path.of(System.getProperty("user.home"), ".j-log");
        private static final String DEFAULT_DB  = "j-log.db";
        private static final String ACTIVE_FILE = ".active-db";

        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if ("/list".equals(path)) {
                try {
                    java.util.List<String> dbs;
                    if (java.nio.file.Files.exists(LOG_DIR)) {
                        dbs = java.nio.file.Files.list(LOG_DIR)
                            .map(p -> p.getFileName().toString())
                            .filter(n -> n.endsWith(".db") && !n.equals("contest.db") && !n.equals("config.db"))
                            .sorted()
                            .collect(java.util.stream.Collectors.toList());
                    } else {
                        dbs = new java.util.ArrayList<>();
                    }
                    if (!dbs.contains(DEFAULT_DB)) dbs.add(0, DEFAULT_DB);
                    com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                    obj.add("databases", ConfigManager.gson().toJsonTree(dbs));
                    obj.addProperty("active", readActiveDb());
                    json(res, obj.toString());
                } catch (Exception e) {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else if ("/active".equals(path)) {
                json(res, "{\"active\":\"" + readActiveDb() + "\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();

            if ("/create".equals(path)) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    String rawName = j.has("name") ? j.get("name").getAsString() : "";
                    String safe = rawName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                    if (safe.isBlank()) { res.setStatus(400); json(res, "{\"error\":\"Invalid database name\"}"); return; }
                    if (!safe.endsWith(".db")) safe += ".db";
                    java.nio.file.Path target = LOG_DIR.resolve(safe);
                    if (java.nio.file.Files.exists(target)) { res.setStatus(409); json(res, "{\"error\":\"Database already exists: " + safe + "\"}"); return; }
                    java.nio.file.Files.createDirectories(LOG_DIR);
                    new java.io.FileOutputStream(target.toFile()).close();
                    json(res, "{\"name\":\"" + safe + "\"}");
                } catch (Exception e) {
                    res.setStatus(500);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }

            } else if ("/select".equals(path)) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    String name = j.has("name") ? j.get("name").getAsString() : "";
                    if (name.isBlank()) { res.setStatus(400); json(res, "{\"error\":\"No database name provided\"}"); return; }
                    writeActiveDb(name);
                    json(res, "{\"active\":\"" + name + "\"}");
                } catch (Exception e) {
                    res.setStatus(500);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }

            } else if ("/export/adif".equals(path) || "/export/csv".equals(path)) {
                boolean isAdif = path.endsWith("adif");
                JHubServer jhubServer = router.getJHubServer();
                if (jhubServer == null || !jhubServer.hasAppConnected("logging-engine")) {
                    res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    json(res, "{\"error\":\"J-Log is not connected — run J-Log to export\"}");
                    return;
                }
                String tmpFile = System.getProperty("java.io.tmpdir") + java.io.File.separator
                    + "jhub-export-" + System.currentTimeMillis() + (isAdif ? ".adi" : ".csv");
                com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                msg.addProperty("type", isAdif ? "EXPORT_ADIF" : "EXPORT_CSV");
                msg.addProperty("targetPath", tmpFile);
                jhubServer.broadcastToAppName("logging-engine", msg.toString());
                java.nio.file.Path outPath = java.nio.file.Path.of(tmpFile);
                for (int i = 0; i < 50; i++) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    if (java.nio.file.Files.exists(outPath)) break;
                }
                if (!java.nio.file.Files.exists(outPath)) {
                    res.setStatus(504);
                    json(res, "{\"error\":\"Export timed out — J-Log did not respond\"}");
                    return;
                }
                try {
                    byte[] data = java.nio.file.Files.readAllBytes(outPath);
                    java.nio.file.Files.deleteIfExists(outPath);
                    res.setContentType(isAdif ? "text/plain; charset=utf-8" : "text/csv; charset=utf-8");
                    res.setHeader("Content-Disposition",
                        "attachment; filename=\"" + (isAdif ? "log-export.adi" : "log-export.csv") + "\"");
                    res.setHeader("Access-Control-Allow-Origin", "*");
                    res.getOutputStream().write(data);
                } catch (Exception e) {
                    res.setStatus(500);
                    json(res, "{\"error\":\"File read failed: " + e.getMessage() + "\"}");
                }

            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
            if ("/delete".equals(req.getPathInfo())) {
                try {
                    String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    com.google.gson.JsonObject j = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                    String name = j.has("name") ? j.get("name").getAsString() : "";
                    if (DEFAULT_DB.equals(name)) {
                        res.setStatus(400);
                        json(res, "{\"error\":\"Cannot delete the default database\"}");
                        return;
                    }
                    java.nio.file.Files.deleteIfExists(LOG_DIR.resolve(name));
                    json(res, "{\"status\":\"deleted\"}");
                } catch (Exception e) {
                    res.setStatus(500);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }
            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res);
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private String readActiveDb() {
            try {
                java.nio.file.Path f = LOG_DIR.resolve(ACTIVE_FILE);
                if (java.nio.file.Files.exists(f)) return java.nio.file.Files.readString(f).trim();
            } catch (Exception ignored) {}
            return DEFAULT_DB;
        }

        private void writeActiveDb(String name) throws java.io.IOException {
            java.nio.file.Files.createDirectories(LOG_DIR);
            java.nio.file.Files.writeString(LOG_DIR.resolve(ACTIVE_FILE), name);
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
            if (is == null) { res.sendError(HttpServletResponse.SC_NOT_FOUND); return; }

            if      (path.endsWith(".html")) res.setContentType("text/html; charset=utf-8");
            else if (path.endsWith(".js"))   res.setContentType("application/javascript; charset=utf-8");
            else if (path.endsWith(".css"))  res.setContentType("text/css; charset=utf-8");
            else if (path.endsWith(".json")) res.setContentType("application/json; charset=utf-8");
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) res.setContentType("image/jpeg");
            else if (path.endsWith(".png")) res.setContentType("image/png");
            else                             res.setContentType("application/octet-stream");

            res.getOutputStream().write(is.readAllBytes());
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // /api/jmap/map-image  — upload world_map.jpg to ~/.j-map/
    // /api/jmap/gcm-image  — upload gcm.jpg to ~/.j-map/
    // Both notify j-map via WebSocket to reload immediately.
    // ---------------------------------------------------------------

    private static class JMapImageUploadServlet extends HttpServlet {

        private static final java.nio.file.Path JMAP_DIR =
            java.nio.file.Path.of(System.getProperty("user.home"), ".j-map");

        private final String fileName;
        private final String wsReloadType;

        JMapImageUploadServlet(String fileName, String wsReloadType) {
            this.fileName     = fileName;
            this.wsReloadType = wsReloadType;
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                jakarta.servlet.MultipartConfigElement mpConfig =
                    new jakarta.servlet.MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"), 20 * 1024 * 1024, 20 * 1024 * 1024, 1024 * 1024);
                req.setAttribute("org.eclipse.jetty.multipartConfig", mpConfig);

                jakarta.servlet.http.Part part = req.getPart("image");
                if (part == null) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"No file part named 'image'\"}");
                    return;
                }

                java.nio.file.Files.createDirectories(JMAP_DIR);
                java.nio.file.Path dest = JMAP_DIR.resolve(fileName);
                try (java.io.InputStream in = part.getInputStream()) {
                    java.nio.file.Files.copy(in, dest,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                log.info("J-Map image saved: {}", dest);

                // Signal j-map to reload
                com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                msg.addProperty("type", wsReloadType);
                String wsJson = msg.toString();
                JHubServer jhub = MessageRouter.getInstance().getJHubServer();
                if (jhub != null) jhub.broadcastToAppName("j-map", wsJson);

                json(res, "{\"status\":\"ok\",\"file\":\"" + fileName + "\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/weather — cached space weather + local weather
    // ---------------------------------------------------------------

    // ── /api/callsign/{call} ───────────────────────────────────────────────────

    private static class CallsignApiServlet extends HttpServlet {

        /**
         * GET  /api/callsign/{call}           — look up a callsign
         * GET  /api/callsign/cache            — dump in-memory lookup cache
         * GET  /api/callsign/db/status        — local DB record count + import state
         * GET  /api/callsign/db/import/progress — live import progress
         * POST /api/callsign/db/import/fcc    — start FCC ULS import  {ulsDir, dbPath?}
         * POST /api/callsign/db/import/csv    — start CSV import       {csvPath, dbPath?}
         * DELETE /api/callsign/{call}         — evict one entry from cache
         */
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if (path == null || path.equals("/")) {
                json(res, "{\"error\":\"No callsign specified\"}");
                return;
            }

            if (path.equals("/db/status")) {
                dbStatus(res); return;
            }
            if (path.equals("/db/import/progress")) {
                dbImportProgress(res); return;
            }
            if (path.equals("/cache")) {
                json(res, CallsignLookupService.getInstance().getCacheJson()); return;
            }

            String call = path.substring(1).toUpperCase().trim();
            json(res, new Gson().toJson(CallsignLookupService.getInstance().lookup(call)));
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject j = body.isBlank() ? new JsonObject()
                : com.google.gson.JsonParser.parseString(body).getAsJsonObject();

            String cfgDbPath = ConfigManager.getInstance().getConfig().callsignLookup.localDbPath;

            if ("/db/download/fcc".equals(path)) {
                String dbPath = j.has("dbPath") ? j.get("dbPath").getAsString() : cfgDbPath;
                if (dbPath.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"localDbPath must be set in config or passed as dbPath\"}");
                    return;
                }
                String urlOverride = ConfigManager.getInstance().getConfig().callsignLookup.fccUlsUrl;
                boolean started = FccUlsDownloader.getInstance().start(dbPath, urlOverride);
                json(res, "{\"started\":" + started + "}");

            } else if ("/db/import/fcc".equals(path)) {
                String ulsDir = j.has("ulsDir") ? j.get("ulsDir").getAsString() : "";
                String dbPath = j.has("dbPath") ? j.get("dbPath").getAsString() : cfgDbPath;
                if (ulsDir.isBlank() || dbPath.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"ulsDir and localDbPath (in config) are required\"}");
                    return;
                }
                boolean started = FccUlsImporter.getInstance().start(ulsDir, dbPath);
                json(res, "{\"started\":" + started + "}");

            } else if ("/db/import/csv".equals(path)) {
                String csvPath = j.has("csvPath") ? j.get("csvPath").getAsString() : "";
                String dbPath  = j.has("dbPath")  ? j.get("dbPath").getAsString()  : cfgDbPath;
                if (csvPath.isBlank() || dbPath.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"csvPath and localDbPath (in config) are required\"}");
                    return;
                }
                boolean started = CsvImporter.getInstance().start(csvPath, dbPath);
                json(res, "{\"started\":" + started + "}");

            } else {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"Unknown endpoint\"}");
            }
        }

        @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo();
            if (path != null && path.length() > 1) {
                String call = path.substring(1).toUpperCase().trim();
                CallsignLookupService.getInstance().invalidate(call);
                json(res, "{\"status\":\"evicted\",\"callsign\":\"" + call + "\"}");
            } else {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"No callsign specified\"}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private void dbStatus(HttpServletResponse res) throws IOException {
            LocalDbProvider local = new LocalDbProvider();
            String dbPath = LocalDbProvider.dbPath();
            JsonObject o  = new JsonObject();
            o.addProperty("dbPath",   dbPath != null ? dbPath : "");
            o.addProperty("exists",   local.isAvailable());
            o.addProperty("records",  local.recordCount());

            if (dbPath != null && !dbPath.isBlank()) {
                try {
                    java.nio.file.Path p = java.nio.file.Path.of(dbPath);
                    if (java.nio.file.Files.exists(p)) {
                        o.addProperty("sizeBytes", java.nio.file.Files.size(p));
                        o.addProperty("lastModified",
                            java.nio.file.Files.getLastModifiedTime(p).toInstant().toString());
                    }
                } catch (Exception ignored) {}
            }

            FccUlsImporter fcc = FccUlsImporter.getInstance();
            CsvImporter    csv = CsvImporter.getInstance();
            JsonObject imp = new JsonObject();
            imp.addProperty("fccStatus",  fcc.getStatus().name());
            imp.addProperty("fccMsg",     fcc.getStatusMsg());
            imp.addProperty("fccCount",   fcc.getImported());
            imp.addProperty("csvStatus",  csv.getStatus().name());
            imp.addProperty("csvMsg",     csv.getStatusMsg());
            imp.addProperty("csvCount",   csv.getImported());
            o.add("importers", imp);

            json(res, o.toString());
        }

        private void dbImportProgress(HttpServletResponse res) throws IOException {
            FccUlsDownloader dl  = FccUlsDownloader.getInstance();
            FccUlsImporter   fcc = FccUlsImporter.getInstance();
            CsvImporter      csv = CsvImporter.getInstance();
            JsonObject o = new JsonObject();
            // downloader (end-to-end pipeline)
            o.addProperty("dlPhase",         dl.getPhase().name());
            o.addProperty("dlMsg",           dl.getStatusMsg());
            o.addProperty("dlBytesReceived", dl.getBytesReceived());
            o.addProperty("dlBytesTotal",    dl.getBytesTotal());
            o.addProperty("dlRunning",       dl.isRunning());
            // importer (manual ULS dir path)
            o.addProperty("fccStatus",  fcc.getStatus().name());
            o.addProperty("fccMsg",     fcc.getStatusMsg());
            o.addProperty("fccCount",   fcc.getImported());
            o.addProperty("fccRunning", fcc.isRunning());
            // CSV importer
            o.addProperty("csvStatus",  csv.getStatus().name());
            o.addProperty("csvMsg",     csv.getStatusMsg());
            o.addProperty("csvCount",   csv.getImported());
            o.addProperty("csvRunning", csv.isRunning());
            json(res, o.toString());
        }
    }

    // ── /api/weather ───────────────────────────────────────────────────────────

    private static class WeatherApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, WeatherService.getInstance().getCachedJson());
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------

    private static void json(HttpServletResponse res, String body) throws IOException {
        res.setContentType("application/json; charset=utf-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        PrintWriter w = res.getWriter();
        w.print(body);
        w.flush();
    }

    private static void cors(HttpServletResponse res) {
        res.setHeader("Access-Control-Allow-Origin",  "*");
        res.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        res.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }
}
