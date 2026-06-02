package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

/**
 * Russian DX Contest helpers (Rule §7/§9). Classifies a callsign as a
 * Russian station, resolves its scoring continent (European vs Asiatic
 * Russia by call-area digit; Kaliningrad = European per §7.3), and gives
 * the country-multiplier token treating UA2F (Kaliningrad), RI1FJ (Franz
 * Josef Land) and RI1AN (Russian Antarctic) as separate DXCC entities
 * (§7.3). Claimed/running approximation — the RDXC committee
 * re-adjudicates; WAE-extra country splits are approximated by DXCC.
 */
public final class RussianDx {

    private RussianDx() {}

    /** True if the call is a Russian station (incl. UA2F / RI1FJ / RI1AN). */
    public static boolean isRussian(String rawCall) {
        if (rawCall == null) return false;
        String c = CallsignRegion.normalise(rawCall);
        // RA-RZ / R* and UA-UI (Russia); UJ-UQ are Uzbekistan/Kazakhstan.
        return c.matches("^(R[A-Z]?[0-9].*|R[0-9].*|U[A-I][0-9].*)")
            || c.startsWith("RI1");
    }

    /** Scoring continent of a Russian call: "AS" if Asiatic (call-area
     *  digit 8/9/0), Antarctica ("AN") for RI1AN, else "EU" (incl.
     *  Kaliningrad UA2 and Franz Josef RI1FJ per §7.3). */
    public static String russianContinent(String rawCall) {
        String c = CallsignRegion.normalise(rawCall);
        if (c.startsWith("RI1AN")) return "AN";
        if (c.startsWith("RI1FJ")) return "EU";
        int a = areaDigit(c);
        return (a == 8 || a == 9 || a == 0) ? "AS" : "EU";
    }

    /** Country-multiplier token (§7.3 / §9): the special entities are
     *  distinct; otherwise the DxccResolver entity id. */
    public static String countryToken(String rawCall) {
        String c = CallsignRegion.normalise(rawCall);
        if (c.startsWith("RI1AN")) return "RI1AN";
        if (c.startsWith("RI1FJ")) return "RI1FJ";
        if (isKaliningrad(c))      return "UA2F";
        String e = DxccResolver.getInstance().entityOf(rawCall);
        return e == null ? "?" : e;
    }

    /** Kaliningrad (UA2 / RA2 / R2 area-2). Separate DXCC + oblast,
     *  scores as European Russia (§7.3). */
    public static boolean isKaliningrad(String c) {
        return c.matches("^(UA2|RA2|RN2|RK2|RU2|RW2|RX2|RZ2|R2)[A-Z].*")
            || c.matches("^(UA2|RA2|R2)F.*");
    }

    private static int areaDigit(String norm) {
        int i = norm.length();
        while (i > 0 && Character.isLetter(norm.charAt(i - 1))) i--;
        if (i > 0 && Character.isDigit(norm.charAt(i - 1))) return norm.charAt(i - 1) - '0';
        for (char ch : norm.toCharArray())
            if (Character.isDigit(ch)) return ch - '0';
        return -1;
    }
}
