package com.hamclock.service.propagation;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;
import com.hamclock.service.propagation.PropagationData.BandCondition;

import java.util.Random;

/**
 * Mock propagation provider returning realistic synthetic band conditions.
 * Used in development and when no live data source is configured.
 */
public class MockPropagationProvider extends AbstractDataProvider<PropagationData>
        implements PropagationDataProvider {

    private final Random random = new Random(77L);

    // Frequency range of each band (MHz) - used for FOT/MUF logic
    private static final double[] BAND_FREQS = {3.5, 5.3, 7.0, 10.1, 14.0, 18.1, 21.0, 24.9, 28.0, 50.0};

    @Override
    protected PropagationData doFetch() throws DataProviderException {
        PropagationData data = new PropagationData();

        double sfi = 80 + random.nextDouble() * 100;
        double kp  = random.nextDouble() * 4.0;

        data.setSfi(sfi);
        data.setKp(kp);

        // MUF rises with SFI, falls with Kp
        double muf = 8.0 + (sfi - 70) * 0.4 - kp * 1.5;
        muf = Math.max(3.0, Math.min(35.0, muf));
        data.setMuf(muf);
        data.setFot(muf * 0.85);
        data.setLuf(3.5);

        // Assign band conditions based on MUF and Kp
        data.setBandCondition("80m",  classifyBand(3.5,  muf, kp, sfi));
        data.setBandCondition("60m",  classifyBand(5.3,  muf, kp, sfi));
        data.setBandCondition("40m",  classifyBand(7.0,  muf, kp, sfi));
        data.setBandCondition("30m",  classifyBand(10.1, muf, kp, sfi));
        data.setBandCondition("20m",  classifyBand(14.0, muf, kp, sfi));
        data.setBandCondition("17m",  classifyBand(18.1, muf, kp, sfi));
        data.setBandCondition("15m",  classifyBand(21.0, muf, kp, sfi));
        data.setBandCondition("12m",  classifyBand(24.9, muf, kp, sfi));
        data.setBandCondition("10m",  classifyBand(28.0, muf, kp, sfi));
        data.setBandCondition("6m",   classifyBand(50.0, muf, kp, sfi));

        return data;
    }

    /**
     * Determine band condition based on whether this band's frequency is
     * within usable range relative to MUF/LUF, adjusted for geomagnetic activity.
     */
    private BandCondition classifyBand(double bandFreq, double muf, double kp, double sfi) {
        // Above MUF = closed
        if (bandFreq > muf * 1.05) return BandCondition.CLOSED;

        // Near MUF = poor
        double ratio = bandFreq / muf;
        if (ratio > 0.9) return degradeByKp(BandCondition.POOR, kp);

        // In the sweet spot (FOT range)
        if (ratio > 0.65) return degradeByKp(BandCondition.EXCELLENT, kp);

        // Well below FOT - low bands affected by absorption
        if (ratio > 0.4) return degradeByKp(BandCondition.GOOD, kp);

        // Very low bands - noise/absorption dominated
        if (sfi > 120) return degradeByKp(BandCondition.FAIR, kp);
        return degradeByKp(BandCondition.POOR, kp);
    }

    private BandCondition degradeByKp(BandCondition base, double kp) {
        int ord = base.ordinal();
        if (kp >= 6) ord = Math.min(ord + 3, BandCondition.values().length - 1);
        else if (kp >= 4) ord = Math.min(ord + 2, BandCondition.values().length - 1);
        else if (kp >= 3) ord = Math.min(ord + 1, BandCondition.values().length - 1);
        return BandCondition.values()[ord];
    }
}
