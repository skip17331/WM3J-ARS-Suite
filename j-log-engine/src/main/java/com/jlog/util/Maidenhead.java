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

    /**
     * Geographic center of a 4-, 6-, 8- or 10-character Maidenhead grid as
     * {@code {latitudeDeg, longitudeDeg}}, or {@code null} if the grid is not
     * valid locator. 6-char input is resolved to the subsquare
     * center; 4-char to the square center. Used by distance-scored contests.
     */
    public static double[] center(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase();
        if (t.matches("[A-R]{2}[0-9]{2}[A-X]{2}[0-9]{2}([A-X]{2})?")) return extended(t);   // 8 / 10 characters
        boolean six = isValid6(t);
        if (!six && !isValid4(t)) return null;

        int lonField = t.charAt(0) - 'A';   // 20° wide
        int latField = t.charAt(1) - 'A';   // 10° wide
        int lonSq    = t.charAt(2) - '0';   //  2° wide
        int latSq    = t.charAt(3) - '0';   //  1° wide

        double lon = -180 + lonField * 20 + lonSq * 2;
        double lat =  -90 + latField * 10 + latSq * 1;

        if (six) {
            int lonSub = Character.toLowerCase(t.charAt(4)) - 'a'; // 2°/24 wide
            int latSub = Character.toLowerCase(t.charAt(5)) - 'a'; // 1°/24 wide
            lon += (lonSub + 0.5) * (2.0 / 24.0);
            lat += (latSub + 0.5) * (1.0 / 24.0);
        } else {
            lon += 1.0;   // center of the 2° square
            lat += 0.5;   // center of the 1° square
        }
        return new double[] { lat, lon };
    }

    /**
     * Centre of an 8- or 10-character locator (FN20xr45, FN20xr45ab). Station setup takes any grid, and an operator who entered
     * a precise 8-character one used to get no location at all: no bearings, distances or home point on the map.
     */
    private static double[] extended(String t) {
        double lon = -180 + (t.charAt(0) - 'A') * 20 + (t.charAt(2) - '0') * 2 + (t.charAt(4) - 'A') * (2.0 / 24) + (t.charAt(6) - '0') * (2.0 / 240);
        double lat =  -90 + (t.charAt(1) - 'A') * 10 + (t.charAt(3) - '0')     + (t.charAt(5) - 'A') * (1.0 / 24) + (t.charAt(7) - '0') * (1.0 / 240);
        double w = 2.0 / 240, h = 1.0 / 240;                                     // the 8-character square
        if (t.length() == 10) {
            lon += (t.charAt(8) - 'A') * (w / 24); lat += (t.charAt(9) - 'A') * (h / 24);
            w /= 24; h /= 24;
        }
        return new double[] { lat + h / 2, lon + w / 2 };
    }

    /**
     * Great-circle distance in km between the centers of two Maidenhead grids
     * (spherical earth, R=6371 km — the convention ARRL/VHF scoring uses).
     * Returns -1 if either grid is invalid (caller decides how to score that).
     */
    public static double distanceKm(String gridA, String gridB) {
        double[] a = center(gridA);
        double[] b = center(gridB);
        if (a == null || b == null) return -1;
        double R    = 6371.0;
        double dLat = Math.toRadians(b[0] - a[0]);
        double dLon = Math.toRadians(b[1] - a[1]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(a[0])) * Math.cos(Math.toRadians(b[0]))
                   * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }
}
