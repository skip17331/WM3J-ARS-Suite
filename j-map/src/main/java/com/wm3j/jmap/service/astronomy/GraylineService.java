package com.wm3j.jmap.service.astronomy;

import java.time.ZonedDateTime;

/**
 * Computes the grayline (day/night terminator) for world map rendering.
 * The terminator is the set of points on Earth where the solar zenith angle = 90°.
 *
 * Returns an array of latitudes (one per longitude) defining the terminator line,
 * plus a darkness mask for shading the nightside.
 */
public class GraylineService {

    private final SolarPositionService solarPositionService;

    // Resolution of the terminator calculation
    private static final int LON_STEPS = 360;
    private static final int LAT_STEPS = 180;

    public GraylineService(SolarPositionService solarPositionService) {
        this.solarPositionService = solarPositionService;
    }

    /**
     * Compute the night mask: a 2D boolean array [lon][lat] where true = night.
     * Array indices map to: lon = -180..+180, lat = +90..-90 (top to bottom)
     *
     * @param utc Current UTC time
     * @return NightMask with day/night array and subsolar point
     */
    public NightMask computeNightMask(ZonedDateTime utc) {
        SolarPosition sun = solarPositionService.computePosition(utc);

        boolean[][] nightMask = new boolean[LON_STEPS][LAT_STEPS];

        double decRad = Math.toRadians(sun.getDeclination());

        for (int lonIdx = 0; lonIdx < LON_STEPS; lonIdx++) {
            double lon = lonIdx - 180.0;  // -180 to +179

            // Hour angle at this longitude
            double ha = sun.getGha() + lon;
            double haRad = Math.toRadians(ha);

            for (int latIdx = 0; latIdx < LAT_STEPS; latIdx++) {
                double lat = 90.0 - latIdx;  // +90 to -90

                double latRad = Math.toRadians(lat);

                // Solar elevation = 90 - zenith
                double cosZ = Math.sin(latRad) * Math.sin(decRad)
                            + Math.cos(latRad) * Math.cos(decRad) * Math.cos(haRad);
                cosZ = Math.max(-1.0, Math.min(1.0, cosZ));

                // Night when cosZ < 0 (zenith > 90°)
                // Civil twilight boundary: cosZ < cos(96°) ≈ -0.1045
                nightMask[lonIdx][latIdx] = cosZ < 0;
            }
        }

        return new NightMask(nightMask, sun, LON_STEPS, LAT_STEPS);
    }

    /**
     * Compute the terminator latitude at a given longitude.
     * Returns the latitude where solar zenith = 90° at that longitude.
     *
     * @param lon Longitude degrees [-180, +180]
     * @param sun Current solar position
     * @return Array of [lat1, lat2] terminator crossings, or empty if entire column is day/night
     */
    public double[] terminatorLatAtLon(double lon, SolarPosition sun) {
        double decRad = Math.toRadians(sun.getDeclination());
        double ha = sun.getGha() + lon;
        double haRad = Math.toRadians(ha);

        // cos(zenith) = sin(lat)*sin(dec) + cos(lat)*cos(dec)*cos(ha) = 0 at terminator
        // sin(lat)*sin(dec) = -cos(lat)*cos(dec)*cos(ha)
        // tan(lat) = -cos(dec)*cos(ha) / sin(dec)

        if (Math.abs(sun.getDeclination()) < 0.01) {
            // Sun near equinox - terminator is the prime meridian shifted by 90°
            return new double[]{0.0};
        }

        double tanLat = -(Math.cos(decRad) * Math.cos(haRad)) / Math.sin(decRad);
        double latRad = Math.atan(tanLat);
        double lat = Math.toDegrees(latRad);

        // Two terminator points per longitude (one on each hemisphere)
        double lat2 = lat > 0 ? lat - 180 : lat + 180;
        lat2 = Math.max(-90, Math.min(90, lat2));

        return new double[]{lat, lat2};
    }

    public int getLonSteps() { return LON_STEPS; }
    public int getLatSteps() { return LAT_STEPS; }
}
