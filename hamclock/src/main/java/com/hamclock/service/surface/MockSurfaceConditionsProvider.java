package com.hamclock.service.surface;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.time.Instant;

/**
 * Mock surface conditions — realistic temperature gradient from equator to poles.
 */
public class MockSurfaceConditionsProvider extends AbstractDataProvider<SurfaceConditions>
        implements SurfaceConditionsProvider {

    private static final int LON_STEPS = 180;
    private static final int LAT_STEPS = 90;

    @Override
    protected SurfaceConditions doFetch() throws DataProviderException {
        double[][] temp = new double[LON_STEPS][LAT_STEPS];
        double[][] pres = new double[LON_STEPS][LAT_STEPS];

        for (int li = 0; li < LON_STEPS; li++) {
            double lon = li * 2.0 - 180;  // -180 to +178
            for (int lj = 0; lj < LAT_STEPS; lj++) {
                double lat = 90.0 - lj * 2.0;  // +90 to -88

                // Temperature: warm equator (30°C), cold poles (-40°C)
                // plus a slight seasonal/longitudinal variation
                double latFactor = Math.cos(Math.toRadians(lat));
                double lonWave   = Math.sin(Math.toRadians(lon * 0.5)) * 5;
                temp[li][lj] = -40 + 70 * latFactor + lonWave;

                // Pressure: 1013 hPa average, subtropical highs ~1020, equatorial trough ~1008
                double latPres = 1013 + 8 * Math.cos(Math.toRadians(lat * 2));
                double lonPres = Math.sin(Math.toRadians(lon * 0.3)) * 4;
                pres[li][lj] = latPres + lonPres;
            }
        }

        return new SurfaceConditions(temp, pres, SurfaceConditions.DisplayMode.TEMPERATURE, Instant.now());
    }
}
