package com.wm3j.jmap.service.pskreporter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of reception reports returned by a PSK Reporter query.
 */
public class PskReporterData {

    private List<PskSpot> spots = new ArrayList<>();
    private Instant fetchTime;

    public List<PskSpot> getSpots() { return spots; }
    public void setSpots(List<PskSpot> spots) { this.spots = spots; }

    public Instant getFetchTime() { return fetchTime; }
    public void setFetchTime(Instant fetchTime) { this.fetchTime = fetchTime; }
}
