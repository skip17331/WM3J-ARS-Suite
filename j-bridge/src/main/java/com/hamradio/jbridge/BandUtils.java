package com.hamradio.jbridge;

/**
 * BandUtils — frequency/band conversion helpers.
 *
 * Band boundary table matches j-hub's ClusterManager.freqToBand() and
 * j-log's DxSpot.getBand() exactly so all three apps agree on band labels.
 *
 * Input is always Hz (j-hub wire convention).
 */
public final class BandUtils {

    private BandUtils() {}

    /**
     * Derive ham band string from RF frequency in Hz.
     * Returns "OOB" (out-of-band) for unrecognised frequencies,
     * matching j-hub ClusterManager behaviour.
     */
    public static String frequencyToBand(long hz) {
        long khz = hz / 1000L;
        if (khz >= 1800  && khz <= 2000)  return "160m";
        if (khz >= 3500  && khz <= 4000)  return "80m";
        if (khz >= 5330  && khz <= 5410)  return "60m";
        if (khz >= 7000  && khz <= 7300)  return "40m";
        if (khz >= 10100 && khz <= 10150) return "30m";
        if (khz >= 14000 && khz <= 14350) return "20m";
        if (khz >= 18068 && khz <= 18168) return "17m";
        if (khz >= 21000 && khz <= 21450) return "15m";
        if (khz >= 24890 && khz <= 24990) return "12m";
        if (khz >= 28000 && khz <= 29700) return "10m";
        if (khz >= 50000 && khz <= 54000) return "6m";
        if (khz >= 144000 && khz <= 148000) return "2m";
        return "OOB";
    }
}
