package com.hamclock.service.tropo;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Fetches the Hepburn global tropo ducting forecast map.
 * Source: http://www.dxinfocentre.com/tropo_eur.html (image)
 */
public class HepburnTropoProvider extends AbstractDataProvider<TropoOverlay>
        implements TropoProvider {

    // Hepburn world tropo forecast image
    private static final String TROPO_URL =
        "http://www.dxinfocentre.com/tropo_wld.png";
    private static final int TIMEOUT_MS = 15_000;

    @Override
    protected TropoOverlay doFetch() throws DataProviderException {
        try {
            URL url = new URL(TROPO_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setRequestProperty("User-Agent", "HamClockClone/1.0");

            if (conn.getResponseCode() != 200) {
                throw new DataProviderException("Hepburn HTTP " + conn.getResponseCode(),
                    DataProviderException.ErrorCode.NETWORK_ERROR);
            }

            try (InputStream is = conn.getInputStream()) {
                byte[] imageBytes = is.readAllBytes();
                TropoOverlay overlay = new TropoOverlay(imageBytes);
                overlay.setSourceUrl(TROPO_URL);
                log.info("Loaded {} bytes tropo overlay", imageBytes.length);
                return overlay;
            }
        } catch (DataProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DataProviderException("Hepburn fetch failed: " + e.getMessage(),
                DataProviderException.ErrorCode.NETWORK_ERROR, e);
        }
    }
}
