package com.wm3j.jmap.service.dx;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches DX spots from the DXHeat JSON API.
 *
 * Endpoint: https://dxheat.com/dxc/
 * Returns recent DX spots in JSON format.
 */
public class HttpDxSpotProvider extends AbstractDataProvider<List<DxSpot>>
        implements DxSpotProvider {

    private static final String API_URL = "https://dxheat.com/dxc/";
    private static final int TIMEOUT_MS = 10_000;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected List<DxSpot> doFetch() throws DataProviderException {
        String json = fetchJson();
        return parseSpots(json);
    }

    private String fetchJson() throws DataProviderException {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "J-Map/1.0");
            conn.setRequestProperty("Accept", "application/json");

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new DataProviderException("HTTP " + code,
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
            throw new DataProviderException("DXHeat fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private List<DxSpot> parseSpots(String json) throws DataProviderException {
        List<DxSpot> spots = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode spotsArray = root.isArray() ? root : root.path("spots");

            for (JsonNode node : spotsArray) {
                try {
                    DxSpot spot = new DxSpot();
                    spot.setSpotter(node.path("spotter").asText("?"));
                    spot.setDxCallsign(node.path("dx").asText("?"));
                    spot.setFrequencyKhz(node.path("freq").asDouble(14000));
                    spot.setComment(node.path("comment").asText(""));

                    // Parse timestamp - DXHeat uses epoch seconds or ISO string
                    long epochSec = node.path("time").asLong(0);
                    if (epochSec > 0) {
                        spot.setTimestamp(Instant.ofEpochSecond(epochSec));
                    } else {
                        spot.setTimestamp(Instant.now());
                    }

                    spots.add(spot);
                } catch (Exception e) {
                    log.warn("Skipping malformed spot: {}", e.getMessage());
                }
            }

            log.info("Loaded {} DX spots from DXHeat", spots.size());
        } catch (Exception e) {
            throw new DataProviderException("DXHeat parse failed: " + e.getMessage(),
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
        return spots;
    }
}
