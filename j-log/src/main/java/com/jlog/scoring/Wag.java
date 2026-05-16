package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

/**
 * Worked All Germany Contest helpers (DARC). A "German station" is one
 * whose DXCC entity is Germany (id 230). Points are asymmetric by
 * entrant; the multiplier is counted per band AND per mode-class —
 * "once in CW and once in SSB" (new from 2024) — so CW and Phone are
 * tracked separately. For a non-German entrant the multiplier is the
 * German district = first letter of the worked station's DOK ("NM" =
 * non-member is never a multiplier); for a German entrant it is each
 * DXCC/WAE area worked (delegated to {@link WaeMultiplier}). Claimed/
 * running — the WAG committee re-adjudicates.
 */
public final class Wag {

    private Wag() {}

    /** DXCC entity id of Germany. */
    public static final String GERMANY = "230";

    /** True if the call is a German station (DXCC Germany). */
    public static boolean isGerman(String rawCall) {
        return GERMANY.equals(DxccResolver.getInstance().entityOf(rawCall));
    }

    /** Mode-class for the WAG multiplier: "CW" or "PH" (SSB/USB/LSB).
     *  WAG is CW + SSB only; anything not CW folds to phone. */
    public static String modeClass(String mode) {
        return mode != null && mode.trim().equalsIgnoreCase("CW") ? "CW" : "PH";
    }

    /** German district multiplier token = first letter of the DOK
     *  (special DOKs included — the first letter still counts), upper-
     *  cased; null if the DOK is blank or "NM" (non-member = no mult). */
    public static String dokDistrict(String dok) {
        if (dok == null) return null;
        String d = dok.trim().toUpperCase();
        if (d.isEmpty() || d.equals("NM")) return null;
        char c = d.charAt(0);
        return Character.isLetter(c) ? String.valueOf(c) : null;
    }

    /** True if the call normalises to a German prefix block — kept for
     *  symmetry with the other helpers; scoring uses {@link #isGerman}. */
    public static boolean looksGerman(String rawCall) {
        String c = CallsignRegion.normalise(rawCall);
        return c.matches("^D[A-R][0-9].*");
    }
}
