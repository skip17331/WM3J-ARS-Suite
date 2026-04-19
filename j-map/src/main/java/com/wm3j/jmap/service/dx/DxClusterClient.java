package com.wm3j.jmap.service.dx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Receives enriched DX spots from ham-radio-hub via WebSocket.
 *
 * Replaces the old telnet DX cluster client. Hub handles cluster
 * connectivity; J-Map just consumes SPOT messages.
 *
 * Auto-discovers Hub via UDP broadcast on port 9999, falls back to
 * ws://localhost:8080. Reconnects automatically on disconnect.
 *
 * Public interface is unchanged so WorldMapCanvas needs no edits.
 */
public class DxClusterClient {

    private static final Logger log = LoggerFactory.getLogger(DxClusterClient.class);

    private static final int    DISCOVERY_PORT = 9999;
    private static final String DEFAULT_URL    = "ws://localhost:8080";
    private static final int    MAX_SPOTS      = 150;
    private static final int    MAX_LINES      = 300;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private volatile WebSocketClient ws;
    private volatile boolean connected = false;
    private volatile String statusMessage = "Disconnected";
    private volatile String hubUrl = DEFAULT_URL;
    private volatile Instant sessionStartTime = Instant.now();

    private final List<DxSpot>   clusterSpots = new CopyOnWriteArrayList<>();
    private final LinkedList<String> lineBuffer = new LinkedList<>();

    private final AtomicBoolean running = new AtomicBoolean(false);

    private volatile java.util.function.Consumer<DxSpot> spotSelectedListener;
    public void setSpotSelectedListener(java.util.function.Consumer<DxSpot> l) { this.spotSelectedListener = l; }

    private volatile java.util.function.Consumer<JsonNode> stationListener;
    public void setStationListener(java.util.function.Consumer<JsonNode> l) { this.stationListener = l; }

    /**
     * Override the hub host before calling start(). Disables UDP discovery
     * so j-map connects directly to this address regardless of beacons.
     * host may be a hostname or IP; wsPort defaults to 8080 if omitted.
     */
    public void setHubHost(String host, int wsPort) {
        this.hubUrl = "ws://" + host + ":" + wsPort;
        this.discoveryEnabled = false;
        log.info("Hub address pinned to {}", this.hubUrl);
    }

