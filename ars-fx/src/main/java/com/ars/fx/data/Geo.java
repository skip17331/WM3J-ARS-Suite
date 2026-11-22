package com.ars.fx.data;

/**
 * Approximate latitude/longitude for a callsign — used to place DX spots and RBN
 * skimmers on the J-Map. The full DXCC prefix data decides the country; the table
 * below only refines the USA to its call areas and catches what DXCC can't resolve.
 */
public final class Geo {
    private Geo() {}

    // prefix → {lat, lon} centroid; longest-prefix wins.
    private static final String[][] PFX = {
        {"W1","42.3","-72.0"},{"W2","41.0","-74.5"},{"W3","39.5","-77.5"},{"W4","34.0","-82.0"},{"W5","32.0","-97.0"},
        {"W6","37.0","-120.0"},{"W7","44.0","-117.0"},{"W8","41.0","-83.0"},{"W9","41.5","-88.0"},{"W0","41.0","-95.0"},
        {"K1","42.3","-72.0"},{"K2","41.0","-74.5"},{"K3","39.5","-77.5"},{"K4","34.0","-82.0"},{"K5","32.0","-97.0"},
        {"K6","37.0","-120.0"},{"K7","44.0","-117.0"},{"K8","41.0","-83.0"},{"K9","41.5","-88.0"},{"K0","41.0","-95.0"},
        {"N1","42.3","-72.0"},{"N2","41.0","-74.5"},{"N3","39.5","-77.5"},{"N4","34.0","-82.0"},{"N6","37.0","-120.0"},
        {"KH6","20.8","-156.5"},{"AH6","20.8","-156.5"},{"NH6","20.8","-156.5"},{"WH6","20.8","-156.5"},   // filed as USA by the DXCC data
        {"KL","64.0","-152.0"},{"AL","64.0","-152.0"},{"NL","64.0","-152.0"},{"WL","64.0","-152.0"},
        {"KP4","18.2","-66.5"},{"NP4","18.2","-66.5"},{"WP4","18.2","-66.5"},
        {"AA","39.0","-95.0"},{"K","39.0","-95.0"},{"N","39.0","-95.0"},{"W","39.0","-95.0"},  // generic US fallback
        {"VE","56.0","-106.0"},{"VA","50.0","-100.0"},{"VO","48.0","-56.0"},{"VY","64.0","-110.0"},{"XE","23.0","-102.0"},
        {"DL","51.0","10.0"},{"DK","51.0","10.0"},{"DJ","51.0","10.0"},{"DF","51.0","10.0"},
        {"G","53.0","-1.5"},{"M","53.0","-1.5"},{"2E","53.0","-1.5"},{"GM","56.5","-4.0"},{"GW","52.3","-3.7"},{"EI","53.4","-8.0"},
        {"F","47.0","2.0"},{"I","42.0","12.0"},{"EA","40.0","-4.0"},{"CT","39.5","-8.0"},{"ON","50.6","4.6"},{"PA","52.0","5.3"},
        {"OE","47.5","14.0"},{"HB","47.0","8.0"},{"OZ","56.0","10.0"},{"SM","60.0","15.0"},{"LA","61.0","9.0"},{"OH","62.0","26.0"},
        {"SP","52.0","19.0"},{"OK","49.8","15.0"},{"S5","46.0","15.0"},{"9A","45.0","16.0"},{"YO","46.0","25.0"},{"LZ","42.7","25.0"},
        {"SV","39.0","22.0"},{"UA","56.0","38.0"},{"UR","49.0","32.0"},{"EU","53.7","27.5"},{"ES","58.6","25.0"},{"YL","57.0","24.6"},
        {"LY","55.2","23.9"},{"TA","39.0","35.0"},{"4X","31.5","35.0"},{"RA","56.0","38.0"},{"RN","56.0","38.0"},{"RW","56.0","38.0"},
        {"JA","36.0","138.0"},{"JE","36.0","138.0"},{"JF","36.0","138.0"},{"JH","36.0","138.0"},{"JR","36.0","138.0"},
        {"BY","35.0","105.0"},{"BG","35.0","105.0"},{"BA","35.0","105.0"},{"HL","36.5","128.0"},{"DS","36.5","128.0"},
        {"VR2","22.3","114.2"},{"9M","3.0","102.0"},{"HS","15.0","101.0"},{"YB","-2.0","118.0"},{"VU","21.0","78.0"},
        {"VK","-25.0","134.0"},{"ZL","-41.0","174.0"},{"FK","-21.5","165.5"},
        {"ZS","-29.0","24.0"},{"5Z","-1.0","37.0"},{"CN","32.0","-6.0"},{"SU","26.0","30.0"},{"EA8","28.3","-15.5"},
        {"PY","-10.0","-55.0"},{"LU","-34.0","-64.0"},{"CE","-33.0","-71.0"},{"HK","4.0","-73.0"},{"YV","8.0","-66.0"},
        {"OA","-10.0","-76.0"},{"CX","-33.0","-56.0"},{"PJ","12.2","-69.0"},{"FY","4.0","-53.0"},
    };

    private static final int DXCC_USA = 291;

