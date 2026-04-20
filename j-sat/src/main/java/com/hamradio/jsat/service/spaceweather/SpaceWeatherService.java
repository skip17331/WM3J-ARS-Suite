package com.hamradio.jsat.service.spaceweather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Fetches space weather data from NOAA SWPC JSON feeds.
 * No API key required. Updated every 5 minutes by the scheduler.
 *
 * Endpoints:
 *   kp_index.json           — Kp 1-min cadence
 *   dscovr_solar_wind.json  — ACE/DSCOVR solar wind
 *   goes/primary/xrays-1-day.json
 *   goes/primary/protons-1-day.json
 */
public class SpaceWeatherService {

    private static final Logger log = LoggerFactory.getLogger(SpaceWeatherService.class);
    private static final String BASE = "https://services.swpc.noaa.gov/json/";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final AtomicReference<SpaceWeatherData> cached = new AtomicReference<>(new SpaceWeatherData());
    private volatile Instant lastUpdated;

    public SpaceWeatherData getCached() { return cached.get(); }
    public Instant getLastUpdated()     { return lastUpdated; }

    /** Fetch all feeds and update cached data. */
    public void fetch() {
        SpaceWeatherData data = new SpaceWeatherData();
        fetchKp(data);
        fetchSolarWind(data);
        fetchXray(data);
        fetchProtons(data);
        data.fetchedAt = Instant.now();
        cached.set(data);
        lastUpdated = data.fetchedAt;
        log.debug("Space weather updated: Kp={} Bz={} {}", data.kp, data.imfBz, data.xrayClass);
    }

    // ── Individual feed fetchers ───────────────────────────────────────────────

    private void fetchKp(SpaceWeatherData d) {
        try {
            JsonNode arr = get(BASE + "kp_index.json");
            if (!arr.isArray()) return;
            // Walk backwards to find last non-null kp value
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonNode row = arr.get(i);
                if (row.size() >= 2 && !row.get(1).isNull()) {
                    String kpStr = row.get(1).asText("");
                    if (!kpStr.isBlank() && !kpStr.equals("null")) {
                        d.kp      = Double.parseDouble(kpStr);
                        d.kpLabel = "Kp " + kpStr;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Kp fetch error: {}", e.getMessage());
        }
    }

    private void fetchSolarWind(SpaceWeatherData d) {
        try {
            JsonNode arr = get(BASE + "dscovr_solar_wind.json");
            if (!arr.isArray()) return;
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonNode row = arr.get(i);
                double speed = row.path("speed").asDouble(0);
                if (speed > 0) {
                    d.solarWindSpeedKmS = speed;
                    d.solarWindDensity  = row.path("density").asDouble(0);
                    d.imfBt             = row.path("bt").asDouble(0);
                    d.imfBz             = row.path("bz_gsm").asDouble(0);
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Solar wind fetch error: {}", e.getMessage());
        }
    }

    private void fetchXray(SpaceWeatherData d) {
        try {
            JsonNode arr = get(BASE + "goes/primary/xrays-1-day.json");
            if (!arr.isArray()) return;
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonNode row = arr.get(i);
                if (!"0.1-0.8nm".equals(row.path("energy").asText())) continue;
                double flux = row.path("flux").asDouble(-1);
                if (flux > 0) {
                    d.xrayFluxWm2 = flux;
                    d.xrayClass   = fluxToClass(flux);
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("X-ray fetch error: {}", e.getMessage());
        }
    }

    private void fetchProtons(SpaceWeatherData d) {
        try {
            JsonNode arr = get(BASE + "goes/primary/protons-1-day.json");
            if (!arr.isArray()) return;
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonNode row = arr.get(i);
                if (!">=10 MeV".equals(row.path("energy").asText())) continue;
                double flux = row.path("flux").asDouble(-1);
                if (flux >= 0) {
                    d.protonFlux = flux;
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Proton fetch error: {}", e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private JsonNode get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Sat/1.0 WM3J-ARS")
            .timeout(Duration.ofSeconds(15))
            .GET().build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new RuntimeException("HTTP " + resp.statusCode());
        return MAPPER.readTree(resp.body());
    }

    private static String fluxToClass(double flux) {
        if (flux <= 0) return "---";
        char cls; double base;
        if      (flux >= 1e-4) { cls = 'X'; base = 1e-4; }
        else if (flux >= 1e-5) { cls = 'M'; base = 1e-5; }
        else if (flux >= 1e-6) { cls = 'C'; base = 1e-6; }
        else if (flux >= 1e-7) { cls = 'B'; base = 1e-7; }
        else                   { cls = 'A'; base = 1e-8; }
        return String.format("%c%.1f", cls, flux / base);
    }
}
