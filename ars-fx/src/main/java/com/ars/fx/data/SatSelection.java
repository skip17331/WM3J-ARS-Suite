package com.ars.fx.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Which satellites J-Sat tracks, as NORAD ids, stored in J-Hub's config ({@code sat.enabled}).
 * Until the operator chooses, the selection falls back to the old j-sat/J-Hub names in
 * {@code ~/.j-sat/settings.json}, then to the registry's default-enabled flags.
 */
public final class SatSelection {
    private SatSelection() {}

    private static final String KEY = "sat.enabled";
    private static final String UNSET = "\u0001";

    public static Set<Integer> selected() {
        String v = HubConfig.get(KEY, UNSET);
        if (!UNSET.equals(v)) {
            Set<Integer> s = new TreeSet<>();
            for (String p : v.split(",")) { try { if (!p.isBlank()) s.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {} }
            return s;
        }
        return defaults();
    }

    /** What applies when the operator hasn't chosen: legacy named selection, else the registry defaults. */
    public static Set<Integer> defaults() {
        Set<String> names = legacyNames();
        Set<Integer> s = new TreeSet<>();
        for (SatRegistry.Entry e : SatRegistry.all()) {
            if (e.norad() <= 0) continue;
            if (names.isEmpty() ? e.defaultEnabled() : names.contains(e.name())) s.add(e.norad());
        }
        return s;
    }

    public static void setSelected(Set<Integer> ids) {
        StringBuilder sb = new StringBuilder();
        for (int id : new TreeSet<>(ids)) { if (sb.length() > 0) sb.append(','); sb.append(id); }
        HubConfig.set(KEY, sb.toString());
    }

    /** Forget the choice and go back to the defaults. */
    public static void reset() { HubConfig.remove(KEY); }

    private static Set<String> legacyNames() {
        Path p = Paths.get(System.getProperty("user.home"), ".j-sat", "settings.json");
        try (Reader r = Files.newBufferedReader(p)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("enabledSatellites")) {
                Set<String> s = new HashSet<>();
                for (JsonElement e : o.getAsJsonArray("enabledSatellites")) s.add(e.getAsString());
                return s;
            }
        } catch (Exception ignored) {}
        return Set.of();
    }
}
