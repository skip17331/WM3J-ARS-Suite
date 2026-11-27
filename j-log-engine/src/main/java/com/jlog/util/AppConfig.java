package com.jlog.util;

import com.jlog.db.DatabaseManager;
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
    // CW keyer WPM (last value used in the J-Log keyer pane)
    // ---------------------------------------------------------------

    public int  getCwWpm()         { return prefs.getInt("cwWpm", 25); }
    public void setCwWpm(int wpm)  { prefs.putInt("cwWpm", wpm); }

    // ---------------------------------------------------------------
    // Voice keyer (PTT timing + concurrent macro behavior)
    // ---------------------------------------------------------------

    /** Milliseconds to wait between PTT-on and starting WAV playback.
     *  Gives the rig time to key fully before audio starts so the first
     *  syllable isn't clipped. */
    public int  getVoicePreRollMs()         { return prefs.getInt("voicePreRollMs",  250); }
    public void setVoicePreRollMs(int ms)   { prefs.putInt("voicePreRollMs", Math.max(0, ms)); }

    /** Milliseconds to wait between WAV playback finishing and PTT-off.
     *  Prevents the rig from un-keying mid-tail of the audio file. */
    public int  getVoicePostRollMs()        { return prefs.getInt("voicePostRollMs", 250); }
    public void setVoicePostRollMs(int ms)  { prefs.putInt("voicePostRollMs", Math.max(0, ms)); }

    /** Behavior when a voice macro fires while another is still playing.
     *  REPLACE — abort current and start new (default; matches CW abort-and-replace).
     *  QUEUE   — let the executor serialize them (current finishes, then new plays).
     *  IGNORE  — drop the new macro silently. */
    public String getVoiceConcurrentMode()           { return prefs.get("voiceConcurrentMode", "REPLACE"); }
    public void   setVoiceConcurrentMode(String m)   {
        if (m == null) m = "REPLACE";
        if (!"REPLACE".equals(m) && !"QUEUE".equals(m) && !"IGNORE".equals(m)) m = "REPLACE";
        prefs.put("voiceConcurrentMode", m);
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

    /**
     * UI density preset driven by j-hub's appearance config. One of
     * {@code "compact"}, {@code "comfortable"} (default), or
     * {@code "spacious"}. Combines multiplicatively with {@link #getFontSize()}
     * when {@code JLogApp.applyTheme} sets the scene's root font-size.
     */
    public String getDensity() {
        return prefs.get("ui.density", "comfortable");
    }
    public void setDensity(String density) {
        prefs.put("ui.density", density);
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
    // Automatic database backup
    // ---------------------------------------------------------------

    public boolean isBackupEnabled()        { return !"false".equalsIgnoreCase(getDb("backup.enabled", "true")); }
    public void    setBackupEnabled(boolean v){ setDb("backup.enabled", String.valueOf(v)); }

    public int getBackupIntervalHours()      {
        try { return Integer.parseInt(getDb("backup.intervalHours", "6")); }
        catch (NumberFormatException e) { return 6; }
    }
    public void setBackupIntervalHours(int h){ setDb("backup.intervalHours", String.valueOf(h)); }

    public int getBackupKeepCount()          {
        try { return Integer.parseInt(getDb("backup.keepCount", "7")); }
        catch (NumberFormatException e) { return 7; }
    }
    public void setBackupKeepCount(int n)    { setDb("backup.keepCount", String.valueOf(n)); }

    // ---------------------------------------------------------------
    // LoTW (TrustedQSL) integration
    // ---------------------------------------------------------------

    public String getTqslPath()                  { return getDb("lotw.tqslPath", "tqsl"); }
    public void   setTqslPath(String v)          { setDb("lotw.tqslPath", v); }

    public String getLotwStationLocation()       { return getDb("lotw.stationLocation", ""); }
    public void   setLotwStationLocation(String v){ setDb("lotw.stationLocation", v); }
}
