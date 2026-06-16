package com.ars.fx.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Callsign-lookup provider configuration, edited in J-Hub ▸ Data and consumed by
 * {@link CallsignLookup}. Providers are tried in the listed order; the first
 * enabled one that returns a hit wins. Persisted to {@code ~/.j-log/lookup.json}.
 */
public final class LookupConfig {
    private LookupConfig() {}

    /** A provider definition: id, display label, hint, whether it needs user/secret, whether it needs a path. */
    public record Prov(String id, String label, String hint, boolean creds, boolean path) {}

    public static final List<Prov> PROVIDERS = List.of(
            new Prov("callook",    "Callook.info",        "US callbook · free, no login",    false, false),
            new Prov("hamqth",     "HamQTH",              "worldwide · free account",        true,  false),
            new Prov("qrz",        "QRZ.com XML",         "worldwide · XML subscription",    true,  false),
            new Prov("qrzcq",      "QRZCQ.com",           "worldwide · account",             true,  false),
            new Prov("hamcall",    "HamCall (online)",    "Buckmaster · subscription",       true,  false),
            new Prov("hamcall_cd", "HamCall (CD / local)","local callbook CSV file",         false, true));

    public record Setting(boolean enabled, String user, String secret, String extra) {}

    private static Map<String, Setting> map;

    private static Path file() { return Paths.get(System.getProperty("user.home"), ".j-log", "lookup.json"); }

    public static synchronized Setting get(String id) {
        ensure();
        return map.getOrDefault(id, new Setting(id.equals("callook"), "", "", ""));
    }

    public static synchronized void set(String id, boolean enabled, String user, String secret, String extra) {
        ensure();
        map.put(id, new Setting(enabled, nz(user), nz(secret), nz(extra)));
        save();
    }

    /** True if at least one provider is enabled (so a lookup is worth attempting). */
    public static synchronized boolean anyEnabled() {
        ensure();
        for (Prov p : PROVIDERS) if (get(p.id()).enabled()) return true;
        return false;
    }

    private static void ensure() {
        if (map != null) return;
        map = new LinkedHashMap<>();
        map.put("callook", new Setting(true, "", "", ""));     // free default
        try {
            Path p = file();
            if (Files.exists(p)) {
                JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
                for (Prov prov : PROVIDERS) {
                    if (!root.has(prov.id())) continue;
                    JsonObject o = root.getAsJsonObject(prov.id());
                    map.put(prov.id(), new Setting(o.has("enabled") && o.get("enabled").getAsBoolean(),
                            str(o, "user"), str(o, "secret"), str(o, "extra")));
                }
            }
        } catch (Exception ignored) { /* defaults */ }
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    private static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, Setting> e : map.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", e.getValue().enabled());
                o.addProperty("user", e.getValue().user());
                o.addProperty("secret", e.getValue().secret());
                o.addProperty("extra", e.getValue().extra());
                root.add(e.getKey(), o);
            }
            Path p = file();
            Files.createDirectories(p.getParent());
            Files.writeString(p, root.toString());
        } catch (Exception ignored) { /* best-effort */ }
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }
}
