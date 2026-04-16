package com.wm3j.jmap.service.astronomy;

/**
 * Represents the day/night mask for the world map.
 * The mask is a 2D boolean array where true = nighttime.
 */
public class NightMask {

    /** [lonIdx][latIdx] where true = night */
    private final boolean[][] mask;

    /** Solar position used to compute this mask */
    private final SolarPosition solarPosition;

    private final int lonSteps;
    private final int latSteps;

    public NightMask(boolean[][] mask, SolarPosition solarPosition, int lonSteps, int latSteps) {
        this.mask = mask;
        this.solarPosition = solarPosition;
        this.lonSteps = lonSteps;
        this.latSteps = latSteps;
    }

    /**
     * Check if a geographic point is in nighttime.
     *
     * @param lat Geographic latitude [-90, +90]
     * @param lon Geographic longitude [-180, +180]
     */
    public boolean isNight(double lat, double lon) {
        int lonIdx = (int) Math.round(lon + 180) % lonSteps;
        int latIdx = (int) Math.round(90 - lat);
        lonIdx = Math.max(0, Math.min(lonSteps - 1, lonIdx));
        latIdx = Math.max(0, Math.min(latSteps - 1, latIdx));
        return mask[lonIdx][latIdx];
    }

    public boolean[][] getMask() { return mask; }
    public SolarPosition getSolarPosition() { return solarPosition; }
    public int getLonSteps() { return lonSteps; }
    public int getLatSteps() { return latSteps; }
}
