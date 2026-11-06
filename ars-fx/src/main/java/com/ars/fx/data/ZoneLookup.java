package com.ars.fx.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CQ zone of a place: the zone outlines the J-Map draws ({@code cq-zones.json}) turned into a point-in-polygon test, so a
 * Maidenhead grid square can name its zone. A callsign alone only gives its country's default zone (a W6 reads zone 5 where
 * it is really 3); the grid fixes that for the countries that span several zones.
 *
 * <p>The outlines are drawn to about half a degree, so a grid-derived zone is only trusted where it lies within the range of
 * zones the callsign's country spans (see {@link #cq(String, String)}); anything else falls back to the country files.
 * ITU zones have no outline data here and still come from the country files.
 */
public final class ZoneLookup {
    private ZoneLookup() {}

    private record Zone(int number, List<double[]> rings) {}

    private static volatile List<Zone> zones;

    private static List<Zone> zones() {
        List<Zone> z = zones;
        if (z == null) {
            z = new ArrayList<>();
            try (InputStream in = ZoneLookup.class.getResourceAsStream("/com/ars/fx/maps/cq-zones.json")) {
                if (in != null) {
                    JsonNode ents = new ObjectMapper().readTree(in).path("entities");
                    List<Zone> out = z;
                    ents.fieldNames().forEachRemaining(id -> {
                        try { out.add(new Zone(Integer.parseInt(id), rings(ents.path(id).path("svgPath").asText("")))); }
                        catch (NumberFormatException ignored) { }
                    });
                }
            } catch (Exception ignored) { /* no outlines: callers fall back to the country files */ }
            zones = z;
        }
        return z;
    }

    private static final Pattern PT = Pattern.compile("(-?\\d+(?:\\.\\d+)?),(-?\\d+(?:\\.\\d+)?)");

    /** An SVG path of M / L / Z commands into its rings, each a flat {x0, y0, x1, y1, ...}. */
    static List<double[]> rings(String path) {
        List<double[]> out = new ArrayList<>();
        for (String sub : path.split("(?=M)")) {
            List<Double> pts = new ArrayList<>();
            Matcher m = PT.matcher(sub);
            while (m.find()) { pts.add(Double.parseDouble(m.group(1))); pts.add(Double.parseDouble(m.group(2))); }
            if (pts.size() >= 6) out.add(pts.stream().mapToDouble(Double::doubleValue).toArray());
        }
        return out;
    }

    private static boolean inside(double[] ring, double x, double y) {
        boolean in = false;
        int n = ring.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = ring[2 * i], yi = ring[2 * i + 1], xj = ring[2 * j], yj = ring[2 * j + 1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) in = !in;
        }
        return in;
    }

    private static double area(double[] ring) {
        double a = 0;
        int n = ring.length / 2;
        for (int i = 0, j = n - 1; i < n; j = i++) a += ring[2 * j] * ring[2 * i + 1] - ring[2 * i] * ring[2 * j + 1];
        return Math.abs(a) / 2;
    }

    /** The CQ zone (1-40) at a point, or 0 when no outline holds it. Where outlines overlap, the smaller outline wins. */
    public static int cqAt(double lat, double lon) {
        if (Double.isNaN(lat) || Double.isNaN(lon) || lat < -90 || lat > 90) return 0;
        double[] p = DxccInfo.toMap(lat, ((lon + 180) % 360 + 360) % 360 - 180);
        int best = 0;
        double bestArea = Double.MAX_VALUE;
        for (Zone z : zones()) {
            for (double[] r : z.rings()) {
                if (!inside(r, p[0], p[1])) continue;
                double a = area(r);
                if (a < bestArea) { bestArea = a; best = z.number(); }
            }
        }
        return best;
    }

    /** Centre of a 4- or 6-character Maidenhead square as {lat, lon}, or null when it is not a locator. */
    public static double[] gridCentre(String grid) {
        if (grid == null) return null;
        String g = grid.strip().toUpperCase(Locale.ROOT);
        if (g.length() != 4 && g.length() != 6 && g.length() != 8) return null;
        if (!g.matches("[A-R]{2}\\d{2}([A-X]{2}(\\d{2})?)?")) return null;
        double lon = (g.charAt(0) - 'A') * 20 - 180 + (g.charAt(2) - '0') * 2;
        double lat = (g.charAt(1) - 'A') * 10 - 90 + (g.charAt(3) - '0');
        if (g.length() == 4) return new double[]{lat + 0.5, lon + 1.0};
        lon += (g.charAt(4) - 'A') * (2.0 / 24); lat += (g.charAt(5) - 'A') * (1.0 / 24);
        return new double[]{lat + 1.0 / 48, lon + 1.0 / 24};
    }

    /**
     * The CQ zone of a grid square: the zone most of a 5 x 5 spread of points inside the square falls in (a coastal square's
     * centre can be out at sea, where the outlines are not reliable). 0 when no outline holds any of them, or when two zones tie.
     */
    public static int cqFromGrid(String grid) {
        double[] c = gridCentre(grid);
        if (c == null) return 0;
        int len = grid.strip().length();
        double dLat = len == 4 ? 1.0 : 1.0 / 24, dLon = len == 4 ? 2.0 : 2.0 / 24;      // the square's size (8 characters: use the 6 size)
        Map<Integer, Integer> votes = new TreeMap<>();
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            int z = cqAt(c[0] + i * dLat * 0.4, c[1] + j * dLon * 0.4);
            if (z > 0) votes.merge(z, 1, Integer::sum);
        }
        int best = 0, top = 0; boolean tie = false;
        for (Map.Entry<Integer, Integer> e : votes.entrySet()) {
            if (e.getValue() > top) { top = e.getValue(); best = e.getKey(); tie = false; }
            else if (e.getValue() == top) tie = true;
        }
        return tie ? 0 : best;
    }

    /**
     * {CQ zone, ITU zone} for a call, using the grid square to settle the CQ zone where the call's country spans several.
     * The ITU zone is the country files' answer.
     */
    public static int[] zones(String call, String grid) {
        CtyData cty = CtyData.current();
        CtyData.Match m = cty.lookup(call);
        if (m == null) return new int[]{0, 0};
        int cq = m.cq();
        java.util.Set<Integer> options = new java.util.TreeSet<>(cty.cqZones(m.entity()));
        options.addAll(EXTRA.getOrDefault(m.entity().name(), java.util.Set.of()));
        if (options.size() > 1) {
            int g = cqFromGrid(grid);
            // the country files only list some of a country's zones (Canada shows 1, 2, 4 and 5 but not BC's 3), so a grid zone is
            // trusted when it lies within the range the country spans; a grid typed in another part of the world is ignored
            if (g > 0 && g >= java.util.Collections.min(options) && g <= java.util.Collections.max(options)) cq = g;
        }
        return new int[]{cq, m.itu()};
    }

    /** Zones a country really spans that the country files only tell apart by exact callsign (VK6 is zone 29, the rest of Australia 30). */
    private static final Map<String, java.util.Set<Integer>> EXTRA = Map.of("Australia", java.util.Set.of(29, 30));

    /** Convenience for the CQ zone alone. */
    public static int cq(String call, String grid) { return zones(call, grid)[0]; }

    /** For tests and diagnostics: zone number to the number of rings it holds. */
    static Map<Integer, Integer> ringCounts() {
        Map<Integer, Integer> m = new TreeMap<>();
        for (Zone z : zones()) m.put(z.number(), z.rings().size());
        return m;
    }
}
