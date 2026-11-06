package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.QtcRules.Line;
import com.jlog.model.QtcRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QtcRulesTest {
    static QtcRecord rec(String partner, String dir, int series, String time, String call, String serial) {
        QtcRecord r = new QtcRecord();
        r.setPartnerCall(partner); r.setDirection(dir); r.setSeriesNo(series); r.setQsoTime(time); r.setQsoCall(call); r.setQsoSerial(serial);
        return r;
    }
    static List<Line> lines(int n) {
        List<Line> l = new ArrayList<>();
        for (int i = 1; i <= n; i++) l.add(new Line(String.format("12%02d", i), "DL" + i + "ABC", String.valueOf(100 + i)));
        return l;
    }
    static String check(String own, String partner, String pc, String dir, List<Line> l, List<QtcRecord> ex) { return QtcRules.validate(own, partner, pc, dir, l, ex); }

    @Test void aGoodSeriesPasses() {
        assertNull(check("NA", "DL1XYZ", "EU", "RECEIVED", lines(5), List.of()));
        assertNull(check("EU", "W1AW", "NA", "SENT", lines(10), List.of()), "a full series of ten is fine");
    }

    @Test void aSeriesHoldsAtMostTen() {
        assertTrue(check("NA", "DL1XYZ", "EU", "RECEIVED", lines(11), List.of()).contains("at most 10"));
    }

    @Test void atMostTenInAllWithTheSameStation() {
        List<QtcRecord> ex = new ArrayList<>();
        for (int i = 1; i <= 6; i++) ex.add(rec("DL1XYZ", "RECEIVED", 1, "11" + String.format("%02d", i), "F" + i + "AAA", String.valueOf(i)));
        assertNull(check("NA", "DL1XYZ", "EU", "RECEIVED", lines(4), ex), "6 + 4 = 10");
        String msg = check("NA", "DL1XYZ", "EU", "RECEIVED", lines(5), ex);
        assertTrue(msg.contains("At most 10") && msg.contains("6 already"), msg);
        assertNull(check("NA", "OH2ABC", "EU", "RECEIVED", lines(10), ex), "another station has its own allowance");
    }

    @Test void qtcsPassOnlyBetweenEuropeAndTheRestOfTheWorld() {
        assertNotNull(check("EU", "DL1XYZ", "EU", "SENT", lines(2), List.of()), "two Europeans");
        assertNotNull(check("NA", "W1AW", "NA", "RECEIVED", lines(2), List.of()), "two non-Europeans");
        assertNull(check("NA", "DL1XYZ", null, "RECEIVED", lines(2), List.of()), "an unknown continent never blocks");
    }

    @Test void rttyNeedsOnlyDifferentContinents() {
        assertNull(QtcRules.validate("NA", "JA1ABC", "AS", "SENT", lines(2), List.of(), true), "two non-Europeans on different continents");
        assertNull(QtcRules.validate("EU", "W1AW", "NA", "RECEIVED", lines(2), List.of(), true));
        assertTrue(QtcRules.validate("NA", "W1AW", "NA", "SENT", lines(2), List.of(), true).contains("different continents"));
        assertNotNull(QtcRules.validate("NA", "JA1ABC", "AS", "SENT", lines(2), List.of(), false), "CW and SSB still need a European on one side");
    }

    @Test void tenInAllCountsSentAndReceivedTogether() {
        List<QtcRecord> ex = new ArrayList<>();
        for (int i = 1; i <= 6; i++) ex.add(rec("W1AW", "SENT", 1, "11" + String.format("%02d", i), "F" + i + "AAA", String.valueOf(i)));
        String msg = QtcRules.validate("EU", "W1AW", "NA", "RECEIVED", lines(5), ex, true);
        assertTrue(msg.contains("At most 10") && msg.contains("6 already"), msg);
        assertNull(QtcRules.validate("EU", "W1AW", "NA", "RECEIVED", lines(4), ex, true), "6 + 4 = 10");
    }

    @Test void aQsoIsReportedOnlyOnce() {
        List<QtcRecord> ex = List.of(rec("W1AW", "SENT", 1, "1201", "DL1ABC", "101"));
        String msg = check("EU", "W2XYZ", "NA", "SENT", List.of(new Line("1201", "DL1ABC", "101")), ex);
        assertTrue(msg.contains("already been reported"), msg);
        assertNull(check("EU", "W2XYZ", "NA", "RECEIVED", List.of(new Line("1201", "DL1ABC", "101")), ex), "received traffic is its own list");
        assertNotNull(check("EU", "W2XYZ", "NA", "SENT", List.of(new Line("1201", "DL1ABC", "101"), new Line("1201", "DL1ABC", "0101")), List.of()),
                "twice in one series, leading zeros ignored");
    }

    @Test void notBackToTheStationTheQsoWasMadeWith() {
        String msg = check("EU", "W1AW", "NA", "SENT", List.of(new Line("1201", "W1AW", "101")), List.of());
        assertTrue(msg.contains("cannot report the QSO with the station"), msg);
    }

    @Test void eachLineIsChecked() {
        assertTrue(check("NA", "DL1XYZ", "EU", "RECEIVED", List.of(new Line("2460", "DL1AAA", "1")), List.of()).contains("HHMM"));
        assertTrue(check("NA", "DL1XYZ", "EU", "RECEIVED", List.of(new Line("1200", "NOCALL", "1")), List.of()).contains("not a callsign"));
        assertTrue(check("NA", "DL1XYZ", "EU", "RECEIVED", List.of(new Line("1200", "DL1AAA", "12a")), List.of()).contains("digits only"));
        assertNotNull(check("NA", "", "EU", "RECEIVED", lines(1), List.of()));
        assertNotNull(check("NA", "DL1XYZ", "EU", "RECEIVED", List.of(), List.of()));
    }

    @Test void seriesNumbersCountUpPerDirection() {
        List<QtcRecord> ex = List.of(rec("A", "SENT", 1, "1", "X", "1"), rec("A", "SENT", 2, "1", "Y", "1"), rec("B", "RECEIVED", 7, "1", "Z", "1"));
        assertEquals(3, QtcRules.nextSeries(ex, "SENT"));
        assertEquals(8, QtcRules.nextSeries(ex, "RECEIVED"));
        assertEquals(1, QtcRules.nextSeries(List.of(), "SENT"));
        assertEquals("RECEIVED", QtcRules.defaultDirection("EU"), "a QTC is reported back to a European station");
        assertEquals("SENT", QtcRules.defaultDirection("NA"));
    }
}
