package com.ars.fx.data;

import java.util.Locale;

/**
 * How much a logged callsign resembles a half-typed one, for the contest "possible dupes" warning: you do not always get the whole call at once, so
 * any three contiguous characters of the typed call that also occur in a logged call count (KC8RHC finds anything with KC8, C8R, 8RH or RHC).
 * No JavaFX, so it can be tested.
 */
public final class CallSimilarity {
    private CallSimilarity() {}

    /** The length of the piece that has to match. */
    public static final int WINDOW = 3;

    /** How many of the typed call's three-character pieces the logged call contains; 0 = not similar. Typed shorter than three never matches. */
    public static int score(String typed, String logged) {
        if (typed == null || logged == null) return 0;
        String t = typed.trim().toUpperCase(Locale.ROOT), l = logged.trim().toUpperCase(Locale.ROOT);
        if (t.length() < WINDOW || l.length() < WINDOW) return 0;
        int n = 0;
        for (int i = 0; i + WINDOW <= t.length(); i++) if (l.contains(t.substring(i, i + WINDOW))) n++;
        return n;
    }
}
