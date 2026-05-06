package com.hamradio.jhub.model;

/**
 * AmpStatus — real-time amplifier state published by HamlibAmpController.
 *
 * <p>Sent over the WebSocket bus as type "AMP_STATUS". J-Log shows a status
 * line in the entry pane; the J-Hub status window can show a fuller panel.
 *
 * <p>Levels that the amp doesn't support are reported as {@code Double.NaN}
 * so consumers can render them as "—" rather than zero.
 */
public class AmpStatus {

    /** Message type discriminator — always "AMP_STATUS". */
    public String type = "AMP_STATUS";

    /** Source of the amp control data (e.g. "HAMLIB"). */
    public String source = "HAMLIB";

    /** Last frequency reported by the amp in Hz, or 0 if unknown. */
    public long   frequency;

    /** Band tag derived from {@link #frequency} (e.g. "20m"), or "" if unknown. */
    public String band = "";

    /** SWR (e.g. 1.4). NaN when not supported by the amp. */
    public double swr  = Double.NaN;

    /** Forward power in watts. NaN when not supported. */
    public double fwdPower = Double.NaN;

    /** Reflected power in watts. NaN when not supported. */
    public double refPower = Double.NaN;

    /** Power state — "OPERATE", "STANDBY", or "UNKNOWN". */
    public String powerstat = "UNKNOWN";

    /** True when the amp reports a fault or SWR exceeds the configured threshold. */
    public boolean faulted = false;

    /** Human-readable fault reason, populated when {@link #faulted} is true. */
    public String faultReason = "";

    /** ISO-8601 UTC timestamp of the reading. */
    public String timestamp = "";

    public AmpStatus() {}
}
