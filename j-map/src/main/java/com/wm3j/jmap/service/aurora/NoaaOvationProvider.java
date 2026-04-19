package com.wm3j.jmap.service.aurora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Fetches the NOAA SWPC Ovation aurora forecast as a JSON intensity grid.
 *
 * Source: services.swpc.noaa.gov/json/ovation_aurora_latest.json
 *
 * The JSON contains a "coordinates" array of [lon, lat, aurora_value] triples
 * where aurora_value is 0-100. We map this into a 360×181 intensity grid
 * (lon -180..179, lat -90..90) normalised to 0.0-1.0.
 */
public class NoaaOvationProvider extends AbstractDataProvider<AuroraOverlay>
        implements AuroraProvider {

    private static final String URL = "https://services.swpc.noaa.gov/json/ovation_aurora_latest.json";

    private static final int GRID_LON = 360;
    private static final int GRID_LAT = 181; // -90 to +90

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    @Override
    protected AuroraOverlay doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("User-Agent", "J-Map/1.0 (ham radio ARS Suite)")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("NOAA Ovation HTTP " + resp.statusCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode coords = root.path("coordinates");
            if (!coords.isArray() || coords.size() == 0) {
                throw new DataProviderException("Empty coordinates in ovation response",
                    DataProviderException.ErrorCode.PARSE_ERROR);
            }

            // Build intensity grid [lon_idx][lat_idx]
            double[][] grid = new double[GRID_LON][GRID_LAT];

            for (JsonNode pt : coords) {
                if (!pt.isArray() || pt.size() < 3) continue;
                double lon   = pt.get(0).asDouble();
                double lat   = pt.get(1).asDouble();
                double value = pt.get(2).asDouble(0);
                if (value <= 0) continue;

                // Map lon -180..179 → index 0..359
                int lonIdx = (int) Math.round(lon + 180) % GRID_LON;
                // Map lat -90..90 → index 0..180
                int latIdx = (int) Math.round(lat + 90);
                if (lonIdx < 0 || lonIdx >= GRID_LON || latIdx < 0 || latIdx >= GRID_LAT) continue;

                // NOAA values are 0-100; normalise to 0.0-1.0
                grid[lonIdx][latIdx] = Math.max(grid[lonIdx][latIdx], value / 100.0);
            }

            String forecastTime = root.path("Forecast Time").asText("");
            AuroraOverlay overlay = new AuroraOverlay(grid);
            overlay.setSourceUrl(URL + " (" + forecastTime + ")");
            log.info("Loaded Ovation aurora grid: {} coordinate points, forecast time={}",
                coords.size(), forecastTime);
            return overlay;

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("NOAA Ovation JSON fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
