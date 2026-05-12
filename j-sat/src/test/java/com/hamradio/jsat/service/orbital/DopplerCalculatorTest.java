package com.hamradio.jsat.service.orbital;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DopplerCalculatorTest {

    @Test
    void recedingSatelliteDownshifts() {
        // 145.5 MHz, satellite moving away at 7 km/s.
        long shifted = DopplerCalculator.correctedFrequency(145_500_000L, 7.0);
        assertTrue(shifted < 145_500_000L, "receding sat should drop the frequency");
        long delta = DopplerCalculator.dopplerShiftHz(145_500_000L, 7.0);
        // Δf = -f × v/c  →  ≈ -3398 Hz at 145.5 MHz / 7 km·s⁻¹
        assertEquals(-3398, delta, 2);
    }

    @Test
    void approachingSatelliteUpshifts() {
        long shifted = DopplerCalculator.correctedFrequency(435_000_000L, -7.0);
        assertTrue(shifted > 435_000_000L, "approaching sat should raise the frequency");
    }

    @Test
    void zeroRangeRateIsZeroShift() {
        assertEquals(0L, DopplerCalculator.dopplerShiftHz(145_500_000L, 0.0));
        assertEquals(145_500_000L,
            DopplerCalculator.correctedFrequency(145_500_000L, 0.0));
    }

    @Test
    void invertingTransponderReversesUplinkSign() {
        // Receding satellite (positive range rate) downshifts downlink but
        // upshifts the corrected uplink we should transmit, since the bird
        // inverts.
        long uplinkCorrected = DopplerCalculator.invertingUplinkCorrection(435_000_000L, 7.0);
        assertTrue(uplinkCorrected < 435_000_000L,
            "inverting uplink correction for a receding sat goes down (we transmit lower so the bird hears nominal)");
    }

    @Test
    void scalesLinearlyWithFrequency() {
        long lowBand  = DopplerCalculator.dopplerShiftHz(28_000_000L,  -5.0);
        long highBand = DopplerCalculator.dopplerShiftHz(435_000_000L, -5.0);
        // 435 / 28 ≈ 15.54, so the high-band shift should be ~15× larger in magnitude.
        assertEquals(15.54, (double) highBand / lowBand, 0.05);
    }
}
