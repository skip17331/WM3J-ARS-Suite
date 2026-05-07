package com.hamradio.jlearn.content;

/**
 * ContentEntry — one row in the J-Learn manifest.
 *
 * <p>Immutable value object describing a single section: its id (the
 * stable {@code NN-NN} chapter-section pointer that survives across
 * releases), title, classpath-relative path to the markdown source,
 * chapter number, section number within the chapter, level
 * ({@code simple} / {@code advanced} / {@code mixed}), and lifecycle
 * status ({@code stub} / {@code draft} / {@code review} / {@code published}).
 *
 * <p>Used by {@link ContentManifest} to build the navigation tree and
 * by {@link ContentLoader} to find the markdown file when a section is
 * opened. The Java loader populates these from the section-index table
 * inside {@code content/manifest.md}.
 *
 * <h2>Status</h2>
 * Skeleton only — fields, accessors, and validation are not yet
 * implemented.
 */
public final class ContentEntry {

    // TODO: declare fields (id, title, path, chapter, section, level, status)
    // TODO: constructor with validation (id matches NN-NN, level/status enums)
    // TODO: accessors
    // TODO: equals/hashCode keyed on id
    // TODO: toString for debug logs

    private ContentEntry() {
        // Disallow instantiation until the real constructor lands.
        throw new UnsupportedOperationException("not yet implemented");
    }
}
