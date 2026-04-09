package com.hamclock.service.aurora;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches the NOAA SWPC Ovation aurora forecast PNG overlay.
 *
 * Source: https://services.swpc.noaa.gov/images/aurora-forecast-northern.png
 */
public class NoaaOvationProvider extends AbstractDataProvider<AuroraOverlay>
        implements AuroraProvider {

    private static final String NORTHERN_URL =
        "https://services.swpc.noaa.gov/images/aurora-forecast-northern.png";
    private static final int TIMEOUT_MS = 15_000;

    @Override
    protected AuroraOverlay doFetch() throws DataProviderException {
        try {
            URL url = new URL(NORTHERN_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HamClockClone/1.0");

            if (conn.getResponseCode() != 200) {
                throw new DataProviderException("NOAA Ovation HTTP " + conn.getResponseCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (InputStream is = conn.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                AuroraOverlay overlay = new AuroraOverlay(imageBytes);
                overlay.setSourceUrl(NORTHERN_URL);
                log.info("Loaded {} bytes of aurora overlay from NOAA", imageBytes.length);
                return overlay;
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("NOAA Ovation fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
