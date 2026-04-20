package com.hamradio.jsat.model;

import java.time.Instant;

/**
 * Result of a pass prediction: one satellite pass over the observer.
 */
public class SatellitePass implements Comparable<SatellitePass> {

    public final String  satName;
    public final String  noradId;
    public final Instant aos;             // Acquisition of Signal
    public final Instant los;             // Loss of Signal
    public final Instant maxElTime;       // Time of maximum elevation
    public final double  maxElDeg;        // Maximum elevation (degrees)
    public final double  aosAzDeg;        // Azimuth at AOS
    public final double  losAzDeg;        // Azimuth at LOS
    public final double  maxElAzDeg;      // Azimuth at max elevation
    public final double  slantRangeKm;    // Range at max elevation

    public SatellitePass(String satName, String noradId,
                         Instant aos, Instant los, Instant maxElTime,
                         double maxElDeg, double aosAzDeg, double losAzDeg,
                         double maxElAzDeg, double slantRangeKm) {
        this.satName      = satName;
        this.noradId      = noradId;
        this.aos          = aos;
        this.los          = los;
        this.maxElTime    = maxElTime;
        this.maxElDeg     = maxElDeg;
        this.aosAzDeg     = aosAzDeg;
        this.losAzDeg     = losAzDeg;
        this.maxElAzDeg   = maxElAzDeg;
        this.slantRangeKm = slantRangeKm;
    }

    public long durationSeconds() {
        return los.getEpochSecond() - aos.getEpochSecond();
    }

    public boolean isActive(Instant now) {
        return !now.isBefore(aos) && !now.isAfter(los);
    }

    public long secondsUntilAos(Instant now) {
        return aos.getEpochSecond() - now.getEpochSecond();
    }

    @Override
    public int compareTo(SatellitePass o) {
        return this.aos.compareTo(o.aos);
    }
}
