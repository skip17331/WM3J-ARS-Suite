package com.wm3j.jmap.service.solar;

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

    /** Solar wind speed (km/s) from DSCOVR */
    private double solarWindSpeed;

    /** Solar wind density (particles/cm³) from DSCOVR */
    private double solarWindDensity;

    /** IMF total field magnitude Bt (nT) from DSCOVR */
    private double btField;

    /** IMF Bz component (nT) from DSCOVR — negative = aurora-geoeffective */
    private double bzField;

    /** Proton flux ≥10 MeV (pfu) from GOES */
    private double protonFlux;

    /** X-ray flux in W/m² (0.1–0.8 nm channel) from GOES */
    private double xrayFlux;

    /** X-ray flux class derived from xrayFlux (e.g., "A1.2", "C3.5", "M1.0", "X2.1") */
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

    public double getBtField() { return btField; }
    public void setBtField(double btField) { this.btField = btField; }

    public double getBzField() { return bzField; }
    public void setBzField(double bzField) { this.bzField = bzField; }

    public double getProtonFlux() { return protonFlux; }
    public void setProtonFlux(double protonFlux) { this.protonFlux = protonFlux; }

    public double getXrayFlux() { return xrayFlux; }
    public void setXrayFlux(double xrayFlux) {
        this.xrayFlux = xrayFlux;
        this.xrayClass = fluxToClass(xrayFlux);
    }

    public String getXrayClass() { return xrayClass; }
    public void setXrayClass(String xrayClass) { this.xrayClass = xrayClass; }

    /** Bz label for UI — shows polarity clearly */
    public String getBzLabel() {
        if (bzField <= -20) return "EXTREME-";
        if (bzField <= -10) return "STRONG-";
        if (bzField <= -5)  return "MOD-";
        if (bzField < 0)    return "NEG";
        if (bzField < 5)    return "POS";
        return "STRONG+";
    }

    /** Color hint for Bz — negative is bad (aurora-geoeffective) */
    public String getBzColor() {
        if (bzField <= -20) return "#ff2222";
        if (bzField <= -10) return "#ff6600";
        if (bzField <= -5)  return "#ffcc00";
        if (bzField < 0)    return "#aaddff";
        return "#44cc44";
    }

    /** Proton flux level for GOES proton event (SEP) */
    public String getProtonLabel() {
        if (protonFlux >= 10000) return "EXTREME";
        if (protonFlux >= 1000)  return "SEVERE";
        if (protonFlux >= 100)   return "STRONG";
        if (protonFlux >= 10)    return "EVENT";
        return "NOMINAL";
    }

    public static String fluxToClass(double flux) {
        if (flux <= 0) return "---";
        char cls; double base;
        if      (flux >= 1e-4) { cls = 'X'; base = 1e-4; }
        else if (flux >= 1e-5) { cls = 'M'; base = 1e-5; }
        else if (flux >= 1e-6) { cls = 'C'; base = 1e-6; }
        else if (flux >= 1e-7) { cls = 'B'; base = 1e-7; }
        else                   { cls = 'A'; base = 1e-8; }
        return String.format("%c%.1f", cls, flux / base);
    }

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
