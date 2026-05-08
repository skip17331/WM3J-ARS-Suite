package com.hamradio.jlearn.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.stream.Stream;

/**
 * Resolves J-Learn content (markdown files) from one of two sources:
 *
 *   1. User content directory at {@code ~/.j-learn/content/} — preferred
 *      so users can edit / add chapters without rebuilding the jar.
 *   2. The bundled classpath copy under {@code /content/} inside the jar.
 *
 * On first run, if the user content directory is missing or empty, we
 * copy the bundled markdown into it. After that point the jar copy is
 * only used as a fallback for individual missing files.
 */
public final class ContentResolver {

    private static final Logger log = LoggerFactory.getLogger(ContentResolver.class);

    private final Path contentDir;

    public ContentResolver() {
        this.contentDir = Path.of(System.getProperty("user.home"), ".j-learn", "content");
    }

    public Path contentDir() { return contentDir; }

    /** Return the manifest text, or {@code null} if it can't be found. */
    public String readManifest() {
        return read("manifest.md");
    }

    /**
     * Return the body of one section. {@code relPath} is the path written
     * in the manifest (e.g. {@code "01-propagation/01-02-skywave.md"}).
     */
    public String readSection(String relPath) {
        return read(relPath);
    }

    /**
     * Copy the bundled content into {@link #contentDir} on first run so
     * users can edit it directly. No-ops if the directory already has
     * any markdown in it.
     */
    public void seedIfEmpty() {
        try {
            if (hasAnyMarkdown(contentDir)) return;
            Files.createDirectories(contentDir);
            int copied = copyBundledContent();
            log.info("Seeded {} J-Learn files into {}", copied, contentDir);
        } catch (Exception e) {
            log.warn("Could not seed J-Learn content directory ({}): {}",
                     contentDir, e.getMessage());
        }
    }

    // -- internals --------------------------------------------------------

    private String read(String rel) {
        Path onDisk = contentDir.resolve(rel);
        if (Files.isRegularFile(onDisk)) {
            try { return Files.readString(onDisk, StandardCharsets.UTF_8); }
            catch (IOException e) {
                log.warn("Failed reading {}: {}", onDisk, e.getMessage());
            }
        }
        // Fallback to whatever shipped in the jar.
        try (InputStream in = getClass().getResourceAsStream("/content/" + rel)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean hasAnyMarkdown(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) return false;
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.anyMatch(p -> p.toString().endsWith(".md"));
        }
    }

    private int copyBundledContent() throws Exception {
        URL root = getClass().getResource("/content");
        if (root == null) {
            log.warn("No bundled /content resource found on classpath — skipping seed.");
            return 0;
        }
        URI uri = root.toURI();
        int copied;
        if ("jar".equals(uri.getScheme())) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path inside = fs.getPath("/content");
                copied = copyTree(inside, contentDir);
            }
        } else {
            // Running from an exploded classpath (mvn exec, IDE, tests).
            copied = copyTree(Path.of(uri), contentDir);
        }
        return copied;
    }

    private static int copyTree(Path src, Path dst) throws IOException {
        int[] count = {0};
        try (Stream<Path> walk = Files.walk(src)) {
            walk.forEach(p -> {
                try {
                    Path rel = src.relativize(p);
                    Path target = dst.resolve(rel.toString());
                    if (Files.isDirectory(p)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(p, target);
                        count[0]++;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return count[0];
    }
}
