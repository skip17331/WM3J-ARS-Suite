package com.wm3j.jmap.service.astronomy;

/**
 * Apparent position of a solar system planet for the current observer.
 */
public record PlanetData(String name, double azimuth, double altitude) {
    public boolean isVisible() { return altitude > 5; }
}
