package com.hamradio.jsat.service;

import java.time.Duration;
import java.time.Instant;

public interface DataProvider<T> {
    T fetch() throws DataProviderException;
    Instant getLastUpdated();
    T getCached();

    default boolean isStale(Duration maxAge) {
        Instant lu = getLastUpdated();
        if (lu == null) return true;
        return Duration.between(lu, Instant.now()).compareTo(maxAge) > 0;
    }
}
