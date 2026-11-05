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
 * Applies an update found by {@link UpdateChecker}: download the release jar for this install, check it against the
 * SHA-256 GitHub publishes for the asset, then hand over to a small per-OS helper that swaps the jar once this app
 * has exited and relaunches it (rolling back if the new version does not start). Windows and Linux are implemented;
 * macOS is not yet. {@link #supported()} is false elsewhere, for a dev run from {@code target/classes}, and when the
 * install folder isn't writable ({@link #manualReason()} says why).
 */
public final class UpdateApplier {
    private UpdateApplier() {}

    public static boolean isWindows() { return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("win"); }
    public static boolean isLinux()   { return System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT).contains("linux"); }

    /** The jar this app is running from, or null when it isn't a packaged jar (IDE / {@code mvn javafx:run}). */
    public static Path installedJar() {
        try {
            Path p = Path.of(UpdateApplier.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar") ? p : null;
        } catch (Exception e) { return null; }
    }

    /** The installed launcher ({@code ars-fx.bat} / {@code ars-fx.sh}) one folder above the jar's {@code target/} folder, or null. */
    static Path launcherFor(Path jar) {
        if (jar == null || jar.getParent() == null || jar.getParent().getParent() == null) return null;
        Path f = jar.getParent().getParent().resolve(isWindows() ? "ars-fx.bat" : "ars-fx.sh");
        return Files.isRegularFile(f) ? f : null;
    }

    /**
     * Which release asset replaces the installed jar. Windows has one. On Linux the INSTALLED JAR decides, not the CPU:
     * {@code install.sh} builds {@code ars-fx-linux.jar} on x86_64 and the JavaFX-less {@code ars-fx-pi.jar} on aarch64.
     */
    static String assetKeyFor(Path jar) {
        if (isWindows()) return "windows-x86_64";
        if (isLinux() && jar != null) return linuxKeyForJarName(jar.getFileName().toString());
        return null;
    }

    static String linuxKeyForJarName(String name) {
        return switch (name) {
            case "ars-fx-linux.jar" -> "linux-x86_64";
            case "ars-fx-pi.jar" -> "linux-aarch64";
            default -> null;
        };
    }

    /** Both the jar and its folder must be writable: the swap creates {@code .new}/{@code .bak} beside it. */
    static boolean writable(Path jar) {
        return jar != null && Files.isWritable(jar) && jar.getParent() != null && Files.isWritable(jar.getParent());
    }

    /** Can this install update itself in place? */
    public static boolean supported() { return installedJar() != null && manualReason() == null; }

    /**
     * Why this install must be updated by hand, or null if it can update itself (or is a dev run, where there is nothing to say).
     * Shown in the update card in place of the "Update now" button.
     */
    public static String manualReason() {
        Path jar = installedJar();
        if (jar == null) return null;
        if (!isWindows() && !isLinux())
            return "In-app update isn't available on this system yet. Download the new version from the release page.";
        if (launcherFor(jar) == null || assetKeyFor(jar) == null)
            return "This copy isn't set up for in-app update. Download the new version from the release page.";
        if (!writable(jar))
            return "The install folder isn't writable by you, so it can't update itself. Update it as the user who owns it, or download the new version.";
        return null;
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
     * Download and verify the jar for this install. Refuses a release whose asset carries no SHA-256, and deletes the
     * file if it does not match. {@code progress} receives 0..1 (called on this thread). Returns the verified file.
     */
    public static Path download(Release r, DoubleConsumer progress) throws IOException, InterruptedException {
        String key = assetKeyFor(installedJar());
        Asset a = key == null ? null : UpdateChecker.assetFor(r, key);
        if (a == null) throw new IOException("this release has no download for this install");
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
        // the download is verified: check the legal documents in it and update the local copies if they are newer
        try { LegalDocs.installFromJar(done); } catch (Exception ignored) { /* never let a notice stop an update */ }
        progress.accept(1.0);
        return done;
    }

    // ---- hand over to the per-OS helper ------------------------------------------------------------

    /** The Windows helper's command line (pure; unit-tested). */
    static List<String> windowsCommand(Path script, long appPid, Path jar, Path newJar, Path launcher, Path log) {
        return List.of("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File", script.toString(),
                "-AppPid", Long.toString(appPid), "-Jar", jar.toString(), "-NewJar", newJar.toString(),
                "-Launcher", launcher.toString(), "-Log", log.toString());
    }

    /** The Linux helper's command line (pure; unit-tested). Positional: APP_PID JAR NEW_JAR LAUNCHER LOG. */
    static List<String> unixCommand(Path script, long appPid, Path jar, Path newJar, Path launcher, Path log) {
        return List.of("sh", script.toString(), Long.toString(appPid), jar.toString(), newJar.toString(), launcher.toString(), log.toString());
    }

    /**
     * Start the helper for this OS. It waits for this process to exit, so the caller must exit right after this returns.
     * Refuses while anything is transmitting.
     * @throws IOException if the update can't start (the app keeps running; nothing was changed)
     */
    public static void launchHelper(Path newJar) throws IOException {
        if (TxController.busy() || TxController.hubTransmitting())
            throw new IOException("the transmitter is keyed — wait until you are off the air, then try again");
        Path jar = installedJar();
        Path launcher = launcherFor(jar);
        if (jar == null || launcher == null) throw new IOException("this copy of the app can't update itself (not an installed jar)");
        String reason = manualReason();
        if (reason != null) throw new IOException(reason);
        Files.createDirectories(updatesDir());
        boolean win = isWindows();
        String resource = win ? "update-windows.ps1" : "update-unix.sh";
        Path script = updatesDir().resolve(resource);
        try (InputStream in = UpdateApplier.class.getResourceAsStream("/com/ars/fx/update/" + resource)) {
            if (in == null) throw new IOException("updater script is missing from this build");
            Files.copy(in, script, StandardCopyOption.REPLACE_EXISTING);
        }
        Path log = updatesDir().resolve("update.log");
        Files.deleteIfExists(updatesDir().resolve("last-result.txt"));
        List<String> cmd = win ? windowsCommand(script, ProcessHandle.current().pid(), jar, newJar, launcher, log)
                               : unixCommand(script, ProcessHandle.current().pid(), jar, newJar, launcher, log);
        new ProcessBuilder(cmd).redirectErrorStream(true).redirectOutput(updatesDir().resolve("update-helper.out").toFile()).start();
    }

    /**
     * What the last update's helper reported, if it didn't go well ({@code failed: …} / {@code rolled back: …}); null if there is
     * nothing to say. Read once: the file is deleted so the message is shown a single time.
     */
    public static String takeLastResult() {
        Path f = updatesDir().resolve("last-result.txt");
        try {
            if (!Files.isRegularFile(f)) return null;
            String t = Files.readString(f).trim();
            Files.deleteIfExists(f);
            return t.startsWith("failed") || t.startsWith("rolled back") ? t : null;
        } catch (IOException e) { return null; }
    }
}
