package com.ars.fx.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Shared station-macro presets, configured in J-Hub ▸ Data and consumed by J-Log.
 * Each macro carries a button label, a CW/RTTY/digital text message ({CALL}
 * expands to the worked station), and a voice-keyer audio file used in phone
 * modes. Persisted to {@code ~/.j-log/macros.json} so the dashboard edit and the
 * logger see the same set within and across sessions.
 */
public final class MacroConfig {
    private MacroConfig() {}

    public record Macro(String label, String text, String audio) {}

    private static final int N = 8;
    /** Default macros are templates: {MYCALL} etc. are filled from the Station / Rig settings at send time, so they
     *  are right for whoever is operating. See {@link #TAGS}. */
    private static final Macro[] DEFAULTS = {
            new Macro("CQ",      "CQ CQ CQ DE {MYCALL} {MYCALL} {MYCALL} K", ""),
            new Macro("TU 73",   "{CALL} TU 73 GL DE {MYCALL}",              ""),
            new Macro("Name/QTH?", "UR NAME AND QTH? BK",                    ""),
            new Macro("My Info", "{MYCALL} NAME {NAME} QTH {QTH} GRID {GRID} PWR {POWER}W", ""),
            new Macro("My Call", "{MYCALL} {MYCALL}",                        ""),
            new Macro("Agn?",    "AGN AGN? PSE BK",                          ""),
            new Macro("Roger",   "R R {CALL} DE {MYCALL}",                   ""),
            new Macro("QRZ?",    "QRZ? DE {MYCALL}",                         ""),
    };

    /** One placeholder a macro can contain. {@code dflt} is what the J-Hub page shows and uses when the operator has not
     *  changed the setting; it is null for settings that are personal (a model, an address, a zone), where an example would
     *  be wrong for most people — those must be filled in. */
    public record Tag(String tag, String key, String group, String desc, String dflt, boolean bool) {}

    private static Tag t(String tag, String key, String group, String desc, String dflt) { return new Tag(tag, key, group, desc, dflt, false); }
    private static Tag onoff(String tag, String key, String group, String desc, boolean dflt) { return new Tag(tag, key, group, desc, String.valueOf(dflt), true); }

    /** Every setting on the J-Hub Station, Rig, Rotor, Amplifier and Antenna pages that a macro can use. {CALL} (the
     *  station you are working) is separate: it is typed in the log, not a setting. */
    public static final java.util.List<Tag> TAGS = java.util.List.of(
            t("{MYCALL}", "station.call",     "STATION", "your callsign", null),
            t("{NAME}",   "station.name",     "STATION", "your name", null),
            t("{QTH}",    "station.qth",      "STATION", "your QTH (city, state)", null),
            t("{GRID}",   "station.grid",     "STATION", "your grid square", null),
            t("{LAT}",    "station.lat",      "STATION", "your latitude", null),
            t("{LON}",    "station.lon",      "STATION", "your longitude", null),
            t("{ITU}",    "station.ituZone",  "STATION", "your ITU zone", null),
            t("{CQZ}",    "station.cqZone",   "STATION", "your CQ zone", null),
            t("{IARU}",   "station.iaru",     "STATION", "your IARU region", "R2"),
            t("{POWER}",  "station.txPower",  "STATION", "your default TX power in watts", "100"),

            t("{RIG}",        "rig.model",        "RIG", "your transceiver", null),
            t("{RIGHAMLIB}",  "rig.hamlibModel",  "RIG", "the Hamlib model number of your rig", null),
            t("{RIGIF}",      "rig.interface",    "RIG", "rig interface (USB / Serial / Network)", "USB"),
            t("{RIGPORT}",    "rig.port",         "RIG", "rig serial port", "/dev/ttyUSB0"),
            t("{RIGBAUD}",    "rig.baud",         "RIG", "rig baud rate", "9600"),
            t("{CIV}",        "rig.civ",          "RIG", "Icom CI-V address", null),
            t("{RIGPOLL}",    "rig.poll",         "RIG", "rig poll interval in ms", "12"),
            t("{PTT}",        "rig.ptt",          "RIG", "PTT method (CAT / RTS / DTR / VOX)", "CAT"),
            t("{KEYER}",      "rig.keyer",        "RIG", "CW keyer", null),
            t("{TXDELAY}",    "tx.pttDelay",      "RIG", "PTT delay in seconds (J-Hub ▸ Data ▸ Transmit)", "0.2"),

            t("{ROTOR}",         "rotor.model",       "ROTOR", "your rotor", null),
            t("{ROTORHAMLIB}",   "rotor.hamlibModel", "ROTOR", "the Hamlib model number of your rotor", null),
            t("{ROTORIF}",       "rotor.interface",   "ROTOR", "rotor interface", "Serial"),
            t("{ROTORPORT}",     "rotor.port",        "ROTOR", "rotor serial port", "/dev/ttyUSB0"),
            t("{ROTORBAUD}",     "rotor.baud",        "ROTOR", "rotor baud rate", "9600"),
            t("{ROTORWRAP}",     "rotor.wrap",        "ROTOR", "where the rotor overlaps (North / South)", "North"),
            t("{ROTORPARK}",     "rotor.park",        "ROTOR", "rotor park azimuth in degrees", "0"),
            t("{ROTORDEADBAND}", "rotor.deadband",    "ROTOR", "rotor dead-band in degrees", "2"),

            t("{AMP}",            "amp.model",         "AMPLIFIER", "your amplifier", null),
            t("{AMPHAMLIB}",      "amp.hamlibModel",   "AMPLIFIER", "the Hamlib model number of your amplifier", null),
            t("{AMPIF}",          "amp.interface",     "AMPLIFIER", "amplifier interface", "USB"),
            t("{AMPPORT}",        "amp.port",          "AMPLIFIER", "amplifier serial port", "/dev/ttyACM0"),
            t("{AMPBAUD}",        "amp.baud",          "AMPLIFIER", "amplifier baud rate", "38400"),
            onoff("{AMPFOLLOW}",  "amp.followBand",    "AMPLIFIER", "amplifier follows the rig's band (ON / OFF)", true),
            t("{AMPMAXPOWER}",    "amp.maxPower",      "AMPLIFIER", "amplifier maximum power in watts", "1000"),
            onoff("{AMPINHIBIT}", "amp.standbyOnInhibit", "AMPLIFIER", "amplifier goes to standby on TX inhibit (ON / OFF)", true),
            t("{AMPMODE}",        "amp.opStandby",     "AMPLIFIER", "amplifier Operate / Standby", "Standby"),

            t("{ANT}",           "ant.model",      "ANTENNA SWITCH", "your antenna switch", null),
            t("{ANTIF}",         "ant.interface",  "ANTENNA SWITCH", "antenna switch interface", "Network"),
            t("{ANTHOST}",       "ant.host",       "ANTENNA SWITCH", "antenna switch host / IP", null),
            t("{ANTPORT}",       "ant.port",       "ANTENNA SWITCH", "antenna switch port", "9007"),
            onoff("{ANTFOLLOW}", "ant.followBand", "ANTENNA SWITCH", "antenna switch follows the rig's band (ON / OFF)", true));

