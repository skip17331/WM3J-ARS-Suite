package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallSimilarityTest {
    @Test void anyThreeContiguousCharactersMatch() {
        assertTrue(CallSimilarity.score("KC8RHC", "KC8XYZ") > 0);   // KC8
        assertTrue(CallSimilarity.score("KC8RHC", "W1C8RQ") > 0);   // C8R
        assertTrue(CallSimilarity.score("KC8RHC", "N2RHCA") > 0);   // RHC
        assertTrue(CallSimilarity.score("KC8RHC", "AB8RHQ") > 0);   // 8RH
    }
    @Test void betterMatchesScoreHigher() {
        assertEquals(4, CallSimilarity.score("KC8RHC", "KC8RHC"));
        assertEquals(1, CallSimilarity.score("KC8RHC", "KC8XYZ"));
    }
    @Test void noMatch() {
        assertEquals(0, CallSimilarity.score("KC8RHC", "W3ABC"));
        assertEquals(0, CallSimilarity.score("KC", "KC8RHC"));      // too short to compare
        assertEquals(0, CallSimilarity.score(null, "KC8RHC"));
    }
    @Test void caseDoesNotMatter() { assertTrue(CallSimilarity.score("kc8rhc", "n2rhca") > 0); }
}
