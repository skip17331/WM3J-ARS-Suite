package com.jlog.cluster;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Maintains a separate j-hub WebSocket registration for the j-log UI window.
 * Connects when a log window opens (registering as "j-log") and disconnects
 * when the window closes. This is distinct from HubEngine ("logging-engine"),
 * allowing j-hub to show each independently.
 */
public class JLogUiPresence {

    private static final Logger log = LoggerFactory.getLogger(JLogUiPresence.class);
    private static final JLogUiPresence INSTANCE = new JLogUiPresence();

    public static JLogUiPresence getInstance() { return INSTANCE; }
    private JLogUiPresence() {}

    private WebSocketClient ws;

    public void connect(String hubUrl) {
        if (hubUrl == null || hubUrl.isBlank()) return;
        disconnect();
        try {
            ws = new WebSocketClient(new URI(hubUrl)) {
                @Override public void onOpen(ServerHandshake h) {
                    send("{\"type\":\"APP_CONNECTED\",\"appName\":\"j-log\",\"version\":\"1.0.0\"}");
                    log.info("j-log UI presence registered with hub");
                }
                @Override public void onMessage(String msg) {}
                @Override public void onClose(int code, String reason, boolean remote) {
                    log.debug("j-log UI presence disconnected");
                }
                @Override public void onError(Exception ex) {
                    log.warn("j-log UI presence error: {}", ex.getMessage());
                }
            };
            ws.connectBlocking(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("j-log UI presence connect failed: {}", e.getMessage());
        }
    }

    public void disconnect() {
        if (ws != null) {
            try { ws.closeBlocking(); } catch (Exception ignored) {}
            ws = null;
        }
    }
}
