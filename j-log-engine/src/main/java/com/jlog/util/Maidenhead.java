package com.jlog.util;

/**
 * Maidenhead locator (grid-square) helpers. The 4-character form used by
 * most contests is two uppercase fields (A-R) followed by two digits (0-9),
 * e.g. "FN10", "JO65", "DM79". 6-character form adds two more letters (a-x).
 */
public final class Maidenhead {
    private Maidenhead() {}

    public static boolean isValid4(String s) {
        if (s == null) return false;
        String t = s.trim().toUpperCase();
        return t.matches("[A-R]{2}[0-9]{2}");
    }

    public static boolean isValid6(String s) {
        if (s == null) return false;
        String t = s.trim();
        // Field letters uppercase, sub-square letters lowercase per spec; accept both for lenience.
        return t.matches("[A-Ra-r]{2}[0-9]{2}[A-Xa-x]{2}");
    }

    /** True if {@code s} is a valid 4- or 6-char grid. */
    public static boolean isValid(String s) {
        return isValid4(s) || isValid6(s);
    }

    /** Returns the 4-char field for any valid grid, or "" if invalid. */
    public static String field4(String s) {
        if (s == null) return "";
        String t = s.trim().toUpperCase();
        if (t.length() >= 4 && t.substring(0, 4).matches("[A-R]{2}[0-9]{2}")) {
            return t.substring(0, 4);
        }
        return "";
    }
}
