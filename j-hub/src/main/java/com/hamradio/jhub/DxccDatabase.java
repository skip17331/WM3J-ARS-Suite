package com.hamradio.jhub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * DxccDatabase — thin wrapper around the DXCC prefix JSON file.
 *
 * Provides:
 *   • lookup(prefix)  → entity info map or null
 *   • allPrefixes()   → sorted list of all known prefixes
 *
 * The actual enrichment logic lives in SpotEnricher which uses this
 * class for its backing data.  This class exists as a separately
 * testable data-access layer.
 */
public class DxccDatabase {

    private static final Logger log = LoggerFactory.getLogger(DxccDatabase.class);

    private static final DxccDatabase INSTANCE = new DxccDatabase();
    public static DxccDatabase getInstance() { return INSTANCE; }
    private DxccDatabase() { loadFromClasspath(); }

    // Each entry maps field name → value (String/Number)
    private final Map<String, JsonObject> prefixEntries = new LinkedHashMap<>();

    // ---------------------------------------------------------------
    // Load
    // ---------------------------------------------------------------

    private void loadFromClasspath() {
        try (InputStream is = getClass().getResourceAsStream("/dxcc/prefixes.json")) {
            if (is == null) {
                log.warn("prefixes.json not found on classpath");
                return;
            }
            JsonArray arr = JsonParser.parseReader(
                new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String prefix = obj.get("prefix").getAsString().toUpperCase();
                prefixEntries.put(prefix, obj);
            }
            log.debug("DxccDatabase loaded {} prefixes", prefixEntries.size());
        } catch (Exception e) {
            log.error("Failed to load prefixes.json", e);
        }
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /**
     * Direct prefix lookup (exact match).
     * @return  JsonObject with dxcc fields or null if not found
     */
    public JsonObject lookup(String prefix) {
        return prefixEntries.get(prefix == null ? null : prefix.toUpperCase());
    }

    /**
     * All known prefixes sorted alphabetically.
     */
    public List<String> allPrefixes() {
        List<String> list = new ArrayList<>(prefixEntries.keySet());
        Collections.sort(list);
        return list;
    }

    public int size() { return prefixEntries.size(); }
}
