package com.hamclock.service.radar;

import java.time.Instant;

/**
 * Radar composite overlay — equirectangular PNG bytes.
 * Intensity grid is an alternative for mock data.
 */
public class RadarOverlay {

    private final byte[] pngBytes;        // May be null (use intensity grid)
    private final double[][] intensity;   // [lonIdx][latIdx] 0.0–1.0, may be null
    private final Instant fetchedAt;

    public RadarOverlay(byte[] pngBytes, Instant fetchedAt) {
        this.pngBytes  = pngBytes;
        this.intensity = null;
        this.fetchedAt = fetchedAt;
    }

    public RadarOverlay(double[][] intensity, Instant fetchedAt) {
        this.pngBytes  = null;
        this.intensity = intensity;
        this.fetchedAt = fetchedAt;
    }

    public byte[]    getPngBytes()  { return pngBytes; }
    public double[][] getIntensity(){ return intensity; }
    public Instant   getFetchedAt() { return fetchedAt; }
    public boolean   hasPng()       { return pngBytes != null && pngBytes.length > 0; }
}
