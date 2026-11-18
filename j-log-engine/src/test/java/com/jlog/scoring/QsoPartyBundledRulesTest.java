package com.jlog.scoring;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The bundled QSO-party definitions switch the entrant-category rules on as their rules say (values in each definition's formula
 * note): checked by scoring small logs with the real plugins, so a typo in a definition's JSON or a missing key shows up here.
 */
class QsoPartyBundledRulesTest {

    private static final List<String> NO_BANDS = List.of();

    /** The bundled definition, read from the classpath as the loader reads it ("PA_QSO_PARTY" is pa_qso_party.json). */
    private static ContestPlugin plugin(String id) {
        String res = "/com/jlog/plugins/" + id.toLowerCase(java.util.Locale.ROOT) + ".json";
        try (var in = QsoPartyBundledRulesTest.class.getResourceAsStream(res)) {
            assertNotNull(in, res + " is bundled");
            ContestPlugin p = new ObjectMapper().readValue(in, ContestPlugin.class);
            assertEquals(id, p.getContestId());
            return p;
        } catch (java.io.IOException e) { throw new AssertionError(res, e); }
    }
    private static ContestPlugin.QsoPartyConfig cfg(String id) { return plugin(id).getScoringRules().getQsoParty(); }

    /** A QSO with received county {@code rcvd} sent from {@code sent}, points worked out by the plugin as the contest page does. */
    private static QsoRecord q(ContestPlugin p, StationContext ctx, String call, String band, String mode, String rcvd, String sent) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode);
        r.setContestField1(rcvd);
        String col = p.fieldSlotColumn("state_prov_sent");
        if ("field2".equals(col)) r.setContestField2(sent); else if ("field3".equals(col)) r.setContestField3(sent);
        r.setDateTimeUtc(LocalDateTime.of(2027, 2, 28, 16, 0));
        r.setPoints(ContestScorer.points(p, r, ctx));
        return r;
    }

    @Test void everyEditedPartyLoads() {
        for (String id : List.of("DE_QSO_PARTY", "KY_QSO_PARTY", "NM_QSO_PARTY", "VT_QSO_PARTY", "WI_QSO_PARTY", "FL_QSO_PARTY",
                "NE_QSO_PARTY", "MDC_QSO_PARTY", "PA_QSO_PARTY", "ID_QSO_PARTY", "AR_QSO_PARTY", "CO_QSO_PARTY", "OK_QSO_PARTY",
                "TN_QSO_PARTY", "TX_QSO_PARTY", "WV_QSO_PARTY", "NS_QSO_PARTY", "ON_QSO_PARTY", "QC_QSO_PARTY", "MO_QSO_PARTY",
                "NC_QSO_PARTY")) {
            assertNotNull(plugin(id).getScoringRules().getQsoParty(), id);
        }
    }

    /**
     * Amateur Radio Club QSO Party (punch list 3-3): points come from the class the other station sends, with no multiplier.
     * It used to score by mode (CW 3, phone 1, digital 2) times club members worked. The rules page's own example: 6
     * registered clubs, 3 unregistered, 46 club members, 7 non-members, 1 youth = 600 + 150 + 230 + 21 + 20 = 1021.
     */
    @Test void clubQsoPartyScoresByTheClassWorked() {
        ContestPlugin arc = plugin("ARC_QSO_PARTY");
        StationContext me = StationContext.of("K2AA", "FM29", "R", "", "");
        List<QsoRecord> l = new ArrayList<>();
        String[] bands = {"160m", "80m", "40m", "20m", "15m", "10m", "6m", "2m"};
        String[] modes = {"CW", "SSB", "FM", "RTTY"};
        int[] counts = {6, 3, 46, 7, 1};
        String[] classes = {"R", "U", "C", "N", "Y"};
        int n = 0;
        for (int k = 0; k < classes.length; k++)
            for (int i = 0; i < counts[k]; i++, n++)
                l.add(q(arc, me, "W" + (n % 10) + "X" + (char) ('A' + n / 10) + (char) ('A' + i % 26), bands[n % bands.length],
                        modes[n % modes.length], classes[k], "R"));
        ContestScore s = ContestScorer.score(arc, l, me, arc.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(63, s.qsoCount());
        assertEquals(1021, s.points());
        assertEquals(1, s.mults(), "no multiplier");
        assertEquals(1021, s.score());
    }

    @Test void clubQsoPartyClassIsTheLastWordAndModeDoesNotMatter() {
        ContestPlugin arc = plugin("ARC_QSO_PARTY");
        StationContext me = StationContext.of("K2AA", "FM29", "R", "", "");
        assertEquals(100, q(arc, me, "W2EA", "40m", "SSB", "R", "R").getPoints());
        assertEquals(100, q(arc, me, "W2EA", "40m", "CW", "110 R", "R").getPoints(), "club age typed in front of the class");
        assertEquals(50, q(arc, me, "W2EB", "20m", "RTTY", "u", "R").getPoints());
        assertEquals(3, q(arc, me, "K2UK", "20m", "CW", "N", "R").getPoints());
        assertEquals(0, q(arc, me, "K2UK", "20m", "CW", "BOB", "R").getPoints(), "no class letter: no points");
    }

    @Test void powerMultipliersFromTheDefinitions() {
        assertEquals(3.0, cfg("DE_QSO_PARTY").getPowerMultiplier().get("QRP"));
        assertEquals(5.0, cfg("NM_QSO_PARTY").getPowerMultiplier().get("QRP"));
        assertEquals(1.5, cfg("WI_QSO_PARTY").getPowerMultiplier().get("LOW"));
        assertEquals(1.5, cfg("VT_QSO_PARTY").getPowerMultiplier().get("LOW"));
        assertEquals(4.0, cfg("MDC_QSO_PARTY").getStationMultiplier().get("ROVER"));
        assertEquals(5, cfg("ID_QSO_PARTY").getPointsByPower().get("QRP"));
        assertEquals(2, cfg("AR_QSO_PARTY").getPointsByStation().get("ROVER"));
    }

    @Test void pennsylvaniaRoverActivatesACountyAt10Qsos() {
        ContestPlugin pa = plugin("PA_QSO_PARTY");
        StationContext rover = StationContext.of("W3XX", "FN10", "CHE", "LOW", "ROVER");
        List<QsoRecord> l = new ArrayList<>();
        for (int i = 0; i < 10; i++) l.add(q(pa, rover, "K3A" + (char) ('A' + i), "40m", "CW", "BUC", "CHE"));
        ContestScore s = ContestScorer.score(pa, l, rover, pa.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(s.points() * s.mults() + 500, s.score(), "10 QSOs from CHE: +500");
        ContestScore fixed = ContestScorer.score(pa, l, StationContext.of("W3XX", "FN10", "CHE", "LOW", "FIXED"), pa.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(fixed.points() * fixed.mults(), fixed.score(), "a fixed station gets no rover bonus");
    }

    @Test void northCarolina2027() {
        ContestPlugin nc = plugin("NC_QSO_PARTY");
        StationContext mobile = StationContext.of("W4XX", "EM95", "WAK", "LOW", "MOBILE");
        List<QsoRecord> l = new ArrayList<>(List.of(
                q(nc, mobile, "K4AA", "40m", "CW", "DUR", "WAK"),
                q(nc, mobile, "K4AB", "40m", "CW", "ORA", "CHA")));
        ContestScore s = ContestScorer.score(nc, l, mobile, nc.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(6, s.points(), "CW 3 each; no rarest-county ×10 any more");
        assertEquals(4, s.mults(), "DUR, ORA worked + WAK, CHA operated from");
        assertEquals(6 * 4 + 2 * 100, s.score(), "+100 per county activated");
        for (String c : List.of("N4H", "N4I", "N4K", "N4E", "N4N", "N4C")) l.add(q(nc, mobile, c, "20m", "SSB", "WAK", "WAK"));
        ContestScore all = ContestScorer.score(nc, l, mobile, nc.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(all.points() * all.mults() + 2 * 100 + 6 * 50 + 200, all.score(), "HIKENC: 50 each + 200 for all six");
    }

    @Test void missouriWindowElogAndMobileCountyMultiplier() {
        ContestPlugin mo = plugin("MO_QSO_PARTY");
        StationContext fixed = StationContext.of("W0XX", "EM48", "SLC", "LOW", "FIXED");
        QsoRecord a = q(mo, fixed, "K0AA", "40m", "CW", "JAC", "SLC");
        a.setDateTimeUtc(LocalDateTime.of(2027, 4, 3, 15, 0));                       // Saturday 15:00 UTC: in the window
        a.setPoints(ContestScorer.points(mo, a, fixed));
        ContestScore s = ContestScorer.score(mo, List.of(a), fixed, mo.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(s.points() * s.mults() + 100 + 1, s.score(), "+100 e-log, +1 time window");
    }

    @Test void missouriCountiesComeFromTheOfficialList() {
        ContestPlugin mo = plugin("MO_QSO_PARTY");
        java.util.List<String> list = com.jlog.plugin.MultiplierLists.load(mo.getMultiplierList());
        assertEquals(115, list.size(), "114 counties + St. Louis City (2026 rules, Table 1)");
        assertTrue(list.containsAll(List.of("SLC", "STL", "STG", "CPG", "NMD", "WRT")));
        StationContext out = StationContext.of("W1XX", "FN31", "MA", "LOW", "FIXED");
        QsoRecord real = q(mo, out, "K0AA", "20m", "CW", "JAC", "MA");
        QsoRecord fake = q(mo, out, "K0AB", "20m", "CW", "XYZ", "MA");       // three letters, but not a Missouri county
        assertTrue(real.getPoints() > 0, "an out-of-state entrant scores a Missouri county");
        assertEquals(0, fake.getPoints(), "any three-letter code used to count; now only the official ones do");
        assertEquals(1, ContestScorer.score(mo, List.of(real, fake), out, mo.computeMultiplierDbColumn(), NO_BANDS, 0).mults());
    }

    /** The official lists added 2026-11-17: each definition uses its list, of the state's real size, and the guesswork rules are gone. */
    @Test void officialCountyListsReplaceTheGuesswork() {
        java.util.Map<String, Integer> want = java.util.Map.of("WI_QSO_PARTY", 72, "CO_QSO_PARTY", 64, "ID_QSO_PARTY", 44,
                "AR_QSO_PARTY", 75, "KY_QSO_PARTY", 120, "ACQP_QSO_PARTY", 47);
        for (var e : want.entrySet()) {
            ContestPlugin p = plugin(e.getKey());
            java.util.List<String> list = com.jlog.plugin.MultiplierLists.load(p.getMultiplierList());
            assertEquals(e.getValue().intValue(), list.size(), e.getKey());
            assertEquals(e.getValue().intValue(), new java.util.HashSet<>(list).size(), e.getKey() + " has no duplicates");
            assertEquals(0, cfg(e.getKey()).getCountyCodeLen(), e.getKey());
            assertFalse(cfg(e.getKey()).isCountyByExclusion(), e.getKey());
        }
        // a few codes checked by eye against the sources
        assertTrue(com.jlog.plugin.MultiplierLists.load(plugin("WI_QSO_PARTY").getMultiplierList()).containsAll(List.of("MRN", "MRQ", "WSB", "WSR", "GRL")));
        assertTrue(com.jlog.plugin.MultiplierLists.load(plugin("KY_QSO_PARTY").getMultiplierList()).containsAll(List.of("BOL", "CAE", "MCY", "TRM", "HSN")));
        assertTrue(com.jlog.plugin.MultiplierLists.load(plugin("ACQP_QSO_PARTY").getMultiplierList()).containsAll(List.of("NSANP", "NLASJ", "PEKGS", "NBSJC", "NSHRM")));
        assertTrue(com.jlog.plugin.MultiplierLists.load(plugin("AR_QSO_PARTY").getMultiplierList()).containsAll(List.of("SFR", "VBN", "LRV", "JAK", "PLK")));
    }

    @Test void kentuckyNoLongerCountsAnyLongCodeAsACounty() {
        ContestPlugin ky = plugin("KY_QSO_PARTY");
        StationContext out = StationContext.of("W1XX", "FN31", "MA", "LOW", "FIXED");
        assertTrue(q(ky, out, "K4AA", "20m", "CW", "FAY", "MA").getPoints() > 0, "Fayette is a Kentucky county");
        assertEquals(0, q(ky, out, "K4AB", "20m", "CW", "XYZ", "MA").getPoints(), "by exclusion any 3 letters counted; the list does not");
    }

    @Test void atlanticCanadaKeepsItsProvinceMultiplier() {
        ContestPlugin ac = plugin("ACQP_QSO_PARTY");
        StationContext in = StationContext.of("VE1XX", "FN84", "NSHRM", "LOW", "FIXED");
        List<QsoRecord> l = List.of(q(ac, in, "VE1AA", "20m", "SSB", "NSANP", "NSHRM"), q(ac, in, "VE9AA", "20m", "SSB", "NBYOR", "NSHRM"));
        assertEquals(2, ContestScorer.score(ac, l, in, ac.computeMultiplierDbColumn(), NO_BANDS, 0).mults(), "NS and NB on 20m, from the codes' first two letters");
        // 2026 rules: an Atlantic province counts on each band AND mode, so NS on 20m CW is another multiplier; a state stays per band
        List<QsoRecord> withCw = new ArrayList<>(l);
        withCw.add(q(ac, in, "VE1AB", "20m", "CW", "NSCOL", "NSHRM"));
        withCw.add(q(ac, in, "W1AA", "20m", "SSB", "MA", "NSHRM"));
        withCw.add(q(ac, in, "W1AB", "20m", "CW", "MA", "NSHRM"));
        assertEquals(4, ContestScorer.score(ac, withCw, in, ac.computeMultiplierDbColumn(), NO_BANDS, 0).mults(), "NS SSB, NB SSB, NS CW, MA once on 20m");
    }

    @Test void oklahomaUsesTheOkdxaList() {
        ContestPlugin ok = plugin("OK_QSO_PARTY");
        java.util.List<String> list = com.jlog.plugin.MultiplierLists.load(ok.getMultiplierList());
        assertEquals(77, list.size());
        assertTrue(list.containsAll(List.of("GNT", "GRA", "GRV", "MCU", "RGM", "WDW", "WAT")));
        StationContext out = StationContext.of("W1XX", "FN31", "MA", "LOW", "FIXED");
        assertTrue(q(ok, out, "K5AA", "20m", "CW", "TUL", "MA").getPoints() > 0);
        assertEquals(0, q(ok, out, "K5AB", "20m", "CW", "XYZ", "MA").getPoints(), "any three letters used to count");
    }

    /** MNQP 2027: CW 3 / Phone 2; mults once per mode; an MN station's mults are states incl. MN (no MN counties), DC, VE, 1 DX. */
    @Test void minnesota2027() {
        ContestPlugin mn = plugin("MN_QSO_PARTY");
        StationContext in = StationContext.of("W0XX", "EN34", "HEN", "LOW", "FIXED");
        List<QsoRecord> l = List.of(
                q(mn, in, "K0AA", "20m", "CW", "DAK", "HEN"),     // an MN county: the MN state multiplier, not a county one
                q(mn, in, "K0AB", "20m", "CW", "RAM", "HEN"),     // another MN county: MN again, no new multiplier
                q(mn, in, "W1AA", "20m", "CW", "MA", "HEN"),
                q(mn, in, "W1AB", "20m", "SSB", "MA", "HEN"));    // MA on SSB: a new multiplier (per mode)
        assertEquals(3, l.get(0).getPoints(), "CW is 3 points in 2027");
        assertEquals(2, l.get(3).getPoints(), "Phone 2");
        ContestScore s = ContestScorer.score(mn, l, in, mn.computeMultiplierDbColumn(), NO_BANDS, 0);
        assertEquals(3, s.mults(), "CW: MN, MA; SSB: MA");
        assertEquals(11 * 3, s.score());
        StationContext out = StationContext.of("W1XX", "FN42", "MA", "LOW", "FIXED");
        List<QsoRecord> o = List.of(q(mn, out, "K0AA", "20m", "CW", "DAK", "MA"), q(mn, out, "K0AA", "40m", "SSB", "DAK", "MA"));
        assertEquals(2, ContestScorer.score(mn, o, out, mn.computeMultiplierDbColumn(), NO_BANDS, 0).mults(), "outside MN: DAK on CW and on SSB");
    }

    @Test void ontarioRoverReworksFromANewArea() {
        ContestPlugin on = plugin("ON_QSO_PARTY");
        assertTrue(cfg("ON_QSO_PARTY").isOwnCountyDupe());
        StationContext rover = StationContext.of("VE3XX", "FN03", "TOR", "LOW", "ROVER");
        QsoRecord first = q(on, rover, "VE3AA", "40m", "CW", "OTT", "TOR");
        assertFalse(ContestScorer.isDupe(on, q(on, rover, "VE3AA", "40m", "CW", "OTT", "YRK"), List.of(first)));
        assertTrue(ContestScorer.isDupe(on, q(on, rover, "VE3AA", "40m", "CW", "OTT", "TOR"), List.of(first)));
    }
}
