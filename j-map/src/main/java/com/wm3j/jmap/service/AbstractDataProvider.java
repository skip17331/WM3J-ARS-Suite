package com.wm3j.jmap.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class providing caching infrastructure for all data providers.
 * Subclasses implement {@link #doFetch()} with the actual data retrieval logic.
 */
public abstract class AbstractDataProvider<T> implements DataProvider<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicReference<T> cachedData = new AtomicReference<>();
    private volatile Instant lastUpdated = null;

    @Override
    public T fetch() throws DataProviderException {
        T data = doFetch();
        cachedData.set(data);
        lastUpdated = Instant.now();
        log.debug("Fetched {} data at {}", getClass().getSimpleName(), lastUpdated);
        return data;
    }

    @Override
    public T getCached() {
        return cachedData.get();
    }

    @Override
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Subclasses implement actual data retrieval here.
     * This method is called by {@link #fetch()} and the result is cached.
     *
     * @return Fresh data from the source
     * @throws DataProviderException on unrecoverable error
     */
    protected abstract T doFetch() throws DataProviderException;
}
