package com.hamradio.hub.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * HubConfig — top-level configuration object serialized as hub.json.
 *
 * All fields are public for Gson serialization/deserialization.
 * Use ConfigManager.getInstance().getConfig() to access the live object.
 */
public class HubConfig {

    public HubSection        hub             = new HubSection();
    public StationSection    station         = new StationSection();
    public ClusterSection    cluster         = new ClusterSection();
    public LoggerSection     logger          = new LoggerSection();
    public InfoScreenSection infoScreen      = new InfoScreenSection();
    public AppsSection       apps            = new AppsSection();
    public com.google.gson.JsonObject hamclockSettings = null;

    // ---------------------------------------------------------------
    // Hub network settings
    // ---------------------------------------------------------------

    public static class HubSection {
        public int websocketPort = 8080;
        public int webConfigPort = 8081;
    }

    // ---------------------------------------------------------------
    // Station operator information
    // ---------------------------------------------------------------

    public static class StationSection {
        public String callsign  = "NOCALL";
        public double lat       = 0.0;
        public double lon       = 0.0;
        public String gridSquare = "";
        public String timezone  = "UTC";
    }

    // ---------------------------------------------------------------
    // DX cluster connection
    // ---------------------------------------------------------------

    public static class ClusterSection {
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
        public String  mapStyle      = "dark";
        public boolean showGreatCircle = true;
        public int     spotTimeout   = 30;   // minutes
        public int     maxCachedSpots = 50;
    }

    // ---------------------------------------------------------------
    // App launcher settings
    // ---------------------------------------------------------------

    public static class AppsSection {
        public AppLaunchEntry hamclock = new AppLaunchEntry();
        public AppLaunchEntry hamlog   = new AppLaunchEntry();
    }

    public static class AppLaunchEntry {
        public boolean autoLaunch = false;
        public String  command    = "";
    }

    // ---------------------------------------------------------------
    // Factory method — creates a sensible default configuration
    // ---------------------------------------------------------------

    public static HubConfig defaults() {
        HubConfig cfg = new HubConfig();
        // defaults are already set by field initializers above
        return cfg;
    }
}
