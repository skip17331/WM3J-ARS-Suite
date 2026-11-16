package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The log browser reads any contest log file without making it the log the app writes to, and never creates or changes one. */
class ContestLogReaderTest {

    /** A contest database as the engine makes it (just the columns the reader maps). */
    private static Path makeLog(Path dir, String name) throws Exception {
        Path f = dir.resolve(name);
        try (Connection c = Sqlite.open(f, 1000); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE contest_qso (
                    id INTEGER PRIMARY KEY AUTOINCREMENT, contest_id TEXT NOT NULL, callsign TEXT NOT NULL, datetime_utc TEXT NOT NULL,
                    band TEXT, mode TEXT, frequency TEXT, operator TEXT, serial_sent TEXT, serial_received TEXT,
                    field1 TEXT, field2 TEXT, field3 TEXT, field4 TEXT, field5 TEXT, points INTEGER DEFAULT 0, is_dupe INTEGER DEFAULT 0,
                    rst_sent TEXT, rst_received TEXT, notes TEXT)""");
            st.executeUpdate("INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode,frequency,serial_sent,serial_received,field1,points,is_dupe,rst_sent,rst_received)"
                    + " VALUES ('CQWW_SSB','DL1AA','2026-11-28 00:05:00','20m','SSB','14250','1','101','14',3,0,'59','59')");
            st.executeUpdate("INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode,frequency,serial_sent,serial_received,field1,points,is_dupe)"
                    + " VALUES ('CQWW_SSB','K1BB','2026-11-28 00:20:00','40m','SSB','7150','2','102','05',3,1)");
            st.executeUpdate("INSERT INTO contest_qso(contest_id,callsign,datetime_utc,band,mode) VALUES ('ARRL_DX','G3CC','2026-02-21 15:00:00','20m','CW')");
        }
        return f;
    }

    @Test
    void readsOneContestNewestFirst(@TempDir Path dir) throws Exception {
        Path f = makeLog(dir, "nov.db");
        List<QsoRecord> rows = ContestLogReader.read(f, "CQWW_SSB");
        assertEquals(2, rows.size());
        assertEquals("K1BB", rows.get(0).getCallsign(), "newest first");
        assertEquals("DL1AA", rows.get(1).getCallsign());
        QsoRecord a = rows.get(1);
        assertEquals("20m", a.getBand());
        assertEquals("14250", a.getFrequency());
        assertEquals("101", a.getSerialReceived());
        assertEquals("14", a.getContestField1());
        assertEquals(3, a.getPoints());
        assertFalse(a.isDupe());
        assertTrue(rows.get(0).isDupe());
        assertNotNull(a.getDateTimeUtc());
    }

    @Test
    void blankContestIdMeansEverythingInTheFile(@TempDir Path dir) throws Exception {
        Path f = makeLog(dir, "all.db");
        assertEquals(3, ContestLogReader.read(f, null).size());
        assertEquals(3, ContestLogReader.read(f, "  ").size());
        assertEquals(1, ContestLogReader.read(f, "ARRL_DX").size());
        assertEquals(0, ContestLogReader.read(f, "NOPE").size());
    }

    @Test
    void aMissingFileIsAnErrorAndIsNotCreated(@TempDir Path dir) {
        Path missing = dir.resolve("not-there.db");
        assertThrows(java.io.FileNotFoundException.class, () -> ContestLogReader.read(missing, "X"));
        assertFalse(Files.exists(missing), "opening a path with SQLite would create an empty database");
        assertThrows(java.io.FileNotFoundException.class, () -> ContestLogReader.read(null, "X"));
    }

    @Test
    void readingDoesNotChangeTheFile(@TempDir Path dir) throws Exception {
        Path f = makeLog(dir, "keep.db");
        byte[] before = Files.readAllBytes(f);
        ContestLogReader.read(f, "CQWW_SSB");
        ContestLogReader.read(f, null);
        assertArrayEquals(before, Files.readAllBytes(f));
    }

    @Test
    void aFileThatIsNotAContestLogIsAnError(@TempDir Path dir) throws Exception {
        Path f = dir.resolve("other.db");
        try (Connection c = Sqlite.open(f, 1000); Statement st = c.createStatement()) { st.executeUpdate("CREATE TABLE something(x)"); }
        assertThrows(java.sql.SQLException.class, () -> ContestLogReader.read(f, null));
    }
}
