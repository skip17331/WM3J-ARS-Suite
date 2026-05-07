package com.hamradio.jhub.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JHubConfig — top-level configuration object serialized as j-hub.json.
 *
 * All fields are public for Gson serialization/deserialization.
 * Use ConfigManager.getInstance().getConfig() to access the live object.
 */
public class JHubConfig {

    public JHubSection        jHub            = new JHubSection();
    public StationSection     station         = new StationSection();
    public RigSection         rig             = new RigSection();
    public RotorSection       rotor           = new RotorSection();
    public AmpSection         amp             = new AmpSection();
    public AntennaSection     antenna         = new AntennaSection();
    public ClusterSection     cluster         = new ClusterSection();
    public RbnSection         rbn             = new RbnSection();
    public BackupSection      backup          = new BackupSection();
    public LoggerSection      logger          = new LoggerSection();
    public InfoScreenSection  infoScreen      = new InfoScreenSection();
    public AppsSection        apps            = new AppsSection();
    public MacrosSection      macros          = new MacrosSection();
    public AppearanceSection  appearance      = new AppearanceSection();
    public CallsignSection    callsignLookup  = new CallsignSection();
    public AutoUpdateSection  autoUpdate      = new AutoUpdateSection();
    public com.google.gson.JsonObject jMapSettings    = null;
    public com.google.gson.JsonObject jSatSettings    = null;
    public com.google.gson.JsonObject jLogSettings    = null;
    public com.google.gson.JsonObject jDigiSettings   = null;
    public com.google.gson.JsonObject jBridgeSettings = null;

    // ---------------------------------------------------------------
    // J-Hub network settings
    // ---------------------------------------------------------------

    public static class JHubSection {
        public int    websocketPort = 8080;
        public int    webConfigPort = 8081;
        public String ip            = "localhost";
    }

    // ---------------------------------------------------------------
    // Station operator information
    // ---------------------------------------------------------------

    public static class StationSection {
        public String callsign    = "NOCALL";
        public String name        = "";
        public String qth         = "";
        public double lat         = 0.0;
        public double lon         = 0.0;
        public String gridSquare  = "";
        public String timezone    = "UTC";
        public String language    = "en";
        public int    cqZone      = 0;
        public String arrlSection = "";
        public int    ituZone     = 0;
        public String rigAlias    = "";   // friendly name displayed in all module UIs
    }

    // ---------------------------------------------------------------
    // Rig control backend
    // ---------------------------------------------------------------

    public static class RigSection {
        public String  backend    = "NONE";         // CI_V | HAMLIB | NONE
        // CI-V settings
        public String  civPort    = "";
        public int     civBaud    = 9600;
        public String  civAddress = "94";            // hex address (e.g. 94 = IC-7300)
        // Hamlib settings
        public String  hamlibHost = "localhost";
        public int     hamlibPort = 4532;
        // Common
        public int     pollRateMs = 500;
        public boolean enablePtt  = false;
    }

    // ---------------------------------------------------------------
    // Rotor control backend
    // ---------------------------------------------------------------

    public static class RotorSection {
        public String  backend         = "NONE";    // INTERNAL | HAMLIB | NONE
        public String  model           = "";
        public String  comPort         = "";
        public String  tcpHost         = "localhost";
        public int     tcpPort         = 4533;
        public double  shortPathOffset = 0.0;
        public double  customPreset    = 0.0;
    }

    // ---------------------------------------------------------------
    // Amplifier control backend (ampctld)
    // ---------------------------------------------------------------

    public static class AmpSection {
        public String  backend     = "NONE";        // HAMLIB | NONE
        public String  model       = "";            // hamlib amp model id (informational)
        public String  comPort     = "";            // serial port (informational; ampctld owns the device)
        public int     baud        = 9600;          // (informational)
        public String  tcpHost     = "localhost";
        public int     tcpPort     = 4531;          // ampctld default
        public int     pollRateMs  = 1000;
        public boolean bandFollow  = true;          // forward rig band changes to the amp
        public boolean faultAlert  = true;          // surface visual fault indicator on overage
        public double  swrFault    = 3.0;           // SWR threshold treated as a fault
    }

    // ---------------------------------------------------------------
    // Automatic antenna switching (serial-controlled relays)
    // ---------------------------------------------------------------

    public static class AntennaSection {
        public boolean              enabled       = false;
        public String               comPort       = "";
        public int                  baud          = 9600;
        public boolean              lockoutOnPtt  = true;   // safety: don't switch while keyed
        public java.util.List<AntennaSwitch> switches = new java.util.ArrayList<>();
        public java.util.List<AntennaRule>   rules    = new java.util.ArrayList<>();
    }

    /** A single physical antenna switch (one serial-addressable relay box). */
    public static class AntennaSwitch {
        public String  id            = "main";  // referenced by rules
        public String  name          = "Main";
        public int     antennaCount  = 4;
    }

