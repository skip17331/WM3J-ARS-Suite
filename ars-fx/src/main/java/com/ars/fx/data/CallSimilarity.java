package com.ars.fx.data;

import java.util.Locale;

/**
 * How much a logged callsign resembles a typed one, for the contest "possible dupes" warning. What counts depends on how much has been typed:
 * <ul>
 *   <li>a call still being typed matches any logged call it is the start of (KC8R finds KC8RHC);</li>
 *   <li>a fragment of three or four characters matches a logged call that contains it anywhere, since the front of a call is often the part
 *       missed (RHC finds KC8RHC, W0M finds NW0M);</li>
 *   <li>five characters or more are taken as the whole call, which then has to be one letter off a logged call (a wrong, missing or extra
 *       letter: KC8RHQ, KC8RH, C8RHC for KC8RHC). A different call that merely shares three characters (W0MET and NW0M) is not a dupe.</li>
 * </ul>
 * A portable suffix (/P, /4) is ignored on both. No JavaFX, so it can be tested.
 */
public final class CallSimilarity {
    private CallSimilarity() {}

    /** The shortest typed call that is compared at all. */
    public static final int WINDOW = 3;

    /** 0 = not similar; higher is a closer match (3 starts the logged call or is one letter off it, 2 is a fragment inside it). */
    public static int score(String typed, String logged) {
        if (typed == null || logged == null) return 0;
        String t = base(typed), l = base(logged);
        if (t.length() < WINDOW || l.length() < WINDOW) return 0;
        if (t.equals(l)) return 4;
        if (l.startsWith(t)) return 3;                                   // still typing it
        if (t.length() <= 4) return l.contains(t) ? 2 : 0;               // a fragment
        return distanceAtMostOne(t, l) ? 3 : 0;                          // a whole call that is one letter off
    }

    private static String base(String call) {
        String c = call.trim().toUpperCase(Locale.ROOT);
        int slash = c.indexOf('/');
        return slash > 0 ? c.substring(0, slash) : c;
    }

    /** True when the two differ by one substituted, inserted or deleted character. */
    static boolean distanceAtMostOne(String a, String b) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > 1) return false;
        int i = 0;
        while (i < la && i < lb && a.charAt(i) == b.charAt(i)) i++;
        if (i == la && i == lb) return true;
        if (la == lb) return a.substring(i + 1).equals(b.substring(i + 1));
        return la > lb ? a.substring(i + 1).equals(b.substring(i)) : a.substring(i).equals(b.substring(i + 1));
    }
}
