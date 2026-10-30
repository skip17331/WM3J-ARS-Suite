package com.ars.fx.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The bundled amateur-satellite registry: radio data (frequencies, type, modes) keyed by NORAD id. */
public final class SatRegistry {
    private SatRegistry() {}

    public record Entry(int norad, String name, String type, long downlinkHz, long uplinkHz, String mode,
                        List<String> modes, String status, String notes, boolean defaultEnabled) {}

    private static volatile List<Entry> cache;

    public static List<Entry> all() {
        List<Entry> c = cache;
        if (c == null) { c = load(); cache = c; }
        return c;
    }

    public static Map<Integer, Entry> byNorad() {
        Map<Integer, Entry> m = new LinkedHashMap<>();
        for (Entry e : all()) if (e.norad() > 0) m.put(e.norad(), e);   // id 0 = placeholder; elements are keyed by NORAD id
        return m;
    }

    private static List<Entry> load() {
        List<Entry> list = new ArrayList<>();
        try (InputStream in = SatRegistry.class.getResourceAsStream("/com/ars/fx/sat/satellite-registry.json");
             Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            JsonArray arr = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray(root.getAsJsonObject().keySet().iterator().next());
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                List<String> modes = new ArrayList<>();
                if (o.has("modes")) for (JsonElement m : o.getAsJsonArray("modes")) modes.add(m.getAsString());
                list.add(new Entry(o.get("noradId").getAsInt(), o.get("name").getAsString(),
                        o.has("type") ? o.get("type").getAsString() : "",
                        o.has("downlinkHz") ? o.get("downlinkHz").getAsLong() : 0,
                        o.has("uplinkHz") ? o.get("uplinkHz").getAsLong() : 0,
                        modes.isEmpty() ? "FM" : modes.get(0), modes,
                        o.has("status") ? o.get("status").getAsString() : "",
                        o.has("notes") ? o.get("notes").getAsString() : "",
                        !o.has("enabled") || o.get("enabled").getAsBoolean()));
            }
        } catch (Exception ignored) {}
        return List.copyOf(list);
    }
}
