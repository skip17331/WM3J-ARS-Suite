package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

/**
 * Oceania DX Contest (OCDX) helper. "Oceania" for OCDX is the set of
 * Oceania DXCC entities in the ARRL country list (Rule 3). That is the
 * DxccResolver OC continent, PLUS the US Pacific group (Hawaii KH6,
 * Guam KH2, Mariana KH0, Wake KH9, American Samoa KH8, … — the
 * KH/AH/NH/WH + digit prefixes) which the generic continent table
 * files under North America but the ARRL DXCC list places in Oceania.
 * Alaska (KL) and the Caribbean US entities (KP) are correctly NOT
 * Oceania. Claimed/running — the OCDX committee re-adjudicates.
 */
public final class OceaniaDx {

    private OceaniaDx() {}

    /** True if the call is an Oceania station per the ARRL DXCC list. */
    public static boolean isOceania(String rawCall) {
        if (rawCall == null) return false;
        if ("OC".equals(DxccResolver.getInstance().continentOf(rawCall))) return true;
        String c = CallsignRegion.normalise(rawCall);
        // US Pacific (KH6/AH6/NH6/WH6 …): 2nd letter H + a digit.
        return c.matches("^[AKNW]H[0-9].*");
    }

    /** OCDX contact points for a band (Rule 10): 160=20, 80=10, 40=5,
     *  20=1, 15=2, 10=3; 0 for any non-contest band. */
    public static int bandPoints(String band) {
        return switch (band == null ? "" : band) {
            case "160m" -> 20;
            case "80m"  -> 10;
            case "40m"  -> 5;
            case "20m"  -> 1;
            case "15m"  -> 2;
            case "10m"  -> 3;
            default     -> 0;
        };
    }
}
