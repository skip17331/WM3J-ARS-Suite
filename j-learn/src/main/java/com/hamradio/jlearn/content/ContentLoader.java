package com.hamradio.jlearn.content;

/**
 * ContentLoader — reads markdown chapter files from the classpath.
 *
 * <p>Every file under {@code src/main/resources/content/} ships inside
 * the j-learn jar, so the loader uses
 * {@code Class.getResourceAsStream("/content/...")} rather than touching
 * the filesystem. Content is small (a few hundred markdown files,
 * total &lt; 5 MB even when fully written), so the loader caches whatever
 * has been requested in this process.
 *
 * <h2>What gets returned</h2>
 * The raw markdown text including the YAML front-matter block. Hosts
 * are responsible for stripping the front matter (front-matter parsing
 * lives in {@link ContentManifest}; the per-file body is delivered as-is
 * so callers can render it with their preferred markdown engine —
 * Flexmark, commonmark-java, or a JavaFX {@code WebView}).
 *
 * <h2>Missing files</h2>
 * Returns {@code null} when no resource matches. The host UI should
 * treat that as "section is in the manifest but the file went missing
 * during build" — log it, skip the section, do not crash.
 *
 * <h2>Status</h2>
 * Skeleton only.
 */
public final class ContentLoader {

    /** Reads the markdown body for the given entry. {@code null} when missing. */
    // TODO: implement
    public static String read(ContentEntry entry) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    /** Reads the markdown body for the given classpath-relative path. */
    // TODO: implement
    public static String read(String classpathPath) {
        throw new UnsupportedOperationException("not yet implemented");
    }

    private ContentLoader() {
        // Static helper — no instances.
    }
}
