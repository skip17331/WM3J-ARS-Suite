package com.hamradio.jlearn.state;

import java.util.Optional;

/**
 * ReadingStateStore — persistence interface for {@link ReadingState}.
 *
 * <p>Modeled as an interface so the host module can plug in an
 * alternative store (in-memory for tests, a shared web-state store for
 * multi-user setups, …) without J-Learn caring. The default
 * implementation is {@link FileReadingStateStore}.
 *
 * <h2>Status</h2>
 * Skeleton only.
 */
public interface ReadingStateStore {

    /** Returns the most recent state, or empty when nothing has been saved. */
    Optional<ReadingState> load();

    /** Persists the snapshot atomically. Failures are surfaced as an exception. */
    void save(ReadingState state);

    /** Clears any stored state — used by the "forget my place" menu item. */
    void clear();
}
