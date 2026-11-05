package com.ars.fx.data;

import com.ars.fx.data.CustomAwards.Def;
import com.ars.fx.data.CustomAwards.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CustomAwardsTest {
    static JLogDb.QsoExport q(String call, String date, String band, String mode, String state, boolean confirmed) {
        return new JLogDb.QsoExport(call, date + " 12:00:00", band, mode, "14.250", 100, "59", "59", "", "", state, "", "", "",
                new JLogDb.Refs("", "", "", 0, 0, confirmed, false, false));
    }

    @Test
    void thirteenColoniesTemplateTracksEachStation() {
        Def d = CustomAwards.template13Colonies();
        assertEquals(14, d.items.size());
        Result r = CustomAwards.compute(d, List.of(
                q("K2A", "2026-07-01", "20m", "SSB", "", false),
                q("K2C", "2026-07-02", "40m", "CW", "", true),
                q("K2C", "2026-07-03", "20m", "CW", "", false),
                q("W1AW", "2026-07-03", "20m", "CW", "", true)));
        assertEquals(2, r.worked);
        assertEquals(1, r.confirmed);
        assertEquals(2, r.rows.get(2).qsos, "K2C worked twice");
        assertEquals("2026-07-02", r.rows.get(2).first.date());
        assertFalse(r.rows.get(1).worked, "K2B not worked");
    }

    @Test
    void bandModeAndDateFiltersApply() {
        Def d = CustomAwards.template13Colonies();
        d.bands.add("20m"); d.modes.add("SSB"); d.from = "2026-07-01"; d.to = "2026-07-31";
        Result r = CustomAwards.compute(d, List.of(
                q("K2A", "2026-07-01", "20m", "USB", "", false),   // USB counts as SSB
                q("K2B", "2026-07-01", "40m", "SSB", "", false),   // wrong band
                q("K2C", "2026-07-01", "20m", "CW", "", false),    // wrong mode
                q("K2D", "2026-08-01", "20m", "SSB", "", false))); // after the window
        assertEquals(1, r.worked);
        assertTrue(r.rows.get(0).worked);
    }

    @Test
    void confirmedOnlyCountsOnlyConfirmed() {
        Def d = CustomAwards.template13Colonies();
        d.confirmedOnly = true;
        Result r = CustomAwards.compute(d, List.of(q("K2A", "2026-07-01", "20m", "SSB", "", false), q("K2B", "2026-07-01", "20m", "SSB", "", true)));
        assertEquals(1, r.worked);
        assertFalse(r.rows.get(0).worked);
    }

    @Test
    void anyFieldAndMatchStyle() {
        Def d = new Def();
        d.name = "New England states"; d.field = "state"; d.how = "equals";
        d.items = CustomAwards.parseItems("Maine = ME\nVermont = VT\nNew Hampshire = NH\n# a comment\nConnecticut = CT, CONN");
        Result r = CustomAwards.compute(d, List.of(q("W1AA", "2026-01-01", "20m", "CW", "me", false), q("W1BB", "2026-01-02", "20m", "CW", "CONN", true)));
        assertEquals(4, r.total());
        assertEquals(2, r.worked);
        assertTrue(r.rows.get(3).worked, "second alternative matched");

        Def p = new Def(); p.name = "W prefixes"; p.field = "callsign"; p.how = "starts with";
        p.items = CustomAwards.parseItems("Nine land = W9\nOne land = W1");
        Result rp = CustomAwards.compute(p, List.of(q("W9XYZ", "2026-01-01", "20m", "CW", "", false)));
        assertEquals(1, rp.worked);
    }

    @Test
    void itemTextRoundTrips() {
        String text = "K2A\nBonus = WM3GPO\n";
        assertEquals(text, CustomAwards.itemsText(CustomAwards.parseItems(text)));
    }

    @Test
    void saveListDeleteImportExport(@TempDir Path tmp) throws Exception {
        CustomAwards.setDirForTest(tmp.resolve("awards"));
        try {
            Def d = CustomAwards.save(CustomAwards.template13Colonies());
            assertEquals("13-colonies-special-event", d.id);
            Def again = CustomAwards.template13Colonies();
            assertEquals("13-colonies-special-event-2", CustomAwards.save(again).id, "a second award never overwrites the first");
            assertEquals(2, CustomAwards.list().size());

            Path shared = CustomAwards.exportJson(d, tmp.resolve("out"));
            Def imported = CustomAwards.importFile(shared);
            assertEquals(14, imported.items.size());
            assertNotEquals(d.id, imported.id);

            CustomAwards.delete(imported);
            assertEquals(2, CustomAwards.list().size());

            Result r = CustomAwards.compute(d, List.of(q("K2A", "2026-07-01", "20m", "SSB", "", false)));
            String csv = java.nio.file.Files.readString(CustomAwards.exportCsv(r, tmp.resolve("out")));
            assertTrue(csv.startsWith("Item,Status"));
            assertTrue(csv.contains("K2A,Worked,2026-07-01,K2A,20m,SSB,1"));
        } finally {
            CustomAwards.setDirForTest(null);
        }
    }

    @Test
    void trackingBandsMakesAnItemDoneOnlyWhenWorkedOnEveryBand() {
        Def d = CustomAwards.template13Colonies();
        d.trackBands.add("20m"); d.trackBands.add("40m");
        Result r = CustomAwards.compute(d, List.of(
                q("K2A", "2026-07-01", "20m", "SSB", "", false),
                q("K2A", "2026-07-02", "40m", "CW", "", true),
                q("K2B", "2026-07-03", "20m", "SSB", "", true)));
        assertTrue(r.rows.get(0).worked, "K2A done on both bands");
        assertTrue(r.rows.get(0).confirmed == false, "20m was not confirmed");
        assertFalse(r.rows.get(1).worked, "K2B only on 20m");
        assertEquals(1, r.worked);
        assertEquals(3, r.slotsWorked);                 // K2A 20m + 40m, K2B 20m
        assertEquals(28, r.slotsTotal);                 // 14 items x 2 bands
    }

    @Test
    void withoutTrackedBandsSlotsAreJustItems() {
        Result r = CustomAwards.compute(CustomAwards.template13Colonies(), List.of(q("K2A", "2026-07-01", "20m", "SSB", "", false)));
        assertEquals(1, r.slotsWorked);
        assertEquals(14, r.slotsTotal);
    }

    @Test
    void thePrintableSheetListsEveryItemAndEscapesText(@TempDir Path tmp) throws Exception {
        Def d = CustomAwards.template13Colonies();
        d.id = "colonies"; d.name = "Colonies <test> & more";
        Result r = CustomAwards.compute(d, List.of(q("K2A", "2026-07-01", "20m", "SSB", "", true)));
        String html = java.nio.file.Files.readString(CustomAwards.exportHtml(r, tmp, "WM3J"));
        assertTrue(html.contains("Colonies &lt;test&gt; &amp; more"));
        assertTrue(html.contains("WM3J"));
        assertTrue(html.contains("K2M"), "unworked items are on the sheet too");
        assertTrue(html.contains("&#9745;"), "a confirmed item is ticked");
        assertTrue(html.contains("&#9744;"), "an unworked item has an empty box");
        assertFalse(html.contains("<test>"));
    }
}
