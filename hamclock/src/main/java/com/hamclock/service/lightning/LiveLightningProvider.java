package com.hamclock.service.lightning;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * Live lightning data from Blitzortung.org public feed.
 *
 * Blitzortung provides a publicly accessible JSON feed of recent strikes.
 * Data is coarsened to 1° grid for map rendering.
 */
public class LiveLightningProvider extends AbstractDataProvider<LightningData>
        implements LightningProvider {

    private static final Logger log = LoggerFactory.getLogger(LiveLightningProvider.class);

    // Blitzortung public JSON archive (recent 10-minute file)
    private static final String URL =
        "https://data.blitzortung.org/Data_1/strikes.json.gz";

    private static final int LON_STEPS = 180;
    private static final int LAT_STEPS = 90;

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected LightningData doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("User-Agent", "HamClock/1.0")
                .build();

            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("Lightning HTTP " + resp.statusCode(), DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            return parseJson(resp.body());

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Lightning fetch failed: " + e.getMessage(), DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private LightningData parseJson(String body) {
        List<LightningData.Strike> strikes = new ArrayList<>();
        double[][] density = new double[LON_STEPS][LAT_STEPS];

        // Parse {"lat":12.345,"lon":-67.890,"time":...} entries
        Pattern p = Pattern.compile("\"lat\"\\s*:\\s*(-?[0-9.]+).*?\"lon\"\\s*:\\s*(-?[0-9.]+)");
        Matcher m = p.matcher(body);

        while (m.find()) {
            try {
                double lat = Double.parseDouble(m.group(1));
                double lon = Double.parseDouble(m.group(2));
                strikes.add(new LightningData.Strike(lat, lon, Instant.now()));

                int lonIdx = (int) Math.min(LON_STEPS - 1, (lon + 180) / 360.0 * LON_STEPS);
                int latIdx = (int) Math.min(LAT_STEPS - 1, (90 - lat) / 180.0 * LAT_STEPS);
                density[lonIdx][latIdx] += 1.0;
            } catch (NumberFormatException ignored) {}
        }

        // Normalize
        double max = 1;
        for (double[] col : density) for (double v : col) if (v > max) max = v;
        for (int i = 0; i < LON_STEPS; i++) for (int j = 0; j < LAT_STEPS; j++) density[i][j] /= max;

        log.debug("Parsed {} lightning strikes", strikes.size());
        return new LightningData(strikes, density, Instant.now());
    }
}
