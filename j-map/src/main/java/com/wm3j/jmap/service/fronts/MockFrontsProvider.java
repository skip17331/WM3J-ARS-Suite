package com.wm3j.jmap.service.fronts;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.util.List;

public class MockFrontsProvider extends AbstractDataProvider<FrontsData>
        implements FrontsProvider {

    @Override
    protected FrontsData doFetch() throws DataProviderException {
        // Cold front across northern US
        List<double[]> cold = List.of(
            new double[]{-120, 48}, new double[]{-110, 44}, new double[]{-100, 42},
            new double[]{-90,  40}, new double[]{-80,  38}, new double[]{-70,  37}
        );
        // Warm front in eastern US
        List<double[]> warm = List.of(
            new double[]{-90, 35}, new double[]{-80, 36}, new double[]{-70, 38}
        );
        // Stationary front in west
        List<double[]> stationary = List.of(
            new double[]{-125, 42}, new double[]{-120, 40}, new double[]{-115, 38}
        );
        return new FrontsData(List.of(
            new FrontsData.Front(FrontsData.FrontType.COLD,       cold),
            new FrontsData.Front(FrontsData.FrontType.WARM,       warm),
            new FrontsData.Front(FrontsData.FrontType.STATIONARY, stationary)
        ), Instant.now());
    }
}
