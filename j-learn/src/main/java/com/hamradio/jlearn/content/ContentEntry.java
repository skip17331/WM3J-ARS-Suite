package com.hamradio.jlearn.content;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * ContentEntry — one row in the J-Learn manifest.
 *
 * <p>Immutable value object describing a single section: id ({@code NN-NN}),
 * title, classpath-relative path, chapter and section numbers, level, and
 * lifecycle status. Equality is keyed on id alone — the id is the only
 * thing that survives across releases.
 *
 * <p>Constructed by {@link ContentManifest}; consumers should not call the
 * constructor directly.
 */
public final class ContentEntry {

    /** Lifecycle status from the markdown file's YAML front-matter. */
    public enum Status {
        STUB, DRAFT, REVIEW, PUBLISHED;

        /** Parse from front-matter value. Returns {@link #DRAFT} when blank or unknown. */
        public static Status parse(String raw) {
            if (raw == null) return DRAFT;
            switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "stub":      return STUB;
                case "draft":     return DRAFT;
                case "review":    return REVIEW;
                case "published": return PUBLISHED;
                default:          return DRAFT;
            }
        }
    }

    /** Reading-difficulty hint from the manifest level column. */
    public enum Level {
        SIMPLE, ADVANCED, MIXED;

        public static Level parse(String raw) {
            if (raw == null) return MIXED;
            switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "simple":   return SIMPLE;
                case "advanced": return ADVANCED;
                case "mixed":    return MIXED;
                default:         return MIXED;
            }
        }
    }

    private static final Pattern ID_PATTERN = Pattern.compile("\\d{2}-\\d{2}");

    private final String id;
    private final String title;
    private final String path;
    private final String chapter;
    private final String section;
    private final Level  level;
    private final Status status;

    public ContentEntry(String id, String title, String path,
                        Level level, Status status) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("id must match NN-NN: " + id);
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must be non-blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("path must be non-blank");
        }
        this.id      = id;
        this.title   = title.trim();
        this.path    = path.trim();
        this.chapter = id.substring(0, 2);
        this.section = id.substring(3, 5);
        this.level   = level  != null ? level  : Level.MIXED;
        this.status  = status != null ? status : Status.DRAFT;
    }

    public String id()      { return id;      }
    public String title()   { return title;   }
    public String path()    { return path;    }
    public String chapter() { return chapter; }
    public String section() { return section; }
    public Level  level()   { return level;   }
    public Status status()  { return status;  }

    @Override public boolean equals(Object o) {
        if (!(o instanceof ContentEntry)) return false;
        return id.equals(((ContentEntry) o).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() {
        return "ContentEntry{" + id + " " + title + " [" + level + "/" + status + "]}";
    }
}
