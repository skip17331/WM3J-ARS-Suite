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
        data.setProtonFlux(0.1 + random.nextDouble() * 2.0);
        data.setXrayClass(randomXrayClass());
        data.setObservationTime(Instant.now());
        data.setFresh(true);
        return data;
    }

    private String randomXrayClass() {
        String[] classes = {"A1.2", "A5.0", "B1.5", "B3.0", "B7.2", "C1.3", "C4.5", "C9.0", "M1.2"};
        return classes[random.nextInt(classes.length)];
    }
}
