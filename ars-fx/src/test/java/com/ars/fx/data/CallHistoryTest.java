package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CallHistoryTest {
    @TempDir Path dir;
    static ContestPlugin ss, qp;

    @BeforeAll static void plugins() throws Exception { ss = MultCheckTest.load("arrl_sweepstakes_ssb.json"); qp = MultCheckTest.load("fl_qso_party.json"); }
    @BeforeEach void setUp() { CallHistory.setDirForTest(dir); }
    @AfterEach void tearDown() { CallHistory.setDirForTest(null); }

    @Test void suffixesAndPrefixesAreDroppedFromTheKey() {
        assertEquals("W1AW", CallHistory.baseCall("w1aw/4"));
        assertEquals("W1AW", CallHistory.baseCall("W1AW/P"));
        assertEquals("W1AW", CallHistory.baseCall("VE3/W1AW"));
        assertEquals("K1ABC", CallHistory.baseCall("KH6/K1ABC"));
        assertEquals("W1AW", CallHistory.baseCall(" W1AW "));
    }

    @Test void onlyBelievableValuesAreKept() {
        assertEquals("NJ", CallHistory.clean("state", " nj "));
        assertNull(CallHistory.clean("state", "CAMDEN"), "a county is not a state");
        assertNull(CallHistory.clean("state", "100"), "a DX station's power is not a state");
        assertEquals("ENY", CallHistory.clean("section", "eny"));
        assertEquals("5", CallHistory.clean("cqz", "05"));
        assertNull(CallHistory.clean("cqz", "41"));
        assertEquals("FN31", CallHistory.clean("grid", "fn31"));
        assertNull(CallHistory.clean("grid", "ZZ99"));
        assertEquals("BOB", CallHistory.clean("name", "bob"));
        assertNull(CallHistory.clean("name", "R2D2"));
        assertEquals("A", CallHistory.clean("prec", "a"));
        assertNull(CallHistory.clean("prec", "Z"));
    }

    @Test void contestFieldsMapToKeys() {
        assertEquals("section", CallHistory.keyForField("section"));
        assertEquals("state", CallHistory.keyForField("state_prov_rcvd"));
        assertEquals("cqz", CallHistory.keyForField("cq_zone"));
        assertNull(CallHistory.keyForField("rst_rcvd"));
        assertNull(CallHistory.keyForField("power_rcvd"));
    }

    @Test void aLoggedSweepstakesQsoIsRememberedByCall() {
        QsoRecord r = MultCheckTest.q(ss, "K1ABC/P", "20m", "SSB", "section", "CT");
        r.setContestField1(null);
        CallHistory.learn(ss, r);
        Map<String, String> h = CallHistory.get("K1ABC");
        assertEquals("CT", h.get("section"));
        assertEquals(1, CallHistory.count());
        assertTrue(CallHistory.get("N0BODY").isEmpty());
    }

    @Test void historyLetsAnExchangeMultiplierBeJudgedBeforeTheExchangeIsHeard() {
        QsoRecord seen = MultCheckTest.q(ss, "K2XYZ", "20m", "SSB", "section", "ENY");
        CallHistory.learn(ss, seen);
        List<QsoRecord> log = new ArrayList<>(List.of(MultCheckTest.q(ss, "K1ABC", "20m", "SSB", "section", "CT")));
        QsoRecord spot = MultCheck.candidateOf(ss, "K2XYZ", "20m", "SSB");
        assertFalse(MultCheck.isNewMult(ss, log, spot, MultCheckTest.CTX), "without the exchange nothing is known");
        String slot = ss.fieldSlotColumn("section");
        assertNotNull(slot);
        String sec = CallHistory.get("K2XYZ").get("section");
        assertEquals("ENY", sec);
        QsoRecord withHistory = MultCheckTest.q(ss, "K2XYZ", "20m", "SSB", "section", sec);
        assertTrue(MultCheck.isNewMult(ss, log, withHistory, MultCheckTest.CTX));
    }

    @Test void importReadsAnN1mmStyleFileAndALoggedValueBeatsAnImportedOne() throws Exception {
        Path f = dir.resolve("hist.txt");
        Files.writeString(f, "# a comment\n!!Order!!,Call,Name,Sect,State,CQZone,ITUZone,Grid,Other\n"
                + "1,K1ABC,BOB,CT,CT,5,8,FN31,x\n2,K2XYZ,AL,ENY,NY,5,8,FN12,y\n3,,NOCALL,,,,,,\n4,W9OLD,JOE,WI,WI,4,7,EN53,z\n");
        CallHistory.Result r = CallHistory.importFile(f);
        assertTrue(r.ok()); assertEquals(3, r.calls());
        assertEquals(3, CallHistory.count());
        assertEquals("BOB", CallHistory.get("K1ABC").get("name"));
        assertEquals("CT", CallHistory.get("K1ABC").get("section"));
        assertEquals("8", CallHistory.get("K1ABC").get("ituz"));
        CallHistory.learn(ss, MultCheckTest.q(ss, "K1ABC", "20m", "SSB", "section", "NNJ"));      // the operator logged him from a new section
        assertEquals("NNJ", CallHistory.get("K1ABC").get("section"));
        CallHistory.importFile(f);                                                                 // importing again must not undo it
        assertEquals("NNJ", CallHistory.get("K1ABC").get("section"));
    }

    @Test void aFileWithoutACallColumnIsRefused() throws Exception {
        Path f = dir.resolve("bad.csv");
        Files.writeString(f, "name,state\nBOB,CT\n");
        assertFalse(CallHistory.importFile(f).ok());
        assertFalse(CallHistory.importFile(dir.resolve("missing.csv")).ok());
        assertEquals(0, CallHistory.count());
    }

    @Test void clearForgetsEverything() {
        CallHistory.learn(ss, MultCheckTest.q(ss, "K1ABC", "20m", "SSB", "section", "CT"));
        assertEquals(1, CallHistory.count());
        CallHistory.clear();
        assertEquals(0, CallHistory.count());
        assertTrue(CallHistory.get("K1ABC").isEmpty());
    }
}