    private static final java.util.Map<String, Tag> BY_KEY = new java.util.HashMap<>();
    static { for (Tag g : TAGS) BY_KEY.put(g.key(), g); }
    /** The tag a setting is available as, or null (e.g. a daemon port that macros have no use for). */
    public static Tag tagForKey(String key) { return BY_KEY.get(key); }

    /** Result of filling a macro: the text to send, and the descriptions of any needed settings that are empty. */
    public record Expanded(String text, java.util.List<String> missing) { public boolean ok() { return missing.isEmpty(); } }

    /** What a tag is worth right now: the operator's value, else the default the page shows, else "" (needs filling in). */
    public static String value(Tag g) {
        if (g.bool()) return HubConfig.getBool(g.key(), Boolean.parseBoolean(g.dflt())) ? "ON" : "OFF";
        String v = HubConfig.get(g.key(), g.dflt() == null ? "" : g.dflt());
        return v == null ? "" : v.trim();
    }

    /** Fill every placeholder. {CALL} becomes the worked station's call ("OM" when none is typed yet). A placeholder whose
     *  setting is empty is left in place and reported in {@code missing}, so the caller can refuse to send it. */
    public static Expanded expand(String text, String workedCall) {
        String out = text == null ? "" : text;
        String worked = workedCall == null || workedCall.isBlank() ? "OM" : workedCall.trim().toUpperCase(java.util.Locale.ROOT);
        out = out.replaceAll("(?i)\\{CALL\\}", java.util.regex.Matcher.quoteReplacement(worked));
        java.util.List<String> missing = new java.util.ArrayList<>();
        String upper = out.toUpperCase(java.util.Locale.ROOT);
        for (Tag g : TAGS) {
            if (!upper.contains(g.tag())) continue;
            String v = value(g);
            if (v.isEmpty()) { missing.add(g.tag() + " — " + g.desc() + " (" + g.group().toLowerCase(java.util.Locale.ROOT) + " page)"); continue; }
            out = out.replaceAll("(?i)" + java.util.regex.Pattern.quote(g.tag()), java.util.regex.Matcher.quoteReplacement(v.toUpperCase(java.util.Locale.ROOT)));
        }
        return new Expanded(out.toUpperCase(java.util.Locale.ROOT), missing);   // everything sent is upper case
    }

    /** Put the default (template) text and labels back on every macro; the voice-file paths are kept. */
    public static synchronized void resetToDefaults() {
        ensure();
        for (int i = 0; i < N; i++) macros[i] = new Macro(DEFAULTS[i].label(), DEFAULTS[i].text(), macros[i].audio());
        save();
    }
    private static Macro[] macros;

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-log", "macros.json"); }

    public static int count() { return N; }

    public static synchronized Macro get(int i) { ensure(); return macros[i]; }

    public static synchronized void set(int i, String label, String text, String audio) {
        ensure();
        macros[i] = new Macro(label == null ? "" : label, text == null ? "" : text, audio == null ? "" : audio);
        save();
    }

    private static void ensure() {
        if (macros != null) return;
        macros = DEFAULTS.clone();
        try {
            Path p = file();
            if (Files.exists(p)) {
                JsonArray a = JsonParser.parseString(Files.readString(p)).getAsJsonArray();
                for (int i = 0; i < N && i < a.size(); i++) {
                    JsonObject o = a.get(i).getAsJsonObject();
                    macros[i] = new Macro(str(o, "label", DEFAULTS[i].label()),
                                          str(o, "text", DEFAULTS[i].text()),
                                          str(o, "audio", ""));
                }
            }
        } catch (Exception ignored) { /* fall back to defaults */ }
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    private static void save() {
        try {
            JsonArray a = new JsonArray();
            for (Macro m : macros) {
                JsonObject o = new JsonObject();
                o.addProperty("label", m.label()); o.addProperty("text", m.text()); o.addProperty("audio", m.audio());
                a.add(o);
            }
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, a.toString());
        } catch (Exception ignored) { /* config is best-effort */ }
    }

    /** Resolve a possibly ~-prefixed path to a File. */
    public static File resolve(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path.startsWith("~") ? System.getProperty("user.home") + path.substring(1) : path;
        return new File(p);
    }

}
