package com.hamradio.jsat.service.orbital;

import com.hamradio.jsat.model.SatellitePass;
import com.hamradio.jsat.model.TleSet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Predicts satellite passes over an observer location.
 *
 * Iterates forward in time, computing elevation at each step.
 * AOS/LOS are found when elevation crosses 0°.
 */
public class PassPredictor {

    private static final double STEP_SEC       = 15.0;   // coarse step (seconds)
    private static final double FINE_STEP_SEC  = 1.0;    // fine step near crossing
    private static final double MIN_ELEVATION  = 0.0;    // horizon (degrees)
    private static final int    MAX_PASSES     = 10;
    private static final double LOOK_AHEAD_SEC = 86400.0; // 24 hours

    private final Sgp4Propagator propagator = new Sgp4Propagator();

    /**
     * Predict up to MAX_PASSES for the given satellite over the next 24 hours.
     *
     * @param tle       current TLE for this satellite
     * @param obsLat    observer latitude (degrees)
     * @param obsLon    observer longitude (degrees)
     * @param obsAlt    observer altitude (km)
     * @param startTime UTC start time for search window
     */
    public List<SatellitePass> predict(TleSet tle,
                                       double obsLat, double obsLon, double obsAlt,
                                       Instant startTime) {
        propagator.init(tle);
        List<SatellitePass> passes = new ArrayList<>();

        double tSec    = 0.0;
        double prevEl  = -90.0;
        boolean inPass = false;

        Instant aosTime    = null;
        double  aosAz      = 0.0;
        double  maxEl      = 0.0;
        Instant maxElTime  = null;
        double  maxElAz    = 0.0;
        double  maxRange   = 0.0;

        while (tSec < LOOK_AHEAD_SEC && passes.size() < MAX_PASSES) {
            Instant when = startTime.plusSeconds((long) tSec);
            double[] state = getState(when, obsLat, obsLon, obsAlt);
            if (state == null) { tSec += STEP_SEC; continue; }

            double el = state[1];  // elevation degrees
            double az = state[0];
            double range = state[2];

            if (!inPass && el > MIN_ELEVATION && prevEl <= MIN_ELEVATION) {
                // Rising: find precise AOS by binary search
                Instant precise = refineCrossing(startTime, tSec - STEP_SEC, tSec,
                                                 obsLat, obsLon, obsAlt, true);
                if (precise != null) {
                    double[] aosState = getState(precise, obsLat, obsLon, obsAlt);
                    aosTime = precise;
                    aosAz   = aosState != null ? aosState[0] : az;
                    maxEl   = el;
                    maxElTime = when;
                    maxElAz = az;
                    maxRange = range;
                    inPass  = true;
                }
            } else if (inPass) {
                if (el > maxEl) {
                    maxEl    = el;
                    maxElTime= when;
                    maxElAz  = az;
                    maxRange = range;
                }
                if (el <= MIN_ELEVATION && prevEl > MIN_ELEVATION) {
                    // Setting: find precise LOS
                    Instant losTime = refineCrossing(startTime, tSec - STEP_SEC, tSec,
                                                     obsLat, obsLon, obsAlt, false);
                    if (losTime == null) losTime = when;
                    double[] losState = getState(losTime, obsLat, obsLon, obsAlt);
                    double losAz = losState != null ? losState[0] : az;

                    if (maxEl >= 5.0) {  // only include passes ≥ 5° elevation
                        passes.add(new SatellitePass(
                            tle.name, tle.noradId,
                            aosTime, losTime, maxElTime,
                            maxEl, aosAz, losAz, maxElAz, maxRange));
                    }
                    inPass = false;
                    tSec  += STEP_SEC * 2; // skip ahead to avoid re-detecting same pass
                    prevEl = el;
                    continue;
                }
            }

            prevEl = el;
            tSec  += STEP_SEC;
        }

        return passes;
    }

    /** Binary search for the precise AOS/LOS crossing time. */
    private Instant refineCrossing(Instant base, double t1, double t2,
                                   double lat, double lon, double alt,
                                   boolean risingEdge) {
        for (int i = 0; i < 16; i++) {
            double tm = (t1 + t2) / 2.0;
            Instant when = base.plusSeconds((long) tm)
                              .plusNanos((long) ((tm % 1.0) * 1e9));
            double[] s = getState(when, lat, lon, alt);
            if (s == null) return null;
            boolean above = s[1] > MIN_ELEVATION;
            if (risingEdge) {
                if (above) t2 = tm; else t1 = tm;
            } else {
                if (above) t1 = tm; else t2 = tm;
            }
            if (t2 - t1 < 0.5) break;
        }
        return base.plusSeconds((long) ((t1 + t2) / 2.0));
    }

    /** Returns [azDeg, elDeg, rangeKm, rangeRateKmSec] or null on error. */
    private double[] getState(Instant when, double lat, double lon, double alt) {
        double[] eci = propagator.propagate(when);
        if (eci == null) return null;
        return CoordTransform.satAzElRange(
            new double[]{eci[0], eci[1], eci[2]},
            new double[]{eci[3], eci[4], eci[5]},
            lat, lon, alt, when);
    }
}
