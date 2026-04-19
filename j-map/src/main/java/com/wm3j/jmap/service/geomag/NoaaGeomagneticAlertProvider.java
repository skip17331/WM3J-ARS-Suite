package com.wm3j.jmap.service.geomag;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fetches real-time Kp and 24-hour geomagnetic forecast from NOAA SWPC.
 *
 * Endpoints:
 *   kp_index.json       — current 3-hour Kp (array of [time_tag, kp, ...])
 *   geomag_forecast.json — 3-day Kp forecast (array of {time_tag, kp, ...})
 */
public class NoaaGeomagneticAlertProvider extends AbstractDataProvider<GeomagneticAlert>
        implements GeomagneticAlertProvider {

    private static final String KP_URL       = "https://services.swpc.noaa.gov/json/kp_index.json";
    private static final String FORECAST_URL = "https://services.swpc.noaa.gov/json/geomag_forecast.json";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    protected GeomagneticAlert doFetch() throws DataProviderException {
        double kp = fetchCurrentKp();
        List<Double> forecast = fetchForecast();
        double a = kpToAIndex(kp);

        GeomagneticAlert.Level level;
        String summary;
        if (kp >= 9)      { level = GeomagneticAlert.Level.ALERT;   summary = "G5 Extreme Storm"; }
        else if (kp >= 8) { level = GeomagneticAlert.Level.ALERT;   summary = "G4 Severe Storm";  }
        else if (kp >= 7) { level = GeomagneticAlert.Level.ALERT;   summary = "G3 Strong Storm";  }
        else if (kp >= 6) { level = GeomagneticAlert.Level.WARNING; summary = "G2 Moderate Storm"; }
        else if (kp >= 5) { level = GeomagneticAlert.Level.WATCH;   summary = "G1 Minor Storm";   }
        else              { level = GeomagneticAlert.Level.NONE;    summary = "Quiet"; }

        // Escalate level if forecast shows upcoming storm
        double maxForecast = forecast.stream().mapToDouble(Double::doubleValue).max().orElse(kp);
        List<String> messages = new ArrayList<>();
        messages.add(String.format("Kp=%.1f  A=%d  %s", kp, (int) a, summary));
        if (maxForecast >= 5 && maxForecast > kp) {
            messages.add(String.format("24h forecast max: Kp=%.1f (%s)",
                maxForecast, kpToGScale(maxForecast)));
        }
        if (messages.size() < 2) messages.add("Conditions nominal");

        GeomagneticAlert alert = new GeomagneticAlert(kp, a, level, summary, messages, Instant.now());
        alert.setKpForecast(forecast);
        return alert;
    }

    // ── Current Kp ─────────────────────────────────────────────
    // Array of [time_tag, kp_value, a_running, station_count] — most recent last.
    private double fetchCurrentKp() throws DataProviderException {
        JsonNode root = get(KP_URL);
        if (!root.isArray() || root.size() == 0) return 0;

        for (int i = root.size() - 1; i >= 0; i--) {
            JsonNode row = root.get(i);
            if (!row.isArray() || row.size() < 2) continue;
            String kpStr = row.get(1).asText("").trim();
            if (!kpStr.isBlank() && !kpStr.equalsIgnoreCase("null")) {
                try { return Double.parseDouble(kpStr); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    // ── 24-hour forecast ────────────────────────────────────────
    // Array of {time_tag, kp, a_minor, storm_level, ...}
    // Returns up to 8 upcoming 3-hour Kp values (24 hours).
    private List<Double> fetchForecast() {
        try {
            JsonNode root = get(FORECAST_URL);
            if (!root.isArray() || root.size() == 0) return Collections.emptyList();

            List<Double> values = new ArrayList<>();
            Instant now = Instant.now();
            for (JsonNode entry : root) {
                if (values.size() >= 8) break;
                double kp = entry.path("kp").asDouble(-1);
                if (kp < 0) continue;
                // Include entries from now onward (skip historical)
                String timeStr = entry.path("time_tag").asText("");
                if (!timeStr.isBlank()) {
                    try {
                        // Format: "2024-01-01 03:00:00" — parse as UTC
                        Instant t = Instant.parse(timeStr.replace(" ", "T") + "Z");
                        if (t.isBefore(now)) continue;
                    } catch (Exception ignored) {}
                }
                values.add(kp);
            }
            return values;
        } catch (Exception e) {
            log.debug("Forecast fetch failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private JsonNode get(String url) throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "J-Map/1.0")
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
            throw new DataProviderException("Geomag fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private static double kpToAIndex(double kp) {
        int[] table = {0, 3, 7, 15, 27, 48, 80, 132, 208, 400};
        return table[(int) Math.min(9, Math.max(0, kp))];
    }

    private static String kpToGScale(double kp) {
        if (kp >= 9) return "G5";
        if (kp >= 8) return "G4";
        if (kp >= 7) return "G3";
        if (kp >= 6) return "G2";
        if (kp >= 5) return "G1";
        return "G0";
    }
}
