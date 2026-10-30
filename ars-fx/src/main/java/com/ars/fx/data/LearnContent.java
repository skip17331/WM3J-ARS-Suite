package com.ars.fx.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The J-Learn chapters on disk ({@code ~/.j-learn/content/}): seeded from the copy bundled in the jar the first time they
 * are needed, and refreshable from a source of the operator's choosing (J-Hub ▸ Data ▸ J-Learn content). The folder is
 * deliberately separate from the program so the text can be updated, and edited, without a new release.
 *
 * <p>A source is a {@code .zip} URL (GitHub's archive of the repo, or a zip holding just the content), a local
 * {@code .zip}, or a local folder. A zip may hold the chapters at its root or inside a {@code j-learn-content/} folder.
 * Whatever it is, the new files are staged and checked first; the old folder is kept as {@code content.backup}.
 */
public final class LearnContent {
    private LearnContent() {}

    public static final String DEFAULT_SOURCE = "https://github.com/skip17331/WM3J-ARS-Suite/archive/refs/heads/main.zip";
    private static final String KEY_SOURCE = "learn.content.source", KEY_UPDATED = "learn.content.updated";
    private static final String BUNDLE = "/com/ars/fx/learn/content/";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public record Result(boolean ok, int files, String message) {}

    public static Path root()   { return Paths.get(System.getProperty("user.home"), ".j-learn"); }
    public static Path dir()    { return root().resolve("content"); }

    public static String source() { return HubConfig.get(KEY_SOURCE, DEFAULT_SOURCE).trim(); }
    public static void setSource(String s) { HubConfig.set(KEY_SOURCE, s == null ? "" : s.trim()); }
    /** "bundled with the app" / "updated 2026-10-30 14:25 from <source>". */
    public static String origin() { String u = HubConfig.get(KEY_UPDATED, ""); return u.isBlank() ? "bundled with the app" : u; }

    /** Markdown files on disk. */
    public static int fileCount() {
        if (!Files.isDirectory(dir())) return 0;
        try (Stream<Path> s = Files.walk(dir())) { return (int) s.filter(p -> p.toString().endsWith(".md")).count(); }
        catch (IOException e) { return 0; }
    }
    public static boolean seeded() { return Files.isReadable(dir().resolve("manifest.md")) && fileCount() > 10; }

    // ── seeding from the jar ────────────────────────────────────────────────
    /** Copy the bundled chapters to disk when they are not there yet. Cheap to call; does nothing once seeded. */
    public static synchronized Result seedIfMissing() {
        if (seeded()) return new Result(true, fileCount(), "already on disk");
        return restoreBundled();
    }

    /** Replace the folder with the copy bundled in the app (the old one is kept as content.backup). */
    public static synchronized Result restoreBundled() {
        Path staging = root().resolve("content.new");
        try {
            deleteTree(staging); Files.createDirectories(staging);
            int n = 0;
            List<String> index = bundledIndex();
            if (index.isEmpty()) return new Result(false, 0, "this build has no bundled J-Learn content");
            for (String rel : index) {
                try (InputStream in = LearnContent.class.getResourceAsStream(BUNDLE + rel)) {
                    if (in == null) continue;
                    Path to = staging.resolve(rel); Files.createDirectories(to.getParent());
                    Files.copy(in, to, StandardCopyOption.REPLACE_EXISTING); n++;
                }
            }
            if (n < 10) { deleteTree(staging); return new Result(false, 0, "the bundled content is incomplete"); }
            install(staging);
            HubConfig.remove(KEY_UPDATED);
            JLearnLibrary.reload();
            return new Result(true, n, "restored " + n + " files from the copy bundled with the app");
        } catch (Exception e) {
            deleteTree(staging);
            return new Result(false, 0, "could not restore: " + e.getMessage());
        }
    }

    /** manifest.md plus every section file it lists — the bundled tree is exactly that. */
    private static List<String> bundledIndex() throws IOException {
        List<String> out = new java.util.ArrayList<>(List.of("manifest.md"));
        try (InputStream in = LearnContent.class.getResourceAsStream(BUNDLE + "manifest.md")) {
            if (in == null) return List.of();
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                String t = line.trim();
                if (!t.startsWith("|")) continue;
                String[] c = t.split("\\|");
                if (c.length > 3 && c[1].trim().matches("\\d{2}-\\d{2}") && !c[3].trim().isEmpty()) out.add(c[3].trim());
            }
        }
        return out;
    }

    // ── update from a source ────────────────────────────────────────────────
    /** Fetch the chapters from {@code source} (see the class comment) and replace the folder. Blocking — run off the FX thread. */
    public static synchronized Result update(String source, java.util.function.Consumer<String> progress) {
        String src = source == null ? "" : source.trim();
        if (src.isEmpty()) return new Result(false, 0, "no source set");
        Path staging = root().resolve("content.new"), tmp = null;
        try {
            deleteTree(staging); Files.createDirectories(staging);
            int n;
            if (src.startsWith("http://") || src.startsWith("https://")) {
                if (progress != null) progress.accept("Downloading " + src + " …");
                tmp = Files.createTempFile("jlearn-", ".zip");
                HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.ALWAYS).build();
                HttpResponse<Path> r = http.send(HttpRequest.newBuilder(URI.create(src)).timeout(Duration.ofMinutes(5))
                        .header("User-Agent", "WM3J-ARS-Suite").GET().build(), HttpResponse.BodyHandlers.ofFile(tmp, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING));
                if (r.statusCode() != 200) return fail(staging, "the server answered HTTP " + r.statusCode());
                if (progress != null) progress.accept("Unpacking …");
                n = extractZip(tmp, staging);
            } else {
                Path p = Paths.get(src.startsWith("~") ? System.getProperty("user.home") + src.substring(1) : src);
                if (Files.isDirectory(p)) n = copyFolder(p, staging);
                else if (Files.isRegularFile(p)) n = extractZip(p, staging);
                else return fail(staging, "not found: " + p);
            }
            if (n < 10 || !Files.isReadable(staging.resolve("manifest.md")))
                return fail(staging, "that source has no J-Learn content (no manifest.md and chapter files found)");
            install(staging);
            HubConfig.set(KEY_SOURCE, src);
            HubConfig.set(KEY_UPDATED, "updated " + LocalDateTime.now().format(STAMP) + " from " + src);
            JLearnLibrary.reload();
            return new Result(true, n, "updated " + n + " files");
        } catch (Exception e) {
            return fail(staging, e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
    }

    private static Result fail(Path staging, String why) { deleteTree(staging); return new Result(false, 0, "nothing changed — " + why); }

    /** Unpack a zip into {@code dest}: the chapters may sit at the root or inside a {@code j-learn-content/} folder. */
    private static int extractZip(Path zip, Path dest) throws IOException {
        try (ZipFile z = new ZipFile(zip.toFile())) {
            String prefix = null;
            for (var en = z.entries(); en.hasMoreElements(); ) {                  // where does manifest.md live?
                String name = en.nextElement().getName();
                if (name.endsWith("j-learn-content/manifest.md")) { prefix = name.substring(0, name.length() - "manifest.md".length()); break; }
            }
            if (prefix == null) for (var en = z.entries(); en.hasMoreElements(); ) {
                String name = en.nextElement().getName();
                if (name.equals("manifest.md") || (name.endsWith("/manifest.md") && name.indexOf('/') == name.lastIndexOf('/'))) {
                    prefix = name.substring(0, name.length() - "manifest.md".length()); break;
                }
            }
            if (prefix == null) return 0;
            int n = 0;
            Path base = dest.toAbsolutePath().normalize();
            for (var en = z.entries(); en.hasMoreElements(); ) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory() || !e.getName().startsWith(prefix)) continue;
                String rel = e.getName().substring(prefix.length());
                if (rel.isEmpty()) continue;
                Path to = base.resolve(rel).normalize();
                if (!to.startsWith(base)) continue;                                // a hostile "../" entry is ignored
                Files.createDirectories(to.getParent());
                try (InputStream in = z.getInputStream(e)) { Files.copy(in, to, StandardCopyOption.REPLACE_EXISTING); }
                n++;
            }
            return n;
        }
    }

    private static int copyFolder(Path from, Path dest) throws IOException {
        Path root = Files.isRegularFile(from.resolve("manifest.md")) ? from
                  : Files.isRegularFile(from.resolve("j-learn-content/manifest.md")) ? from.resolve("j-learn-content") : from;
        int[] n = {0};
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (!Files.isRegularFile(p)) continue;
                Path to = dest.resolve(root.relativize(p).toString()); Files.createDirectories(to.getParent());
                Files.copy(p, to, StandardCopyOption.REPLACE_EXISTING); n[0]++;
            }
        }
        return n[0];
    }

    /** Swap the staged tree in; the previous folder becomes content.backup (replacing any older backup). */
    private static void install(Path staging) throws IOException {
        Path live = dir(), backup = root().resolve("content.backup");
        if (Files.exists(live)) { deleteTree(backup); Files.move(live, backup); }
        try { Files.move(staging, live); }
        catch (IOException e) { if (Files.exists(backup) && !Files.exists(live)) Files.move(backup, live); throw e; }
    }

    private static void deleteTree(Path p) {
        if (!Files.exists(p)) return;
        try (Stream<Path> s = Files.walk(p)) { s.sorted(Comparator.reverseOrder()).forEach(q -> { try { Files.delete(q); } catch (IOException ignored) {} }); }
        catch (IOException ignored) {}
    }
}
