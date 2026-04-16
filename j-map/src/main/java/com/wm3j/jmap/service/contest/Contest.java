package com.wm3j.jmap.service.contest;

import java.time.Instant;

/**
 * Represents a single amateur radio contest.
 */
public class Contest {

    private final String name;
    private final Instant startTime;
    private final Instant endTime;
    private final String bands;     // e.g. "160,80,40,20,15,10" or "All HF"
    private final String modes;     // e.g. "CW,SSB,RTTY" or "CW"
    private final String url;       // WA7BNM contest detail page

    public Contest(String name, Instant startTime, Instant endTime,
                   String bands, String modes, String url) {
        this.name      = name;
        this.startTime = startTime;
        this.endTime   = endTime;
        this.bands     = bands;
        this.modes     = modes;
        this.url       = url;
    }

    public String  getName()      { return name; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime()   { return endTime; }
    public String  getBands()     { return bands; }
    public String  getModes()     { return modes; }
    public String  getUrl()       { return url; }

    /** True if contest is currently running */
    public boolean isActive() {
        Instant now = Instant.now();
        return now.isAfter(startTime) && now.isBefore(endTime);
    }

    /** True if contest starts within the next 24 hours */
    public boolean isUpcoming() {
        Instant now = Instant.now();
        return now.isBefore(startTime) &&
               startTime.isBefore(now.plusSeconds(86400));
    }
}
