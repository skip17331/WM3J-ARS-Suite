package com.jlog.scoring;

import com.jlog.util.CallsignRegion;

import java.util.List;

/**
 * Classifies a callsign as an Asian station per the JARL All Asian DX
 * Contest "LIST OF ASIAN ENTITIES" (55 entities). Used for the contest's
 * asymmetric points and multiplier (Rule §7).
 *
 * <p>Claimed/running-score approximation — JARL re-adjudicates. Documented
 * coarse spots: Turkey (TA-TC) and Maldives (8Q) are credited Asian in
 * full although the rules count only their Asian area; JD1 is treated
 * Asian (Ogasawara) although Minami-tori Shima is Oceania — not separable
 * from a bare callsign; Asiatic Russia is split off European Russia by
 * the call-area digit (8/9/0 ⇒ Asian, 1-7 ⇒ not).
 */
public final class AsianEntities {

    private AsianEntities() {}

    /** JARL Asian-entity prefixes (ranges expanded). Sorted longest-first
     *  at match so 9M2/BV9P/VU4/XX9/JD1 beat their shorter relatives, and
     *  9M (East Malaysia = Oceania) is deliberately ABSENT. */
    private static final List<String> ASIAN = List.of(
        "BV9P","9M2","9M4","BS7","VU4","VU7","XX9","JD1","ZC4",
        "1S","3W","XV","4J","4K","4L","4S","4X","4Z","5B","C4","P3","7O","8Q",
        "9K","9N","9V","A4","A5","A6","A7","A9","AP","BV","B","E4","EK","EP",
        "EQ","EX","EY","EZ","HL","6K","6L","6M","6N","HS","E2","HZ",
        "JA","JB","JC","JD","JE","JF","JG","JH","JI","JJ","JK","JL","JM","JN",
        "JO","JP","JQ","JR","JS","7J","7K","7L","7M","7N","8J","8K","8L","8M",
        "8N","JT","JU","JV","JY","OD","P5","S2","TA","TB","TC",
        "UJ","UK","UL","UM","UN","UO","UP","UQ","VR","VU","XU","XW","XY","XZ",
        "YA","YI","YK"
    );

    /** True if the worked station is an Asian entity per the JARL list. */
    public static boolean isAsian(String rawCall) {
        if (rawCall == null || rawCall.isBlank()) return false;
        String c = CallsignRegion.normalise(rawCall);
        if (c.isEmpty()) return false;
        // Russia (R* / U[A-I]): Asian only for call areas 8, 9, 0. Uzbekistan
        // (UJ-UM) / Kazakhstan (UN-UQ) are always Asian and fall to the list.
        if (c.matches("^(R[A-Z]?|U[A-I]).*") && !c.matches("^U[J-Q].*")) {
            int a = areaDigit(c);
            return a == 8 || a == 9 || a == 0;
        }
        for (String p : sortedLongestFirst())
            if (c.startsWith(p)) return true;
        return false;
    }

    // cache the longest-first ordering
    private static List<String> sorted;
    private static List<String> sortedLongestFirst() {
        if (sorted == null) {
            var l = new java.util.ArrayList<>(ASIAN);
            l.sort((x, y) -> y.length() - x.length());
            sorted = List.copyOf(l);
        }
        return sorted;
    }

    /** Call-area digit: explicit /N portable wins, else the last digit of
     *  the leading prefix block (strip trailing suffix letters). */
    private static int areaDigit(String norm) {
        int i = norm.length();
        while (i > 0 && Character.isLetter(norm.charAt(i - 1))) i--;
        if (i > 0 && Character.isDigit(norm.charAt(i - 1))) return norm.charAt(i - 1) - '0';
        for (char ch : norm.toCharArray())
            if (Character.isDigit(ch)) return ch - '0';
        return -1;
    }
}
