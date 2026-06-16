package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Credentials for the online log services J-Log uploads to — LoTW (via TrustedQSL),
 * eQSL, QRZ Logbook and Club Log — edited in J-Hub ▸ Data and persisted to
 * {@code ~/.j-log/upload.json}. The TQSL binary path is stored globally.
 */
public final class UploadConfig {
    private UploadConfig() {}

    public record Field(String key, String label, boolean secret) {}
    public record Service(String id, String label, String hint, List<Field> fields) {}

    public static final List<Service> SERVICES = List.of(
            new Service("lotw", "LoTW (ARRL)", "signed upload via TrustedQSL (tqsl)", List.of(
                    new Field("location", "Station location", false),
                    new Field("password", "Signing-key password", true))),
            new Service("eqsl", "eQSL.cc", "ADIF upload", List.of(
                    new Field("user", "Username", false),
                    new Field("password", "Password", true))),
            new Service("qrz_logbook", "QRZ Logbook", "Logbook API key (not the XML subscription)", List.of(
                    new Field("apikey", "Logbook API key", true))),
            new Service("clublog", "Club Log", "bulk ADIF upload", List.of(
                    new Field("callsign", "Callsign", false),
                    new Field("email", "Email", false),
                    new Field("password", "Password", true),
                    new Field("apikey", "API key", true))));

    private static JsonObject root;

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-log", "upload.json"); }

    public static synchronized boolean enabled(String id) {
        ensure();
        return root.has(id) && root.getAsJsonObject(id).has("enabled") && root.getAsJsonObject(id).get("enabled").getAsBoolean();
    }
    public static synchronized String field(String id, String key) {
        ensure();
        if (!root.has(id)) return "";
        JsonObject o = root.getAsJsonObject(id);
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
    public static synchronized void setEnabled(String id, boolean en) {
        ensure(); obj(id).addProperty("enabled", en); save();
    }
    public static synchronized void setField(String id, String key, String val) {
        ensure(); obj(id).addProperty(key, val == null ? "" : val); save();
    }
    public static synchronized String tqslPath() {
        ensure();
        return root.has("tqslPath") && !root.get("tqslPath").isJsonNull() ? root.get("tqslPath").getAsString() : "";
    }
    public static synchronized void setTqslPath(String p) { ensure(); root.addProperty("tqslPath", p == null ? "" : p); save(); }

    public static synchronized boolean anyEnabled() {
        ensure();
        for (Service s : SERVICES) if (enabled(s.id())) return true;
        return false;
    }

    private static JsonObject obj(String id) {
        if (!root.has(id)) root.add(id, new JsonObject());
        return root.getAsJsonObject(id);
    }
    private static void ensure() {
        if (root != null) return;
        root = new JsonObject();
        try {
            Path p = file();
            if (Files.exists(p)) root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
        } catch (Exception ignored) { root = new JsonObject(); }
    }
    private static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, root.toString());
        } catch (Exception ignored) { /* best-effort */ }
    }
}
