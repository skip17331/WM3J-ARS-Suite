package com.ars.fx.sat;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Pass prediction when the search starts during a pass (J-Sat recomputes passes every minute, so it often does). */
class PassPredictorTest {

    // ISS elements of 2026-11-17 and an FM19 station; the pass of 21:20:18Z-21:31:09Z (peak 53°) is the one under test
    private static final TleSet ISS = new TleSet("ISS (ZARYA)",
            "1 25544U 98067A   26321.52804937  .00010237  00000+0  19073-3 0  9997",
            "2 25544  51.6318 261.2656 0007379  36.1207 324.0279 15.49435330595529");
    private static final double LAT = 39.745583, LON = -76.959714;

    private static SatellitePass first(String utc) {
        List<SatellitePass> ps = new PassPredictor().predict(ISS, LAT, LON, 0, Instant.parse(utc));
        assertFalse(ps.isEmpty());
        return ps.get(0);
    }

    @Test
    void passStartedBeforeNowKeepsItsRealAosAndPeak() {
        SatellitePass mid = first("2026-11-17T21:25:00Z");
        SatellitePass before = first("2026-11-17T21:00:00Z");
        assertEquals(before.aos.getEpochSecond(), mid.aos.getEpochSecond(), 2, "AOS is when it rose, not now");
        assertEquals(before.los.getEpochSecond(), mid.los.getEpochSecond(), 2);
        assertEquals(before.maxElDeg, mid.maxElDeg, 1.0, "the peak already passed still counts");
    }

    @Test
    void lowTailOfAPassIsNotSwappedForTheNextOrbit() {
        // the last minute of the pass: the satellite is a few degrees up and sets at 21:31:09Z
        for (String now : new String[]{"2026-11-17T21:30:00Z", "2026-11-17T21:30:45Z"}) {
            SatellitePass p = first(now);
            assertTrue(p.isActive(Instant.parse(now)), now + ": the pass in progress comes first");
            assertTrue(p.durationSeconds() < 15 * 60, now + ": an ISS pass, not an orbit (" + p.durationSeconds() + " s)");
        }
    }
}
