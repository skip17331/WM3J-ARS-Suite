package com.hamclock.service.geomag;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches real-time geomagnetic Kp data from NOAA SWPC.
 * Uses the planetary_k_index_1m.json feed (1-minute cadence).
 */
public class NoaaGeomagneticAlertProvider extends AbstractDataProvider<GeomagneticAlert>
        implements GeomagneticAlertProvider {

    private static final Logger log = LoggerFactory.getLogger(NoaaGeomagneticAlertProvider.class);

    // NOAA SWPC 1-minute K-index
    private static final String KP_URL =
        "https://services.swpc.noaa.gov/products/noaa-planetary-k-index.json";

    // NOAA SWPC alert messages
    private static final String ALERTS_URL =
        "https://services.swpc.noaa.gov/products/alerts.json";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected GeomagneticAlert doFetch() throws DataProviderException {
        try {
            // Fetch current Kp
            double kp = fetchCurrentKp();
            double a  = kpToAIndex(kp);

            // Fetch any active alerts
            List<String> messages = fetchAlertMessages();

            // Determine level from Kp
            GeomagneticAlert.Level level;
            String summary;
            if (kp >= 9) {
                level = GeomagneticAlert.Level.ALERT;   summary = "G5 Extreme Storm";
            } else if (kp >= 8) {
                level = GeomagneticAlert.Level.ALERT;   summary = "G4 Severe Storm";
            } else if (kp >= 7) {
                level = GeomagneticAlert.Level.ALERT;   summary = "G3 Strong Storm";
            } else if (kp >= 6) {
                level = GeomagneticAlert.Level.WARNING; summary = "G2 Moderate Storm";
            } else if (kp >= 5) {
                level = GeomagneticAlert.Level.WATCH;   summary = "G1 Minor Storm";
            } else {
                level = GeomagneticAlert.Level.NONE;    summary = "Quiet (Kp=" + String.format("%.1f", kp) + ")";
            }

            return new GeomagneticAlert(kp, a, level, summary, messages, Instant.now());

        } catch (Exception e) {
            throw new DataProviderException("NOAA geomag fetch failed: " + e.getMessage(), DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private double fetchCurrentKp() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(KP_URL))
            .header("User-Agent", "HamClock/1.0")
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new Exception("HTTP " + resp.statusCode());

        // JSON is array of [time, Kp, ...] rows; last entry is most recent
        String body = resp.body().trim();
        // Find last numeric Kp value: parse last array entry
        Pattern p = Pattern.compile("\\[\"[^\"]+\",\\s*\"([0-9.]+)\"");
        Matcher m = p.matcher(body);
        double kp = 0;
        while (m.find()) {
            try { kp = Double.parseDouble(m.group(1)); } catch (NumberFormatException ignored) {}
        }
        return kp;
    }

    private List<String> fetchAlertMessages() {
        List<String> msgs = new ArrayList<>();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(ALERTS_URL))
                .header("User-Agent", "HamClock/1.0")
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                // Extract "message" fields from JSON array
                Pattern p = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]{5,80})\"");
                Matcher m = p.matcher(resp.body());
                while (m.find() && msgs.size() < 5) {
                    msgs.add(m.group(1));
                }
            }
        } catch (Exception e) {
            log.debug("Could not fetch alert messages: {}", e.getMessage());
        }
        if (msgs.isEmpty()) msgs.add("No active alerts");
        return msgs;
    }

    private static double kpToAIndex(double kp) {
        // Standard conversion table (approximate)
        double[] table = {0, 3, 7, 15, 27, 48, 80, 132, 208, 400};
        int idx = (int) Math.min(9, Math.max(0, kp));
        return table[idx];
    }
}
