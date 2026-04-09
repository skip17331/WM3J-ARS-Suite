package com.hamclock.service.surface;

import java.time.Instant;

/**
 * Surface conditions overlay — temperature or pressure field as a grid,
 * or PNG image bytes from an external provider.
 */
public class SurfaceConditions {

    public enum DisplayMode { TEMPERATURE, PRESSURE }

    private final double[][] temperatureGrid;  // [lonIdx][latIdx] Celsius, may be null
    private final double[][] pressureGrid;     // [lonIdx][latIdx] hPa, may be null
    private final byte[]     pngBytes;         // pre-rendered PNG, may be null
    private final DisplayMode displayMode;
    private final Instant fetchedAt;

    public SurfaceConditions(double[][] temperatureGrid, double[][] pressureGrid,
                              DisplayMode displayMode, Instant fetchedAt) {
        this.temperatureGrid = temperatureGrid;
        this.pressureGrid    = pressureGrid;
        this.pngBytes        = null;
        this.displayMode     = displayMode;
        this.fetchedAt       = fetchedAt;
    }

    public SurfaceConditions(byte[] pngBytes, DisplayMode displayMode, Instant fetchedAt) {
        this.temperatureGrid = null;
        this.pressureGrid    = null;
        this.pngBytes        = pngBytes;
        this.displayMode     = displayMode;
        this.fetchedAt       = fetchedAt;
    }

    public double[][] getTemperatureGrid() { return temperatureGrid; }
    public double[][] getPressureGrid()    { return pressureGrid; }
    public byte[]     getPngBytes()        { return pngBytes; }
    public DisplayMode getDisplayMode()    { return displayMode; }
    public Instant    getFetchedAt()       { return fetchedAt; }
    public boolean    hasPng()             { return pngBytes != null && pngBytes.length > 0; }
}
