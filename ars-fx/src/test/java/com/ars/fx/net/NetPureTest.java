package com.ars.fx.net;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.db.ContestQsoDao.Meta;
import com.jlog.model.QsoRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class NetPureTest {
    // ---- auth ----
    @Test void aProofIsRightOnlyForTheSameKeyNonceAndStation() {
        String n = NetAuth.nonce();
        String p = NetAuth.proof("secret1", n, "STN-B");
        assertTrue(NetAuth.verify("secret1", n, "STN-B", p));
        assertFalse(NetAuth.verify("secret2", n, "STN-B", p), "another key");
        assertFalse(NetAuth.verify("secret1", NetAuth.nonce(), "STN-B", p), "a recorded proof cannot be replayed on a new connection");
        assertFalse(NetAuth.verify("secret1", n, "STN-C", p), "another station name");
        assertFalse(NetAuth.verify("secret1", n, "STN-B", null)); assertFalse(NetAuth.verify(null, n, "STN-B", p)); assertFalse(NetAuth.verify("abc", n, "STN-B", p), "a key under 4 characters is refused");
        assertFalse(p.contains("secret1"), "the key is not in the proof");
    }
    @Test void noncesAreFreshAndHex() {
        String a = NetAuth.nonce(), b = NetAuth.nonce();
        assertTrue(a.matches("[0-9a-f]{32}")); assertNotEquals(a, b);
        assertTrue(NetAuth.keyOk("abcd")); assertFalse(NetAuth.keyOk("abc")); assertFalse(NetAuth.keyOk("   ")); assertFalse(NetAuth.keyOk(null));
    }

    // ---- merge ----
    static Meta live(String uid, long ts, String origin) { return new Meta(uid, ts, origin, false); }
    static Meta gone(String uid, long ts) { return new Meta(uid, ts, "", true); }

    @Test void theLaterChangeWins() {
        assertTrue(NetMerge.incomingWins(null, new NetMerge.Version(5, false, "A")), "a QSO we do not have");
        assertTrue(NetMerge.incomingWins(new NetMerge.Version(5, false, "A"), new NetMerge.Version(6, false, "B")));
        assertFalse(NetMerge.incomingWins(new NetMerge.Version(6, false, "A"), new NetMerge.Version(5, false, "B")), "a stale copy never overwrites");
        assertFalse(NetMerge.incomingWins(new NetMerge.Version(5, false, "A"), new NetMerge.Version(5, false, "A")), "the same version is not applied twice");
    }
    @Test void aDeletionIsAChangeLikeAnyOtherAndWinsATie() {
        assertTrue(NetMerge.incomingWins(new NetMerge.Version(5, false, "A"), new NetMerge.Version(6, true, "")), "an erase after the edit");
        assertFalse(NetMerge.incomingWins(new NetMerge.Version(7, true, ""), new NetMerge.Version(6, false, "B")), "an edit older than the erase does not bring it back");
        assertTrue(NetMerge.incomingWins(new NetMerge.Version(5, false, "A"), new NetMerge.Version(5, true, "")), "at the same instant the deletion wins");
        assertTrue(NetMerge.incomingWins(new NetMerge.Version(8, true, ""), new NetMerge.Version(9, false, "B")), "a later edit does bring a QSO back");
        assertTrue(NetMerge.compare(new NetMerge.Version(5, false, "B"), new NetMerge.Version(5, false, "A")) > 0, "equal times: station name decides, the same everywhere");
    }
    @Test void toSendIsWhatTheOtherLacksOrHoldsAnOlderVersionOf() {
        List<Meta> mine = List.of(live("a", 10, "X"), live("b", 10, "X"), live("c", 10, "X"), gone("d", 10), live("e", 10, "X"));
        List<Meta> theirs = List.of(live("a", 10, "X"), live("b", 5, "X"), live("c", 20, "X"), live("d", 5, "X"));
        List<String> out = NetMerge.toSend(mine, theirs).stream().map(Meta::uid).toList();
        assertEquals(List.of("b", "d", "e"), out, "b: theirs is older; c: theirs is newer, nothing to send; d: our deletion is newer; e: they lack it; a: the same");
    }

    // ---- protocol ----
    @Test void aQsoSurvivesTheWireWithEveryFieldAndNothingElse() {
        QsoRecord q = new QsoRecord(); q.setUid("u1"); q.setContestId("CQ_WW_CW"); q.setCallsign("K1ABC"); q.setDateTimeUtc(LocalDateTime.of(2026, 11, 5, 16, 43, 38));
        q.setBand("20m"); q.setMode("CW"); q.setFrequency("14.025"); q.setOperator("K1TTT"); q.setSerialSent("12"); q.setSerialReceived("34");
        q.setContestField1("5"); q.setContestField3("WI"); q.setRstSent("599"); q.setRstReceived("579"); q.setNotes("Tom & \"Jerry\" <ok>"); q.setOrigin("STN-A"); q.setUpdatedMs(1234567890123L);
        q.setPoints(3); q.setDupe(true);
        String wire = NetProto.qso(q);
        assertFalse(wire.contains("points") || wire.contains("dupe"), "points and dupe flags are worked out by each station");
        QsoRecord b = NetProto.qsoFrom(NetProto.parse(wire));
        assertEquals("u1", b.getUid()); assertEquals("CQ_WW_CW", b.getContestId()); assertEquals("K1ABC", b.getCallsign());
        assertEquals(q.getDateTimeUtc(), b.getDateTimeUtc()); assertEquals("20m", b.getBand()); assertEquals("CW", b.getMode()); assertEquals("14.025", b.getFrequency());
        assertEquals("K1TTT", b.getOperator()); assertEquals("12", b.getSerialSent()); assertEquals("34", b.getSerialReceived());
        assertEquals("5", b.getContestField1()); assertNull(b.getContestField2()); assertEquals("WI", b.getContestField3());
        assertEquals("599", b.getRstSent()); assertEquals("579", b.getRstReceived()); assertEquals("Tom & \"Jerry\" <ok>", b.getNotes());
        assertEquals("STN-A", b.getOrigin()); assertEquals(1234567890123L, b.getUpdatedMs());
        assertEquals(0, b.getPoints()); assertFalse(b.isDupe());
    }
    @Test void haveListsRoundTripWithDeletions() {
        List<Meta> in = List.of(live("a", 1, "X"), gone("b", 2));
        List<Meta> out = NetProto.metasFrom(NetProto.parse(NetProto.have("CQ_WW_CW", in)));
        assertEquals(in, out);
        assertEquals("CQ_WW_CW", NetProto.str(NetProto.parse(NetProto.have("CQ_WW_CW", in)), "contest"));
    }
    @Test void aStatusRoundTripsAndIsStampedOnArrival() {
        NetStatus s = NetProto.statusFrom(NetProto.parse(NetProto.status(new NetStatus("STN-B", "K1ABC", "CQ_WW_CW", "20m", "CW", 14_025_000L, 0))));
        assertEquals("STN-B", s.station()); assertEquals("K1ABC", s.op()); assertEquals("CQ_WW_CW", s.contest()); assertEquals("20m", s.band()); assertEquals(14_025_000L, s.freqHz());
        assertTrue(System.currentTimeMillis() - s.seenMs() < 2000);
        assertEquals("STN-B (K1ABC)", s.label());
    }
    @Test void unreadableOrIncompleteMessagesAreIgnoredNotThrown() {
        assertNull(NetProto.parse("not json")); assertNull(NetProto.parse("[1,2]")); assertNull(NetProto.parse(""));
        assertNull(NetProto.qsoFrom(NetProto.parse("{\"t\":\"qso\",\"uid\":\"x\"}")), "a QSO without a call or time");
        assertNull(NetProto.qsoFrom(NetProto.parse("{\"t\":\"qso\",\"uid\":\"x\",\"c\":\"C\",\"call\":\"K1A\",\"dt\":\"yesterday\"}")));
        assertTrue(NetProto.metasFrom(NetProto.parse("{\"t\":\"have\",\"items\":\"no\"}")).isEmpty());
        assertNull(NetProto.statusFrom(NetProto.parse("{\"t\":\"status\"}")));
    }

    // ---- conflicts ----
    @Test void conflictsAreAnotherContestOrTheSameBandAndMode() {
        NetStatus me = new NetStatus("STN-A", "K1ABC", "CQ_WW_CW", "20m", "CW", 0, 0);
        NetStatus same = new NetStatus("STN-B", "K2DEF", "CQ_WW_CW", "20m", "CW", 0, 0), other = new NetStatus("STN-C", "", "CQ_WW_CW", "40m", "CW", 0, 0),
                otherMode = new NetStatus("STN-D", "", "CQ_WW_CW", "20m", "SSB", 0, 0), otherContest = new NetStatus("STN-E", "K5X", "ARRL_DX_CW", "20m", "CW", 0, 0);
        List<String> w = NetConflicts.warnings(me, List.of(same, other, otherMode, otherContest, me));
        assertEquals(2, w.size(), w.toString());
        assertTrue(w.get(0).contains("STN-B (K2DEF) is also on 20m CW"));
        assertTrue(w.get(1).contains("STN-E") && w.get(1).contains("different contest") && w.get(1).contains("ARRL_DX_CW"));
        assertTrue(NetConflicts.warnings(null, List.of(same)).isEmpty());
        assertTrue(NetConflicts.warnings(new NetStatus("STN-A", "", "CQ_WW_CW", "", "", 0, 0), List.of(same)).isEmpty(), "no rig band: nothing to conflict");
    }

    @Test void aStatusCarriesTheSendersClockSoTheSkewCanBeSeen() {
        NetStatus sent = new NetStatus("STN-B", "K1ABC", "CQ_WW_CW", "20m", "CW", 0, 0);
        NetStatus back = NetProto.statusFrom(NetProto.parse(NetProto.status(sent)));
        assertEquals("status", NetProto.type(NetProto.parse(NetProto.status(sent))), "the clock must not clobber the message type");
        assertTrue(Math.abs(back.skewMs()) < 2_000, "same machine: no skew, got " + back.skewMs());
        com.google.gson.JsonObject old = NetProto.parse(NetProto.status(sent));
        old.remove("clock");
        assertEquals(0, NetProto.statusFrom(old).skewMs(), "an older build sends no clock: no skew is claimed");
    }

    @Test void aClockWellOffIsWarnedAboutAndALittleIsNot() {
        NetStatus me = new NetStatus("STN-A", "", "CQ_WW_CW", "", "", 0, 0);
        NetStatus fast = new NetStatus("STN-B", "K2DEF", "CQ_WW_CW", "", "", 0, 0, 45_000), slow = new NetStatus("STN-C", "", "CQ_WW_CW", "", "", 0, 0, -400_000),
                near = new NetStatus("STN-D", "", "CQ_WW_CW", "", "", 0, 0, 900);
        List<String> w = NetConflicts.warnings(me, List.of(fast, slow, near));
        assertEquals(2, w.size(), w.toString());
        assertTrue(w.get(0).contains("45 seconds ahead of"), w.get(0));
        assertTrue(w.get(1).contains("6 minutes behind"), w.get(1));
    }
}
