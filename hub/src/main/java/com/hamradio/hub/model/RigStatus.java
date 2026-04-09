package com.hamradio.hub.model;

/**
 * RigStatus — real-time transceiver state published by the Logger application.
 *
 * The Logger owns the CAT/CI-V rig control interface and publishes a
 * RIG_STATUS message whenever the frequency, mode, band, or power changes.
 * The hub caches the latest RigStatus and rebroadcasts it to all connected apps.
 */
public class RigStatus {

    /** Message type discriminator — always "RIG_STATUS". */
    public String type = "RIG_STATUS";

    /** Frequency in Hz (e.g. 14225000 for 14.225 MHz). */
    public long   frequency;

    /** Operating mode (e.g. "SSB", "CW", "FT8", "AM", "FM"). */
    public String mode;

    /** Band designation (e.g. "20m", "40m"). */
    public String band;

    /** Transmitter power in watts. */
    public int    power;

    /** Source of rig control data (e.g. "CI-V", "CAT", "MANUAL"). */
    public String source;

    /** ISO-8601 UTC timestamp of the status reading. */
    public String timestamp;

    public RigStatus() {}
}
