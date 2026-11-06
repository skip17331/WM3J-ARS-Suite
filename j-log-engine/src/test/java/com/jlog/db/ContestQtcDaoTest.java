package com.jlog.db;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QtcRecord;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContestQtcDaoTest {
    static final ContestQtcDao DAO = ContestQtcDao.getInstance();
    static final String C = "WAE_CW";

    @BeforeEach void database() throws Exception {
        Path home = Files.createTempDirectory("jlog-qtc-test");
        Files.createDirectories(home.resolve(".j-log"));
        System.setProperty("user.home", home.toString());
        DatabaseManager.getInstance().initAll();
    }

    static QtcRecord row(String dir, int series, String partner, String time, String call, String serial) {
        QtcRecord r = new QtcRecord();
        r.setContestId(C); r.setDirection(dir); r.setSeriesNo(series); r.setSeriesSize(2); r.setPartnerCall(partner);
        r.setQsoTime(time); r.setQsoCall(call); r.setQsoSerial(serial);
        return r;
    }

    @Test void deletingASeriesRemovesOnlyThatSeriesAndItsPoints() throws Exception {
        DAO.insert(row("SENT", 1, "W1AW", "1200", "K1AAA", "5")); DAO.insert(row("SENT", 1, "W1AW", "1201", "K1BBB", "6"));
        DAO.insert(row("SENT", 2, "W1AW", "1210", "K1CCC", "7")); DAO.insert(row("RECEIVED", 1, "VK2X", "1300", "K2DDD", "8"));
        assertEquals(4, DAO.totalQtcPointsByContest(C));
        assertEquals(2, DAO.deleteSeries(C, "SENT", 1));
        assertEquals(2, DAO.totalQtcPointsByContest(C));
        assertEquals(0, DAO.deleteSeries(C, "SENT", 1), "nothing left to delete");
        List<QtcRecord> left = DAO.fetchByContest(C);
        assertTrue(left.stream().anyMatch(r -> r.getSeriesNo() == 2 && r.getDirection().equals("SENT")));
        assertTrue(left.stream().anyMatch(r -> r.getDirection().equals("RECEIVED")), "the other direction's series 1 is untouched");
    }

    @Test void replacingASeriesSwapsTheRowsAtomically() throws Exception {
        DAO.insert(row("SENT", 1, "W1AW", "1200", "K1AAA", "5")); DAO.insert(row("SENT", 1, "W1AW", "1201", "K1BBB", "6"));
        List<QtcRecord> fixed = new ArrayList<>(List.of(row("SENT", 1, "W1AW", "1200", "K1AAA", "15"), row("SENT", 1, "W1AW", "1201", "K1BBB", "16"), row("SENT", 1, "W1AW", "1202", "K1EEE", "17")));
        DAO.replaceSeries(C, "SENT", 1, fixed);
        List<QtcRecord> now = DAO.fetchByContest(C);
        assertEquals(3, now.size());
        assertEquals("15", now.get(0).getQsoSerial());
    }

    @Test void aFailedReplaceLeavesTheOriginal() throws Exception {
        DAO.insert(row("SENT", 1, "W1AW", "1200", "K1AAA", "5"));
        QtcRecord bad = row("SENT", 1, "W1AW", "1200", "K1AAA", "9");
        bad.setContestId(null);                                    // contest_id is NOT NULL: the insert fails
        assertThrows(Exception.class, () -> DAO.replaceSeries(C, "SENT", 1, List.of(bad)));
        List<QtcRecord> now = DAO.fetchByContest(C);
        assertEquals(1, now.size());
        assertEquals("5", now.get(0).getQsoSerial());
    }
}
