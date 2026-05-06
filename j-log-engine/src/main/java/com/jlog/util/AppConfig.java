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
    // Hidden contest plugins — contest IDs the user has removed from the
    // chooser. Lets bundled (resource-only) plugins also be "removed".
    // ---------------------------------------------------------------

    public java.util.Set<String> getHiddenContestIds() {
        String raw = prefs.get("hiddenContestIds", "");
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        if (raw.isBlank()) return out;
        for (String s : raw.split(",")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }

    public void setHiddenContestIds(java.util.Set<String> ids) {
        prefs.put("hiddenContestIds", String.join(",", ids));
    }

    public void addHiddenContestId(String id) {
        if (id == null || id.isBlank()) return;
        java.util.Set<String> s = getHiddenContestIds();
        s.add(id);
        setHiddenContestIds(s);
    }

    public void removeHiddenContestId(String id) {
        if (id == null || id.isBlank()) return;
        java.util.Set<String> s = getHiddenContestIds();
        if (s.remove(id)) setHiddenContestIds(s);
    }

    // ---------------------------------------------------------------
    // Last band / mode (prefill for contest entry; persists until changed)
    // ---------------------------------------------------------------

    public String getLastBand()         { return prefs.get("lastBand", ""); }
    public void   setLastBand(String v) { prefs.put("lastBand", v == null ? "" : v); }

    public String getLastMode()         { return prefs.get("lastMode", ""); }
    public void   setLastMode(String v) { prefs.put("lastMode", v == null ? "" : v); }

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

    // ---------------------------------------------------------------
    // Cabrillo category defaults — persisted between exports so operators
    // don't re-enter them every weekend. Values map to Cabrillo 3.0 headers.
    // ---------------------------------------------------------------

    public String getCabOperator()     { return getDb("cab.operator",    "SINGLE-OP"); }
    public void   setCabOperator(String v)   { setDb("cab.operator", v); }
    public String getCabBand()         { return getDb("cab.band",        "ALL"); }
    public void   setCabBand(String v)       { setDb("cab.band", v); }
    public String getCabMode()         { return getDb("cab.mode",        "MIXED"); }
    public void   setCabMode(String v)       { setDb("cab.mode", v); }
    public String getCabPower()        { return getDb("cab.power",       "HIGH"); }
    public void   setCabPower(String v)      { setDb("cab.power", v); }
    public String getCabAssisted()     { return getDb("cab.assisted",    "NON-ASSISTED"); }
    public void   setCabAssisted(String v)   { setDb("cab.assisted", v); }
    public String getCabTransmitter()  { return getDb("cab.transmitter", "ONE"); }
    public void   setCabTransmitter(String v){ setDb("cab.transmitter", v); }
    public String getCabStation()      { return getDb("cab.station",     "FIXED"); }
    public void   setCabStation(String v)    { setDb("cab.station", v); }
    public String getCabTime()         { return getDb("cab.time",        ""); }
    public void   setCabTime(String v)       { setDb("cab.time", v); }
    public String getCabEmail()        { return getDb("cab.email",       ""); }
    public void   setCabEmail(String v)      { setDb("cab.email", v); }
    public String getCabClub()         { return getDb("cab.club",        ""); }
    public void   setCabClub(String v)       { setDb("cab.club", v); }

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
