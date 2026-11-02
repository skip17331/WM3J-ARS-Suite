package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteTest {

    @TempDir Path dir;

    private static String stderrOf(Runnable r) {
        PrintStream old = System.err;
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        System.setErr(new PrintStream(buf, true, StandardCharsets.UTF_8));
        try { r.run(); } finally { System.setErr(old); }
        return buf.toString(StandardCharsets.UTF_8);
    }

    /** '?' is illegal in a Windows file name; NUL is illegal everywhere, so this case also bites on Linux and macOS. */
    @Test
    void strayNameTheFilesystemRejectsIsSilentlyNothingToAdopt() {
        Path real = dir.resolve("j-log.db");
        for (String stray : new String[]{"?busyTimeout=5000", "\0bad"}) {
            assertEquals("", stderrOf(() -> Sqlite.adoptLegacy(real, stray, "qso")), "stray name: " + stray);
        }
        assertFalse(Files.exists(real));
    }

    @Test
    void strayFileWithRowsStillBecomesTheRealFile() throws Exception {
        Path real = dir.resolve("j-log.db");
        Path stray = dir.resolve("j-log.db.stray");
        try (Connection c = Sqlite.open(stray, 1000); Statement st = c.createStatement()) {
            st.execute("CREATE TABLE qso (id INTEGER)");
            st.execute("INSERT INTO qso VALUES (1)");
        }

        Sqlite.adoptLegacy(real, ".stray", "qso");

        assertFalse(Files.exists(stray));
        assertTrue(Files.isRegularFile(real));
        try (Connection c = Sqlite.open(real, 1000); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM qso")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getLong(1));
        }
    }
}
