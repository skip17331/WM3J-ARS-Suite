package com.hamradio.jhub;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Broadcasts a {@code SOLAR_FLUX} WebSocket message every 60 seconds, sourced
 * from the shared {@link WeatherService} cache (NOAA SWPC). This used to fetch
 * hamqsl.com directly, but that path was unreliable — the Weather tab in the
 * j-hub web UI already shows good NOAA data, so we now pull from the same cache
 * instead of double-fetching. Everyone sees the same numbers.
 *
 * Field mapping (WeatherService → SOLAR_FLUX):
 *   kp        → k
 *   sfi       → sfi     (NOAA 10.7cm flux; ham-radio SFI)
 *   sunspots  → sn
 *   xrayClass → xray
 *   solarWindSpeed → solarwind
 */
public class SolarFluxService {

    private static final Logger log = LoggerFactory.getLogger(SolarFluxService.class);

    private static final SolarFluxService INSTANCE = new SolarFluxService();
    public  static SolarFluxService getInstance() { return INSTANCE; }
    private SolarFluxService() {}

    private ScheduledExecutorService scheduler;
    private volatile JHubServer server;
    private volatile String     lastBroadcast;     // last JSON we sent
    private volatile Instant    lastFetchUtc;

    public void start(JHubServer server) {
        this.server = server;
        if (scheduler != null) return;
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "solar-flux");
            t.setDaemon(true);
            return t;
        });
        // Poll the WeatherService cache every minute — WeatherService itself
        // refreshes from NOAA every 5 minutes, so this is cheap and keeps
        // newly-connected apps current.
        scheduler.scheduleAtFixedRate(this::broadcastFromCache, 10, 60, TimeUnit.SECONDS);
        log.info("Solar flux broadcaster scheduled (from WeatherService cache)");
    }

    public void stop() {
        if (scheduler != null) { scheduler.shutdownNow(); scheduler = null; }
    }

    /** Last JSON payload broadcast. Null until the first successful broadcast. */
    public String getLatestBroadcast() { return lastBroadcast; }
    public Instant getLastFetchUtc()   { return lastFetchUtc; }

    // -----------------------------------------------------------------

    private void broadcastFromCache() {
        try {
            String cached = WeatherService.getInstance().getCachedJson();
            if (cached == null || cached.isBlank()) return;
            JsonObject root = JsonParser.parseString(cached).getAsJsonObject();
            JsonObject sw = root.has("spaceWeather") && root.get("spaceWeather").isJsonObject()
                          ? root.getAsJsonObject("spaceWeather")
                          : new JsonObject();

            JsonObject msg = new JsonObject();
            msg.addProperty("type", "SOLAR_FLUX");
            if (sw.has("sfi"))            msg.addProperty("sfi",       fmtNum(sw.get("sfi")));
            if (sw.has("kp"))             msg.addProperty("k",         fmtNum(sw.get("kp")));
            if (sw.has("sunspots"))       msg.addProperty("sn",        fmtNum(sw.get("sunspots")));
            if (sw.has("xrayClass"))      msg.addProperty("xray",      sw.get("xrayClass").getAsString());
            if (sw.has("solarWindSpeed")) msg.addProperty("solarwind", fmtNum(sw.get("solarWindSpeed")));
            if (sw.has("kpCondition"))    msg.addProperty("geomag",    sw.get("kpCondition").getAsString());
            msg.addProperty("fetchedAt", Instant.now().toString());
            msg.addProperty("source", "NOAA SWPC");

            String json = msg.toString();
            // Only rebroadcast when the payload actually changes, and always
            // on the first tick after startup.
            if (json.equals(lastBroadcast)) return;
            lastBroadcast = json;
            lastFetchUtc  = Instant.now();
            if (server != null) server.broadcastToAll(json);
            log.debug("Broadcast SOLAR_FLUX: {}", json);
        } catch (Exception e) {
            log.debug("broadcastFromCache failed: {}", e.getMessage());
        }
    }

    /** Trim trailing ".0" from integers so the UI reads "123" not "123.0". */
    private static String fmtNum(JsonElement el) {
        if (el == null || el.isJsonNull()) return "";
        try {
            double d = el.getAsDouble();
            return (d == Math.floor(d)) ? String.valueOf((long) d)
                                        : String.format("%.1f", d);
        } catch (Exception ignored) {
            return el.getAsString();
        }
    }
}
