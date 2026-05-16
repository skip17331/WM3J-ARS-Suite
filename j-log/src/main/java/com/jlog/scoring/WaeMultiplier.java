package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

import java.util.List;

/**
 * Resolves a worked callsign to its WAE-DC multiplier token (Rule §6).
 *
 * <ul>
 *   <li><b>European</b> station (DxccResolver continent EU / on the WAE
 *       Country List): token = the matching WAE Country List prefix
 *       (longest-prefix match).</li>
 *   <li><b>Non-European</b> station: token = its DXCC entity, EXCEPT the
 *       eleven entities split by numerical call area
 *       (W, VE, VK, ZL, ZS, JA, BY, PY, RA8/RA9/RAØ) where the token is
 *       the country letter-group + call-area digit.</li>
 * </ul>
 *
 * <p>Tokens are namespaced (<code>E:</code> / <code>C:</code> /
 * <code>X:</code>) so the three spaces never collide when counted
 * distinct. This is a claimed/running-score approximation — the WAEDC
 * committee re-adjudicates. Known coarse spots (documented): Shetland
 * GM/s, Bear Is JW/b and Mt Athos SV/A are not separable from a bare
 * callsign so fold into GM/JW/SV; Russian Asiatic split keys off the
 * call-area digit (8/9/Ø ⇒ non-EU RA8/9/0, 2 ⇒ Kaliningrad RA2,
 * 1/3-7 ⇒ European RA).
 */
public final class WaeMultiplier {

    private WaeMultiplier() {}

    /** WAE Country List (Rule §6), Ø→0, slash entities folded to base.
     *  Sorted longest-first at use so e.g. IT/IS beat I, OH0 beats OH. */
    private static final List<String> WAE_PREFIXES = List.of(
        "1A0","4U1I","4U1V","OH0","OJ0","HB0","EA6","SV5","SV9","R1F","RA2",
        "TA1","3A","4O","9A","9H","C3","CT","CU","DL","E7","EA","EI","ER",
        "ES","EU","GD","GI","GJ","GM","GU","GW","HA","HB","HV","IS","IT",
        "JW","JX","LA","LX","LY","LZ","OE","OH","OK","OM","ON","OY","OZ",
        "PA","RA","S5","SM","SP","SV","T7","TF","TK","UR","YL","YO","YU",
        "Z6","Z3","ZA","ZB","F","G","I"
    );

    /** Namespaced WAE multiplier token, or null if unresolvable. */
    public static String token(String rawCall) {
        if (rawCall == null || rawCall.isBlank()) return null;
        String call = CallsignRegion.normalise(rawCall);
        if (call.isEmpty()) return null;
        int area = callAreaDigit(rawCall, call);

        // Russia: split by call-area digit irrespective of the coarse
        // continent the prefix table reports.
        if (isRussia(call)) {
            if (area == 8 || area == 9 || area == 0) return "C:RA" + area; // Asiatic (non-EU)
            if (area == 2)                            return "E:RA2";       // Kaliningrad
            return "E:RA";                                                 // European Russia
        }

        String continent = com.jlog.scoring.DxccResolver.getInstance().continentOf(rawCall);
        boolean european = "EU".equals(continent) || matchesWae(call) != null;

        if (european) {
            String w = matchesWae(call);
            return w != null ? "E:" + w : "E:" + dxcc(rawCall);
        }
        // Non-European: call-area split for the eleven, else DXCC entity.
        String fam = splitFamily(call);
        if (fam != null) return "C:" + fam + area;
        return "X:" + dxcc(rawCall);
    }

    /** Band-weight bonus for the WAE multiplier (Rule §6). */
    public static int bandWeight(String band) {
        if (band == null) return 0;
        return switch (band) {
            case "80m" -> 4;   // 3.5 MHz
            case "40m" -> 3;   // 7 MHz
            case "20m", "15m", "10m" -> 2;  // 14/21/28 MHz
            default -> 0;      // out-of-contest band — no mult credit
        };
    }

    // ---- helpers -------------------------------------------------------

    private static String dxcc(String rawCall) {
        String id = DxccResolver.getInstance().entityOf(rawCall);
        return id == null ? "?" : id;
    }

    /** Longest WAE-list prefix the (normalised) call starts with, or null. */
    private static String matchesWae(String call) {
        String best = null;
        for (String p : WAE_PREFIXES)
            if (call.startsWith(p) && (best == null || p.length() > best.length()))
                best = p;
        return best;
    }

    private static boolean isRussia(String call) {
        return call.matches("^(R|RA|RW|RX|RU|RZ|RK|RN|RM|RO|RT|RV|RD|RG|RL|RQ|RY"
                + "|UA|UB|UC|UD|UE|UF|UG|UH|UI).*");
    }

    /** Country letter-group for the eleven call-area-split entities, else null. */
    private static String splitFamily(String call) {
        if (call.matches("^(A[A-L]|[KNW]).*"))                       return "W";
        if (call.matches("^(VE|VA|VO|VY|CY|CZ|C[F-K]|X[J-O]).*"))    return "VE";
        if (call.matches("^(VK|AX).*"))                              return "VK";
        if (call.matches("^(ZL|ZM|ZK).*"))                           return "ZL";
        if (call.matches("^(ZS|ZR|ZT|ZU).*"))                        return "ZS";
        if (call.matches("^(J[A-S]|7[J-N]|8[J-N]).*"))               return "JA";
        if (call.matches("^B.*"))                                    return "BY";
        if (call.matches("^(P[P-Y]|Z[V-Z]).*"))                      return "PY";
        return null;
    }

    /** Call-area digit: an explicit /N portable suffix wins; otherwise the
     *  LAST digit of the leading prefix block — i.e. strip the trailing
     *  letter suffix, the area digit is what remains' last char (handles
     *  digit-leading prefixes like 7M4 → 4, not 7). Defaults to 0. */
    private static int callAreaDigit(String raw, String norm) {
        String up = raw.trim().toUpperCase();
        java.util.regex.Matcher m =
            java.util.regex.Pattern.compile("/([0-9])$").matcher(up);
        if (m.find()) return m.group(1).charAt(0) - '0';
        int i = norm.length();
        while (i > 0 && Character.isLetter(norm.charAt(i - 1))) i--;   // drop suffix letters
        if (i > 0 && Character.isDigit(norm.charAt(i - 1))) return norm.charAt(i - 1) - '0';
        for (char c : norm.toCharArray())                              // fallback
            if (Character.isDigit(c)) return c - '0';
        return 0;
    }
}