    /**
     * One band/mode/heading-conditioned rule. The first matching rule (in list
     * order) wins — operators put more-specific rules above less-specific ones.
     *
     * <p>{@code commandTemplate} substitutes {@code {switch}} and {@code {antenna}};
     * literal {@code \r} / {@code \n} are honored. Examples for common switches:
     * <ul>
     *   <li>DXEngineering RR8: "SW{switch}={antenna}\r"
     *   <li>Microham µStation: "ANT{antenna}\r\n"
     *   <li>ARCO RC-1A: "{antenna}\n"
     * </ul>
     */
    public static class AntennaRule {
        public String  band            = "";    // e.g. "20m" — required
        public String  mode            = "";    // optional ("CW", "SSB", "FT8", ...) — empty = any
        public double  headingMin      = -1;    // optional rotor heading window — -1 = unset
        public double  headingMax      = -1;
        public String  switchId        = "main";
        public int     antenna         = 1;
        public String  commandTemplate = "SW{switch}={antenna}\\r";
    }

    // ---------------------------------------------------------------
    // Appearance / theme
    // ---------------------------------------------------------------

    public static class AppearanceSection {
        public String theme          = "dark";      // dark | light | grayline
        public int    fontSize       = 13;
        public String waterfallColor = "viridis";   // viridis | plasma | inferno | grayscale
        public String mapTheme       = "dark";      // dark | light | terrain | satellite
    }

    // ---------------------------------------------------------------
    // DX cluster connection
    // ---------------------------------------------------------------

    public static class DxNetwork {
        public String name          = "";
        public String server        = "";
        public int    port          = 7373;
        public String loginCallsign = "";
    }

    public static class ClusterSection {
        public boolean          autoConnect  = false;
        public String           server       = "";
        public int              port         = 7373;
        public String           loginCallsign = "";
        public ClusterFilters   filters      = new ClusterFilters();
        public List<DxNetwork>  networks     = new ArrayList<>();
    }

    public static class ClusterFilters {
        public Set<String> bands = new HashSet<>(Arrays.asList("160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"));
        public Set<String> modes = new HashSet<>(Arrays.asList("SSB","CW","FT8","FT4","RTTY","PSK31","JS8"));
    }

    // ---------------------------------------------------------------
    // Reverse Beacon Network — parallel skimmer-fed spot stream
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // Cloud backup of all ~/.j-* state directories
    // ---------------------------------------------------------------

    public static class BackupSection {
        public boolean enabled       = false;
        public String  mode          = "FOLDER";   // "FOLDER" | "WEBDAV"
        public String  folderPath    = "";          // e.g. "/home/op/Dropbox/J-Suite-Backups"
        public String  webdavUrl     = "";          // e.g. "https://nextcloud.example/remote.php/dav/files/me/J-Backups"
        public int     scheduleHours = 24;          // periodic backup interval (0 = on-demand only)
        public int     retain        = 14;          // how many timestamped backups to keep
        public boolean includeJHub   = true;
        public boolean includeJLog   = true;
        public boolean includeJMap   = true;
        public boolean includeJSat   = true;
        public boolean includeJDigi  = false;       // mostly local logs, large
        public boolean includeJBridge = false;
    }

    public static class RbnSection {
        public boolean enabled       = false;
        public String  server        = "telnet.reversebeacon.net";
        public int     port          = 7000;
        public String  loginCallsign = "";       // falls back to station callsign if blank
        public Set<String> bands     = new HashSet<>(Arrays.asList("160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"));
        public Set<String> modes     = new HashSet<>(Arrays.asList("CW","FT8","FT4","RTTY","PSK31"));
        public int     minSnrDb      = 5;        // drop weak skimmer reports below this
    }

    // ---------------------------------------------------------------
    // Logger application settings
    // ---------------------------------------------------------------

    public static class LoggerSection {
        public String  mode          = "normal";
        public NormalLog normalLog   = new NormalLog();
        public Object[] contests     = new Object[0];
        public String  activeContest = null;
    }

    public static class NormalLog {
        public String dbPath = "";
    }

    // ---------------------------------------------------------------
    // Info / display screen settings
    // ---------------------------------------------------------------

    public static class InfoScreenSection {
        public String  mapStyle       = "dark";
        public boolean showGreatCircle = true;
        public int     spotTimeout    = 30;   // minutes
        public int     maxCachedSpots = 50;
    }

    // ---------------------------------------------------------------
    // App launcher settings
    // ---------------------------------------------------------------

    public static class AppsSection {
        public AppLaunchEntry jMap    = new AppLaunchEntry();
        public AppLaunchEntry jLog    = new AppLaunchEntry();
        public AppLaunchEntry jBridge = new AppLaunchEntry();
        public AppLaunchEntry jDigi   = new AppLaunchEntry();
        public AppLaunchEntry jSat    = new AppLaunchEntry();
    }

    public static class AppLaunchEntry {
        public boolean autoLaunch = false;
        public String  command    = "";
        public String  ip         = "localhost";
    }

