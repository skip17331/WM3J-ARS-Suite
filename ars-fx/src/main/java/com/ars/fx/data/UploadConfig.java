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
                    new Field("password", "Signing-key password", true),
                    new Field("weblogin", "LoTW website login (to download)", false),
                    new Field("webpass", "LoTW website password", true))),
            new Service("eqsl", "eQSL.cc", "ADIF upload", List.of(
                    new Field("user", "Username", false),
                    new Field("password", "Password", true))),
            new Service("qrz_logbook", "QRZ Logbook", "Logbook API key (not the XML subscription)", List.of(
                    new Field("apikey", "Logbook API key", true))),
            new Service("clublog", "Club Log", "bulk ADIF upload", List.of(
                    new Field("callsign", "Callsign", false),
                    new Field("email", "Email", false),
                    new Field("password", "Password", true),
                    new Field("apikey", "API key", true))),
            new Service("wavelog", "Wavelog", "your Wavelog's address, a read/write API key (Account > API) and the station profile id", List.of(
                    new Field("url", "Wavelog address (https://...)", false),
                    new Field("apikey", "API key (read/write)", true),
                    new Field("station", "Station profile id", false))),
            new Service("cloudlog", "Cloudlog", "your Cloudlog's address, a read/write API key (Admin > API) and the station profile id", List.of(
                    new Field("url", "Cloudlog address (https://...)", false),
                    new Field("apikey", "API key (read/write)", true),
                    new Field("station", "Station profile id", false))),
            new Service("hrdlog", "HRDLog.net", "your HRDLog callsign and upload code (HRDLog: Options > Upload code); sends only QSOs not sent before", List.of(
                    new Field("callsign", "HRDLog callsign", false),
                    new Field("code", "Upload code", true))));

    private static JsonObject root;

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-log", "upload.json"); }

    public static synchronized boolean enabled(String id) {
        ensure();
        return root.has(id) && root.getAsJsonObject(id).has("enabled") && root.getAsJsonObject(id).get("enabled").getAsBoolean();
    }
    public static synchronized String field(String id, String key) {
        ensure();
        if (isSecret(id, key)) { String v = SecretStore.get(account(id, key)); if (!v.isEmpty()) return v; }   // the system keyring first
        if (!root.has(id)) return "";
        JsonObject o = root.getAsJsonObject(id);
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
    }
    public static synchronized void setEnabled(String id, boolean en) {
        ensure(); obj(id).addProperty("enabled", en); save();
    }
    public static synchronized void setField(String id, String key, String val) {
        ensure();
        if (isSecret(id, key) && SecretStore.put(account(id, key), val)) {        // kept in the system keyring: none of it goes in the file
            if (root.has(id)) root.getAsJsonObject(id).remove(key);
            save();
            return;
        }
        obj(id).addProperty(key, val == null ? "" : val); save();
    }

    /** A password, API key or token: kept in the system keyring when there is one. */
    static boolean isSecret(String id, String key) {
        if ("token".equals(key)) return true;                                        // Club Log's Most Wanted token (not in SERVICES)
        for (Service s : SERVICES) if (s.id().equals(id)) for (Field f : s.fields()) if (f.key().equals(key)) return f.secret();
        return false;
    }
    static String account(String id, String key) { return "upload." + id + "." + key; }

    /** Move every secret still in the file into the keyring (when there is one), leaving the file without them. */
    private static void migrate() {
        if (!SecretStore.available()) return;
        boolean changed = false;
        for (String id : new java.util.ArrayList<>(root.keySet())) {
            if (!root.get(id).isJsonObject()) continue;
            JsonObject o = root.getAsJsonObject(id);
            for (String key : new java.util.ArrayList<>(o.keySet())) {
                if (!isSecret(id, key) || o.get(key).isJsonNull() || !o.get(key).isJsonPrimitive()) continue;
                String v = o.get(key).getAsString();
                if (v.isBlank()) { o.remove(key); changed = true; continue; }
                if (SecretStore.put(account(id, key), v)) { o.remove(key); changed = true; }
            }
        }
        if (changed) save();
    }

    /** Test hook: forget what was read, so the next call reads the file again. */
    static synchronized void resetForTest() { root = null; }
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
        migrate();
    }
    private static void save() {
        try {
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, root.toString());
            PrivateFiles.restrict(p);                     // holds the upload passwords
        } catch (Exception ignored) { /* best-effort */ }
    }
}
