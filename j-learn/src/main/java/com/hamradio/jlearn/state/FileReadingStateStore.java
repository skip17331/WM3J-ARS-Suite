package com.hamradio.jlearn.state;

import java.nio.file.Path;
import java.util.Optional;

/**
 * FileReadingStateStore — flat-file backing for {@link ReadingStateStore}.
 *
 * <p>Persists to {@code ~/.j-learn/state.properties} using Java's standard
 * {@link java.util.Properties} format — one line per field
 * ({@code lastSectionId=…}, {@code scrollFraction=…}, {@code savedAt=…}).
 * Properties was chosen over JSON because it has zero runtime
 * dependencies and the data is small and human-readable.
 *
 * <p>The state directory is {@code ~/.j-learn/} to match the convention
 * every other module in the suite uses ({@code ~/.j-hub/}, {@code ~/.j-log/},
 * {@code ~/.j-map/}, etc.) — a per-module dotfile directory under the
 * user's home.
 *
 * <h2>Atomicity</h2>
 * Writes go to {@code state.properties.tmp} first and are then renamed
 * over the live file with {@code Files.move(... ATOMIC_MOVE)}. A power
 * loss mid-write leaves the previous state intact.
 *
 * <h2>Status</h2>
 * Skeleton only.
 */
public final class FileReadingStateStore implements ReadingStateStore {

    /** Default location: {@code ~/.j-learn/state.properties}. */
    public static final Path DEFAULT_PATH =
        Path.of(System.getProperty("user.home"), ".j-learn", "state.properties");

    /** Constructs a store using {@link #DEFAULT_PATH}. */
    public FileReadingStateStore() {
        // TODO: implement (resolve path, ensure parent dir, etc.)
    }

    /** Constructs a store using a caller-supplied path — used by tests. */
    public FileReadingStateStore(Path path) {
        // TODO: implement
    }

    @Override
    // TODO: implement
    public Optional<ReadingState> load() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    // TODO: implement
    public void save(ReadingState state) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    @Override
    // TODO: implement
    public void clear() {
        throw new UnsupportedOperationException("not yet implemented");
    }
}
