package com.hamradio.jsat.model;

import java.time.Instant;

/**
 * Real-time tracking state for a single satellite.
 */
public class SatelliteState {

    public final String  name;
    public final String  noradId;

    // Sub-satellite point
    public final double  latDeg;
    public final double  lonDeg;
    public final double  altKm;

    // Observer-relative
    public final double  elevationDeg;
    public final double  azimuthDeg;
    public final double  slantRangeKm;
    public final double  rangeRateKmSec;   // negative = approaching

    // Doppler (Hz)
    public final long    uplinkDopplerHz;
    public final long    downlinkDopplerHz;
    public final long    correctedUplinkHz;
    public final long    correctedDownlinkHz;

    // Sunlight
    public final boolean inSunlight;

    // Ground track (lat/lon pairs for future 90 min)
    public final double[][] groundTrack;

    // Footprint radius in degrees (great-circle)
    public final double footprintRadiusDeg;

    // Orbital altitude limits (km above sea level)
    public final double perigeeKm;
    public final double apogeeKm;

    public final Instant computedAt;

    public SatelliteState(String name, String noradId,
                          double latDeg, double lonDeg, double altKm,
                          double elevationDeg, double azimuthDeg,
                          double slantRangeKm, double rangeRateKmSec,
                          long uplinkDopplerHz, long downlinkDopplerHz,
                          long correctedUplinkHz, long correctedDownlinkHz,
                          boolean inSunlight, double[][] groundTrack,
                          double footprintRadiusDeg,
                          double perigeeKm, double apogeeKm) {
        this.name                 = name;
        this.noradId              = noradId;
        this.latDeg               = latDeg;
        this.lonDeg               = lonDeg;
        this.altKm                = altKm;
        this.elevationDeg         = elevationDeg;
        this.azimuthDeg           = azimuthDeg;
        this.slantRangeKm         = slantRangeKm;
        this.rangeRateKmSec       = rangeRateKmSec;
        this.uplinkDopplerHz      = uplinkDopplerHz;
        this.downlinkDopplerHz    = downlinkDopplerHz;
        this.correctedUplinkHz    = correctedUplinkHz;
        this.correctedDownlinkHz  = correctedDownlinkHz;
        this.inSunlight           = inSunlight;
        this.groundTrack          = groundTrack;
        this.footprintRadiusDeg   = footprintRadiusDeg;
        this.perigeeKm            = perigeeKm;
        this.apogeeKm             = apogeeKm;
        this.computedAt           = Instant.now();
    }

    public boolean isVisible() { return elevationDeg > 0.0; }
}
