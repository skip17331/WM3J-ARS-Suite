package com.hamradio.jsat.api;

import com.hamradio.jsat.model.TleSet;
import com.hamradio.jsat.service.tle.TleManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.Executors;

/**
 * JsatApiServer — lightweight HTTP API server making J-Sat the authoritative
 * TLE source for all ARS Suite modules.
 *
 * Endpoints:
 *   GET  /api/tle/all       — all TLEs with stale flag
 *   GET  /api/tle/{id}      — single TLE by NORAD ID or name
 *   GET  /api/tle/stale     — only stale TLEs
 *   POST /api/tle/refresh   — force TLE refresh from Celestrak
 *   GET  /api/status        — module health check
 */
public class JsatApiServer {

    private static final Logger log = LoggerFactory.getLogger(JsatApiServer.class);

    private final TleManager tleManager;
    private final int        port;
    private final int        staleThresholdHours;
    private HttpServer       server;

    public JsatApiServer(TleManager tleManager, int port, int staleThresholdHours) {
        this.tleManager          = tleManager;
        this.port                = port;
        this.staleThresholdHours = staleThresholdHours;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/tle",    this::handleTle);
        server.createContext("/api/status", this::handleStatus);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "jsat-api");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        log.info("J-Sat TLE API server started on port {}", port);
    }

    public void stop() {
        if (server != null) server.stop(0);
    }

    // ── /api/status ──────────────────────────────────────────────────────────

    private void handleStatus(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendResponse(ex, 405, "{\"error\":\"method not allowed\"}");
            return;
        }
        String body = "{\"module\":\"J-Sat\",\"status\":\"online\"}";
        sendResponse(ex, 200, body);
    }

    // ── /api/tle/* ───────────────────────────────────────────────────────────

    private void handleTle(HttpExchange ex) throws IOException {
        String path   = ex.getRequestURI().getPath(); // e.g. /api/tle/all
        String method = ex.getRequestMethod();
        cors(ex);

        // POST /api/tle/refresh
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/refresh")) {
            tleManager.updateNow();
            sendResponse(ex, 200, "{\"status\":\"refreshed\",\"count\":" + tleManager.allTles().size() + "}");
            return;
        }

        if (!"GET".equalsIgnoreCase(method)) {
            sendResponse(ex, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        // GET /api/tle/all
        if (path.endsWith("/all") || path.endsWith("/tle")) {
            sendResponse(ex, 200, buildTleListJson(tleManager.allTles(), false));
            return;
        }

        // GET /api/tle/stale
        if (path.endsWith("/stale")) {
            Collection<TleSet> stale = tleManager.allTles().stream()
                .filter(t -> t.ageInDays() * 24 > staleThresholdHours)
                .toList();
            sendResponse(ex, 200, buildTleListJson(stale, true));
            return;
        }

        // GET /api/tle/{id}  — NORAD ID or satellite name
        String id = path.substring(path.lastIndexOf('/') + 1);
        if (!id.isBlank() && !id.equals("tle")) {
            TleSet t = tleManager.findByNorad(id);
            if (t == null) t = tleManager.findByName(id);
            if (t == null) {
                sendResponse(ex, 404, "{\"error\":\"satellite not found\"}");
            } else {
                sendResponse(ex, 200, tleToJson(t));
            }
            return;
        }

        sendResponse(ex, 400, "{\"error\":\"invalid path\"}");
    }

    // ── JSON builders ─────────────────────────────────────────────────────────

    private String buildTleListJson(Collection<TleSet> tles, boolean staleOnly) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (TleSet t : tles) {
            boolean stale = t.ageInDays() * 24 > staleThresholdHours;
            if (staleOnly && !stale) continue;
            if (!first) sb.append(',');
            sb.append(tleToJson(t));
            first = false;
        }
        sb.append(']');
        return sb.toString();
    }

    private String tleToJson(TleSet t) {
        boolean stale = t.ageInDays() * 24 > staleThresholdHours;
        String lastUpdated = t.fetchedAt != null ? t.fetchedAt.toString() : "unknown";
        return String.format(
            "{\"name\":\"%s\",\"noradId\":\"%s\",\"line1\":\"%s\",\"line2\":\"%s\"," +
            "\"ageHours\":%.1f,\"stale\":%b,\"freshness\":\"%s\",\"lastUpdated\":\"%s\"}",
            escape(t.name), escape(t.noradId), escape(t.line1), escape(t.line2),
            t.ageInDays() * 24, stale, t.freshness().name(), lastUpdated
        );
    }

    // ── HTTP helpers ─────────────────────────────────────────────────────────

    private void sendResponse(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void cors(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
