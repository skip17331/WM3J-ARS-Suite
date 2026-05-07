package com.hamradio.jlearn.content;

import java.util.List;

/**
 * ContentManifest — parsed view of {@code content/manifest.md}.
 *
 * <p>The manifest is a markdown file with a series of pipe-delimited
 * tables grouped under "Part I" through "Part V" headers. Every row
 * whose first cell matches the {@code NN-NN} pattern becomes one
 * {@link ContentEntry}; everything else is human-readable
 * documentation and is ignored by the loader.
 *
 * <h2>Loading</h2>
 * {@link #load()} reads the manifest from the classpath at
 * {@code /content/manifest.md}, parses it into an in-memory tree, and
 * caches the result. Subsequent calls return the same instance.
 *
 * <h2>Lookups the host UI typically needs</h2>
 * <ul>
 *   <li>{@link #all()} — flat list of every section, in manifest order.</li>
 *   <li>{@link #byId(String)} — random-access lookup by {@code NN-NN} id.</li>
 *   <li>{@link #byChapter(String)} — sections grouped under one chapter.</li>
 *   <li>{@link #chapters()} — distinct chapter ids in canonical order.</li>
 * </ul>
 *
 * <h2>Status</h2>
 * Skeleton only.
 */
public final class ContentManifest {

    /** Loads (or returns the cached) manifest from the classpath. Thread-safe. */
    // TODO: implement
    public static ContentManifest load() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /** Every entry in manifest order (Part I → Part V). */
    // TODO: implement
    public List<ContentEntry> all() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /** O(1) lookup by id. Returns {@code null} when the id is unknown. */
    // TODO: implement
    public ContentEntry byId(String id) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /** Entries belonging to the given chapter ({@code "01"} … {@code "26"}). */
    // TODO: implement
    public List<ContentEntry> byChapter(String chapter) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /** Ordered list of chapter ids that have at least one entry. */
    // TODO: implement
    public List<String> chapters() {
        throw new UnsupportedOperationException("not yet implemented");
    }

    private ContentManifest() {
        // Use load() — never instantiate directly.
    }
}
