package com.hamclock.service.contest;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Container for a fetched list of upcoming and active contests.
 */
public class ContestList {

    private final List<Contest> contests;
    private final Instant fetchedAt;

    public ContestList(List<Contest> contests, Instant fetchedAt) {
        this.contests  = contests;
        this.fetchedAt = fetchedAt;
    }

    public List<Contest> getContests()  { return contests; }
    public Instant       getFetchedAt() { return fetchedAt; }

    public List<Contest> getActive() {
        return contests.stream().filter(Contest::isActive).collect(Collectors.toList());
    }

    public List<Contest> getUpcoming() {
        return contests.stream().filter(Contest::isUpcoming).collect(Collectors.toList());
    }
}
