package com.hamradio.jlearn.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ContentManifest — parsed view of {@code /content/manifest.md}.
 *
 * <p>The manifest is a markdown file with pipe-delimited tables grouped
 * under "Part I" through "Part V" headers. The parser reads every row
 * whose first cell matches {@code NN-NN} and treats everything else as
 * human prose. Columns: {@code id | title | path | level}.
 *
 * <p>Per-entry {@code status} is resolved from each file's YAML
 * front-matter at load time so callers can filter "show me only
 * published sections" without re-parsing files.
 */
public final class ContentManifest {

    private static final Pattern ROW_ID = Pattern.compile("^\\s*\\|?\\s*(\\d{2}-\\d{2})\\s*\\|");

    private static volatile ContentManifest cached;
    private static final Object LOCK = new Object();

    private final List<ContentEntry>          entries;            // manifest order
    private final Map<String, ContentEntry>   byId;
    private final Map<String, List<ContentEntry>> byChapter;     // ordered insertion
    private final List<String>                chapters;          // ordered

    private ContentManifest(List<ContentEntry> entries) {
        this.entries = List.copyOf(entries);
        Map<String, ContentEntry> id = new LinkedHashMap<>();
        Map<String, List<ContentEntry>> ch = new LinkedHashMap<>();
        for (ContentEntry e : entries) {
            id.put(e.id(), e);
            ch.computeIfAbsent(e.chapter(), k -> new ArrayList<>()).add(e);
        }
        this.byId = Collections.unmodifiableMap(id);
        Map<String, List<ContentEntry>> chRO = new LinkedHashMap<>();
        for (var en : ch.entrySet()) chRO.put(en.getKey(), List.copyOf(en.getValue()));
        this.byChapter = Collections.unmodifiableMap(chRO);
        this.chapters  = List.copyOf(ch.keySet());
    }

    /** Loads (or returns the cached) manifest from the classpath. Thread-safe. */
    public static ContentManifest load() {
        ContentManifest c = cached;
        if (c != null) return c;
        synchronized (LOCK) {
            if (cached != null) return cached;
            String body = ContentLoader.read("manifest.md");
            if (body == null) {
                cached = new ContentManifest(List.of());
                return cached;
            }
            cached = new ContentManifest(parseRows(body));
            return cached;
        }
    }

    /** Test hook — drops the cached singleton so the next {@link #load()} re-parses. */
    static void resetCache() {
        synchronized (LOCK) { cached = null; }
        ContentLoader.clearCache();
    }

    public List<ContentEntry>             all()                      { return entries;   }
    public ContentEntry                   byId(String id)            { return byId.get(id); }
    public List<ContentEntry>             byChapter(String chapter)  {
        List<ContentEntry> v = byChapter.get(chapter);
        return v == null ? List.of() : v;
    }
    public List<String>                   chapters()                 { return chapters;  }
    public int                            size()                     { return entries.size(); }

    // ---- parsing -------------------------------------------------------

    private static List<ContentEntry> parseRows(String body) {
        List<ContentEntry> out = new ArrayList<>();
        for (String line : body.split("\n")) {
            Matcher m = ROW_ID.matcher(line);
            if (!m.find()) continue;
            String[] cells = splitPipes(line);
            if (cells.length < 4) continue;
            String id    = cells[0];
            String title = cells[1];
            String path  = cells[2];
            ContentEntry.Level level = ContentEntry.Level.parse(cells[3]);

            ContentEntry.Status status = readStatusFromFrontMatter(path);
            try {
                out.add(new ContentEntry(id, title, path, level, status));
            } catch (IllegalArgumentException ignore) {
                // Malformed row — skip rather than fail the whole load.
            }
        }
        return out;
    }

    /** Split a markdown table line on `|`, trim each cell, drop empty
     *  leading/trailing cells from the bordering pipes. */
    private static String[] splitPipes(String line) {
        String[] raw = line.split("\\|", -1);
        List<String> kept = new ArrayList<>(raw.length);
        for (String c : raw) {
            String t = c.trim();
            kept.add(t);
        }
        // Strip the single empty cell on each end that comes from `|…|…|`.
        int lo = 0, hi = kept.size();
        if (lo < hi && kept.get(lo).isEmpty())       lo++;
        if (hi > lo && kept.get(hi - 1).isEmpty())   hi--;
        return kept.subList(lo, hi).toArray(new String[0]);
    }

    private static ContentEntry.Status readStatusFromFrontMatter(String path) {
        String body = ContentLoader.read(path);
        if (body == null) return ContentEntry.Status.DRAFT;
        Map<String, String> fm = ContentLoader.frontMatter(body);
        return ContentEntry.Status.parse(fm.get("status"));
    }
}
