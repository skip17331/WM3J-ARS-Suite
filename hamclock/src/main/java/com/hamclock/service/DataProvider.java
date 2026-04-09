package com.hamclock.service;

import java.time.Duration;
import java.time.Instant;

/**
 * Base interface for all external data providers.
 * Implementations must be thread-safe and handle caching internally.
 *
 * @param <T> The domain data type this provider returns
 */
public interface DataProvider<T> {

    /**
     * Fetch data from the source. Implementations should cache results
     * and only make network calls when stale or never fetched.
     *
     * @return The fetched (or cached) data
     * @throws DataProviderException on unrecoverable fetch failure
     */
    T fetch() throws DataProviderException;

    /**
     * @return The timestamp of the most recently successful fetch, or null if never fetched
     */
    Instant getLastUpdated();

    /**
     * @param maxAge Maximum acceptable data age
     * @return true if the cached data is older than maxAge (or was never fetched)
     */
    default boolean isStale(Duration maxAge) {
        Instant lu = getLastUpdated();
        if (lu == null) return true;
        return Duration.between(lu, Instant.now()).compareTo(maxAge) > 0;
    }

    /**
     * @return The most recently cached data without triggering a new fetch, or null
     */
    T getCached();
}
