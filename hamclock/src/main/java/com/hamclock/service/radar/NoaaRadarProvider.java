package com.hamclock.service.radar;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

/**
 * Fetches the NOAA national radar mosaic composite PNG.
 * Image is a full-globe equirectangular composite from Iowa Environmental Mesonet.
 */
public class NoaaRadarProvider extends AbstractDataProvider<RadarOverlay>
        implements RadarProvider {

    private static final Logger log = LoggerFactory.getLogger(NoaaRadarProvider.class);

    // Iowa Environmental Mesonet CONUS radar composite (publicly available)
    private static final String RADAR_URL =
        "https://mesonet.agron.iastate.edu/cache/tile.py/1.0.0/nexrad-n0q-900913/0/0/0.png";

    private final HttpClient http = HttpClient.newHttpClient();

    @Override
    protected RadarOverlay doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(RADAR_URL))
                .header("User-Agent", "HamClock/1.0")
                .build();

            HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("Radar HTTP " + resp.statusCode(), DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            log.debug("Fetched radar composite: {} bytes", resp.body().length);
            return new RadarOverlay(resp.body(), Instant.now());

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Radar fetch failed: " + e.getMessage(), DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
