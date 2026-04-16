package com.wm3j.jmap.service.weather;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches global cloud cover tile from OpenWeatherMap.
 * Uses the cloud cover tile layer (no key required for basic PNG tiles).
 */
public class OpenWeatherMapProvider extends AbstractDataProvider<WeatherOverlay>
        implements WeatherProvider {

    private final String apiKey;
    private static final int TIMEOUT_MS = 15_000;

    public OpenWeatherMapProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    protected WeatherOverlay doFetch() throws DataProviderException {
        // OpenWeatherMap tile URL - cloud layer at zoom level 1 (whole world in 4 tiles)
        // https://tile.openweathermap.org/map/clouds_new/{z}/{x}/{y}.png?appid={API_KEY}
        String urlStr = String.format(
            "https://tile.openweathermap.org/map/clouds_new/1/0/0.png?appid=%s", apiKey);

        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "J-Map/1.0");

            if (conn.getResponseCode() != 200) {
                throw new DataProviderException("OWM HTTP " + conn.getResponseCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (InputStream is = conn.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                WeatherOverlay overlay = new WeatherOverlay(imageBytes);
                overlay.setSourceUrl(urlStr);
                return overlay;
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("OWM fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
