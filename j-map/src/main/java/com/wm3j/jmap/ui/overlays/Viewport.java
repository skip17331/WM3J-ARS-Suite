package com.wm3j.jmap.ui.overlays;

public final class Viewport {
    public final double lonMin, lonMax, latMin, latMax;

    private Viewport(double lonMin, double lonMax, double latMin, double latMax) {
        this.lonMin = lonMin; this.lonMax = lonMax;
        this.latMin = latMin; this.latMax = latMax;
    }

    public double lonRange() { return lonMax - lonMin; }
    public double latRange() { return latMax - latMin; }

    public static Viewport forKey(String key) {
        if (key == null) return WORLD;
        return switch (key) {
            case "NORTH_AMERICA" -> new Viewport(-170, -50,  10, 75);
            case "EUROPE"        -> new Viewport( -25,  45,  30, 75);
            case "ASIA"          -> new Viewport(  40, 180,   0, 75);
            case "OCEANIA"       -> new Viewport(  90, 180, -55, 10);
            default              -> WORLD;
        };
    }

    public static final Viewport WORLD = new Viewport(-180, 180, -90, 90);
}
