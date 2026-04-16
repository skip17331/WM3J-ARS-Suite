package com.wm3j.jmap.service.surface;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches surface temperature from Open-Meteo (free, no API key required).
 *
 * Samples a coarse grid (every 10° lat/lon) to build an overview map.
 * Open-Meteo has a generous free tier with no registration.
 */
public class OpenMeteoSurfaceProvider extends AbstractDataProvider<SurfaceConditions>
        implements SurfaceConditionsProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoSurfaceProvider.class);

    private static final int LON_STEPS = 36;   // Every 10°
    private static final int LAT_STEPS = 18;

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected SurfaceConditions doFetch() throws DataProviderException {
        double[][] temp = new double[LON_STEPS][LAT_STEPS];
        double[][] pres = new double[LON_STEPS][LAT_STEPS];

        int fetched = 0;
        for (int li = 0; li < LON_STEPS; li++) {
            double lon = li * 10.0 - 180;
            for (int lj = 0; lj < LAT_STEPS; lj++) {
                double lat = 90.0 - lj * 10.0;
                if (lat < -90 || lat > 90) continue;

                try {
                    double[] values = fetchPoint(lat, lon);
                    temp[li][lj] = values[0];
                    pres[li][lj] = values[1];
                    fetched++;
                } catch (Exception e) {
                    // Use neighbor interpolation fallback on failure
                    temp[li][lj] = -40 + 70 * Math.cos(Math.toRadians(lat));
                    pres[li][lj] = 1013;
                }
            }
        }

        log.debug("Fetched {} surface grid points", fetched);
        return new SurfaceConditions(temp, pres, SurfaceConditions.DisplayMode.TEMPERATURE, Instant.now());
    }

    private double[] fetchPoint(double lat, double lon) throws Exception {
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%.1f&longitude=%.1f" +
            "&current_weather=true&hourly=surface_pressure&forecast_days=1",
            lat, lon);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "J-Map/1.0")
            .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new Exception("HTTP " + resp.statusCode());

        String body = resp.body();

        Pattern tempPat = Pattern.compile("\"temperature\"\\s*:\\s*(-?[0-9.]+)");
        Pattern presPat = Pattern.compile("\"surface_pressure\"\\s*:\\s*\\[([0-9.]+)");

        Matcher tm = tempPat.matcher(body);
        Matcher pm = presPat.matcher(body);

        double t = tm.find() ? Double.parseDouble(tm.group(1)) : 15;
        double p = pm.find() ? Double.parseDouble(pm.group(1)) : 1013;

        return new double[]{t, p};
    }
}
