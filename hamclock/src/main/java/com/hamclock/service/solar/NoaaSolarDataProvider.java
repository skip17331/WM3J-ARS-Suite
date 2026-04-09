package com.hamclock.service.solar;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches solar and geomagnetic data from NOAA SWPC.
 *
 * Primary sources:
 *   - https://services.swpc.noaa.gov/products/summary/solar-geophysical-activity.json
 *   - https://services.swpc.noaa.gov/products/summary/10cm-flux.json
 *   - https://services.swpc.noaa.gov/products/summary/planetary-k-index.json
 */
public class NoaaSolarDataProvider extends AbstractDataProvider<SolarData>
        implements SolarDataProvider {

    private static final String SFI_URL =
        "https://services.swpc.noaa.gov/products/summary/10cm-flux.json";
    private static final String KP_URL =
        "https://services.swpc.noaa.gov/products/summary/planetary-k-index.json";
    private static final String ACTIVITY_URL =
        "https://services.swpc.noaa.gov/products/summary/solar-geophysical-activity.json";

    private static final int TIMEOUT_MS = 10_000;

    @Override
    protected SolarData doFetch() throws DataProviderException {
        SolarData data = new SolarData();

        try {
            // Fetch SFI
            String sfiJson = fetchUrl(SFI_URL);
            data.setSfi(parseDouble(sfiJson, "\"Flux\""));

            // Fetch Kp
            String kpJson = fetchUrl(KP_URL);
            data.setKp(parseDouble(kpJson, "\"Kp\""));

            // Fetch geomagnetic activity summary
            String activityJson = fetchUrl(ACTIVITY_URL);
            data.setAIndex(parseInt(activityJson, "\"AIndex\""));
            data.setSunspotNumber(parseInt(activityJson, "\"SunspotNumber\""));

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("NOAA fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }

        data.setXrayClass("C1.0");  // Default if not separately fetched
        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    private String fetchUrl(String urlStr) throws DataProviderException {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HamClockClone/1.0");

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new DataProviderException("HTTP " + code + " from " + urlStr,
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Failed to fetch " + urlStr,
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private double parseDouble(String json, String key) {
        Pattern p = Pattern.compile(Pattern.quote(key) + "\\s*:\\s*\"?([\\d.]+)\"?");
        Matcher m = p.matcher(json);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        log.warn("Could not parse {} from response", key);
        return 0.0;
    }

    private int parseInt(String json, String key) {
        return (int) parseDouble(json, key);
    }
}
