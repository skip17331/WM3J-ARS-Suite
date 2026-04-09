package com.hamclock.service.astronomy;

/**
 * Represents the computed position of the Sun at a given instant.
 */
public class SolarPosition {

    /** Subsolar latitude (= declination), degrees [-23.45, +23.45] */
    private final double subsolarLat;

    /** Subsolar longitude (geographic), degrees [-180, +180] */
    private final double subsolarLon;

    /** Declination of the Sun, degrees */
    private final double declination;

    /** Right Ascension, degrees */
    private final double rightAscension;

    /** Greenwich Hour Angle, degrees */
    private final double gha;

    /** Julian Day Number */
    private final double julianDay;

    public SolarPosition(double subsolarLat, double subsolarLon, double declination,
                         double rightAscension, double gha, double julianDay) {
        this.subsolarLat = subsolarLat;
        this.subsolarLon = subsolarLon;
        this.declination = declination;
        this.rightAscension = rightAscension;
        this.gha = gha;
        this.julianDay = julianDay;
    }

    public double getSubsolarLat() { return subsolarLat; }
    public double getSubsolarLon() { return subsolarLon; }
    public double getDeclination() { return declination; }
    public double getRightAscension() { return rightAscension; }
    public double getGha() { return gha; }
    public double getJulianDay() { return julianDay; }

    @Override
    public String toString() {
        return String.format("SolarPosition[subsolar=(%.2f°, %.2f°), dec=%.2f°, ra=%.2f°]",
            subsolarLat, subsolarLon, declination, rightAscension);
    }
}
