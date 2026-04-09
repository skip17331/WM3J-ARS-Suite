package com.hamclock.service.dx;

import java.time.Instant;

/**
 * Represents a single DX spot from a DX cluster.
 */
public class DxSpot {

    /** Callsign of the spotter (who reported the spot) */
    private String spotter;

    /** Callsign of the DX station being spotted */
    private String dxCallsign;

    /** Frequency in kHz */
    private double frequencyKhz;

    /** Band name derived from frequency (e.g., "20m", "40m") */
    private String band;

    /** Optional comment from the spotter */
    private String comment;

    /** When this spot was observed */
    private Instant timestamp;

    /** Geographic coordinates of the DX station (if known) */
    private double dxLat;
    private double dxLon;

    /** Geographic coordinates of the spotter (if known) */
    private double spotterLat;
    private double spotterLon;

    /** DXCC entity name of the DX station */
    private String dxccEntity;

    /** DXCC prefix of DX station */
    private String dxccPrefix;

    public DxSpot() {}

    public DxSpot(String spotter, String dxCallsign, double frequencyKhz, Instant timestamp) {
        this.spotter = spotter;
        this.dxCallsign = dxCallsign;
        this.frequencyKhz = frequencyKhz;
        this.timestamp = timestamp;
        this.band = frequencyToBand(frequencyKhz);
    }

    /** Derive band name from frequency in kHz */
    public static String frequencyToBand(double freqKhz) {
        if (freqKhz < 2000)  return "160m";
        if (freqKhz < 4000)  return "80m";
        if (freqKhz < 5500)  return "60m";
        if (freqKhz < 8000)  return "40m";
        if (freqKhz < 11000) return "30m";
        if (freqKhz < 15000) return "20m";
        if (freqKhz < 19000) return "17m";
        if (freqKhz < 22000) return "15m";
        if (freqKhz < 26000) return "12m";
        if (freqKhz < 35000) return "10m";
        if (freqKhz < 60000) return "6m";
        if (freqKhz < 150000) return "2m";
        return "UHF";
    }

    /** Color for this spot on the map, based on band */
    public String getBandColor() {
        return switch (band) {
            case "160m" -> "#ff4444";
            case "80m"  -> "#ff8800";
            case "60m"  -> "#ffaa00";
            case "40m"  -> "#ffff00";
            case "30m"  -> "#88ff00";
            case "20m"  -> "#00ff00";
            case "17m"  -> "#00ffaa";
            case "15m"  -> "#00aaff";
            case "12m"  -> "#0066ff";
            case "10m"  -> "#aa00ff";
            case "6m"   -> "#ff00ff";
            default     -> "#ffffff";
        };
    }

    /**
     * Infer operating mode from comment text and frequency.
     * Returns a short label: CW, SSB, FT8, FT4, RTTY, PSK, AM, FM, or DIGI.
     */
    public String getMode() {
        String c = comment != null ? comment.toUpperCase() : "";

        // Explicit mode keywords in comment take highest priority
        if (c.contains("FT8"))  return "FT8";
        if (c.contains("FT4"))  return "FT4";
        if (c.contains("RTTY") || c.contains("RTTYM")) return "RTTY";
        if (c.contains("PSK"))  return "PSK";
        if (c.contains("JS8"))  return "JS8";
        if (c.contains("WSPR")) return "WSPR";
        if (c.contains("SSB") || c.contains("USB") || c.contains("LSB")) return "SSB";
        if (c.contains(" CW") || c.startsWith("CW") || c.contains("CW ")) return "CW";
        if (c.contains("DIGI") || c.contains("DATA")) return "DIGI";
        if (c.contains(" AM ") || c.startsWith("AM"))  return "AM";
        if (c.contains(" FM ") || c.startsWith("FM"))  return "FM";

        // Infer from frequency sub-band if no comment keyword
        return inferModeFromFreq(frequencyKhz);
    }

