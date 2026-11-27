package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CallHistoryTest {
    @TempDir Path dir;
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

    @Test void importReadsACallHistoryFileAndALoggedValueBeatsAnImportedOne() throws Exception {
        Path f = dir.resolve("hist.txt");
        Files.writeString(f, "# a comment\n!!Order!!,Call,Name,Sect,State,CQZone,ITUZone,Grid,Other\n"
                + "1,K1ABC,BOB,CT,CT,5,8,FN31,x\n2,K2XYZ,AL,ENY,NY,5,8,FN12,y\n3,,NOCALL,,,,,,\n4,W9OLD,JOE,WI,WI,4,7,EN53,z\n");
        CallHistory.Result r = CallHistory.importFile(f);
        assertTrue(r.ok()); assertEquals(3, r.calls());
        assertEquals(3, CallHistory.count());
        assertEquals("BOB", CallHistory.get("K1ABC").get("name"));
        assertEquals("CT", CallHistory.get("K1ABC").get("section"));
        assertEquals("8", CallHistory.get("K1ABC").get("ituz"));
        CallHistory.learnFromAdif(List.of(Map.of("CALL", "K1ABC", "STATE", "NJ")));                // the operator logged him from a new state
        assertEquals("NJ", CallHistory.get("K1ABC").get("state"));
        CallHistory.importFile(f);                                                                 // importing again must not undo it
        assertEquals("NJ", CallHistory.get("K1ABC").get("state"));
    }

    @Test void aFileWithoutACallColumnIsRefused() throws Exception {
        Path f = dir.resolve("bad.csv");
        Files.writeString(f, "name,state\nBOB,CT\n");
        assertFalse(CallHistory.importFile(f).ok());
        assertFalse(CallHistory.importFile(dir.resolve("missing.csv")).ok());
        assertEquals(0, CallHistory.count());
    }

    @Test void anAdifImportTeachesNameStateGridAndZones() {
        List<Map<String, String>> recs = List.of(
                Map.of("CALL", "K1ABC", "NAME", "Bob", "STATE", "CT", "GRIDSQUARE", "FN31", "CQZ", "5", "ITUZ", "8"),
                Map.of("CALL", "N0COUNTY", "STATE", "Camden"), Map.of("NAME", "no call"));
        CallHistory.learnFromAdif(recs);
        Map<String, String> h = CallHistory.get("K1ABC");
        assertEquals("BOB", h.get("name")); assertEquals("CT", h.get("state")); assertEquals("FN31", h.get("grid"));
        assertEquals("5", h.get("cqz")); assertEquals("8", h.get("ituz"));
        assertTrue(CallHistory.get("N0COUNTY").isEmpty(), "a county in the STATE field is not kept as a state");
    }

    @Test void clearForgetsEverything() {
        CallHistory.learnFromAdif(List.of(Map.of("CALL", "K1ABC", "STATE", "CT")));
        assertEquals(1, CallHistory.count());
        CallHistory.clear();
        assertEquals(0, CallHistory.count());
        assertTrue(CallHistory.get("K1ABC").isEmpty());
    }
}
