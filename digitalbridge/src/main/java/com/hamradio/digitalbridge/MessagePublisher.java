package com.hamradio.digitalbridge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hamradio.digitalbridge.model.WsjtxDecode;
import com.hamradio.digitalbridge.model.WsjtxQsoLogged;
import com.hamradio.digitalbridge.model.WsjtxStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * MessagePublisher — formats domain objects as j-hub wire-protocol JSON
 * and delivers them via HubClient.
 *
 * JSON library: Gson (matches j-hub ConfigManager.gson() / HubServer).
 * Frequency convention: Hz throughout (matches j-hub Spot.frequency and RigStatus.frequency).
 *
 * Message types published:
 *   APP_CONNECTED     — on hub connect (registration)
 *   WSJTX_DECODE      — routed through MessageRouter → broadcast to all apps
 *   WSJTX_STATUS      — rig frequency/mode for HamClock / j-log
 *   WSJTX_QSO_LOGGED  — routed to j-log's logEntryDraftListener
 *   WSJTX_CONNECTION  — Digital Bridge ↔ WSJT-X link state
 */
public class MessagePublisher {

    private static final Logger log = LoggerFactory.getLogger(MessagePublisher.class);

    private static final DateTimeFormatter ISO_UTC =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    private static final Gson GSON = new Gson(); // compact, matches j-hub wire format

    private final HubClient hub;

    public MessagePublisher(HubClient hub) { this.hub = hub; }

    // ── Registration ───────────────────────────────────────────────────────────

    /**
     * Send APP_CONNECTED immediately after WebSocket opens.
     * Called automatically by HubClient.onOpen — exposed here for testing.
     */
    public void sendAppConnected() {
        JsonObject msg = new JsonObject();
        msg.addProperty("type",    "APP_CONNECTED");
        msg.addProperty("appName", "digitalBridge");
        msg.addProperty("version", "1.0.0");
        send(msg);
    }

    // ── WSJT-X event publishers ────────────────────────────────────────────────

    /**
     * Publish an enriched WSJTX_DECODE to j-hub.
     * j-hub MessageRouter will rebroadcast to all connected apps.
     */
    public void publishDecode(WsjtxDecode d) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type",           "WSJTX_DECODE");
        msg.addProperty("callsign",       safe(d.getCallsign()));
        msg.addProperty("frequency",      d.getFrequency());       // Hz — j-hub convention
        msg.addProperty("band",           safe(d.getBand()));
        msg.addProperty("mode",           safe(d.getMode()));
        msg.addProperty("snr",            d.getSnr());
        msg.addProperty("deltaTime",      round2(d.getDeltaTime()));
        msg.addProperty("message",        safe(d.getMessage()));
        msg.addProperty("timestamp",      isoNow(d.getTimestamp()));
        msg.addProperty("lat",            d.getLat());
        msg.addProperty("lon",            d.getLon());
        msg.addProperty("country",        safe(d.getCountry()));
        msg.addProperty("continent",      safe(d.getContinent()));
        msg.addProperty("dxcc",           d.getDxcc());
        msg.addProperty("bearing",        round1(d.getBearing()));
        msg.addProperty("distanceKm",     round1(d.getDistanceKm()));
        msg.addProperty("distanceMi",     round1(d.getDistanceMi()));
        msg.addProperty("localTimeAtSpot",safe(d.getLocalTimeAtSpot()));
        msg.addProperty("workedStatus",   safe(d.getWorkedStatus()));
        send(msg);
    }

    /**
     * Publish WSJTX_STATUS.
     * j-hub MessageRouter will update the StateCache RigStatus and rebroadcast —
     * this allows HamClock and j-log to show current frequency/mode from WSJT-X.
     */
    public void publishStatus(WsjtxStatus s) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type",        "WSJTX_STATUS");
        msg.addProperty("frequency",   s.getDialFrequency());      // Hz
        msg.addProperty("mode",        safe(s.getMode()));
        msg.addProperty("band",        safe(s.getBand()));
        msg.addProperty("transmitting",s.isTransmitting());
        msg.addProperty("decoding",    s.isDecoding());
        msg.addProperty("dxCall",      safe(s.getDxCall()));
        msg.addProperty("report",      safe(s.getReport()));
        msg.addProperty("txEnabled",   s.isTxEnabled());
        send(msg);
    }

    /**
     * Publish WSJTX_QSO_LOGGED.
     * j-hub MessageRouter rebroadcasts to all — j-log receives it via
     * logEntryDraftListener and can pre-populate its log entry fields.
     */
    public void publishQsoLogged(WsjtxQsoLogged q) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type",        "WSJTX_QSO_LOGGED");
        msg.addProperty("callsign",    safe(q.getDxCall()));
        msg.addProperty("frequency",   q.getDialFrequency());      // Hz
        msg.addProperty("mode",        safe(q.getMode()));
        msg.addProperty("band",        safe(q.getBand()));
        msg.addProperty("rstSent",     safe(q.getRstSent()));
        msg.addProperty("rstReceived", safe(q.getRstReceived()));
        msg.addProperty("timestamp",   isoNow(q.getTimestamp()));
        msg.addProperty("country",     safe(q.getCountry()));
        msg.addProperty("continent",   safe(q.getContinent()));
        msg.addProperty("dxcc",        q.getDxcc());
        msg.addProperty("name",        safe(q.getName()));
        msg.addProperty("comment",     safe(q.getComments()));
        send(msg);
    }

    /**
     * Publish WSJTX_CONNECTION state change to j-hub.
     */
    public void publishConnectionStatus(boolean connected, String version) {
        JsonObject msg = new JsonObject();
        msg.addProperty("type",      "WSJTX_CONNECTION");
        msg.addProperty("connected", connected);
        msg.addProperty("version",   version != null ? version : "");
        msg.addProperty("timestamp", isoNow(Instant.now()));
        send(msg);
    }

    // ── Delivery ───────────────────────────────────────────────────────────────

    private void send(JsonObject msg) {
        if (hub != null && hub.isConnected()) {
            String json = GSON.toJson(msg);
            log.debug("→ HUB: {}", json);
            hub.send(json);
        }
    }

    // ── Utilities ──────────────────────────────────────────────────────────────

    private String isoNow(Instant instant) {
        return ISO_UTC.format(instant != null ? instant : Instant.now());
    }

    private String safe(String s)    { return s != null ? s : ""; }
    private double round1(double v)  { return Math.round(v * 10.0)   / 10.0; }
    private double round2(double v)  { return Math.round(v * 100.0)  / 100.0; }
}
