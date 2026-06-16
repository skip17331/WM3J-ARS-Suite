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
    private static final Macro[] DEFAULTS = {
            new Macro("CQ",      "CQ CQ CQ DE WM3J WM3J WM3J K", ""),
            new Macro("TU 73",   "{CALL} TU 73 GL DE WM3J",      ""),
            new Macro("Name?",   "UR NAME? BK",                  ""),
            new Macro("QTH?",    "UR QTH? BK",                   ""),
            new Macro("My Call", "WM3J WM3J",                    ""),
            new Macro("Agn?",    "AGN AGN? PSE BK",              ""),
            new Macro("Roger",   "R R {CALL} DE WM3J",           ""),
            new Macro("QRZ?",    "QRZ? DE WM3J",                 ""),
    };
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

    /** Play a voice-keyer WAV/AIFF/AU file via javax.sound (no extra deps). Returns true if it started. */
    public static boolean playVoice(String path) {
        File f = resolve(path);
        if (f == null || !f.isFile()) return false;
        try {
            javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(javax.sound.sampled.AudioSystem.getAudioInputStream(f));
            clip.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
