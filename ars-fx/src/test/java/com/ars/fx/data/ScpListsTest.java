package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScpListsTest {
    String oldHome; Path dir;
    final List<String> requests = new ArrayList<>();
    static final Instant T0 = Instant.parse("2026-11-05T12:00:00Z");

    static String body(String release, String prefix) {
        StringBuilder b = new StringBuilder("!!Order,1,1\n#\n# Super Check Partial\n# Release " + release + "\n#\n");
        for (int i = 0; i < 1500; i++) b.append(prefix).append(i % 10).append((char) ('A' + i / 100 % 26)).append((char) ('A' + i / 10 % 26)).append((char) ('A' + i % 26)).append('\n');
        return b.toString();
    }
    static ContestPlugin plugin(String id) throws Exception { return new ObjectMapper().readValue("{\"contestId\":\"" + id + "\"}", ContestPlugin.class); }

    @BeforeEach void setUp(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload();
        dir = tmp.resolve("scp"); ScpStore.setDirForTest(dir);
        ScpStore.setHttpForTest(url -> {
            requests.add(url);
            if (url.endsWith("/api/v1/files")) return new ScpStore.Reply(200, ("[{\"name\":\"MASTER.SCP\",\"etag\":\"m1\"},{\"name\":\"MASTERSS.SCP\",\"etag\":\"s1\"},"
                    + "{\"name\":\"MASTERDX.SCP\",\"etag\":\"d1\"},{\"name\":\"MASUSVE.SCP\",\"etag\":\"u1\"}]").getBytes(StandardCharsets.UTF_8));
            if (url.endsWith("/MASTER.SCP")) return new ScpStore.Reply(200, body("2026.11.06", "K").getBytes(StandardCharsets.ISO_8859_1));
            if (url.endsWith("/MASTERSS.SCP")) return new ScpStore.Reply(200, body("2026.11.06", "N").getBytes(StandardCharsets.ISO_8859_1));
            if (url.endsWith("/MASTERDX.SCP")) return new ScpStore.Reply(200, body("2026.11.06", "D").getBytes(StandardCharsets.ISO_8859_1));
            if (url.endsWith("/MASUSVE.SCP")) return new ScpStore.Reply(200, body("2026.11.06", "W").getBytes(StandardCharsets.ISO_8859_1));
            throw new IOException("unexpected " + url);
        });
    }
    @AfterEach void tearDown() { ScpStore.setHttpForTest(null); ScpStore.setDirForTest(null); System.setProperty("user.home", oldHome); HubConfig.reload(); }

    @Test void eachContestGetsTheListThatSuitsIt() throws Exception {
        assertEquals(ScpStore.Kind.SS, ScpStore.listFor(plugin("ARRL_SS_CW")));
        assertEquals(ScpStore.Kind.SS, ScpStore.listFor(plugin("ARRL_SS_SSB")));
        assertEquals(ScpStore.Kind.DX, ScpStore.listFor(plugin("ARRL_DX_CW_US")), "a W/VE station works DX calls");
        assertEquals(ScpStore.Kind.DX, ScpStore.listFor(plugin("ARRL_DX_SSB_US")));
        assertEquals(ScpStore.Kind.USVE, ScpStore.listFor(plugin("ARRL_DX_CW_DX")), "a DX station works US and Canadian calls");
        assertEquals(ScpStore.Kind.MASTER, ScpStore.listFor(plugin("CQ_WW_CW")));
        assertEquals(ScpStore.Kind.MASTER, ScpStore.listFor(plugin("TX_QSO_PARTY")));
        assertEquals(ScpStore.Kind.MASTER, ScpStore.listFor(null));
    }

    @Test void aListIsFetchedAndKeptOnItsOwn() throws Exception {
        ScpStore.Outcome o = ScpStore.refresh(ScpStore.Kind.SS, T0, false);
        assertTrue(o.changed(), o.message());
        assertEquals(List.of(ScpStore.SITE + "/api/v1/files", ScpStore.SITE + "/MASTERSS.SCP"), requests, "only the list that was asked for");
        assertTrue(Files.exists(dir.resolve("MASTERSS.SCP")));
        assertEquals("s1", Files.readString(dir.resolve("MASTERSS.etag")).trim());
        assertTrue(Files.exists(dir.resolve("MASTERSS.stamp")));
        assertEquals(1500, ScpStore.current(ScpStore.Kind.SS).size());
        assertTrue(ScpStore.current(ScpStore.Kind.MASTER).isEmpty(), "the full list was not touched");
        assertFalse(Files.exists(dir.resolve("MASTER.SCP")));
    }

    @Test void theListsKeepTheirOwnEtagsAndTimes() throws Exception {
        ScpStore.refresh(ScpStore.Kind.MASTER, T0, false);
        ScpStore.refresh(ScpStore.Kind.DX, T0, false);
        requests.clear();
        assertEquals("Checked recently", ScpStore.refresh(ScpStore.Kind.MASTER, T0.plusSeconds(3600), false).message());
        assertTrue(requests.isEmpty());
        ScpStore.Outcome sd = ScpStore.refresh(ScpStore.Kind.SS, T0.plusSeconds(3600), false);
        assertTrue(sd.changed(), "a list not fetched before is fetched even though another was checked a moment ago");
        assertEquals("m1", Files.readString(dir.resolve("MASTER.etag")).trim());
        assertEquals("d1", Files.readString(dir.resolve("MASTERDX.etag")).trim());
        assertEquals("s1", Files.readString(dir.resolve("MASTERSS.etag")).trim());
    }

    @Test void aListMissingFromTheSitesFileListIsReportedNotInvented() throws Exception {
        ScpStore.setHttpForTest(url -> {
            if (url.endsWith("/api/v1/files")) return new ScpStore.Reply(200, "[{\"name\":\"MASTER.SCP\",\"etag\":\"m1\"}]".getBytes(StandardCharsets.UTF_8));
            throw new IOException("unexpected " + url);
        });
        ScpStore.Outcome o = ScpStore.refresh(ScpStore.Kind.USVE, T0, false);
        assertFalse(o.changed());
        assertTrue(o.message().contains("MASUSVE.SCP is not in the site's file list"), o.message());
        assertTrue(ScpStore.current(ScpStore.Kind.USVE).isEmpty());
    }

    @Test void theOldSingleListCallsStillMeanTheFullList() throws Exception {
        assertTrue(ScpStore.refresh(T0, false).changed());
        assertEquals(1500, ScpStore.current().size());
        assertEquals(ScpStore.current(), ScpStore.current(ScpStore.Kind.MASTER));
    }
}
