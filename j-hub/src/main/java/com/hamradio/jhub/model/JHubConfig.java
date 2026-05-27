package com.hamradio.jhub.model;

import com.hamradio.jhub.Platform;
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
    public SkimmerSection     skimmer         = new SkimmerSection();
    public BackupSection      backup          = new BackupSection();
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

    // Optional override for the Hamlib bin/ directory (the folder containing
    // rigctl/rigctld/rotctl/rotctld/ampctl/ampctld). When blank, detection
    // falls back to PATH and a list of common install locations. Useful on
    // Windows where the official ZIP extracts to a versioned folder
    // (e.g. C:\hamlib-w64-4.5.5\bin) that the auto-probe wouldn't find.
    public String hamlibBinDir = "";

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
        public String iaruRegion  = "IARU-R2";   // IARU-R1 | IARU-R2 | IARU-R3 — band edges + segment layout
        public String country     = "US";        // ISO-3166 alpha-2 country overlay for the bandplan (empty = region only)
        public int    cqZone      = 0;
        public String arrlSection = "";
        public int    ituZone     = 0;
        public String rigModel    = "";   // actual model (e.g. "IC-746pro", "FT-991A")
        public String rigAlias    = "";   // friendly nickname (e.g. "Main", "Backup")
        public String rigOperator = "";   // operator at this rig (override; defaults to "name")
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
        // Hamlib — managed-rigctld mode. When manageRigctld=true, j-hub spawns
        // rigctld itself using rigModel/serialPort/baudRate so the operator
        // never has to start a daemon by hand. When false, we assume an
        // external rigctld is already listening at hamlibHost:hamlibPort
        // (e.g. someone else launched it, or it's on another machine).
        public boolean manageRigctld = true;
        public int     rigModel     = 0;             // Hamlib model id (e.g. 132 = FT-1000MP, 3073 = IC-7300)
        public String  serialPort   = "";            // COM3, /dev/ttyUSB0, …
        public int     baudRate     = 0;             // 0 = use rig default; e.g. 4800, 9600, 19200…
        // Common
        public int     pollRateMs = 500;
        public boolean enablePtt  = false;
    }

    // ---------------------------------------------------------------
    // Rotor control backend
    // ---------------------------------------------------------------

    public static class RotorSection {
        public String  backend         = "NONE";    // HAMLIB | NONE
        // tcpHost / tcpPort are the Hamlib rotctld endpoint. Heading offsets
        // are consumed client-side in config.js (rotorPreset()) — they round-
        // trip through JSON but never need a Java reader.
        public String  tcpHost         = "localhost";
        public int     tcpPort         = 4533;
        public double  shortPathOffset = 0.0;
        public double  customPreset    = 0.0;
        // Managed-rotctld mode. When manageRotctld=true, j-hub spawns rotctld
        // itself using rotorModel/serialPort/baudRate so the operator never
        // has to start a daemon by hand — same pattern as RigSection. When
        // false, we assume an external rotctld is already listening at
        // tcpHost:tcpPort.
        public boolean manageRotctld = true;
        public int     rotorModel    = 0;            // Hamlib rotor model id
        public String  serialPort    = "";           // COM3, /dev/ttyUSB1, …
        public int     baudRate      = 0;            // 0 = rotor default
    }

    // ---------------------------------------------------------------
    // Amplifier control backend (ampctld)
    // ---------------------------------------------------------------

    public static class AmpSection {
        public String  backend     = "NONE";        // HAMLIB | NONE
        public String  tcpHost     = "localhost";
        public int     tcpPort     = 4531;          // ampctld default
        // Managed-ampctld mode (same pattern as RigSection / RotorSection).
        // When manageAmpctld=true, j-hub spawns ampctld itself using
        // ampModel/serialPort/baudRate. When false, connect to an existing
        // ampctld at tcpHost:tcpPort.
        public boolean manageAmpctld = true;
        public int     ampModel      = 0;           // Hamlib amp model id
        public String  serialPort    = "";          // COM4, /dev/ttyUSB2, …
        public int     baudRate      = 0;           // 0 = amp default
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
        public String theme    = "dark";          // dark | light | grayline
        public String mapTheme = "dark";          // dark | light | terrain | satellite
        public String density  = "comfortable";   // compact | comfortable | spacious — UI breathing room
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
    // Local CW Skimmer Server feed (or similar Skimmer-compatible source)
    //
    // VE7CC's CW Skimmer Server, RTTY Skimmer Server, and AR-Cluster-format
    // sources all speak the same line protocol the RBN backbone uses. This
    // lets an operator ingest spots from their own SDR-fed skimmer running
    // on the LAN at minimal extra code cost — same parser, different source
    // tag ("SKIMMER").
    // ---------------------------------------------------------------

    public static class SkimmerSection {
        public boolean enabled       = false;
        public String  server        = "127.0.0.1"; // default: local skimmer on the same shack PC
        public int     port          = 7300;        // CW Skimmer Server's default telnet port
        public String  loginCallsign = "";          // falls back to station callsign if blank
        public Set<String> bands     = new HashSet<>(Arrays.asList("160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"));
        public Set<String> modes     = new HashSet<>(Arrays.asList("CW","FT8","FT4","RTTY","PSK31"));
        public int     minSnrDb      = 5;
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
        public AppLaunchEntry jVault  = new AppLaunchEntry();
        public AppLaunchEntry jLearn  = new AppLaunchEntry();

        /**
         * Populate empty {@code command} fields with sensible defaults using
         * sibling-directory layout under the given ARS Suite root. Existing
         * (non-empty) commands are left alone. Returns true if anything
         * changed — caller should re-save the config.
         */
        public boolean applyDefaults(java.nio.file.Path arsRoot) {
            if (arsRoot == null) return false;
            String r = arsRoot.toString();
            boolean win = Platform.isWindows();
            // Point at each module's native launcher: the per-module .bat on
            // Windows (run via cmd /c by AppLauncher), the <module>.sh on
            // Linux/macOS (run via bash -c). Unquoted, matching the Linux
            // convention — a leading quote would trip cmd.exe's /c
            // quote-stripping rule. (Paths with spaces are a pre-existing
            // limitation noted in AppLauncher; the default install root has
            // none.)
            String[][] defaults = win
                ? new String[][] {
                    {"jMap",    r + "\\j-map\\j-map.bat"},
                    {"jLog",    r + "\\j-log\\j-log.bat"},
                    {"jBridge", r + "\\j-bridge\\j-bridge.bat"},
                    {"jDigi",   r + "\\j-digi\\j-digi.bat"},
                    {"jSat",    r + "\\j-sat\\j-sat.bat --launched-by-hub"},
                    {"jVault",  r + "\\j-vault\\j-vault.bat"},
                    {"jLearn",  r + "\\j-learn\\j-learn.bat"},
                  }
                : new String[][] {
                    {"jMap",    "bash " + r + "/j-map/j-map.sh"},
                    {"jLog",    "bash " + r + "/j-log/j-log.sh"},
                    {"jBridge", "bash " + r + "/j-bridge/j-bridge.sh"},
                    {"jDigi",   "bash " + r + "/j-digi/j-digi.sh"},
                    {"jSat",    "bash " + r + "/j-sat/j-sat.sh --launched-by-hub"},
                    {"jVault",  "bash " + r + "/j-vault/j-vault.sh"},
                    {"jLearn",  "bash " + r + "/j-learn/j-learn.sh"},
                  };
            boolean changed = false;
            for (String[] d : defaults) {
                AppLaunchEntry e = entryByName(d[0]);
                if (e == null) continue;
                if (e.command == null || e.command.isBlank()
                        || isForeignCommand(e.command, win)) {
                    // Fill blanks, and also rewrite a command left over from a
                    // different OS (e.g. a config carried from Linux still
                    // holding "bash .../run.sh" when now run on Windows).
                    // Same-OS user customisations have no foreign markers and
                    // are preserved.
                    e.command = d[1];
                    changed = true;
                }
            }
            return changed;
        }

        /**
         * True if {@code cmd} is plainly meant for the other OS. A valid
         * Windows command never contains {@code bash }/{@code .sh}/{@code
         * /home/}; a valid *nix command never contains {@code .bat} or a
         * {@code C:\} drive prefix. Anything else (a same-OS custom path) is
         * left untouched.
         */
        private static boolean isForeignCommand(String cmd, boolean win) {
            String low = cmd.trim().toLowerCase();
            if (win) {
                return low.startsWith("bash ") || low.contains(".sh")
                        || low.contains("/home/");
            }
            return low.contains(".bat") || low.matches(".*[a-z]:\\\\.*");
        }

        private AppLaunchEntry entryByName(String n) {
            switch (n) {
                case "jMap":    return jMap;
                case "jLog":    return jLog;
                case "jBridge": return jBridge;
                case "jDigi":   return jDigi;
                case "jSat":    return jSat;
                case "jVault":  return jVault;
                case "jLearn":  return jLearn;
                default:        return null;
            }
        }
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
