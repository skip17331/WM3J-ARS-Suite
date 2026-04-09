package com.hamclock.service.propagation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HF propagation conditions including band conditions, FOT, and MUF.
 */
public class PropagationData {

    public enum BandCondition {
        EXCELLENT("EXCEL", "#00ff88"),
        GOOD("GOOD",  "#44cc44"),
        FAIR("FAIR",  "#ffcc00"),
        POOR("POOR",  "#ff6600"),
        CLOSED("CLOSED","#cc2222");

        public final String label;
        public final String colorHex;

        BandCondition(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }
    }

    /** Optimum Traffic Frequency (FOT) in MHz */
    private double fot;

    /** Maximum Usable Frequency (MUF) in MHz */
    private double muf;

    /** Lowest Usable Frequency (LUF) in MHz */
    private double luf;

    /** Band conditions map: band name → condition */
    private final Map<String, BandCondition> bandConditions = new LinkedHashMap<>();

    /** Solar flux used in calculation */
    private double sfi;

    /** K-index used in calculation */
    private double kp;

    public PropagationData() {
        // Default band conditions
        bandConditions.put("80m",  BandCondition.POOR);
        bandConditions.put("60m",  BandCondition.FAIR);
        bandConditions.put("40m",  BandCondition.FAIR);
        bandConditions.put("30m",  BandCondition.FAIR);
        bandConditions.put("20m",  BandCondition.GOOD);
        bandConditions.put("17m",  BandCondition.GOOD);
        bandConditions.put("15m",  BandCondition.FAIR);
        bandConditions.put("12m",  BandCondition.FAIR);
        bandConditions.put("10m",  BandCondition.POOR);
        bandConditions.put("6m",   BandCondition.CLOSED);
    }

    public double getFot() { return fot; }
    public void setFot(double fot) { this.fot = fot; }

    public double getMuf() { return muf; }
    public void setMuf(double muf) { this.muf = muf; }

    public double getLuf() { return luf; }
    public void setLuf(double luf) { this.luf = luf; }

    public double getSfi() { return sfi; }
    public void setSfi(double sfi) { this.sfi = sfi; }

    public double getKp() { return kp; }
    public void setKp(double kp) { this.kp = kp; }

    public Map<String, BandCondition> getBandConditions() { return bandConditions; }

    public void setBandCondition(String band, BandCondition condition) {
        bandConditions.put(band, condition);
    }

    @Override
    public String toString() {
        return String.format("PropagationData[FOT=%.1fMHz, MUF=%.1fMHz, bands=%s]",
            fot, muf, bandConditions);
    }
}
