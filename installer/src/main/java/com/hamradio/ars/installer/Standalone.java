package com.hamradio.ars.installer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The install that needs no build: download the newest ARS Suite release for this computer, check it against the SHA-256 GitHub
 * publishes for it, and lay it out the way a source checkout looks, so the rest of {@link Installer} (the shortcuts, the menu folder)
 * and the app's own in-place updater work on it unchanged:
 *
 * <pre>
 *   &lt;install folder&gt;/ars-fx/ars-fx.sh | .bat       the launcher (from this jar)
 *   &lt;install folder&gt;/ars-fx/target/ars-fx-&lt;dist&gt;.jar   the release jar, named as a local build would name it
 *   &lt;install folder&gt;/ars-fx/src/main/resources/com/ars/fx/icons/*.png   the three icons the shortcuts use (from this jar)
 * </pre>
 *
 * It is used when {@code --download} is given, or when no source checkout is found. Needs only Java; no Maven, no Git.
 * Never touches your settings, logs and databases ({@code ~/.j-hub}, {@code ~/.j-log}, {@code ~/.j-vault} ...).
 */
final class Standalone {
    private Standalone() {}

    static final String TAG_PREFIX = "v", ASSET_PREFIX = "ars-fx-";
    static final String RELEASES_URL = "https://api.github.com/repos/skip17331/WM3J-ARS-Suite/releases/latest";
    static final String VERSION_FILE = "INSTALLED-VERSION.txt";

    static final class Asset {
        private final String name, url, digest;
        Asset(String name, String url, String digest) { this.name = name; this.url = url; this.digest = digest; }
        String name() { return name; }
        String url() { return url; }
        String digest() { return digest; }
    }

    static final class Release {
        private final String tag;
        private final List<Asset> assets;
        Release(String tag, List<Asset> assets) { this.tag = tag; this.assets = assets; }
        String tag() { return tag; }
        List<Asset> assets() { return assets; }
        /** The version without the {@code v} in front. */
        String version() { return tag.startsWith(TAG_PREFIX) ? tag.substring(TAG_PREFIX.length()) : tag; }
    }

    // ---- when -----------------------------------------------------------------------------------------------------------------

    /** Standalone when asked for with {@code --download}, or when there is no checkout to install from (no {@code --root}, no
     *  {@code ARS_SUITE_HOME}, and no {@code ars-fx/pom.xml} in the current folder or its parent). */
    static boolean wanted(String[] args, String env, Path cwd) {
        for (String a : args) if ("--download".equals(a)) return true;
        for (String a : args) if ("--root".equals(a) || "-r".equals(a)) return false;
        if (env != null && !env.isBlank()) return false;
        if (Files.exists(cwd.resolve("ars-fx/pom.xml"))) return false;
        return cwd.getParent() == null || !Files.exists(cwd.getParent().resolve("ars-fx/pom.xml"));
    }

