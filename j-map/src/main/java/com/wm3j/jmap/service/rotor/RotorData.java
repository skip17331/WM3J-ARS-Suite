package com.wm3j.jmap.service.rotor;

import java.time.Instant;

/**
 * Antenna rotor position data from an IP-connected Arduino controller.
 */
public class RotorData {

    /** Current azimuth angle (0-360 degrees, 0=North, 90=East) */
    private double azimuth;

    /** Current elevation angle (0-90 degrees), if supported */
    private double elevation;

    /** Whether elevation data is available */
    private boolean elevationSupported;

    /** Target azimuth (if rotor is in motion) */
    private double targetAzimuth;

    /** True if the rotor is currently moving */
    private boolean inMotion;

    /** When this reading was taken */
    private Instant timestamp;

    /** True if connection to rotor controller is active */
    private boolean connected;

    public RotorData() {
        this.timestamp = Instant.now();
        this.connected = false;
    }

    public RotorData(double azimuth) {
        this.azimuth = azimuth;
        this.elevation = 0;
        this.timestamp = Instant.now();
        this.connected = true;
    }

    public RotorData(double azimuth, double elevation) {
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.elevationSupported = true;
        this.timestamp = Instant.now();
        this.connected = true;
    }

    /** Long-path bearing = short-path + 180° */
    public double getLongPathAzimuth() {
        return (azimuth + 180.0) % 360.0;
    }

    public double getAzimuth() { return azimuth; }
    public void setAzimuth(double azimuth) { this.azimuth = azimuth; }

    public double getElevation() { return elevation; }
    public void setElevation(double elevation) { this.elevation = elevation; }

    public boolean isElevationSupported() { return elevationSupported; }
    public void setElevationSupported(boolean elevationSupported) {
        this.elevationSupported = elevationSupported;
    }

    public double getTargetAzimuth() { return targetAzimuth; }
    public void setTargetAzimuth(double targetAzimuth) { this.targetAzimuth = targetAzimuth; }

    public boolean isInMotion() { return inMotion; }
    public void setInMotion(boolean inMotion) { this.inMotion = inMotion; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    @Override
    public String toString() {
        return String.format("RotorData[az=%.1f°, el=%.1f°, connected=%b, moving=%b]",
            azimuth, elevation, connected, inMotion);
    }
}
