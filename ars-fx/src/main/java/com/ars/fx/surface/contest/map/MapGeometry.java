package com.ars.fx.surface.contest.map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a contest multiplier map's geometry from a bundled JSON
 * ({@code /com/ars/fx/maps/<name>.json}). Each map is a viewBox + a set of
 * SVG-path regions keyed by multiplier id ({@code sections} for section/state
 * maps, {@code entities} for DXCC/zone maps), plus {@code aliasTargets}
 * (rendered-id → the plugin ids that should light it).
 */
public final class MapGeometry {
    private MapGeometry() {}

    public record Region(String svgPath, double labelX, double labelY) {}

    public static final class MapDef {
        public final double[] viewBox;
        public final Map<String, Region> regions;
        public final Map<String, List<String>> aliasTargets;  // primary -> aliases
        MapDef(double[] vb, Map<String, Region> r, Map<String, List<String>> a) { viewBox = vb; regions = r; aliasTargets = a; }
    }

    public static MapDef load(String name) {
        try (var in = MapGeometry.class.getResourceAsStream("/com/ars/fx/maps/" + name + ".json")) {
            if (in == null) return null;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            double[] vb = new double[4];
            var vbA = root.getAsJsonArray("viewBox");
            for (int i = 0; i < 4 && i < vbA.size(); i++) vb[i] = vbA.get(i).getAsDouble();

            JsonObject container = root.has("sections") ? root.getAsJsonObject("sections")
                    : root.has("entities") ? root.getAsJsonObject("entities") : new JsonObject();
            Map<String, Region> regions = new LinkedHashMap<>();
            for (var e : container.entrySet()) {
                JsonObject o = e.getValue().getAsJsonObject();
                String d = o.has("svgPath") ? o.get("svgPath").getAsString() : (o.has("d") ? o.get("d").getAsString() : "");
                double lx = o.has("labelX") ? o.get("labelX").getAsDouble() : 0;
                double ly = o.has("labelY") ? o.get("labelY").getAsDouble() : 0;
                if (!d.isBlank()) regions.put(e.getKey(), new Region(d, lx, ly));
            }

            Map<String, List<String>> aliases = new LinkedHashMap<>();
            if (root.has("aliasTargets") && root.get("aliasTargets").isJsonObject()) {
                for (var e : root.getAsJsonObject("aliasTargets").entrySet()) {
                    List<String> ids = new ArrayList<>();
                    if (e.getValue().isJsonArray()) for (var x : e.getValue().getAsJsonArray()) ids.add(x.getAsString());
                    else ids.add(e.getValue().getAsString());
                    aliases.put(e.getKey(), ids);
                }
            }
            return new MapDef(vb, regions, aliases);
        } catch (Exception e) {
            System.err.println("[contest-map] load failed for " + name + ": " + e.getMessage());
            return null;
        }
    }
}
