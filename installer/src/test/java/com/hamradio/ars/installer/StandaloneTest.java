package com.hamradio.ars.installer;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** The install that needs no build: which release, where it goes, that it is checked, and that the rest of the installer can use the result. */
class StandaloneTest {
    @TempDir Path dir;

    // ---- when it is used -------------------------------------------------------------------------------------------------------

    @Test void downloadIsWantedWhenAskedForOrWhenThereIsNoCheckout() throws Exception {
        Path cwd = Files.createDirectories(dir.resolve("somewhere/else"));
        assertTrue(Standalone.wanted(new String[]{}, null, cwd), "no checkout anywhere: nothing to build from");
        assertTrue(Standalone.wanted(new String[]{"--download"}, "/some/checkout", cwd), "asked for, whatever else is set");
        assertFalse(Standalone.wanted(new String[]{"--root", "/some/checkout"}, null, cwd));
        assertFalse(Standalone.wanted(new String[]{}, "/some/checkout", cwd), "ARS_CONTEST_HOME names a checkout");
        assertTrue(Standalone.wanted(new String[]{}, "  ", cwd), "a blank ARS_CONTEST_HOME names nothing");
    }

    @Test void aCheckoutInTheCurrentFolderOrItsParentIsBuiltFromNotDownloaded() throws Exception {
        Path repo = Files.createDirectories(dir.resolve("repo"));
        Files.createDirectories(repo.resolve("ars-fx")); Files.writeString(repo.resolve("ars-fx/pom.xml"), "<project/>");
        assertFalse(Standalone.wanted(new String[]{}, null, repo));
        assertFalse(Standalone.wanted(new String[]{}, null, Files.createDirectories(repo.resolve("installer"))), "run from installer/");
    }

    @Test void optionReadsTheValueAfterAFlag() {
        assertEquals("/x", Standalone.option(new String[]{"--download", "--dir", "/x"}, "--dir"));
        assertNull(Standalone.option(new String[]{"--dir"}, "--dir"), "a flag with no value");
        assertNull(Standalone.option(new String[]{}, "--dir"));
    }

    // ---- where ---------------------------------------------------------------------------------------------------------------

    @Test void theInstallFolderIsNeverTheOperatorsDataFolder() {
        Path home = Path.of("/home/op");
        assertEquals(home.resolve(".local/share/ars-suite-app"), Standalone.installDir("Linux", home, null));
        assertEquals(home.resolve("Library/Application Support/ARS-Suite"), Standalone.installDir("Mac OS X", home, null));
        assertEquals(Path.of("C:/Users/op/AppData/Local").resolve("ARS-Suite").resolve("app"), Standalone.installDir("Windows 11", home, "C:/Users/op/AppData/Local"));
        assertEquals(home.resolve("AppData/Local").resolve("ARS-Suite").resolve("app"), Standalone.installDir("Windows 10", home, ""), "no LOCALAPPDATA set");
        for (String os : new String[]{"Linux", "Mac OS X", "Windows 11"})
            assertFalse(Standalone.installDir(os, home, null).startsWith(home.resolve(".j-hub")));
    }

    // ---- which release -------------------------------------------------------------------------------------------------------

    @Test void platformKeysAreTheOnesTheAppsOwnUpdaterUses() {
        assertEquals("linux-x86_64", Standalone.platformKey("Linux", "amd64"));
        assertEquals("linux-aarch64", Standalone.platformKey("Linux", "aarch64"));
        assertEquals("windows-x86_64", Standalone.platformKey("Windows 11", "amd64"));
        assertEquals("macos-x86_64", Standalone.platformKey("Mac OS X", "x86_64"));
        assertEquals("macos-aarch64", Standalone.platformKey("Mac OS X", "aarch64"));
        assertEquals("macos-aarch64", Standalone.platformKey("Mac OS X", "arm64"));
    }

    @Test void theJarIsSavedUnderTheNameALocalBuildGivesIt() {
        // the Linux updater picks the release asset from this name alone, so these two must not drift
        assertEquals("ars-fx-linux.jar", Standalone.jarName("linux-x86_64"));
        assertEquals("ars-fx-pi.jar", Standalone.jarName("linux-aarch64"));
        assertEquals("ars-fx-windows.jar", Standalone.jarName("windows-x86_64"));
        assertEquals("ars-fx-mac.jar", Standalone.jarName("macos-x86_64"));
        assertEquals("ars-fx-mac-aarch64.jar", Standalone.jarName("macos-aarch64"));
        for (String k : new String[]{"linux-x86_64", "linux-aarch64", "windows-x86_64", "macos-x86_64", "macos-aarch64"})
            assertTrue(Standalone.jarName(k).matches("ars-fx-[a-z0-9-]+\\.jar"), "the launchers glob ars-fx-*.jar: " + k);
    }

