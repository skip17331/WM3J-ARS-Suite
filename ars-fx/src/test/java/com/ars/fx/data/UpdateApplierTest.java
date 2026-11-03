package com.ars.fx.data;

import com.ars.fx.data.UpdateChecker.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UpdateApplierTest {
    private static final String HELLO_SHA = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"; // sha256("hello")

    @Test
    void expectedSha256ParsesGithubDigest() {
        assertEquals(HELLO_SHA, UpdateApplier.expectedSha256(new Asset("a", "u", 1, "sha256:" + HELLO_SHA)));
        assertEquals(HELLO_SHA, UpdateApplier.expectedSha256(new Asset("a", "u", 1, "SHA256:" + HELLO_SHA.toUpperCase())));
    }

    @Test
    void expectedSha256RejectsMissingOrOtherDigests() {
        assertNull(UpdateApplier.expectedSha256(null));
        assertNull(UpdateApplier.expectedSha256(new Asset("a", "u", 1, "")));
        assertNull(UpdateApplier.expectedSha256(new Asset("a", "u", 1, "md5:abc")));
        assertNull(UpdateApplier.expectedSha256(new Asset("a", "u", 1, "sha256:tooshort")));
    }

    @Test
    void sha256OfFileMatchesKnownValue(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("f.bin");
        Files.writeString(f, "hello");
        assertEquals(HELLO_SHA, UpdateApplier.sha256(f));
    }

    @Test
    void launcherIsFoundOnlyInTheInstalledLayout(@TempDir Path dir) throws Exception {
        Path jar = Files.createDirectories(dir.resolve("ars-fx/target")).resolve("ars-fx-windows.jar");
        Files.writeString(jar, "x");
        assertNull(UpdateApplier.launcherFor(jar));                       // no ars-fx.bat yet
        Path bat = Files.writeString(dir.resolve("ars-fx/ars-fx.bat"), "rem");
        assertEquals(bat, UpdateApplier.launcherFor(jar));
        assertNull(UpdateApplier.launcherFor(null));
    }

    @Test
    void windowsCommandPassesEveryArgument() {
        List<String> c = UpdateApplier.windowsCommand(Path.of("s.ps1"), 42, Path.of("a.jar"), Path.of("n.jar"), Path.of("l.bat"), Path.of("u.log"));
        assertEquals("powershell.exe", c.get(0));
        assertTrue(c.containsAll(List.of("-File", "s.ps1", "-AppPid", "42", "-Jar", "a.jar", "-NewJar", "n.jar", "-Launcher", "l.bat", "-Log", "u.log")));
    }

    @Test
    void notSupportedWhenNotRunningFromAnInstalledJar() {
        assertFalse(UpdateApplier.supported());   // under surefire the code lives in target/classes, not a jar
    }
}
