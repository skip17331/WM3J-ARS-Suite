package com.jlog.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pure dupe rules over a prior-QSO list — no DB. One test per dispatch branch. */
class ContestScorerDupeTest {

    private final ObjectMapper M = new ObjectMapper();
    private ContestPlugin p(String json) throws Exception { return M.readValue(json, ContestPlugin.class); }

    private QsoRecord r(String call, String band, String mode, String f1, boolean dupe) {
        QsoRecord q = new QsoRecord();
        q.setCallsign(call); q.setBand(band); q.setMode(mode); q.setContestField1(f1); q.setDupe(dupe);
        return q;
    }
    private QsoRecord cand(String call, String band, String mode, String f1) { return r(call, band, mode, f1, false); }

    @Test void contestWide() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"contestWideDupe\":true}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","40m","SSB",null), List.of(r("W1AW","20m","CW",null,false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","40m","SSB",null), List.of()));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","40m","SSB",null), List.of(r("W1AW","20m","CW",null,true)))); // prior already dupe
    }

    @Test void roverAwareNonRoverIsPerBand() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"roverAwareDupe\":true}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","20m","CW",null), List.of(r("W1AW","20m","SSB",null,false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","40m","CW",null), List.of(r("W1AW","20m","CW",null,false))));
    }

    @Test void roverAwareRoverIsBandGrid() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"roverAwareDupe\":true,"
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"grid_rcvd\"}],"
            + "\"multiplierModel\":{\"field\":\"grid_rcvd\"}}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW/R","20m","CW","FN31"), List.of(r("W1AW/R","20m","CW","FN31",false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW/R","20m","CW","FN42"), List.of(r("W1AW/R","20m","CW","FN31",false)))); // new grid
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW/R","40m","CW","FN31"), List.of(r("W1AW/R","20m","CW","FN31",false)))); // new band
    }

    @Test void perBandGrid() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"perBandGridDupe\":true,"
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"grid_rcvd\"}],"
            + "\"multiplierModel\":{\"field\":\"grid_rcvd\"}}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","20m","CW","FN31"), List.of(r("W1AW","20m","CW","FN31",false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","CW","FN42"), List.of(r("W1AW","20m","CW","FN31",false))));
    }

    @Test void perMode() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"perModeMultipliers\":true}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","40m","CW",null), List.of(r("W1AW","20m","CW",null,false)))); // band-agnostic
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","CW",null), List.of(r("W1AW","20m","SSB",null,false))));
    }

    @Test void fieldDayModeClass() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\",\"fieldDayModeDupe\":true}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","20m","SSB",null), List.of(r("W1AW","20m","USB",null,false)))); // both PH
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","CW",null),  List.of(r("W1AW","20m","USB",null,false)))); // CW vs PH
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","40m","SSB",null), List.of(r("W1AW","20m","USB",null,false)))); // new band
    }

    @Test void qsoParty() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\","
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"state_prov_rcvd\"}],"
            + "\"scoringRules\":{\"multiplierType\":\"qso_party\",\"qsoParty\":{}}}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","20m","SSB","KING"), List.of(r("W1AW","20m","USB","KING",false)))); // PH+county
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","CW","KING"),  List.of(r("W1AW","20m","USB","KING",false)))); // CW vs PH
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","SSB","PIER"), List.of(r("W1AW","20m","USB","KING",false)))); // new county
    }

    @Test void defaultBandMode() throws Exception {
        ContestPlugin pl = p("{\"contestId\":\"X\"}");
        assertTrue (ContestScorer.isDupe(pl, cand("W1AW","20m","CW",null), List.of(r("W1AW","20m","CW",null,false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","20m","SSB",null), List.of(r("W1AW","20m","CW",null,false))));
        assertFalse(ContestScorer.isDupe(pl, cand("W1AW","40m","CW",null), List.of(r("W1AW","20m","CW",null,false))));
    }
}
