package com.ars.fx.data;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CtyUpdaterTest {
    @TempDir Path dir;
    byte[] good;

    @BeforeEach void setUp() throws Exception {
        CtyData.setUserDirForTest(dir);
        try (InputStream in = CtyData.class.getResourceAsStream("/com/ars/fx/cty/cty.dat")) { good = in.readAllBytes(); }
    }
    @AfterEach void tearDown() { CtyData.setUserDirForTest(null); }

    @Test void aGoodDownloadIsSavedAndUsed() throws Exception {
        assertTrue(CtyUpdater.refreshIfStale(Instant.now(), () -> good));
        assertTrue(Files.isRegularFile(CtyData.userFile()));
        assertTrue(CtyData.plausible(CtyData.current()));
    }
    @Test void aFreshCopyIsNotFetchedAgain() throws Exception {
        assertTrue(CtyUpdater.refreshIfStale(Instant.now(), () -> good));
        assertFalse(CtyUpdater.refreshIfStale(Instant.now().plus(Duration.ofDays(2)), () -> { fail("fetched a fresh copy"); return null; }));
        assertTrue(CtyUpdater.refreshIfStale(Instant.now().plus(Duration.ofDays(8)), () -> good));
    }
    @Test void garbageTruncatedAndEmptyDownloadsAreRefused() throws Exception {
        assertFalse(CtyUpdater.refreshIfStale(Instant.now(), () -> "<html>404 not found</html>".getBytes()));
        assertFalse(CtyUpdater.refreshIfStale(Instant.now(), () -> java.util.Arrays.copyOf(good, good.length / 3)));
        assertFalse(CtyUpdater.refreshIfStale(Instant.now(), () -> new byte[0]));
        assertFalse(CtyUpdater.refreshIfStale(Instant.now(), () -> null));
        assertFalse(Files.exists(CtyData.userFile()));
        assertTrue(CtyData.current().size() > 300);          // still the bundled copy
    }
    @Test void aBrokenSavedCopyFallsBackToTheBundledOne() throws Exception {
        Files.createDirectories(CtyData.userFile().getParent());
        Files.writeString(CtyData.userFile(), "not a country file");
        CtyData.reload();
        assertTrue(CtyData.current().size() > 300);
    }
}
