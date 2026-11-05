package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.QsoPartySide.Side;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.ContestScorer;
import com.jlog.scoring.StationContext;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class QsoPartySideTest {
    static ContestPlugin vt, oh, ss;
    private String oldHome;

    static ContestPlugin load(String f) throws Exception {
        return new ObjectMapper().readValue(Path.of("..", "j-log-engine", "src", "main", "resources", "com", "jlog", "plugins", f).toFile(), ContestPlugin.class);
    }
    @BeforeAll static void plugins() throws Exception { vt = load("vt_qso_party.json"); oh = load("oh_qso_party.json"); ss = load("arrl_sweepstakes_ssb.json"); }
    @BeforeEach void home(@TempDir Path tmp) { oldHome = System.getProperty("user.home"); System.setProperty("user.home", tmp.toString()); HubConfig.reload(); }
    @AfterEach void restore() { System.setProperty("user.home", oldHome); HubConfig.reload(); }

    static String vtCounty() { return vt.getScoringRules().getQsoParty().getInStateCounties().get(0); }

    @Test void onlyStatePartiesAreParties() {
        assertTrue(QsoPartySide.isParty(vt)); assertTrue(QsoPartySide.isParty(oh));
        assertFalse(QsoPartySide.isParty(ss)); assertFalse(QsoPartySide.isParty(null));
        assertEquals("VT", QsoPartySide.partyState(vt));
        assertNull(QsoPartySide.side(ss, "CT"), "Sweepstakes has no sides");
    }

    @Test void theSideIsDecidedTheWayTheEngineDecidesIt() {
        // VT lists its 14 counties; OH treats any code that is not DX and not two letters as a county
        assertEquals(Side.IN_STATE, QsoPartySide.side(vt, vtCounty()));
        assertEquals(Side.OUT_OF_STATE, QsoPartySide.side(vt, "MA"));
        assertEquals(Side.OUT_OF_STATE, QsoPartySide.side(vt, "DX"));
        assertEquals(Side.IN_STATE, QsoPartySide.side(oh, "FRA"));
        assertEquals(Side.OUT_OF_STATE, QsoPartySide.side(oh, "MI"));
        assertNull(QsoPartySide.side(vt, ""), "nothing set yet");
        assertEquals(Side.IN_STATE, QsoPartySide.side(vt, " " + vtCounty().toLowerCase() + " "), "case and spaces do not matter");
    }

    @Test void theSentQthIsKeptPerPartyWithTheSharedAnswerAsFallback() {
        assertEquals("", QsoPartySide.sentQth(vt));
        HubConfig.set("contest.sent.state_prov_sent", "ma");                    // the old shared answer
        assertEquals("MA", QsoPartySide.sentQth(vt));
        QsoPartySide.saveSentQth(vt, vtCounty());
        assertEquals(vtCounty(), QsoPartySide.sentQth(vt), "this party has its own");
        assertEquals("MA", QsoPartySide.sentQth(oh), "another party is not affected");
        assertEquals("", QsoPartySide.sentQth(ss), "not a party");
    }

    @Test void whatEachSideCanReceive() {
        var out = QsoPartySide.receivable(vt, Side.OUT_OF_STATE);
        assertTrue(out.contains(vtCounty())); assertFalse(out.contains("MA")); assertFalse(out.contains("DX"));
        var in = QsoPartySide.receivable(vt, Side.IN_STATE);
        assertTrue(in.contains(vtCounty()) && in.contains("MA") && in.contains("ON") && in.contains("DX"));
    }

    @Test void whatIsLeftToWorkDependsOnTheSide() {
        List<String> out = QsoPartySide.multUniverse(vt, Side.OUT_OF_STATE);
        assertEquals(14, out.size(), "from outside Vermont: its 14 counties");
        List<String> in = QsoPartySide.multUniverse(vt, Side.IN_STATE);
        assertTrue(in.contains("MA") && in.contains("ON"), "from inside: states and provinces");
        var cfg = vt.getScoringRules().getQsoParty();
        assertEquals(!cfg.isInStateNoDxMult() && !cfg.isInStateCountsDxccEach(), in.contains("DX"),
                "a single DX entry only where the party counts DX as one; Vermont counts each DX country, which cannot be listed");
        assertFalse(in.contains("VT"), "not your own state");
        assertNull(QsoPartySide.multUniverse(ss, Side.IN_STATE));
        assertNull(QsoPartySide.multUniverse(vt, null));
    }

    // ---- the reason it matters: the engine scores the two sides differently, by the sent QTH ----
    static QsoRecord qso(String call, String state) {
        QsoRecord r = MultCheck.candidateOf(vt, call, "20m", "SSB");
        r.setRstReceived("59");
        String slot = vt.fieldSlotColumn("state_prov_rcvd");
        if ("field1".equals(slot)) r.setContestField1(state); else if ("field2".equals(slot)) r.setContestField2(state);
        else if ("field3".equals(slot)) r.setContestField3(state); else if ("field4".equals(slot)) r.setContestField4(state);
        return r;
    }
    /** As the app does it: each QSO's points are worked out and stored when it is logged, then the log is scored. */
    static ContestScore score(String sentQth, List<QsoRecord> log) {
        StationContext ctx = StationContext.of("WM3J", "FN20", sentQth);
        for (QsoRecord q : log) q.setPoints(ContestScorer.points(vt, q, ctx));
        return ContestScorer.score(vt, log, ctx, vt.computeMultiplierDbColumn(), List.of("20m"), 0);
    }

    @Test void anInStateStationScoresContactsWithOutOfStateStationsAnOutOfStateOneDoesNot() {
        List<QsoRecord> log = List.of(qso("W1AW", "MA"));                       // a Massachusetts station
        assertTrue(score(vtCounty(), log).points() > 0, "in Vermont, working Massachusetts counts");
        assertEquals(0, score("MA", log).points(), "outside Vermont, working another outside station does not");
        assertEquals(0, score("EPA", log).points(), "what the app used to pass: the Sweepstakes section, which the engine reads as out of state");
        List<QsoRecord> toVermont = List.of(qso("W1ABC", vtCounty()));
        assertTrue(score("MA", toVermont).points() > 0, "outside Vermont, working a Vermont county counts");
    }
}
