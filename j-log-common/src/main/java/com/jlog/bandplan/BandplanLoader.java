package com.jlog.bandplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Loads the suite-wide bandplan reference from
 * {@code /com/jlog/bandplan/bandplans.json} on the classpath. Caches the
 * parsed tree as a process-wide singleton; subsequent calls are O(1).
 *
 * <p>Consumers normally want one of three things:
 * <ul>
 *   <li>{@link #region(String)} — IARU R1 / R2 / R3 view of a band.</li>
 *   <li>{@link #country(String)} — license-class overlay (only {@code "US"}
 *       in v1; others can be added without code changes).</li>
 *   <li>{@link #describe(long)} — one-shot "what's at {@code hz}?" given
 *       the operator's preferred region and country, returning a small
 *       record suitable for status-bar display.</li>
 * </ul>
 *
 * <h2>JSON shape</h2>
 * <pre>
 * {
 *   "regions":   { "IARU-R2": { "name": "...", "bands": [ ... ] } },
 *   "countries": { "US":       { "name": "FCC Part 97", "bands": [ ... ] } }
 * }
 * </pre>
 *
 * <p>Each "bands" entry is {@code {"name":"20m","lower":14000000,"upper":14350000,
 * "segments":[{"lower":...,"upper":...,"activity":"CW","label":"..."}]}}.
 */
public final class BandplanLoader {

    private static final Logger log = LoggerFactory.getLogger(BandplanLoader.class);
    private static final String RESOURCE = "/com/jlog/bandplan/bandplans.json";

    private static final BandplanLoader INSTANCE = new BandplanLoader();
    public  static BandplanLoader getInstance() { return INSTANCE; }

    /** Default operator region — overridable per call, but lets callers
     *  that don't track region drop straight into {@link #describe(long)}. */
    public static final String DEFAULT_REGION  = "IARU-R2";
    /** Default operator country overlay — null means "no override, IARU only". */
    public static final String DEFAULT_COUNTRY = "US";

    private final Map<String, Bandplan> regions   = new LinkedHashMap<>();
    private final Map<String, Bandplan> countries = new LinkedHashMap<>();
    private boolean loaded = false;
    private final Object lock = new Object();

    private BandplanLoader() {}

    /** Force a fresh parse — test hook. */
    public void reset() {
        synchronized (lock) {
            regions.clear();
            countries.clear();
            loaded = false;
        }
    }

    public Bandplan region(String regionId) {
        ensureLoaded();
        return regions.get(regionId);
    }

    public Bandplan country(String countryCode) {
        ensureLoaded();
        return countryCode == null ? null : countries.get(countryCode.toUpperCase(Locale.ROOT));
    }

    public List<String> regionIds()    { ensureLoaded(); return List.copyOf(regions.keySet()); }
    public List<String> countryCodes() { ensureLoaded(); return List.copyOf(countries.keySet()); }

    /**
     * "What's at {@code hz}?" — convenience for status-bar overlays.
     * Tries the country override first (license-class subband info),
     * falls back to the IARU region plan, returns {@code null} when
     * {@code hz} isn't in any known amateur band.
     */
    public Description describe(long hz, String region, String country) {
        ensureLoaded();
        // Country overlay first (US-Extra/Advanced/General subbands etc.)
        Bandplan c = country(country);
        if (c != null) {
            BandRange   r = c.bandAt(hz);
            BandSegment s = r == null ? null : r.segmentAt(hz);
            if (r != null && s != null) {
                return new Description(r.name(), s.activity(), s.label(), c.id());
            }
        }
        // Region fallback
        Bandplan rgn = region(region != null ? region : DEFAULT_REGION);
        if (rgn != null) {
            BandRange   r = rgn.bandAt(hz);
            BandSegment s = r == null ? null : r.segmentAt(hz);
            if (r != null && s != null) {
                return new Description(r.name(), s.activity(), s.label(), rgn.id());
            }
            if (r != null) {
                // In-band but no specific segment matched.
                return new Description(r.name(), Activity.MIXED, "in-band", rgn.id());
            }
        }
        return null;
    }

    /** {@link #describe(long, String, String)} with the {@link #DEFAULT_REGION}
     *  and {@link #DEFAULT_COUNTRY}. */
    public Description describe(long hz) {
        return describe(hz, DEFAULT_REGION, DEFAULT_COUNTRY);
    }

    private void ensureLoaded() {
        if (loaded) return;
        synchronized (lock) {
            if (loaded) return;
            try (InputStream in = BandplanLoader.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    log.warn("bandplans.json missing from classpath; bandplan API will be empty");
                    loaded = true;
                    return;
                }
                JsonNode root = new ObjectMapper().readTree(in);
                parseSection(root.get("regions"),   regions);
                parseSection(root.get("countries"), countries);
                log.info("Bandplan loaded: {} region(s), {} country override(s)",
                        regions.size(), countries.size());
            } catch (IOException e) {
                log.warn("Bandplan load failed: {}", e.getMessage());
            }
            loaded = true;
        }
    }

    private static void parseSection(JsonNode node, Map<String, Bandplan> target) {
        if (node == null || !node.isObject()) return;
        Iterator<Map.Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            String id = e.getKey();
            JsonNode body = e.getValue();
            String name = textOrNull(body.get("name"));
            JsonNode bandsNode = body.get("bands");
            List<BandRange> bands = new ArrayList<>();
            if (bandsNode != null && bandsNode.isArray()) {
                for (JsonNode b : bandsNode) bands.add(parseBand(b));
            }
            target.put(id, new Bandplan(id, name, bands));
        }
    }

    private static BandRange parseBand(JsonNode b) {
        String name = textOrNull(b.get("name"));
        long lower  = b.get("lower").asLong();
        long upper  = b.get("upper").asLong();
        List<BandSegment> segs = new ArrayList<>();
        JsonNode segArr = b.get("segments");
        if (segArr != null && segArr.isArray()) {
            for (JsonNode s : segArr) {
                segs.add(new BandSegment(
                        s.get("lower").asLong(),
                        s.get("upper").asLong(),
                        Activity.valueOf(textOrNull(s.get("activity")).toUpperCase(Locale.ROOT)),
                        textOrNull(s.get("label"))));
            }
        }
        return new BandRange(name, lower, upper, segs);
    }

    private static String textOrNull(JsonNode n) {
        return n == null || n.isNull() ? null : n.asText();
    }

    /** Compact result of {@link BandplanLoader#describe(long)}. Suitable
     *  for "20m / CW (US Extra)" status-bar display. */
    public static final class Description {
        public final String   band;        // "20m"
        public final Activity activity;    // CW
        public final String   label;       // "CW segment (Extra)"
        public final String   sourceId;    // "US" or "IARU-R2"

        public Description(String band, Activity activity, String label, String sourceId) {
            this.band     = Objects.requireNonNull(band);
            this.activity = Objects.requireNonNull(activity);
            this.label    = label == null ? activity.name() : label;
            this.sourceId = sourceId == null ? "" : sourceId;
        }

        @Override public String toString() {
            return band + " / " + activity + (label.isEmpty() ? "" : " — " + label)
                    + " (" + sourceId + ")";
        }
    }
}
