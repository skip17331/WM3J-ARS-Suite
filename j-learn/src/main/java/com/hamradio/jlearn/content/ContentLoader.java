package com.hamradio.jlearn.content;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ContentLoader — reads markdown chapter files from the classpath at
 * {@code /content/}. Caches loaded bodies in-process; content is small
 * (a few hundred files, &lt; 5 MB total) so keeping it resident is cheap.
 *
 * <p>Returns {@code null} when the resource is missing — hosts should
 * treat that as "section is in the manifest but the file went missing
 * during build", log it, and skip the section.
 */
public final class ContentLoader {

    private static final String CLASSPATH_ROOT = "/content/";
    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private ContentLoader() {}  // static helper

    /** Reads the markdown body for the given entry. {@code null} when missing. */
    public static String read(ContentEntry entry) {
        if (entry == null) return null;
        return read(entry.path());
    }

    /**
     * Reads the markdown body for a classpath-relative path under
     * {@code /content/}. The leading slash and {@code content/} prefix are
     * tolerated for convenience.
     */
    public static String read(String relativePath) {
        if (relativePath == null) return null;
        String rel = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        if (rel.startsWith("content/")) rel = rel.substring("content/".length());
        String key = rel;
        return CACHE.computeIfAbsent(key, ContentLoader::readFromClasspath);
    }

    /** Parse the YAML front-matter block at the head of a markdown body
     *  into a key→value map. Returns an empty map if there's no
     *  front-matter, or {@code null} if the body itself is null. */
    public static Map<String, String> frontMatter(String body) {
        if (body == null) return null;
        if (!body.startsWith("---")) return Map.of();
        int end = body.indexOf("\n---", 3);
        if (end < 0) return Map.of();
        String block = body.substring(3, end);
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : block.split("\n")) {
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String k = line.substring(0, colon).trim();
            String v = line.substring(colon + 1).trim();
            // Strip trailing comment, if any
            int hash = v.indexOf('#');
            if (hash >= 0) v = v.substring(0, hash).trim();
            if (!k.isEmpty()) out.put(k, v);
        }
        return out;
    }

    private static String readFromClasspath(String rel) {
        String resource = CLASSPATH_ROOT + rel;
        try (InputStream in = ContentLoader.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    /** Test hook — wipes the in-process cache. */
    static void clearCache() { CACHE.clear(); }
}
