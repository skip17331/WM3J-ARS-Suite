package com.hamradio.digitalbridge.model;

/**
 * BandActivity — aggregated decode statistics for one ham band.
 * Displayed in the BandActivityPanel side panel.
 */
public class BandActivity {

    private final String band;
    private int    spotCount;
    private String topDxCall    = "-";
    private double topDxDistKm  = 0;

    public BandActivity(String band) { this.band = band; }

    public String getBand()        { return band; }
    public int    getSpotCount()   { return spotCount; }
    public String getTopDxCall()   { return topDxCall; }
    public double getTopDxDistKm() { return topDxDistKm; }

    public void incrementSpots()   { spotCount++; }

    public void updateTopDx(String callsign, double distKm) {
        if (callsign == null || callsign.isBlank()) return;
        if (distKm > topDxDistKm || "-".equals(topDxCall)) {
            topDxCall   = callsign;
            topDxDistKm = distKm;
        }
    }

    public void reset() {
        spotCount   = 0;
        topDxCall   = "-";
        topDxDistKm = 0;
    }
}
