package com.hamradio.modem.hub;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;

public class HubClient extends WebSocketClient {
    private static final Logger log = LoggerFactory.getLogger(HubClient.class);
    private static final Gson GSON = new Gson();

    private final HubMessageListener listener;
    private final String appName;
    private final String version;
    private java.util.concurrent.ScheduledExecutorService heartbeatScheduler;

    public HubClient(URI serverUri, String appName, String version, HubMessageListener listener) {
        super(serverUri);
        this.appName = appName;
        this.version = version;
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        JsonObject registration = new JsonObject();
        registration.addProperty("type", "APP_CONNECTED");
        registration.addProperty("appName", appName);
        registration.addProperty("version", version);
        send(GSON.toJson(registration));
        startHeartbeat();
        listener.onConnected();
        log.info("Connected to hub {}", getURI());
    }

    private void startHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) return;
        heartbeatScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jdigi-heartbeat");
            t.setDaemon(true);
            return t;
        });
        final String hb = "{\"type\":\"HEARTBEAT\",\"appName\":\"" + appName + "\"}";
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (!isOpen()) return;
            try { send(hb); } catch (Exception ignored) {}
        }, 15, 15, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void onMessage(String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            listener.onMessage(json);
        } catch (Exception e) {
            listener.onError("Invalid hub message: " + e.getMessage());
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (heartbeatScheduler != null) { heartbeatScheduler.shutdownNow(); heartbeatScheduler = null; }
        listener.onDisconnected();
        log.info("Disconnected from hub: code={}, reason={}", code, reason);
    }

    @Override
    public void onError(Exception ex) {
        listener.onError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        log.error("Hub client error", ex);
    }

    /**
     * Send an arbitrary pre-built JSON message over the WebSocket. Used by
     * the local CW skimmer to publish {@code LOCAL_SKIMMER_ACTIVITY}
     * snapshots without requiring a typed wrapper for every message kind.
     */
    public void sendJson(JsonObject msg) {
        if (msg == null) return;
        if (isOpen()) {
            send(msg.toString());
        }
    }

    public void sendDecode(String mode,
                           String text,
                           long frequency,
                           double offsetHz,
                           double snr,
                           double confidence) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "MODEM_DECODE");
        msg.addProperty("mode", mode);
        msg.addProperty("text", text);
        msg.addProperty("frequency", frequency);
        msg.addProperty("offsetHz", offsetHz);
        msg.addProperty("snr", snr);
        msg.addProperty("confidence", confidence);
        msg.addProperty("timestamp", Instant.now().toString());
        if (isOpen()) {
            send(msg.toString());
        }
    }

    /**
     * Send a structured logging draft to the hub.
     *
     * This is intended for operator-approved or operator-selected content from j-digi
     * that j-log can use to populate its backend entry fields without needing the
     * full modem UI.
     */
    public void sendLogDraft(String callsign,
                             String mode,
                             String band,
                             long frequency,
                             String rstSent,
                             String rstReceived,
                             String exchange,
                             String notes,
                             double confidence) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type", "LOG_ENTRY_DRAFT");
        msg.addProperty("source", appName);
        msg.addProperty("timestamp", Instant.now().toString());

        msg.addProperty("callsign", safe(callsign));
        msg.addProperty("mode", safe(mode));
        msg.addProperty("band", safe(band));
        msg.addProperty("frequency", frequency);
        msg.addProperty("rstSent", safe(rstSent));
        msg.addProperty("rstReceived", safe(rstReceived));
        msg.addProperty("exchange", safe(exchange));
        msg.addProperty("notes", safe(notes));
        msg.addProperty("confidence", confidence);

        if (isOpen()) {
            send(msg.toString());
            log.info("Sent LOG_ENTRY_DRAFT call={} mode={} freq={}",
                    safe(callsign), safe(mode), frequency);
        } else {
            log.warn("Cannot send LOG_ENTRY_DRAFT because hub socket is not open");
        }
    }

    /**
     * Broadcast a contest QSO just saved to the shared DB by j-digi. Wire
     * format mirrors HubEngine.sendContestQsoSaved so j-log (and any other
     * peer stations) can refresh via their QSO_SAVED listener.
     */
    public void sendContestQsoSaved(com.jlog.model.QsoRecord q) {
        if (q == null) return;
        JsonObject qso = new JsonObject();
        qso.addProperty("contestId",      safe(q.getContestId()));
        qso.addProperty("callsign",       safe(q.getCallsign()));
        if (q.getDateTimeUtc() != null) qso.addProperty("datetimeUtc", q.getDateTimeUtc().toString());
        qso.addProperty("band",           safe(q.getBand()));
        qso.addProperty("mode",           safe(q.getMode()));
        qso.addProperty("frequency",      safe(q.getFrequency()));
        qso.addProperty("operator",       safe(q.getOperator()));
        qso.addProperty("serialSent",     safe(q.getSerialSent()));
        qso.addProperty("serialReceived", safe(q.getSerialReceived()));
        qso.addProperty("exchange",       safe(q.getExchange()));
        qso.addProperty("field1",         safe(q.getContestField1()));
        qso.addProperty("field2",         safe(q.getContestField2()));
        qso.addProperty("field3",         safe(q.getContestField3()));
        qso.addProperty("field4",         safe(q.getContestField4()));
        qso.addProperty("field5",         safe(q.getContestField5()));
        qso.addProperty("points",         q.getPoints());
        qso.addProperty("isDupe",         q.isDupe());
        qso.addProperty("rstSent",        safe(q.getRstSent()));
        qso.addProperty("rstReceived",    safe(q.getRstReceived()));
        qso.addProperty("notes",          safe(q.getNotes()));

        JsonObject msg = new JsonObject();
        msg.addProperty("type", "QSO_SAVED");
        msg.add("qso", qso);

        if (isOpen()) {
            send(msg.toString());
            log.info("Sent QSO_SAVED call={} contest={}", q.getCallsign(), q.getContestId());
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
