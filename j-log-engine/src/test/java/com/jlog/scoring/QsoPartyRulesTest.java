package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The QSO-party rules that depend on the entrant's own category or county: power and station-category multipliers, points by
 * power, the mobile / rover county activation bonus (points or multiplier), own county as a free multiplier, the own-county dupe
 * rule, bonus stations below 50 MHz only, the electronic-log bonus, the time-window bonus and the bonus-station sweep.
 *
 * <p>The test plugin's entry fields put the received county in field1 and the sent (own) county in field2, as the real QSO-party
 * definitions do. Points are pre-set on each QSO, as the scorer sums stored points.
 */
class QsoPartyRulesTest {

    private final ObjectMapper M = new ObjectMapper();
    private static final List<String> NO_BANDS = List.of();
    private static final String FIELDS = "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"band\"},{\"id\":\"mode\"},"
            + "{\"id\":\"state_prov_rcvd\"},{\"id\":\"state_prov_sent\"}],";

    private ContestPlugin party(String extraQsoParty) throws Exception {
        return M.readValue("{\"contestId\":\"T\"," + FIELDS + "\"scoringRules\":{\"multiplierType\":\"qso_party\",\"pointsPerQso\":1,"
                + "\"qsoParty\":{\"stateAbbr\":\"XX\",\"inStateCounties\":[\"AAA\",\"BBB\",\"CCC\"],\"inStateCountsCounties\":true,"
                + "\"inStateCountsStates\":true,\"multScope\":\"once\"" + (extraQsoParty.isEmpty() ? "" : "," + extraQsoParty) + "}}}",
                ContestPlugin.class);
    }

