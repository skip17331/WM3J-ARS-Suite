package com.hamclock.service.satellite;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Mock satellite provider — simulates several common amateur satellites
 * (ISS, AO-91, AO-92, SO-50, LILACSAT-2) on simplified circular orbits.
 */
public class MockSatelliteProvider extends AbstractDataProvider<SatelliteData>
        implements SatelliteProvider {

    // name, inclination (deg), period (min), phase offset (deg)
    private static final double[][] SATS = {
        /* ISS     */ {51.6, 92.9,   0},
        /* AO-91   */ {24.0, 97.3,  45},
        /* AO-92   */ {48.0, 96.8,  90},
        /* SO-50   */ {64.6, 98.0, 135},
        /* RS-44   */ {82.5, 115.7, 180},
    };
    private static final String[] NAMES    = {"ISS", "AO-91", "AO-92", "SO-50", "RS-44"};
    private static final String[] NORAD_IDS = {"25544", "43017", "43137", "27607", "44909"};
    private static final double[] ALT_KM   = {420, 680, 720, 830, 1325};

    @Override
    protected SatelliteData doFetch() throws DataProviderException {
        List<SatelliteData.SatPosition> positions = new ArrayList<>();
        double nowMin = System.currentTimeMillis() / 60_000.0;  // current time in minutes

        for (int s = 0; s < SATS.length; s++) {
            double inclDeg   = SATS[s][0];
            double periodMin = SATS[s][1];
            double phaseOff  = SATS[s][2];

            // Current mean anomaly (0–360°) based on elapsed time
            double anomaly = ((nowMin / periodMin) * 360 + phaseOff) % 360;

            // Simple circular orbit position
            double anomRad = Math.toRadians(anomaly);
            double inclRad = Math.toRadians(inclDeg);

            // Sub-satellite point (simplified, ignores RAAN)
            double lat = Math.toDegrees(Math.asin(Math.sin(inclRad) * Math.sin(anomRad)));
            double lon = Math.toDegrees(anomRad) - (nowMin % 1440) * 0.25; // Earth rotation
            lon = ((lon + 180) % 360) - 180;  // normalize to -180..180

            // Build a ground track: next 90 minutes, 1-minute steps
            List<double[]> track = new ArrayList<>();
            for (int t = 0; t < 90; t++) {
                double futAnomaly = ((anomaly + t * 360.0 / periodMin) % 360);
                double futRad = Math.toRadians(futAnomaly);
                double tLat = Math.toDegrees(Math.asin(Math.sin(inclRad) * Math.sin(futRad)));
                double tLon = lon + t * (360.0 / periodMin) - t * 0.25;
                tLon = ((tLon + 180) % 360) - 180;
                track.add(new double[]{tLat, tLon});
            }

            positions.add(new SatelliteData.SatPosition(
                NAMES[s], NORAD_IDS[s], lat, lon, ALT_KM[s], track));
        }

        return new SatelliteData(positions, Instant.now());
    }
}
