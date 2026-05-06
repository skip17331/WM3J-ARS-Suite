package com.hamradio.jhub.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AntennaStatus — current antenna selection per switch, broadcast as
 * {@code AMP_STATUS}'s antenna sibling. J-Log shows a single status-bar line.
 *
 * <p>Sent over the WebSocket bus as type "ANT_STATUS" whenever the
 * {@link com.hamradio.jhub.AntennaController} changes a selection.
 */
public class AntennaStatus {

    /** Message type discriminator — always "ANT_STATUS". */
    public String type = "ANT_STATUS";

    /** Current antenna selected on each switch, keyed by switch id. */
    public Map<String, SwitchState> switches = new LinkedHashMap<>();

    /** True when PTT lockout is currently suppressing rule evaluation. */
    public boolean locked = false;

    /** True when the last serial write failed; cleared on the next success. */
    public boolean faulted = false;

    /** Free-text reason, populated when {@link #faulted} or {@link #locked}. */
    public String reason = "";

    /** ISO-8601 UTC timestamp of the last state change. */
    public String timestamp = "";

    public AntennaStatus() {}

    public static class SwitchState {
        public String name        = "";
        public int    antenna     = 0;     // 0 = unset / unknown
        public boolean overridden = false; // manual override active
        public String  reason     = "";    // which rule fired (or "OVERRIDE")
        public SwitchState() {}
        public SwitchState(String name, int antenna, boolean overridden, String reason) {
            this.name = name; this.antenna = antenna;
            this.overridden = overridden; this.reason = reason;
        }
    }
}
