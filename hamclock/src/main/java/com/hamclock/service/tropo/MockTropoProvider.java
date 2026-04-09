package com.hamclock.service.tropo;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

/** Mock tropo provider */
public class MockTropoProvider extends AbstractDataProvider<TropoOverlay>
        implements TropoProvider {
    @Override
    protected TropoOverlay doFetch() throws DataProviderException {
        return new TropoOverlay(new byte[0]);
    }
}
