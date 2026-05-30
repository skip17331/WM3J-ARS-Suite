package com.hamradio.jhub.model;

import java.util.ArrayList;
import java.util.List;

/**
 * RigCapabilities — what the *connected* rig (via its Hamlib backend) can
 * actually do, parsed from {@code \dump_caps} + {@code \dump_state}.
 *
 * <p>Published once per connection as a {@code RIG_CAPS} message so UI clients
 * (the J-Log Rig Control pane) can enable/disable controls to match the rig
 * instead of assuming an IC-746 shape. RIT/XIT/split-mode/power/S-meter all
 * gate off the flags here.
 *
 * <p><b>Fallback contract:</b> when {@link #known} is {@code false} the backend
 * could not parse capabilities (old Hamlib, a dummy/dongle backend, a parse
 * miss). Clients MUST then fall back to "everything enabled" so a rig that
 * worked before this feature — notably the IC-746 — never regresses. Every
 * {@code canXxx} convenience accessor below already returns {@code true} when
 * {@code !known}, so call those rather than reading the raw booleans.
 *
 * <p>Plain public fields + Gson, matching {@link RigStatus}.
 */
public class RigCapabilities {

    /** Message type discriminator — always "RIG_CAPS". */
    public String type = "RIG_CAPS";

    /** True once {@code dump_caps}/{@code dump_state} parsed into something
     *  usable. False = don't gate anything (see class header). */
    public boolean known = false;

    // ── Identity (from dump_caps) ────────────────────────────────────
    public int    model;                 // "Caps dump for model: N"
    public String modelName = "";        // "Model name:\tIC-746"
    public String mfgName   = "";         // "Mfg name:\tIcom"

    // ── Supported sets ───────────────────────────────────────────────
    /** Mode tokens the backend reports (e.g. USB LSB CW FM). Intersect with
     *  the UI's canonical mode set. */
    public List<String> modes   = new ArrayList<>();
    /** VFO tokens the backend reports (e.g. VFOA VFOB Main Sub MEM). */
    public List<String> vfoList = new ArrayList<>();

    // ── Operation flags (from "Can set/get X" lines) ─────────────────
    public boolean setFreq;
    public boolean getFreq;
    public boolean setMode;
    public boolean setVfo;
    public boolean getVfo;        // many Icoms report N here — handle gracefully
    public boolean setPtt;
    public boolean splitVfo;      // "Can set Split VFO"
    public boolean splitFreq;     // "Can set Split Freq"
    public boolean splitMode;     // "Can set Split Mode"
    public boolean ritOffset;     // "Can set RIT" (the set_rit offset call)
    public boolean xitOffset;     // "Can set XIT"

    // ── Func list membership (set_func RIT/XIT enable toggles) ───────
    public boolean ritFunc;       // "RIT" present in Set functions
    public boolean xitFunc;       // "XIT" present in Set functions

    // ── Level membership ─────────────────────────────────────────────
    public boolean getStrength;   // STRENGTH in "Get level" → S-meter
    public boolean getRfPower;    // RFPOWER in "Get level"
    public boolean setRfPower;    // RFPOWER in "Set level"

    // ── TX frequency ranges (Hz) from dump_state → band-keypad gating ─
    /** Each entry is {@code {lowHz, highHz}}. Empty = unknown (don't gate). */
    public List<long[]> txRanges = new ArrayList<>();

    public String source;         // "HAMLIB" / "CI-V"
    public String timestamp;      // ISO-8601 UTC

    public RigCapabilities() {}

    // ── Gating accessors: every one returns true when caps are unknown so
    //    the fallback is "enable everything". ─────────────────────────

    public boolean canSetFreq()    { return !known || setFreq; }
    public boolean canSetMode()    { return !known || setMode; }
    public boolean canSetVfo()     { return !known || setVfo; }
    public boolean canGetVfo()     { return !known || getVfo; }
    public boolean canSetPtt()     { return !known || setPtt; }
    public boolean canSplit()      { return !known || splitVfo; }
    public boolean canSplitFreq()  { return !known || splitFreq; }
    public boolean canSplitMode()  { return !known || splitMode; }
    /** RIT controls usable if either the offset call or the enable func exists. */
    public boolean canRit()        { return !known || ritOffset || ritFunc; }
    public boolean canXit()        { return !known || xitOffset || xitFunc; }
    public boolean canShowSMeter() { return !known || getStrength; }
    public boolean canSetRfPower() { return !known || setRfPower; }
    public boolean canShowRfPower(){ return !known || getRfPower; }

    /** True if {@code hz} falls in any TX range. Returns true when ranges are
     *  unknown (don't gate the band keypad). */
    public boolean canTransmitAt(long hz) {
        if (txRanges == null || txRanges.isEmpty()) return true;
        for (long[] r : txRanges) {
            if (r.length >= 2 && hz >= r[0] && hz <= r[1]) return true;
        }
        return false;
    }

    /** True if the backend reports the given mode token (case-insensitive).
     *  Returns true when the mode list is unknown/empty (don't gate). */
    public boolean hasMode(String mode) {
        if (mode == null) return false;
        if (modes == null || modes.isEmpty()) return true;
        for (String m : modes) if (m.equalsIgnoreCase(mode)) return true;
        return false;
    }
}
