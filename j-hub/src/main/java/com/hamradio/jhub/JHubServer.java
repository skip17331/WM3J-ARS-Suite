package com.hamradio.jhub;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hamradio.jhub.model.RigStatus;
import com.hamradio.jhub.model.Spot;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JHubServer — WebSocket server that manages all connected application sessions.
 *
 * Each connecting app must immediately send an APP_CONNECTED registration
 * message.  After registration j-hub:
 *   • stores the connection metadata
 *   • replays cached state (rig status, recent spots, logger session)
 *   • routes subsequent messages through MessageRouter
 *
 * All public methods are thread-safe; the underlying WebSocketServer uses
 * one thread per connection for onMessage callbacks.
 */
public class JHubServer extends WebSocketServer {

    private static final Logger log = LoggerFactory.getLogger(JHubServer.class);

    // Metadata tracked per connected socket
    public static class AppSession {
        public final WebSocket socket;
        public String appName    = "unknown";
        public String version    = "";
        public final Instant connectedAt = Instant.now();
        public boolean registered = false;

        AppSession(WebSocket socket) { this.socket = socket; }
    }

    // Active sessions — keyed by the WebSocket reference
    private final Map<WebSocket, AppSession> sessions = new ConcurrentHashMap<>();

    private final MessageRouter router;
    private final StateCache    cache;

    public JHubServer(InetSocketAddress address, MessageRouter router, StateCache cache) {
        super(address);
        this.router = router;
        this.cache  = cache;
        setReuseAddr(true);
    }

    // ---------------------------------------------------------------
    // WebSocketServer callbacks
    // ---------------------------------------------------------------

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        AppSession session = new AppSession(conn);
        sessions.put(conn, session);
        log.info("New connection from {}", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        AppSession session = sessions.remove(conn);
        if (session != null) {
            log.info("App '{}' disconnected (code={}, reason={})", session.appName, code, reason);
            router.onAppDisconnected(session.appName);
            broadcastAppList();

        }
    }

    @Override
    public void onMessage(WebSocket conn, String rawMessage) {
        AppSession session = sessions.get(conn);
        if (session == null) return; // race — connection closed

        try {
            JsonObject msg = JsonParser.parseString(rawMessage).getAsJsonObject();
            String type = msg.get("type").getAsString();

            // First message MUST be APP_CONNECTED registration
            if (!session.registered) {
                if ("APP_CONNECTED".equals(type)) {
                    handleRegistration(session, msg);
                } else {
                    log.warn("Received '{}' from unregistered client — ignoring", type);
                }
                return;
            }

            // Route all other message types
            router.route(session, msg, rawMessage, this);

        } catch (Exception e) {
            log.warn("Invalid message from {}: {} — {}", conn.getRemoteSocketAddress(), rawMessage, e.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        log.error("WebSocket error on {}: {}", conn != null ? conn.getRemoteSocketAddress() : "null", ex.getMessage());
    }

    @Override
    public void onStart() {
        log.info("JHubServer WebSocket listener started");
    }

    // ---------------------------------------------------------------
    // Registration
    // ---------------------------------------------------------------

    private void handleRegistration(AppSession session, JsonObject msg) {
        session.appName   = msg.has("appName")  ? msg.get("appName").getAsString()  : "unknown";
        session.version   = msg.has("version")  ? msg.get("version").getAsString()  : "";
        session.registered = true;

        log.info("App registered: {} v{}", session.appName, session.version);

        // Acknowledge registration
        JsonObject ack = new JsonObject();
        ack.addProperty("type", "JHUB_WELCOME");
        ack.addProperty("jHubVersion", "1.0.0");
        ack.addProperty("timestamp", Instant.now().toString());
        sendTo(session.socket, ack.toString());

        // Replay cached state so the new app is immediately current
        replayStateToNewApp(session);

        // Tell everyone about the new app list
        broadcastAppList();
    }

    // ---------------------------------------------------------------
    // State replay for late joiners
    // ---------------------------------------------------------------

    private void replayStateToNewApp(AppSession session) {
        // Last rig status
        RigStatus rig = cache.getLastRigStatus();
        if (rig != null) {
            sendTo(session.socket, ConfigManager.gson().toJson(rig));
        }

        // Logger session
        String loggerSession = cache.getLastLoggerSession();
        if (loggerSession != null) {
            sendTo(session.socket, loggerSession);
        }

        // Recent spots
        for (Spot spot : cache.getRecentSpots()) {
            sendTo(session.socket, ConfigManager.gson().toJson(spot));
        }

        // Last selected spot (so new joiners know what was clicked)
        String selectedSpot = cache.getLastSelectedSpot();
        if (selectedSpot != null) {
            sendTo(session.socket, selectedSpot);
        }

        log.debug("State replayed to '{}'", session.appName);
    }

    // ---------------------------------------------------------------
    // Broadcast helpers (public so router/cluster can use them)
    // ---------------------------------------------------------------

    /**
     * Broadcast a raw JSON string to every registered session.
     */
    public void broadcastToAll(String json) {
        for (AppSession s : sessions.values()) {
            if (s.registered && s.socket.isOpen()) {
                s.socket.send(json);
            }
        }
    }

    /**
     * Broadcast only to registered sessions whose appName matches.
     * Used to send internal messages (e.g. CLUSTER_RAW) only to the web UI.
     */
    public void broadcastToAppName(String appName, String json) {
        for (AppSession s : sessions.values()) {
            if (s.registered && s.socket.isOpen() && appName.equals(s.appName)) {
                s.socket.send(json);
            }
        }
    }

    /**
     * Broadcast to all registered sessions EXCEPT the one that sent the message.
     */
    public void broadcastExcept(String json, WebSocket excludeSocket) {
        for (AppSession s : sessions.values()) {
            if (s.registered && s.socket.isOpen() && s.socket != excludeSocket) {
                s.socket.send(json);
            }
        }
    }

    /**
     * Send a message to a single socket; swallows if not open.
     */
    public void sendTo(WebSocket socket, String json) {
        if (socket != null && socket.isOpen()) {
            socket.send(json);
        }
    }

    /**
     * Build and broadcast the current connected-app list.
     */
    public void broadcastAppList() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "APP_LIST");
        com.google.gson.JsonArray apps = new com.google.gson.JsonArray();
        for (AppSession s : sessions.values()) {
            if (s.registered) {
                JsonObject a = new JsonObject();
                a.addProperty("appName", s.appName);
                a.addProperty("version", s.version);
                a.addProperty("connectedAt", s.connectedAt.toString());
                apps.add(a);
            }
        }
        msg.add("apps", apps);
        broadcastToAll(msg.toString());
    }

    // ---------------------------------------------------------------
    // Accessors used by the status window
    // ---------------------------------------------------------------

    public Map<WebSocket, AppSession> getSessions() {
        return sessions;
    }

    public int getConnectedCount() {
        return (int) sessions.values().stream().filter(s -> s.registered).count();
    }
}
