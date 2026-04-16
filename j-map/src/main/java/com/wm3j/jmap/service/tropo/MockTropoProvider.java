package com.wm3j.jmap.service.tropo;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

/** Mock tropo provider */
public class MockTropoProvider extends AbstractDataProvider<TropoOverlay>
        implements TropoProvider {
    @Override
    protected TropoOverlay doFetch() throws DataProviderException {
        return new TropoOverlay(new byte[0]);
    }
}
