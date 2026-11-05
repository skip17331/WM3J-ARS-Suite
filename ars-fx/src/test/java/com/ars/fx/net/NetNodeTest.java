package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QsoRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Real WebSockets on loopback between {@link NetNode}s, each with its own in-memory log. */
class NetNodeTest {
    static final String KEY = "contest-key";
    final List<NetNode> nodes = new ArrayList<>();

    @AfterEach void stopAll() { for (NetNode n : nodes) n.stop(); }

    NetNode node(String name, MemStore s, String key) {
        NetNode n = new NetNode(name, "OP-" + name, key, s, () -> new NetStatus(name, "OP-" + name, s.contest(), "20m", "CW", 14_025_000L, 0));
        nodes.add(n);
        return n;
    }
    NetNode host(String name, MemStore s) throws Exception { NetNode n = node(name, s, KEY); n.startHost(0); return n; }
    NetNode join(String name, MemStore s, NetNode h) { return join(name, s, h, KEY); }
    NetNode join(String name, MemStore s, NetNode h, String key) { NetNode n = node(name, s, key); n.startJoin("127.0.0.1", h.port()); return n; }

    static void await(String what, BooleanSupplier c) throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) { if (c.getAsBoolean()) return; Thread.sleep(25); }
        fail("timed out waiting for: " + what);
    }

    @Test void whenAStationJoinsEachSideReceivesWhatItLacks() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        hs.log("K1HOST", "20m");
        cs.log("K2CLI", "40m"); cs.log("K3CLI", "15m");
        NetNode h = host("HOST", hs); NetNode c = join("CLI", cs, h);
        await("both logs hold all three", () -> hs.calls().size() == 3 && cs.calls().size() == 3);
        assertEquals(List.of("K1HOST", "K2CLI", "K3CLI"), hs.calls()); assertEquals(hs.calls(), cs.calls());
        assertTrue(c.online()); assertEquals(1, h.connectedCount());
        assertEquals("CLI", hs.byCall("K2CLI").getOrigin(), "the QSO still says which station logged it");
    }

    @Test void aQsoLoggedAfterwardsEditsAndErasesAllReachTheOtherStation() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        NetNode h = host("HOST", hs); join("CLI", cs, h);
        await("joined", () -> h.connectedCount() == 1);

        QsoRecord a = cs.log("K1NEW", "20m");
        await("host received it", () -> hs.byCall("K1NEW") != null);
        QsoRecord b = hs.log("K2FROMHOST", "40m");
        await("client received it", () -> cs.byCall("K2FROMHOST") != null);

        cs.edit(a.getUid(), "edited at the client");
        await("edit reached the host", () -> "edited at the client".equals(hs.byCall("K1NEW").getNotes()));
        hs.erase(b.getUid());
        await("erase reached the client", () -> cs.byCall("K2FROMHOST") == null);
        assertNotNull(cs.metaOf(b.getUid()), "and the client remembers it was erased");
        assertTrue(cs.metaOf(b.getUid()).deleted());
    }

    @Test void aQsoIsAppliedOnceAtEachStationAndNeverEchoedBack() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        NetNode h = host("HOST", hs); join("CLI", cs, h);
        await("joined", () -> h.connectedCount() == 1);
        cs.log("K1ONE", "20m");
        await("host has it", () -> hs.byCall("K1ONE") != null);
        Thread.sleep(400);
        assertEquals(1, hs.applied.get(), "the host applied it once");
        assertEquals(0, cs.applied.get(), "the station that logged it was not sent its own QSO back");
        assertTrue(hs.remoteNotices.get() >= 1, "the host was told to re-work its dupes and score");
    }

    @Test void theHostPassesAQsoOnToTheOtherJoinedStations() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), c1 = new MemStore("C1", "CQ_WW_CW"), c2 = new MemStore("C2", "CQ_WW_CW");
        NetNode h = host("HOST", hs); join("C1", c1, h); join("C2", c2, h);
        await("both joined", () -> h.connectedCount() == 2);
        QsoRecord q = c1.log("K1VIAHOST", "20m");
        await("the other client received it through the host", () -> c2.byCall("K1VIAHOST") != null && hs.byCall("K1VIAHOST") != null);
        c2.erase(c2.byCall("K1VIAHOST").getUid());
        await("an erase travels the same way", () -> c1.byCall("K1VIAHOST") == null && hs.byCall("K1VIAHOST") == null);
        assertEquals(1, c1.applied.get(), "C1 applied only the erase, not its own QSO");
    }

    @Test void aStationThatDoesNotKnowTheKeyIsKeptOut() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("INTRUDER", "CQ_WW_CW");
        hs.log("K1SECRET", "20m"); cs.log("K9BAD", "20m");
        NetNode h = host("HOST", hs); NetNode c = join("INTRUDER", cs, h, "wrong-key!");
        await("it is told why", () -> "wrong key".equals(c.problem()));
        Thread.sleep(300);
        assertFalse(c.online()); assertEquals(0, h.connectedCount());
        assertEquals(List.of("K9BAD"), cs.calls(), "it received nothing");
        assertEquals(List.of("K1SECRET"), hs.calls(), "and the host received nothing from it");
    }

    @Test void twoStationsCannotUseTheSameName() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), c1 = new MemStore("SAME", "CQ_WW_CW"), c2 = new MemStore("SAME", "CQ_WW_CW");
        NetNode h = host("HOST", hs); join("SAME", c1, h);
        await("first joined", () -> h.connectedCount() == 1);
        NetNode second = join("SAME", c2, h);
        await("the second is told", () -> second.problem().contains("already in use"));
        assertEquals(1, h.connectedCount());
        NetNode hostNamed = join("HOST", new MemStore("HOST", "CQ_WW_CW"), h);
        await("a station cannot take the host's name", () -> hostNamed.problem().contains("already in use"));
    }

    @Test void stationsInDifferentContestsShareNothing() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "ARRL_DX_CW");
        hs.log("K1HOST", "20m"); cs.log("K2CLI", "20m");
        NetNode h = host("HOST", hs); join("CLI", cs, h);
        await("connected", () -> h.connectedCount() == 1);
        hs.log("K3LIVE", "20m"); cs.log("K4LIVE", "20m");
        Thread.sleep(500);
        assertEquals(List.of("K1HOST", "K3LIVE"), hs.calls()); assertEquals(List.of("K2CLI", "K4LIVE"), cs.calls());
        await("each sees the other's contest in the roster", () -> !h.roster().isEmpty());
        assertEquals("ARRL_DX_CW", h.roster().get(0).contest());
        assertEquals(1, NetConflicts.warnings(new NetStatus("HOST", "", "CQ_WW_CW", "20m", "CW", 0, 0), h.roster()).size());
    }

    @Test void aStationThatLostTheHostCatchesUpOnReconnecting() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        NetNode h = host("HOST", hs); NetNode c = join("CLI", cs, h);
        await("joined", () -> h.connectedCount() == 1);
        QsoRecord shared = cs.log("K1SHARED", "20m");
        await("shared", () -> hs.byCall("K1SHARED") != null);

        c.stop();                                                                  // the client's link goes down; both keep logging
        await("the host sees it go", () -> h.connectedCount() == 0);
        hs.log("K2WHILEAWAY-HOST", "40m");
        cs.log("K3WHILEAWAY-CLI", "15m");
        cs.edit(shared.getUid(), "edited while away");
        hs.erase(hs.byCall("K2WHILEAWAY-HOST").getUid());                          // the host erases its own new QSO while apart

        c.startJoin("127.0.0.1", h.port());
        await("both logs agree again", () -> hs.calls().equals(cs.calls()) && hs.calls().size() == 2);
        assertEquals(List.of("K1SHARED", "K3WHILEAWAY-CLI"), hs.calls(), "the host's erased QSO is gone at the client, the client's new one is at the host");
        assertEquals("edited while away", hs.byCall("K1SHARED").getNotes());
    }

    @Test void openingTheContestAfterJoiningSharesTheLogOnResync() throws Exception {
        MemStore hs = new MemStore("HOST", null), cs = new MemStore("CLI", null);          // neither has a contest open yet
        hs.seed("K1HOST", "CQ_WW_CW"); cs.seed("K2CLI", "CQ_WW_CW");                       // but both already hold QSOs from earlier
        NetNode h = host("HOST", hs); NetNode c = join("CLI", cs, h);
        await("joined", () -> h.connectedCount() == 1);
        hs.contest = "CQ_WW_CW"; cs.contest = "CQ_WW_CW";                                  // the contest is opened at each station
        Thread.sleep(300);
        assertEquals(List.of("K1HOST"), hs.calls(), "nothing is exchanged until the exchange starts");
        h.resync(); c.resync();
        await("both have both", () -> hs.calls().size() == 2 && cs.calls().size() == 2);
        assertEquals(hs.calls(), cs.calls());
    }

    @Test @org.junit.jupiter.api.Timeout(60) void aSocketCallbackNeverWaitsForTheNodesLock() throws Exception {
        // Regression. stop() holds the node's lock while it closes the socket, and closing needs the socket's own lock; the socket's read thread
        // holds that lock while it calls back into the node. If a callback also took the node's lock, each side waited for the other and
        // changing a network setting while joined hung the app. So: hold the lock exactly as stop() does, have the host drop the connection,
        // and the client's close callback must still complete.
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        NetNode h = host("HOST", hs); NetNode c = join("CLI", cs, h);
        await("joined", () -> c.online());
        boolean noticedWhileLocked;
        synchronized (c) {
            h.stop();
            long end = System.currentTimeMillis() + 3000;
            while (c.online() && System.currentTimeMillis() < end) Thread.sleep(20);
            noticedWhileLocked = !c.online();
        }
        assertTrue(noticedWhileLocked, "the close callback waited for the node's lock");
    }

    @Test void theRosterShowsTheOtherStationsAndWhatTheyAreDoing() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), cs = new MemStore("CLI", "CQ_WW_CW");
        NetNode h = host("HOST", hs); NetNode c = join("CLI", cs, h);
        await("joined", () -> h.connectedCount() == 1);
        c.publishStatus(); h.publishStatus();
        await("each sees the other", () -> h.roster().size() == 1 && c.roster().size() == 1);
        NetStatus seen = h.roster().get(0);
        assertEquals("CLI", seen.station()); assertEquals("OP-CLI", seen.op()); assertEquals("20m", seen.band()); assertEquals("CW", seen.mode());
        assertEquals("HOST", c.roster().get(0).station());
        assertTrue(h.summary().startsWith("Hosting on port ") && h.summary().contains("1 station joined"), h.summary());
        assertTrue(c.summary().startsWith("Joined 127.0.0.1:"), c.summary());
    }

    @Test void aShortKeyIsRefusedAndAnUnreachableHostIsReportedNotThrown() throws Exception {
        MemStore s = new MemStore("HOST", "CQ_WW_CW");
        NetNode weak = node("HOST", s, "abc");
        assertThrows(IllegalArgumentException.class, () -> weak.startHost(0));
        NetNode lost = node("CLI", new MemStore("CLI", "CQ_WW_CW"), KEY);
        lost.startJoin("127.0.0.1", 1);                                              // nothing listens on port 1
        await("it says so", () -> lost.summary().startsWith("Connecting to 127.0.0.1:1"));
        assertFalse(lost.online());
        lost.stop();
        assertEquals(NetNode.Role.OFF, lost.role()); assertEquals("", lost.summary());
    }
}
