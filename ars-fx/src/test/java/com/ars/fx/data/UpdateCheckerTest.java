package com.ars.fx.data;

import com.ars.fx.data.UpdateChecker.Release;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerTest {

    private static final String JSON = """
        {"tag_name":"v1.12.0","name":"WM3J ARS Suite v1.12.0","html_url":"https://github.com/x/y/releases/tag/v1.12.0",
         "body":"notes here","draft":false,"prerelease":false,
         "assets":[
          {"name":"ars-fx-1.12.0-windows-x86_64.jar","browser_download_url":"https://dl/win.jar","size":100},
          {"name":"ars-fx-1.12.0-linux-x86_64.jar","browser_download_url":"https://dl/lin.jar","size":101},
          {"name":"ars-fx-1.12.0-linux-aarch64.jar","browser_download_url":"https://dl/pi.jar","size":102},
          {"name":"ars-fx-1.12.0-macos-x86_64.jar","browser_download_url":"https://dl/mac.jar","size":103},
          {"name":"ars-fx-1.12.0-macos-aarch64.jar","browser_download_url":"https://dl/mac-arm.jar","size":104},
          {"name":"SHA256SUMS","browser_download_url":"https://dl/sums","size":5}]}
        """;

    @Test
    void parsesReleaseFields() {
        Release r = UpdateChecker.parse(JSON);
        assertEquals("v1.12.0", r.tag());
        assertEquals("1.12.0", r.version());
        assertEquals("notes here", r.notes());
        assertEquals(6, r.assets().size());
        assertEquals(100, r.assets().get(0).size());
    }

    @Test
    void rejectsJsonWithoutTag() {
        assertThrows(IllegalArgumentException.class, () -> UpdateChecker.parse("{\"name\":\"x\"}"));
        assertThrows(IllegalArgumentException.class, () -> UpdateChecker.parse("not json"));
    }

    @Test
    void toleratesMissingAssetsAndNulls() {
        Release r = UpdateChecker.parse("{\"tag_name\":\"v2.0.0\",\"body\":null}");
        assertEquals("", r.notes());
        assertTrue(r.assets().isEmpty());
    }

    @Test
    void platformKeyCoversThreeOsesAndArms() {
        assertEquals("windows-x86_64", UpdateChecker.platformKey("Windows 11", "amd64"));
        assertEquals("macos-aarch64", UpdateChecker.platformKey("Mac OS X", "aarch64"));
        assertEquals("macos-x86_64", UpdateChecker.platformKey("Mac OS X", "x86_64"));
        assertEquals("linux-x86_64", UpdateChecker.platformKey("Linux", "amd64"));
        assertEquals("linux-aarch64", UpdateChecker.platformKey("Linux", "aarch64"));
    }

    @Test
    void picksTheMatchingAssetAndIgnoresOthers() {
        Release r = UpdateChecker.parse(JSON);
        assertEquals("https://dl/win.jar", UpdateChecker.assetFor(r, "windows-x86_64").url());
        assertEquals("https://dl/mac-arm.jar", UpdateChecker.assetFor(r, "macos-aarch64").url());
        assertEquals("https://dl/pi.jar", UpdateChecker.assetFor(r, "linux-aarch64").url());
        assertNull(UpdateChecker.assetFor(r, "freebsd-x86_64"));
    }

    @Test
    void offersOnlyNewerAndNotSkipped() {
        Release r = UpdateChecker.parse(JSON);
        assertTrue(UpdateChecker.shouldOffer(r, "1.11.0", ""));
        assertFalse(UpdateChecker.shouldOffer(r, "1.12.0", ""));
        assertFalse(UpdateChecker.shouldOffer(r, "1.13.0", ""));
        assertFalse(UpdateChecker.shouldOffer(r, "1.11.0", "v1.12.0"));          // skipped
        assertTrue(UpdateChecker.shouldOffer(r, "1.11.0", "v1.11.5"));           // an older skip doesn't hide a newer release
    }
}
