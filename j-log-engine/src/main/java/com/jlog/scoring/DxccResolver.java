package com.jlog.scoring;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.util.CallsignRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves an amateur callsign to its DXCC entity (the CQ WW "country"
 * multiplier key — the unique DXCC entity number) and continent code
 * (NA/SA/EU/AS/AF/OC).
 *
 * <p>Data is a self-contained j-log resource
 * ({@code /com/jlog/scoring/dxcc-prefixes.json}, callsign-prefix →
 * {country, continent, dxcc} records). This class adds no shared API and
 * does not touch j-log-engine or j-hub.
 *
 * <p>Resolution is a longest-prefix match on the
 * {@link CallsignRegion#normalise(String) normalised} callsign — a
 * deliberate approximation for a running / claimed cockpit score; the CQ
 * WW Committee re-adjudicates countries from submitted logs. Known coarse
 * spots: Asiatic Russia resolves as Russia/EU, KH6 (Hawaii) as US/NA.
 * Maritime / aeronautical mobile ({@code /MM}, {@code /AM}) resolve to no
 * entity — CQ WW Rule IV.C: an MM station counts for a zone mult only.
 */
public final class DxccResolver {

    private static final Logger log = LoggerFactory.getLogger(DxccResolver.class);
    private static final String RESOURCE = "/com/jlog/scoring/dxcc-prefixes.json";
    /** Fallback continent source — the 217-entity world-map data. Only its
     *  {@code entities}→continent is used (sound; what the map renders);
     *  its {@code aliasTargets} is a display-collapse table and is NOT used. */
    private static final String ENTITY_FALLBACK = "/com/jlog/maps/dxcc-entities.json";

    public record Entity(String id, String continent) {}

    /** whole callsign → entity, for the country files' exact-call entries ("exact":true rows): 4U1UN is the UN in New York,
     *  not Italy; KC4USV is Antarctica, not the USA. Checked before any prefix. */
    private final Map<String, Entity> byExactCall = new HashMap<>();
    /** prefix → resolved entity (dxcc id + continent). Primary table. */
    private final Map<String, Entity> byPrefix = new HashMap<>();
    /** length of the longest prefix in the table, so a lookup tries at most that many leading slices. */
    private final int maxPrefix;
    /** entity-prefix → entity (id = entity prefix). Fallback for entities the
     *  primary table lacks entirely (China BY, Israel 4X, …). */
    private final Map<String, Entity> entityFallback = new HashMap<>();
    private final int maxFallback;

    private static final DxccResolver INSTANCE = new DxccResolver();
    public static DxccResolver getInstance() { return INSTANCE; }

    private DxccResolver() {
        try (InputStream in = DxccResolver.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                log.warn("DXCC prefix data {} not found — CQ-WW country mult disabled", RESOURCE);
            } else {
                JsonNode arr = new ObjectMapper().readTree(in);
                if (arr.isArray()) {
                    for (JsonNode e : arr) {
                        String pfx  = e.path("prefix").asText("");
                        String cont = e.path("continent").asText("");
                        JsonNode dx = e.get("dxcc");
                        if (pfx.isEmpty() || cont.isEmpty() || dx == null) continue;
                        // Multiple prefixes can share a DXCC number; the entity
                        // id is that number so distinct-country counts collapse
                        // correctly. First write wins on duplicate prefixes.
                        (e.path("exact").asBoolean(false) ? byExactCall : byPrefix)
                                .putIfAbsent(pfx.toUpperCase(), new Entity(dx.asText(), cont));
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to load DXCC prefix data: {}", ex.getMessage());
        }
        maxPrefix = byPrefix.keySet().stream().mapToInt(String::length).max().orElse(1);

        try (InputStream in = DxccResolver.class.getResourceAsStream(ENTITY_FALLBACK)) {
            if (in != null) {
                JsonNode root = new ObjectMapper().readTree(in);
                JsonNode ents = root.path("entities");   // aliasTargets deliberately ignored
                ents.fieldNames().forEachRemaining(id -> {
                    String cont = ents.path(id).path("continent").asText("");
                    if (!cont.isEmpty() && !"??".equals(cont))
                        entityFallback.put(id.toUpperCase(), new Entity(id, cont));
                });
            }
        } catch (Exception ex) {
            log.warn("Failed to load DXCC entity fallback: {}", ex.getMessage());
        }
        maxFallback = entityFallback.keySet().stream().mapToInt(String::length).max().orElse(1);

        log.info("DxccResolver: {} prefixes + {} exact calls + {} entity fallbacks loaded",
                byPrefix.size(), byExactCall.size(), entityFallback.size());
    }

    /** True for maritime / aeronautical mobile — CQ WW: zone mult only, no country. */
    public static boolean isMaritimeOrAir(String rawCall) {
        if (rawCall == null) return false;
        String c = rawCall.trim().toUpperCase();
        return c.endsWith("/MM") || c.endsWith("/AM");
    }

    /** Resolve to {entity,continent}; null if MM/AM or unresolvable. */
    public Entity resolve(String rawCall) {
        if (rawCall == null || rawCall.isBlank() || isMaritimeOrAir(rawCall)) return null;
        Entity x = byExactCall.get(rawCall.trim().toUpperCase(java.util.Locale.ROOT));   // e.g. KC4/W1WCN as written
        if (x != null) return x;
        String call = CallsignRegion.normalise(rawCall);
        if (call.isEmpty()) return null;
        x = byExactCall.get(call);                           // 4U1UN/P is still 4U1UN
        if (x != null) return x;
        Entity e = longest(call, byPrefix, maxPrefix);       // longest match wins
        return e != null ? e : longest(call, entityFallback, maxFallback);   // entities the table lacks
    }

    private static Entity longest(String call, Map<String, Entity> table, int max) {
        for (int n = Math.min(call.length(), max); n >= 1; n--) {
            Entity e = table.get(call.substring(0, n));
            if (e != null) return e;
        }
        return null;
    }

    /** DXCC entity id (country multiplier key), or null if unresolvable/MM. */
    public String entityOf(String call) {
        Entity e = resolve(call);
        return e == null ? null : e.id();
    }

    /** Continent code (NA/SA/EU/AS/AF/OC), or null if unresolvable/MM. */
    public String continentOf(String call) {
        Entity e = resolve(call);
        return e == null ? null : e.continent();
    }
}
