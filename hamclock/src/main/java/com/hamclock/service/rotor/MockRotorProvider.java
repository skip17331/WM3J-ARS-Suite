package com.hamclock.service.rotor;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

/**
 * Mock rotor provider that slowly sweeps the antenna around.
 * Returns a disconnected RotorData to indicate no live rotor is configured.
 */
public class MockRotorProvider extends AbstractDataProvider<RotorData>
        implements RotorProvider {

    private double currentAzimuth = 90.0;
    private final double sweepRate = 0.5; // degrees per call

    @Override
    protected RotorData doFetch() throws DataProviderException {
        // Slowly sweep for visual demo
        currentAzimuth = (currentAzimuth + sweepRate) % 360.0;

        RotorData data = new RotorData(currentAzimuth, 0.0);
        data.setConnected(false); // Indicate this is mock/no rotor
        data.setInMotion(false);
        return data;
    }
}
