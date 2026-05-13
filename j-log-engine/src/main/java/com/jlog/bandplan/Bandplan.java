package com.jlog.bandplan;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Top-level bandplan: a set of {@link BandRange}s organized under a
 * region id ({@code "IARU-R1"} / {@code "IARU-R2"} / {@code "IARU-R3"})
 * or a country override id ({@code "US"} for FCC subbands).
 *
 * <p>Lookup pattern for the consumer:
 * <ol>
 *   <li>Try {@code countryBandplan(operatorCountry).bandAt(hz)} — if a
 *       country-specific overlay exists (e.g. US FCC), use it; the country
 *       overlay's segments capture license-class subbands that IARU
 *       allocations don't.</li>
 *   <li>Fall back to {@code regionBandplan(region).bandAt(hz)}.</li>
 * </ol>
 *
 * <p>Single-region/single-country instance is normal; the
 * {@link BandplanLoader} returns a cached singleton at the suite level
 * with all known regions/countries pre-parsed.
 */
public final class Bandplan {

    private final String id;
    private final String displayName;
    private final Map<String, BandRange> byName;   // "20m" → range

    public Bandplan(String id, String displayName, List<BandRange> ranges) {
        this.id          = Objects.requireNonNull(id);
        this.displayName = displayName == null ? id : displayName;
        Map<String, BandRange> m = new LinkedHashMap<>();
        if (ranges != null) for (BandRange r : ranges) m.put(r.name(), r);
        this.byName = Collections.unmodifiableMap(m);
    }

    public String           id()          { return id;          }
    public String           displayName() { return displayName; }
    public List<BandRange>  bands()       { return List.copyOf(byName.values()); }
    public BandRange        band(String name) { return byName.get(name); }

    /** First {@link BandRange} containing {@code hz}, or {@code null}. */
    public BandRange bandAt(long hz) {
        for (BandRange r : byName.values()) if (r.contains(hz)) return r;
        return null;
    }

    /** First {@link BandSegment} containing {@code hz}, or {@code null}.
     *  Convenience for callers that only care about the segment, not the
     *  band wrapping it. */
    public BandSegment segmentAt(long hz) {
        BandRange r = bandAt(hz);
        return r == null ? null : r.segmentAt(hz);
    }
}
