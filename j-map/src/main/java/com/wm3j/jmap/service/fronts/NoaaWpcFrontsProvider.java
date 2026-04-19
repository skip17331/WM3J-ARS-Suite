package com.wm3j.jmap.service.fronts;

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
import java.util.List;

/**
 * Fetches Day-1 surface analysis fronts from NOAA's national forecast weather chart service.
 * No API key required.
 * Endpoint: mapservices.weather.noaa.gov/vector/.../natl_fcst_wx_chart/MapServer/2/query
 */
public class NoaaWpcFrontsProvider extends AbstractDataProvider<FrontsData>
        implements FrontsProvider {

    private static final String URL =
        "https://mapservices.weather.noaa.gov/vector/rest/services/outlooks/" +
        "natl_fcst_wx_chart/MapServer/2/query" +
        "?where=1%3D1&outFields=feat&f=geojson";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient   HTTP   = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    @Override
    protected FrontsData doFetch() throws DataProviderException {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("User-Agent", "J-Map/1.0")
                .GET()
                .build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                throw new DataProviderException("NOAA WPC fronts HTTP " + resp.statusCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            JsonNode root     = MAPPER.readTree(resp.body());
            JsonNode features = root.path("features");
            List<FrontsData.Front> fronts = new ArrayList<>();

            for (JsonNode feat : features) {
                String featStr = feat.path("properties").path("feat").asText("");
                FrontsData.FrontType type = FrontsData.FrontType.fromFeat(featStr);

                JsonNode geom = feat.path("geometry");
                String geomType = geom.path("type").asText();
                JsonNode coords = geom.path("coordinates");

                // Support both LineString and MultiLineString
                if ("LineString".equals(geomType)) {
                    fronts.add(new FrontsData.Front(type, parseLineString(coords)));
                } else if ("MultiLineString".equals(geomType)) {
                    for (JsonNode line : coords) {
                        fronts.add(new FrontsData.Front(type, parseLineString(line)));
                    }
                }
            }

            log.debug("WPC fronts loaded: {} segments", fronts.size());
            return new FrontsData(fronts, Instant.now());

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("NOAA WPC fronts fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private List<double[]> parseLineString(JsonNode coords) {
        List<double[]> pts = new ArrayList<>(coords.size());
        for (JsonNode pt : coords) {
            if (pt.size() >= 2) {
                pts.add(new double[]{ pt.get(0).asDouble(), pt.get(1).asDouble() });
            }
        }
        return pts;
    }
}
