package com.jlog.cluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.DxSpot;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Hub engine — connects to the ham-radio-hub WebSocket server (ws://localhost:8080)
 * and receives enriched DX spots as JSON messages.
 *
 * Replaces the old Telnet-based DxClusterEngine. Hub handles cluster connectivity
 * and DXCC enrichment; j-log just consumes the SPOT messages.
 *
 * Extended to also receive LOG_ENTRY_DRAFT messages from j-digi via j-hub.
 */
public class HubEngine {

    private static final Logger log = LoggerFactory.getLogger(HubEngine.class);
    private static final HubEngine INSTANCE = new HubEngine();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static HubEngine getInstance() { return INSTANCE; }
    private HubEngine() {}

    // ---------------------------------------------------------------
    // State
    // ---------------------------------------------------------------

    private WebSocketClient wsClient;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private String url;

    // Listeners (same contract as the old DxClusterEngine)
    private Consumer<DxSpot>   spotListener;
    private Consumer<DxSpot>   spotSelectedListener;
    private Consumer<String>   rawLineListener;
    private Consumer<JsonNode> logEntryDraftListener;
    private Runnable           onConnected;
    private Runnable           onDisconnected;

    // ---------------------------------------------------------------
    // Connection management
    // ---------------------------------------------------------------

    /**
     * Connect to the hub WebSocket server.
     * @param wsUrl  WebSocket URL, e.g. "ws://localhost:8080"
     * @return true if connection succeeded
     */
    public boolean connect(String wsUrl) {
        if (connected.get()) disconnect();
        this.url = wsUrl;

        try {
            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected.set(true);
                    // Register with hub — required before any other message
                    send("{\"type\":\"APP_CONNECTED\",\"appName\":\"j-log\",\"version\":\"1.0.0\"}");
                    log.info("Hub connected: {}", wsUrl);
                    if (onConnected != null)
                        javafx.application.Platform.runLater(onConnected);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    boolean wasConnected = connected.getAndSet(false);
                    log.info("Hub disconnected (code={}, reason={})", code, reason);
                    if (wasConnected && onDisconnected != null)
                        javafx.application.Platform.runLater(onDisconnected);
                }

                @Override
                public void onError(Exception ex) {
                    log.error("Hub WebSocket error", ex);
                }
            };

            wsClient.connectBlocking();
            return connected.get();

        } catch (Exception ex) {
            log.error("Hub connect failed: {}", wsUrl, ex);
            return false;
        }
    }

    public void disconnect() {
        if (wsClient != null) {
            try { wsClient.closeBlocking(); } catch (Exception ignored) {}
        }
        connected.set(false);
        if (onDisconnected != null)
            javafx.application.Platform.runLater(onDisconnected);
    }

    public boolean isConnected() { return connected.get(); }
    public String  getUrl()      { return url; }

    // ---------------------------------------------------------------
    // Message handling
    // ---------------------------------------------------------------

    private void handleMessage(String json) {
        // Always forward raw message to raw listener
        if (rawLineListener != null)
            javafx.application.Platform.runLater(() -> rawLineListener.accept(json));

        try {
            JsonNode node = MAPPER.readTree(json);
            String type = node.path("type").asText("");

            if ("SPOT".equals(type)) {
                DxSpot spot = spotFromJson(node);
                if (spotListener != null)
                    javafx.application.Platform.runLater(() -> spotListener.accept(spot));

            } else if ("SPOT_SELECTED".equals(type)) {
                DxSpot spot = spotFromJson(node);
                if (spotSelectedListener != null)
                    javafx.application.Platform.runLater(() -> spotSelectedListener.accept(spot));

            } else if ("LOG_ENTRY_DRAFT".equals(type)) {
                if (logEntryDraftListener != null)
                    javafx.application.Platform.runLater(() -> logEntryDraftListener.accept(node));

            } else if ("SHUTDOWN".equals(type)) {
                // J-Hub is shutting down — exit cleanly
                log.info("SHUTDOWN command received from j-hub — closing j-log");
                javafx.application.Platform.runLater(() -> javafx.application.Platform.exit());
            }
            // HUB_WELCOME, APP_LIST, RIG_STATUS etc. appear in raw tab — no special handling needed

        } catch (Exception ex) {
            log.warn("Hub message parse error: {}", json, ex);
        }
    }

    private DxSpot spotFromJson(JsonNode node) {
        DxSpot spot = new DxSpot();

        spot.setSpotter   (node.path("spotter").asText(""));
        spot.setDxCallsign(node.path("spotted").asText(""));

        // Hub sends frequency in Hz — convert to kHz for DxSpot
        double freqHz = node.path("frequency").asDouble(0);
        spot.setFrequencyKHz(freqHz / 1000.0);

        spot.setComment     (node.path("comment").asText(""));
        spot.setMode        (node.path("mode").asText(""));
        spot.setCountry     (node.path("country").asText(""));
        spot.setContinent   (node.path("continent").asText(""));
        spot.setBearing     (node.path("bearing").asDouble(0));
        spot.setDistanceKm  (node.path("distanceKm").asDouble(0));
        spot.setWorkedStatus(node.path("workedStatus").asText("unknown"));
        spot.setRawLine     (node.toString());

        // Parse ISO-8601 UTC timestamp
        String ts = node.path("timestamp").asText("");
        if (!ts.isBlank()) {
            try {
                spot.setTime(LocalDateTime.ofInstant(Instant.parse(ts), ZoneId.systemDefault()));
            } catch (Exception ex) {
                spot.setTime(LocalDateTime.now());
            }
        } else {
            spot.setTime(LocalDateTime.now());
        }

        return spot;
    }

    // ---------------------------------------------------------------
    // Listener setters
    // ---------------------------------------------------------------

    public void setSpotListener         (Consumer<DxSpot> l)   { this.spotListener          = l; }
    public void setSpotSelectedListener (Consumer<DxSpot> l)   { this.spotSelectedListener  = l; }
    public void setRawLineListener      (Consumer<String> l)   { this.rawLineListener       = l; }
    public void setLogEntryDraftListener(Consumer<JsonNode> l) { this.logEntryDraftListener = l; }
    public void setOnConnected          (Runnable r)           { this.onConnected           = r; }
    public void setOnDisconnected       (Runnable r)           { this.onDisconnected        = r; }

    // ---------------------------------------------------------------
    // Spot selection — publish to hub so other apps (e.g. HamClock) react
    // ---------------------------------------------------------------

    /**
     * Fire the spotSelectedListener directly without a hub round-trip.
     * Call this on local spot selection so the entry bar populates immediately,
     * then call sendSpotSelected separately to broadcast to other apps via hub.
     */
    public void notifySpotSelected(DxSpot spot) {
        if (spotSelectedListener != null)
            javafx.application.Platform.runLater(() -> spotSelectedListener.accept(spot));
    }

    public void sendSpotSelected(DxSpot spot) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",       "SPOT_SELECTED");
            msg.put("spotted",    spot.getDxCallsign());
            msg.put("spotter",    spot.getSpotter());
            msg.put("frequency",  (long)(spot.getFrequencyKHz() * 1000));
            msg.put("mode",       spot.getMode()    != null ? spot.getMode()    : "");
            msg.put("comment",    spot.getComment() != null ? spot.getComment() : "");
            msg.put("country",    spot.getCountry() != null ? spot.getCountry() : "");
            msg.put("bearing",    spot.getBearing());
            msg.put("distanceKm", spot.getDistanceKm());
            msg.put("timestamp",  Instant.now().toString());
            wsClient.send(msg.toString());
            log.debug("Sent SPOT_SELECTED: {} @ {} kHz", spot.getDxCallsign(), spot.getFrequencyKHz());
        } catch (Exception e) {
            log.warn("sendSpotSelected failed: {}", e.getMessage());
        }
    }
}
