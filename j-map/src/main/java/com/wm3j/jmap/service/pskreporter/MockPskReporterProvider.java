package com.wm3j.jmap.service.pskreporter;

import com.wm3j.jmap.service.AbstractDataProvider;
import com.wm3j.jmap.service.DataProviderException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Returns 20 plausible synthetic PSK Reporter spots for development.
 * Fixed seed for reproducibility.
 */
public class MockPskReporterProvider extends AbstractDataProvider<PskReporterData>
        implements PskReporterProvider {

    private static final Random RAND = new Random(42L);

    // [lat, lon] of plausible North America / Europe / Japan stations
    private static final double[][] LOCS = {
        { 40.7, -74.0},  // New York
        { 51.5,  -0.1},  // London
        { 48.8,   2.3},  // Paris
        { 52.5,  13.4},  // Berlin
        { 37.8, -122.4}, // San Francisco
        { 35.7, 139.7},  // Tokyo
        { 43.7,  -79.4}, // Toronto
        { 55.7,  37.6},  // Moscow
        {-33.9,  18.4},  // Cape Town
        { 34.0, -118.2}, // Los Angeles
    };

    private static final String[] MODES = {"FT8", "FT8", "FT8", "FT4", "PSK31", "CW", "FT8"};

    // Frequencies (Hz) for common bands
    private static final long[] FREQS = {
        14_074_000L,  // 20m FT8
         7_074_000L,  // 40m FT8
        21_074_000L,  // 15m FT8
        18_100_000L,  // 17m FT8
        28_074_000L,  // 10m FT8
        14_070_000L,  // 20m PSK31
         7_030_000L,  // 40m CW
    };

    @Override
    protected PskReporterData doFetch() throws DataProviderException {
        Random r = new Random(42L);
        List<PskSpot> spots = new ArrayList<>();
        long nowSec = System.currentTimeMillis() / 1000;

        for (int i = 0; i < 20; i++) {
            PskSpot spot = new PskSpot();
            double[] sender   = LOCS[r.nextInt(LOCS.length)];
            double[] receiver = LOCS[r.nextInt(LOCS.length)];

            spot.setSenderCallsign("W" + (i + 1) + "AW");
            spot.setReceiverCallsign("K" + (i + 1) + "XY");
            spot.setSenderLat(sender[0]   + (r.nextDouble() - 0.5) * 3);
            spot.setSenderLon(sender[1]   + (r.nextDouble() - 0.5) * 3);
            spot.setReceiverLat(receiver[0] + (r.nextDouble() - 0.5) * 3);
            spot.setReceiverLon(receiver[1] + (r.nextDouble() - 0.5) * 3);
            spot.setFrequencyHz(FREQS[r.nextInt(FREQS.length)]);
            spot.setMode(MODES[r.nextInt(MODES.length)]);
            spot.setSnr(-10 + r.nextInt(25));
            // Age between 0 and 29 minutes
            spot.setFlowStartSeconds(nowSec - r.nextInt(29 * 60));

            spots.add(spot);
        }

        PskReporterData data = new PskReporterData();
        data.setSpots(spots);
        data.setFetchTime(Instant.now());
        return data;
    }
}
