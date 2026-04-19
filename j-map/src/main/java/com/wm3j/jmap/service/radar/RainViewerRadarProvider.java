package com.wm3j.jmap.service.radar;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import com.wm3j.jmap.service.tiles.TileCompositor;
import javafx.scene.image.WritableImage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Fetches the latest NEXRAD radar composite from RainViewer's tile CDN.
 *
 * Step 1 — GET api.rainviewer.com/public/weather-maps.json to find the
 *           most recent radar frame path.
 * Step 2 — Fetch z=2 tiles (16 tiles) and composite/reproject to
 *           equirectangular via TileCompositor.
 *
 * Tile URL: https://tilecache.rainviewer.com{path}/{z}/{x}/{y}/256/2_1.png
 *   color scheme 2 = NOAA-style, smooth=1
 */
public class RainViewerRadarProvider extends AbstractDataProvider<RadarOverlay>
        implements RadarProvider {

    private static final String API_URL =
        "https://api.rainviewer.com/public/weather-maps.json";
    private static final String TILE_BASE =
        "https://tilecache.rainviewer.com";

    private static final int ZOOM = 2;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Override
    protected RadarOverlay doFetch() throws DataProviderException {
        String path = fetchLatestRadarPath();
        // Tile URL: {base}{path}/{z}/{x}/{y}/256/2_1.png
        String urlTemplate = TILE_BASE + path + "/{z}/{x}/{y}/256/2_1.png";
        log.debug("Fetching radar tiles: {}", urlTemplate.replace("{z}/{x}/{y}", "z/x/y"));

        WritableImage img = TileCompositor.fetchEquirectangular(urlTemplate, ZOOM);
        if (img == null) {
            throw new DataProviderException("RainViewer radar: no tiles loaded",
                DataProviderException.ErrorCode.NETWORK_ERROR);
        }
        return new RadarOverlay(img, Instant.now());
    }

    /** Returns the path portion of the most recent radar frame, e.g. "/v2/radar/1234567890" */
    private String fetchLatestRadarPath() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("User-Agent", "J-Map/1.0")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("RainViewer API HTTP " + resp.statusCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }
            JsonNode root = MAPPER.readTree(resp.body());

            // Try nowcast[0] first (most current), then past[-1]
            JsonNode nowcast = root.path("radar").path("nowcast");
            if (nowcast.isArray() && nowcast.size() > 0) {
                String path = nowcast.get(0).path("path").asText("");
                if (!path.isBlank()) return path;
            }
            JsonNode past = root.path("radar").path("past");
            if (past.isArray() && past.size() > 0) {
                String path = past.get(past.size() - 1).path("path").asText("");
                if (!path.isBlank()) return path;
            }
            throw new DataProviderException("No radar path in weather-maps.json",
                DataProviderException.ErrorCode.PARSE_ERROR);
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("RainViewer API fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