    /** The value after {@code flag} in {@code args}, or null. */
    static String option(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) if (flag.equals(args[i])) return args[i + 1];
        return null;
    }

    // ---- where ----------------------------------------------------------------------------------------------------------------

    /** The folder the app is installed in. Not the operator's data folders ({@code ~/.j-hub}, {@code ~/.j-log} ...): nothing is written there. */
    static Path installDir(String osName, Path home, String localAppData) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            Path base = localAppData != null && !localAppData.isBlank() ? Paths.get(localAppData) : home.resolve("AppData/Local");
            return base.resolve("ARS-Suite").resolve("app");
        }
        if (os.contains("mac") || os.contains("darwin")) return home.resolve("Library/Application Support/ARS-Suite");
        return home.resolve(".local/share/ars-suite-app");
    }

    // ---- which release ----------------------------------------------------------------------------------------------------------

    /** The release-asset platform key for an OS and CPU, e.g. {@code linux-x86_64}; the same keys the app's own updater uses. */
    static String platformKey(String osName, String osArch) {
        String os = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        String ar = osArch == null ? "" : osArch.toLowerCase(Locale.ROOT);
        boolean arm = ar.contains("aarch64") || ar.contains("arm64");
        if (os.contains("win")) return "windows-x86_64";
        if (os.contains("mac") || os.contains("darwin")) return arm ? "macos-aarch64" : "macos-x86_64";
        return arm ? "linux-aarch64" : "linux-x86_64";
    }

    /** The name the jar is saved under, as a local build would name it. The launchers find it by these names and, on Linux, the app's
     *  updater decides which release asset replaces it from the name alone ({@code ars-fx-linux.jar} or {@code ars-fx-pi.jar}). */
    static String jarName(String platformKey) {
        if ("linux-x86_64".equals(platformKey))   return "ars-fx-linux.jar";
        if ("linux-aarch64".equals(platformKey))  return "ars-fx-pi.jar";
        if ("windows-x86_64".equals(platformKey)) return "ars-fx-windows.jar";
        if ("macos-aarch64".equals(platformKey))  return "ars-fx-mac-aarch64.jar";
        return "ars-fx-mac.jar";
    }

    /** The release to install: GitHub's {@code /releases/latest} answer (one release; it never is a draft or pre-release), or, from a
     *  mirror, a newest-first list, where the first release tagged {@code v…} that is neither a draft nor a pre-release is taken.
     *  Null when there is none. */
    static Release pickLatest(String json) {
        JsonElement root;
        try { root = JsonParser.parseString(json); }
        catch (RuntimeException e) { throw new IllegalArgumentException("GitHub's answer was not a release", e); }
        if (root.isJsonObject()) {
            JsonObject o = root.getAsJsonObject();
            if (!o.has("tag_name")) throw new IllegalArgumentException("GitHub's answer was not a release: " + str(o, "message"));
            return usable(o) ? release(o) : null;
        }
        if (!root.isJsonArray()) throw new IllegalArgumentException("GitHub's answer was not a release");
        for (JsonElement e : root.getAsJsonArray()) if (e.isJsonObject() && usable(e.getAsJsonObject())) return release(e.getAsJsonObject());
        return null;
    }

    private static boolean usable(JsonObject o) { return !flag(o, "draft") && !flag(o, "prerelease") && str(o, "tag_name").startsWith(TAG_PREFIX); }

    private static Release release(JsonObject o) {
        List<Asset> assets = new ArrayList<>();
        JsonElement arr = o.get("assets");
        if (arr != null && arr.isJsonArray())
            for (JsonElement a : arr.getAsJsonArray())
                if (a.isJsonObject()) assets.add(new Asset(str(a.getAsJsonObject(), "name"), str(a.getAsJsonObject(), "browser_download_url"), str(a.getAsJsonObject(), "digest")));
        return new Release(str(o, "tag_name"), List.copyOf(assets));
    }

    /** The release jar for {@code platformKey} ({@code ars-fx-<version>-<platform>.jar}), or null. */
    static Asset assetFor(Release r, String platformKey) {
        String suffix = "-" + platformKey + ".jar";
        for (Asset a : r.assets()) if (a.name().startsWith(ASSET_PREFIX) && a.name().endsWith(suffix)) return a;
        return null;
    }

    /** The hex SHA-256 in GitHub's {@code "sha256:<hex>"} digest, lower case; null when there is none or it is another algorithm. */
    static String expectedSha256(Asset a) {
        String d = a == null ? null : a.digest();
        if (d == null || !d.regionMatches(true, 0, "sha256:", 0, 7)) return null;
        String hex = d.substring(7).trim().toLowerCase(Locale.ROOT);
        return hex.matches("[0-9a-f]{64}") ? hex : null;
    }

    // ---- fetching -------------------------------------------------------------------------------------------------------------

    static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
            StringBuilder hex = new StringBuilder();
            for (byte b : md.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    /** Download {@code url} to {@code dest} (through a {@code .part} file), and keep it only if its SHA-256 is {@code expectedHex}. */
    static void download(HttpClient http, String url, Path dest, String expectedHex) throws IOException, InterruptedException {
        Files.createDirectories(dest.getParent());
        Path part = dest.resolveSibling(dest.getFileName() + ".part");
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "ARS-Suite-Installer").GET().build();
            HttpResponse<Path> resp = http.send(req, HttpResponse.BodyHandlers.ofFile(part));
            if (resp.statusCode() != 200) throw new IOException("the download failed: HTTP " + resp.statusCode() + " from " + url);
            String have = sha256(part);
            if (!have.equalsIgnoreCase(expectedHex))
                throw new IOException("the downloaded file does not match the SHA-256 GitHub published for it (expected " + expectedHex + ", got " + have + "): not installed");
            Files.move(part, dest, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    static HttpClient client() {
        return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
    }

    static String fetchReleases(HttpClient http) throws IOException, InterruptedException { return fetchReleases(http, RELEASES_URL); }

    /** The release list from {@code url}: GitHub's, unless {@code --releases-url} points at a mirror (or a test server). */
    static String fetchReleases(HttpClient http, String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30))
                .header("Accept", "application/vnd.github+json").header("User-Agent", "ARS-Suite-Installer").GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) throw new IOException("could not read the release list from GitHub: HTTP " + resp.statusCode());
        return resp.body();
    }

    // ---- laying it out ---------------------------------------------------------------------------------------------------------

    /** What the launchers and shortcuts expect to find, relative to the install folder. */
    static final List<String> ICONS = List.of("suite.png", "log.png", "map.png", "sat.png", "digi.png", "vault.png", "learn.png");
    static final String ICON_DIR = "ars-fx/src/main/resources/com/ars/fx/icons/";

    /** Write the launchers and icons (bundled in this jar) into the install folder. The jar itself is placed by {@link #install}. */
    static void writeSupportFiles(Path dir) throws IOException {
        Path app = dir.resolve("ars-fx");
        Files.createDirectories(app.resolve("target"));
        for (String f : List.of("ars-fx.sh", "ars-fx.bat")) {
            Path out = app.resolve(f);
            copyResource("/standalone/" + f, out);
        }
        try { app.resolve("ars-fx.sh").toFile().setExecutable(true, false); } catch (Exception ignored) {}
        Path icons = dir.resolve(ICON_DIR);
        Files.createDirectories(icons);
        for (String i : ICONS) copyResource("/standalone/icons/" + i, icons.resolve(i));
    }

    private static void copyResource(String res, Path out) throws IOException {
        try (InputStream in = Standalone.class.getResourceAsStream(res)) {
            if (in == null) throw new IOException("this installer jar is missing " + res + " (rebuild it)");
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** The version recorded by the last standalone install in {@code dir}, or null. */
    static String installedVersion(Path dir) {
        try { return Files.readString(dir.resolve(VERSION_FILE)).trim(); } catch (IOException e) { return null; }
    }

    /**
     * Fetch the newest release for this computer into {@code dir} and lay it out. When the same version is already there it is not
     * downloaded again. Returns the install folder to hand to the shortcut writers.
     */
    static Path install(Path dir, String osName, String osArch, HttpClient http, String releasesJson) throws IOException, InterruptedException {
        if (Runtime.version().feature() < 21)
            throw new IOException("ARS Suite needs Java 21 or newer; this is Java " + Runtime.version().feature() + ". Install Java 21 (Temurin, OpenJDK, Zulu, Microsoft Build...) and run this installer again.");
        Release r = pickLatest(releasesJson);
        if (r == null) throw new IOException("no ARS Suite release (a v… tag) was found on GitHub");
        String key = platformKey(osName, osArch);
        Asset a = assetFor(r, key);
        if (a == null) throw new IOException("release " + r.tag() + " has no jar for this computer (" + key + ")");
        String hex = expectedSha256(a);
        if (hex == null) throw new IOException("GitHub published no SHA-256 for " + a.name() + ", so it cannot be checked: not installed");

        Path jar = dir.resolve("ars-fx").resolve("target").resolve(jarName(key));
        writeSupportFiles(dir);
        if (Files.isRegularFile(jar) && r.version().equals(installedVersion(dir)) && sha256(jar).equals(hex)) {
            Installer.info("ARS Suite " + r.version() + " is already installed here; not downloaded again.");
        } else {
            Installer.info("Downloading " + a.name() + " …");
            download(http, a.url(), jar, hex);
            Files.writeString(dir.resolve(VERSION_FILE), r.version() + System.lineSeparator());
            Installer.info("Downloaded and checked (SHA-256 matches): ARS Suite " + r.version());
        }
        return dir;
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------------

    private static boolean flag(JsonObject o, String k) { return o.has(k) && o.get(k).isJsonPrimitive() && o.get(k).getAsBoolean(); }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        return e == null || e.isJsonNull() ? "" : e.getAsString();
    }
}
