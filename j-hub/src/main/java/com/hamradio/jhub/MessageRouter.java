package com.hamradio.jhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hamradio.jhub.callsign.CallsignLookupService;
import com.hamradio.jhub.callsign.CallsignResult;
import com.hamradio.jhub.model.RigStatus;
import com.hamradio.jhub.model.Spot;
import org.java_websocket.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    // Contest-sync roster. Keyed by contestId; each entry is the ordered list
    // of stationName strings currently joined. The ordering determines stride
    // allocation — joiners get the next slot.
    private final Map<String, List<String>> contestRoster = new ConcurrentHashMap<>();

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

            case "MODEM_TX":
                // j-log sends text to be transmitted via the modem app.
                server.broadcastExcept(rawJson, session.socket);
                log.debug("MODEM_TX relayed from '{}'", session.appName);
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

            case "ROTOR_CMD":
                handleRotorCmd(msg, session.socket, server);
                break;

            case "JMAP_CONFIG_REQUEST":
                handleJMapConfigRequest(session, server);
                break;

            case "CALLSIGN_LOOKUP":
                handleCallsignLookup(msg, server);
                break;

            case "CONTEST_ACTIVE":
            case "CONTEST_INACTIVE":
                server.broadcastExcept(rawJson, session.socket);
                log.debug("{} broadcast from '{}'", type, session.appName);
                break;

            // Contest multi-op sync — hub relays QSOs and maintains the roster.
            case "QSO_SAVED":
            case "QSO_DELETED":
            case "REQUEST_FULL_SYNC":
            case "FULL_SYNC_START":
                server.broadcastExcept(rawJson, session.socket);
                log.debug("{} relayed from '{}'", type, session.appName);
                break;

            case "JOIN_CONTEST":
                handleJoinContest(msg, session, server);
                break;

            case "LEAVE_CONTEST":
                handleLeaveContest(msg, session, server);
                break;

            default:
                log.debug("Unhandled message type '{}' from '{}'", type, session.appName);
        }
    }

    // ---------------------------------------------------------------
    // Contest roster (multi-op sync)
    // ---------------------------------------------------------------

    private void handleJoinContest(JsonObject msg, JHubServer.AppSession session, JHubServer server) {
        String contestId   = msg.has("contestId")   ? msg.get("contestId").getAsString()   : "";
        String stationName = msg.has("stationName") ? msg.get("stationName").getAsString() : session.appName;
        if (contestId.isBlank()) return;

        List<String> roster = contestRoster.computeIfAbsent(contestId, k -> new ArrayList<>());
        synchronized (roster) {
            roster.remove(stationName);           // idempotent rejoin
            roster.add(stationName);
        }
        broadcastRoster(contestId, server);
        log.info("JOIN_CONTEST: {} joined {} (now {} stations)", stationName, contestId, roster.size());
    }

    private void handleLeaveContest(JsonObject msg, JHubServer.AppSession session, JHubServer server) {
        String contestId   = msg.has("contestId")   ? msg.get("contestId").getAsString()   : "";
        String stationName = msg.has("stationName") ? msg.get("stationName").getAsString() : session.appName;
        if (contestId.isBlank()) return;

        List<String> roster = contestRoster.get(contestId);
        if (roster == null) return;
        synchronized (roster) { roster.remove(stationName); }
        if (roster.isEmpty()) contestRoster.remove(contestId);
        broadcastRoster(contestId, server);
        log.info("LEAVE_CONTEST: {} left {} (now {} stations)", stationName, contestId, roster.size());
    }

    private void broadcastRoster(String contestId, JHubServer server) {
        List<String> roster = contestRoster.get(contestId);
        JsonObject msg = new JsonObject();
        msg.addProperty("type",      "CONTEST_ROSTER");
        msg.addProperty("contestId", contestId);
        JsonArray arr = new JsonArray();
        if (roster != null) synchronized (roster) { roster.forEach(arr::add); }
        msg.add("stations", arr);
        server.broadcastToAll(msg.toString());
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

            // Kick off an async callsign lookup so all apps get operator details
            String spotted = msg.has("spotted") ? msg.get("spotted").getAsString().trim() : "";
            if (!spotted.isEmpty()) asyncCallsignLookup(spotted, server);

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
    // JMAP_CONFIG_REQUEST — j-map asks for its current config
    // ---------------------------------------------------------------

    private void handleJMapConfigRequest(JHubServer.AppSession session, JHubServer server) {
        String cached = StateCache.getInstance().getLastJMapConfig();
        if (cached != null) {
            server.sendTo(session.socket, cached);
            return;
        }
        // Fall back to stored jMapSettings if cache is cold (e.g. first boot)
        com.google.gson.JsonObject stored = ConfigManager.getInstance().getConfig().jMapSettings;
        if (stored != null && stored.size() > 0) {
            com.google.gson.JsonObject reply = stored.deepCopy();
            reply.addProperty("type", "JMAP_CONFIG");
            server.sendTo(session.socket, reply.toString());
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
    // ROTOR_CMD — manual command from web config UI
    // ---------------------------------------------------------------

    private void handleRotorCmd(JsonObject msg, WebSocket sender, JHubServer server) {
        try {
            HamlibRotorController rotor = HamlibRotorController.getInstance();
            if (!rotor.isRunning()) return;

            if (msg.has("stop") && msg.get("stop").getAsBoolean()) {
                rotor.stopRotor();
                log.debug("ROTOR_CMD → STOP");
                return;
            }

            // Determine current az/el from cached status so partial updates work
            double currentAz = 0.0, currentEl = 0.0;
            String cached = StateCache.getInstance().getLastRotorStatus();
            if (cached != null) {
                try {
                    JsonObject c = JsonParser.parseString(cached).getAsJsonObject();
                    if (c.has("bearing"))   currentAz = c.get("bearing").getAsDouble();
                    if (c.has("elevation")) currentEl = c.get("elevation").getAsDouble();
                } catch (Exception ignored) {}
            }

            double az = msg.has("heading")   ? msg.get("heading").getAsDouble()   : currentAz;
            double el = msg.has("elevation") ? msg.get("elevation").getAsDouble() : currentEl;

            rotor.trackPosition(az, el);
            log.debug("ROTOR_CMD → AZ={} EL={}", az, el);
        } catch (Exception e) {
            log.warn("Failed to process ROTOR_CMD: {}", e.getMessage());
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

    /**
     * Publish an AMP_STATUS originating from HamlibAmpController.
     */
    public void publishAmpStatus(com.hamradio.jhub.model.AmpStatus amp) {
        if (jHubServer == null) return;
        jHubServer.broadcastToAll(ConfigManager.gson().toJson(amp));
        if (amp.faulted) {
            log.warn("AMP fault: {}", amp.faultReason);
        } else {
            log.debug("AMP_STATUS: {} Hz, swr={}, pwr={}, {}",
                    amp.frequency, amp.swr, amp.fwdPower, amp.powerstat);
        }
    }

    // ---------------------------------------------------------------
    // CALLSIGN_LOOKUP — app requests a callsign lookup
    // ---------------------------------------------------------------

    private void handleCallsignLookup(JsonObject msg, JHubServer server) {
        String callsign = msg.has("callsign") ? msg.get("callsign").getAsString().trim().toUpperCase() : "";
        if (!callsign.isEmpty()) asyncCallsignLookup(callsign, server);
    }

    /**
     * Runs a callsign lookup in a daemon thread and broadcasts CALLSIGN_RESULT to all apps.
     * Lookup results are cached by CallsignLookupService so repeated calls are fast.
     */
    private void asyncCallsignLookup(String callsign, JHubServer server) {
        Thread t = new Thread(() -> {
            try {
                CallsignResult r = CallsignLookupService.getInstance().lookup(callsign);
                JsonObject out = new JsonObject();
                out.addProperty("type",           "CALLSIGN_RESULT");
                out.addProperty("callsign",        r.callsign != null ? r.callsign : callsign);
                out.addProperty("found",           r.found);
                out.addProperty("name",            nvl(r.name));
                out.addProperty("firstName",       nvl(r.firstName));
                out.addProperty("lastName",        nvl(r.lastName));
                out.addProperty("addr1",           nvl(r.addr1));
                out.addProperty("city",            nvl(r.city));
                out.addProperty("state",           nvl(r.state));
                out.addProperty("country",         nvl(r.country));
                out.addProperty("grid",            nvl(r.grid));
                out.addProperty("licenseClass",    nvl(r.licenseClass));
                out.addProperty("licenseExpires",  nvl(r.licenseExpires));
                out.addProperty("lat",             r.lat);
                out.addProperty("lon",             r.lon);
                out.addProperty("source",          nvl(r.source));
                server.broadcastToAll(out.toString());
                log.debug("CALLSIGN_RESULT broadcast for {}: found={} source={}", callsign, r.found, r.source);
            } catch (Exception e) {
                log.warn("Callsign lookup failed for {}: {}", callsign, e.getMessage());
            }
        }, "jhub-callsign-lookup");
        t.setDaemon(true);
        t.start();
    }

    private static String nvl(String v) { return v != null ? v : ""; }

    // ---------------------------------------------------------------
    // Called when an app disconnects
    // ---------------------------------------------------------------

    public void onAppDisconnected(String appName) {
        log.info("Routing: app '{}' removed", appName);
        // The app list broadcast is handled by JHubServer.onClose
    }
}
