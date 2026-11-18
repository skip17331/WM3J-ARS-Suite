package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class GroundTrackCacheTest {
    private final AtomicInteger calls = new AtomicInteger();
    private double[][] make() { calls.incrementAndGet(); return new double[][]{{calls.get(), 0}}; }

    @Test
    void reusesATrackUntilItIsOldEnough() {
        GroundTrackCache c = new GroundTrackCache(10_000);
        Object tle = new Object();
        double[][] a = c.get(25544, tle, 1_000, this::make);
        assertSame(a, c.get(25544, tle, 3_000, this::make), "2 s later: the same track");
        assertSame(a, c.get(25544, tle, 10_999, this::make), "just under 10 s");
        assertEquals(1, calls.get());
        assertNotSame(a, c.get(25544, tle, 11_000, this::make), "10 s: recomputed");
        assertEquals(2, calls.get());
    }

    @Test
    void eachSatelliteHasItsOwnTrackAndNewElementsReplaceIt() {
        GroundTrackCache c = new GroundTrackCache(10_000);
        Object tle1 = new Object(), tle2 = new Object();
        c.get(1, tle1, 0, this::make); c.get(2, tle1, 0, this::make);
        assertEquals(2, calls.get(), "two satellites, two tracks");
        c.get(1, tle2, 100, this::make);
        assertEquals(3, calls.get(), "new element set: recomputed at once");
    }

    @Test
    void aClockThatGoesBackwardsOrAClearRecomputes() {
        GroundTrackCache c = new GroundTrackCache(10_000);
        Object tle = new Object();
        c.get(1, tle, 50_000, this::make);
        c.get(1, tle, 40_000, this::make);
        assertEquals(2, calls.get(), "time went backwards (clock change)");
        c.clear();
        c.get(1, tle, 40_000, this::make);
        assertEquals(3, calls.get());
    }
}
