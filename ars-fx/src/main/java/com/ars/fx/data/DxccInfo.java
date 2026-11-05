package com.ars.fx.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.scoring.DxccResolver;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Country, continent and position for a callsign, from the engine's DXCC prefix data ({@link DxccResolver} picks the
 * entity; the same data file supplies its name and a representative point), plus the great-circle helpers used to
 * turn that into a distance and bearing from the station.
 */
public final class DxccInfo {
    private DxccInfo() {}

    /** {@code dxcc} is 0 for an entity missing from the prefix data; {@code lat}/{@code lon} are NaN when no position is known. */
    public record Info(String country, String continent, int dxcc, double lat, double lon) {}

    private record Entry(String country, double lat, double lon) {}

    private static volatile Map<String, Entry> byDxcc;
    private static volatile Map<String, Entry> outlines;

    /** Entities the prefix data lacks. The map outlines have no coordinates, but each has a label position to work back from. */
    private static Map<String, Entry> outlines() {
        Map<String, Entry> m = outlines;
        if (m == null) {
            m = new HashMap<>();
            try (InputStream in = DxccResolver.class.getResourceAsStream("/com/jlog/maps/dxcc-entities.json")) {
                if (in != null) {
                    JsonNode ents = new ObjectMapper().readTree(in).path("entities");
                    Map<String, Entry> out = m;
                    ents.fieldNames().forEachRemaining(id -> {
                        JsonNode n = ents.path(id);
                        double[] ll = fromMap(n.path("labelX").asDouble(Double.NaN), n.path("labelY").asDouble(Double.NaN));
                        out.put(id, new Entry(n.path("name").asText(null), ll[0], ll[1]));
                    });
                }
            } catch (Exception ignored) { /* no outlines just means continent only */ }
            outlines = m;
        }
        return m;
    }

    // Robinson projection table (every 5 degrees of latitude), and how dxcc-entities.json is laid out on it.
    private static final double[] ROBIN_X = {1, .9986, .9954, .9900, .9822, .9730, .9600, .9427, .9216, .8962, .8679, .8350, .7986, .7597, .7186, .6732, .6213, .5722, .5322};
    private static final double[] ROBIN_Y = {0, .0620, .1240, .1860, .2480, .3100, .3720, .4340, .4958, .5571, .6176, .6769, .7346, .7903, .8435, .8936, .9394, .9761, 1};
    private static final double MAP_CX = 595.97, MAP_CY = 315.80, MAP_SX = 3.3178, MAP_SY = 307.16;

    /** Map position → {lat, lon}. Good to about a degree for a small country; a label can sit far from a big one's centre. */
    private static double[] fromMap(double x, double y) {
        if (Double.isNaN(x) || Double.isNaN(y)) return new double[]{Double.NaN, Double.NaN};
        double north = (MAP_CY - y) / MAP_SY, v = Math.min(Math.abs(north), 1);
        int i = 0;
        while (i < ROBIN_Y.length - 2 && v > ROBIN_Y[i + 1]) i++;
        double f = (v - ROBIN_Y[i]) / (ROBIN_Y[i + 1] - ROBIN_Y[i]);
        double lat = Math.copySign((i + f) * 5, north);
        double lon = (x - MAP_CX) / MAP_SX / (ROBIN_X[i] + f * (ROBIN_X[i + 1] - ROBIN_X[i]));
        return new double[]{lat, lon};
    }

    /** DXCC number → country name, centre point. Read once from the engine's prefix data. */
    private static Map<String, Entry> table() {
        Map<String, Entry> m = byDxcc;
        if (m == null) {
            m = new HashMap<>();
            try (InputStream in = DxccResolver.class.getResourceAsStream("/com/jlog/scoring/dxcc-prefixes.json")) {
                if (in != null) for (JsonNode n : new ObjectMapper().readTree(in)) {
                    String id = n.path("dxcc").asText(""), name = n.path("country").asText("");
                    if (id.isEmpty() || name.isEmpty()) continue;
                    m.putIfAbsent(id, new Entry(name, n.path("lat").asDouble(Double.NaN), n.path("lon").asDouble(Double.NaN)));
                }
            } catch (Exception ignored) { /* an empty table just means no names */ }
            byDxcc = m;
        }
        return m;
    }

