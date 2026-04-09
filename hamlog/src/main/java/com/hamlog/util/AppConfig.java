package com.hamlog.util;

import com.hamlog.db.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Application configuration.
 * Small values are stored in Java Preferences (fast, no DB needed at startup).
 * Larger/station data lives in config.db via DatabaseManager.
 */
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final AppConfig INSTANCE = new AppConfig();
    public  static AppConfig getInstance() { return INSTANCE; }
    private AppConfig() {}

    private Preferences prefs;

    // ---------------------------------------------------------------
    // Load — call once before DatabaseManager.initAll()
    // ---------------------------------------------------------------

    public void load() {
        prefs = Preferences.userNodeForPackage(AppConfig.class);
        log.debug("AppConfig loaded from Java Preferences");
    }

    // ---------------------------------------------------------------
    // Theme
    // ---------------------------------------------------------------

    public String getTheme() {
        return prefs.get("theme", "light");
    }
    public void setTheme(String theme) {
        prefs.put("theme", theme);
    }

    // ---------------------------------------------------------------
    // Language
    // ---------------------------------------------------------------

    public String getLanguage() {
        return prefs.get("language", "en");
    }
    public void setLanguage(String lang) {
        prefs.put("language", lang);
    }

    // ---------------------------------------------------------------
    // Debug mode
    // ---------------------------------------------------------------

    public boolean isDebugMode() {
        return prefs.getBoolean("debugMode", false);
    }
    public void setDebugMode(boolean debug) {
        prefs.putBoolean("debugMode", debug);
    }

    // ---------------------------------------------------------------
    // Font size
    // ---------------------------------------------------------------

    public int getFontSize() {
        return prefs.getInt("fontSize", 13);
    }
    public void setFontSize(int size) {
        prefs.putInt("fontSize", size);
    }

    // ---------------------------------------------------------------
    // Current mode (NORMAL / CONTEST)
    // ---------------------------------------------------------------

    public String getCurrentMode() {
        return prefs.get("currentMode", "NORMAL");
    }
    public void setCurrentMode(String mode) {
        prefs.put("currentMode", mode);
    }

    // ---------------------------------------------------------------
    // Active contest id
    // ---------------------------------------------------------------

    public String getActiveContestId() {
        return prefs.get("activeContestId", "");
    }
    public void setActiveContestId(String id) {
        prefs.put("activeContestId", id);
    }

    // ---------------------------------------------------------------
    // Window geometry persistence
    // ---------------------------------------------------------------

    public int  getWindowWidth (String key, int def) { return prefs.getInt(key + ".w", def); }
    public int  getWindowHeight(String key, int def) { return prefs.getInt(key + ".h", def); }
    public int  getWindowX     (String key, int def) { return prefs.getInt(key + ".x", def); }
    public int  getWindowY     (String key, int def) { return prefs.getInt(key + ".y", def); }

    public void setWindowWidth (String key, int v)   { prefs.putInt(key + ".w", v); }
    public void setWindowHeight(String key, int v)   { prefs.putInt(key + ".h", v); }
    public void setWindowX     (String key, int v)   { prefs.putInt(key + ".x", v); }
    public void setWindowY     (String key, int v)   { prefs.putInt(key + ".y", v); }

    // SplitPane divider positions
    public double getDivider(String key, double def) { return prefs.getDouble(key, def); }
    public void   setDivider(String key, double val) { prefs.putDouble(key, val); }

    // ---------------------------------------------------------------
    // Station info (delegated to config.db after DB init)
    // ---------------------------------------------------------------

    private String getDb(String key, String def) {
        try { return DatabaseManager.getInstance().getConfig(key, def); }
        catch (Exception e) { return def; }
    }
    private void setDb(String key, String val) {
        try { DatabaseManager.getInstance().setConfig(key, val); }
        catch (Exception e) { log.warn("setDb failed for {}", key, e); }
    }

    public String getStationCallsign()          { return getDb("station.callsign", ""); }
    public void   setStationCallsign(String v)  { setDb("station.callsign", v); }

    public String getOperatorName()             { return getDb("station.operator", ""); }
    public void   setOperatorName(String v)     { setDb("station.operator", v); }

    public String getQth()                      { return getDb("station.qth", ""); }
    public void   setQth(String v)              { setDb("station.qth", v); }

    public String getGridSquare()               { return getDb("station.grid", ""); }
    public void   setGridSquare(String v)       { setDb("station.grid", v); }

    public String getLatitude()                 { return getDb("station.lat", "0.0"); }
    public void   setLatitude(String v)         { setDb("station.lat", v); }

    public String getLongitude()                { return getDb("station.lon", "0.0"); }
    public void   setLongitude(String v)        { setDb("station.lon", v); }

    public String getRadioModel()               { return getDb("station.radio", ""); }
    public void   setRadioModel(String v)       { setDb("station.radio", v); }

    public String getAntenna()                  { return getDb("station.antenna", ""); }
    public void   setAntenna(String v)          { setDb("station.antenna", v); }

    public String getDefaultPower()             { return getDb("station.power", "100"); }
    public void   setDefaultPower(String v)     { setDb("station.power", v); }

    public String getQrzUsername()              { return getDb("qrz.username", ""); }
    public void   setQrzUsername(String v)      { setDb("qrz.username", v); }

    public String getQrzPassword()              { return getDb("qrz.password", ""); }
    public void   setQrzPassword(String v)      { setDb("qrz.password", v); }

    // CI-V
    public String getCivPort()                  { return getDb("civ.port", "/dev/ttyUSB0"); }
    public void   setCivPort(String v)          { setDb("civ.port", v); }

    public String getCivBaud()                  { return getDb("civ.baud", "19200"); }
    public void   setCivBaud(String v)          { setDb("civ.baud", v); }

    public String getCivAddress()               { return getDb("civ.address", "94"); }
    public void   setCivAddress(String v)       { setDb("civ.address", v); }

    public boolean getCivAutoConnect() {
        return "true".equals(getDb("civ.autoConnect", "false"));
    }
    public void setCivAutoConnect(boolean v)    { setDb("civ.autoConnect", String.valueOf(v)); }

    // ---------------------------------------------------------------
    // SS Contest Exchange (saved per-station, not per-contest)
    // ---------------------------------------------------------------

    public String getSsCallsign()            { return getDb("ss.callsign", getStationCallsign()); }
    public void   setSsCallsign(String v)    { setDb("ss.callsign", v); }

    public String getSsPrecedence()          { return getDb("ss.precedence", ""); }
    public void   setSsPrecedence(String v)  { setDb("ss.precedence", v); }

    public String getSsCheck()               { return getDb("ss.check", ""); }
    public void   setSsCheck(String v)       { setDb("ss.check", v); }

    public String getSsSection()             { return getDb("ss.section", ""); }
    public void   setSsSection(String v)     { setDb("ss.section", v); }
}
