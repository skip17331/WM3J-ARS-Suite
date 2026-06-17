package com.jlog.bandplan;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One amateur band: a name ({@code "20m"}), the overall lower/upper edges,
 * and an ordered list of {@link BandSegment}s subdividing it by activity.
 */
public final class BandRange {

    private final String name;
    private final long   lowerHz;
    private final long   upperHz;
    private final List<BandSegment> segments;

    public BandRange(String name, long lowerHz, long upperHz, List<BandSegment> segments) {
        this.name     = Objects.requireNonNull(name, "name");
        this.lowerHz  = lowerHz;
        this.upperHz  = upperHz;
        this.segments = List.copyOf(segments == null ? List.of() : segments);
    }

    public String name()    { return name;    }
    public long   lowerHz() { return lowerHz; }
    public long   upperHz() { return upperHz; }
    public List<BandSegment> segments() { return Collections.unmodifiableList(segments); }

    public boolean contains(long hz) { return hz >= lowerHz && hz <= upperHz; }

    /** First segment containing {@code hz}, or {@code null} if none. */
    public BandSegment segmentAt(long hz) {
        for (BandSegment s : segments) if (s.contains(hz)) return s;
        return null;
    }

    @Override public String toString() {
        return name + " (" + (lowerHz / 1000) + "-" + (upperHz / 1000) + " kHz, "
             + segments.size() + " segments)";
    }
}
