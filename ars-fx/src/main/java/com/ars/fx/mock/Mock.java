package com.ars.fx.mock;

import java.util.List;

/** Mock data for the UI-first build. */
public final class Mock {
    private Mock() {}

    public record Mod(String id, String name, String hue, String sub, boolean running) {}

    /** Module list for the dock + J-Hub OPERATE group (order per the screenshots). */
    public static final List<Mod> MODULES = List.of(
        new Mod("log",    "J-Log",    "log",    "running",   true),
        new Mod("map",    "J-Map",    "map",    "running",   true),
        new Mod("bridge", "J-Bridge", "bridge", "running",   true),
        new Mod("digi",   "J-Digi",   "digi",   "Digital",   false),
        new Mod("sat",    "J-Sat",    "sat",    "Satellite", false),
        new Mod("vault",  "J-Vault",  "vault",  "Inventory", false),
        new Mod("learn",  "J-Learn",  "learn",  "Reference", false)
    );

    /** Standard solar tiles (key, value) for the Space-weather drawer. */
    public static final String[][] SOLAR = {
        {"SFI","168"},{"SN","142"},{"A","7"},{"K","2"},{"X-ray","B1"},{"304Å","171"},{"Aur","1.5"},{"MUF","28"}
    };
    /** Band conditions (band, day, night). */
    public static final String[][] BANDCOND = {
        {"80–40m","good","good"},{"30–20m","good","fair"},{"17–15m","fair","poor"},{"12–10m","fair","poor"}
    };
    public static final Object[][] ROTOR_PRESETS = {
        {"EU",50},{"AF",95},{"SA",155},{"Carib",135},{"AS",330},{"OC",250}
    };

    // ---- J-Log cockpit ----
    /** {utc, call, band, mode, snt, rcv, mult("y"|"")}. */
    public static final String[][] QSOS = {
        {"14:42","DL8WPX","20","SSB","59 05","59 14",""},
        {"14:41","ZD7BG","20","SSB","59 05","59 36","y"},
        {"14:40","VK9DX","20","SSB","59 05","59 30","y"},
        {"14:39","JA1XYZ","20","SSB","59 05","59 25",""},
        {"14:38","EA5K","20","SSB","59 05","59 14",""},
        {"14:37","G4ABC","20","SSB","59 05","59 14",""},
        {"14:36","PY2NY","20","SSB","59 05","59 11",""},
        {"14:35","VE7CC","20","SSB","59 05","59 03",""},
        {"14:33","CT1BOH","20","SSB","59 05","59 14",""},
        {"14:31","9M6XRO","20","SSB","59 05","59 28","y"},
    };
    /** {freq, call, me("y"|"")}. */
    public static final String[][] BANDMAP = {
        {"14.182","ZD7BG",""},{"14.205","VK9DX",""},{"14.225","CN2AA",""},{"14.250","WM3J","y"},
        {"14.263","PY2NY",""},{"14.285","9M6XRO",""},{"14.310","JA7QVI",""},{"14.332","5U5R",""},
    };
    public static final String[] BM_TICKS = {"14.150","14.200","14.250","14.300","14.350"};
    public static final String[] ZONES = {"14","36","30","25","11","3","28","15","16","20","8","33","40"};
    public static final int[] RATE = {3,2,4,5,3,6,4,5,7,5};
    /** {freq, call, mult("y"|""), age}. */
    public static final String[][] CLUSTER = {
        {"14.182","ZD7BG","y","1m"},{"14.205","VK9DX","y","2m"},{"14.225","CN2AA","y","3m"},
        {"14.263","PY2NY","","4m"},{"14.285","9M6XRO","y","5m"},{"14.310","JA7QVI","","6m"},{"14.332","5U5R","y","7m"},
    };
    public static final String[][] MACROS = {
        {"F1","CQ"},{"F2","Exch"},{"F3","TU"},{"F4","My Call"},{"F5","His Call"},{"F6","Repeat"},{"F7","Fill?"},{"F8","QRZ"},
    };

    // ---- J-Hub dashboard ----
    /** Mini-logger Today's log: {utc, call, freq, band, mode, rst}. */
    public static final String[][] TODAY_LOG = {
        {"14:42","DL8WPX","14.074.00","20m","FT8","-13"},
        {"14:31","VK9DX","14.074.00","20m","CW","559"},
        {"14:18","JA1XYZ","14.074.00","20m","SSB","59"},
        {"14:09","EA5K","14.074.00","20m","FT8","-08"},
        {"13:57","G4ABC","14.074.00","20m","CW","599"},
        {"13:44","PY2NY","14.074.00","20m","SSB","57"},
        {"13:30","VE7CC","14.074.00","20m","FT8","-04"},
    };
    /** DX cluster spots: {freq, call, need, mode, band, de, comment, age}. */
    public static final String[][] HUB_CLUSTER = {
        {"14.022.0","VK9DX","y","CW","20m","JA1ABC","up 2","1m"},
        {"21.295.0","ZD7BG","y","SSB","15m","G4XYZ","59 into EU","3m"},
        {"7.005.0","5U5R","y","CW","40m","W3LPL","ATNO!","4m"},
        {"14.074.0","DL8WPX","","FT8","20m","EA5K","-13 dB","5m"},
        {"18.100.0","F05QB","y","CW","17m","VE7CC","","7m"},
        {"28.495.0","PY2NY","","SSB","10m","K1TTT","strong","9m"},
        {"10.136.0","JA7QVI","","FT8","30m","N6TR","+02 dB","11m"},
        {"3.798.0","TF3IRA","","SSB","80m","OH2BH","gray line","14m"},
        {"24.915.0","EA8RKL","","CW","12m","F6BEE","","18m"},
    };
    /** Setup profiles: {name, hue}. */
    public static final String[][] PROFILES = {
        {"Contest Day","log"},{"Casual DX","map"},{"Digital","digi"},{"Satellite Pass","sat"}
    };
    public static final String[] RIG_BANDS = {"160","80","40","30","20","17","15","12","10","6"};
}
