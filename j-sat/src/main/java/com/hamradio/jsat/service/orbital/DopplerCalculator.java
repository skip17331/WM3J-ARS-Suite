package com.hamradio.jsat.service.orbital;

/**
 * Computes Doppler shift for satellite uplink and downlink frequencies.
 *
 * Doppler shift: Δf = -f₀ × (v_radial / c)
 *   v_radial positive = increasing range (satellite moving away) → negative shift
 *   rangeRate is in km/s, positive = moving away
 */
public final class DopplerCalculator {

    private static final double C = 299792.458;  // speed of light (km/s)

    private DopplerCalculator() {}

    /**
     * Compute Doppler-shifted frequency.
     *
     * @param nominalHz  nominal (unshifted) frequency in Hz
     * @param rangeRateKmSec  range rate in km/s (positive = satellite receding)
     * @return corrected frequency in Hz
     */
    public static long correctedFrequency(long nominalHz, double rangeRateKmSec) {
        double shift = -(double) nominalHz * rangeRateKmSec / C;
        return nominalHz + Math.round(shift);
    }

    /**
     * Doppler shift in Hz (positive = upshift, negative = downshift).
     */
    public static long dopplerShiftHz(long nominalHz, double rangeRateKmSec) {
        return Math.round(-(double) nominalHz * rangeRateKmSec / C);
    }

    /**
     * For a linear transponder (inverting), the uplink Doppler is reversed.
     * Pass-band center for inverting transponder:
     *   downlink_corrected = downlink_nominal + rangeRate × downlink_nominal / c
     *   uplink_corrected   = uplink_nominal   - rangeRate × uplink_nominal   / c
     */
    public static long invertingUplinkCorrection(long uplinkHz, double rangeRateKmSec) {
        double shift = (double) uplinkHz * rangeRateKmSec / C;
        return uplinkHz - Math.round(shift);
    }
}
