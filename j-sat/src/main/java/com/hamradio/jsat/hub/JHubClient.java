package com.hamradio.jsat.hub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hamradio.jsat.model.SatelliteState;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * WebSocket client connecting J-Sat to J-Hub.
 *
 * On connect: sends APP_CONNECTED, receives JHUB_WELCOME + state replay.
 * Publishes SAT_STATE updates to all connected apps via hub.
 * Receives RIG_STATUS, SPOT_SELECTED for context awareness.
 */
public class JHubClient {

    private static final Logger log = LoggerFactory.getLogger(JHubClient.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String hubHost;
    private final int    hubPort;
    private volatile WebSocketClient ws;
    private volatile boolean discoveryEnabled;
    private volatile boolean intentionalClose;

    private Consumer<JsonNode> onRigStatus;
    private Consumer<JsonNode> onRotorStatus;
    private Consumer<JsonNode> onSpotSelected;
    private Consumer<JsonNode> onStationUpdate;

    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "jsat-hub-reconnect");
        t.setDaemon(true);
        return t;
    });

    private ScheduledFuture<?> discoveryFuture;

    public JHubClient(String hubHost, int hubPort) {
        this.hubHost          = hubHost;
        this.hubPort          = hubPort;
        this.discoveryEnabled = "localhost".equals(hubHost) || "127.0.0.1".equals(hubHost);
    }

    public void start() {
        if (discoveryEnabled) {
            startUdpDiscovery();
        } else {
            connectDirect();
        }
    }

    public void disconnect() {
        intentionalClose = true;
        if (discoveryFuture != null) discoveryFuture.cancel(true);
        reconnectScheduler.shutdownNow();
        if (ws != null) ws.close();
    }

    /** Publish current satellite tracking state to J-Hub. */
    public void publishSatState(SatelliteState state) {
        if (ws == null || !ws.isOpen()) return;
        try {
            ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",         "SAT_STATE");
            msg.put("satName",      state.name);
            msg.put("noradId",      state.noradId);
            msg.put("lat",          state.latDeg);
            msg.put("lon",          state.lonDeg);
            msg.put("altKm",        state.altKm);
            msg.put("azDeg",        state.azimuthDeg);
            msg.put("elDeg",        state.elevationDeg);
            msg.put("rangeKm",      state.slantRangeKm);
            msg.put("rangeRateKms", state.rangeRateKmSec);
            msg.put("downlinkHz",   state.correctedDownlinkHz);
            msg.put("uplinkHz",     state.correctedUplinkHz);
            msg.put("inSunlight",   state.inSunlight);
            ws.send(msg.toString());
        } catch (Exception e) {
            log.debug("SAT_STATE publish failed: {}", e.getMessage());
        }
    }

    /** Send Doppler-corrected frequencies to J-Hub for rig tuning. */
    public void publishDopplerFreqs(long downlinkHz, long uplinkHz, String mode) {
        if (ws == null || !ws.isOpen()) return;
        try {
            ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",        "SAT_DOPPLER");
            msg.put("downlinkHz",  downlinkHz);
            msg.put("uplinkHz",    uplinkHz);
            if (mode != null && !mode.isEmpty()) msg.put("mode", mode);
            ws.send(msg.toString());
        } catch (Exception e) {
            log.debug("SAT_DOPPLER publish failed: {}", e.getMessage());
        }
    }

    /** Send AZ/EL rotor command to J-Hub. */
    public void publishRotorCmd(double azDeg, double elDeg) {
        if (ws == null || !ws.isOpen()) return;
        try {
            ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",  "SAT_ROTOR_CMD");
            msg.put("azDeg", azDeg);
            msg.put("elDeg", elDeg);
            ws.send(msg.toString());
        } catch (Exception e) {
            log.debug("SAT_ROTOR_CMD publish failed: {}", e.getMessage());
        }
    }

    public boolean isConnected() { return ws != null && ws.isOpen(); }

    public void setOnRigStatus(Consumer<JsonNode> cb)     { this.onRigStatus     = cb; }
    public void setOnRotorStatus(Consumer<JsonNode> cb)   { this.onRotorStatus   = cb; }
    public void setOnSpotSelected(Consumer<JsonNode> cb)  { this.onSpotSelected  = cb; }
    public void setOnStationUpdate(Consumer<JsonNode> cb) { this.onStationUpdate = cb; }

    // ── Private connection logic ───────────────────────────────────────────────

    private void connectDirect() {
        connectTo("ws://" + hubHost + ":" + hubPort);
    }

    private void connectTo(String url) {
        try {
            ws = buildClient(URI.create(url));
            ws.connect();
        } catch (Exception e) {
            log.warn("Hub connect failed to {}: {}", url, e.getMessage());
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!intentionalClose) {
            reconnectScheduler.schedule(this::connectDirect, 10, TimeUnit.SECONDS);
        }
    }

    private void startUdpDiscovery() {
        discoveryFuture = reconnectScheduler.scheduleAtFixedRate(() -> {
            if (ws != null && ws.isOpen()) return;
            try {
                java.net.DatagramSocket sock = new java.net.DatagramSocket();
                sock.setSoTimeout(3000);
                byte[] buf = new byte[512];
                java.net.DatagramPacket pkt = new java.net.DatagramPacket(buf, buf.length);
                sock.receive(pkt);
                String msg = new String(pkt.getData(), 0, pkt.getLength()).trim();
                JsonNode node = MAPPER.readTree(msg);
                if ("JHUB_BEACON".equals(node.path("type").asText())) {
                    int wsPort = node.path("wsPort").asInt(hubPort);
                    String host = pkt.getAddress().getHostAddress();
                    connectTo("ws://" + host + ":" + wsPort);
                }
                sock.close();
            } catch (Exception e) {
                log.trace("Discovery: {}", e.getMessage());
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private WebSocketClient buildClient(URI uri) {
        return new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake h) {
                log.info("Connected to J-Hub at {}", uri);
                try {
                    ObjectNode reg = MAPPER.createObjectNode();
                    reg.put("type",    "APP_CONNECTED");
                    reg.put("appName", "j-sat");
                    reg.put("version", "1.0.0");
                    send(reg.toString());
                } catch (Exception e) {
                    log.warn("APP_CONNECTED send failed", e);
                }
            }

            @Override
            public void onMessage(String message) {
                try {
                    JsonNode msg = MAPPER.readTree(message);
                    String type = msg.path("type").asText("");
                    switch (type) {
                        case "RIG_STATUS"    -> { if (onRigStatus     != null) onRigStatus.accept(msg); }
                        case "ROTOR_STATUS"  -> { if (onRotorStatus   != null) onRotorStatus.accept(msg); }
                        case "SPOT_SELECTED" -> { if (onSpotSelected  != null) onSpotSelected.accept(msg); }
                        case "JHUB_WELCOME"  -> {
                            log.info("J-Hub welcomed J-Sat");
                            JsonNode st = msg.path("station");
                            if (!st.isMissingNode() && onStationUpdate != null)
                                onStationUpdate.accept(st);
                        }
                        default -> {}
                    }
                } catch (Exception e) {
                    log.debug("Hub message parse error: {}", e.getMessage());
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("J-Hub connection closed (code={} reason={})", code, reason);
                if (!intentionalClose) scheduleReconnect();
            }

            @Override
            public void onError(Exception e) {
                log.debug("Hub WebSocket error: {}", e.getMessage());
            }
        };
    }
}
