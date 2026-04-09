package com.hamclock.service.solar;

import java.time.Instant;

/**
 * Solar and geomagnetic data fetched from NOAA SWPC or similar.
 */
public class SolarData {

    /** Solar Flux Index (SFI) at 10.7cm, in sfu */
    private double sfi;

    /** Planetary K-index (0-9) */
    private double kp;

    /** A-index (geomagnetic activity, 0-400) */
    private int aIndex;

    /** Sunspot number (Wolf number) */
    private int sunspotNumber;

    /** Solar wind speed (km/s) */
    private double solarWindSpeed;

    /** Solar wind density (particles/cm³) */
    private double solarWindDensity;

    /** Proton flux (particles/cm²/s/sr) */
    private double protonFlux;

    /** X-ray flux class (e.g., "A1.2", "C3.5", "M1.0", "X2.1") */
    private String xrayClass;

    /** When this data was observed */
    private Instant observationTime;

    /** True if this data is considered current */
    private boolean fresh;

    public SolarData() {}

    public SolarData(double sfi, double kp, int aIndex, int sunspotNumber) {
        this.sfi = sfi;
        this.kp = kp;
        this.aIndex = aIndex;
        this.sunspotNumber = sunspotNumber;
        this.observationTime = Instant.now();
        this.fresh = true;
    }

    /** Derived: geomagnetic storm level label */
    public String getKpLabel() {
        if (kp >= 8) return "EXTREME";
        if (kp >= 7) return "SEVERE";
        if (kp >= 6) return "STRONG";
        if (kp >= 5) return "MODERATE";
        if (kp >= 3) return "MINOR";    // NOAA G1 storm threshold = Kp 3
        return "QUIET";
    }

    /** Derived: propagation quality estimate from SFI */
    public String getSfiQuality() {
        if (sfi >= 200) return "EXCELLENT";
        if (sfi >= 150) return "VERY GOOD";
        if (sfi >= 120) return "GOOD";
        if (sfi >= 100) return "FAIR";
        if (sfi >= 80)  return "POOR";
        return "VERY POOR";
    }

    // Getters and setters
    public double getSfi() { return sfi; }
    public void setSfi(double sfi) { this.sfi = sfi; }

    public double getKp() { return kp; }
    public void setKp(double kp) { this.kp = kp; }

    public int getAIndex() { return aIndex; }
    public void setAIndex(int aIndex) { this.aIndex = aIndex; }

    public int getSunspotNumber() { return sunspotNumber; }
    public void setSunspotNumber(int sunspotNumber) { this.sunspotNumber = sunspotNumber; }

    public double getSolarWindSpeed() { return solarWindSpeed; }
    public void setSolarWindSpeed(double solarWindSpeed) { this.solarWindSpeed = solarWindSpeed; }

    public double getSolarWindDensity() { return solarWindDensity; }
    public void setSolarWindDensity(double solarWindDensity) { this.solarWindDensity = solarWindDensity; }

    public double getProtonFlux() { return protonFlux; }
    public void setProtonFlux(double protonFlux) { this.protonFlux = protonFlux; }

    public String getXrayClass() { return xrayClass; }
    public void setXrayClass(String xrayClass) { this.xrayClass = xrayClass; }

    public Instant getObservationTime() { return observationTime; }
    public void setObservationTime(Instant observationTime) { this.observationTime = observationTime; }

    public boolean isFresh() { return fresh; }
    public void setFresh(boolean fresh) { this.fresh = fresh; }

    @Override
    public String toString() {
        return String.format("SolarData[SFI=%.1f, Kp=%.1f, A=%d, SSN=%d, %s]",
            sfi, kp, aIndex, sunspotNumber, xrayClass);
    }
}
