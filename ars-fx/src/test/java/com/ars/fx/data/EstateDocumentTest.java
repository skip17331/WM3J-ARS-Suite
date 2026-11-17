package com.ars.fx.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EstateDocumentTest {

    private static JVaultDb.Item item(String cat, String mfr, String model, double value, String disposition) {
        return new JVaultDb.Item(0, 0, cat, mfr, model, "SN-1", "2020-05-01", value * 1.5, value, disposition, "installed", "shack desk", "");
    }
    private static final EstateDocument.Station ST = new EstateDocument.Station("wm3j", "Mike", "Somewhere, PA");
    private static final LocalDate DAY = LocalDate.of(2026, 11, 17);

    @Test
    void containsTheStationContactsAndInventory() {
        String h = EstateDocument.html(ST,
                List.of(item("Radio (HF Base)", "Icom", "IC-7300", 900, "working"), item("Amplifier", "Elecraft", "KPA500", 1800, "working")),
                List.of(new JVaultDb.Contact(1, "Pat Jones", "k3abc", "club friend", "the amplifier", "555-0100", "pat@example.org")),
                DAY);
        assertTrue(h.startsWith("<!doctype html>"));
        assertTrue(h.contains("WM3J · Mike · Somewhere, PA"));
        assertTrue(h.contains("Prepared 17 November 2026"));
        assertTrue(h.contains("Pat Jones") && h.contains("K3ABC") && h.contains("555-0100") && h.contains("pat@example.org") && h.contains("the amplifier"));
        assertTrue(h.contains("Icom IC-7300") && h.contains("Elecraft KPA500"));
        assertTrue(h.contains("2 items, estimated total <b>$2,700</b>"));
        assertTrue(h.contains("Do not climb a tower"));
        assertTrue(h.contains("(WM3J) should be canceled with the FCC"));
    }

    @Test
    void usCallGetsTheFccAndArrlStepsWithTheLicenseDetails() {
        JVaultDb.LicenseInfo li = new JVaultDb.LicenseInfo("0012345678", "Amateur Extra", "2031-04-12", "Blue folder, desk drawer", "1234567", "Anytown ARC");
        String h = EstateDocument.html(ST, li, List.of(), List.of(), DAY);
        int fcc = h.indexOf("<h2>Canceling the radio license (FCC)</h2>"), arrl = h.indexOf("<h2>Reporting to ARRL (Silent Key)</h2>");
        assertTrue(fcc > 0 && arrl > fcc, "FCC section, then ARRL");
        assertTrue(h.contains("0012345678") && h.contains("Amateur Extra") && h.contains("2031-04-12") && h.contains("Blue folder, desk drawer"));
        assertTrue(h.contains("fcc.gov/wireless/available-support-services") && h.contains("Submit a help request"));
        assertTrue(h.contains("Print it and sign it by hand") && h.contains("does not accept requests by mail or fax"));
        assertTrue(h.contains("CANCELED") && h.contains("877-480-3201"));
        assertTrue(h.contains("sk@arrl.org") && h.contains("1-860-594-0303") && h.contains("1234567") && h.contains("Anytown ARC"));
        assertTrue(h.contains("does <b>not</b> cancel the FCC license"));
    }

    @Test
    void noFrnStillShowsTheStepsForAUsCallAndSaysWhereToFindIt() {
        String h = EstateDocument.html(ST, List.of(), List.of(), DAY);
        assertTrue(h.contains("<h2>Canceling the radio license (FCC)</h2>"));
        assertTrue(h.contains("not recorded (the FCC&#39;s license search shows it with the call sign)"));
        assertFalse(h.contains("ARRL member number"), "no empty member row");
    }

    @Test
    void nonUsCallWithoutFrnGetsTheGeneralNoteOnly() {
        String h = EstateDocument.html(new EstateDocument.Station("G4ABC", "", ""), List.of(), List.of(), DAY);
        assertFalse(h.contains("Canceling the radio license (FCC)"));
        assertFalse(h.contains("sk@arrl.org"));
        assertTrue(h.contains("(G4ABC) should be reported to the licensing authority"));
    }

    @Test
    void recognisesUsCalls() {
        for (String c : new String[]{"WM3J", "K1A", "N0XYZ", "KD9ABC", "AA1AA", "AL7X", "wm3j/p", "W1AW/4"})
            assertTrue(EstateDocument.isUsCall(c), c);
        for (String c : new String[]{"", "G4ABC", "VE3XYZ", "AM1A", "JA1ABC", "KH6", "W1"})
            assertFalse(EstateDocument.isUsCall(c), c);
    }

    @Test
    void soldItemsAreListedApartAndNotCounted() {
        String h = EstateDocument.html(ST,
                List.of(item("Radio (HF Base)", "Icom", "IC-7300", 900, "working"), item("Radio (HF Base)", "Yaesu", "FT-991A", 700, "sold")),
                List.of(), DAY);
        assertTrue(h.contains("1 item, estimated total <b>$900</b>"));
        int soldAt = h.indexOf("<h2>Already sold</h2>");
        assertTrue(soldAt > 0);
        assertTrue(h.indexOf("Yaesu FT-991A") > soldAt, "the sold radio appears only under Already sold");
    }

    @Test
    void emptyVaultStillReadsSensibly() {
        String h = EstateDocument.html(new EstateDocument.Station("", "", ""), List.of(), List.of(), DAY);
        assertTrue(h.contains("No first-call contacts have been entered"));
        assertTrue(h.contains("No equipment has been entered"));
        assertTrue(h.contains("0 items, estimated total <b>not given</b>"));
        assertFalse(h.contains("<h2>Already sold</h2>"));
        assertTrue(h.contains("<b>The radio license</b> should be reported"), "no empty callsign brackets when no call is set");
    }

    @Test
    void userTextIsEscaped() {
        String h = EstateDocument.html(ST, List.of(item("Other", "<script>alert(1)</script>", "A&B", 10, "working")),
                List.of(new JVaultDb.Contact(1, "O'Neil \"Ham\"", null, null, null, null, null)), DAY);
        assertFalse(h.contains("<script>alert"));
        assertTrue(h.contains("&lt;script&gt;alert(1)&lt;/script&gt; A&amp;B"));
        assertTrue(h.contains("O&#39;Neil &quot;Ham&quot;"));
    }

    @Test
    void savesWithTheCallAndTimeInTheName(@TempDir Path dir) throws Exception {
        Path f = EstateDocument.save(dir.resolve("estate"), "wm3j/p", "<html></html>", LocalDateTime.of(2026, 11, 17, 9, 5, 7));
        assertEquals("estate-WM3J-P-20261117-090507.html", f.getFileName().toString());
        assertEquals("<html></html>", Files.readString(f));
        if (Files.getFileAttributeView(f, PosixFileAttributeView.class) != null)
            assertEquals(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), Files.getPosixFilePermissions(f));
    }
}
