package com.hamclock.service.lightning;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Mock lightning data — simulates strike clusters in tropical/mid-latitude storm zones.
 */
public class MockLightningProvider extends AbstractDataProvider<LightningData>
        implements LightningProvider {

    private static final int LON_STEPS = 180;
    private static final int LAT_STEPS = 90;
    private final Random random = new Random();

    @Override
    protected LightningData doFetch() throws DataProviderException {
        List<LightningData.Strike> strikes = new ArrayList<>();
        double[][] density = new double[LON_STEPS][LAT_STEPS];

        // Tropical storm belt and mid-latitude thunderstorm clusters
        double[][] centers = {
            {5, -75},   // Amazon
            {8, 20},    // Congo basin
            {15, 80},   // Indian subcontinent
            {35, -95},  // US Great Plains
            {50, 15},   // European low
        };

        for (double[] center : centers) {
            int count = 30 + random.nextInt(60);
            for (int i = 0; i < count; i++) {
                double lat = center[0] + (random.nextGaussian() * 5);
                double lon = center[1] + (random.nextGaussian() * 8);
                lat = Math.max(-90, Math.min(90, lat));
                lon = Math.max(-180, Math.min(180, lon));
                strikes.add(new LightningData.Strike(lat, lon, Instant.now().minusSeconds(random.nextInt(300))));

                int lonIdx = (int) Math.min(LON_STEPS - 1, (lon + 180) / 360.0 * LON_STEPS);
                int latIdx = (int) Math.min(LAT_STEPS - 1, (90 - lat) / 180.0 * LAT_STEPS);
                density[lonIdx][latIdx] += 1.0;
            }
        }

        // Normalize density to 0–1
        double max = 1;
        for (double[] col : density) for (double v : col) if (v > max) max = v;
        for (int i = 0; i < LON_STEPS; i++) for (int j = 0; j < LAT_STEPS; j++) density[i][j] /= max;

        return new LightningData(strikes, density, Instant.now());
    }
}
