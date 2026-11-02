package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class SolarPositionTest {

    @Test
    void subSolarPointFollowsTheClockAndTheSeason() {
        double[] juneNoon = SolarPosition.subSolar(Instant.parse("2026-06-21T12:00:00Z"));
        assertEquals(23.4, juneNoon[0], 0.2);
        assertEquals(0, juneNoon[1], 0.01);
        double[] decemberEvening = SolarPosition.subSolar(Instant.parse("2026-12-21T18:00:00Z"));
        assertEquals(-23.4, decemberEvening[0], 0.2);
        assertEquals(-90, decemberEvening[1], 0.01);
    }

    @Test
    void poleHasNoSunriseOrSunset() {
        double[] rs = SolarPosition.riseSet(89.9, 0);
        assertTrue(Double.isNaN(rs[0]));
    }

    @Test
    void formatsHoursAcrossMidnight() {
        assertEquals("12:02", SolarPosition.hhmm(12.04));
        assertEquals("00:00", SolarPosition.hhmm(24.0));
    }
}
