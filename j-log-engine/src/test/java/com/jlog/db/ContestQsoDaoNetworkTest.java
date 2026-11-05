package com.jlog.db;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QsoRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * The contest log's network-mode data layer: identity, versions, tombstones, quiet replica writes. One database for the whole class, started
 * from an old-format file (no uid / origin / change time) the way an upgraded installation would find it.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContestQsoDaoNetworkTest {
    static final ContestQsoDao DAO = ContestQsoDao.getInstance();
    static final List<ContestQsoDao.Change> events = new ArrayList<>();

    @BeforeAll static void database() throws Exception {
        Path home = Files.createTempDirectory("jlog-net-test");
        Path dir = Files.createDirectories(home.resolve(".j-log"));
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("contest.db")); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE contest_qso (id INTEGER PRIMARY KEY AUTOINCREMENT, contest_id TEXT NOT NULL, callsign TEXT NOT NULL, datetime_utc TEXT NOT NULL,
                  band TEXT, mode TEXT, frequency TEXT, operator TEXT, serial_sent TEXT, serial_received TEXT, field1 TEXT, field2 TEXT, field3 TEXT, field4 TEXT, field5 TEXT,
                  points INTEGER DEFAULT 0, is_dupe INTEGER DEFAULT 0, rst_sent TEXT, rst_received TEXT, notes TEXT, created_at TEXT DEFAULT (datetime('now')))
                """);
            st.executeUpdate("INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode,points) VALUES('CQ_WW_CW','K1OLD','2026-11-01 12:00:00','20m','CW',3)");
        }
        System.setProperty("user.home", home.toString());
        DatabaseManager.getInstance().initAll();
        DAO.setStationId("STN-A");
        DAO.addListener(events::add);
    }

    static QsoRecord qso(String call, String band) {
        QsoRecord q = new QsoRecord(); q.setContestId("CQ_WW_CW"); q.setCallsign(call); q.setBand(band); q.setMode("CW");
        q.setDateTimeUtc(LocalDateTime.of(2026, 11, 5, 16, 0)); q.setRstSent("599"); q.setRstReceived("599"); q.setPoints(3);
        return q;
    }

    @Test @Order(1) void anOldDatabaseIsUpgradedAndItsQsosGetIdentities() throws Exception {
        List<QsoRecord> all = DAO.fetchByContest("CQ_WW_CW");
        assertEquals(1, all.size());
        QsoRecord old = all.get(0);
        assertEquals("K1OLD", old.getCallsign());
        assertNotNull(old.getUid()); assertEquals(32, old.getUid().length(), "a random 32-hex id was filled in");
        assertEquals("", old.getOrigin());
        assertEquals(LocalDateTime.of(2026, 11, 1, 12, 0).toEpochSecond(java.time.ZoneOffset.UTC) * 1000, old.getUpdatedMs(), "its version is its logged time");
        assertEquals(3, old.getPoints(), "nothing else was touched");
    }

    @Test @Order(2) void aNewQsoGetsAUidTheStationNameAndATimeAndTellsTheListeners() throws Exception {
        events.clear();
        QsoRecord q = qso("K2NEW", "20m");
        long before = System.currentTimeMillis();
        DAO.insert(q);
        assertNotNull(q.getUid()); assertEquals("STN-A", q.getOrigin()); assertTrue(q.getUpdatedMs() >= before);
        assertEquals(1, events.size());
        assertEquals(ContestQsoDao.Change.Kind.ADD, events.get(0).kind()); assertEquals(q.getUid(), events.get(0).uid());
        QsoRecord back = DAO.findByUid(q.getUid());
        assertEquals("K2NEW", back.getCallsign()); assertEquals(q.getId(), back.getId()); assertEquals("STN-A", back.getOrigin());
    }

    @Test @Order(3) void anEditStampsANewVersionButDerivedValuesDoNot() throws Exception {
        QsoRecord q = qso("K3EDIT", "40m"); DAO.insert(q);
        long v1 = DAO.findByUid(q.getUid()).getUpdatedMs();
        Thread.sleep(5);
        events.clear();
        q.setBand("80m"); DAO.update(q);
        QsoRecord after = DAO.findByUid(q.getUid());
        assertEquals("80m", after.getBand()); assertTrue(after.getUpdatedMs() > v1);
        assertEquals(ContestQsoDao.Change.Kind.EDIT, events.get(0).kind());
        long v2 = after.getUpdatedMs();
        events.clear();
        DAO.updateDerived(q.getId(), 9, true);
        QsoRecord derived = DAO.findByUid(q.getUid());
        assertEquals(9, derived.getPoints()); assertTrue(derived.isDupe());
        assertEquals(v2, derived.getUpdatedMs(), "re-working the dupe flag is not an edit");
        assertTrue(events.isEmpty(), "and it is not sent to the other stations");
    }

    @Test @Order(4) void anEraseLeavesATombstoneAndTellsTheListeners() throws Exception {
        QsoRecord q = qso("K4GONE", "15m"); DAO.insert(q);
        events.clear();
        DAO.delete(q.getId());
        assertNull(DAO.findByUid(q.getUid()));
        assertEquals(ContestQsoDao.Change.Kind.DELETE, events.get(0).kind()); assertEquals(q.getUid(), events.get(0).uid());
        ContestQsoDao.Meta m = DAO.metaOf(q.getUid());
        assertTrue(m.deleted()); assertTrue(m.ts() > 0);
        Map<String, ContestQsoDao.Meta> inv = DAO.inventory("CQ_WW_CW").stream().collect(Collectors.toMap(ContestQsoDao.Meta::uid, x -> x));
        assertTrue(inv.get(q.getUid()).deleted(), "the inventory carries deletions, so they reach the other stations");
        assertNull(DAO.metaOf("never-heard-of-it"));
    }

    @Test @Order(5) void aReplicaIsStoredAsItIsWithoutTellingAnyone() throws Exception {
        events.clear();
        QsoRecord r = qso("DL1REP", "20m"); r.setUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); r.setOrigin("STN-B"); r.setUpdatedMs(1_700_000_000_000L); r.setPoints(77);
        DAO.applyReplica(r);
        QsoRecord back = DAO.findByUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertEquals("DL1REP", back.getCallsign()); assertEquals("STN-B", back.getOrigin()); assertEquals(1_700_000_000_000L, back.getUpdatedMs());
        assertEquals(0, back.getPoints(), "points are worked out here, not taken from the sender");
        assertTrue(events.isEmpty(), "a replica is never echoed back");

        back.setUpdatedMs(1_700_000_005_000L); back.setNotes("edited at B"); back.setPoints(55);
        DAO.applyReplica(back);
        QsoRecord again = DAO.findByUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertEquals("edited at B", again.getNotes()); assertEquals(1_700_000_005_000L, again.getUpdatedMs());
        assertEquals(back.getId(), again.getId(), "an update replaces the row, it does not add another");
        assertEquals(1, DAO.fetchByContest("CQ_WW_CW").stream().filter(x -> "DL1REP".equals(x.getCallsign())).count());
        assertTrue(events.isEmpty());
    }

    @Test @Order(6) void aReplicaDeleteRemovesTheQsoRemembersItAndALaterVersionBringsItBack() throws Exception {
        events.clear();
        DAO.applyReplicaDelete("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "CQ_WW_CW", 1_700_000_010_000L);
        assertNull(DAO.findByUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        ContestQsoDao.Meta m = DAO.metaOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        assertTrue(m.deleted()); assertEquals(1_700_000_010_000L, m.ts());
        assertTrue(events.isEmpty());
        QsoRecord back = qso("DL1REP", "20m"); back.setUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"); back.setOrigin("STN-B"); back.setUpdatedMs(1_700_000_020_000L);
        DAO.applyReplica(back);
        assertNotNull(DAO.findByUid("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
        assertFalse(DAO.metaOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").deleted(), "a live version clears the tombstone");
    }

    @Test @Order(7) void theInventoryListsEveryQsoOfTheContestAndNothingFromAnother() throws Exception {
        QsoRecord other = qso("K9OTHER", "20m"); other.setContestId("ARRL_DX_CW"); DAO.insert(other);
        List<ContestQsoDao.Meta> inv = DAO.inventory("CQ_WW_CW");
        assertTrue(inv.stream().noneMatch(x -> x.uid().equals(other.getUid())));
        String firstUid = DAO.fetchByContest("CQ_WW_CW").get(0).getUid();
        assertTrue(inv.stream().anyMatch(x -> !x.deleted() && x.uid().equals(firstUid)));
        assertEquals(1, DAO.inventory("ARRL_DX_CW").size());
    }
}
