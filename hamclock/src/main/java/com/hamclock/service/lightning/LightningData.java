package com.hamclock.service.lightning;

import java.time.Instant;
import java.util.List;

/**
 * Recent lightning strike data — a list of lat/lon strike positions
 * and an optional density grid for heatmap rendering.
 */
public class LightningData {

    public record Strike(double lat, double lon, Instant time) {}

    private final List<Strike> strikes;
    private final double[][] densityGrid;  // [lonIdx][latIdx] strikes per cell, may be null
    private final Instant fetchedAt;

    public LightningData(List<Strike> strikes, double[][] densityGrid, Instant fetchedAt) {
        this.strikes     = strikes;
        this.densityGrid = densityGrid;
        this.fetchedAt   = fetchedAt;
    }

    public List<Strike> getStrikes()       { return strikes; }
    public double[][] getDensityGrid()     { return densityGrid; }
    public Instant    getFetchedAt()       { return fetchedAt; }
}
