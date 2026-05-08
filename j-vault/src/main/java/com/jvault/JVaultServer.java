package com.jvault;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletException;
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
 * JVaultServer — embedded Jetty serving the J-Vault inventory web UI
 * + REST API on a single port (default 8083).
 *
 * Endpoints:
 *   GET  /                          → index.html
 *   GET  /api/inventory/types       → list equipment types
 *   POST /api/inventory/types       → create type
 *   DEL  /api/inventory/types/:id   → delete type
 *   GET  /api/inventory/items       → list items
 *   GET  /api/inventory/items/:id   → get one item
 *   POST /api/inventory/items       → create item
 *   PUT  /api/inventory/items/:id   → update item
 *   DEL  /api/inventory/items/:id   → delete item
 *   GET  /api/inventory/export.csv  → CSV export
 *   GET  /api/inventory/contacts    → list first-call contacts
 *   POST /api/inventory/contacts    → create contact
 *   PUT  /api/inventory/contacts/:id → update
 *   DEL  /api/inventory/contacts/:id → delete
 *   GET  /api/health                → "{\"ok\":true}"
 */
public class JVaultServer {

    private static final Logger log = LoggerFactory.getLogger(JVaultServer.class);
    private static final Gson GSON = new Gson();

    private final int port;
    private Server server;

    public JVaultServer(int port) { this.port = port; }

    public void start() throws Exception {
        server = new Server(port);
        ServletContextHandler ctx = new ServletContextHandler(ServletContextHandler.SESSIONS);
        ctx.setContextPath("/");

        ctx.addServlet(new ServletHolder(new InventoryApiServlet()), "/api/inventory/*");
        ctx.addServlet(new ServletHolder(new HealthServlet()),       "/api/health");
        // Static fallback (everything else from /web/ on the classpath).
        ctx.addServlet(new ServletHolder(new StaticServlet()),       "/*");

        server.setHandler(ctx);
        server.start();
        log.info("J-Vault web UI listening on http://localhost:{}", port);
    }

    public void stop() {
        if (server != null) {
            try { server.stop(); } catch (Exception ignore) {}
        }
    }

    static Gson gson() { return GSON; }

    static void json(HttpServletResponse res, String body) throws IOException {
        res.setContentType("application/json; charset=utf-8");
        res.setHeader("Access-Control-Allow-Origin", "*");
        try (PrintWriter w = res.getWriter()) { w.write(body); }
    }

    // ----- /api/health ----------------------------------------------------

