package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.MultiplierLists;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks the bundled QSO party definitions against the rules the engine used to get wrong: a cap on DX multipliers, DC counted as
 * Maryland, California's 2026 phone points, Illinois' bonus stations, Louisiana's four-letter parish codes, and the corrected
 * county lists.
 */
class QsoPartyRuleFixesTest {

    private static ContestPlugin load(String id) throws Exception {
        try (InputStream in = QsoPartyRuleFixesTest.class.getResourceAsStream("/com/jlog/plugins/" + id + ".json")) {
            assertNotNull(in, id);
            return new ObjectMapper().readValue(in, ContestPlugin.class);
        }
    }

    private static QsoRecord q(String call, String band, String mode, String rcvd) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode); r.setContestField1(rcvd);
        return r;
    }

    private static ContestScore score(ContestPlugin p, String sentQth, QsoRecord... qsos) {
        StationContext ctx = StationContext.of("W1AW", "FN31", sentQth);
        List<QsoRecord> l = new ArrayList<>();
        for (QsoRecord r : qsos) { r.setPoints(ContestScorer.points(p, r, ctx)); l.add(r); }
        return ContestScorer.score(p, l, ctx, "field1", List.of(), 0);
    }

    @Test void dxMultipliersAreCappedAtTenInWashington() throws Exception {
        String[] dx = {"G3ABC", "DL1ABC", "F1ABC", "I1ABC", "EA1ABC", "PA1ABC", "ON4ABC", "OZ1ABC", "SM5ABC", "OH2ABC", "LA1ABC", "SP1ABC"};
        List<QsoRecord> l = new ArrayList<>();
        for (String c : dx) l.add(q(c, "20m", "CW", "DX"));
        ContestScore s = score(load("wa_qso_party"), "KING", l.toArray(new QsoRecord[0]));
        assertEquals(12, s.qsoCount(), "every DX QSO still scores points");
        assertEquals(10, s.mults(), "but only ten entities earn a multiplier");
    }

    @Test void dxMultipliersAreCappedAtFiveInIllinois() throws Exception {
        String[] dx = {"G3ABC", "DL1ABC", "F1ABC", "I1ABC", "EA1ABC", "PA1ABC", "ON4ABC"};
        List<QsoRecord> l = new ArrayList<>();
        for (String c : dx) l.add(q(c, "20m", "CW", "DX"));
        assertEquals(5, score(load("il_qso_party"), "COOK", l.toArray(new QsoRecord[0])).mults());
    }

    @Test void dcCountsAsMarylandWhereTheRulesSayso() throws Exception {
        ContestScore s = score(load("tn_qso_party"), "KNOX",
            q("K1ABC", "20m", "CW", "MD"),
            q("W1XYZ", "20m", "CW", "DC"));
        assertEquals(1, s.mults(), "MD and DC are one multiplier in Tennessee");
        ContestScore t = score(load("oh_qso_party"), "FRAN",
            q("K1ABC", "20m", "CW", "MD"),
            q("W1XYZ", "20m", "CW", "DC"));
        assertEquals(2, t.mults(), "Ohio counts DC as its own multiplier");
    }

    @Test void californiaPhoneIsThreePointsFrom2026() throws Exception {
        ContestPlugin ca = load("ca_qso_party");
        StationContext in = StationContext.of("W1AW", "CM97", "ALAM");
        assertEquals(3, ContestScorer.points(ca, q("K1ABC", "20m", "SSB", "PA"), in));
        assertEquals(3, ContestScorer.points(ca, q("K1ABC", "20m", "CW", "PA"), in));
    }

    @Test void illinoisBonusStationsAreWorthAHundredEachOnce() throws Exception {
        ContestScore s = score(load("il_qso_party"), "COOK",
            q("K9AAA", "20m", "SSB", "PA"),        // 1 point, mult PA
            q("W9AWE", "20m", "SSB", "COOK"),      // 1 point, mult COOK, bonus 100
            q("W9OAB", "20m", "CW", "COOK"),       // 2 points, bonus 100
            q("W9AWE", "40m", "CW", "COOK"));      // second contact: no second bonus
        assertEquals(6, s.points());
        assertEquals(6 * 2 + 200, s.score());
    }

    @Test void louisianaParishesAreFourLettersLong() throws Exception {
        ContestPlugin la = load("la_qso_party");
        assertEquals(2, ContestScorer.points(la, q("W1AA", "20m", "SSB", "TX"), StationContext.of("W5AA", "EM30", "ACAD")),
            "a Louisiana entrant (parish ACAD) is in state and works everyone");
        assertEquals(2, ContestScorer.points(la, q("W5BB", "20m", "SSB", "JEFF"), StationContext.of("W1AA", "FN31", "TX")),
            "a Texas entrant works a Louisiana parish");
        assertEquals(0, ContestScorer.points(la, q("W1CC", "20m", "SSB", "PA"), StationContext.of("W1AA", "FN31", "TX")),
            "but not another out-of-state station");
    }

    @Test void theCorrectedCountyListsHaveTheOfficialSizesAndCodes() throws Exception {
        assertListed("ca_qso_party", 58, "CCOS", "LANG", "MARN");
        assertListed("fl_qso_party", 67, "ALC", "CAH", "WAG");
        assertListed("nc_qso_party", 100, "CAR", "GRM", "WLK");
        assertListed("ny_qso_party", 62, "CGO", "WES");
        assertListed("pa_qso_party", 67, "BUX", "MGY", "NUM");
        assertListed("mi_qso_party", 83, "ALCO", "BAY", "WEXF");
        assertListed("la_qso_party", 64, "ACAD", "EBR", "WFEL");
        assertListed("wa_qso_party", 39);
    }

    private static void assertListed(String id, int size, String... codes) throws Exception {
        List<String> list = MultiplierLists.load(load(id).getMultiplierList());
        assertEquals(size, list.size(), id);
        assertEquals(size, list.stream().distinct().count(), id + " has duplicates");
        for (String c : codes) assertTrue(list.contains(c), id + " is missing " + c);
    }

    @Test void georgiaAndTexasListsUseTheOfficialCodes() throws Exception {
        List<String> ga = MultiplierLists.load(load("ga_qso_party").getMultiplierList());
        assertEquals(159, ga.size());
        assertTrue(ga.contains("APPL") && ga.contains("DKLB") && ga.contains("WORT"));
        List<String> tx = MultiplierLists.load(load("tx_qso_party").getMultiplierList());
        assertEquals(254, tx.size());
        assertTrue(tx.contains("ANDE") && tx.contains("EPAS") && tx.contains("ZAVA") && tx.contains("BEE"));
    }
}
