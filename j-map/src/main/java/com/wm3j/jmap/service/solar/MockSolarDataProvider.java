package com.wm3j.jmap.service.solar;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.util.Random;

/**
 * Mock solar data provider that returns realistic-looking synthetic data.
 * Used when no API key is configured or in development/testing.
 */
public class MockSolarDataProvider extends AbstractDataProvider<SolarData>
        implements SolarDataProvider {

    private final Random random = new Random(42L);

    @Override
    protected SolarData doFetch() throws DataProviderException {
        // Return plausible solar data values
        SolarData data = new SolarData();
        data.setSfi(80 + random.nextDouble() * 120);          // 80-200 sfu
        data.setKp(random.nextDouble() * 5.0);                // 0-5 Kp
        data.setAIndex(random.nextInt(30));                    // 0-30
        data.setSunspotNumber(50 + random.nextInt(150));       // 50-200
        data.setSolarWindSpeed(350 + random.nextDouble() * 250); // 350-600 km/s
        data.setSolarWindDensity(3 + random.nextDouble() * 10);  // 3-13 p/cm³
        data.setBtField(2 + random.nextDouble() * 10);           // 2-12 nT
        data.setBzField(-8 + random.nextDouble() * 16);          // -8 to +8 nT
        data.setProtonFlux(0.1 + random.nextDouble() * 2.0);
        data.setXrayFlux(randomXrayFlux());
        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    private double randomXrayFlux() {
        // Weighted toward A/B class (typical quiet sun)
        double[] fluxes = {1e-8, 3e-8, 8e-8, 2e-7, 5e-7, 1.5e-6, 4e-6, 1e-5, 3e-5};
        return fluxes[random.nextInt(fluxes.length)];
    }
}
