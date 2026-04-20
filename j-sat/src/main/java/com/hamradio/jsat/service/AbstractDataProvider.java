package com.hamradio.jsat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractDataProvider<T> implements DataProvider<T> {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final AtomicReference<T> cachedData = new AtomicReference<>();
    private volatile Instant lastUpdated;

    @Override
    public T fetch() throws DataProviderException {
        T data = doFetch();
        cachedData.set(data);
        lastUpdated = Instant.now();
        log.debug("Fetched {} at {}", getClass().getSimpleName(), lastUpdated);
        return data;
    }

    @Override
    public T getCached() { return cachedData.get(); }

    @Override
    public Instant getLastUpdated() { return lastUpdated; }

    protected abstract T doFetch() throws DataProviderException;
}
