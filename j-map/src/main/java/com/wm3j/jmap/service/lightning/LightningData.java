package com.wm3j.jmap.service.lightning;

import javafx.scene.image.WritableImage;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Lightning overlay data.
 * Two modes:
 *   tileImage   — reprojected equirectangular WritableImage (RainViewer tiles)
 *   strikes + densityGrid — lat/lon strike list with heatmap (Blitzortung / mock)
 */
public class LightningData {

    public record Strike(double lat, double lon, Instant time) {}

    private final WritableImage  tileImage;
    private final List<Strike>   strikes;
    private final double[][]     densityGrid;
    private final Instant        fetchedAt;

    /** Tile-based constructor (RainViewer). */
    public LightningData(WritableImage tileImage, Instant fetchedAt) {
        this.tileImage   = tileImage;
        this.strikes     = Collections.emptyList();
        this.densityGrid = null;
        this.fetchedAt   = fetchedAt;
    }

    /** Strike-based constructor (Blitzortung / mock). */
    public LightningData(List<Strike> strikes, double[][] densityGrid, Instant fetchedAt) {
        this.tileImage   = null;
        this.strikes     = strikes;
        this.densityGrid = densityGrid;
        this.fetchedAt   = fetchedAt;
    }

    public WritableImage getTileImage()    { return tileImage; }
    public boolean       hasTileImage()   { return tileImage != null; }
    public List<Strike>  getStrikes()     { return strikes; }
    public double[][]    getDensityGrid() { return densityGrid; }
    public Instant       getFetchedAt()   { return fetchedAt; }
}
