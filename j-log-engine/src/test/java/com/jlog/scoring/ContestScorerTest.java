package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Golden tests for the extracted per-QSO scorer — the suite's first scoring
 * tests. Inputs are chosen to be robust to DXCC-table specifics (same-entity,
 * unresolvable, and non-entrant-vs-non-target cases) so they pin behavior
 * without depending on exact continent data.
 */
class ContestScorerTest {

    private final ObjectMapper M = new ObjectMapper();

    private ContestPlugin plugin(String json) throws Exception { return M.readValue(json, ContestPlugin.class); }

    private QsoRecord qso(String call, String band, String mode, String field1) {
        QsoRecord q = new QsoRecord();
        q.setCallsign(call); q.setBand(band); q.setMode(mode); q.setContestField1(field1);
        return q;
    }
    private final StationContext usCtx = StationContext.of("W1AW", "FN31", "");

    // ---- generic / table paths ----
    @Test void pointsPerQso() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"pointsPerQso\":3}}");
        assertEquals(3, ContestScorer.points(p, qso("K5XYZ", "20m", "CW", null), usCtx));
    }
    @Test void pointsByBand() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"pointsByBand\":{\"20m\":5,\"40m\":2}}}");
        assertEquals(5, ContestScorer.points(p, qso("K5XYZ", "20m", "CW", null), usCtx));
        assertEquals(2, ContestScorer.points(p, qso("K5XYZ", "40m", "CW", null), usCtx));
    }
    @Test void regionPair() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"pointsByRegionPair\":{\"US_DX\":5,\"US_US\":1}}}");
        assertEquals(5, ContestScorer.points(p, qso("DL1ABC", "20m", "CW", null), usCtx)); // US→DX
        assertEquals(1, ContestScorer.points(p, qso("W2ABC", "20m", "CW", null), usCtx));  // US→US
    }

    // ---- distance ----
    @Test void distanceUnknownFormulaFallsBackToBasePoints() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"entryFields\":[{\"id\":\"grid_rcvd\"}],"
            + "\"scoringRules\":{\"distanceScoring\":{\"formula\":\"bogus\",\"basePoints\":7}}}");
        assertEquals(7, ContestScorer.points(p, qso("K5XYZ", "20m", "CW", "FN31"), usCtx));
    }
    @Test void distanceInvalidGridScoresZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"entryFields\":[{\"id\":\"grid_rcvd\"}],"
            + "\"scoringRules\":{\"distanceScoring\":{\"formula\":\"km_x_bandfactor\"}}}");
        assertEquals(0, ContestScorer.points(p, qso("K5XYZ", "20m", "CW", "ZZ"), usCtx)); // bad grid → km<0
    }

    // ---- rookie ----
    @Test void rookieCurrentYearIsTwo_oldIsOne() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"entryFields\":[{\"id\":\"year_rcvd\"}],"
            + "\"scoringRules\":{\"rookieRoundupScoring\":true}}");
        int cur = LocalDateTime.now(ZoneOffset.UTC).getYear() % 100;
        String curYY = String.format("%02d", cur);
        String oldYY = String.format("%02d", (cur - 10 + 100) % 100);
        assertEquals(2, ContestScorer.points(p, qso("K5XYZ", "20m", "SSB", curYY), usCtx));
        assertEquals(1, ContestScorer.points(p, qso("K5XYZ", "20m", "SSB", oldYY), usCtx));
    }

    // ---- CQ family (robust cases) ----
    @Test void cqWwSameCallIsZero_unresolvableIsOne() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"zone_country\"}}");
        assertEquals(0, ContestScorer.points(p, qso("W1AW", "20m", "CW", null), usCtx));       // same entity
        assertEquals(1, ContestScorer.points(p, qso("99XYZ9", "20m", "CW", null), usCtx));     // unresolvable → default
    }

    // ---- entrant-asymmetric: non-entrant vs non-target → 0 (no DXCC continent dependency) ----
    @Test void allAsianNonAsianBothZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"all_asian\"}}");
        assertEquals(0, ContestScorer.points(p, qso("W2ABC", "20m", "CW", null), usCtx));
    }
    @Test void wagNonGermanBothZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"wag\"}}");
        assertEquals(0, ContestScorer.points(p, qso("W2ABC", "20m", "CW", null), usCtx));
    }
    @Test void sacNonScandBothZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"sac\"}}");
        assertEquals(0, ContestScorer.points(p, qso("W2ABC", "20m", "CW", null), usCtx));
    }
    @Test void oceaniaBandNotInTableZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"scoringRules\":{\"multiplierType\":\"oceania_dx\"}}");
        assertEquals(0, ContestScorer.points(p, qso("VK2ABC", "6m", "CW", null), usCtx)); // 6m not in band table
    }

    // ---- qso party ----
    @Test void qsoPartyInStateScoresModeClass_outOfStateZero() throws Exception {
        ContestPlugin p = plugin("{\"contestId\":\"X\",\"entryFields\":[{\"id\":\"state_prov_rcvd\"}],"
            + "\"scoringRules\":{\"multiplierType\":\"qso_party\",\"pointsPerQso\":1,"
            + "\"qsoParty\":{\"inStateCounties\":[\"KING\"],\"pointsByModeClass\":{\"PH\":2}}}}");
        StationContext inState = StationContext.of("W1AW", "FN31", "KING");
        // in-state op works an in-state county on phone → PH points (2)
        assertEquals(2, ContestScorer.points(p, qso("K7ABC", "20m", "SSB", "KING"), inState));
        // out-of-state op works an out-of-state station → no valid QSO (0)
        StationContext outState = StationContext.of("W1AW", "FN31", "ELSEWHERE");
        assertEquals(0, ContestScorer.points(p, qso("K7ABC", "20m", "SSB", "NOTACOUNTY"), outState));
    }
}
