package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.ContestState;
import com.ars.fx.data.HubConfig;
import com.jlog.db.ContestQsoDao;
import com.jlog.db.DatabaseManager;
import com.jlog.model.QsoRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The host uses the real contest database (as the app does); a joined station is held in memory. Covers the bridge the unit tests cannot: DAO, derived values, score. */
class NetDbIntegrationTest {
    static final String CONTEST = "CQ_WW_CW", KEY = "contest-key";
    static final ContestQsoDao DAO = ContestQsoDao.getInstance();
    static NetNode host; static MemStore client; static NetNode joined;

    @BeforeAll static void start() throws Exception {
        Path home = Files.createTempDirectory("ars-net-it");
        System.setProperty("user.home", home.toString());
        HubConfig.reload();
        HubConfig.set("station.call", "W1AW"); HubConfig.set("station.grid", "FN31pr");
        DatabaseManager.getInstance().initAll();
        // open the contest without ContestState.setActive: that writes the operator's real "active contest" preference
        com.jlog.plugin.PluginLoader.getInstance().init();
        var plugin = com.jlog.plugin.PluginLoader.getInstance().getById(CONTEST);
        assertNotNull(plugin, "the contest definition loads");
        var f = ContestState.class.getDeclaredField("plugin"); f.setAccessible(true); f.set(null, plugin);
        assertNotNull(ContestState.plugin(), "the contest is open");
        DAO.setStationId("HOSTPC");
        host = new NetNode("HOSTPC", "W1AW", KEY, new ContestNetwork.DbStore(), () -> null);
        host.startHost(0);
        client = new MemStore("CLIENTPC", CONTEST);
        joined = new NetNode("CLIENTPC", "K1ABC", KEY, client, () -> null);
        joined.startJoin("127.0.0.1", host.port());
        await("joined", () -> host.connectedCount() == 1);
    }
    @AfterAll static void stop() { if (joined != null) joined.stop(); if (host != null) host.stop(); }

    interface Cond { boolean ok() throws Exception; }
    static void await(String what, Cond c) throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) { if (c.ok()) return; Thread.sleep(30); }
        fail("timed out waiting for: " + what);
    }
    static QsoRecord row(String uid) throws Exception { return DAO.findByUid(uid); }

    @Test void aQsoFromAnotherStationIsStoredWithItsIdentityAndGetsPointsHere() throws Exception {
        QsoRecord q = client.log("DL1ABC", "20m", LocalDateTime.of(2026, 11, 5, 16, 0));
        await("stored in the host's database", () -> row(q.getUid()) != null);
        QsoRecord r = row(q.getUid());
        assertEquals("CLIENTPC", r.getOrigin(), "it says which station logged it"); assertEquals("DL1ABC", r.getCallsign()); assertEquals(q.getUpdatedMs(), r.getUpdatedMs());
        await("points worked out here", () -> row(q.getUid()).getPoints() > 0);
        assertFalse(row(q.getUid()).isDupe());
        await("the score counts it", () -> ContestState.score().qsoCount() >= 1);
    }

    @Test void aSecondQsoWithTheSameStationOnTheSameBandIsAcrossStationsADupeHere() throws Exception {
        QsoRecord first = client.log("JA1XYZ", "15m", LocalDateTime.of(2026, 11, 5, 17, 0));
        await("first stored", () -> row(first.getUid()) != null);
        QsoRecord second = client.log("JA1XYZ", "15m", LocalDateTime.of(2026, 11, 5, 17, 10));
        await("second stored and flagged", () -> row(second.getUid()) != null && row(second.getUid()).isDupe());
        assertFalse(row(first.getUid()).isDupe(), "the earlier one is the valid QSO");
        // the scorer skips every dupe, so the score counts the pair once (the dupe's stored points are not used, as when logging locally)
        await("the score counts only the valid QSOs", () -> ContestState.score().qsoCount() == DAO.fetchByContest(CONTEST).stream().filter(x -> !x.isDupe()).count());
        await("the first has points", () -> row(first.getUid()).getPoints() > 0);

        client.erase(first.getUid());                                                  // erasing the first frees the second
        await("the erase reached the host", () -> row(first.getUid()) == null);
        await("the later QSO is no longer a dupe and scores", () -> !row(second.getUid()).isDupe() && row(second.getUid()).getPoints() > 0);
    }

    @Test void aQsoLoggedAtTheHostGoesOutAndAnEraseLeavesATombstoneThere() throws Exception {
        QsoRecord q = new QsoRecord(); q.setContestId(CONTEST); q.setCallsign("EA8XX"); q.setBand("40m"); q.setMode("CW");
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 5, 18, 0)); q.setRstSent("599"); q.setRstReceived("599");
        DAO.insert(q);                                                                   // exactly what the contest page does
        await("the joined station received it", () -> client.byCall("EA8XX") != null);
        assertEquals("HOSTPC", client.byCall("EA8XX").getOrigin());
        DAO.delete(q.getId());
        await("and the erase", () -> client.byCall("EA8XX") == null);
        assertTrue(client.metaOf(q.getUid()).deleted());
        assertTrue(DAO.metaOf(q.getUid()).deleted(), "the host remembers the erase too");
    }

    @Test void re_workingDupesAtTheHostIsNotSentAsAnEdit() throws Exception {
        QsoRecord q = client.log("PY2ZZ", "10m", LocalDateTime.of(2026, 11, 5, 19, 0));
        await("stored", () -> row(q.getUid()) != null);
        int before = client.applied.get();
        long v = row(q.getUid()).getUpdatedMs();
        DAO.updateDerived(row(q.getUid()).getId(), 9, false);
        Thread.sleep(400);
        assertEquals(v, row(q.getUid()).getUpdatedMs(), "the version did not change");
        assertEquals(before, client.applied.get(), "nothing was sent to the other station");
    }
}