    private static QsoRecord q(String call, String band, String mode, String rcvd, String sent, int pts) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode);
        r.setContestField1(rcvd); r.setContestField2(sent); r.setPoints(pts);
        r.setDateTimeUtc(LocalDateTime.of(2026, 4, 11, 12, 0));       // a Saturday
        return r;
    }

    private static StationContext in(String power, String station) { return StationContext.of("W1XX", "FN20", "AAA", power, station); }

    // ---- power and station multipliers -----------------------------------------

    @Test void powerMultiplierScalesPointsTimesMultsButNotBonuses() throws Exception {
        ContestPlugin pl = party("\"powerMultiplier\":{\"QRP\":3,\"LOW\":2,\"HIGH\":1},\"bonusStationsOnce\":{\"W9BON\":100}");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 2), q("W9BON", "20m", "CW", "CCC", "AAA", 2));
        assertEquals(4 * 2 * 3 + 100, ContestScorer.score(pl, l, in("QRP", "FIXED"), "field1", NO_BANDS, 0).score());
        assertEquals(4 * 2 * 2 + 100, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score());
        assertEquals(4 * 2 + 100,     ContestScorer.score(pl, l, in("HIGH", "FIXED"), "field1", NO_BANDS, 0).score());
        assertEquals(4 * 2 + 100,     ContestScorer.score(pl, l, in("", "FIXED"), "field1", NO_BANDS, 0).score(), "unknown power = ×1");
    }

    @Test void powerMultiplierMayBeFractional() throws Exception {
        ContestPlugin pl = party("\"powerMultiplier\":{\"LOW\":1.5}");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 3));
        assertEquals(Math.round(3 * 1 * 1.5), ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score());
    }

    @Test void stationMultiplierMatchesTheCategoryOrItsHead() throws Exception {
        ContestPlugin pl = party("\"stationMultiplier\":{\"ROVER\":4,\"PORTABLE\":3,\"MOBILE\":2,\"FIXED\":1},\"powerMultiplier\":{\"QRP\":3}");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1));
        assertEquals(4,     ContestScorer.score(pl, l, in("HIGH", "ROVER-LIMITED"), "field1", NO_BANDS, 0).score());
        assertEquals(2 * 3, ContestScorer.score(pl, l, in("QRP", "MOBILE"), "field1", NO_BANDS, 0).score(), "station × power");
    }

    @Test void pointsByPowerSetsEveryScoringQsosPoints() throws Exception {
        ContestPlugin pl = party("\"pointsByModeClass\":{\"PH\":1,\"CW\":2},\"pointsByPower\":{\"QRP\":5}");
        QsoRecord ph = q("K1A", "20m", "SSB", "BBB", "AAA", 0), cw = q("K1B", "20m", "CW", "BBB", "AAA", 0);
        assertEquals(5, ContestScorer.points(pl, ph, in("QRP", "FIXED")));
        assertEquals(5, ContestScorer.points(pl, cw, in("QRP", "FIXED")));
        assertEquals(1, ContestScorer.points(pl, ph, in("LOW", "FIXED")));
        assertEquals(2, ContestScorer.points(pl, cw, in("LOW", "FIXED")));
    }

    @Test void pointsByStationSetsEveryScoringQsosPoints() throws Exception {
        ContestPlugin pl = party("\"pointsByStation\":{\"MOBILE\":2,\"ROVER\":2}");
        QsoRecord a = q("K1A", "20m", "SSB", "BBB", "AAA", 0);
        assertEquals(2, ContestScorer.points(pl, a, in("LOW", "MOBILE")));
        assertEquals(2, ContestScorer.points(pl, a, in("LOW", "ROVER-UNLIMITED")), "ROVER-UNLIMITED counts as ROVER");
        assertEquals(1, ContestScorer.points(pl, a, in("LOW", "FIXED")));
    }

    // ---- mobile / rover county activation ---------------------------------------

    @Test void activationBonusPerOwnCountyWithEnoughQsos() throws Exception {
        ContestPlugin pl = party("\"activationBonus\":{\"points\":500,\"minQsos\":2,\"stations\":[\"MOBILE\",\"ROVER\"]}");
        List<QsoRecord> l = List.of(
                q("K1A", "20m", "CW", "BBB", "AAA", 1), q("K1B", "20m", "CW", "BBB", "AAA", 1),   // AAA: 2 QSOs → +500
                q("K1C", "20m", "CW", "CCC", "BBB", 1));                                          // BBB: 1 QSO → nothing
        int base = 3 * 2;                                                                         // 3 pts × {BBB, CCC}
        assertEquals(base + 500, ContestScorer.score(pl, l, in("LOW", "MOBILE"), "field1", NO_BANDS, 0).score());
        assertEquals(base, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score(), "fixed stations get no bonus");
    }

    @Test void activationNeedsDistinctStationsWhenAsked() throws Exception {
        ContestPlugin pl = party("\"activationBonus\":{\"points\":300,\"minQsos\":3,\"minCalls\":3,\"stations\":[\"MOBILE\"]}");
        List<QsoRecord> same = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1), q("K1A", "40m", "CW", "BBB", "AAA", 1), q("K1A", "80m", "CW", "BBB", "AAA", 1));
        List<QsoRecord> three = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1), q("K1B", "40m", "CW", "BBB", "AAA", 1), q("K1C", "80m", "CW", "BBB", "AAA", 1));
        assertEquals(3, ContestScorer.score(pl, same, in("LOW", "MOBILE"), "field1", NO_BANDS, 0).score(), "3 QSOs with one station: no bonus");
        assertEquals(3 + 300, ContestScorer.score(pl, three, in("LOW", "MOBILE"), "field1", NO_BANDS, 0).score());
    }

    @Test void activationAsMultiplierGivesTheCounty() throws Exception {
        ContestPlugin pl = party("\"activationBonus\":{\"minQsos\":2,\"asMultiplier\":true,\"stations\":[\"MOBILE\",\"PORTABLE\"]}");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1), q("K1B", "20m", "CW", "BBB", "AAA", 1));
        assertEquals(2 * 2, ContestScorer.score(pl, l, in("LOW", "PORTABLE"), "field1", NO_BANDS, 0).score(), "{BBB} + activated AAA");
        assertEquals(2 * 1, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score());
    }

    @Test void homeCountyIsAFreeMultiplierForInStateEntrants() throws Exception {
        ContestPlugin pl = party("\"homeCountyMult\":true");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1), q("K1B", "20m", "CW", "AAA", "CCC", 1));
        // worked BBB and AAA; own counties AAA (already worked) and CCC → {AAA, BBB, CCC}
        assertEquals(2 * 3, ContestScorer.score(pl, l, in("LOW", "MOBILE"), "field1", NO_BANDS, 0).score());
        StationContext out = StationContext.of("W1XX", "FN20", "ZZ", "LOW", "FIXED");      // out-of-state: no free county
        List<QsoRecord> outL = List.of(q("K1A", "20m", "CW", "BBB", "ZZ", 1));
        assertEquals(1, ContestScorer.score(pl, outL, out, "field1", NO_BANDS, 0).score());
    }

    // ---- dupes ---------------------------------------------------------------------

    @Test void ownCountyDupeLetsAMobileReworkFromANewCounty() throws Exception {
        ContestPlugin on = party("\"ownCountyDupe\":true"), off = party("");
        List<QsoRecord> prior = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 1));
        QsoRecord fromNewCounty = q("K1A", "20m", "CW", "BBB", "CCC", 1), sameCounty = q("K1A", "20m", "CW", "BBB", "AAA", 1);
        assertFalse(ContestScorer.isDupe(on, fromNewCounty, prior));
        assertTrue(ContestScorer.isDupe(on, sameCounty, prior));
        assertTrue(ContestScorer.isDupe(off, fromNewCounty, prior), "off: as before, the own county is not part of the key");
    }

    // ---- bonuses -------------------------------------------------------------------

    @Test void bonusStationsBelow50MhzOnly() throws Exception {
        ContestPlugin pl = party("\"bonusStations\":{\"W9FK\":100},\"bonusStationsBelow50Mhz\":true");
        List<QsoRecord> l = List.of(q("W9FK", "20m", "CW", "BBB", "AAA", 1), q("W9FK", "6m", "CW", "BBB", "AAA", 1), q("W9FK", "2m", "CW", "BBB", "AAA", 1));
        assertEquals(3 * 1 + 100, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score());
    }

    @Test void electronicLogAndTimeWindowAndSweepBonuses() throws Exception {
        ContestPlugin pl = party("\"electronicLogBonus\":100,"
                + "\"timeWindowBonus\":{\"bands\":[\"40m\",\"80m\"],\"windows\":[\"SAT 14:00-20:00\",\"SUN 14:00-20:00\"],\"pointsPerQso\":1,\"cap\":2},"
                + "\"bonusStationsOnce\":{\"N4H\":50,\"N4I\":50},\"bonusSweep\":{\"calls\":[\"N4H\",\"N4I\"],\"points\":200}");
        List<QsoRecord> l = new ArrayList<>();
        QsoRecord a = q("K1A", "40m", "CW", "BBB", "AAA", 1); a.setDateTimeUtc(LocalDateTime.of(2026, 4, 11, 14, 0)); l.add(a);   // Sat 14:00 in
        QsoRecord b = q("K1B", "80m", "CW", "BBB", "AAA", 1); b.setDateTimeUtc(LocalDateTime.of(2026, 4, 12, 19, 59)); l.add(b);  // Sun 19:59 in
        QsoRecord c = q("K1C", "80m", "CW", "BBB", "AAA", 1); c.setDateTimeUtc(LocalDateTime.of(2026, 4, 12, 15, 0)); l.add(c);   // in, over the cap
        QsoRecord d = q("K1D", "20m", "CW", "BBB", "AAA", 1); d.setDateTimeUtc(LocalDateTime.of(2026, 4, 11, 15, 0)); l.add(d);   // wrong band
        QsoRecord e = q("K1E", "40m", "CW", "BBB", "AAA", 1); e.setDateTimeUtc(LocalDateTime.of(2026, 4, 11, 20, 0)); l.add(e);   // 20:00 is out
        l.add(q("N4H", "20m", "CW", "BBB", "AAA", 1));
        ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0);
        int base = 6 * 1;
        assertEquals(base + 100 + 2 + 50, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score(), "one sweep call: no sweep");
        l.add(q("N4I", "20m", "CW", "BBB", "AAA", 1));
        assertEquals(7 + 100 + 2 + 100 + 200, ContestScorer.score(pl, l, in("LOW", "FIXED"), "field1", NO_BANDS, 0).score());
    }

    @Test void noNewRulesMeansTheScoreIsUnchanged() throws Exception {
        ContestPlugin pl = party("");
        List<QsoRecord> l = List.of(q("K1A", "20m", "CW", "BBB", "AAA", 2), q("K1B", "20m", "CW", "CCC", "BBB", 2));
        assertEquals(4 * 2, ContestScorer.score(pl, l, in("QRP", "MOBILE"), "field1", NO_BANDS, 0).score());
    }
}
