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
 * Polls <a href="https://db1.wspr.live/">db1.wspr.live</a> (a ClickHouse HTTP
 * interface fronting the full WSPRnet spot archive) every 10 minutes for
 * receptions of the configured station callsign in the last hour, and
 * rebroadcasts each as a {@code HEARD_BY_SPOT} WebSocket message. WSPR spots
 * carry real computed distance, so we pass it straight through.
 *
 * Quiet no-op when the station callsign is unset.
 */
public class WsprReporterService {

    private static final Logger log = LoggerFactory.getLogger(WsprReporterService.class);
    private static final String BASE_URL = "https://db1.wspr.live/";
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private static final WsprReporterService INSTANCE = new WsprReporterService();
    public  static WsprReporterService getInstance() { return INSTANCE; }
    private WsprReporterService() {}

    private ScheduledExecutorService scheduler;
    private volatile JHubServer server;
    private volatile String      lastSeenTime = "";   // dedupe across polls

    public void start(JHubServer server) {
        this.server = server;
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "wspr-reporter");
            t.setDaemon(true);
            return t;
        });
        // First fetch 30s after startup, then every 10 min.
        scheduler.scheduleAtFixedRate(this::fetchAndBroadcast, 30, 10 * 60, TimeUnit.SECONDS);
        log.info("WSPR polling scheduled (every 10 min)");
    }

    public void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    // -----------------------------------------------------------------
    // Fetch + parse + broadcast
    // -----------------------------------------------------------------

    private void fetchAndBroadcast() {
        try {
            String stationCall = ConfigManager.getInstance().getStation().callsign;
            if (stationCall == null || stationCall.isBlank()) return;
            String call = stationCall.trim().toUpperCase().replace("'", "");

            String sql = "SELECT toString(time) AS t, band, tx_sign, rx_sign, rx_loc, tx_loc, "
                       + "distance, snr, frequency "
                       + "FROM wspr.rx WHERE tx_sign='" + call + "' "
                       + "AND time > subtractHours(now(), 1) "
                       + "ORDER BY time DESC LIMIT 200 FORMAT JSON";
            String url = BASE_URL + "?query=" + URLEncoder.encode(sql, StandardCharsets.UTF_8);

            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "j-Hub/1.0")
                .GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                log.warn("wspr.live fetch got HTTP {}", resp.statusCode());
                return;
            }

            JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (!root.has("data")) return;

            JsonArray rows = root.getAsJsonArray("data");
            String maxTime = lastSeenTime;
            int broadcast = 0;
            for (JsonElement el : rows) {
                JsonObject r = el.getAsJsonObject();
                String t = str(r, "t");
                if (t.isBlank() || (!lastSeenTime.isEmpty() && t.compareTo(lastSeenTime) <= 0)) continue;
                if (t.compareTo(maxTime) > 0) maxTime = t;

                long freqHz = 0L;
                if (r.has("frequency") && !r.get("frequency").isJsonNull()) {
                    // wspr.live returns frequency in MHz as a double string.
                    try { freqHz = Math.round(r.get("frequency").getAsDouble() * 1_000_000); }
                    catch (Exception ignored) {}
                }

                JsonObject msg = new JsonObject();
                msg.addProperty("type",         "HEARD_BY_SPOT");
                msg.addProperty("source",       "WSPR");
                msg.addProperty("senderCall",   str(r, "tx_sign"));
                msg.addProperty("receiverCall", str(r, "rx_sign"));
                msg.addProperty("receiverGrid", str(r, "rx_loc"));
                msg.addProperty("senderGrid",   str(r, "tx_loc"));
                msg.addProperty("band",         str(r, "band"));
                msg.addProperty("mode",         "WSPR");
                msg.addProperty("frequencyHz",  freqHz);
                msg.addProperty("snr",          r.has("snr") ? r.get("snr").getAsInt() : 0);
                msg.addProperty("distanceKm",   r.has("distance") ? r.get("distance").getAsInt() : 0);
                msg.addProperty("timestampUtc", isoFromClickhouse(t));
                msg.addProperty("fetchedAt",    Instant.now().toString());

                String json = msg.toString();
                StateCache.getInstance().addHeardBySpot(json);
                if (server != null) server.broadcastToAll(json);
                broadcast++;
            }
            lastSeenTime = maxTime;
            if (broadcast > 0) log.debug("WSPR: broadcast {} new reports", broadcast);
        } catch (Exception e) {
            log.warn("WSPR fetch failed: {}", e.getMessage());
        }
    }

    /** ClickHouse returns "2026-04-24 06:28:00" — convert to ISO-8601 UTC. */
    private static String isoFromClickhouse(String t) {
        if (t == null || t.isBlank()) return "";
        return t.replace(' ', 'T') + "Z";
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
