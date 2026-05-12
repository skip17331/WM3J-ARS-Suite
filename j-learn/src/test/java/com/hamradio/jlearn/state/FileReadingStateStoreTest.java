package com.hamradio.jlearn.state;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FileReadingStateStoreTest {

    @Test
    void roundTripsTheSnapshot(@TempDir Path tmp) {
        Path file = tmp.resolve("state.properties");
        FileReadingStateStore store = new FileReadingStateStore(file);

        Instant when = Instant.parse("2026-05-12T18:30:00Z");
        ReadingState snap = new ReadingState("01-04", 0.42, when);
        store.save(snap);

        Optional<ReadingState> loaded = store.load();
        assertTrue(loaded.isPresent());
        assertEquals(snap, loaded.get());
    }

    @Test
    void loadFromMissingFileReturnsEmpty(@TempDir Path tmp) {
        FileReadingStateStore store = new FileReadingStateStore(tmp.resolve("nope.properties"));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void clearRemovesTheFile(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("state.properties");
        FileReadingStateStore store = new FileReadingStateStore(file);
        store.save(ReadingState.newSnapshot("01-01", 0.5));
        assertTrue(Files.exists(file));
        store.clear();
        assertFalse(Files.exists(file));
        assertTrue(store.load().isEmpty());
    }

    @Test
    void saveIsAtomicLeavingNoStrayTmpFile(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("state.properties");
        FileReadingStateStore store = new FileReadingStateStore(file);
        store.save(ReadingState.newSnapshot("02-01", 0.1));
        assertTrue(Files.exists(file));
        assertFalse(Files.exists(file.resolveSibling("state.properties.tmp")),
            "tmp file must be moved away atomically");
    }

    @Test
    void malformedFractionIsClampedNotRejected(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("state.properties");
        Files.writeString(file, ""
            + "lastSectionId=03-02\n"
            + "scrollFraction=2.5\n"
            + "savedAt=2026-05-12T18:30:00Z\n");
        FileReadingStateStore store = new FileReadingStateStore(file);
        Optional<ReadingState> loaded = store.load();
        assertTrue(loaded.isPresent());
        assertEquals(1.0, loaded.get().scrollFraction(),
            "fractions > 1.0 must be clamped, not crash");
    }

    @Test
    void readingStateRejectsOutOfRangeFraction() {
        assertThrows(IllegalArgumentException.class,
            () -> new ReadingState("01-01", -0.1, Instant.now()));
        assertThrows(IllegalArgumentException.class,
            () -> new ReadingState("01-01",  1.1, Instant.now()));
    }
}
