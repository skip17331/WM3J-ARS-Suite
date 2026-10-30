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

    /** {@code dxcc} is 0 and {@code lat}/{@code lon} are NaN when only the continent is known. */
    public record Info(String country, String continent, int dxcc, double lat, double lon) {}

    private record Entry(String country, double lat, double lon) {}

    private static volatile Map<String, Entry> byDxcc;

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
        if (e == null) return null;
        Entry en = table().get(e.id());
        if (en == null) return new Info(null, e.continent(), 0, Double.NaN, Double.NaN);
        int n; try { n = Integer.parseInt(e.id()); } catch (NumberFormatException ex) { n = 0; }
        return new Info(en.country(), e.continent(), n, en.lat(), en.lon());
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
