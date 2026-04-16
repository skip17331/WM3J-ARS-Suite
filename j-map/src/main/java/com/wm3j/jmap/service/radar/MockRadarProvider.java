package com.wm3j.jmap.service.radar;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.util.Random;

/**
 * Mock radar overlay — simulates precipitation cells scattered mid-latitudes.
 */
public class MockRadarProvider extends AbstractDataProvider<RadarOverlay>
        implements RadarProvider {

    private static final int LON_STEPS = 360;
    private static final int LAT_STEPS = 180;
    private final Random random = new Random(42);

    @Override
    protected RadarOverlay doFetch() throws DataProviderException {
        double[][] intensity = new double[LON_STEPS][LAT_STEPS];

        // Sprinkle a few precipitation "cells" at random mid-latitude locations
        int cells = 8 + random.nextInt(8);
        for (int c = 0; c < cells; c++) {
            double centerLon = random.nextDouble() * 360 - 180;
            double centerLat = (random.nextDouble() * 80) - 40;  // ±40°
            double radius    = 5 + random.nextDouble() * 15;      // degrees
            double peakVal   = 0.4 + random.nextDouble() * 0.6;

            int lonCenter = (int) (centerLon + 180);
            int latCenter = (int) (90 - centerLat);

            for (int di = -(int) radius; di <= radius; di++) {
                for (int dj = -(int) radius; dj <= radius; dj++) {
                    int li = (lonCenter + di + LON_STEPS) % LON_STEPS;
                    int lj = Math.max(0, Math.min(LAT_STEPS - 1, latCenter + dj));
                    double dist = Math.sqrt(di * di + dj * dj);
                    if (dist < radius) {
                        double val = peakVal * (1 - dist / radius);
                        intensity[li][lj] = Math.max(intensity[li][lj], val);
                    }
                }
            }
        }

        return new RadarOverlay(intensity, Instant.now());
    }
}
