package com.ars.fx.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The one-time repair of QSOs saved with the frequency in kHz (or a decimal comma) before #252. */
class FreqRepairTest {

    private String oldHome;

    @BeforeEach void home(@TempDir Path tmp) {
        oldHome = System.getProperty("user.home");
        System.setProperty("user.home", tmp.toString());
        HubConfig.reload();
        assertTrue(JLogDb.available());          // creates the empty log
    }

    @AfterEach void restore() { System.setProperty("user.home", oldHome); HubConfig.reload(); }

    private static Connection db() throws Exception { return DriverManager.getConnection("jdbc:sqlite:" + JLogDb.path()); }

    /** Rows written as an older build stored them, bypassing today's normalising insert. */
    private static void raw(String call, String freq, String band) throws Exception {
        try (Connection c = db(); PreparedStatement ps = c.prepareStatement(
                "INSERT INTO qso(callsign, datetime_utc, frequency, band, mode) VALUES (?, '2026-01-02 15:30:00', ?, ?, 'CW')")) {
            ps.setString(1, call); ps.setString(2, freq); ps.setString(3, band); ps.executeUpdate();
        }
    }

    private static Map<String, String> rows() throws Exception {
        Map<String, String> m = new LinkedHashMap<>();
        try (Connection c = db(); ResultSet rs = c.createStatement().executeQuery("SELECT callsign, frequency, band FROM qso ORDER BY id")) {
            while (rs.next()) m.put(rs.getString(1), rs.getString(2) + "|" + rs.getString(3));
        }
        return m;
    }

    @Test void kilohertzAndCommaEntriesGetMegahertzAndABand() throws Exception {
        raw("K1KHZ", "14025", "");
        raw("DL1COMMA", "7,074", null);
        raw("G4SPOT", "14.025.0", "");
        raw("W1MHZ", "14.0740", "20m");          // already right: left exactly as it is
        raw("W2NOBAND", "21.074", "");           // MHz but no band: only the band is filled
        raw("W3NOFREQ", "", "40m");              // no frequency: untouched
        raw("W4ODD", "99999", "");               // not a frequency in any band: untouched

        JLogDb.FreqRepair r = JLogDb.repairFrequencies();
        assertEquals(3, r.frequencies());
        assertEquals(4, r.bands());

        Map<String, String> m = rows();
        assertEquals("14.025|20m", m.get("K1KHZ"));
        assertEquals("7.074|40m", m.get("DL1COMMA"));
        assertEquals("14.025|20m", m.get("G4SPOT"));
        assertEquals("14.0740|20m", m.get("W1MHZ"));
        assertEquals("21.074|15m", m.get("W2NOBAND"));
        assertEquals("|40m", m.get("W3NOFREQ"));
        assertEquals("99999|", m.get("W4ODD"));
        assertTrue(Files.exists(JLogDb.path().resolveSibling("j-log.db.bak-before-freq-repair")), "a copy is kept first");
    }

    @Test void aSecondRunChangesNothing() throws Exception {
        raw("K1KHZ", "14025", "");
        JLogDb.repairFrequencies();
        JLogDb.FreqRepair again = JLogDb.repairFrequencies();
        assertEquals(0, again.frequencies() + again.bands());
    }

    @Test void aCleanLogIsNotCopied() throws Exception {
        raw("W1MHZ", "14.074", "20m");
        JLogDb.FreqRepair r = JLogDb.repairFrequencies();
        assertEquals(0, r.frequencies() + r.bands());
        assertFalse(Files.exists(JLogDb.path().resolveSibling("j-log.db.bak-before-freq-repair")));
    }
}
