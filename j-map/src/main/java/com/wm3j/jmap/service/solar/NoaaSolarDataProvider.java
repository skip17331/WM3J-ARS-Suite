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
 *   products/noaa-planetary-k-index.json   — Kp (array of objects, field "Kp" capital K)
 *   products/solar-wind/plasma-1-day.json  — Solar wind speed, density (array of arrays)
 *   products/solar-wind/mag-1-day.json     — IMF Bz, Bt (array of arrays)
 *   json/goes/primary/xrays-1-day.json     — GOES X-ray flux
 *   json/goes/primary/protons-1-day.json   — GOES proton flux ≥10 MeV
 */
public class NoaaSolarDataProvider extends AbstractDataProvider<SolarData>
        implements SolarDataProvider {

    private static final String BASE     = "https://services.swpc.noaa.gov/json/";
    private static final String PRODUCTS = "https://services.swpc.noaa.gov/products/";

    private static final String KP_URL      = PRODUCTS + "noaa-planetary-k-index.json";
    private static final String PLASMA_URL  = PRODUCTS + "solar-wind/plasma-1-day.json";
    private static final String MAG_URL     = PRODUCTS + "solar-wind/mag-1-day.json";
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
        fetchSolarWindPlasma(data);
        fetchSolarWindMag(data);
        fetchXray(data);
        fetchProtons(data);

        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    // ── Kp index ───────────────────────────────────────────────
    // Array of objects with field "Kp" (capital K).  Walk backwards for last valid value.
    private void fetchKp(SolarData data) {
        try {
            JsonNode root = get(KP_URL);
            if (!root.isArray() || root.size() == 0) return;
            for (int i = root.size() - 1; i >= 0; i--) {
                JsonNode row = root.get(i);
                JsonNode kpNode = row.path("Kp");
                if (!kpNode.isMissingNode() && !kpNode.isNull()) {
                    double kp = kpNode.asDouble(-1);
                    if (kp >= 0) {
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

    // ── Solar wind plasma ──────────────────────────────────────
    // Array of arrays.  Row 0 = headers.  Columns: [time_tag, density, speed, temperature]
    private void fetchSolarWindPlasma(SolarData data) {
        try {
            JsonNode root = get(PLASMA_URL);
            if (!root.isArray() || root.size() < 2) return;
            for (int i = root.size() - 1; i >= 1; i--) {
                JsonNode row = root.get(i);
                double speed = parseCell(row.get(2));
                if (!Double.isNaN(speed) && speed > 0) {
                    data.setSolarWindSpeed(speed);
                    double dens = parseCell(row.get(1));
                    data.setSolarWindDensity(Double.isNaN(dens) ? 0 : dens);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Solar wind plasma fetch failed: {}", e.getMessage());
        }
    }

    // ── Solar wind mag (IMF) ───────────────────────────────────
    // Array of arrays.  Row 0 = headers.  Columns: [time_tag, bx_gsm, by_gsm, bz_gsm, lon_gsm, lat_gsm, bt]
    private void fetchSolarWindMag(SolarData data) {
        try {
            JsonNode root = get(MAG_URL);
            if (!root.isArray() || root.size() < 2) return;
            for (int i = root.size() - 1; i >= 1; i--) {
                JsonNode row = root.get(i);
                double bz = parseCell(row.get(3));
                if (!Double.isNaN(bz)) {
                    data.setBzField(bz);
                    double bt = parseCell(row.get(6));
                    data.setBtField(Double.isNaN(bt) ? 0 : bt);
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Solar wind mag fetch failed: {}", e.getMessage());
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

    // ── Helpers ────────────────────────────────────────────────

    private static double parseCell(JsonNode cell) {
        if (cell == null || cell.isNull()) return Double.NaN;
        String s = cell.asText("").trim();
        if (s.isEmpty() || s.equalsIgnoreCase("null")) return Double.NaN;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return Double.NaN; }
    }

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
