package com.hamradio.modem.model;

public class HubSpot {
    public String type;
    public String spotted;       // DX callsign
    public String spotter;
    public long   frequency;     // Hz
    public String band;
    public String mode;
    public String country;
    public String continent;
    public double bearing;
    public double distanceKm;
    public double distanceMi;
    public String comment;
    public String workedStatus;  // "needed" | "worked" | "confirmed" | "unknown"
    public String timestamp;
}
