package com.wm3j.jmap.service.solar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Fetches solar and space weather data from NOAA SWPC JSON feeds.
 *
 * Endpoints:
 *   kp_index.json          — Kp (3-hour planetary index)
 *   dscovr_solar_wind.json — Solar wind speed, density, IMF Bt/Bz
 *   xrays-1-day.json       — GOES X-ray flux → X-ray class
 *   protons-1-day.json     — GOES proton flux ≥10 MeV
 */
public class NoaaSolarDataProvider extends AbstractDataProvider<SolarData>
        implements SolarDataProvider {

    private static final String BASE = "https://services.swpc.noaa.gov/json/";

    private static final String KP_URL      = BASE + "kp_index.json";
    private static final String SW_URL      = BASE + "dscovr_solar_wind.json";
    private static final String XRAY_URL    = BASE + "goes/primary/xrays-1-day.json";
    private static final String PROTON_URL  = BASE + "goes/primary/protons-1-day.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    protected SolarData doFetch() throws DataProviderException {
        SolarData data = new SolarData();

        fetchKp(data);
        fetchSolarWind(data);
        fetchXray(data);
        fetchProtons(data);

        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    // ── Kp index ───────────────────────────────────────────────
    // Array of [time_tag, kp, a_running, station_count] — last entry = most recent.
    // Values are strings.
    private void fetchKp(SolarData data) {
        try {
            JsonNode root = get(KP_URL);
            if (!root.isArray() || root.size() == 0) return;

            // Walk backwards to find last non-null Kp value
            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode row = root.get(i);
                if (row.isArray() && row.size() >= 2) {
                    String kpStr = row.get(1).asText("");
                    if (!kpStr.isBlank() && !kpStr.equals("null")) {
                        double kp = Double.parseDouble(kpStr);
                        data.setKp(kp);
                        data.setAIndex(kpToAIndex(kp));
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Kp fetch failed: {}", e.getMessage());
        }
    }

    // ── DSCOVR solar wind ──────────────────────────────────────
    // Array of objects; last entry = most recent 1-minute reading.
    private void fetchSolarWind(SolarData data) {
        try {
            JsonNode root = get(SW_URL);
            if (!root.isArray() || root.size() == 0) return;

            // Walk backwards for a recent entry with valid speed
            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                double speed = entry.path("speed").asDouble(-1);
                if (speed > 0) {
                    data.setSolarWindSpeed(speed);
                    data.setSolarWindDensity(entry.path("density").asDouble(0));
                    data.setBtField(entry.path("bt").asDouble(0));
                    data.setBzField(entry.path("bz_gsm").asDouble(0));
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Solar wind fetch failed: {}", e.getMessage());
        }
    }

    // ── GOES X-ray flux ────────────────────────────────────────
    // Array of {time_tag, flux, energy}. Use 0.1-0.8nm (XRS-B) channel.
    // Flux in W/m². Most recent data is at end of array.
    private void fetchXray(SolarData data) {
        try {
            JsonNode root = get(XRAY_URL);
            if (!root.isArray() || root.size() == 0) return;

            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                String energy = entry.path("energy").asText("");
                if ("0.1-0.8nm".equals(energy)) {
                    double flux = entry.path("flux").asDouble(-1);
                    if (flux > 0) {
                        data.setXrayFlux(flux); // also sets xrayClass via setter
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("X-ray fetch failed: {}", e.getMessage());
        }
    }

    // ── GOES proton flux ───────────────────────────────────────
    // Array of {time_tag, flux, energy}. Use >=10 MeV channel.
    // Flux in pfu (particles/cm²/s/sr).
    private void fetchProtons(SolarData data) {
        try {
            JsonNode root = get(PROTON_URL);
            if (!root.isArray() || root.size() == 0) return;

            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode entry = root.get(i);
                String energy = entry.path("energy").asText("");
                if (">=10 MeV".equals(energy)) {
                    double flux = entry.path("flux").asDouble(-1);
                    if (flux >= 0) {
                        data.setProtonFlux(flux);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Proton fetch failed: {}", e.getMessage());
        }
    }

    // ── HTTP helper ────────────────────────────────────────────

    private JsonNode get(String url) throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "J-Map/1.0 (ham radio ARS Suite)")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("HTTP " + resp.statusCode() + " from " + url,
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }
            return MAPPER.readTree(resp.body());
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Fetch failed: " + url + " — " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private static int kpToAIndex(double kp) {
        int[] table = {0, 3, 7, 15, 27, 48, 80, 132, 208, 400};
        return table[(int) Math.min(9, Math.max(0, kp))];
    }
}