    private static class HealthServlet extends HttpServlet {
        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            json(res, "{\"ok\":true,\"app\":\"j-vault\",\"version\":\"1.0.0\"}");
        }
    }

    // ----- Static files ---------------------------------------------------

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
            else if (path.endsWith(".md"))   res.setContentType("text/markdown; charset=utf-8");
            else if (path.endsWith(".png"))  res.setContentType("image/png");
            else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) res.setContentType("image/jpeg");
            else                             res.setContentType("application/octet-stream");

            res.getOutputStream().write(is.readAllBytes());
        }
    }

    // ----- /api/inventory ------------------------------------------------

    private static class InventoryApiServlet extends HttpServlet {

        @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            InventoryDao dao = InventoryDao.getInstance();

            if ("/types".equals(path))    { json(res, dao.listTypes().toString());    return; }
            if ("/items".equals(path))    { json(res, dao.listItems().toString());    return; }
            if ("/contacts".equals(path)) { json(res, dao.listContacts().toString()); return; }
            if ("/export.csv".equals(path)) {
                res.setContentType("text/csv; charset=utf-8");
                res.setHeader("Access-Control-Allow-Origin", "*");
                res.setHeader("Content-Disposition", "attachment; filename=\"shack-inventory.csv\"");
                try (PrintWriter w = res.getWriter()) { w.write(dao.exportCsv()); }
                return;
            }

            Integer id = parseTrailingId(path, "/items/");
            if (id != null) {
                JsonObject item = dao.getItem(id);
                if (item == null) { res.setStatus(HttpServletResponse.SC_NOT_FOUND); json(res, "{\"error\":\"item not found\"}"); return; }
                json(res, item.toString());
                return;
            }
            id = parseTrailingId(path, "/contacts/");
            if (id != null) {
                JsonObject c = dao.getContact(id);
                if (c == null) { res.setStatus(HttpServletResponse.SC_NOT_FOUND); json(res, "{\"error\":\"contact not found\"}"); return; }
                json(res, c.toString());
                return;
            }
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json(res, "{\"error\":\"unknown inventory endpoint: " + path + "\"}");
        }

        @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            InventoryDao dao = InventoryDao.getInstance();
            try {
                JsonObject body = readJson(req);

                if ("/types".equals(path)) {
                    String name = body.has("name") ? body.get("name").getAsString() : null;
                    if (name == null || name.isBlank()) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        json(res, "{\"error\":\"name required\"}");
                        return;
                    }
                    int order = body.has("display_order") ? body.get("display_order").getAsInt() : 500;
                    JsonObject created = dao.createType(name, order);
                    if (created == null) {
                        res.setStatus(HttpServletResponse.SC_CONFLICT);
                        json(res, "{\"error\":\"could not create type (duplicate name?)\"}");
                        return;
                    }
                    json(res, created.toString());
                    return;
                }
                if ("/items".equals(path)) {
                    JsonObject created = dao.createItem(body);
                    if (created == null) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        json(res, "{\"error\":\"could not create item\"}");
                        return;
                    }
                    json(res, created.toString());
                    return;
                }
                if ("/contacts".equals(path)) {
                    JsonObject created = dao.createContact(body);
                    if (created == null) {
                        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        json(res, "{\"error\":\"could not create contact (name required?)\"}");
                        return;
                    }
                    json(res, created.toString());
                    return;
                }
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"unknown POST endpoint: " + path + "\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            InventoryDao dao = InventoryDao.getInstance();
            try {
                JsonObject body = readJson(req);

                Integer id = parseTrailingId(path, "/items/");
                if (id != null) {
                    JsonObject updated = dao.updateItem(id, body);
                    if (updated == null) { res.setStatus(HttpServletResponse.SC_NOT_FOUND); json(res, "{\"error\":\"item not found\"}"); return; }
                    json(res, updated.toString());
                    return;
                }
                id = parseTrailingId(path, "/contacts/");
                if (id != null) {
                    JsonObject updated = dao.updateContact(id, body);
                    if (updated == null) { res.setStatus(HttpServletResponse.SC_NOT_FOUND); json(res, "{\"error\":\"contact not found\"}"); return; }
                    json(res, updated.toString());
                    return;
                }
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                json(res, "{\"error\":\"unknown PUT endpoint: " + path + "\"}");
            } catch (Exception e) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                json(res, "{\"error\":\"" + e.getMessage() + "\"}");
            }
        }

        @Override protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws IOException {
            String path = req.getPathInfo() == null ? "" : req.getPathInfo();
            InventoryDao dao = InventoryDao.getInstance();

            Integer id = parseTrailingId(path, "/types/");
            if (id != null) { json(res, "{\"deleted\":" + dao.deleteType(id) + "}"); return; }
            id = parseTrailingId(path, "/items/");
            if (id != null) { json(res, "{\"deleted\":" + dao.deleteItem(id) + "}"); return; }
            id = parseTrailingId(path, "/contacts/");
            if (id != null) { json(res, "{\"deleted\":" + dao.deleteContact(id) + "}"); return; }
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            json(res, "{\"error\":\"unknown DELETE endpoint: " + path + "\"}");
        }

        @Override protected void doOptions(HttpServletRequest req, HttpServletResponse res) {
            res.setHeader("Access-Control-Allow-Origin",  "*");
            res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            res.setHeader("Access-Control-Allow-Headers", "Content-Type");
            res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        }

        private static JsonObject readJson(HttpServletRequest req) throws IOException {
            String body = new String(req.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (body.isBlank()) return new JsonObject();
            return GSON.fromJson(body, JsonObject.class);
        }

        private static Integer parseTrailingId(String path, String prefix) {
            if (path == null || !path.startsWith(prefix)) return null;
            String tail = path.substring(prefix.length());
            if (tail.isEmpty() || tail.contains("/")) return null;
            try { return Integer.parseInt(tail); }
            catch (NumberFormatException e) { return null; }
        }
    }
}
