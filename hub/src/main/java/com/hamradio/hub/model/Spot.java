package com.hamradio.hub.model;

/**
 * Spot — represents a DX cluster spot, enriched with geographic data.
 *
 * All fields are public so Gson can serialize/deserialize without reflection
 * configuration.  The {@code type} field is always "SPOT" so connected apps
 * can identify the message type.
 */
public class Spot {

    // --- Protocol ---
    public String type = "SPOT";

    // --- Core spot data ---
    public String spotter;      // callsign of the spotting station
    public String spotted;      // callsign of the spotted DX station
    public long   frequency;    // in Hz
    public String band;         // e.g. "20m"
    public String mode;         // e.g. "FT8", "SSB", "CW"
    public String comment;      // raw comment from cluster line
    public String timestamp;    // ISO-8601 UTC

    // --- DXCC enrichment ---
    public String country;      // country name of spotted station
    public String continent;    // two-letter continent code e.g. "EU"
    public int    dxcc;         // DXCC entity number

    // --- Geographic enrichment ---
    public double lat;           // latitude of spotted entity
    public double lon;           // longitude of spotted entity
    public double bearing;       // true bearing from our station
    public double distanceKm;    // km
    public double distanceMi;    // miles

    // --- Temporal enrichment ---
    public String localTimeAtSpot; // HH:mm local time at the DX entity

    // --- Logger integration ---
    public String workedStatus; // "needed", "worked", "confirmed", "unknown"

    public Spot() {}
}
