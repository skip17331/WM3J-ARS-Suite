package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallSimilarityTest {
    /** The report: W0MET is not NW0M, though they share "W0M". */
    @Test void aWholeCallThatOnlySharesAPieceIsNotSimilar() {
        assertEquals(0, CallSimilarity.score("W0MET", "NW0M"));
        assertEquals(0, CallSimilarity.score("KC8RHC", "KC8XYZ"));
        assertEquals(0, CallSimilarity.score("KC8RHC", "N2RHCA"));
    }
    @Test void aWholeCallOneLetterOffIsSimilar() {
        assertTrue(CallSimilarity.score("KC8RHC", "KC8RHQ") > 0);    // wrong letter
        assertTrue(CallSimilarity.score("KC8RHC", "KC8RH") > 0);     // missing the end
        assertTrue(CallSimilarity.score("KC8RHC", "KC8RHCC") > 0);   // an extra letter
        assertTrue(CallSimilarity.score("C8RHC", "KC8RHC") > 0);     // missing the front
        assertTrue(CallSimilarity.score("W0MET", "W0MEX") > 0);
    }
    @Test void aFragmentMatchesInsideACall() {
        assertTrue(CallSimilarity.score("W0M", "NW0M") > 0);
        assertTrue(CallSimilarity.score("RHC", "KC8RHC") > 0);
        assertTrue(CallSimilarity.score("8RHC", "KC8RHC") > 0);
    }
    @Test void aCallStillBeingTypedMatchesItsStart() {
        assertTrue(CallSimilarity.score("KC8R", "KC8RHC") > 0);
        assertTrue(CallSimilarity.score("KC8RH", "KC8RHC") > 0);
    }
    @Test void betterMatchesScoreHigher() {
        assertEquals(4, CallSimilarity.score("KC8RHC", "KC8RHC"));
        assertTrue(CallSimilarity.score("KC8R", "KC8RHC") > CallSimilarity.score("RHC", "KC8RHC"));
    }
    @Test void portableSuffixIsIgnored() {
        assertTrue(CallSimilarity.score("W0MET/P", "W0MET") > 0);
        assertTrue(CallSimilarity.score("W0MET", "W0MET/4") > 0);
    }
    @Test void noMatch() {
        assertEquals(0, CallSimilarity.score("KC8RHC", "W3ABC"));
        assertEquals(0, CallSimilarity.score("KC", "KC8RHC"));      // too short to compare
        assertEquals(0, CallSimilarity.score(null, "KC8RHC"));
    }
    @Test void caseDoesNotMatter() { assertTrue(CallSimilarity.score("kc8rhq", "kc8rhc") > 0); }
}
