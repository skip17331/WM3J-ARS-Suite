package com.ars.fx.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.util.Maidenhead;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Names for the award references typed on a QSO, and ideas for the ones left blank: a POTA park (api.pota.app), a SOTA
 * summit (api2.sota.org.uk), an IOTA group (the public IOTA list). Answers are kept on disk for a week
 * ({@link FeedFetcher}), so each reference is asked for once. All lookups block: call them off the FX thread.
 *
 * <p>The POTA interface is public but unofficial and may change; a failed lookup just returns null.
 */
public final class RefLookup {
    private RefLookup() {}

    public enum Kind { POTA, SOTA, IOTA }
    /** What a reference is: {@code detail} is a short extra line (type, points, region). */
    public record Ref(Kind kind, String ref, String name, String grid, String detail) {}
    public record IotaGroup(String ref, String name, int dxcc, double latMin, double latMax, double lonMin, double lonMax) {
        boolean contains(double lat, double lon) { return lat >= latMin && lat <= latMax && lon >= lonMin && lon <= lonMax; }
    }

    static final Pattern POTA = Pattern.compile("^[A-Z0-9]{1,3}-\\d{3,6}$"), SOTA = Pattern.compile("^[A-Z0-9]{1,3}/[A-Z]{2}-\\d{3}$"),
            IOTA = Pattern.compile("^(AF|AN|AS|EU|NA|OC|SA)-\\d{3}$");
    private static final Duration KEEP = Duration.ofDays(7);
    private static final ObjectMapper JSON = new ObjectMapper();

    public static String normalise(String ref) { return ref == null ? "" : ref.trim().toUpperCase(Locale.ROOT); }
    public static boolean looksLikePota(String r) { return POTA.matcher(normalise(r)).matches(); }
    public static boolean looksLikeSota(String r) { return SOTA.matcher(normalise(r)).matches(); }
    public static boolean looksLikeIota(String r) { return IOTA.matcher(normalise(r)).matches(); }

    // ---- POTA / SOTA ----------------------------------------------------------------------------------
    public static Ref pota(String ref) {
        String r = normalise(ref);
        if (!looksLikePota(r)) return null;
        String body = get("pota-" + r, "https://api.pota.app/park/" + r);
        if (body == null || body.isBlank() || body.trim().equals("null")) return null;
        try {
            JsonNode n = JSON.readTree(body);
            String name = n.path("name").asText("");
            if (name.isEmpty()) return null;
            String grid = n.path("grid6").asText(n.path("grid4").asText(""));
            String where = n.path("locationDesc").asText("");
            return new Ref(Kind.POTA, r, name, grid, where);
        } catch (Exception e) { return null; }
    }

    public static Ref sota(String ref) {
        String r = normalise(ref);
        if (!looksLikeSota(r)) return null;
        String body = get("sota-" + r.replace('/', '_'), "https://api2.sota.org.uk/api/summits/" + r);
        if (body == null || body.isBlank()) return null;
        try {
            JsonNode n = JSON.readTree(body);
            String name = n.path("name").asText("");
            if (name.isEmpty()) return null;
            String detail = n.path("points").asInt(0) + " pts · " + n.path("altM").asInt(0) + " m · " + n.path("associationName").asText("");
            return new Ref(Kind.SOTA, r, name, n.path("locator").asText(""), detail);
        } catch (Exception e) { return null; }
    }

    // ---- IOTA -----------------------------------------------------------------------------------------
    private static volatile List<IotaGroup> iotaGroups;
    private static volatile String iotaFrom;

    static void resetForTest() { iotaGroups = null; iotaFrom = null; }

    /** The IOTA groups (about 1,200). Downloaded once a week; empty when the list cannot be had. */
    static List<IotaGroup> iota() {
        String body = get("iota-list", "https://www.iota-world.org/islands-on-the-air/downloads/download-file.html?path=fulllist.json");
        if (body == null) return List.of();
        if (iotaGroups != null && body.equals(iotaFrom)) return iotaGroups;
        List<IotaGroup> out = new ArrayList<>();
        try {
            for (JsonNode g : JSON.readTree(body)) {
                out.add(new IotaGroup(g.path("refno").asText(""), g.path("name").asText(""), num(g.path("dxcc_num").asText("0")),
                        dbl(g.path("latitude_min").asText()), dbl(g.path("latitude_max").asText()),
                        dbl(g.path("longitude_min").asText()), dbl(g.path("longitude_max").asText())));
            }
        } catch (Exception e) { return List.of(); }
        iotaGroups = out; iotaFrom = body;
        return out;
    }
    private static int num(String s) { try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; } }
    private static double dbl(String s) { try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return Double.NaN; } }

    public static Ref iota(String ref) {
        String r = normalise(ref);
        if (!looksLikeIota(r)) return null;
        for (IotaGroup g : iota()) if (g.ref().equals(r)) return new Ref(Kind.IOTA, r, g.name(), "", "DXCC " + g.dxcc());
        return null;
    }

    /**
     * IOTA groups the other station could be on: those in its DXCC entity (all of them if few), narrowed to the groups whose area
     * contains its grid square when that is known. At most {@code limit}; empty when nothing sensible can be said.
     */
    public static List<IotaGroup> suggestIota(int dxccNumber, String grid, int limit) {
        if (dxccNumber <= 0) return List.of();
        List<IotaGroup> inEntity = new ArrayList<>();
        for (IotaGroup g : iota()) if (g.dxcc() == dxccNumber) inEntity.add(g);
        if (inEntity.isEmpty()) return List.of();
        if (grid != null && Maidenhead.isValid(grid.trim().toUpperCase(Locale.ROOT))) {
            double[] c = Maidenhead.center(grid.trim().toUpperCase(Locale.ROOT));
            List<IotaGroup> here = new ArrayList<>();
            for (IotaGroup g : inEntity) if (g.contains(c[0], c[1])) here.add(g);
            if (!here.isEmpty()) inEntity = here;
        }
        return inEntity.size() > limit ? new ArrayList<>(inEntity.subList(0, limit)) : inEntity;
    }

    private static String get(String key, String url) {
        FeedFetcher.Fetched f = FeedFetcher.get(key, url, false, Instant.now(), KEEP);
        return f.body();
    }
}