    private volatile boolean discoveryEnabled = true;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "j-map-hub-reconnect");
        t.setDaemon(true);
        return t;
    });

    // -------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------

    /** Start connecting to Hub and begin UDP discovery (unless hub host was pinned). */
    public void start() {
        if (!running.compareAndSet(false, true)) return;
        if (discoveryEnabled) startDiscovery();
        scheduleConnect(0);
    }

    public synchronized void disconnect() {
        running.set(false);
        connected = false;
        statusMessage = "Disconnected";
        scheduler.shutdownNow();
        if (ws != null) {
            try { ws.close(); } catch (Exception ignored) {}
            ws = null;
        }
        log.info("Hub spot client disconnected");
    }

    // -------------------------------------------------------
    // UDP discovery — updates hubUrl when a beacon is seen
    // -------------------------------------------------------

    private void startDiscovery() {
        Thread t = new Thread(() -> {
            try (DatagramSocket sock = new DatagramSocket(DISCOVERY_PORT)) {
                sock.setSoTimeout(2000);
                byte[] buf = new byte[512];
                while (running.get()) {
                    try {
                        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                        sock.receive(pkt);
                        String json = new String(pkt.getData(), 0, pkt.getLength());
                        JsonNode node = MAPPER.readTree(json);
                        if ("JHUB_BEACON".equals(node.path("type").asText())) {
                            int port = node.path("wsPort").asInt(8080);
                            String url = "ws://" + pkt.getAddress().getHostAddress() + ":" + port;
                            if (!url.equals(hubUrl)) {
                                hubUrl = url;
                                log.info("Hub discovered via beacon: {}", hubUrl);
                                if (!connected) scheduleConnect(0);
                            }
                        }
                    } catch (java.net.SocketTimeoutException ignored) {}
                }
            } catch (Exception e) {
                log.debug("Discovery listener: {}", e.getMessage());
            }
        }, "j-map-hub-discovery");
        t.setDaemon(true);
        t.start();
    }

    // -------------------------------------------------------
    // WebSocket connection
    // -------------------------------------------------------

    private void scheduleConnect(int delaySec) {
        if (!running.get()) return;
        scheduler.schedule(this::doConnect, delaySec, TimeUnit.SECONDS);
    }

    private void doConnect() {
        if (!running.get() || connected) return;
        String url = hubUrl;
        log.info("Connecting to Hub at {}", url);
        statusMessage = "Connecting to " + url + "…";
        addLine(">>> Connecting to Hub at " + url);

        try {
            ws = new WebSocketClient(new URI(url)) {
                @Override public void onOpen(ServerHandshake h) {
                    send("{\"type\":\"APP_CONNECTED\",\"appName\":\"j-map\",\"version\":\"1.0.0\"}");
                    connected = true;
                    sessionStartTime = Instant.now();
                    clusterSpots.clear();   // discard any spots from previous session
                    statusMessage = "Connected to " + url;
                    addLine(">>> Hub connected");
                    log.info("Hub connected: {}", url);
                }

                @Override public void onMessage(String msg) {
                    handleMessage(msg);
                }

                @Override public void onClose(int code, String reason, boolean remote) {
                    connected = false;
                    statusMessage = "Disconnected";
                    addLine(">>> Hub disconnected");
                    log.info("Hub disconnected ({})", reason);
                    scheduleConnect(10);
                }

                @Override public void onError(Exception ex) {
                    log.warn("Hub WS error: {}", ex.getMessage());
                }
            };
            ws.connectBlocking(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Hub connect failed: {} — retrying in 15s", e.getMessage());
            scheduleConnect(15);
        }
    }

    private void handleMessage(String json) {
        addLine(json);
        try {
            JsonNode node = MAPPER.readTree(json);
            String type = node.path("type").asText();

            if ("JHUB_WELCOME".equals(type)) {
                JsonNode st = node.path("station");
                if (!st.isMissingNode() && stationListener != null) {
                    stationListener.accept(st);
                }

            } else if ("SPOT".equals(type)) {
                // Parse the actual spot timestamp from j-hub; ignore replayed pre-session spots
                String tsStr = node.path("timestamp").asText("");
                Instant spotTime;
                try {
                    spotTime = tsStr.isEmpty() ? Instant.now() : Instant.parse(tsStr);
                } catch (Exception e) {
                    spotTime = Instant.now();
                }
                if (spotTime.isBefore(sessionStartTime)) return;

                double freqHz = node.path("frequency").asDouble(0);
                DxSpot spot = new DxSpot(
                    node.path("spotter").asText(""),
                    node.path("spotted").asText(""),
                    freqHz / 1000.0,
                    spotTime);
                spot.setComment(node.path("comment").asText(""));
                spot.setDxLat(node.path("lat").asDouble(0));
                spot.setDxLon(node.path("lon").asDouble(0));
                if (!node.path("mode").asText("").isEmpty())
                    spot.setServerMode(node.path("mode").asText());
                if (!node.path("localTimeAtSpot").asText("").isEmpty())
                    spot.setLocalTimeAtSpot(node.path("localTimeAtSpot").asText());
                spot.setDxccEntity(node.path("country").asText(""));
                clusterSpots.add(0, spot);
                while (clusterSpots.size() > MAX_SPOTS) clusterSpots.remove(clusterSpots.size() - 1);

            } else if ("SPOT_SELECTED".equals(type)) {
                double freqHz = node.path("frequency").asDouble(0);
                DxSpot spot = new DxSpot(
                    node.path("spotter").asText(""),
                    node.path("spotted").asText(""),
                    freqHz / 1000.0,
                    Instant.now());
                spot.setComment(node.path("comment").asText(""));
                spot.setDxLat(node.path("lat").asDouble(0));
                spot.setDxLon(node.path("lon").asDouble(0));
                if (!node.path("mode").asText("").isEmpty())
                    spot.setServerMode(node.path("mode").asText());
                if (spotSelectedListener != null)
                    spotSelectedListener.accept(spot);
            }

        } catch (Exception e) {
            log.debug("Message parse error: {}", e.getMessage());
        }
    }

    /** Send a SPOT_SELECTED message to the hub so other apps (e.g. HamLog) can react. */
    public void sendSpotSelected(DxSpot spot) {
        WebSocketClient wsCopy = ws;
        if (!connected || wsCopy == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",       "SPOT_SELECTED");
            msg.put("spotted",    spot.getDxCallsign());
            msg.put("spotter",    spot.getSpotter());
            msg.put("frequency",  (long)(spot.getFrequencyKhz() * 1000));
            msg.put("mode",       spot.getMode() != null ? spot.getMode() : "");
            msg.put("comment",    spot.getComment() != null ? spot.getComment() : "");
            msg.put("country",    spot.getDxccEntity() != null ? spot.getDxccEntity() : "");
            msg.put("timestamp",  Instant.now().toString());
            wsCopy.send(msg.toString());
            log.debug("Sent SPOT_SELECTED: {} @ {} kHz", spot.getDxCallsign(), spot.getFrequencyKhz());
        } catch (Exception e) {
            log.warn("sendSpotSelected failed: {}", e.getMessage());
        }
    }

    private synchronized void addLine(String line) {
        lineBuffer.addLast(line);
        while (lineBuffer.size() > MAX_LINES) lineBuffer.removeFirst();
    }

    // -------------------------------------------------------
    // Public accessors (unchanged interface)
    // -------------------------------------------------------

    public boolean isConnected() { return connected; }

    public String getStatusMessage() { return statusMessage; }

    public synchronized List<String> getRecentLines() {
        return new ArrayList<>(lineBuffer);
    }

    public List<DxSpot> getClusterSpots() {
        List<DxSpot> recent = new ArrayList<>();
        for (DxSpot s : clusterSpots) {
            if (s.ageMinutes() <= 60) recent.add(s);
        }
        return recent;
    }

    /** No-op: commands were for telnet; Hub manages the cluster. */
    public void sendCommand(String cmd) {
        log.debug("sendCommand ignored (Hub manages cluster): {}", cmd);
    }
}
