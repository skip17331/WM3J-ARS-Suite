package com.hamradio.jhub.model;

/**
 * RigStatus — real-time transceiver state published by the Logger application.
 *
 * The Logger owns the CAT/CI-V rig control interface and publishes a
 * RIG_STATUS message whenever the frequency, mode, band, or power changes.
 * J-Hub caches the latest RigStatus and rebroadcasts it to all connected apps.
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

    // ── Extended fields (added with the capability-driven Rig Control pane) ──
    // All default to neutral values; older/other consumers ignore unknown
    // fields. Populated by HamlibRigController from the get_rig_info snapshot
    // plus capability-gated level reads. The CI-V backend leaves them at
    // defaults, which the UI renders as "—".

    /** Active VFO name (e.g. "VFOA"/"Main"); "" when the rig can't report it. */
    public String  vfo = "";

    /** Split operation enabled. */
    public boolean split = false;

    /** Split (TX) frequency in Hz; 0 when not in split or unknown. */
    public long    splitTxFreq = 0;

    /** RIT offset in Hz, and whether RIT is engaged. */
    public int     rit = 0;
    public boolean ritOn = false;

    /** XIT offset in Hz, and whether XIT is engaged. */
    public int     xit = 0;
    public boolean xitOn = false;

    /** S-meter strength in dB relative to S9 (Hamlib STRENGTH);
     *  {@link Integer#MIN_VALUE} = not available / not read. */
    public int     sMeterDb = Integer.MIN_VALUE;

    /** RF output power as a 0.0–1.0 fraction; negative = not available. */
    public double  rfPower = -1.0;

    public RigStatus() {}
}
