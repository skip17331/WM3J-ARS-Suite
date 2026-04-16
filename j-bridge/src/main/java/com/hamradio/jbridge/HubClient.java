package com.hamradio.jbridge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * HubClient -- WebSocket client for j-hub.
 *
 * NOTE: This class deliberately avoids a field named "uri". WebSocketClient
 * (superclass of the anonymous inner class in doConnect) exposes getURI() which
 * returns java.net.URI, and the Java compiler resolves bare "uri" inside that
 * anonymous class to the superclass member rather than our String field.
 * We use "hubWsUrl" throughout to avoid this ambiguity.
 */
public class HubClient {

    private static final Logger log = LoggerFactory.getLogger(HubClient.class);

    private static final long RECONNECT_INITIAL_MS = 2_000;
    private static final long RECONNECT_MAX_MS     = 30_000;
    private static final int  DISCOVERY_PORT       = 9999;

    private volatile String  hubWsUrl      = null;
    private volatile boolean wantConnected = false;
    private volatile long    reconnectMs   = RECONNECT_INITIAL_MS;

    private WebSocketClient ws;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "hub-reconnect");
        t.setDaemon(true);
        return t;
    });

    private final ExecutorService discoveryExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "hub-discovery-listener");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> reconnectFuture;
    private DatagramSocket     discoverySocket;
    private volatile boolean   discoveryRunning = false;

    private final AtomicLong sent     = new AtomicLong();
    private final AtomicLong received = new AtomicLong();

    private BiConsumer<Boolean, String> onConnectionChange;
    private Consumer<JsonObject>        onMessage;

    public synchronized void connect(String wsUrl) {
        this.hubWsUrl      = wsUrl;
        this.wantConnected = true;
        this.reconnectMs   = RECONNECT_INITIAL_MS;
        startDiscovery();
        doConnect();
    }

    public synchronized void disconnect() {
        wantConnected = false;
        stopDiscovery();
        cancelReconnect();
        if (ws != null) { try { ws.close(); } catch (Exception ignored) {} }
        log.info("Hub client disconnected");
    }

    public void send(String json) {
        WebSocketClient current = ws;
        if (current != null && current.isOpen()) {
            try { current.send(json); sent.incrementAndGet(); }
            catch (Exception e) { log.warn("Hub send failed: {}", e.getMessage()); }
        }
    }

    public boolean isConnected()         { return ws != null && ws.isOpen(); }
    public long    getMessagesSent()     { return sent.get(); }
    public long    getMessagesReceived() { return received.get(); }

    public void setOnConnectionChange(BiConsumer<Boolean, String> cb) { this.onConnectionChange = cb; }
    public void setOnMessage(Consumer<JsonObject> cb)                 { this.onMessage = cb; }

    private void doConnect() {
        if (!wantConnected || hubWsUrl == null) return;
        cancelReconnect();

        // Capture URL as a local final before the anonymous class.
        // Do NOT reference hubWsUrl inside the anonymous WebSocketClient --
        // any field named "uri" would resolve to WebSocketClient.getURI() (java.net.URI).
        final String connectingTo = hubWsUrl;

        try {
            ws = new WebSocketClient(new URI(connectingTo)) {

                @Override
                public void onOpen(ServerHandshake hs) {
                    log.info("Hub connected: {}", connectingTo);
                    reconnectMs = RECONNECT_INITIAL_MS;
                    JsonObject reg = new JsonObject();
                    reg.addProperty("type",    "APP_CONNECTED");
                    reg.addProperty("appName", "jBridge");
                    reg.addProperty("version", "1.0.0");
                    send(reg.toString());
                    if (onConnectionChange != null) onConnectionChange.accept(true, connectingTo);
                }

                @Override
                public void onMessage(String text) {
                    received.incrementAndGet();
                    try {
                        JsonObject parsed = JsonParser.parseString(text).getAsJsonObject();
                        // Handle SHUTDOWN from J-Hub — exit cleanly
                        if (parsed.has("type") &&
                                "SHUTDOWN".equals(parsed.get("type").getAsString())) {
                            log.info("SHUTDOWN command received from j-hub — exiting");
                            javafx.application.Platform.runLater(
                                    javafx.application.Platform::exit);
                            return;
                        }
                        if (onMessage != null) onMessage.accept(parsed);
                    } catch (Exception e) {
                        log.warn("Hub message parse error: {}", text);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.info("Hub disconnected (code={}, reason={})", code, reason);
                    if (onConnectionChange != null) onConnectionChange.accept(false, reason);
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception e) {
                    log.warn("Hub WebSocket error: {}", e.getMessage());
                }
            };
            ws.connect();

        } catch (Exception e) {
            log.warn("Hub connection failed ({}): {} -- retrying in {} ms",
                     connectingTo, e.getMessage(), reconnectMs);
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!wantConnected) return;
        long delay = reconnectMs;
        reconnectMs = Math.min(reconnectMs * 2, RECONNECT_MAX_MS);
        log.info("Hub reconnecting in {} ms", delay);
        reconnectFuture = scheduler.schedule(this::doConnect, delay, TimeUnit.MILLISECONDS);
    }

    private void cancelReconnect() {
        if (reconnectFuture != null && !reconnectFuture.isDone()) reconnectFuture.cancel(false);
    }

    private void startDiscovery() {
        if (discoveryRunning) return;
        discoveryRunning = true;
        discoveryExecutor.submit(this::discoveryLoop);
        log.info("Hub discovery listener starting on UDP {}", DISCOVERY_PORT);
    }

    private void stopDiscovery() {
        discoveryRunning = false;
        if (discoverySocket != null && !discoverySocket.isClosed()) discoverySocket.close();
    }

    private void discoveryLoop() {
        try {
            discoverySocket = new DatagramSocket(null);
            discoverySocket.setReuseAddress(true);
            discoverySocket.bind(new InetSocketAddress(DISCOVERY_PORT));
            discoverySocket.setSoTimeout(2000);
            byte[] buf = new byte[512];
            log.info("Hub discovery listening on UDP {}", DISCOVERY_PORT);

            while (discoveryRunning) {
                try {
                    DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                    discoverySocket.receive(pkt);
                    handleBeacon(new String(pkt.getData(), 0, pkt.getLength()),
                                 pkt.getAddress().getHostAddress());
                } catch (java.net.SocketTimeoutException ignored) {}
            }

        } catch (java.net.BindException e) {
            log.info("UDP {} bind failed -- hub discovery disabled, using configured URL: {}",
                     DISCOVERY_PORT, hubWsUrl);
        } catch (Exception e) {
            if (discoveryRunning) {
                log.warn("Discovery listener error: {} -- retrying in 10 s", e.getMessage());
                sleep(10_000);
                if (discoveryRunning) discoveryExecutor.submit(this::discoveryLoop);
            }
        }
    }

    private void handleBeacon(String payload, String senderIp) {
        try {
            JsonObject node = JsonParser.parseString(payload).getAsJsonObject();
            if (!node.has("type") || !"HUB_BEACON".equals(node.get("type").getAsString())) return;
            int wsPort = node.has("wsPort") ? node.get("wsPort").getAsInt() : 8080;
            String wsUrl = "ws://" + senderIp + ":" + wsPort;
            log.debug("Hub beacon from {} wsPort={}", senderIp, wsPort);
            if (!isConnected()) {
                synchronized (this) { hubWsUrl = wsUrl; }
                doConnect();
            }
        } catch (Exception e) {
            log.trace("Beacon parse failed: {}", payload);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
