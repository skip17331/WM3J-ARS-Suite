package com.wm3j.jmap.service.astronomy;

import java.util.List;

/**
 * Computed lunar position and phase data for the current observer.
 */
public class LunarData {

    private double azimuth;       // degrees 0=N clockwise
    private double altitude;      // degrees above horizon
    private double illumination;  // 0.0-1.0
    private double phaseAngle;    // 0-360 degrees
    private String phaseName;
    private double sublunarLat;
    private double sublunarLon;
    private List<PlanetData> planets;

    public double getAzimuth() { return azimuth; }
    public void setAzimuth(double azimuth) { this.azimuth = azimuth; }

    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }

    public double getIllumination() { return illumination; }
    public void setIllumination(double illumination) { this.illumination = illumination; }

    public double getPhaseAngle() { return phaseAngle; }
    public void setPhaseAngle(double phaseAngle) { this.phaseAngle = phaseAngle; }

    public String getPhaseName() { return phaseName; }
    public void setPhaseName(String phaseName) { this.phaseName = phaseName; }

    public double getSublunarLat() { return sublunarLat; }
    public void setSublunarLat(double sublunarLat) { this.sublunarLat = sublunarLat; }

    public double getSublunarLon() { return sublunarLon; }
    public void setSublunarLon(double sublunarLon) { this.sublunarLon = sublunarLon; }

    public List<PlanetData> getPlanets() { return planets; }
    public void setPlanets(List<PlanetData> planets) { this.planets = planets; }
}
