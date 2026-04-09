package com.hamclock.service.satellite;

import java.time.Instant;
import java.util.List;

/**
 * Collection of amateur satellite positions and ground tracks.
 */
public class SatelliteData {

    public record SatPosition(String name, String noradId,
                               double lat, double lon, double altKm,
                               List<double[]> groundTrack) {}

    private final List<SatPosition> satellites;
    private final Instant fetchedAt;

    public SatelliteData(List<SatPosition> satellites, Instant fetchedAt) {
        this.satellites = satellites;
        this.fetchedAt  = fetchedAt;
    }

    public List<SatPosition> getSatellites() { return satellites; }
    public Instant           getFetchedAt()  { return fetchedAt; }
}
