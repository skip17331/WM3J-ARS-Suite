package com.hamradio.jhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Polls <a href="https://pskreporter.info">pskreporter.info</a> every 5 minutes
 * for stations that have heard the configured station callsign within the last
 * hour, and rebroadcasts each reception as a {@code HEARD_BY_SPOT} WebSocket
 * message. Quiet no-op when the station callsign is unset.
 *
 * Each broadcast also gets pushed into {@link StateCache} so late-joining apps
 * can replay recent heard-by activity without waiting for the next poll.
 *
 * Rate-limit note: PSK Reporter asks that automated clients query no more
 * often than every 3 minutes per IP.
 */
public class PskReporterService {

    private static final Logger log = LoggerFactory.getLogger(PskReporterService.class);
    private static final String BASE_URL = "https://retrieve.pskreporter.info/query";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final PskReporterService INSTANCE = new PskReporterService();
    public  static PskReporterService getInstance() { return INSTANCE; }
    private PskReporterService() {}

    private ScheduledExecutorService scheduler;
    private volatile JHubServer server;
    private volatile Instant     lastFetchUtc;
    private volatile long        lastFlowStartSeconds;  // dedupe across polls

    public void start(JHubServer server) {
        this.server = server;
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "psk-reporter");
            t.setDaemon(true);
            return t;
        });
        // First fetch 20s after startup, then every 5 min.
        scheduler.scheduleAtFixedRate(this::fetchAndBroadcast, 20, 5 * 60, TimeUnit.SECONDS);
        log.info("PSK Reporter polling scheduled (every 5 min)");
    }

    public void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    public Instant getLastFetchUtc() { return lastFetchUtc; }

    // -----------------------------------------------------------------
    // Fetch + parse + broadcast
    // -----------------------------------------------------------------

    private void fetchAndBroadcast() {
        try {
            String stationCall = ConfigManager.getInstance().getStation().callsign;
            if (stationCall == null || stationCall.isBlank()) return;

            String url = BASE_URL + "?senderCallsign="
                       + URLEncoder.encode(stationCall.trim().toUpperCase(), StandardCharsets.UTF_8)
                       + "&flowStartSeconds=-3600&format=json";

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "j-Hub/1.0")
                .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("PSK Reporter fetch got HTTP {}", resp.statusCode());
                return;
            }

            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!root.has("receptionReport")) return;

            JsonArray reports = root.getAsJsonArray("receptionReport");
            long maxFlow = lastFlowStartSeconds;
            int broadcast = 0;
            for (JsonElement el : reports) {
                JsonObject r = el.getAsJsonObject();
                long flow = r.has("flowStartSeconds") ? r.get("flowStartSeconds").getAsLong() : 0L;
                if (flow <= lastFlowStartSeconds) continue;   // already seen
                if (flow > maxFlow) maxFlow = flow;

                JsonObject msg = new JsonObject();
                msg.addProperty("type",         "HEARD_BY_SPOT");
                msg.addProperty("source",       "PSK_REPORTER");
                msg.addProperty("senderCall",   str(r, "senderCallsign"));
                msg.addProperty("receiverCall", str(r, "receiverCallsign"));
                msg.addProperty("receiverGrid", str(r, "receiverLocator"));
                msg.addProperty("senderGrid",   str(r, "senderLocator"));
                msg.addProperty("frequencyHz",  r.has("frequency") ? r.get("frequency").getAsLong() : 0L);
                msg.addProperty("mode",         str(r, "mode"));
                msg.addProperty("snr",          r.has("sNR") ? r.get("sNR").getAsInt() : 0);
                msg.addProperty("timestampUtc", Instant.ofEpochSecond(flow).toString());
                msg.addProperty("fetchedAt",    Instant.now().toString());

                String json = msg.toString();
                StateCache.getInstance().addHeardBySpot(json);
                if (server != null) server.broadcastToAll(json);
                broadcast++;
            }
            lastFlowStartSeconds = maxFlow;
            lastFetchUtc = Instant.now();
            if (broadcast > 0) log.debug("PSK Reporter: broadcast {} new reports", broadcast);
        } catch (Exception e) {
            log.warn("PSK Reporter fetch failed: {}", e.getMessage());
        }
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