    private static String releases(String... items) { return "[" + String.join(",", items) + "]"; }
    private static String rel(String tag, boolean draft, boolean pre, String assets) {
        return "{\"tag_name\":\"" + tag + "\",\"draft\":" + draft + ",\"prerelease\":" + pre + ",\"assets\":[" + assets + "]}";
    }
    private static String asset(String name, String url, String digest) {
        return "{\"name\":\"" + name + "\",\"browser_download_url\":\"" + url + "\"" + (digest == null ? "" : ",\"digest\":\"" + digest + "\"") + "}";
    }

    @Test void githubsLatestAnswerIsOneReleaseAndIsTaken() {
        Standalone.Release r = Standalone.pickLatest(rel("v1.13.39", false, false, asset("ars-fx-1.13.39-linux-x86_64.jar", "http://x/a", "sha256:" + "a".repeat(64))));
        assertEquals("v1.13.39", r.tag());
        assertEquals("1.13.39", r.version());
        assertEquals(1, r.assets().size());
        assertNull(Standalone.pickLatest(rel("nightly-2026", false, false, "")), "a tag that is not a version");
        assertNull(Standalone.pickLatest(rel("v1.13.40", true, false, "")), "a draft");
        assertNull(Standalone.pickLatest(rel("v1.13.40", false, true, "")), "a pre-release");
    }

    @Test void aMirrorsNewestFirstListWorksToo() {
        String json = releases(
                rel("nightly-2026", false, false, ""),
                rel("v1.13.41", true, false, ""),                                         // a draft
                rel("v1.13.40", false, true, ""),                                         // a pre-release
                rel("v1.13.39", false, false, asset("ars-fx-1.13.39-linux-x86_64.jar", "http://x/a", "sha256:" + "a".repeat(64))),
                rel("v1.13.38", false, false, ""));
        assertEquals("v1.13.39", Standalone.pickLatest(json).tag());
        assertNull(Standalone.pickLatest(releases(rel("nightly-2026", false, false, ""))), "no version tag at all: none");
        assertThrows(IllegalArgumentException.class, () -> Standalone.pickLatest("{\"message\":\"rate limited\"}"));
        assertThrows(IllegalArgumentException.class, () -> Standalone.pickLatest("not json at all {"));
    }

    @Test void theAssetForThisComputerIsFoundByPlatformAndNeverByTheInstallerJar() {
        String json = releases(rel("v1.13.39", false, false,
                asset("j-installer-1.13.39.jar", "http://x/i", null) + "," +
                asset("ars-fx-1.13.39-macos-aarch64.jar", "http://x/h", null) + "," +
                asset("ars-fx-1.13.39-linux-aarch64.jar", "http://x/pi", null) + "," +
                asset("ars-fx-1.13.39-linux-x86_64.jar", "http://x/l", null)));
        Standalone.Release r = Standalone.pickLatest(json);
        assertEquals("ars-fx-1.13.39-linux-x86_64.jar", Standalone.assetFor(r, "linux-x86_64").name());
        assertEquals("ars-fx-1.13.39-linux-aarch64.jar", Standalone.assetFor(r, "linux-aarch64").name());
        assertNull(Standalone.assetFor(r, "windows-x86_64"));
    }

    @Test void onlyAWellFormedSha256DigestIsTrusted() {
        String hex = "0123456789abcdef".repeat(4);
        assertEquals(hex, Standalone.expectedSha256(new Standalone.Asset("a", "u", "sha256:" + hex)));
        assertEquals(hex, Standalone.expectedSha256(new Standalone.Asset("a", "u", "SHA256:" + hex.toUpperCase())), "case does not matter");
        assertNull(Standalone.expectedSha256(new Standalone.Asset("a", "u", "")));
        assertNull(Standalone.expectedSha256(new Standalone.Asset("a", "u", "md5:" + hex)));
        assertNull(Standalone.expectedSha256(new Standalone.Asset("a", "u", "sha256:abc")), "too short");
        assertNull(Standalone.expectedSha256(null));
    }

    // ---- downloading, with a local server standing in for GitHub ---------------------------------------------------------------

    private HttpServer server;
    private final AtomicInteger hits = new AtomicInteger();
    private byte[] body = "pretend this is a jar".getBytes(StandardCharsets.UTF_8);

