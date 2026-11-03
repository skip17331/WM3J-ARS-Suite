package com.ars.fx.data;

import com.ars.fx.AppVersion;
import com.ars.fx.data.UpdateChecker.Asset;
import com.ars.fx.data.UpdateChecker.Release;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.function.DoubleConsumer;

/**
 * Applies an update found by {@link UpdateChecker}: download the release jar for this machine, check it against the
 * SHA-256 GitHub publishes for the asset, then hand over to a small per-OS helper that swaps the jar once this app
 * has exited and relaunches it (rolling back if the new version does not start). Windows is implemented;
 * {@link #supported()} is false elsewhere, and for a dev run from {@code target/classes}.
 */
public final class UpdateApplier {
    private UpdateApplier() {}

    public static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"); }

    /** The jar this app is running from, or null when it isn't a packaged jar (IDE / {@code mvn javafx:run}). */
    public static Path installedJar() {
        try {
            Path p = Path.of(UpdateApplier.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar") ? p : null;
        } catch (Exception e) { return null; }
    }

    /** The installed {@code ars-fx.bat} that sits one folder above the jar's {@code target/} folder, or null. */
    static Path launcherFor(Path jar) {
        if (jar == null || jar.getParent() == null || jar.getParent().getParent() == null) return null;
        Path bat = jar.getParent().getParent().resolve("ars-fx.bat");
        return Files.isRegularFile(bat) ? bat : null;
    }

    /** Can this install update itself in place? */
    public static boolean supported() {
        if (!isWindows()) return false;               // macOS / Linux updaters are separate and not built yet
        Path jar = installedJar();
        return jar != null && launcherFor(jar) != null;
    }

    /** Where downloads and the helper script go: {@code ~/.j-hub/updates}, next to the suite's other state. */
    static Path updatesDir() {
        return Path.of(System.getProperty("user.home"), ".j-hub", "updates");
    }

    // ---- checksum (pure; unit-tested) -------------------------------------------------------------

    /** The hex SHA-256 in a GitHub digest ({@code "sha256:<hex>"}), lower-cased; null if absent or another algorithm. */
    public static String expectedSha256(Asset a) {
        String d = a == null ? null : a.digest();
        if (d == null || !d.regionMatches(true, 0, "sha256:", 0, 7)) return null;
        String hex = d.substring(7).trim().toLowerCase(java.util.Locale.ROOT);
        return hex.matches("[0-9a-f]{64}") ? hex : null;
    }

    public static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream din = new DigestInputStream(in, md)) { din.transferTo(OutputStream.nullOutputStream()); }
            return HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) { throw new IOException(e); }
    }

    // ---- download ---------------------------------------------------------------------------------

    /**
     * Download and verify the jar for this machine. Refuses a release whose asset carries no SHA-256, and deletes the
     * file if it does not match. {@code progress} receives 0..1 (called on this thread). Returns the verified file.
     */
    public static Path download(Release r, DoubleConsumer progress) throws IOException, InterruptedException {
        Asset a = UpdateChecker.assetForThisMachine(r);
        if (a == null) throw new IOException("this release has no download for this computer");
        String want = expectedSha256(a);
        if (want == null) throw new IOException("this release has no published checksum, so it can't be verified — update manually");
        Files.createDirectories(updatesDir());
        Path part = updatesDir().resolve(a.name() + ".part"), done = updatesDir().resolve(a.name());
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.ALWAYS).build();
        HttpResponse<InputStream> resp = http.send(HttpRequest.newBuilder(URI.create(a.url())).timeout(Duration.ofMinutes(10))
                .header("User-Agent", "WM3J-ARS-Suite/" + AppVersion.current()).GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) throw new IOException("download failed: HTTP " + resp.statusCode());
        long total = a.size() > 0 ? a.size() : resp.headers().firstValueAsLong("Content-Length").orElse(-1);
        try (InputStream in = resp.body(); OutputStream out = Files.newOutputStream(part)) {
            byte[] buf = new byte[64 * 1024]; long got = 0; int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n); got += n;
                if (total > 0) progress.accept(Math.min(1.0, (double) got / total));
            }
        } catch (IOException | RuntimeException e) { Files.deleteIfExists(part); throw e; }
        String have = sha256(part);
        if (!have.equals(want)) {
            Files.deleteIfExists(part);
            throw new IOException("the download failed its checksum check and was discarded");
        }
        Files.move(part, done, StandardCopyOption.REPLACE_EXISTING);
        progress.accept(1.0);
        return done;
    }

    // ---- hand over to the per-OS helper ------------------------------------------------------------

    /** The helper's command line (pure; unit-tested). */
    static List<String> windowsCommand(Path script, long appPid, Path jar, Path newJar, Path launcher, Path log) {
        return List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString(),
                "-AppPid", Long.toString(appPid), "-Jar", jar.toString(), "-NewJar", newJar.toString(),
                "-Launcher", launcher.toString(), "-Log", log.toString());
    }

    /**
     * Start the Windows helper. It waits for this process to exit, so the caller must exit right after this returns.
     * @throws IOException if the helper can't be written or started (the app keeps running; nothing was changed)
     */
    public static void launchHelper(Path newJar) throws IOException {
        if (!isWindows()) throw new IOException("in-app update is only available on Windows so far");
        Path jar = installedJar();
        Path launcher = launcherFor(jar);
        if (jar == null || launcher == null) throw new IOException("this copy of the app can't update itself (not an installed jar)");
        Files.createDirectories(updatesDir());
        Path script = updatesDir().resolve("update-windows.ps1");
        try (InputStream in = UpdateApplier.class.getResourceAsStream("/com/ars/fx/update/update-windows.ps1")) {
            if (in == null) throw new IOException("updater script is missing from this build");
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        Path log = updatesDir().resolve("update.log");
        new ProcessBuilder(windowsCommand(script, ProcessHandle.current().pid(), jar, newJar, launcher, log))
                .redirectErrorStream(true).redirectOutput(updatesDir().resolve("update-helper.out").toFile()).start();
    }
}