    /** What is known about {@code call}'s DXCC entity, or null when it cannot be resolved (or is maritime / aero mobile). */
    public static Info of(String call) {
        DxccResolver.Entity e;
        try { e = DxccResolver.getInstance().resolve(call); } catch (RuntimeException ex) { e = null; }
        if (e == null) return fromCty(call);          // the engine's table has no answer: ask the contest country files
        Entry en = table().get(e.id());
        if (en == null) {
            Entry o = outlines().get(e.id());
            return o == null ? new Info(null, e.continent(), 0, Double.NaN, Double.NaN)
                             : new Info(o.country(), e.continent(), 0, o.lat(), o.lon());
        }
        int n; try { n = Integer.parseInt(e.id()); } catch (NumberFormatException ex) { n = 0; }
        return new Info(en.country(), e.continent(), n, en.lat(), en.lon());
    }

    private static final java.util.regex.Pattern CALLISH = java.util.regex.Pattern.compile("[A-Z0-9]{1,3}[0-9][A-Z0-9]*[A-Z](/[A-Z0-9]+)*|[A-Z0-9]+/[A-Z0-9]+");
    private static final Map<CtyData.Entity, Integer> BRIDGE = new java.util.concurrent.ConcurrentHashMap<>();

    /** The contest country files (cty.dat) as a second opinion for a call the prefix table cannot place. */
    private static Info fromCty(String call) {
        if (call == null) return null;
        String c = call.strip().toUpperCase(java.util.Locale.ROOT);
        if (!CALLISH.matcher(c).matches()) return null;                    // junk such as "TEST" must not land in an entity
        CtyData.Match m = CtyData.bundled().lookup(c);
        if (m == null) return null;
        CtyData.Entity en = m.entity();
        int n = bridgeNumber(en);
        // when the engine knows the entity under another prefix, use its number and name so the entity is counted once
        String name = n > 0 ? countryName(String.valueOf(n)) : en.name();
        return new Info(name, m.continent(), n, en.lat(), en.lonEast());
    }

    /** The engine's DXCC number for a cty.dat entity, found by resolving one of its other prefixes; 0 when it has none. */
    private static int bridgeNumber(CtyData.Entity en) {
        return BRIDGE.computeIfAbsent(en, k -> {
            for (String p : CtyData.bundled().aliasPrefixes(k)) {
                String probe = p + (Character.isDigit(p.charAt(p.length() - 1)) ? "AA" : "1AA");
                CtyData.Match pm = CtyData.bundled().lookup(probe);
                if (pm == null || pm.entity() != k) continue;
                try {
                    DxccResolver.Entity r = DxccResolver.getInstance().resolve(probe);
                    if (r != null) return Integer.parseInt(r.id());
                } catch (RuntimeException ignored) { /* try the next prefix */ }
            }
            return 0;
        });
    }

    /** The country name for a DXCC entity id as the resolver reports it ("230" → "Germany"); an unknown id passes through. */
    public static String countryName(String entityId) {
        Entry en = table().get(entityId);
        return en == null ? entityId : en.country();
    }

    private static final double EARTH_KM = 6371.0;

    /** Great-circle distance in km. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_KM * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));
    }

    /** Initial great-circle bearing in degrees (0–360, true north) from point 1 to point 2. */
    public static double bearingDeg(double lat1, double lon1, double lat2, double lon2) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2), dl = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dl) * Math.cos(p2);
        double x = Math.cos(p1) * Math.sin(p2) - Math.sin(p1) * Math.cos(p2) * Math.cos(dl);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }
}
