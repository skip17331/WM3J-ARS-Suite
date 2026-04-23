package com.jlog.util;

/**
 * Coarse region classifier for amateur-radio callsigns. Used by contest plugins
 * (notably ARRL 10M) to pick which received exchange fields are relevant —
 * US / Canada stations send state or province, DX sends a serial number.
 *
 * The classifier is prefix-based and intentionally simple. For ARRL contests
 * the 50 US states + DC + AK + HI count as {@link Region#US}; everything else
 * under the US flag (Puerto Rico, Guam, etc.) remains DX because it submits a
 * DX-style exchange. Portable indicators ("/P", "/M", "/QRP", "/MM", "/AM",
 * numeric call-area reassignments) are stripped before classification.
 */
public final class CallsignRegion {

    public enum Region { US, CANADA, DX }

    private CallsignRegion() {}

    public static Region classify(String raw) {
        String call = normalise(raw);
        if (call.isEmpty()) return Region.DX;

        // Canadian prefixes: VA, VE, VO, VY, CY plus the rarely used CF-CK, XJ-XO blocks.
        // Check Canada first because "CY" and "CF" alphabetically clash with "K/N/W"-free DX.
        if (call.startsWith("VE") || call.startsWith("VA") || call.startsWith("VO")
                || call.startsWith("VY") || call.startsWith("CY")
                || call.startsWith("CF") || call.startsWith("CG") || call.startsWith("CH")
                || call.startsWith("CI") || call.startsWith("CJ") || call.startsWith("CK")
                || call.startsWith("XJ") || call.startsWith("XK") || call.startsWith("XL")
                || call.startsWith("XM") || call.startsWith("XN") || call.startsWith("XO")) {
            return Region.CANADA;
        }

        // US prefixes: K, N, W single-letter, plus AA-AL double-letter.
        // Special-case Alaska (KL) and Hawaii (KH6/AH6/WH6/NH6/KH7K) which count as US
        // states in ARRL contests. Other KH*/KP*/NP*/WP* entities are DX.
        if (call.startsWith("KL") || call.matches("^(KH|AH|NH|WH)[67].*")) return Region.US;
        if (call.matches("^(KH|KP|NP|WP|KG4|KC4|KC6|KX|KY|KV4)[0-9].*")) return Region.DX;

        char c0 = call.charAt(0);
        if (c0 == 'K' || c0 == 'N' || c0 == 'W') return Region.US;
        if (call.length() >= 2) {
            char c1 = call.charAt(1);
            // AA-AL two-letter US prefixes
            if (c0 == 'A' && c1 >= 'A' && c1 <= 'L') return Region.US;
        }

        return Region.DX;
    }

    public static boolean isUs(String call)     { return classify(call) == Region.US; }
    public static boolean isCanada(String call) { return classify(call) == Region.CANADA; }
    public static boolean isDx(String call)     { return classify(call) == Region.DX; }

    /**
     * Returns the letter-cluster before the first digit of a callsign — a coarse
     * DXCC-entity proxy used by single-exchange contests (e.g. ARRL 160M) where
     * DX stations send the literal string "DX" and the logger must differentiate
     * countries from the call prefix alone.
     *
     * Examples: JA1ABC → JA, PY2XYZ → PY, G0ABC → G, DL1ABC → DL.
     *
     * This is an approximation: M* and G* are both UK but will register as
     * distinct prefixes, while JA1 and JA9 correctly collapse to one Japan entry.
     * Swap in a proper DXCC database when one is available in the engine.
     */
    public static String dxccPrefix(String raw) {
        String call = normalise(raw);
        if (call.isEmpty()) return "";
        StringBuilder pre = new StringBuilder();
        for (char c : call.toCharArray()) {
            if (Character.isLetter(c)) pre.append(c);
            else break;
        }
        return pre.toString();
    }

    /** WPX prefix — letters + first digit. K1ABC → K1, JA9ABC → JA9, DL/W1AB → DL.
     *  W1AW/7 → W7 (portable-digit override), VE3/W1AW → VE3 (prefix override).
     *  Used by CQ WPX contests where each prefix counts as a multiplier. */
    public static String wpxPrefix(String raw) {
        if (raw == null) return "";
        String call = raw.trim().toUpperCase();
        if (call.isEmpty()) return "";
        if (call.contains("/")) {
            String[] parts = call.split("/");
            String left  = parts[0];
            String right = parts.length > 1 ? parts[parts.length - 1] : "";
            // Strip common modifier suffixes (W1AW/P → W1AW) — checked FIRST so
            // "P" / "M" don't get mistaken for country prefixes below.
            if (right.matches("P|M|MM|AM|QRP|MOBILE|PORTABLE")) {
                call = left;
            }
            // Portable digit override: W1AW/7 → letters-of-left + that digit = W7
            else if (right.matches("[0-9]")) {
                StringBuilder letters = new StringBuilder();
                for (char c : left.toCharArray()) {
                    if (Character.isLetter(c)) letters.append(c);
                    else break;
                }
                return letters + right;
            }
            // Country-prefix override: VE3/W1AW → VE3 or KP2/W1AW → KP2
            else if (right.matches("[A-Z]{2,3}[0-9]?")) return wpxPrefix(right);
            else {
                call = left;
            }
        }
        int i = 0;
        while (i < call.length() && Character.isLetter(call.charAt(i))) i++;
        if (i < call.length() && Character.isDigit(call.charAt(i))) {
            return call.substring(0, i + 1);
        }
        return call.substring(0, i);
    }

    /**
     * Strip portable / compound call indicators, returning the "effective" call
     * for classification purposes. Rules:
     *   W1AW/P   → W1AW  (stripping modifier suffix)
     *   W1AW/7   → W1AW  (call-area reassignment — treat as base)
     *   VE3/W1AW → VE3   (compound prefix override, left is country)
     *   W1AW/JA  → JA    (country override on right)
     *   K1ABC    → K1ABC (no change)
     */
    static String normalise(String raw) {
        if (raw == null) return "";
        String call = raw.trim().toUpperCase();
        if (!call.contains("/")) return call;
        String[] parts = call.split("/");
        String left  = parts[0];
        String right = parts.length > 1 ? parts[parts.length - 1] : "";
        boolean rightIsModifier = right.matches("[0-9]|P|M|MM|AM|QRP|MOBILE|PORTABLE");
        if (rightIsModifier) return left;
        boolean leftIsPrefix  = left .matches("[A-Z]{1,3}[0-9]?");
        boolean rightIsPrefix = right.matches("[A-Z]{1,3}[0-9]?");
        if (rightIsPrefix && !leftIsPrefix) return right;
        if (leftIsPrefix  && !rightIsPrefix) return left;
        return left;
    }
}
