package com.ars.fx.data;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Opens SQLite files for the ars-fx data classes.
 *
 * <p>This driver takes everything after {@code jdbc:sqlite:} as the file name, so a URL such as
 * {@code …/j-log.db?busyTimeout=5000} silently creates and uses a file literally named
 * {@code j-log.db?busyTimeout=5000}. The busy timeout is therefore set with a PRAGMA after connecting.
 */
public final class Sqlite {
    private Sqlite() {}

    static { try { Class.forName("org.sqlite.JDBC"); } catch (Throwable ignored) {} }

    public static Connection open(Path file, int busyMs) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement st = c.createStatement()) { st.execute("PRAGMA busy_timeout=" + busyMs); }
        return c;
    }

    /**
     * Earlier builds opened {@code <file>?busyTimeout=…} (see above), so data may sit in a wrongly named file.
     * If that file holds rows and the real file holds none, the stray file becomes the real one. If both hold
     * rows nothing is touched (and a note is printed) — merging is left to the operator.
     */
    public static void adoptLegacy(Path real, String strayName, String... dataTables) {
        try {
            Path stray;
            try { stray = real.resolveSibling(real.getFileName() + strayName); }
            catch (InvalidPathException illegalHere) { return; }                       // e.g. '?' on Windows: no such file can exist, so nothing to adopt
            if (!Files.isRegularFile(stray) || Files.size(stray) == 0 || rows(stray, dataTables) == 0) return;
            if (Files.isRegularFile(real) && rows(real, dataTables) > 0) {
                System.err.println("[db] " + stray + " holds data but so does " + real + " — left as is; merge by hand.");
                return;
            }
            for (String s : new String[]{"", "-wal", "-shm"}) {                       // the stray file's connections are closed by now
                Path from = real.resolveSibling(real.getFileName() + strayName + s), to = real.resolveSibling(real.getFileName() + s);
                if (s.isEmpty()) { Files.deleteIfExists(to); Files.deleteIfExists(real.resolveSibling(real.getFileName() + "-wal")); Files.deleteIfExists(real.resolveSibling(real.getFileName() + "-shm")); }
                if (Files.exists(from)) Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
            System.err.println("[db] adopted data from " + stray + " into " + real);
        } catch (Exception e) { System.err.println("[db] could not adopt legacy file for " + real + ": " + e.getMessage()); }
    }

    private static long rows(Path file, String... tables) {
        long n = 0;
        try (Connection c = open(file, 3000); Statement st = c.createStatement()) {
            for (String t : tables) {
                try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + t)) { if (rs.next()) n += rs.getLong(1); }
                catch (Exception noTable) { /* table not created in that file */ }
            }
        } catch (Exception ignored) {}
        return n;
    }
}
