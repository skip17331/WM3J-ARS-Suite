package com.ars.fx.data;

import java.util.Locale;

/**
 * The operator's unit system, chosen in J-Hub ▸ Station ▸ Units: {@link System#METRIC} (km, °C, km/h, hPa, m) or
 * {@link System#STANDARD} (US customary: mi, °F, mph, inHg, ft). Values stay metric everywhere internally; this only
 * changes what is shown. Angles (azimuth, elevation, bearings) and frequencies are unaffected.
 */
public final class Units {
    private Units() {}

    public enum System { METRIC, STANDARD }

    public static final String KEY = "units.system";
    private static final double KM_PER_MI = 1.609344, M_PER_FT = 0.3048;

    public static System system() { return "standard".equalsIgnoreCase(HubConfig.get(KEY, "metric")) ? System.STANDARD : System.METRIC; }
    public static boolean standard() { return system() == System.STANDARD; }
    public static void set(System s) { HubConfig.set(KEY, s == System.STANDARD ? "standard" : "metric"); }

    // ── distance (km) ───────────────────────────────────────────────────────
    public static String distUnit() { return standard() ? "mi" : "km"; }
    public static double dist(double km) { return standard() ? km / KM_PER_MI : km; }
    /** "1,234 km" or "767 mi". */
    public static String distText(double km) { return String.format(Locale.US, "%,d %s", Math.round(dist(km)), distUnit()); }
    /** Number only, grouped: "1,234" / "767". */
    public static String distNum(double km) { return String.format(Locale.US, "%,d", Math.round(dist(km))); }

    // ── speed ───────────────────────────────────────────────────────────────
    /** km/s (range rate): "−5.2" with the unit from {@link #kmsUnit()}. */
    public static String kmsUnit() { return standard() ? "mi/s" : "km/s"; }
    public static String kmsNum(double kmPerSec) { return String.format(Locale.US, "%+.1f", standard() ? kmPerSec / KM_PER_MI : kmPerSec); }
    public static String windUnit() { return standard() ? "mph" : "km/h"; }
    public static long wind(double kmh) { return Math.round(standard() ? kmh / KM_PER_MI : kmh); }

    // ── temperature / pressure / height ─────────────────────────────────────
    public static long temp(double c) { return Math.round(standard() ? c * 9.0 / 5.0 + 32.0 : c); }
    public static String tempText(double c) { return temp(c) + "°"; }
    public static String pressureText(double hPa) {
        return standard() ? String.format(Locale.US, "%.2f inHg", hPa * 0.0295299830714) : Math.round(hPa) + " hPa";
    }
    public static String heightUnit() { return standard() ? "ft" : "m"; }
    public static double heightFromMeters(double m) { return standard() ? m / M_PER_FT : m; }
    public static double heightToMeters(double shown) { return standard() ? shown * M_PER_FT : shown; }
}
