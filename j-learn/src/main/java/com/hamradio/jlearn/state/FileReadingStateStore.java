package com.hamradio.jlearn.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Properties;

/**
 * FileReadingStateStore — flat-file backing for {@link ReadingStateStore}.
 *
 * <p>Persists to {@code ~/.j-learn/state.properties} using Java's standard
 * {@link Properties} format. Writes go to {@code state.properties.tmp}
 * first and are then renamed over the live file with
 * {@link StandardCopyOption#ATOMIC_MOVE} so a power loss mid-write leaves
 * the previous state intact.
 */
public final class FileReadingStateStore implements ReadingStateStore {

    private static final Logger log = LoggerFactory.getLogger(FileReadingStateStore.class);

    public static final Path DEFAULT_PATH =
        Path.of(System.getProperty("user.home"), ".j-learn", "state.properties");

    private final Path path;

    /** Constructs a store using {@link #DEFAULT_PATH}. */
    public FileReadingStateStore() {
        this(DEFAULT_PATH);
    }

    /** Constructs a store using a caller-supplied path — used by tests. */
    public FileReadingStateStore(Path path) {
        this.path = path;
    }

    @Override
    public Optional<ReadingState> load() {
        if (!Files.isRegularFile(path)) return Optional.empty();
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            p.load(in);
        } catch (IOException e) {
            log.warn("Reading state load failed ({}): {}", path, e.getMessage());
            return Optional.empty();
        }
        String id = p.getProperty("lastSectionId");
        if (id == null || id.isBlank()) return Optional.empty();
        double frac;
        try { frac = Double.parseDouble(p.getProperty("scrollFraction", "0")); }
        catch (NumberFormatException e) { frac = 0.0; }
        if (frac < 0.0) frac = 0.0;
        if (frac > 1.0) frac = 1.0;
        Instant when;
        try { when = Instant.parse(p.getProperty("savedAt", "")); }
        catch (DateTimeParseException | NullPointerException e) { when = Instant.now(); }
        try {
            return Optional.of(new ReadingState(id, frac, when));
        } catch (IllegalArgumentException e) {
            log.warn("Reading state in {} is malformed: {}", path, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void save(ReadingState state) {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Properties p = new Properties();
            p.setProperty("lastSectionId",  state.lastSectionId());
            p.setProperty("scrollFraction", Double.toString(state.scrollFraction()));
            p.setProperty("savedAt",        state.savedAt().toString());
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "j-learn reading state");
            }
            try {
                Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE,
                                       StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException atomicFailed) {
                // Some filesystems (e.g. across mounts) reject ATOMIC_MOVE —
                // fall back to a plain replace.
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not save reading state to " + path, e);
        }
    }

    @Override
    public void clear() {
        try { Files.deleteIfExists(path); }
        catch (IOException e) {
            log.warn("Clearing reading state at {} failed: {}", path, e.getMessage());
        }
    }
}
