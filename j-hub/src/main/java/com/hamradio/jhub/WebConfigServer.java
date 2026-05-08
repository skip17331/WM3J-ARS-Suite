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
            java.util.Set.of("jMap", "j-log", "j-bridge", "j-digi", "j-sat", "jVault", "j-learn");

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
        ctx.addServlet(new ServletHolder(new JLogRestartServlet()),    "/api/jlog/restart");
        ctx.addServlet(new ServletHolder(new AppRestartServlet("jMap",    "j-map")),     "/api/jmap/restart");
        ctx.addServlet(new ServletHolder(new AppRestartServlet("j-digi",  "j-digi")),    "/api/jdigi/restart");
        ctx.addServlet(new ServletHolder(new AppRestartServlet("j-bridge","jBridge")),   "/api/jbridge/restart");
        ctx.addServlet(new ServletHolder(new AppRestartServlet("j-sat",   "j-sat")),     "/api/jsat/restart");
        ctx.addServlet(new ServletHolder(new JDigiApiServlet()),       "/api/jdigi");
        ctx.addServlet(new ServletHolder(new AppsApiServlet()),      "/api/apps/*");
        ctx.addServlet(new ServletHolder(new MacrosApiServlet()),    "/api/macros");
        ctx.addServlet(new ServletHolder(new RbnApiServlet()),       "/api/rbn");
        // J-Learn now runs in its own process on port 8082 — the web UI iframes it.
        ctx.addServlet(new ServletHolder(new MorseTrainerLaunchServlet()), "/api/morsetrainer/*");
        ctx.addServlet(new ServletHolder(new BackupApiServlet()),    "/api/backup/*");
        ctx.addServlet(new ServletHolder(new UploadersApiServlet()), "/api/uploaders/*");
        ctx.addServlet(new ServletHolder(new CredentialsApiServlet()), "/api/credentials/*");
        ctx.addServlet(new ServletHolder(new MacroTriggerServlet()), "/api/macros/trigger");
        ctx.addServlet(new ServletHolder(new VoiceUploadServlet()),  "/api/voice/upload");
        ctx.addServlet(new ServletHolder(new VoiceFileServlet()),    "/api/voice/file");
        ctx.addServlet(new ServletHolder(new RigApiServlet()),       "/api/rig/*");
        ctx.addServlet(new ServletHolder(new RotorApiServlet()),     "/api/rotor");
        ctx.addServlet(new ServletHolder(new AmpApiServlet()),       "/api/amp");
        ctx.addServlet(new ServletHolder(new AntennaApiServlet()),   "/api/antenna/*");
        ctx.addServlet(new ServletHolder(new DataApiServlet()),      "/api/data/*");
        ctx.addServlet(new ServletHolder(new AppearanceApiServlet()),"/api/appearance");
        ctx.addServlet(new ServletHolder(new JBridgeApiServlet()),   "/api/jbridge");
        ctx.addServlet(new ServletHolder(new SessionsApiServlet()),  "/api/sessions");
        ctx.addServlet(new ServletHolder(new DepsApiServlet()),      "/api/deps");
        ctx.addServlet(new ServletHolder(new DiagnosticsBundleServlet()), "/api/diagnostics/bundle");
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
                java.util.List<JHubConfig.DxNetwork> list = new java.util.ArrayList<>(
                    c.networks != null ? c.networks : java.util.Collections.emptyList());
                if (c.server != null && !c.server.isBlank()) {
                    boolean alreadyPresent = list.stream()
                        .anyMatch(n -> c.server.equals(n.server) && c.port == n.port);
                    if (!alreadyPresent) {
                        JHubConfig.DxNetwork def = new JHubConfig.DxNetwork();
                        def.name   = c.server + ":" + c.port;
                        def.server = c.server;
                        def.port   = c.port;
                        def.loginCallsign = c.loginCallsign != null ? c.loginCallsign : "";
                        list.add(0, def);
                    }
                }
                json(res, ConfigManager.gson().toJson(list));
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
                    if (settings.has("mapStyle"))          remote.add("mapStyle",           settings.get("mapStyle"));
                    if (settings.has("mapView"))           remote.add("mapView",            settings.get("mapView"));
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
            if ("jVault".equals(name))   return apps.jVault;
            if ("j-learn".equals(name))  return apps.jLearn;
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
    // /api/amp — GET/POST amp config (Hamlib ampctld)
    // ---------------------------------------------------------------

    private static class AmpApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().amp));
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.AmpSection amp = ConfigManager.gson().fromJson(body, JHubConfig.AmpSection.class);
                ConfigManager.getInstance().getConfig().amp = amp;
                ConfigManager.getInstance().save();
                HamlibAmpController.getInstance().restart(amp);
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
    // /api/antenna — GET/POST antenna config; GET /api/antenna/status returns
    // the live switch state. Mirrors the rig/rotor servlet pattern.
    // ---------------------------------------------------------------

    private static class AntennaApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if ("/status".equals(path)) {
                json(res, ConfigManager.gson().toJson(AntennaController.getInstance().snapshotStatus()));
            } else {
                json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().antenna));
            }
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.AntennaSection ant =
                        ConfigManager.gson().fromJson(body, JHubConfig.AntennaSection.class);
                ConfigManager.getInstance().getConfig().antenna = ant;
                ConfigManager.getInstance().save();
                AntennaController.getInstance().restart(ant);
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
    // /api/data — GET status / config; POST /api/data/refresh forces an
    // immediate fetch; POST /api/data with body replaces the schedule cfg.
    // ---------------------------------------------------------------

    private static class DataApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if ("/config".equals(path)) {
                json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().autoUpdate));
            } else {
                // Default GET / and GET /status both return the live status snapshot
                json(res, ConfigManager.gson().toJson(DataUpdateService.getInstance().getStatus()));
            }
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            try {
                if ("/refresh".equals(path)) {
                    DataUpdateService.getInstance().updateNow();
                    json(res, "{\"status\":\"refresh started\"}");
                    return;
                }
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.AutoUpdateSection cfg =
                        ConfigManager.gson().fromJson(body, JHubConfig.AutoUpdateSection.class);
                ConfigManager.getInstance().getConfig().autoUpdate = cfg;
                ConfigManager.getInstance().save();
                DataUpdateService.getInstance().restart(cfg);
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
    // /api/jlog/restart — send SHUTDOWN to the j-log UI and relaunch
    // ---------------------------------------------------------------

    private class JLogRestartServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JHubServer server = router.getJHubServer();
            // The j-log UI registers as "logging-engine" on the hub; send it
            // a SHUTDOWN so JLogApp.setOnShutdown fires and Platform.exit()
            // runs the stop() hook (WebSocket close + DB close).
            if (server != null) {
                server.broadcastToAppName("logging-engine",
                    "{\"type\":\"SHUTDOWN\",\"reason\":\"restart-requested\"}");
            }
            // Re-launch after a short delay so the old process has time to
            // release its ports/file locks. Runs on a daemon thread so the
            // HTTP response returns immediately.
            Thread t = new Thread(() -> {
                try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
                try {
                    JHubConfig.AppsSection apps = ConfigManager.getInstance().getApps();
                    String cmd = (apps != null && apps.jLog != null) ? apps.jLog.command : null;
                    if (cmd == null || cmd.isBlank()) {
                        log.warn("Cannot restart j-log: no launch command configured");
                        return;
                    }
                    AppLauncher.getInstance().launch("j-log", cmd);
                } catch (Exception e) {
                    log.warn("j-log relaunch failed: {}", e.getMessage());
                }
            }, "jlog-restart");
            t.setDaemon(true);
            t.start();

            json(res, "{\"status\":\"restarting\"}");
        }
        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/{jmap,jdigi,jbridge,jsat}/restart — generic shutdown + relaunch
    // ---------------------------------------------------------------

    /** Restart one of the managed apps: broadcast SHUTDOWN to its connected
     *  session, wait briefly for the process to unwind, then re-launch via
     *  {@link AppLauncher} using the command saved in {@code j-hub.json}. */
    private class AppRestartServlet extends HttpServlet {
        private final String launcherKey;   // e.g. "j-map" — key for AppLauncher + config
        private final String wsAppName;     // e.g. "j-map" — appName used on the WebSocket
        AppRestartServlet(String launcherKey, String wsAppName) {
            this.launcherKey = launcherKey;
            this.wsAppName   = wsAppName;
        }
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            JHubServer server = router.getJHubServer();
            if (server != null) {
                server.broadcastToAppName(wsAppName,
                    "{\"type\":\"SHUTDOWN\",\"reason\":\"restart-requested\"}");
            }
            Thread t = new Thread(() -> {
                try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
                try {
                    JHubConfig.AppsSection apps = ConfigManager.getInstance().getApps();
                    JHubConfig.AppLaunchEntry entry = entryFor(apps, launcherKey);
                    String cmd = (entry != null) ? entry.command : null;
                    if (cmd == null || cmd.isBlank()) {
                        log.warn("Cannot restart {}: no launch command configured", launcherKey);
                        return;
                    }
                    AppLauncher.getInstance().launch(launcherKey, cmd);
                } catch (Exception e) {
                    log.warn("{} relaunch failed: {}", launcherKey, e.getMessage());
                }
            }, launcherKey + "-restart");
            t.setDaemon(true);
            t.start();
            json(res, "{\"status\":\"restarting\"}");
        }
        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
        private JHubConfig.AppLaunchEntry entryFor(JHubConfig.AppsSection apps, String name) {
            if (apps == null) return null;
            if ("jMap".equals(name))     return apps.jMap;
            if ("j-log".equals(name))    return apps.jLog;
            if ("j-bridge".equals(name)) return apps.jBridge;
            if ("j-digi".equals(name))   return apps.jDigi;
            if ("j-sat".equals(name))    return apps.jSat;
            if ("jVault".equals(name))   return apps.jVault;
            if ("j-learn".equals(name))  return apps.jLearn;
            return null;
        }
    }

    // ---------------------------------------------------------------
    // /api/diagnostics/bundle — zip of all module logs + config snapshot
    //
    // Used by beta testers to attach a support bundle to bug reports. Walks
    // the standard ARS_Suite layout ($ARS_SUITE_HOME or ~/ARS_Suite) and
    // collects every module's logs/*.log plus the active j-hub.json, the
    // current session list, and deps-check output.
    // ---------------------------------------------------------------

    private class DiagnosticsBundleServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String stamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            res.setContentType("application/zip");
            res.setHeader("Content-Disposition",
                "attachment; filename=\"ars-diag-" + stamp + ".zip\"");
            res.setHeader("Access-Control-Allow-Origin", "*");

            java.nio.file.Path arsRoot = resolveArsRoot();
            try (java.util.zip.ZipOutputStream zip =
                    new java.util.zip.ZipOutputStream(res.getOutputStream())) {

                // 1. Logs from each module
                String[] modules = { "j-hub", "j-log", "j-map", "j-digi", "j-bridge", "j-sat", "j-wae" };
                for (String m : modules) {
                    java.nio.file.Path logs = arsRoot.resolve(m).resolve("logs");
                    if (!java.nio.file.Files.isDirectory(logs)) continue;
                    try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(logs)) {
                        for (java.nio.file.Path f : s.toList()) {
                            if (!java.nio.file.Files.isRegularFile(f)) continue;
                            String entry = m + "/logs/" + f.getFileName();
                            zip.putNextEntry(new java.util.zip.ZipEntry(entry));
                            java.nio.file.Files.copy(f, zip);
                            zip.closeEntry();
                        }
                    }
                }

                // 2. j-hub config snapshot
                writeZipEntry(zip, "j-hub/j-hub.json",
                    ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig()));

                // 3. Live session list + heartbeat timestamps
                com.google.gson.JsonArray sessions = new com.google.gson.JsonArray();
                JHubServer server = router.getJHubServer();
                if (server != null) {
                    java.time.Instant now = java.time.Instant.now();
                    for (JHubServer.AppSession s : server.getSessions().values()) {
                        if (!s.registered) continue;
                        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                        o.addProperty("appName",   s.appName);
                        o.addProperty("version",   s.version);
                        o.addProperty("connectedAt", s.connectedAt.toString());
                        o.addProperty("ageSeconds", java.time.Duration.between(s.connectedAt, now).getSeconds());
                        sessions.add(o);
                    }
                }
                writeZipEntry(zip, "snapshot/sessions.json", sessions.toString());

                // 4. External dependency check
                writeZipEntry(zip, "snapshot/deps.json",
                    DependencyChecker.checkAll().toString());

                // 5. Environment summary — Java version, OS, ARS_SUITE_HOME
                com.google.gson.JsonObject env = new com.google.gson.JsonObject();
                env.addProperty("javaVersion",   System.getProperty("java.version", ""));
                env.addProperty("javaVendor",    System.getProperty("java.vendor",  ""));
                env.addProperty("osName",        System.getProperty("os.name",      ""));
                env.addProperty("osArch",        System.getProperty("os.arch",      ""));
                env.addProperty("osVersion",     System.getProperty("os.version",   ""));
                env.addProperty("userHome",      System.getProperty("user.home",    ""));
                env.addProperty("arsSuiteHome",  System.getenv().getOrDefault("ARS_SUITE_HOME", ""));
                env.addProperty("resolvedRoot",  arsRoot.toString());
                env.addProperty("bundledAt",     java.time.Instant.now().toString());
                writeZipEntry(zip, "snapshot/environment.json", env.toString());

            } catch (Exception e) {
                log.warn("Diagnostics bundle failed: {}", e.getMessage());
                // Best-effort; client already has a partial zip.
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private java.nio.file.Path resolveArsRoot() {
            String env = System.getenv("ARS_SUITE_HOME");
            if (env != null && !env.isBlank()) return java.nio.file.Path.of(env);
            return java.nio.file.Path.of(System.getProperty("user.home", ""), "ARS_Suite");
        }

        private void writeZipEntry(java.util.zip.ZipOutputStream zip, String name, String content) throws IOException {
            zip.putNextEntry(new java.util.zip.ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    // ---------------------------------------------------------------
    // /api/deps — probe Hamlib + WSJT-X presence and version
    // ---------------------------------------------------------------

    private static class DepsApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, DependencyChecker.checkAll().toString());
        }
        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/sessions — active WebSocket sessions + heartbeat timestamps
    // ---------------------------------------------------------------

    private class SessionsApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            JHubServer server = router.getJHubServer();
            if (server != null) {
                java.time.Instant now = java.time.Instant.now();
                for (JHubServer.AppSession s : server.getSessions().values()) {
                    if (!s.registered) continue;
                    com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                    o.addProperty("appName",       s.appName);
                    o.addProperty("version",       s.version);
                    o.addProperty("connectedAt",   s.connectedAt.toString());
                    o.addProperty("ageSeconds",    java.time.Duration.between(s.connectedAt, now).getSeconds());
                    o.addProperty("lastMessageAgeSeconds",
                        s.lastMessageAt != null ? java.time.Duration.between(s.lastMessageAt, now).getSeconds() : -1);
                    o.addProperty("lastHeartbeatAgeSeconds",
                        s.lastHeartbeatAt != null ? java.time.Duration.between(s.lastHeartbeatAt, now).getSeconds() : -1);
                    arr.add(o);
                }
            }
            json(res, arr.toString());
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

            } else if ("/backup".equals(path)) {
                // Create a timestamped copy of the active DB alongside it in ~/.j-log/
                try {
                    String active = readActiveDb();
                    java.nio.file.Path src = LOG_DIR.resolve(active);
                    if (!java.nio.file.Files.exists(src)) {
                        res.setStatus(404);
                        json(res, "{\"error\":\"Active DB not found: " + active + "\"}");
                        return;
                    }
                    String stamp = java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                    String backupName = active + ".bak-" + stamp;
                    java.nio.file.Path dest = LOG_DIR.resolve(backupName);
                    java.nio.file.Files.copy(src, dest,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    json(res, "{\"backup\":\"" + backupName + "\"}");
                } catch (Exception e) {
                    res.setStatus(500);
                    json(res, "{\"error\":\"" + e.getMessage() + "\"}");
                }

            } else if ("/import/adif".equals(path)) {
                // Multipart upload → temp file → ask j-log to run the
                // existing AdifImporter. j-log must be running; j-hub has no
                // ADIF parser of its own.
                try {
                    JHubServer jhubServer = router.getJHubServer();
                    if (jhubServer == null || !jhubServer.hasAppConnected("logging-engine")) {
                        res.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        json(res, "{\"error\":\"J-Log is not connected — start J-Log first\"}");
                        return;
                    }
                    jakarta.servlet.MultipartConfigElement mpConfig =
                        new jakarta.servlet.MultipartConfigElement(
                            System.getProperty("java.io.tmpdir"),
                            100L * 1024 * 1024, 100L * 1024 * 1024, 1024 * 1024);
                    req.setAttribute("org.eclipse.jetty.multipartConfig", mpConfig);
                    jakarta.servlet.http.Part part = req.getPart("adif");
                    if (part == null) {
                        res.setStatus(400);
                        json(res, "{\"error\":\"No file part named 'adif'\"}");
                        return;
                    }
                    java.nio.file.Path tmp = java.nio.file.Files.createTempFile("jhub-import-", ".adi");
                    try (java.io.InputStream in = part.getInputStream()) {
                        java.nio.file.Files.copy(in, tmp,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                    com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                    msg.addProperty("type", "IMPORT_ADIF");
                    msg.addProperty("path", tmp.toString());
                    jhubServer.broadcastToAppName("logging-engine", msg.toString());
                    json(res, "{\"status\":\"queued\",\"path\":\"" + tmp + "\"}");
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

    // J-Learn moved out — it's a standalone module on port 8082. The web UI
    // iframes it; AppLauncher manages the j-learn process lifecycle.

    // ---------------------------------------------------------------
    // /api/rbn — GET/POST RBN config; restarts the client on change
    // ---------------------------------------------------------------

    private static class RbnApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            com.google.gson.JsonObject body = new com.google.gson.JsonObject();
            JHubConfig.RbnSection cfg = ConfigManager.getInstance().getConfig().rbn;
            body.add("config", ConfigManager.gson().toJsonTree(cfg));
            body.addProperty("connected", RbnClient.getInstance().isConnected());
            body.addProperty("running",   RbnClient.getInstance().isRunning());
            json(res, body.toString());
        }
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.RbnSection rbn = ConfigManager.gson().fromJson(body, JHubConfig.RbnSection.class);
                ConfigManager.getInstance().getConfig().rbn = rbn;
                ConfigManager.getInstance().save();
                RbnClient.getInstance().restart();
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
    // /api/backup — cloud backup config + status; POST /api/backup/run
    // ---------------------------------------------------------------

    private static class BackupApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if ("/status".equals(path)) {
                json(res, ConfigManager.gson().toJson(CloudBackupService.getInstance().snapshot()));
            } else {
                json(res, ConfigManager.gson().toJson(ConfigManager.getInstance().getConfig().backup));
            }
        }
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if ("/run".equals(path)) {
                CloudBackupService.BackupResult r = CloudBackupService.getInstance().runOnce();
                json(res, ConfigManager.gson().toJson(r));
                return;
            }
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                JHubConfig.BackupSection b =
                    ConfigManager.gson().fromJson(body, JHubConfig.BackupSection.class);
                ConfigManager.getInstance().getConfig().backup = b;
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
    // /api/credentials/{serviceId} — get/put credentials (encrypted)
    //
    // GET returns ONLY which fields are populated (not their values), so
    // the UI can show "✓ saved" without leaking plaintext back over HTTP.
    // ---------------------------------------------------------------

    private static class CredentialsApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String svc = idFromPath(req);
            if (svc == null) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                for (String s : CredentialStore.getInstance().listServices()) arr.add(s);
                json(res, arr.toString());
                return;
            }
            com.google.gson.JsonObject creds = CredentialStore.getInstance().get(svc);
            com.google.gson.JsonObject mask  = new com.google.gson.JsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e : creds.entrySet()) {
                mask.addProperty(e.getKey(), true);   // value omitted
            }
            json(res, mask.toString());
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String svc = idFromPath(req);
            if (svc == null) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"missing service id\"}"); return;
            }
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject creds =
                    com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                CredentialStore.getInstance().put(svc, creds);
                json(res, "{\"status\":\"saved\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String svc = idFromPath(req);
            if (svc == null) { res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return; }
            CredentialStore.getInstance().remove(svc);
            json(res, "{\"status\":\"removed\"}");
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private static String idFromPath(HttpServletRequest req) {
            String p = req.getPathInfo();
            if (p == null || p.length() < 2) return null;
            return p.substring(1);  // strip leading "/"
        }
    }

    // ---------------------------------------------------------------
    // /api/uploaders — list configured uploaders + push pending QSOs
    // ---------------------------------------------------------------

    private static class UploadersApiServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, ConfigManager.gson().toJson(LogUploaderRegistry.snapshot()));
        }
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if (!path.startsWith("/upload/")) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"POST /api/uploaders/upload/{serviceId}\"}"); return;
            }
            String svc = path.substring("/upload/".length());
            LogUploader.UploadResult r = LogUploaderRegistry.upload(svc);
            json(res, ConfigManager.gson().toJson(r));
        }
        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    // ---------------------------------------------------------------
    // /api/voice/upload — POST multipart WAV; returns { path }
    // /api/voice/file?path=...  — GET serves a previously-uploaded WAV
    // /api/macros/trigger — POST { key } broadcasts MACRO_TRIGGER over WS
    // ---------------------------------------------------------------

    private static final java.nio.file.Path VOICE_DIR =
        java.nio.file.Path.of(System.getProperty("user.home"), ".j-hub", "voice");

    private static class VoiceUploadServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                jakarta.servlet.MultipartConfigElement mp =
                    new jakarta.servlet.MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"), 16 * 1024 * 1024, 16 * 1024 * 1024, 1024 * 1024);
                req.setAttribute("org.eclipse.jetty.multipartConfig", mp);

                jakarta.servlet.http.Part filePart = req.getPart("file");
                jakarta.servlet.http.Part namePart = req.getPart("name");
                if (filePart == null) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"missing 'file' part\"}");
                    return;
                }
                String name = namePart != null
                    ? new String(namePart.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim()
                    : "macro_" + System.currentTimeMillis();
                name = name.replaceAll("[^A-Za-z0-9._-]", "_");
                if (!name.toLowerCase().endsWith(".wav")) name += ".wav";

                java.nio.file.Files.createDirectories(VOICE_DIR);
                java.nio.file.Path dest = VOICE_DIR.resolve(name);
                try (java.io.InputStream in = filePart.getInputStream()) {
                    java.nio.file.Files.copy(in, dest,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                log.info("Voice macro WAV saved: {} ({} bytes)", dest, java.nio.file.Files.size(dest));
                json(res, "{\"path\":\"" + dest.toString().replace("\\", "\\\\") + "\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }
    }

    private static class VoiceFileServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getParameter("path");
            if (path == null || path.isBlank()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST); return;
            }
            java.nio.file.Path p = java.nio.file.Path.of(path).toAbsolutePath().normalize();
            // Confine reads to ~/.j-hub/voice — refuse anything else (path-traversal guard)
            if (!p.startsWith(VOICE_DIR.toAbsolutePath().normalize())) {
                res.setStatus(HttpServletResponse.SC_FORBIDDEN); return;
            }
            if (!java.nio.file.Files.isRegularFile(p)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND); return;
            }
            res.setContentType("audio/wav");
            res.setHeader("Access-Control-Allow-Origin", "*");
            try (java.io.OutputStream out = res.getOutputStream()) {
                java.nio.file.Files.copy(p, out);
            }
        }
    }

    private static class MacroTriggerServlet extends HttpServlet {
        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            try {
                String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                com.google.gson.JsonObject in = com.google.gson.JsonParser.parseString(body).getAsJsonObject();
                String key = in.has("key") ? in.get("key").getAsString() : "";
                if (key.isBlank()) {
                    res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    json(res, "{\"error\":\"missing 'key'\"}"); return;
                }
                JHubConfig.MacrosSection ms = ConfigManager.getInstance().getConfig().macros;
                JHubConfig.MacroDefinition def = null;
                if (ms != null && ms.list != null) {
                    for (JHubConfig.MacroDefinition d : ms.list) {
                        if (key.equals(d.key)) { def = d; break; }
                    }
                }
                if (def == null) {
                    res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    json(res, "{\"error\":\"unknown macro key\"}"); return;
                }
                com.google.gson.JsonObject msg = new com.google.gson.JsonObject();
                msg.addProperty("type",    "MACRO_TRIGGER");
                msg.addProperty("key",     def.key);
                msg.addProperty("kind",    def.kind == null ? "CW" : def.kind);
                msg.addProperty("text",    def.text == null ? "" : def.text);
                msg.addProperty("wavPath", def.wavPath == null ? "" : def.wavPath);
                msg.addProperty("source",  "j-hub-web");
                JHubServer s = MessageRouter.getInstance().getJHubServer();
                if (s != null) s.broadcastToAll(msg.toString());
                json(res, "{\"status\":\"triggered\"}");
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
    // /api/morsetrainer — launches the standalone morse-trainer JavaFX app.
    //
    //   POST /api/morsetrainer/launch   → spawn morse-trainer/run.sh detached
    // ---------------------------------------------------------------

    private static class MorseTrainerLaunchServlet extends HttpServlet {

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            if (!"/launch".equals(path)) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"unknown morsetrainer endpoint\"}");
                return;
            }
            java.nio.file.Path script = locateRunScript();
            if (script == null) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"morse-trainer/run.sh not found near working dir\"}");
                return;
            }
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", script.toString())
                        .directory(script.getParent().toFile())
                        .redirectErrorStream(true)
                        .redirectOutput(ProcessBuilder.Redirect.DISCARD);
                pb.start();
                json(res, "{\"status\":\"launched\",\"path\":" +
                        ConfigManager.gson().toJson(script.toString()) + "}");
            } catch (IOException e) {
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                json(res, "{\"error\":" + ConfigManager.gson().toJson(e.getMessage()) + "}");
            }
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            cors(res); res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        /**
         * Walk up from user.dir looking for a sibling morse-trainer/run.sh.
         * Stops after a few levels so it can't run away on a misconfigured host.
         */
        private static java.nio.file.Path locateRunScript() {
            java.nio.file.Path here = java.nio.file.Paths.get(System.getProperty("user.dir"));
            for (int i = 0; i < 4 && here != null; i++, here = here.getParent()) {
                java.nio.file.Path candidate = here.resolve("morse-trainer").resolve("run.sh");
                if (java.nio.file.Files.isRegularFile(candidate)) return candidate;
                java.nio.file.Path sibling = here.resolveSibling("morse-trainer").resolve("run.sh");
                if (java.nio.file.Files.isRegularFile(sibling)) return sibling;
            }
            return null;
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
