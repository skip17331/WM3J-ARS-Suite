package com.hamradio.digitalbridge.model;

import java.time.Instant;

/**
 * WsjtxDecode — a signal decoded by WSJT-X (UDP message type 2).
 *
 * Raw fields come from the WSJT-X binary protocol.
 * Enrichment fields (country, bearing, etc.) are populated by j-hub's
 * SpotEnricher after the WSJTX_DECODE message is published — Digital Bridge
 * does NOT compute these locally; it receives them back via the hub broadcast.
 *
 * Wire-protocol field names match the j-hub WSJTX_DECODE JSON schema so that
 * MessageRouter can rebroadcast to j-log without transformation.
 */
public class WsjtxDecode {

    // ── WSJT-X raw fields ─────────────────────────────────────────────────────

    /** UTC timestamp of the decode period start */
    private Instant timestamp;

    /** Signal-to-noise ratio in dB */
    private int snr;

    /** Delta time offset in seconds from period start */
    private double deltaTime;

    /** Audio frequency offset within the passband, Hz */
    private int deltaFrequency;

    /** Full decoded message text e.g. "CQ W3ABC FM19" */
    private String message;

    /** Low-confidence decode flag */
    private boolean lowConfidence;

    /** Decoded during off-period flag */
    private boolean offAir;

    // ── Fields set by Digital Bridge before publishing ────────────────────────

    /** RF dial frequency in Hz (from last WSJT-X Status message) */
    private long frequency;

    /** Band derived from frequency e.g. "20m" */
    private String band;

    /** Operating mode e.g. "FT8" (from last WSJT-X Status message) */
    private String mode;

    /** DX callsign extracted from message text by CallsignParser */
    private String callsign;

    /** Worked status: "worked" | "needed" | "unknown" */
    private String workedStatus = "unknown";

    // ── Enrichment fields — populated by j-hub, read back from broadcast ─────

    private String country;
    private String continent;
    private int    dxcc;
    private double lat;
    private double lon;
    private double bearing;
    private double distanceKm;
    private double distanceMi;
    private String localTimeAtSpot;

    // ── Constructors ──────────────────────────────────────────────────────────

    public WsjtxDecode() {}

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns true if the message text begins with "CQ". */
    public boolean isCqCall() {
        return message != null && message.trim().toUpperCase().startsWith("CQ");
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Instant getTimestamp()                    { return timestamp; }
    public void    setTimestamp(Instant v)           { this.timestamp = v; }

    public int     getSnr()                          { return snr; }
    public void    setSnr(int v)                     { this.snr = v; }

    public double  getDeltaTime()                    { return deltaTime; }
    public void    setDeltaTime(double v)            { this.deltaTime = v; }

    public int     getDeltaFrequency()               { return deltaFrequency; }
    public void    setDeltaFrequency(int v)          { this.deltaFrequency = v; }

    public String  getMessage()                      { return message; }
    public void    setMessage(String v)              { this.message = v; }

    public boolean isLowConfidence()                 { return lowConfidence; }
    public void    setLowConfidence(boolean v)       { this.lowConfidence = v; }

    public boolean isOffAir()                        { return offAir; }
    public void    setOffAir(boolean v)              { this.offAir = v; }

    public long    getFrequency()                    { return frequency; }
    public void    setFrequency(long v)              { this.frequency = v; }

    public String  getBand()                         { return band; }
    public void    setBand(String v)                 { this.band = v; }

    public String  getMode()                         { return mode; }
    public void    setMode(String v)                 { this.mode = v; }

    public String  getCallsign()                     { return callsign; }
    public void    setCallsign(String v)             { this.callsign = v; }

    public String  getWorkedStatus()                 { return workedStatus; }
    public void    setWorkedStatus(String v)         { this.workedStatus = v; }

    public String  getCountry()                      { return country; }
    public void    setCountry(String v)              { this.country = v; }

    public String  getContinent()                    { return continent; }
    public void    setContinent(String v)            { this.continent = v; }

    public int     getDxcc()                         { return dxcc; }
    public void    setDxcc(int v)                    { this.dxcc = v; }

    public double  getLat()                          { return lat; }
    public void    setLat(double v)                  { this.lat = v; }

    public double  getLon()                          { return lon; }
    public void    setLon(double v)                  { this.lon = v; }

    public double  getBearing()                      { return bearing; }
    public void    setBearing(double v)              { this.bearing = v; }

    public double  getDistanceKm()                   { return distanceKm; }
    public void    setDistanceKm(double v)           { this.distanceKm = v; }

    public double  getDistanceMi()                   { return distanceMi; }
    public void    setDistanceMi(double v)           { this.distanceMi = v; }

    public String  getLocalTimeAtSpot()              { return localTimeAtSpot; }
    public void    setLocalTimeAtSpot(String v)      { this.localTimeAtSpot = v; }

    @Override
    public String toString() {
        return "WsjtxDecode{call=" + callsign + ", snr=" + snr +
               ", freq=" + deltaFrequency + ", msg='" + message + "'}";
    }
}
