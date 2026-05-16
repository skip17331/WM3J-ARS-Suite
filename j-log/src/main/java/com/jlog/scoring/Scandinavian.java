package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Scandinavian Activity Contest helpers (Rule §2/§8). Classifies a call
 * as Scandinavian (the §2 prefix list — Greenland included though it is
 * geographically NA) and builds the non-Scandinavian-entrant multiplier
 * token = Scandinavian-DXCC-entity + call-area number (§8.2: SI3/SK3/
 * SM3/7S3/8S3 → one mult; no number → 0th area; ≥2 numbers → first).
 * Claimed/running approximation — the SAC committee re-adjudicates.
 */
public final class Scandinavian {

    private Scandinavian() {}

    /** prefix → entity key. Ordered longest-first (OH0/OF0/OG0 = Åland
     *  before OH/OF/OG = Finland; OJ0 Market Reef; etc.). */
    private static final Map<String, String> PFX = new LinkedHashMap<>();
    static {
        for (String p : new String[]{"OH0","OF0","OG0"}) PFX.put(p, "AX");   // Åland
        PFX.put("OJ0", "OJ0");                                               // Market Reef
        for (String p : new String[]{"JW"}) PFX.put(p, "JW");                // Svalbard/Bear
        for (String p : new String[]{"JX"}) PFX.put(p, "JX");                // Jan Mayen
        for (String p : new String[]{"LA","LB","LC","LG","LI","LJ","LN"}) PFX.put(p,"NO");
        for (String p : new String[]{"OF","OG","OH","OI"}) PFX.put(p, "FI");
        for (String p : new String[]{"OX","XP"}) PFX.put(p, "GL");           // Greenland
        for (String p : new String[]{"OW","OY"}) PFX.put(p, "FO");           // Faroe
        for (String p : new String[]{"5P","5Q","OU","OV","OZ"}) PFX.put(p,"DK");
        for (String p : new String[]{"7S","8S","SA","SB","SC","SD","SE","SF",
                "SG","SH","SI","SJ","SK","SL","SM"}) PFX.put(p, "SE");
        PFX.put("TF", "IS");                                                 // Iceland
    }

    /** True if the call is a Scandinavian station per Rule §2. */
    public static boolean isScandinavian(String rawCall) {
        return entityKey(rawCall) != null;
    }

    /** Scandinavian entity key, or null if not Scandinavian. Longest
     *  prefix wins so OH0 (Åland) beats OH (Finland). */
    public static String entityKey(String rawCall) {
        if (rawCall == null) return null;
        String c = CallsignRegion.normalise(rawCall);
        if (c.isEmpty()) return null;
        String best = null, bestKey = null;
        for (Map.Entry<String, String> e : PFX.entrySet()) {
            String p = e.getKey();
            if (c.startsWith(p) && (best == null || p.length() > best.length())) {
                best = p; bestKey = e.getValue();
            }
        }
        return bestKey;
    }

    /** Non-Scandinavian-entrant multiplier token (§8.2): entity + first
     *  call-area digit (0 if the call carries no number). */
    public static String multToken(String rawCall) {
        String key = entityKey(rawCall);
        if (key == null) return null;
        String c = CallsignRegion.normalise(rawCall);
        int d = 0;
        for (char ch : c.toCharArray())
            if (Character.isDigit(ch)) { d = ch - '0'; break; }   // FIRST number (§8.2)
        return key + d;
    }
}
