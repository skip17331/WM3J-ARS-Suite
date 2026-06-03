package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden tests for the extracted aggregate scorer ({@link ContestScorer#score}).
 * Pure over constructed QSO lists with pre-set stored points (the scorer sums
 * {@code getPoints()} for non-dupes, exactly as the old {@code totalPointsByContest}
 * SQL did). One test per representative multiplier family; callsign-derived
 * branches use stable entities (US/DE/FR/VK) so they don't depend on DXCC-table
 * minutiae. {@code count} is always the non-dupe QSO count.
 */
class ContestScorerScoreTest {

    private final ObjectMapper M = new ObjectMapper();
    private ContestPlugin p(String json) throws Exception { return M.readValue(json, ContestPlugin.class); }
    private final StationContext usCtx = StationContext.of("W1AW", "FN31", "");
    private static final List<String> NO_BANDS = List.of();

    private QsoRecord q(String call, String band, String mode, String f1, int pts, boolean dupe) {
        QsoRecord r = new QsoRecord();
        r.setCallsign(call); r.setBand(band); r.setMode(mode);
        r.setContestField1(f1); r.setPoints(pts); r.setDupe(dupe);
        return r;
    }

    @Test void defaultWorkedMults_distinctField1_dupesIgnored() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\"}");
        List<QsoRecord> qsos = List.of(
            q("W1AW", "20m", "CW", "MA", 2, false),
            q("K5XYZ", "40m", "CW", "TX", 3, false),
            q("N0XX", "20m", "CW", "MA", 2, false),   // MA again → no new mult
            q("DUPE", "20m", "CW", "CA", 99, true));  // dupe → excluded entirely
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 0);
        assertEquals(3, s.qsoCount());
        assertEquals(7, s.points());
        assertEquals(2, s.mults());       // {MA, TX}
        assertEquals(14, s.score());      // 7 × 2
        assertEquals(List.of("MA", "TX"), s.worked());
    }

    @Test void scoreIsPointsOnly_noMultiplier() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"scoringRules\":{\"scoreIsPointsOnly\":true}}");
        List<QsoRecord> qsos = List.of(
            q("W1AW", "20m", "CW", "ENE", 2, false),
            q("K5XYZ", "20m", "CW", "WMA", 2, false));
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 0);
        assertEquals(2, s.qsoCount());
        assertEquals(4, s.points());
        assertEquals(2, s.mults());       // worked sections still reported
        assertEquals(4, s.score());       // score == points (no multiply)
    }

    @Test void perMode_modeScopedMultsAndPoints() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"perModeMultipliers\":true}");
        List<QsoRecord> qsos = List.of(
            q("A", "20m", "CW", "1", 2, false),
            q("B", "40m", "CW", "2", 2, false),
            q("C", "20m", "Phone", "1", 3, false),
            q("D", "20m", "SSB", "9", 50, false));  // untracked mode: counted only in qsoCount
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 0);
        assertEquals(4, s.qsoCount());
        assertEquals(7, s.points());      // CW(2+2) + Phone(3); SSB ignored
        assertEquals(3, s.mults());       // CW{1,2}=2 + Phone{1}=1
        assertEquals(21, s.score());      // 7 × 3
        assertEquals(List.of("1", "2"), s.workedByMode().get("CW"));
        assertEquals(List.of("1"), s.workedByMode().get("Phone"));
    }

    @Test void perBandModel_distinctPerBand_pointsIncludeOffBand() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"multiplierModel\":{\"perBand\":true}}");
        List<QsoRecord> qsos = List.of(
            q("A", "20m", "CW", "FN31", 1, false),
            q("B", "20m", "CW", "FN42", 1, false),
            q("C", "40m", "CW", "FN31", 1, false),
            q("D", "15m", "CW", "XX", 5, false));   // off the contest band list
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", List.of("20m", "40m"), 0);
        assertEquals(4, s.qsoCount());
        assertEquals(8, s.points());      // all non-dupe points, incl. off-band 15m
        assertEquals(3, s.mults());       // 20m{FN31,FN42}=2 + 40m{FN31}=1
        assertEquals(24, s.score());      // 8 × 3
        assertEquals(List.of("FN31", "FN42"), s.workedByBand().get("20m"));
    }

    @Test void zoneCountry_zonePlusCountryPerBand() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"zone_country\"}}");
        List<QsoRecord> qsos = List.of(
            q("W1AW", "20m", "CW", "05", 1, false),
            q("DL1ABC", "20m", "CW", "14", 3, false));
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 0);
        assertEquals(2, s.qsoCount());
        assertEquals(4, s.points());
        assertEquals(4, s.mults());       // zones{05,14}=2 + countries{US,DE}=2 (same band)
        assertEquals(16, s.score());      // 4 × 4
        assertEquals(2, s.zonesWorked().size());
    }

    @Test void qsoParty_inStateCounties_withBonusStation() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"scoringRules\":{"
            + "\"multiplierType\":\"qso_party\",\"pointsPerQso\":1,"
            + "\"qsoParty\":{\"inStateCounties\":[\"KING\",\"PIER\"],\"inStateCountsCounties\":true,"
            + "\"multScope\":\"once\",\"bonusStations\":{\"W1AW\":100}}}}");
        StationContext inState = StationContext.of("W1AW", "FN31", "KING");
        List<QsoRecord> qsos = List.of(
            q("K7ABC", "20m", "SSB", "KING", 1, false),
            q("K7XYZ", "40m", "CW", "PIER", 1, false),
            q("N0DUP", "20m", "SSB", "KING", 1, false),   // KING again → no new mult
            q("W1AW", "20m", "SSB", "KING", 1, false));    // bonus station → +100
        ContestScore s = ContestScorer.score(pl, qsos, inState, "field1", NO_BANDS, 0);
        assertEquals(4, s.qsoCount());
        assertEquals(4, s.points());
        assertEquals(2, s.mults());        // {C|KING, C|PIER}
        assertEquals(108, s.score());      // 4 × 2 + 100 bonus
    }

    @Test void wae_scoreIncludesQtcPoints() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"wae\"}}");
        List<QsoRecord> qsos = List.of(
            q("DL1ABC", "20m", "CW", null, 1, false),
            q("F1ABC", "20m", "CW", null, 1, false));
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 3);
        assertEquals(2, s.qsoCount());
        assertEquals(2, s.points());
        assertTrue(s.mults() > 0);                                  // ≥1 weighted WAE token
        assertEquals((s.points() + 3) * s.mults(), s.score());      // (QSO + QTC) × weighted
    }

    @Test void oceaniaDx_nonOceaniaEntrant_countsOnlyOceaniaPrefixesPerBand() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"oceania_dx\"}}");
        List<QsoRecord> qsos = List.of(
            q("VK2ABC", "20m", "CW", null, 3, false),
            q("VK2XYZ", "40m", "CW", null, 2, false),   // same prefix VK2, different band
            q("W2ABC", "20m", "CW", null, 0, false));    // non-Oc↔non-Oc (Rule 4b): no mult
        ContestScore s = ContestScorer.score(pl, qsos, usCtx, "field1", NO_BANDS, 0);
        assertEquals(3, s.qsoCount());
        assertEquals(5, s.points());
        assertEquals(2, s.mults());        // {20m|VK2, 40m|VK2}
        assertEquals(10, s.score());       // 5 × 2
    }
}
