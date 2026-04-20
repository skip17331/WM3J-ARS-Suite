package com.hamradio.jhub;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hamradio.jhub.model.RigStatus;
import com.hamradio.jhub.model.Spot;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * MessageRouter — central dispatcher for all WebSocket messages.
 *
 * Each message type is handled here:
 *   RIG_STATUS      — cache latest rig state, rebroadcast to all
 *   LOGGER_SESSION  — cache session info, rebroadcast
 *   SPOT_SELECTED   — rebroadcast to all
 *   WSJTX_DECODE    — rebroadcast to all
 *   MODEM_DECODE    — rebroadcast to all
 *   LOG_ENTRY_DRAFT — rebroadcast to all
 *   SPOT            — cache in recent-spot ring buffer, rebroadcast
 *   Unknown types   — log and ignore
 *
 * The router is a singleton wired by JHubMain.
 */
public class MessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MessageRouter.class);

    // Singleton
    private static final MessageRouter INSTANCE = new MessageRouter();
    public static MessageRouter getInstance() { return INSTANCE; }
    private MessageRouter() {}

    // Late-bound reference to the WS server (set by JHubMain after server starts)
    private JHubServer jHubServer;

    public void setJHubServer(JHubServer server) {
        this.jHubServer = server;
    }

    public JHubServer getJHubServer() { return jHubServer; }

    // ---------------------------------------------------------------
    // Main dispatch method — called by JHubServer.onMessage
    // ---------------------------------------------------------------

    /**
     * Route an inbound message from a registered app session.
     *
     * @param session    sender session
     * @param msg        parsed JSON object
     * @param rawJson    original raw string (for re-broadcast without re-serialization)
     * @param server     WebSocket server reference
     */
    public void route(JHubServer.AppSession session,
                      JsonObject msg,
                      String rawJson,
                      JHubServer server) {

        this.jHubServer = server; // keep reference current
        String type = msg.has("type") ? msg.get("type").getAsString() : "";

        switch (type) {
            case "RIG_STATUS":
                handleRigStatus(msg, rawJson, session.socket, server);
                break;

            case "LOGGER_SESSION":
                handleLoggerSession(msg, rawJson, session.socket, server);
                break;

            case "SPOT_SELECTED":
                handleSpotSelected(rawJson, session.socket, server);
                break;

            case "WSJTX_DECODE":
                handleWsjtxDecode(rawJson, session.socket, server);
                break;

            case "MODEM_DECODE":
                handleModemDecode(msg, rawJson, session.socket, server);
                break;

            case "LOG_ENTRY_DRAFT":
                handleLogEntryDraft(msg, rawJson, session.socket, server);
                break;

            case "SPOT":
                // Apps may re-publish spot data; handle same as cluster spots
                handleInboundSpot(msg, rawJson, session.socket, server);
                break;

            case "ROTOR_STATUS":
                handleRotorStatus(rawJson, session.socket, server);
                break;

            case "SAT_STATE":
                handleSatState(rawJson, session.socket, server);
                break;

            case "SAT_DOPPLER":
                handleSatDoppler(msg, rawJson, session.socket, server);
                break;

            case "SAT_ROTOR_CMD":
                handleSatRotorCmd(msg, rawJson, session.socket, server);
                break;

            default:
                log.debug("Unhandled message type '{}' from '{}'", type, session.appName);
        }
    }

    // ---------------------------------------------------------------
    // RIG_STATUS
    // ---------------------------------------------------------------

    private void handleRigStatus(JsonObject msg, String rawJson,
                                 WebSocket sender, JHubServer server) {
        try {
            RigStatus rig = ConfigManager.gson().fromJson(msg, RigStatus.class);
            rig.type = "RIG_STATUS"; // ensure type field is set
            StateCache.getInstance().setLastRigStatus(rig);

            // Rebroadcast to all connected apps (including sender so all stay in sync)
            server.broadcastToAll(ConfigManager.gson().toJson(rig));
            log.debug("RIG_STATUS: {} Hz, {}", rig.frequency, rig.mode);
        } catch (Exception e) {
            log.warn("Failed to process RIG_STATUS: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // LOGGER_SESSION
    // ---------------------------------------------------------------

    private void handleLoggerSession(JsonObject msg, String rawJson,
                                     WebSocket sender, JHubServer server) {
        StateCache.getInstance().setLastLoggerSession(rawJson);
        server.broadcastExcept(rawJson, sender);
        log.debug("LOGGER_SESSION cached and rebroadcast");
    }

    // ---------------------------------------------------------------
    // SPOT_SELECTED
    // ---------------------------------------------------------------

    private void handleSpotSelected(String rawJson, WebSocket sender, JHubServer server) {
        try {
            JsonObject msg = JsonParser.parseString(rawJson).getAsJsonObject();

            // If the message is missing frequency or has frequency=0, look up the full
            // spot from the cache so CI-V and entry-bar population get correct data.
            long freq = msg.has("frequency") ? msg.get("frequency").getAsLong() : 0L;
            if (freq == 0 && msg.has("spotted")) {
                String callsign = msg.get("spotted").getAsString();
                Spot cached = StateCache.getInstance().findSpotByCallsign(callsign);
                if (cached != null) {
                    msg.addProperty("frequency",   cached.frequency);
                    msg.addProperty("band",        cached.band);
                    msg.addProperty("mode",        cached.mode);
                    msg.addProperty("spotter",     cached.spotter);
                    msg.addProperty("comment",     cached.comment);
                    msg.addProperty("country",     cached.country);
                    msg.addProperty("continent",   cached.continent);
                    msg.addProperty("dxcc",        cached.dxcc);
                    msg.addProperty("lat",         cached.lat);
                    msg.addProperty("lon",         cached.lon);
                    msg.addProperty("bearing",     cached.bearing);
                    msg.addProperty("distanceKm",  cached.distanceKm);
                    msg.addProperty("distanceMi",  cached.distanceMi);
                    msg.addProperty("workedStatus", cached.workedStatus);
                    freq    = cached.frequency;
                    rawJson = msg.toString();
                    log.debug("SPOT_SELECTED enriched from cache for {}: freq={}", callsign, freq);
                }
            }

            // Cache for replay to late-joining apps
            StateCache.getInstance().setLastSelectedSpot(rawJson);

            // Broadcast enriched SPOT_SELECTED to ALL apps:
            //   • j-log populates its entry bar
            //   • j-map populates its DX window
            server.broadcastToAll(rawJson);

            // If j-hub owns Hamlib rig control, tune the rig directly.
            HamlibRigController hamlibCtrl = HamlibRigController.getInstance();
            if (freq > 0 && hamlibCtrl.isRunning()) {
                String m = msg.has("mode") ? msg.get("mode").getAsString() : null;
                hamlibCtrl.tune(freq, m);
            }

            // Also derive and broadcast RIG_STATUS so the app that owns CI-V/CAT
            // (j-log) tunes the rig to the correct frequency and mode.
            if (freq > 0) {
                RigStatus rig = new RigStatus();
                rig.source    = "SPOT_SELECTED";
                rig.timestamp = Instant.now().toString();
                rig.frequency = freq;
                if (msg.has("band")) rig.band = msg.get("band").getAsString();
                if (msg.has("mode")) rig.mode = msg.get("mode").getAsString();

                StateCache.getInstance().setLastRigStatus(rig);
                server.broadcastToAll(ConfigManager.gson().toJson(rig));
                log.debug("SPOT_SELECTED → RIG_STATUS freq={} mode={}", rig.frequency, rig.mode);
            } else {
                log.warn("SPOT_SELECTED has no frequency and no matching cached spot — CI-V skipped");
            }

        } catch (Exception e) {
            log.warn("Failed to handle SPOT_SELECTED: {}", e.getMessage());
            // Fall back: broadcast raw so apps at least get notified
            server.broadcastToAll(rawJson);
        }
    }

    // ---------------------------------------------------------------
    // WSJTX_DECODE
    // ---------------------------------------------------------------

    private void handleWsjtxDecode(String rawJson, WebSocket sender, JHubServer server) {
        server.broadcastToAll(rawJson);
        log.debug("WSJTX_DECODE rebroadcast");
    }

    // ---------------------------------------------------------------
    // MODEM_DECODE
    // ---------------------------------------------------------------

    private void handleModemDecode(JsonObject msg,
                                   String rawJson,
                                   WebSocket sender,
                                   JHubServer server) {
        try {
            server.broadcastToAll(rawJson);

            String mode = msg.has("mode") ? msg.get("mode").getAsString() : "";
            String text = msg.has("text") ? msg.get("text").getAsString() : "";
            log.debug("MODEM_DECODE [{}]: {}", mode, text);

        } catch (Exception e) {
            log.warn("Failed to process MODEM_DECODE: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // LOG_ENTRY_DRAFT
    // ---------------------------------------------------------------

    private void handleLogEntryDraft(JsonObject msg,
                                     String rawJson,
                                     WebSocket sender,
                                     JHubServer server) {
        try {
            server.broadcastToAll(rawJson);

            String callsign = msg.has("callsign") ? msg.get("callsign").getAsString() : "";
            String mode = msg.has("mode") ? msg.get("mode").getAsString() : "";
            log.debug("LOG_ENTRY_DRAFT call={} mode={}", callsign, mode);

        } catch (Exception e) {
            log.warn("Failed to process LOG_ENTRY_DRAFT: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // SPOT (from cluster or app)
    // ---------------------------------------------------------------

    private void handleInboundSpot(JsonObject msg, String rawJson,
                                   WebSocket sender, JHubServer server) {
        try {
            Spot spot = ConfigManager.gson().fromJson(msg, Spot.class);
            if (spot.timestamp == null) spot.timestamp = Instant.now().toString();
            StateCache.getInstance().addSpot(spot);
            server.broadcastToAll(ConfigManager.gson().toJson(spot));
        } catch (Exception e) {
            log.warn("Failed to process SPOT: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // ROTOR_STATUS
    // ---------------------------------------------------------------

    private void handleRotorStatus(String rawJson, WebSocket sender, JHubServer server) {
        StateCache.getInstance().setLastRotorStatus(rawJson);
        server.broadcastToAll(rawJson);
        log.debug("ROTOR_STATUS cached and rebroadcast");
    }

    // ---------------------------------------------------------------
    // SAT_STATE (from J-Sat)
    // ---------------------------------------------------------------

    private void handleSatState(String rawJson, WebSocket sender, JHubServer server) {
        StateCache.getInstance().setLastSatState(rawJson);
        server.broadcastToAll(rawJson);
        log.debug("SAT_STATE cached and rebroadcast");
    }

    // ---------------------------------------------------------------
    // SAT_DOPPLER — Doppler-corrected frequencies from J-Sat
    // ---------------------------------------------------------------

    private void handleSatDoppler(JsonObject msg, String rawJson,
                                  WebSocket sender, JHubServer server) {
        try {
            long downlinkHz = msg.has("downlinkHz") ? msg.get("downlinkHz").getAsLong() : 0L;
            long uplinkHz   = msg.has("uplinkHz")   ? msg.get("uplinkHz").getAsLong()   : 0L;
            String mode     = msg.has("mode")        ? msg.get("mode").getAsString()     : null;

            HamlibRigController rig = HamlibRigController.getInstance();
            if (rig.isRunning() && downlinkHz > 0) {
                rig.tune(downlinkHz, mode);
                log.debug("SAT_DOPPLER → rig DL={} Hz mode={}", downlinkHz, mode);
            }
            server.broadcastToAll(rawJson);
        } catch (Exception e) {
            log.warn("Failed to process SAT_DOPPLER: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // SAT_ROTOR_CMD — AZ/EL rotor command from J-Sat
    // ---------------------------------------------------------------

    private void handleSatRotorCmd(JsonObject msg, String rawJson,
                                   WebSocket sender, JHubServer server) {
        try {
            double az = msg.has("azDeg") ? msg.get("azDeg").getAsDouble() : 0.0;
            double el = msg.has("elDeg") ? msg.get("elDeg").getAsDouble() : 0.0;

            HamlibRotorController rotor = HamlibRotorController.getInstance();
            if (rotor.isRunning()) {
                rotor.trackPosition(az, el);
                log.debug("SAT_ROTOR_CMD → AZ={} EL={}", az, el);
            }
        } catch (Exception e) {
            log.warn("Failed to process SAT_ROTOR_CMD: {}", e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Called by ClusterManager when a new enriched spot arrives
    // ---------------------------------------------------------------

    /**
     * Broadcast a raw cluster telnet line to all connected apps (web UI feed).
     */
    public void publishRawLine(String line) {
        if (jHubServer != null) {
            JsonObject msg = new JsonObject();
            msg.addProperty("type", "CLUSTER_RAW");
            msg.addProperty("line", line);
            // Only send to the web config UI — ham radio apps don't need raw telnet lines
            jHubServer.broadcastToAppName("webconfig", msg.toString());
        }
    }

    /**
     * Publish a RIG_STATUS originating from HamlibRigController (or any internal source).
     */
    public void publishRigStatus(RigStatus rig) {
        StateCache.getInstance().setLastRigStatus(rig);
        if (jHubServer != null) {
            jHubServer.broadcastToAll(ConfigManager.gson().toJson(rig));
        }
        log.debug("RIG_STATUS published: {} Hz, {}", rig.frequency, rig.mode);
    }

    /**
     * Publish a cluster-originated spot to all connected apps and cache it.
     */
    public void publishSpot(Spot spot) {
        StateCache.getInstance().addSpot(spot);
        if (jHubServer != null) {
            jHubServer.broadcastToAll(ConfigManager.gson().toJson(spot));
        }
    }

    /**
     * Publish a ROTOR_STATUS originating from HamlibRotorController.
     */
    public void publishRotorStatus(String rawJson) {
        StateCache.getInstance().setLastRotorStatus(rawJson);
        if (jHubServer != null) {
            jHubServer.broadcastToAll(rawJson);
        }
    }

    // ---------------------------------------------------------------
    // Called when an app disconnects
    // ---------------------------------------------------------------

    public void onAppDisconnected(String appName) {
        log.info("Routing: app '{}' removed", appName);
        // The app list broadcast is handled by JHubServer.onClose
    }
}
