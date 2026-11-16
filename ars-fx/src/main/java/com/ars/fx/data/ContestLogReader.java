package com.ars.fx.data;

import com.jlog.db.ContestQsoDao;
import com.jlog.model.QsoRecord;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * Reads the QSOs of any contest log file for the log browser, without making it the log the app is writing to. Read-only: the file must
 * already exist (opening a missing path would make SQLite create an empty one) and the connection is switched to query-only.
 */
public final class ContestLogReader {
    private ContestLogReader() {}

    /** Every QSO of {@code contestId} in {@code file} (all contests in it when null or blank), newest first. */
    public static List<QsoRecord> read(Path file, String contestId) throws Exception {
        if (file == null || !Files.isRegularFile(file)) throw new java.io.FileNotFoundException("no such contest log: " + file);
        try (Connection c = Sqlite.open(file, 3000)) {
            try (Statement st = c.createStatement()) { st.execute("PRAGMA query_only=ON"); }
            return ContestQsoDao.readAll(c, contestId);
        }
    }
}
