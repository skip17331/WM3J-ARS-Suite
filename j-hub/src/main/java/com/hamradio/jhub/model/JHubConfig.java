package com.hamradio.jhub.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JHubConfig — top-level configuration object serialized as j-hub.json.
 *
 * All fields are public for Gson serialization/deserialization.
 * Use ConfigManager.getInstance().getConfig() to access the live object.
 */
public class JHubConfig {

    public JHubSection       jHub            = new JHubSection();
    public StationSection    station         = new StationSection();
    public RigSection        rig             = new RigSection();
    public RotorSection      rotor           = new RotorSection();
    public ClusterSection    cluster         = new ClusterSection();
    public LoggerSection     logger          = new LoggerSection();
    public InfoScreenSection infoScreen      = new InfoScreenSection();
    public AppsSection       apps            = new AppsSection();
    public MacrosSection     macros          = new MacrosSection();
    public AppearanceSection appearance      = new AppearanceSection();
    public com.google.gson.JsonObject jMapSettings  = null;
    public com.google.gson.JsonObject jSatSettings  = null;

    // ---------------------------------------------------------------
    // J-Hub network settings
    // ---------------------------------------------------------------

    public static class JHubSection {
        public int websocketPort = 8080;
        public int webConfigPort = 8081;
    }

    // ---------------------------------------------------------------
    // Station operator information
    // ---------------------------------------------------------------

    public static class StationSection {
        public String callsign   = "NOCALL";
        public String name       = "";
        public String qth        = "";
        public double lat        = 0.0;
        public double lon        = 0.0;
        public String gridSquare = "";
        public String timezone   = "UTC";
        public String language   = "en";
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

    public static class ClusterSection {
        public boolean autoConnect  = false;
        public String server        = "";
        public int    port          = 7373;
        public String loginCallsign = "";
        public ClusterFilters filters = new ClusterFilters();
    }

    public static class ClusterFilters {
        public Set<String> bands = new HashSet<>(Arrays.asList("160m","80m","40m","30m","20m","17m","15m","12m","10m","6m"));
        public Set<String> modes = new HashSet<>(Arrays.asList("SSB","CW","FT8","FT4","RTTY","PSK31","JS8"));
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
    }

    // ---------------------------------------------------------------
    // Macro definitions (stored in j-hub.json, edited via web UI)
    // ---------------------------------------------------------------

    public static class MacroDefinition {
        public String key;    // "CQ", "ANS_CQ", "QSO", "SK", "KN", "F1"…"F4"
        public String label;  // button label
        public String text;   // template — supports {MYCALL} {CALL} {RST} {NAME} {BAND} {FREQ} {MODE}
        public String type;   // "FIXED" | "PROGRAMMABLE"
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
    // Factory method — creates a sensible default configuration
    // ---------------------------------------------------------------

    public static JHubConfig defaults() {
        JHubConfig cfg = new JHubConfig();
        // defaults are already set by field initializers above
        return cfg;
    }
}
