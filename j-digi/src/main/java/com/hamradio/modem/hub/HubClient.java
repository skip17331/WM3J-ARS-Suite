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
        listener.onConnected();
        log.info("Connected to hub {}", getURI());
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
        listener.onDisconnected();
        log.info("Disconnected from hub: code={}, reason={}", code, reason);
    }

    @Override
    public void onError(Exception ex) {
        listener.onError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
        log.error("Hub client error", ex);
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

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
