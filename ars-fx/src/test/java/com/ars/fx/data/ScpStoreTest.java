package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScpStoreTest {
    String oldHome; Path dir;
    final List<String> requests = new ArrayList<>();
    String etag = "aaa111"; String fileBody = bigFile("2026.11.04");
    int listStatus = 200, fileStatus = 200;

    static String bigFile(String release) {
        StringBuilder b = new StringBuilder("!!Order,1,1\n#\n# Super Check Partial\n# Release " + release + "\n#\n");
        for (int i = 0; i < 1500; i++) b.append("K").append(i % 10).append((char) ('A' + i / 100 % 26)).append((char) ('A' + i / 10 % 26)).append((char) ('A' + i % 26)).append('\n');
        return b.toString();
    }

    @BeforeEach void setUp(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload();
        dir = tmp.resolve("scp"); ScpStore.setDirForTest(dir);
        ScpStore.setHttpForTest(url -> {
            requests.add(url);
            if (url.endsWith("/api/v1/files")) return new ScpStore.Reply(listStatus,
                    ("[{\"name\":\"MASTER.DTA\",\"size\":1,\"etag\":\"zzz\"},{\"name\":\"MASTER.SCP\",\"size\":359611,\"etag\":\"" + etag + "\",\"modified\":\"x\"}]").getBytes(StandardCharsets.UTF_8));
            if (url.endsWith("/MASTER.SCP")) return new ScpStore.Reply(fileStatus, fileBody.getBytes(StandardCharsets.ISO_8859_1));
            throw new IOException("unexpected " + url);
        });
    }
    @AfterEach void tearDown() { ScpStore.setHttpForTest(null); ScpStore.setDirForTest(null); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    static final Instant T0 = Instant.parse("2026-11-05T12:00:00Z");

    @Test void firstUseDownloadsTheFileAndKeepsItWithItsEtag() throws Exception {
        ScpStore.Outcome o = ScpStore.refresh(T0, false);
        assertTrue(o.changed(), o.message());
        assertTrue(o.message().contains("1500") || o.message().contains("calls"), o.message());
        assertEquals(List.of(ScpStore.SITE + "/api/v1/files", ScpStore.SITE + "/MASTER.SCP"), requests);
        assertTrue(Files.exists(dir.resolve("MASTER.SCP")));
        assertEquals("aaa111", Files.readString(dir.resolve("MASTER.etag")).trim());
        assertEquals(1500, ScpStore.current().size());
        assertEquals("2026.11.04", ScpStore.current().release());
        assertFalse(Files.exists(dir.resolve("MASTER.SCP.part")), "no half-written file is left behind");
    }

    @Test void aSecondCheckWithinADayDoesNotTouchTheNetwork() {
        ScpStore.refresh(T0, false); requests.clear();
        ScpStore.Outcome o = ScpStore.refresh(T0.plus(Duration.ofHours(23)), false);
        assertFalse(o.changed()); assertTrue(requests.isEmpty(), "at most one check a day: " + requests);
    }

    @Test void afterADayAnUnchangedFileIsNotDownloadedAgain() {
        ScpStore.refresh(T0, false); requests.clear();
        ScpStore.Outcome o = ScpStore.refresh(T0.plus(Duration.ofHours(25)), false);
        assertFalse(o.changed()); assertEquals("Up to date", o.message());
        assertEquals(List.of(ScpStore.SITE + "/api/v1/files"), requests, "only the small file list is asked for");
    }

    @Test void aChangedEtagDownloadsTheNewFile() {
        ScpStore.refresh(T0, false); requests.clear();
        etag = "bbb222"; fileBody = bigFile("2026.11.07");
        ScpStore.Outcome o = ScpStore.refresh(T0.plus(Duration.ofHours(30)), false);
        assertTrue(o.changed(), o.message());
        assertEquals("2026.11.07", ScpStore.current().release());
        assertEquals(2, requests.size());
    }

    @Test void whenAutomaticUpdatesAreOffNothingIsFetchedUnlessTheOperatorAsks() {
        HubConfig.setBool(ScpStore.KEY_AUTO, false);
        ScpStore.Outcome o = ScpStore.refresh(T0, false);
        assertFalse(o.changed()); assertTrue(requests.isEmpty());
        assertTrue(ScpStore.refresh(T0, true).changed(), "a manual refresh still works");
    }

    @Test void aManualRefreshIsLimitedToOnceAnHour() {
        ScpStore.refresh(T0, true); requests.clear();
        ScpStore.refresh(T0.plus(Duration.ofMinutes(30)), true);
        assertTrue(requests.isEmpty());
        ScpStore.refresh(T0.plus(Duration.ofMinutes(70)), true);
        assertEquals(1, requests.size());
    }

    @Test void aFailureKeepsTheCopyWeHave() {
        ScpStore.refresh(T0, false);
        etag = "ccc333"; fileStatus = 503;
        ScpStore.Outcome o = ScpStore.refresh(T0.plus(Duration.ofHours(30)), false);
        assertFalse(o.changed()); assertTrue(o.message().contains("saved copy"), o.message());
        assertEquals(1500, ScpStore.current().size());
        assertEquals("aaa111", read(dir.resolve("MASTER.etag")), "the stored etag still describes the file we hold, so we try again");
    }

    @Test void aDownloadThatIsNotACallListIsRefused() {
        fileBody = "<html>Service unavailable</html>";
        ScpStore.Outcome o = ScpStore.refresh(T0, false);
        assertFalse(o.changed());
        assertFalse(Files.exists(dir.resolve("MASTER.SCP")));
        assertTrue(ScpStore.current().isEmpty());
        assertTrue(o.message().startsWith("Could not download"), o.message());
    }

    @Test void aMissingFileOrABadListIsReportedNotThrown() {
        listStatus = 500;
        assertTrue(ScpStore.refresh(T0, false).message().contains("HTTP 500"));
        listStatus = 200;
        ScpStore.setHttpForTest(url -> new ScpStore.Reply(200, "[{\"name\":\"OTHER\",\"etag\":\"x\"}]".getBytes(StandardCharsets.UTF_8)));
        assertTrue(ScpStore.refresh(T0, false).message().contains("not in the site's file list"));
    }

    @Test void theEtagIsReadFromTheRightEntry() {
        String json = "[{\"name\":\"MASTER.DTA\",\"etag\":\"one\"},{\"name\":\"MASTER.SCP\",\"size\":5,\"etag\":\"two\",\"modified\":\"m\"},{\"name\":\"MASTERDX.SCP\",\"etag\":\"three\"}]";
        assertEquals("two", ScpStore.etagOf(json, "MASTER.SCP"));
        assertEquals("three", ScpStore.etagOf(json, "MASTERDX.SCP"));
        assertNull(ScpStore.etagOf(json, "SCP.DB"));
    }

    static String read(Path p) { try { return Files.readString(p).trim(); } catch (IOException e) { return "?"; } }
}