    // ---------------------------------------------------------------
    // Macro definitions (stored in j-hub.json, edited via web UI)
    // ---------------------------------------------------------------

    public static class MacroDefinition {
        public String key;     // "CQ", "ANS_CQ", "QSO", "SK", "KN", "F1"…"F12"
        public String label;   // button label
        public String text;    // template — supports {MYCALL} {CALL} {RST} {NAME} {BAND} {FREQ} {MODE}
        public String type;    // "FIXED" | "PROGRAMMABLE"
        public String kind = "CW";  // "CW" (digital/text) | "VOICE" (WAV playback)
        public String wavPath;      // absolute path to WAV file (VOICE macros only)
    }

    public static class MacrosSection {
        public java.util.List<MacroDefinition> list = defaultList();

        private static java.util.List<MacroDefinition> defaultList() {
            java.util.List<MacroDefinition> l = new java.util.ArrayList<>();
            l.add(m("CQ",     "CQ",      "CQ CQ CQ DE {MYCALL} {MYCALL} {MYCALL} K",                                "FIXED"));
            l.add(m("ANS_CQ", "Ans CQ",  "{CALL} DE {MYCALL} {MYCALL} PSE K",                                        "FIXED"));
            l.add(m("QSO",    "QSO",     "{CALL} DE {MYCALL} UR RST {RST} {RST} NAME {NAME} QTH {BAND} 73 DE {MYCALL} K", "FIXED"));
            l.add(m("SK",     "73 SK",   "{CALL} DE {MYCALL} QSL TNX QSO 73 SK",                                     "FIXED"));
            l.add(m("KN",     "KN",      "{CALL} DE {MYCALL} KN",                                                     "FIXED"));
            l.add(m("F1",     "F1",      "",  "PROGRAMMABLE"));
            l.add(m("F2",     "F2",      "",  "PROGRAMMABLE"));
            l.add(m("F3",     "F3",      "",  "PROGRAMMABLE"));
            l.add(m("F4",     "F4",      "",  "PROGRAMMABLE"));
            return l;
        }

        private static MacroDefinition m(String key, String label, String text, String type) {
            MacroDefinition d = new MacroDefinition();
            d.key = key; d.label = label; d.text = text; d.type = type;
            return d;
        }
    }

    // ---------------------------------------------------------------
    // Callsign lookup service configuration
    // ---------------------------------------------------------------

    public static class CallsignSection {
        /** Enable or disable the lookup service entirely. */
        public boolean enabled          = true;
        /**
         * Which provider to use: "auto" (priority chain), "qrz", "hamqth", "hamdb", "callook".
         * In "auto" mode the order is: QRZ → HamQTH → HamDB → Callook.
         */
        public String  provider         = "auto";
        /**
         * Path to the local SQLite callsign database (e.g. "/home/user/callsigns.db").
         * Leave blank to disable local-DB lookups.
         * Populate via POST /api/callsign/db/import/fcc or /api/callsign/db/import/csv.
         */
        public String  localDbPath      = "";
        // QRZ.com XML subscription credentials (https://www.qrz.com/page/xml_data.html)
        public String  qrzUsername      = "";
        public String  qrzPassword      = "";
        // HamQTH.com free-registration credentials (https://www.hamqth.com)
        public String  hamqthUsername   = "";
        public String  hamqthPassword   = "";
        /** How long to cache a successful lookup result (hours). */
        public int     cacheTtlHours    = 24;
        /**
         * Override URL for the FCC ULS amateur ZIP download.
         * Leave blank to use the default: https://data.fcc.gov/download/pub/uls/complete/l_amat.zip
         */
        public String  fccUlsUrl        = "";
    }

    // ---------------------------------------------------------------
    // Factory method — creates a sensible default configuration
    // ---------------------------------------------------------------

    public static JHubConfig defaults() {
        JHubConfig cfg = new JHubConfig();
        // defaults are already set by field initializers above
        return cfg;
    }

    // ---------------------------------------------------------------
    // Auto-update of contest data files (CTY.DAT + MASTER.SCP)
    // ---------------------------------------------------------------

    public static class AutoUpdateSection {
        public boolean enabled       = true;
        /** Days between scheduled fetches; default weekly. */
        public int     intervalDays  = 7;
        /** CTY.DAT — country/prefix list (Jim Reisert AD1C). */
        public String  ctyUrl        = "https://www.country-files.com/cty/cty.dat";
        /** MASTER.SCP — Super Check Partial database. */
        public String  scpUrl        = "https://supercheckpartial.com/MASTER.SCP";
        /** Where the fetched files land. Empty = ~/.j-hub/data/. */
        public String  dataDir       = "";
        /** Sanity-check minimum file sizes (bytes). A truncated CTY.DAT is
         *  worse than an old one — these guards keep half-fetched garbage
         *  from clobbering the working file. */
        public int     minCtySize    = 50_000;
        public int     minScpSize    = 200_000;
    }
}
