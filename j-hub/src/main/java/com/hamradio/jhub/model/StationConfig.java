package com.hamradio.jhub.model;

/**
 * StationConfig — operator station details used for geographic calculations.
 *
 * This is a convenience POJO that mirrors JHubConfig.StationSection; it can
 * be used when passing station data around without carrying the full
 * JHubConfig object.
 */
public class StationConfig {

    /** Operator callsign (e.g. "W3ABC"). */
    public String callsign;

    /** Latitude of station location in decimal degrees. */
    public double lat;

    /** Longitude of station location in decimal degrees. */
    public double lon;

    /** Maidenhead grid square (e.g. "FM19"). */
    public String gridSquare;

    /** Java timezone ID (e.g. "America/New_York"). */
    public String timezone;

    public StationConfig() {}

    public StationConfig(String callsign, double lat, double lon,
                         String gridSquare, String timezone) {
        this.callsign  = callsign;
        this.lat       = lat;
        this.lon       = lon;
        this.gridSquare = gridSquare;
        this.timezone  = timezone;
    }
}
