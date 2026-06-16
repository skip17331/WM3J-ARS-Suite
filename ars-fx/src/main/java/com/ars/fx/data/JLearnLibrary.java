package com.ars.fx.data;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the J-Learn reference library's chapter/section index — the same
 * pipe-delimited manifest the standalone web app parses. Source priority mirrors
 * J-Learn's own ContentResolver: the seeded, user-editable copy on disk
 * ({@code ~/.j-learn/content/manifest.md}) wins, falling back to the bundled
 * snapshot so the surface still populates on a machine where J-Learn never ran.
 *
 * Section bodies are loaded best-effort from the on-disk content tree only.
 */
public final class JLearnLibrary {
    private JLearnLibrary() {}

    public record Section(String id, String chapter, String section, String title, String path, String level) {
        public boolean advanced() { return "advanced".equalsIgnoreCase(level); }
    }
    public record Chapter(String num, String title, List<Section> sections) {}

    private static List<Chapter> cache;

    public static synchronized List<Chapter> chapters() {
        if (cache == null) cache = load();
        return cache;
    }

    public static int sectionCount() {
        int n = 0; for (Chapter c : chapters()) n += c.sections().size(); return n;
    }

    private static List<Chapter> load() {
        List<String> lines = readManifest();
        // group sections by chapter, preserving manifest order
        Map<String, List<Section>> byChapter = new LinkedHashMap<>();
        Map<String, String> chapterTitle = new LinkedHashMap<>();
        for (String line : lines) {
            String t = line.trim();
            if (!t.startsWith("|")) continue;
            String[] cells = t.split("\\|");
            // cells[0] is empty (leading pipe); id is cells[1]
            if (cells.length < 3) continue;
            String id = cells[1].trim();
            if (!id.matches("\\d{2}-\\d{2}")) continue;          // skip header/separator rows
            String title = cells[2].trim();
            String path = cells.length > 3 ? cells[3].trim() : "";
            String level = cells.length > 4 ? cells[4].trim() : "mixed";
            String ch = id.substring(0, 2), sec = id.substring(3);
            byChapter.computeIfAbsent(ch, k -> new ArrayList<>()).add(new Section(id, ch, sec, title, path, level));
            if (sec.equals("00")) chapterTitle.put(ch,
                    title.replaceAll("\\s*[—-]\\s*Overview$", "").replaceAll("^Overview\\s*[—-]\\s*", ""));
        }
        List<Chapter> out = new ArrayList<>();
        for (Map.Entry<String, List<Section>> e : byChapter.entrySet()) {
            String ch = e.getKey();
            String title = chapterTitle.getOrDefault(ch, "Chapter " + ch);
            out.add(new Chapter(ch, title, e.getValue()));
        }
        return out;
    }

    private static List<String> readManifest() {
        // 1) seeded disk copy
        Path disk = Paths.get(System.getProperty("user.home"), ".j-learn", "content", "manifest.md");
        try {
            if (Files.isReadable(disk)) return Files.readAllLines(disk, StandardCharsets.UTF_8);
        } catch (Exception ignored) { /* fall through */ }
        // 2) bundled fallback
        try (InputStream in = JLearnLibrary.class.getResourceAsStream("/com/ars/fx/learn/manifest.md")) {
            if (in != null) {
                List<String> out = new ArrayList<>();
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String l; (l = r.readLine()) != null; ) out.add(l);
                return out;
            }
        } catch (Exception ignored) { /* fall through */ }
        return List.of();
    }

    /** Section markdown body (front-matter stripped), or null if the content tree isn't seeded. */
    public static String body(Section s) {
        if (s == null || s.path() == null || s.path().isBlank()) return null;
        Path p = Paths.get(System.getProperty("user.home"), ".j-learn", "content", s.path());
        try {
            if (!Files.isReadable(p)) return null;
            String raw = Files.readString(p, StandardCharsets.UTF_8);
            return stripFrontMatter(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private static String stripFrontMatter(String md) {
        String t = md.stripLeading();
        if (t.startsWith("---")) {
            int end = t.indexOf("\n---", 3);
            if (end >= 0) {
                int nl = t.indexOf('\n', end + 1);
                return nl >= 0 ? t.substring(nl + 1).stripLeading() : "";
            }
        }
        return md;
    }
}
