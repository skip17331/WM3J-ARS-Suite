package com.hamclock.service.rotor;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches rotor position from an IP-connected Arduino via HTTP.
 *
 * Expected Arduino response format (JSON):
 *   {"azimuth": 135.5, "elevation": 0.0, "moving": false}
 * Or plain text:
 *   "135.5"  (azimuth only)
 *
 * Arduino sketch should serve a simple HTTP endpoint on port 80 or configurable.
 */
public class ArduinoRotorHttpProvider extends AbstractDataProvider<RotorData>
        implements RotorProvider {

    private final String host;
    private final int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final int TIMEOUT_MS = 3_000;

    public ArduinoRotorHttpProvider(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    protected RotorData doFetch() throws DataProviderException {
        String urlStr = String.format("http://%s:%d/rotor", host, port);
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HamClockClone/1.0");

            int code = conn.getResponseCode();
            if (code != 200) {
                throw new DataProviderException("Arduino HTTP " + code,
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line.trim());
                return parseResponse(sb.toString());
            }

        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Arduino rotor unreachable at " + urlStr,
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }

    private RotorData parseResponse(String response) throws DataProviderException {
        response = response.trim();
        try {
            if (response.startsWith("{")) {
                // JSON format
                JsonNode node = mapper.readTree(response);
                double az = node.path("azimuth").asDouble(0);
                double el = node.path("elevation").asDouble(0);
                boolean moving = node.path("moving").asBoolean(false);

                RotorData data = new RotorData(az, el);
                data.setInMotion(moving);
                data.setConnected(true);
                return data;
            } else {
                // Plain text - just azimuth
                double az = Double.parseDouble(response);
                RotorData data = new RotorData(az);
                data.setConnected(true);
                return data;
            }
        } catch (Exception e) {
            throw new DataProviderException("Failed to parse rotor response: " + response,
                DataProviderException.ErrorCode.PARSE_ERROR, e);
        }
    }
}
