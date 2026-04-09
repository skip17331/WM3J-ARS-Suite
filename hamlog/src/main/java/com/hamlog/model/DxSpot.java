package com.hamlog.model;

import java.time.LocalDateTime;

/**
 * Represents an enriched DX spot received from the hub WebSocket server.
 */
public class DxSpot {

    // Core spot fields
    private String        spotter;
    private String        dxCallsign;     // "spotted" in hub JSON
    private double        frequencyKHz;   // hub sends Hz; HubEngine converts to kHz
    private String        comment;
    private LocalDateTime time;
    private String        rawLine;
    private boolean       active = true;

    // Enrichment fields provided by hub
    private String  mode;
    private String  country;
    private String  continent;
    private double  bearing;
    private double  distanceKm;
    private String  workedStatus;   // "needed", "worked", "confirmed", "unknown"

    public DxSpot() {}

    // ---------------------------------------------------------------
    // Core getters/setters
    // ---------------------------------------------------------------

    public String getSpotter()          { return spotter; }
    public void   setSpotter(String s)  { this.spotter = s; }

    public String getDxCallsign()           { return dxCallsign; }
    public void   setDxCallsign(String dx)  { this.dxCallsign = dx; }

    public double getFrequencyKHz()         { return frequencyKHz; }
    public void   setFrequencyKHz(double f) { this.frequencyKHz = f; }

    public String getComment()          { return comment; }
    public void   setComment(String c)  { this.comment = c; }

    public LocalDateTime getTime()              { return time; }
    public void          setTime(LocalDateTime t){ this.time = t; }

    public String getRawLine()          { return rawLine; }
    public void   setRawLine(String r)  { this.rawLine = r; }

    public boolean isActive()              { return active; }
    public void    setActive(boolean a)    { this.active = a; }

    // ---------------------------------------------------------------
    // Enrichment getters/setters
    // ---------------------------------------------------------------

    public String getMode()             { return mode; }
    public void   setMode(String m)     { this.mode = m; }

    public String getCountry()          { return country; }
    public void   setCountry(String c)  { this.country = c; }

    public String getContinent()            { return continent; }
    public void   setContinent(String c)    { this.continent = c; }

    public double getBearing()          { return bearing; }
    public void   setBearing(double b)  { this.bearing = b; }

    public double getDistanceKm()           { return distanceKm; }
    public void   setDistanceKm(double d)   { this.distanceKm = d; }

    public String getWorkedStatus()             { return workedStatus; }
    public void   setWorkedStatus(String ws)    { this.workedStatus = ws; }

    // ---------------------------------------------------------------
    // Derived
    // ---------------------------------------------------------------

    /** Band derived from frequency in kHz. */
    public String getBand() {
        if (frequencyKHz >= 1800  && frequencyKHz <= 2000)  return "160m";
        if (frequencyKHz >= 3500  && frequencyKHz <= 4000)  return "80m";
        if (frequencyKHz >= 7000  && frequencyKHz <= 7300)  return "40m";
        if (frequencyKHz >= 10100 && frequencyKHz <= 10150) return "30m";
        if (frequencyKHz >= 14000 && frequencyKHz <= 14350) return "20m";
        if (frequencyKHz >= 18068 && frequencyKHz <= 18168) return "17m";
        if (frequencyKHz >= 21000 && frequencyKHz <= 21450) return "15m";
        if (frequencyKHz >= 24890 && frequencyKHz <= 24990) return "12m";
        if (frequencyKHz >= 28000 && frequencyKHz <= 29700) return "10m";
        if (frequencyKHz >= 50000 && frequencyKHz <= 54000) return "6m";
        return "?";
    }
}