    @BeforeEach void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", ex -> { hits.incrementAndGet(); ex.sendResponseHeaders(200, body.length); ex.getResponseBody().write(body); ex.close(); });
        server.createContext("/gone", ex -> { hits.incrementAndGet(); ex.sendResponseHeaders(404, -1); ex.close(); });
        server.start();
    }
    @AfterEach void stopServer() { server.stop(0); }

    private String url(String path) { return "http://127.0.0.1:" + server.getAddress().getPort() + path; }
    private String sha(byte[] b) throws Exception {
        Path f = Files.write(dir.resolve("tmp-" + System.nanoTime()), b);
        return Standalone.sha256(f);
    }

    @Test void aDownloadThatMatchesItsChecksumIsKept() throws Exception {
        Path out = dir.resolve("get/x.jar");
        Standalone.download(Standalone.client(), url("/ok"), out, sha(body));
        assertArrayEquals(body, Files.readAllBytes(out));
        assertFalse(Files.exists(dir.resolve("get/x.jar.part")), "no half-file left behind");
    }

    @Test void aDownloadThatDoesNotMatchIsNotInstalledAndLeavesNothing() throws Exception {
        Path out = dir.resolve("get/x.jar");
        IOException e = assertThrows(IOException.class, () -> Standalone.download(Standalone.client(), url("/ok"), out, "0".repeat(64)));
        assertTrue(e.getMessage().contains("SHA-256"), e.getMessage());
        assertFalse(Files.exists(out));
        assertFalse(Files.exists(dir.resolve("get/x.jar.part")));
    }

    @Test void aMatchingDownloadReplacesAnOlderJarButAFailedOneKeepsIt() throws Exception {
        Path out = dir.resolve("get/x.jar");
        Files.createDirectories(out.getParent()); Files.writeString(out, "the old jar");
        assertThrows(IOException.class, () -> Standalone.download(Standalone.client(), url("/gone"), out, sha(body)));
        assertEquals("the old jar", Files.readString(out), "a failed download leaves the working install as it was");
        Standalone.download(Standalone.client(), url("/ok"), out, sha(body));
        assertArrayEquals(body, Files.readAllBytes(out));
    }

    // ---- the whole install ---------------------------------------------------------------------------------------------------

    private String json(String digest) {
        return releases(rel("v1.13.39", false, false, asset("ars-fx-1.13.39-linux-x86_64.jar", url("/ok"), digest)));
    }

    @Test void theInstallLaysOutWhatTheShortcutsAndTheUpdaterExpect() throws Exception {
        Path app = dir.resolve("app");
        Path root = Standalone.install(app, "Linux", "amd64", Standalone.client(), json("sha256:" + sha(body)));
        assertEquals(app, root);
        Path jar = app.resolve("ars-fx/target/ars-fx-linux.jar");
        assertArrayEquals(body, Files.readAllBytes(jar));
        assertTrue(Files.isRegularFile(app.resolve("ars-fx/ars-fx.sh")));
        assertTrue(Files.isRegularFile(app.resolve("ars-fx/ars-fx.bat")));
        assertTrue(Files.isExecutable(app.resolve("ars-fx/ars-fx.sh")));
        for (String i : Standalone.ICONS) assertTrue(Files.size(app.resolve(Standalone.ICON_DIR + i)) > 1000, i);
        assertEquals("1.13.39", Standalone.installedVersion(app));
        assertTrue(Files.readString(app.resolve("ars-fx/ars-fx.sh")).contains("ars-fx-linux.jar"), "the launcher finds the jar by this name");
        // the shortcut writers find everything under their usual relative paths
        assertEquals(jar, Installer.resolveJar(app, "ars-fx/target/ars-fx-linux.jar"));
        assertTrue(Files.exists(app.resolve("ars-fx/src/main/resources/com/ars/fx/icons/suite.png")));
    }

    @Test void theSameVersionIsNotDownloadedTwice() throws Exception {
        Path app = dir.resolve("app");
        String j = json("sha256:" + sha(body));
        Standalone.install(app, "Linux", "amd64", Standalone.client(), j);
        Standalone.install(app, "Linux", "amd64", Standalone.client(), j);
        assertEquals(1, hits.get(), "the second run only rewrites the shortcuts");
    }

    @Test void aJarThatWasChangedOnDiskIsDownloadedAgain() throws Exception {
        Path app = dir.resolve("app");
        String j = json("sha256:" + sha(body));
        Standalone.install(app, "Linux", "amd64", Standalone.client(), j);
        Files.writeString(app.resolve("ars-fx/target/ars-fx-linux.jar"), "damaged");
        Standalone.install(app, "Linux", "amd64", Standalone.client(), j);
        assertEquals(2, hits.get());
        assertArrayEquals(body, Files.readAllBytes(app.resolve("ars-fx/target/ars-fx-linux.jar")));
    }

    @Test void aReleaseWithNoPublishedChecksumIsRefusedAndNothingIsInstalled() throws Exception {
        Path app = dir.resolve("app");
        IOException e = assertThrows(IOException.class, () -> Standalone.install(app, "Linux", "amd64", Standalone.client(), json(null)));
        assertTrue(e.getMessage().contains("SHA-256"), e.getMessage());
        assertEquals(0, hits.get(), "nothing was fetched");
        assertFalse(Files.exists(app.resolve("ars-fx/target")));
    }

    @Test void aReleaseWithoutAJarForThisComputerSaysSo() {
        Path app = dir.resolve("app");
        IOException e = assertThrows(IOException.class, () -> Standalone.install(app, "Windows 11", "amd64", Standalone.client(), json("sha256:" + "a".repeat(64))));
        assertTrue(e.getMessage().contains("windows-x86_64"), e.getMessage());
    }

    @Test void noContestReleaseAtAllSaysSo() {
        IOException e = assertThrows(IOException.class, () -> Standalone.install(dir.resolve("app"), "Linux", "amd64", Standalone.client(), "[]"));
        assertTrue(e.getMessage().contains("v… tag"), e.getMessage());
    }
}
