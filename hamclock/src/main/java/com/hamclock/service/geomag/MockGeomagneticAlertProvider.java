package com.hamclock.service.geomag;

import com.hamclock.service.AbstractDataProvider;
import com.hamclock.service.DataProviderException;

import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockGeomagneticAlertProvider extends AbstractDataProvider<GeomagneticAlert>
        implements GeomagneticAlertProvider {

    private final Random random = new Random();

    @Override
    protected GeomagneticAlert doFetch() throws DataProviderException {
        // Simulate a mild geomagnetic storm
        double kp = 3.0 + random.nextDouble() * 4.0;  // 3–7
        double a  = kp * kp * 1.5;

        GeomagneticAlert.Level level;
        String summary;
        if (kp >= 7) {
            level = GeomagneticAlert.Level.ALERT;
            summary = "G3 Strong Storm";
        } else if (kp >= 6) {
            level = GeomagneticAlert.Level.WARNING;
            summary = "G2 Moderate Storm";
        } else if (kp >= 5) {
            level = GeomagneticAlert.Level.WATCH;
            summary = "G1 Minor Storm";
        } else {
            level = GeomagneticAlert.Level.NONE;
            summary = "Quiet";
        }

        return new GeomagneticAlert(
            kp, a, level, summary,
            List.of(
                "NOAA/NWS Space Weather Prediction Center",
                "Geomagnetic K-index: " + String.format("%.0f", kp),
                "A-index: " + String.format("%.0f", a),
                summary
            ),
            Instant.now()
        );
    }
}
