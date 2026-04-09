package com.hamclock.service.aurora;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.util.Random;

/**
 * Mock aurora overlay using a synthetic intensity grid.
 * Real aurora concentrates at high latitudes in oval patterns.
 */
public class MockAuroraProvider extends AbstractDataProvider<AuroraOverlay>
        implements AuroraProvider {

    private final Random random = new Random();
    private static final int LON_STEPS = 360;
    private static final int LAT_STEPS = 180;

    @Override
    protected AuroraOverlay doFetch() throws DataProviderException {
        double[][] intensity = new double[LON_STEPS][LAT_STEPS];
        double activity = 0.3 + random.nextDouble() * 0.5; // 0.3-0.8 activity level

        for (int lonIdx = 0; lonIdx < LON_STEPS; lonIdx++) {
            double lon = lonIdx - 180.0;
            for (int latIdx = 0; latIdx < LAT_STEPS; latIdx++) {
                double lat = 90.0 - latIdx;

                // Aurora oval is strongest around 65-75° latitude
                double absLat = Math.abs(lat);
                double ovalCenter = 68.0;
                double ovalWidth = 8.0;
                double latFactor = Math.exp(-Math.pow(absLat - ovalCenter, 2) / (2 * ovalWidth * ovalWidth));

                // Add longitude variation (oval is not perfectly symmetric)
                double lonFactor = 0.5 + 0.5 * Math.cos(Math.toRadians(lon * 0.5));

                // Add noise
                double noise = (random.nextDouble() - 0.5) * 0.2;

                intensity[lonIdx][latIdx] = Math.max(0, Math.min(1.0,
                    latFactor * lonFactor * activity + noise));
            }
        }

        return new AuroraOverlay(intensity);
    }
}
