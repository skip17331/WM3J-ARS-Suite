package com.wm3j.jmap.service.radar;

import javafx.scene.image.WritableImage;

import java.time.Instant;

/**
 * Radar composite overlay.
 * Three modes (only one populated per instance):
 *   tileImage — reprojected equirectangular WritableImage from tile compositor
 *   pngBytes  — raw equirectangular PNG bytes (legacy)
 *   intensity — [lonIdx][latIdx] 0.0–1.0 grid (mock data)
 */
public class RadarOverlay {

    private final WritableImage tileImage;
    private final byte[]        pngBytes;
    private final double[][]    intensity;
    private final Instant       fetchedAt;

    public RadarOverlay(WritableImage tileImage, Instant fetchedAt) {
        this.tileImage = tileImage;
        this.pngBytes  = null;
        this.intensity = null;
        this.fetchedAt = fetchedAt;
    }

    public RadarOverlay(byte[] pngBytes, Instant fetchedAt) {
        this.tileImage = null;
        this.pngBytes  = pngBytes;
        this.intensity = null;
        this.fetchedAt = fetchedAt;
    }

    public RadarOverlay(double[][] intensity, Instant fetchedAt) {
        this.tileImage = null;
        this.pngBytes  = null;
        this.intensity = intensity;
        this.fetchedAt = fetchedAt;
    }

    public WritableImage getTileImage()  { return tileImage; }
    public byte[]        getPngBytes()   { return pngBytes; }
    public double[][]    getIntensity()  { return intensity; }
    public Instant       getFetchedAt()  { return fetchedAt; }
    public boolean       hasTileImage()  { return tileImage != null; }
    public boolean       hasPng()        { return pngBytes != null && pngBytes.length > 0; }
}
