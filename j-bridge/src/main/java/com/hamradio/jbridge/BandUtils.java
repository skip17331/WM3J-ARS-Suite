package com.hamradio.jbridge;

/**
 * BandUtils — frequency/band conversion helpers.
 *
 * The band edges live in one place — the shared band plan in j-log-common.
 *
 * Input is always Hz (j-hub wire convention).
 */
public final class BandUtils {

    private BandUtils() {}

    /**
     * Derive ham band string from RF frequency in Hz, using the suite's shared band plan
     * ({@link com.jlog.bandplan.BandplanLoader}) so WSJT-X decodes get the same band names as the log and the cluster.
     * Returns "OOB" (out-of-band) for frequencies outside every amateur band.
     */
    public static String frequencyToBand(long hz) {
        String b = com.jlog.bandplan.BandplanLoader.getInstance().bandName(hz);
        return b == null ? "OOB" : b;
    }
}
