package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QsoRecord;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Run status, the run-conflict warning, per-operator figures and chat for the contest network. */
class NetFollowUpTest {
    static final String KEY = "contest-key";
    final List<NetNode> nodes = new ArrayList<>();

    @AfterEach void stopAll() { for (NetNode n : nodes) n.stop(); }

    NetNode node(String name, MemStore s) {
        NetNode n = new NetNode(name, "OP-" + name, KEY, s, () -> new NetStatus(name, "OP-" + name, s.contest(), "20m", "CW", 14_025_000L, 0));
        nodes.add(n);
        return n;
    }
    static void await(String what, BooleanSupplier c) throws Exception {
        long end = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < end) { if (c.getAsBoolean()) return; Thread.sleep(25); }
        fail("timed out waiting for: " + what);
    }

    // ---- run status -------------------------------------------------------------------------------------------
    @Test void theRunStateTravelsInTheStatusMessage() {
        NetStatus running = new NetStatus("STN-A", "K1ABC", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0, 0, true);
        assertTrue(NetProto.statusFrom(NetProto.parse(NetProto.status(running))).run());
        NetStatus hunting = new NetStatus("STN-A", "K1ABC", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0);
        assertFalse(NetProto.statusFrom(NetProto.parse(NetProto.status(hunting))).run(), "the older constructors mean search and pounce");
        assertFalse(NetProto.statusFrom(NetProto.parse("{\"t\":\"status\",\"station\":\"OLD\",\"clock\":1}")).run(), "an older build sends no run field");
    }

    @Test void twoStationsRunningOnTheSameBandAreWarnedAboutOnce() {
        NetStatus me = new NetStatus("STN-A", "K1ABC", "CQ_WW_CW", "20m", "CW", 0, 0, 0, true);
        NetStatus runner = new NetStatus("STN-B", "K2DEF", "CQ_WW_CW", "20m", "SSB", 0, 0, 0, true);
        List<String> w = NetConflicts.warnings(me, List.of(runner));
        assertEquals(1, w.size(), w.toString());
        assertTrue(w.get(0).contains("both running on 20m"), w.get(0));
        NetStatus hunter = new NetStatus("STN-C", "K3GHI", "CQ_WW_CW", "20m", "CW", 0, 0, 0, false);
        assertTrue(NetConflicts.warnings(me, List.of(hunter)).get(0).contains("is also on 20m CW"), "one running and one hunting is the ordinary same-band warning");
        NetStatus other = new NetStatus("STN-D", "K4JKL", "CQ_WW_CW", "40m", "CW", 0, 0, 0, true);
        assertTrue(NetConflicts.warnings(me, List.of(other)).isEmpty(), "running on different bands is how a multi-multi works");
    }

    // ---- operators ------------------------------------------------------------------------------------------
    static QsoRecord q(String op, int pts, boolean dupe, LocalDateTime t) {
        QsoRecord r = new QsoRecord();
        r.setOperator(op); r.setPoints(pts); r.setDupe(dupe); r.setDateTimeUtc(t); r.setCallsign("K1ABC");
        return r;
    }

    @Test void eachOperatorHasQsosPointsAndALastHourRate() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 6, 15, 0);
        List<QsoRecord> qs = List.of(q("w1aw", 2, false, now.minusMinutes(10)), q("W1AW", 2, false, now.minusMinutes(90)), q("W1AW", 2, true, now.minusMinutes(5)),
                q("N1ABC", 3, false, now.minusMinutes(30)), q("N1ABC", 3, false, now.minusMinutes(20)), q("N1ABC", 3, false, now.minusMinutes(3)), q(null, 1, false, now.minusMinutes(1)));
        List<OperatorStats.Row> rows = OperatorStats.compute(qs, now);
        assertEquals(3, rows.size());
        assertEquals(new OperatorStats.Row("N1ABC", 3, 9, 3), rows.get(0), "most QSOs first");
        assertEquals(new OperatorStats.Row("W1AW", 2, 4, 1), rows.get(1), "case is ignored, dupes are not counted, only the last hour is in the rate");
        assertEquals("?", rows.get(2).operator(), "a QSO with no operator is not lost");
        assertEquals("N1ABC 3 (3/h)   ·   W1AW 2 (1/h)   ·   ? 1 (1/h)", OperatorStats.line(rows));
    }

    @Test void oneOperatorSaysNothingExtra() {
        assertEquals("", OperatorStats.line(OperatorStats.compute(List.of(q("W1AW", 1, false, LocalDateTime.now())))));
        assertEquals("", OperatorStats.line(List.of()));
    }

    // ---- chat -----------------------------------------------------------------------------------------------
    @Test void aMessageReachesEveryStationOnceAndTheSenderKeepsIt() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW"), as = new MemStore("AAA", "CQ_WW_CW"), bs = new MemStore("BBB", "CQ_WW_CW");
        NetNode h = node("HOST", hs); h.startHost(0);
        NetNode a = node("AAA", as); a.startJoin("127.0.0.1", h.port());
        NetNode b = node("BBB", bs); b.startJoin("127.0.0.1", h.port());
        await("both joined", () -> h.connectedCount() == 2);

        a.sendChat("  need a mult on 15m  ");
        await("host and the other client heard it", () -> h.chatHistory().size() == 1 && b.chatHistory().size() == 1);
        Thread.sleep(300);
        assertEquals(1, h.chatHistory().size());
        assertEquals(1, b.chatHistory().size(), "relayed once, not repeated");
        assertEquals(1, a.chatHistory().size(), "the sender has its own line and is not sent it back");
        assertEquals("AAA", b.chatHistory().get(0).station());
        assertEquals("need a mult on 15m", b.chatHistory().get(0).text(), "trimmed");

        h.sendChat("QSY to 40m at 1800");
        await("both clients heard the host", () -> a.chatHistory().size() == 2 && b.chatHistory().size() == 2);
        assertEquals("HOST", a.chatHistory().get(1).station());
    }

    @Test void emptyMessagesAreIgnoredAndTheHistoryAndLengthAreBounded() throws Exception {
        MemStore hs = new MemStore("HOST", "CQ_WW_CW");
        NetNode h = node("HOST", hs); h.startHost(0);
        h.sendChat("   "); h.sendChat(null);
        assertTrue(h.chatHistory().isEmpty());
        for (int i = 0; i < 120; i++) h.sendChat("m" + i);
        assertEquals(100, h.chatHistory().size());
        assertEquals("m20", h.chatHistory().get(0).text(), "the oldest are dropped");
        h.sendChat("x".repeat(900));
        assertEquals(500, h.chatHistory().get(99).text().length());
        NetNode off = node("OFF", new MemStore("OFF", "CQ_WW_CW"));
        off.sendChat("hello");
        assertTrue(off.chatHistory().isEmpty(), "a station not on the network has nobody to tell");
    }
}