    private static String inferModeFromFreq(double f) {
        // FT8 calling frequencies (±3 kHz)
        double[] ft8 = {1840, 3573, 5357, 7074, 10136, 14074, 18100, 21074, 24915, 28074, 50313};
        for (double freq : ft8) {
            if (Math.abs(f - freq) <= 3) return "FT8";
        }
        // FT4 calling frequencies
        double[] ft4 = {3575, 7047.5, 14080, 18104, 21140, 24919, 28180, 50318};
        for (double freq : ft4) {
            if (Math.abs(f - freq) <= 2) return "FT4";
        }
        // RTTY sub-bands
        if ((f >= 1800 && f <= 1810) || (f >= 3580 && f <= 3600) ||
            (f >= 7040 && f <= 7050) || (f >= 14080 && f <= 14112) ||
            (f >= 21080 && f <= 21120) || (f >= 28080 && f <= 28120)) return "RTTY";
        // CW sub-bands
        if ((f >= 1800 && f <= 1840) || (f >= 3500 && f <= 3570) ||
            (f >= 7000 && f <= 7044) || (f >= 10100 && f <= 10130) ||
            (f >= 14000 && f <= 14070) || (f >= 18068 && f <= 18095) ||
            (f >= 21000 && f <= 21070) || (f >= 24890 && f <= 24915) ||
            (f >= 28000 && f <= 28070) || (f >= 50000 && f <= 50100)) return "CW";
        // SSB sub-bands
        if ((f >= 1840 && f <= 2000) || (f >= 3600 && f <= 4000) ||
            (f >= 7074 && f <= 7300) || (f >= 14100 && f <= 14350) ||
            (f >= 18120 && f <= 18168) || (f >= 21150 && f <= 21450) ||
            (f >= 24930 && f <= 24990) || (f >= 28300 && f <= 29700)) return "SSB";
        return "?";
    }

    /** Color for this spot on the map, based on mode */
    public String getModeColor() {
        return switch (getMode()) {
            case "CW"   -> "#00ccff";   // cyan
            case "SSB"  -> "#ffd700";   // gold
            case "FT8"  -> "#00ff88";   // green
            case "FT4"  -> "#aaff00";   // lime
            case "RTTY" -> "#ff8800";   // orange
            case "PSK"  -> "#ff44ff";   // magenta
            case "JS8"  -> "#88ffff";   // light cyan
            case "WSPR" -> "#aaaaff";   // lavender
            case "AM"   -> "#ff4444";   // red
            case "FM"   -> "#ffffff";   // white
            default     -> "#aaaaaa";   // gray
        };
    }

    /** How old this spot is in minutes */
    public long ageMinutes() {
        return java.time.Duration.between(timestamp, Instant.now()).toMinutes();
    }

    // Getters and setters
    public String getSpotter() { return spotter; }
    public void setSpotter(String spotter) { this.spotter = spotter; }

    public String getDxCallsign() { return dxCallsign; }
    public void setDxCallsign(String dxCallsign) { this.dxCallsign = dxCallsign; }

    public double getFrequencyKhz() { return frequencyKhz; }
    public void setFrequencyKhz(double frequencyKhz) {
        this.frequencyKhz = frequencyKhz;
        this.band = frequencyToBand(frequencyKhz);
    }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public double getDxLat() { return dxLat; }
    public void setDxLat(double dxLat) { this.dxLat = dxLat; }

    public double getDxLon() { return dxLon; }
    public void setDxLon(double dxLon) { this.dxLon = dxLon; }

    public double getSpotterLat() { return spotterLat; }
    public void setSpotterLat(double spotterLat) { this.spotterLat = spotterLat; }

    public double getSpotterLon() { return spotterLon; }
    public void setSpotterLon(double spotterLon) { this.spotterLon = spotterLon; }

    public String getDxccEntity() { return dxccEntity; }
    public void setDxccEntity(String dxccEntity) { this.dxccEntity = dxccEntity; }

    public String getDxccPrefix() { return dxccPrefix; }
    public void setDxccPrefix(String dxccPrefix) { this.dxccPrefix = dxccPrefix; }

    @Override
    public String toString() {
        return String.format("DxSpot[%s de %s @ %.1f kHz (%s) %dmin ago]",
            dxCallsign, spotter, frequencyKhz, band, ageMinutes());
    }
}
