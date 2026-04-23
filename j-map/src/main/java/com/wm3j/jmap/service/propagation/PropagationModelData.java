package com.wm3j.jmap.service.propagation;

/**
 * Grid of computed MUF values across the globe, relative to a QTH source point.
 * Grid is 72 longitude cells × 36 latitude cells (5° × 5° resolution).
 */
public class PropagationModelData {

    private double[][] mufGrid;   // [lonIdx][latIdx]
    private double qthLat;
    private double qthLon;

    public double[][] getMufGrid() { return mufGrid; }
    public void setMufGrid(double[][] mufGrid) { this.mufGrid = mufGrid; }

    public double getQthLat() { return qthLat; }
    public void setQthLat(double qthLat) { this.qthLat = qthLat; }

    public double getQthLon() { return qthLon; }
    public void setQthLon(double qthLon) { this.qthLon = qthLon; }
}
