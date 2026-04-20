package com.hamradio.jsat.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;

/**
 * J-Sat user configuration — loaded from J-Hub REST API, with disk fallback.
 * All setup is performed via the J-Hub web UI; j-sat only reads this data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JsatSettings {

    // Station
    public String callsign  = "WM3J";
    public double qthLat    = 40.0;
    public double qthLon    = -75.0;
    public double qthAltKm  = 0.0;

    /**
     * Satellites to track.
     * null  = not yet configured — fall back to the registry JSON enabled defaults.
     * empty = user explicitly cleared selection — track nothing.
     * set   = track only the named satellites.
     */
    public Set<String> enabledSatellites = null;

    public double minPassElevationDeg = 5.0;
    public int    passLookAheadHours  = 24;

    // Rig / Rotor (hardware is on J-Hub; these flags tell j-sat to send the commands)
    public boolean rigControlEnabled   = false;
    public boolean rotorControlEnabled = false;

    // Space weather
    public boolean showSpaceWeather = true;

    // Display
    public boolean showGroundTrack = true;
    public boolean showFootprint   = true;
    public boolean showPassList    = true;
    public int     fontSize        = 13;

    // J-Hub connection
    public String hubHost    = "localhost";
    public int    hubWsPort  = 8080;
    public int    hubWebPort = 8081;
}
