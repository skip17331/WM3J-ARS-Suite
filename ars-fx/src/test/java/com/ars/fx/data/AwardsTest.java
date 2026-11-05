package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AwardsTest {
    private static JLogDb.QsoExport q(String call, String dt, String band, String mode, String state, String grid, JLogDb.Refs r) {
        return new JLogDb.QsoExport(call, dt, band, mode, "14.2", 0, "59", "59", null, null, state, null, null, grid, r);
    }
    private static JLogDb.Refs conf() { return new JLogDb.Refs("", "", "", 0, 0, false, true, false); }

    @Test void dxccCountsEntitiesAndConfirmationsPerBandAndMode() {
        Awards.Snapshot s = Awards.compute(List.of(
                q("W1AW", "2026-01-02 15:30:00", "20m", "SSB", "CT", "FN31pr", JLogDb.Refs.NONE),
                q("W1AW", "2026-01-03 15:30:00", "40m", "CW", "CT", "FN31pr", conf()),
                q("DL1ABC", "2026-01-04 10:00:00", "20m", "FT8", "", "JO62", JLogDb.Refs.NONE),
                q("DL1ABC", "2026-01-05 10:00:00", "20m", "FT8", "", "JO62", JLogDb.Refs.NONE)));
        assertEquals(2, s.dxcc.worked());
        assertEquals(1, s.dxcc.confirmed());
        assertEquals(2, s.dxccByBand.get("20m")[0]);
        assertEquals(0, s.dxccByBand.get("20m")[1]);     // the 20m QSOs are not the confirmed ones
        assertEquals(1, s.dxccByBand.get("40m")[1]);
        assertEquals(1, s.dxccByMode.get("CW")[1]);
        assertEquals(1, s.dxccByMode.get("Digital")[0]);
        assertEquals(2, s.wac.worked());                  // NA + EU
        assertEquals(2, s.grids.worked());
    }

    @Test void wasCountsOnlyTheFiftyStates() {
        Awards.Snapshot s = Awards.compute(List.of(
                q("W1AW", "2026-01-02 15:30:00", "20m", "SSB", "CT", "", conf()),
                q("K1ABC", "2026-01-02 15:31:00", "20m", "SSB", "DC", "", JLogDb.Refs.NONE),      // DC is not a state
                q("VE3AA", "2026-01-02 15:32:00", "20m", "SSB", "ON", "", JLogDb.Refs.NONE)));    // neither is Ontario
        assertEquals(1, s.was.worked());
        assertEquals(1, s.was.confirmed());
        assertEquals(50, s.was.target());
    }

    @Test void referencesAndZonesAreCounted() {
        JLogDb.Refs island = new JLogDb.Refs("na-005", "", "K-0039", 5, 8, false, false, true);
        Awards.Snapshot s = Awards.compute(List.of(q("K1ABC", "2026-01-02 15:30:00", "20m", "SSB", "", "", island),
                q("K1ABC", "2026-01-03 15:30:00", "17m", "SSB", "", "", JLogDb.Refs.NONE)));
        assertEquals(1, s.pota.worked());
        assertEquals(1, s.pota.confirmed());
        assertEquals(1, s.cqz.worked());
        assertEquals(1, s.ituz.worked());
        assertEquals(0, s.sota.worked());
        assertEquals(10, s.pota.target() >= 10 ? 10 : s.pota.target());   // no fixed target: next milestone
    }

    @Test void callsignBlocksThatUsedToResolveToNothingNowDo() {
        // Prefixes the DXCC table lacked: Canadian VA/VY, UK M and 2x, Madeira / Azores, Hungary HG, Israel 4Z, Japan JG, China B.
        String[][] expect = {{"VA3SK", "Canada"}, {"VY2ZM", "Canada"}, {"M0SIY", "England"}, {"2E0ABC", "England"}, {"MS0OXE", "Scotland"},
                {"CR3A", "Madeira"}, {"CS2C", "Azores"}, {"CQ3L", "Madeira"}, {"HG7T", "Hungary"}, {"4Z1RZ", "Israel"}, {"JG1ABC", "Japan"},
                {"BG7ABC", "China"}, {"TM6M", "France"}, {"SY2ALU", "Greece"}, {"HU0A", "El Salvador"}, {"C4W", "Cyprus"}};
        for (String[] e : expect) {
            DxccInfo.Info i = DxccInfo.of(e[0]);
            assertNotNull(i, e[0]);
            assertEquals(e[1], i.country(), e[0]);
        }
    }

    @Test void distinctEntitiesDoNotShareADxccNumber() {
        assertNotEquals(DxccInfo.of("JA1ABC").dxcc(), DxccInfo.of("VU2ABC").dxcc());
        assertNotEquals(DxccInfo.of("HL1ABC").dxcc(), DxccInfo.of("VU2ABC").dxcc());       // South Korea and India both used to be 137
        assertNotEquals(DxccInfo.of("EP2ABC").dxcc(), DxccInfo.of("UN7ABC").dxcc());       // Iran and Kazakhstan both used to be 130
        assertNotEquals(DxccInfo.of("JY1ABC").dxcc(), DxccInfo.of("OD5ABC").dxcc());       // Jordan and Lebanon both used to be 354
        assertNotEquals(DxccInfo.of("9L1ABC").dxcc(), DxccInfo.of("ZD7ABC").dxcc());       // Sierra Leone and St Helena both used to be 250
    }

    @Test void milestonesRoundUp() {
        assertEquals(10, Awards.milestone(0));
        assertEquals(25, Awards.milestone(10));
        assertEquals(100, Awards.milestone(99));
        assertEquals(3000, Awards.milestone(2500));
    }

    @Test void modeGroups() {
        assertEquals("CW", Awards.modeGroup("cw"));
        assertEquals("Phone", Awards.modeGroup("USB"));
        assertEquals("Digital", Awards.modeGroup("FT8"));
        assertEquals("Digital", Awards.modeGroup(null));
    }

    // ---- database and ADIF round trip ----------------------------------------------------------------
    private String oldHome;
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); }
    @AfterEach void restore() { System.setProperty("user.home", oldHome); }

    @Test void referencesAndConfirmationsRoundTripThroughTheDatabaseAndAdif() {
        JLogDb.Refs r = new JLogDb.Refs("EU-005", "G/LD-001", "K-0039", 14, 27, true, true, false);
        assertTrue(JLogDb.insertAt("2026-02-03 12:00:00", "g4abc", "14.250", "20m", "SSB", "59", "59", "Tim", "Kendal", "IO84", "", "", "", 100, r));
        JLogDb.QsoFull f = JLogDb.all().get(0);
        assertEquals(r, f.refs());

        String adif = LogExport.toAdif(JLogDb.allForExport());
        assertTrue(adif.contains("<IOTA:6>EU-005"));
        assertTrue(adif.contains("<POTA_REF:6>K-0039"));
        assertTrue(adif.contains("<LOTW_QSL_RCVD:1>Y"));
        assertFalse(adif.contains("EQSL_QSL_RCVD"));
        Map<String, String> rec = LogImport.parse(adif).get(0);
        assertEquals(r, LogImport.refsOf(rec));
    }

    @Test void importingConfirmationsMarksTheQsoAlreadyInTheLog() throws Exception {
        assertTrue(JLogDb.insertAt("2026-02-03 12:00:07", "G4ABC", "14.250", "20m", "SSB", "59", "59", "", "", "", "", "", "", 0));
        assertFalse(JLogDb.all().get(0).refs().lotw());
        Path adi = Path.of(System.getProperty("user.home"), "lotw.adi");
        java.nio.file.Files.writeString(adi, "<EOH><CALL:5>G4ABC<QSO_DATE:8>20260203<TIME_ON:4>1200<BAND:3>20m<MODE:3>SSB<LOTW_QSL_RCVD:1>Y<EOR>");
        LogImport.Result res = LogImport.importAdif(adi);
        assertTrue(res.ok());
        assertEquals(0, res.imported());
        assertEquals(1, res.duplicates());
        assertTrue(JLogDb.all().get(0).refs().lotw(), "the duplicate should have been confirmed");
        assertEquals(1, JLogDb.count());
    }

    @Test void editingAQsoKeepsItsReferences() {
        JLogDb.Refs r = new JLogDb.Refs("", "", "K-0039", 0, 0, false, true, false);
        JLogDb.insertAt("2026-02-03 12:00:00", "N1ABC", "14.250", "20m", "SSB", "59", "59", "", "", "", "", "", "", 0, r);
        long id = JLogDb.all().get(0).id();
        assertTrue(JLogDb.update(id, "2026-02-03 12:00:00", "N1ABC", "14.250", "20m", "SSB", "59", "57", "Bob", "", "", "", "", "", 0));   // old signature
        assertEquals(r, JLogDb.byId(id).refs());
    }
}
