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

    // ---- J-Map ----
    /** Azimuthal spots: {call, bearingDeg, distNorm(0..1), colorHex}. */
    public static final Object[][] AZ_SPOTS = {
        {"9M6XRO",340.0,0.86,"#eebb58"}, {"VU2XYZ",30.0,0.76,"#26c0cf"}, {"JA7QVI",312.0,0.60,"#67d283"},
        {"JA1XYZ",316.0,0.64,"#67d283"}, {"UA9ABC",25.0,0.55,"#26c0cf"}, {"4X1ABC",70.0,0.50,"#67d283"},
        {"DL8WPX",55.0,0.36,"#26c0cf"}, {"TF3IRA",35.0,0.30,"#eebb58"}, {"G4ABC",50.0,0.33,"#26c0cf"},
        {"VE7CC",300.0,0.30,"#b191ea"}, {"CN2AA",96.0,0.36,"#eebb58"}, {"5U5R",110.0,0.44,"#eebb58"},
        {"EA8RKL",105.0,0.42,"#26c0cf"}, {"K1TTT",248.0,0.16,"#26c0cf"}, {"VK9DX",250.0,0.55,"#ec5b57"},
        {"F05QB",225.0,0.46,"#eebb58"}, {"ZL3ABC",220.0,0.62,"#b191ea"}, {"PY2NY",160.0,0.50,"#ef7d83"},
        {"ZS6ABC",130.0,0.60,"#26c0cf"}, {"ZD7BG",136.0,0.63,"#67d283"}, {"CE3LU",190.0,0.66,"#67d283"},
    };
    /** DX-Spots drawer list: {freq, call, country, bearing, need}. */
    public static final String[][] DX_LIST = {
        {"14.182","DL8WPX","Germany","049°",""},
        {"14.022","VK9DX","Norfolk Is.","262°",""},
        {"14.025","G4ABC","England","051°",""},
        {"14.182","ZD7BG","St. Helena","115°","y"},
        {"14.040","K1TTT","USA","055°",""},
        {"21.024","EA8RKL","Canary Is.","086°","y"},
    };
    /** Left-panel bands: {num, colorHex}. */
    public static final String[][] MAP_BANDS = {
        {"160","#ec5b57"},{"80","#ec5b57"},{"40","#eebb58"},{"30","#67d283"},{"20","#26c0cf"},
        {"17","#26c0cf"},{"15","#b191ea"},{"12","#b191ea"},{"10","#ef7d83"},
    };
    /** Overlay toggles: {label, on}. */
    public static final Object[][] OVERLAYS = {
        {"Gray line / sun",true},{"Basemap image",true},{"Beam heading",true},
        {"Range rings",true},{"Callsigns",true},{"Needed only",false},
    };

    // ---- J-Sat ----
    /** Satellites: {name, type, pill, pillClass, maxEl, dur, downlink}. */
    public static final String[][] SATS = {
        {"AO-91","FM","AOS","aos","47","11m","145.960"},
        {"SO-50","FM","+18M","","22","9m","436.795"},
        {"ISS","APRS/Voice","+42M","","78","10m","145.800"},
        {"RS-44","Linear","+1H 26M","","34","22m","435.610"},
        {"AO-7","Linear","LOS","los","12","14m","145.970"},
        {"CAS-4B","Linear","+2H 11M","","58","16m","145.870"},
    };
    /** Next passes: {name, el, time}. */
    public static final String[][] NEXT_PASSES = {
        {"SO-50","el 22°","+18m"},{"ISS","el 78°","+42m"},{"RS-44","el 34°","+1h 26m"},
        {"AO-7","el 12°","+3h 02m"},{"CAS-4B","el 58°","+2h 11m"},
    };
    /** Telemetry cells: {key, value, unit, neg}. */
    public static final String[][] SAT_TELEMETRY = {
        {"AZIMUTH","096","°",""},{"ELEVATION","31","°",""},{"RANGE","1,842","km",""},
        {"RANGE RATE","-5.2","km/s","y"},{"PHASE","112","",""},
    };

    // ---- J-Digi ----
    /** Decode lines: {timestamp, text, cursor("y"|"")}. */
    public static final String[][] DIGI_DECODE = {
        {"1438Z","CQ CQ CQ DE DL8WPX DL8WPX DL8WPX K",""},
        {"1439Z","DL8WPX DE WM3J WM3J K",""},
        {"1439Z","WM3J DE DL8WPX = GM DR OM TKS FER CALL = UR RST 599 599 = NAME HANS HANS = QTH NR MUNICH MUNICH = HW CPY? WM3J DE DL8WPX KN",""},
        {"1440Z","DL8WPX DE WM3J = FB HANS TKS RPRT = UR 599 ALSO = NAME JIM QTH NEW JERSEY = ","y"},
    };
    public static final String[][] DIGI_MACROS = {{"F1","CQ"},{"F2","Call"},{"F3","RST"},{"F4","QTH"},{"F5","BTU"},{"F6","73"},{"F7","Tune"}};
    public static final String[] DIGI_SCALE = {"0","500","1000","1500","2000","2500"};
    /** Decoder drawer: {key, value, green("y"|"")}. */
    public static final String[][] DIGI_DECODER = {
        {"Mode","CW",""},{"Speed","28 WPM",""},{"AFC","locked","y"},{"Squelch","on","y"},{"Audio in","-6 dB",""},{"Log to","J-Log",""},
    };

    // ---- J-Bridge ----
    /** FT8 decode: {utc, db, dt, hz, msg, type(cq|me|"")}. */
    public static final String[][] BAND_ACTIVITY = {
        {"1415","-8","0.2","1577","CQ DL8WPX JO62","cq"},
        {"1415","-14","0.1","892","K1ABC W2XYZ FN20",""},
        {"1415","-2","0.3","1230","CQ VK9DX RG29 ●","cq"},
        {"1415","-16","-0.2","2104","JA7QVI EA5K -11",""},
        {"1415","-5","0.1","1450","CQ DX 5U5R JN06 ●","cq"},
        {"1415","-19","0.4","760","CQ PY2NY GG66","cq"},
        {"1430","-11","0.0","1577","WM3J DL8WPX -08","me"},
        {"1430","-3","0.2","1230","CQ VK9DX RG29 ●","cq"},
        {"1430","-22","-0.1","2360","CQ CN2AA IM63 ●","cq"},
        {"1430","-9","0.1","980","G4ABC W1AW FN31",""},
        {"1445","-12","0.2","1577","WM3J DL8WPX R-08","me"},
        {"1445","-7","0.3","1680","CQ JA1XYZ PM95","cq"},
    };
    public static final String[][] RX_FREQUENCY = {
        {"1415","-8","0.2","1577","CQ DL8WPX JO62","cq"},
        {"1430","-11","0.0","1577","WM3J DL8WPX -08","me"},
        {"1445","-12","0.2","1577","WM3J DL8WPX R-08","me"},
    };
    public static final String[] BRIDGE_SCALE = {"200","700","1200","1700","2200","2800"};
    /** WSJT-X link drawer: {key, value, green}. */
    public static final String[][] WSJTX_LINK = {
        {"Status","● Connected","y"},{"Version","2.7.0",""},{"UDP port","2237",""},{"Rig (CAT)","IC-7610",""},{"Auto-log QSO","on → J-Log","y"},
    };
}
