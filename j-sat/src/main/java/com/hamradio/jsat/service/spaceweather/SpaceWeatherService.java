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
 *   products/noaa-planetary-k-index.json      — Kp (array of objects, "Kp" field, capital K)
 *   products/solar-wind/plasma-1-day.json     — speed, density (array of arrays)
 *   products/solar-wind/mag-1-day.json        — Bz, Bt (array of arrays)
 *   json/goes/primary/xrays-1-day.json        — X-ray flux
 *   json/goes/primary/protons-1-day.json      — proton flux
 */
public class SpaceWeatherService {

    private static final Logger log = LoggerFactory.getLogger(SpaceWeatherService.class);
    private static final String BASE     = "https://services.swpc.noaa.gov/json/";
    private static final String PRODUCTS = "https://services.swpc.noaa.gov/products/";

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
            // products/noaa-planetary-k-index.json — array of objects, field "Kp" (capital K)
            JsonNode arr = get(PRODUCTS + "noaa-planetary-k-index.json");
            if (!arr.isArray() || arr.isEmpty()) return;
            for (int i = arr.size() - 1; i >= 0; i--) {
                JsonNode row = arr.get(i);
                JsonNode kpNode = row.path("Kp");
                if (!kpNode.isMissingNode() && !kpNode.isNull()) {
                    double kp = kpNode.asDouble(-1);
                    if (kp >= 0) {
                        d.kp      = kp;
                        d.kpLabel = String.format("Kp %.2f", kp);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Kp fetch error: {}", e.getMessage());
        }
    }

    private void fetchSolarWind(SpaceWeatherData d) {
        fetchSolarWindPlasma(d);
        fetchSolarWindMag(d);
    }

    // plasma-1-day: array of arrays, row[0]=headers ["time_tag","density","speed","temperature"]
    private void fetchSolarWindPlasma(SpaceWeatherData d) {
        try {
            JsonNode arr = get(PRODUCTS + "solar-wind/plasma-1-day.json");
            if (!arr.isArray() || arr.size() < 2) return;
            for (int i = arr.size() - 1; i >= 1; i--) {
                JsonNode row = arr.get(i);
                double speed = parseCell(row.get(2));
                if (!Double.isNaN(speed) && speed > 0) {
                    d.solarWindSpeedKmS = speed;
                    double dens = parseCell(row.get(1));
                    d.solarWindDensity  = Double.isNaN(dens) ? 0 : dens;
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Solar wind plasma fetch error: {}", e.getMessage());
        }
    }

    // mag-1-day: array of arrays, row[0]=headers ["time_tag","bx_gsm","by_gsm","bz_gsm","lon_gsm","lat_gsm","bt"]
    private void fetchSolarWindMag(SpaceWeatherData d) {
        try {
            JsonNode arr = get(PRODUCTS + "solar-wind/mag-1-day.json");
            if (!arr.isArray() || arr.size() < 2) return;
            for (int i = arr.size() - 1; i >= 1; i--) {
                JsonNode row = arr.get(i);
                double bz = parseCell(row.get(3));
                if (!Double.isNaN(bz)) {
                    d.imfBz = bz;
                    double bt = parseCell(row.get(6));
                    d.imfBt = Double.isNaN(bt) ? 0 : bt;
                    break;
                }
            }
        } catch (Exception e) {
            log.debug("Solar wind mag fetch error: {}", e.getMessage());
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

    private static double parseCell(JsonNode cell) {
        if (cell == null || cell.isNull()) return Double.NaN;
        String s = cell.asText("").trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return Double.NaN;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

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
