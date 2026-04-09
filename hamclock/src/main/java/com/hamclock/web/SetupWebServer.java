package com.hamclock.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.config.Settings;
import com.hamclock.service.config.SettingsLoader;
import fi.iki.elonen.NanoHTTPD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Embedded NanoHTTPD web server providing the Setup Page.
 *
 * Routes:
 *   GET  /setup         → Setup Page HTML (mobile-friendly)
 *   POST /api/settings  → Save settings (JSON body), triggers live update
 *   GET  /api/settings  → Return current settings as JSON
 *   GET  /api/status    → Return system status
 *
 * This server is intentionally separate from the JavaFX UI.
 * The Setup Page NEVER appears on the main display.
 */
public class SetupWebServer extends NanoHTTPD {

    private static final Logger log = LoggerFactory.getLogger(SetupWebServer.class);

    private final ServiceRegistry services;
    private volatile Settings settings;
    private final ObjectMapper mapper = new ObjectMapper();

    public SetupWebServer(Settings settings, ServiceRegistry services, int port) throws IOException {
        super(port);
        this.settings = settings;
        this.services = services;
    }

    @Override
    public void start() throws IOException {
        super.start(SOCKET_READ_TIMEOUT, true); // daemon threads
        log.info("Setup web server started on port {}", getListeningPort());
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();

        try {
            // CORS headers for development convenience
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            headers.put("Access-Control-Allow-Headers", "Content-Type");

            if (Method.OPTIONS.equals(method)) {
                Response r = newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "");
                headers.forEach(r::addHeader);
                return r;
            }

            Response response = switch (uri) {
                case "/", "/setup" -> serveSetupPage();
                case "/api/settings" -> {
                    if (Method.POST.equals(method)) yield handleSaveSettings(session);
                    else yield handleGetSettings();
                }
                case "/api/status"         -> handleStatus();
                case "/api/rotor"          -> handleRotorStatus();
                case "/api/dxcluster/feed" -> handleClusterFeed();
                default -> newFixedLengthResponse(Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT, "404 Not Found: " + uri);
            };

            headers.forEach(response::addHeader);
            return response;

        } catch (Exception e) {
            log.error("Error serving {}: {}", uri, e.getMessage(), e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT, "500 Internal Server Error: " + e.getMessage());
        }
    }

    private Response serveSetupPage() {
        String html = SetupPageHtml.generate(settings);
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html);
    }

    private Response handleGetSettings() throws Exception {
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(settings);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handleSaveSettings(IHTTPSession session) throws Exception {
        // Read request body
        Map<String, String> body = new HashMap<>();
        session.parseBody(body);
        String json = body.getOrDefault("postData", "{}");

        if (json.isBlank() && session.getInputStream().available() > 0) {
            json = new String(session.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }

        // Parse and validate new settings
        Settings newSettings = mapper.readValue(json, Settings.class);
        this.settings = newSettings;

        // Persist to disk
        SettingsLoader.save(newSettings);

        // Trigger live update in JavaFX UI (non-blocking)
        Thread t = new Thread(() -> services.onSettingsChanged(newSettings));
        t.setDaemon(true);
        t.start();

        log.info("Settings updated via Setup Page");

        String resp = "{\"status\":\"ok\",\"message\":\"Settings saved and applied\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", resp);
    }

    private Response handleStatus() throws Exception {
        Map<String, Object> status = new HashMap<>();
        status.put("callsign", settings.getCallsign());
        status.put("uptime", System.currentTimeMillis());
        status.put("useMockData", settings.isUseMockData());

        var solar = services.solarDataProvider;
        status.put("solarLastUpdated", solar.getLastUpdated() != null ? solar.getLastUpdated().toString() : null);
        status.put("solarStale", solar.isStale(java.time.Duration.ofMinutes(30)));

        var prop = services.propagationDataProvider;
        status.put("propLastUpdated", prop.getLastUpdated() != null ? prop.getLastUpdated().toString() : null);

        String json = mapper.writeValueAsString(status);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handleRotorStatus() throws Exception {
        var rotorData = services.rotorProvider.getCached();
        String json = rotorData != null ? mapper.writeValueAsString(rotorData) : "null";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response handleClusterFeed() throws Exception {
        var client = services.dxClusterClient;
        Map<String, Object> resp = new HashMap<>();
        resp.put("connected", client.isConnected());
        resp.put("status", client.getStatusMessage());
        resp.put("lines", client.getRecentLines());
        return newFixedLengthResponse(Response.Status.OK, "application/json",
            mapper.writeValueAsString(resp));
    }

}
