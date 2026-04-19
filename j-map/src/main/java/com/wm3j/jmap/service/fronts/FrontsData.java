package com.wm3j.jmap.service.fronts;

import java.time.Instant;
import java.util.List;

public class FrontsData {

    public enum FrontType {
        COLD, WARM, OCCLUDED, STATIONARY, TROUGH, UNKNOWN;

        public static FrontType fromFeat(String feat) {
            if (feat == null) return UNKNOWN;
            String f = feat.toLowerCase();
            if (f.contains("cold"))       return COLD;
            if (f.contains("warm"))       return WARM;
            if (f.contains("occluded"))   return OCCLUDED;
            if (f.contains("stationary")) return STATIONARY;
            if (f.contains("trough"))     return TROUGH;
            return UNKNOWN;
        }
    }

    public record Front(FrontType type, List<double[]> coords) {}

    private final List<Front> fronts;
    private final Instant     fetchedAt;

    public FrontsData(List<Front> fronts, Instant fetchedAt) {
        this.fronts    = fronts;
        this.fetchedAt = fetchedAt;
    }

    public List<Front> getFronts()  { return fronts; }
    public Instant     getFetchedAt() { return fetchedAt; }
}
