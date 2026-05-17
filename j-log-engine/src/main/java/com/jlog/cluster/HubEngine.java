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
    private final AtomicBoolean connected       = new AtomicBoolean(false);
    private final AtomicBoolean connecting      = new AtomicBoolean(false);
    private final AtomicBoolean autoReconnect   = new AtomicBoolean(false);
    private String url;
    private Thread reconnectThread;
    private java.util.concurrent.ScheduledExecutorService heartbeatScheduler;
    private static final int HEARTBEAT_INTERVAL_SEC = 15;

    // Listeners — called on the WebSocket thread; callers must dispatch to UI thread if needed
    private Consumer<DxSpot>   spotListener;
    private Consumer<DxSpot>   spotSelectedListener;
    private Consumer<String>   rawLineListener;
    private Consumer<JsonNode> logEntryDraftListener;
    private Consumer<JsonNode> callsignResultListener;
    private Consumer<Integer>  configUpdateListener;
    private Consumer<String>   densityListener;
    // Contest multi-op sync
    private Consumer<JsonNode> contestQsoSavedListener;
    private Consumer<JsonNode> contestQsoDeletedListener;
    private Consumer<JsonNode> contestRosterListener;
    private Consumer<JsonNode> contestFullSyncRequestListener;
    private Consumer<JsonNode> contestFullSyncStartListener;
    private Consumer<JsonNode> modemDecodeListener;
    private Consumer<JsonNode> solarFluxListener;
    /** Last-seen SOLAR_FLUX payload — replayed to a listener the moment it's
     *  registered, since the hub's state-replay can fire this before
     *  controllers have wired up. */
    private volatile JsonNode  lastSolarFluxPayload;
    private Consumer<JsonNode> heardBySpotListener;
    private Consumer<JsonNode> adifImportListener;
    private Consumer<String>   cwUnsupportedListener;
    // Latches false the first time j-hub broadcasts CW_UNSUPPORTED for the
    // current rig. MacroEngine reads this to choose between the Hamlib CW path
    // and the legacy CI-V fallback. Reset on (re)connect to re-probe support.
    private final AtomicBoolean hamlibCwAvailable = new AtomicBoolean(true);
    private Consumer<JsonNode> ampStatusListener;
    private Consumer<JsonNode> antStatusListener;
    private Runnable           onConnected;
    private Runnable           onDisconnected;
    private Runnable           onShutdown;

    // ---------------------------------------------------------------
    // Connection management
    // ---------------------------------------------------------------

    /**
     * Connect to the hub WebSocket server.
     * @param wsUrl  WebSocket URL, e.g. "ws://localhost:8080"
     * @return true if connection succeeded
     */
    public boolean connect(String wsUrl) {
        if (!connecting.compareAndSet(false, true)) {
            log.debug("Connection attempt ignored — already connecting to hub");
            return false;
        }
        try {
            if (connected.get()) disconnect();
            this.url = wsUrl;

            wsClient = new WebSocketClient(new URI(wsUrl)) {

                @Override
                public void onOpen(ServerHandshake handshake) {
                    connected.set(true);
                    // Re-probe Hamlib CW capability on each (re)connect — the rig
                    // configuration on the hub side may have changed.
                    hamlibCwAvailable.set(true);
                    // Register with hub — required before any other message
                    send("{\"type\":\"APP_CONNECTED\",\"appName\":\"logging-engine\",\"version\":\"1.0.0\"}");
                    log.info("Hub connected: {}", wsUrl);
                    startHeartbeat();
                    if (onConnected != null)
                        onConnected.run();
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
                        onDisconnected.run();
                    if (autoReconnect.get()) scheduleReconnect();
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
        } finally {
            connecting.set(false);
        }
    }

    /** Start the background heartbeat sender. Idempotent — no-op if already running. */
    private void startHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) return;
        heartbeatScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "hub-heartbeat");
            t.setDaemon(true);
            return t;
        });
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!connected.get() || wsClient == null) return;
            try { wsClient.send("{\"type\":\"HEARTBEAT\",\"appName\":\"logging-engine\"}"); }
            catch (Exception ignored) {}
        }, HEARTBEAT_INTERVAL_SEC, HEARTBEAT_INTERVAL_SEC, java.util.concurrent.TimeUnit.SECONDS);
    }

    public void disconnect() {
        if (heartbeatScheduler != null) { heartbeatScheduler.shutdownNow(); heartbeatScheduler = null; }
        if (wsClient != null) {
            try { wsClient.closeBlocking(); } catch (Exception ignored) {}
        }
        connected.set(false);
        if (onDisconnected != null)
            onDisconnected.run();
    }

    public boolean isConnected() { return connected.get(); }
    public String  getUrl()      { return url; }

    // ---------------------------------------------------------------
    // Message handling
    // ---------------------------------------------------------------

    private void handleMessage(String json) {
        // Always forward raw message to raw listener
        if (rawLineListener != null)
            rawLineListener.accept(json);

        try {
            JsonNode node = MAPPER.readTree(json);
            String type = node.path("type").asText("");

            if ("SPOT".equals(type)) {
                DxSpot spot = spotFromJson(node);
                if (spotListener != null)
                    spotListener.accept(spot);

            } else if ("SPOT_SELECTED".equals(type)) {
                DxSpot spot = spotFromJson(node);
                if (spotSelectedListener != null)
                    spotSelectedListener.accept(spot);

            } else if ("CALLSIGN_RESULT".equals(type)) {
                if (callsignResultListener != null)
                    callsignResultListener.accept(node);

            } else if ("LOG_ENTRY_DRAFT".equals(type)) {
                if (logEntryDraftListener != null)
                    logEntryDraftListener.accept(node);

            } else if ("CONFIG_UPDATE".equals(type)) {
                JsonNode settings = node.path("settings");
                int size = settings.path("fontSize").asInt(0);
                if (size > 0 && configUpdateListener != null)
                    configUpdateListener.accept(size);
                String dens = settings.path("density").asText("");
                if (!dens.isBlank() && densityListener != null)
                    densityListener.accept(dens);

            } else if ("SHUTDOWN".equals(type)) {
                log.info("SHUTDOWN command received from j-hub");
                if (onShutdown != null)
                    onShutdown.run();

            } else if ("QSO_SAVED".equals(type)) {
                if (contestQsoSavedListener != null)
                    contestQsoSavedListener.accept(node);

            } else if ("QSO_DELETED".equals(type)) {
                if (contestQsoDeletedListener != null)
                    contestQsoDeletedListener.accept(node);

            } else if ("CONTEST_ROSTER".equals(type)) {
                if (contestRosterListener != null)
                    contestRosterListener.accept(node);

            } else if ("REQUEST_FULL_SYNC".equals(type)) {
                if (contestFullSyncRequestListener != null)
                    contestFullSyncRequestListener.accept(node);

            } else if ("FULL_SYNC_START".equals(type)) {
                if (contestFullSyncStartListener != null)
                    contestFullSyncStartListener.accept(node);

            } else if ("MODEM_DECODE".equals(type)) {
                if (modemDecodeListener != null)
                    modemDecodeListener.accept(node);

            } else if ("SOLAR_FLUX".equals(type)) {
                lastSolarFluxPayload = node;
                if (solarFluxListener != null)
                    solarFluxListener.accept(node);

            } else if ("HEARD_BY_SPOT".equals(type)) {
                if (heardBySpotListener != null)
                    heardBySpotListener.accept(node);

            } else if ("IMPORT_ADIF".equals(type)) {
                if (adifImportListener != null)
                    adifImportListener.accept(node);

            } else if ("CW_UNSUPPORTED".equals(type)) {
                // Hub reports the configured rig rejected a CW command. Latch the
                // capability flag false so subsequent macros take the CI-V fallback
                // path; notify the listener once so the UI can surface it.
                boolean firstNotice = hamlibCwAvailable.getAndSet(false);
                if (firstNotice && cwUnsupportedListener != null) {
                    cwUnsupportedListener.accept(node.path("reason").asText(""));
                }

            } else if ("AMP_STATUS".equals(type)) {
                if (ampStatusListener != null)
                    ampStatusListener.accept(node);

            } else if ("ANT_STATUS".equals(type)) {
                if (antStatusListener != null)
                    antStatusListener.accept(node);
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

    public void setSpotListener             (Consumer<DxSpot> l)   { this.spotListener             = l; }
    public void setSpotSelectedListener     (Consumer<DxSpot> l)   { this.spotSelectedListener     = l; }
    public void setRawLineListener          (Consumer<String> l)   { this.rawLineListener          = l; }
    public void setLogEntryDraftListener    (Consumer<JsonNode> l) { this.logEntryDraftListener    = l; }
    public void setCallsignResultListener   (Consumer<JsonNode> l) { this.callsignResultListener   = l; }
    public void setContestQsoSavedListener  (Consumer<JsonNode> l) { this.contestQsoSavedListener  = l; }
    public void setContestQsoDeletedListener(Consumer<JsonNode> l) { this.contestQsoDeletedListener= l; }
    public void setContestRosterListener    (Consumer<JsonNode> l) { this.contestRosterListener    = l; }
    public void setContestFullSyncRequestListener(Consumer<JsonNode> l) { this.contestFullSyncRequestListener = l; }
    public void setContestFullSyncStartListener  (Consumer<JsonNode> l) { this.contestFullSyncStartListener   = l; }
    public void setModemDecodeListener           (Consumer<JsonNode> l) { this.modemDecodeListener            = l; }
    public void setSolarFluxListener             (Consumer<JsonNode> l) {
        this.solarFluxListener = l;
        // Replay last-seen payload immediately — covers the timing race where
        // j-hub's state-replay fires SOLAR_FLUX before the controller wires up.
        if (l != null && lastSolarFluxPayload != null) {
            try { l.accept(lastSolarFluxPayload); } catch (Exception ignored) {}
        }
    }
    public void setHeardBySpotListener           (Consumer<JsonNode> l) { this.heardBySpotListener            = l; }
    public void setAdifImportListener            (Consumer<JsonNode> l) { this.adifImportListener             = l; }
    public void setConfigUpdateListener     (Consumer<Integer> l)  { this.configUpdateListener     = l; }
    public void setDensityListener          (Consumer<String>  l)  { this.densityListener          = l; }
    /** Fires once (per (re)connect) when j-hub reports the rig doesn't support Hamlib CW. */
    public void setCwUnsupportedListener    (Consumer<String>  l)  { this.cwUnsupportedListener    = l; }

    /** True until j-hub broadcasts CW_UNSUPPORTED for the current rig. */
    public boolean isHamlibCwAvailable() { return hamlibCwAvailable.get(); }
    public void setAmpStatusListener        (Consumer<JsonNode> l) { this.ampStatusListener        = l; }
    public void setAntStatusListener        (Consumer<JsonNode> l) { this.antStatusListener        = l; }

    /** Ask j-hub to force the named switch to the given antenna until cleared. */
    public void sendAntOverride(String switchId, int antenna) {
        if (!connected.get() || wsClient == null || switchId == null || antenna <= 0) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "ANT_OVERRIDE");
            msg.put("switchId", switchId);
            msg.put("antenna",  antenna);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendAntOverride failed: {}", e.getMessage()); }
    }

    /** Clear the override on a single switch (or all switches when switchId is null/empty). */
    public void sendAntOverrideClear(String switchId) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "ANT_OVERRIDE_CLEAR");
            if (switchId != null && !switchId.isEmpty()) msg.put("switchId", switchId);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendAntOverrideClear failed: {}", e.getMessage()); }
    }
    public void setOnConnected              (Runnable r)           { this.onConnected              = r; }
    public void setOnDisconnected           (Runnable r)           { this.onDisconnected           = r; }
    public void setOnShutdown               (Runnable r)           { this.onShutdown               = r; }

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
            spotSelectedListener.accept(spot);
    }

    /** Send a CALLSIGN_LOOKUP request to hub; hub will broadcast CALLSIGN_RESULT to all apps. */
    public void sendCallsignLookup(String callsign) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",     "CALLSIGN_LOOKUP");
            msg.put("callsign", callsign.toUpperCase().trim());
            wsClient.send(msg.toString());
            log.debug("Sent CALLSIGN_LOOKUP: {}", callsign);
        } catch (Exception e) {
            log.warn("sendCallsignLookup failed: {}", e.getMessage());
        }
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

    // ---------------------------------------------------------------
    // Contest protocol — broadcast to all connected apps via hub
    // ---------------------------------------------------------------

    /** Broadcast CONTEST_ACTIVE when a contest is loaded in J-Log. */
    public void sendContestActive(com.jlog.plugin.ContestPlugin plugin) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",              "CONTEST_ACTIVE");
            msg.put("contestId",         plugin.getContestId());
            msg.put("contestName",       plugin.getContestName());
            msg.put("exchangeFormat",    plugin.getExchangeFormat() != null ? plugin.getExchangeFormat() : "");
            msg.put("multiplierDbColumn", plugin.computeMultiplierDbColumn());

            com.fasterxml.jackson.databind.node.ArrayNode fields = MAPPER.createArrayNode();
            if (plugin.getEntryFields() != null) {
                for (com.jlog.plugin.ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
                    com.fasterxml.jackson.databind.node.ObjectNode f = MAPPER.createObjectNode();
                    f.put("id",         fd.getId()    != null ? fd.getId()    : "");
                    f.put("label",      fd.getLabel() != null ? fd.getLabel() : "");
                    f.put("type",       fd.getType()  != null ? fd.getType()  : "text");
                    f.put("required",   fd.isRequired());
                    f.put("persistent", fd.isPersistent());
                    f.put("entryRow",   fd.getEntryRow());
                    f.put("width",      fd.getWidth());
                    if (fd.getOptions() != null) {
                        com.fasterxml.jackson.databind.node.ArrayNode opts = MAPPER.createArrayNode();
                        fd.getOptions().forEach(opts::add);
                        f.set("options", opts);
                    }
                    fields.add(f);
                }
            }
            msg.set("entryFields", fields);

            wsClient.send(msg.toString());
            log.info("Sent CONTEST_ACTIVE: {}", plugin.getContestId());
        } catch (Exception e) {
            log.warn("sendContestActive failed: {}", e.getMessage());
        }
    }

    /** Broadcast CONTEST_INACTIVE when the contest log is closed. */
    public void sendContestInactive() {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "CONTEST_INACTIVE");
            wsClient.send(msg.toString());
            log.info("Sent CONTEST_INACTIVE");
        } catch (Exception e) {
            log.warn("sendContestInactive failed: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    // Contest multi-op sync outbound
    // -----------------------------------------------------------------

    /** Announce that this station is joining a networked contest. The hub
     *  replies with CONTEST_ROSTER (relayed to every station) which the
     *  controller uses to determine its stride/offset. */
    public void sendJoinContest(String contestId, String stationName) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",        "JOIN_CONTEST");
            msg.put("contestId",   contestId);
            msg.put("stationName", stationName == null ? "" : stationName);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendJoinContest failed: {}", e.getMessage()); }
    }

    public void sendLeaveContest(String contestId, String stationName) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",        "LEAVE_CONTEST");
            msg.put("contestId",   contestId);
            msg.put("stationName", stationName == null ? "" : stationName);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendLeaveContest failed: {}", e.getMessage()); }
    }

    /** Broadcast a just-saved contest QSO for peer stations to apply. The
     *  receiving side uses ContestQsoDao.upsertByNaturalKey so repeats are
     *  idempotent (e.g. self-echo, late joiners). */
    public void sendContestQsoSaved(com.jlog.model.QsoRecord q) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "QSO_SAVED");
            msg.set ("qso",  qsoToJson(q));
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendContestQsoSaved failed: {}", e.getMessage()); }
    }

    /** Enable automatic reconnect with exponential backoff after every drop.
     *  Call once after the initial {@link #connect(String)}; the reconnect
     *  thread re-fires {@code onConnected} on success, so controllers can
     *  re-join contests / re-register listeners naturally. */
    public void enableAutoReconnect() {
        autoReconnect.set(true);
    }

    public void disableAutoReconnect() {
        autoReconnect.set(false);
        if (reconnectThread != null) reconnectThread.interrupt();
    }

    private void scheduleReconnect() {
        if (reconnectThread != null && reconnectThread.isAlive()) return;
        reconnectThread = new Thread(() -> {
            int backoffMs = 1000;
            while (autoReconnect.get() && !connected.get()) {
                try { Thread.sleep(backoffMs); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                if (!autoReconnect.get()) return;
                log.info("Hub: attempting reconnect to {}", url);
                try {
                    boolean ok = connect(url);
                    if (ok) { log.info("Hub: reconnect succeeded"); return; }
                } catch (Exception e) {
                    log.debug("Hub: reconnect attempt failed: {}", e.getMessage());
                }
                // Exponential backoff capped at 30s.
                backoffMs = Math.min(backoffMs * 2, 30_000);
            }
        }, "hub-reconnect");
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    /** Send text to be transmitted by the modem app (j-digi). Optionally
     *  switches the modem to a specific mode first (CW / RTTY / PSK31 / FT8 /
     *  …). The modem app echoes audio out its configured output device. */
    public void sendModemTx(String mode, String text) {
        if (!connected.get() || wsClient == null || text == null || text.isBlank()) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "MODEM_TX");
            if (mode != null && !mode.isBlank()) msg.put("mode", mode);
            msg.put("text", text);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendModemTx failed: {}", e.getMessage()); }
    }

    /** Ask j-hub to key the rig's CW keyer (Hamlib send_morse). The hub also
     *  sets the keyer speed (Hamlib KEYSPD) before dispatching the morse text.
     *  No-op when not connected or when the rig has previously rejected a CW
     *  command — callers should use {@link #isHamlibCwAvailable()} to choose
     *  between this path and the legacy CI-V fallback. */
    public void sendCw(String text, int wpm) {
        if (!connected.get() || wsClient == null || text == null || text.isBlank()) return;
        if (!hamlibCwAvailable.get()) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "SEND_CW");
            msg.put("text", text);
            msg.put("wpm",  wpm);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendCw failed: {}", e.getMessage()); }
    }

    /** Ask j-hub to abort any in-flight CW transmission (Hamlib stop_morse). */
    public void stopCw() {
        if (!connected.get() || wsClient == null) return;
        if (!hamlibCwAvailable.get()) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "STOP_CW");
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("stopCw failed: {}", e.getMessage()); }
    }

    /** Ask j-hub to key (on=true) or un-key (on=false) the rig via Hamlib.
     *  Used by the voice keyer to bracket WAV playback with PTT control.
     *  No-op if the hub isn't connected. The hub silently ignores the call
     *  when the rig backend is not Hamlib or PTT is disabled in config. */
    public void sendPtt(boolean on) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type", "SET_PTT");
            msg.put("on",   on);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendPtt failed: {}", e.getMessage()); }
    }

    /** Announce "I'm about to stream N QSOs to you" before a full-sync response.
     *  The requesting station uses {@code count} to drive a progress indicator. */
    public void sendFullSyncStart(String contestId, String toStation, int count) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",        "FULL_SYNC_START");
            msg.put("contestId",   contestId);
            msg.put("toStation",   toStation == null ? "" : toStation);
            msg.put("count",       count);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendFullSyncStart failed: {}", e.getMessage()); }
    }

    /** Ask peer stations for their full contest log — they respond by streaming
     *  each of their QSOs as QSO_SAVED. Idempotent upserts on the receive side
     *  handle the natural cross-station duplication. */
    public void sendRequestFullSync(String contestId, String stationName) {
        if (!connected.get() || wsClient == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",        "REQUEST_FULL_SYNC");
            msg.put("contestId",   contestId);
            msg.put("stationName", stationName == null ? "" : stationName);
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendRequestFullSync failed: {}", e.getMessage()); }
    }

    /** Broadcast a deleted contest QSO. Recipients call deleteByNaturalKey. */
    public void sendContestQsoDeleted(String contestId, String callsign,
                                       java.time.LocalDateTime dt) {
        if (!connected.get() || wsClient == null || dt == null) return;
        try {
            com.fasterxml.jackson.databind.node.ObjectNode msg = MAPPER.createObjectNode();
            msg.put("type",      "QSO_DELETED");
            msg.put("contestId", contestId);
            msg.put("callsign",  callsign);
            msg.put("datetimeUtc", dt.toString());
            wsClient.send(msg.toString());
        } catch (Exception e) { log.warn("sendContestQsoDeleted failed: {}", e.getMessage()); }
    }

    private static com.fasterxml.jackson.databind.node.ObjectNode qsoToJson(com.jlog.model.QsoRecord q) {
        com.fasterxml.jackson.databind.node.ObjectNode n = MAPPER.createObjectNode();
        n.put("contestId",      q.getContestId());
        n.put("callsign",       q.getCallsign());
        if (q.getDateTimeUtc() != null) n.put("datetimeUtc", q.getDateTimeUtc().toString());
        n.put("band",           q.getBand());
        n.put("mode",           q.getMode());
        n.put("frequency",      q.getFrequency());
        n.put("operator",       q.getOperator());
        n.put("serialSent",     q.getSerialSent());
        n.put("serialReceived", q.getSerialReceived());
        n.put("field1",         q.getContestField1());
        n.put("field2",         q.getContestField2());
        n.put("field3",         q.getContestField3());
        n.put("field4",         q.getContestField4());
        n.put("field5",         q.getContestField5());
        n.put("points",         q.getPoints());
        n.put("isDupe",         q.isDupe());
        n.put("rstSent",        q.getRstSent());
        n.put("rstReceived",    q.getRstReceived());
        n.put("notes",          q.getNotes());
        return n;
    }

    /** Reconstruct a QsoRecord from the JSON payload produced by {@link #qsoToJson}. */
    public static com.jlog.model.QsoRecord qsoFromJson(JsonNode n) {
        com.jlog.model.QsoRecord q = new com.jlog.model.QsoRecord();
        q.setContestId     (n.path("contestId").asText(null));
        q.setCallsign      (n.path("callsign").asText(null));
        String dt = n.path("datetimeUtc").asText(null);
        if (dt != null && !dt.isBlank()) {
            try { q.setDateTimeUtc(java.time.LocalDateTime.parse(dt)); }
            catch (Exception ignored) {}
        }
        q.setBand          (n.path("band").asText(null));
        q.setMode          (n.path("mode").asText(null));
        q.setFrequency     (n.path("frequency").asText(null));
        q.setOperator      (n.path("operator").asText(null));
        q.setSerialSent    (n.path("serialSent").asText(null));
        q.setSerialReceived(n.path("serialReceived").asText(null));
        q.setContestField1 (n.path("field1").asText(null));
        q.setContestField2 (n.path("field2").asText(null));
        q.setContestField3 (n.path("field3").asText(null));
        q.setContestField4 (n.path("field4").asText(null));
        q.setContestField5 (n.path("field5").asText(null));
        q.setPoints        (n.path("points").asInt(0));
        q.setDupe          (n.path("isDupe").asBoolean(false));
        q.setRstSent       (n.path("rstSent").asText(null));
        q.setRstReceived   (n.path("rstReceived").asText(null));
        q.setNotes         (n.path("notes").asText(null));
        return q;
    }
}