    /** {lat, lon} for a callsign (RBN "-#" suffix stripped), or null when nothing recognises it. */
    public static double[] of(String call) {
        if (call == null || call.isEmpty()) return null;
        DxccInfo.Info dxcc = DxccInfo.of(call.toUpperCase().replaceAll("-.*", ""));   // W3LPL-# → W3LPL
        boolean located = dxcc != null && !Double.isNaN(dxcc.lat()) && !Double.isNaN(dxcc.lon());
        if (located && dxcc.dxcc() != DXCC_USA) return new double[]{dxcc.lat(), dxcc.lon()};
        double[] prefix = byPrefix(call);
        return prefix != null || !located ? prefix : new double[]{dxcc.lat(), dxcc.lon()};
    }

    private static double[] byPrefix(String call) {
        String c = call.toUpperCase().replaceAll("[-/].*", "");   // W3LPL-# → W3LPL, F/K1ABC → F
        double[] best = null; int bl = 0;
        for (String[] e : PFX) {
            if (c.startsWith(e[0]) && e[0].length() > bl) { best = new double[]{Double.parseDouble(e[1]), Double.parseDouble(e[2])}; bl = e[0].length(); }
        }
        if (bl <= 2 && (best == null || GENERIC_US.contains(c.substring(0, Math.min(bl, 2))))) {   // WA7XYZ, KB6ABC, N7ABC: the call area is the digit, not the first letter
            java.util.regex.Matcher m = US_AREA.matcher(c);
            if (m.find()) { double[] a = AREA[m.group(1).charAt(0) - '0']; return new double[]{a[0], a[1]}; }
        }
        return best;
    }

    private static final java.util.Set<String> GENERIC_US = java.util.Set.of("K", "N", "W", "AA");
    private static final java.util.regex.Pattern US_AREA = java.util.regex.Pattern.compile("^(?:[KNW][A-Z]?|A[A-L])(\\d)[A-Z]");
    private static final double[][] AREA = {{41.0,-95.0},{42.3,-72.0},{41.0,-74.5},{39.5,-77.5},{34.0,-82.0},{32.0,-97.0},{37.0,-120.0},{44.0,-117.0},{41.0,-83.0},{41.5,-88.0}};

    // ---- a better place for a US station: its own grid, section or state from the call history --------------------

    private static final java.util.Map<String, double[]> STATE = new java.util.HashMap<>();
    static {
        String[] t = {"AL 32.8 -86.8","AZ 34.2 -111.7","AR 34.9 -92.4","CA 37.2 -119.5","CO 39.0 -105.5","CT 41.6 -72.7","DE 39.0 -75.5","FL 28.6 -82.4",
            "GA 32.7 -83.4","ID 44.4 -114.6","IL 40.0 -89.2","IN 39.9 -86.3","IA 42.0 -93.5","KS 38.5 -98.4","KY 37.5 -85.3","LA 31.0 -92.0","ME 45.3 -69.2",
            "MD 39.0 -76.8","MA 42.3 -71.8","MI 44.3 -85.4","MN 46.3 -94.3","MS 32.7 -89.7","MO 38.4 -92.5","MT 47.0 -109.6","NE 41.5 -99.8","NV 39.3 -116.6",
            "NH 43.7 -71.6","NJ 40.2 -74.7","NM 34.4 -106.1","NY 42.9 -75.5","NC 35.6 -79.4","ND 47.5 -100.5","OH 40.3 -82.8","OK 35.6 -97.5","OR 44.0 -120.5",
            "PA 40.9 -77.8","RI 41.7 -71.5","SC 33.9 -80.9","SD 44.4 -100.2","TN 35.9 -86.4","TX 31.5 -99.3","UT 39.3 -111.7","VT 44.0 -72.7","VA 37.5 -78.8",
            "WA 47.4 -120.5","WV 38.6 -80.6","WI 44.6 -89.9","WY 43.0 -107.5","DC 38.9 -77.0","AK 64.0 -152.0","HI 20.8 -156.5","PR 18.2 -66.5",
            "WWA 47.5 -122.3","EWA 47.4 -119.0"};   // the two Washington sections; every other section is placed by its state
        for (String e : t) { String[] f = e.split(" "); STATE.put(f[0], new double[]{Double.parseDouble(f[1]), Double.parseDouble(f[2])}); }
    }

    /** The centre of a US state, or of a Washington section, by its code; null for anything else. */
    public static double[] stateCentre(String code) {
        double[] d = code == null ? null : STATE.get(code.trim().toUpperCase());
        return d == null ? null : new double[]{d[0], d[1]};
    }

    /**
     * {@link #of} for a call, but a station already in the call history is put where it said it was: its grid, else its section or state. The
     * call-area centre alone drops every W7 on the Oregon / Idaho line and every W6 in the middle of California.
     */
    public static double[] near(String call) {
        double[] base = of(call);
        if (call == null || call.isEmpty()) return base;
        java.util.Map<String, String> h = CallHistory.get(call);
        if (h.isEmpty()) { double[] p = PlaceLookup.get(call); return p != null ? p : base; }     // not worked before: the callbook, once it has answered
        String grid = h.get("grid");
        if (grid != null) { double[] g = com.jlog.util.Maidenhead.center(grid); if (g != null) return g; }
        for (String k : new String[]{"section", "state"}) {
            double[] s = stateCentre(h.get(k));
            if (s != null) return s;
        }
        double[] p = PlaceLookup.get(call);
        return p != null ? p : base;
    }

    /** Is this a US-area call (K, N, W, AA-AL, including Hawaii and Alaska)? The call area alone cannot say which state it is in, so these are worth a callbook lookup. */
    public static boolean isUs(String call) {
        return call != null && US_AREA.matcher(call.toUpperCase(java.util.Locale.ROOT).replaceAll("[-/].*", "")).find();
    }
}
