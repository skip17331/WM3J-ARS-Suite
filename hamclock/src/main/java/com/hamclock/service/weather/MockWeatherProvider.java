package com.hamclock.service.weather;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

/** Mock weather provider - returns placeholder overlay */
public class MockWeatherProvider extends AbstractDataProvider<WeatherOverlay>
        implements WeatherProvider {

    @Override
    protected WeatherOverlay doFetch() throws DataProviderException {
        // Return empty overlay - real implementation would generate a synthetic image
        return new WeatherOverlay(new byte[0]);
    }
}
