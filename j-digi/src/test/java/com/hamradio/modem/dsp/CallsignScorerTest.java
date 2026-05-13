package com.hamradio.modem.dsp;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the callsign extractor + scorer against representative CW
 * operating phrases. The scorer is stateless — each test feeds a
 * snippet and asserts which calls fall out and what the confidence
 * looks like.
 */
class CallsignScorerTest {

    private static CallsignScorer.Candidate findCall(List<CallsignScorer.Candidate> list, String call) {
        return list.stream().filter(c -> c.callsign.equals(call)).findFirst()
                   .orElseThrow(() -> new AssertionError("Expected " + call + " in " + list));
    }

    @Test
    void bareCallsignGetsBaseConfidence() {
        List<CallsignScorer.Candidate> out = CallsignScorer.score("W1AW");
        assertEquals(1, out.size());
        CallsignScorer.Candidate c = out.get(0);
        assertEquals("W1AW", c.callsign);
        assertEquals(CallsignScorer.BASE_CONFIDENCE, c.confidence, 0.001);
        assertEquals("BARE", c.context);
        assertEquals(1, c.repetitions);
    }

    @Test
    void repeatedCallsignBoostsConfidence() {
        // "CQ DE W1AW W1AW K" is the canonical pattern.
        List<CallsignScorer.Candidate> out = CallsignScorer.score("CQ DE W1AW W1AW K");
        CallsignScorer.Candidate w1aw = findCall(out, "W1AW");
        // Base + REP + CTX(DE) + EOT(K) — should clear 0.9.
        assertTrue(w1aw.confidence > 0.9,
            "expected confidence > 0.9 for CQ/DE/RPT/EOT, got " + w1aw.confidence);
        assertEquals(2, w1aw.repetitions);
    }

    @Test
    void cqContextBoostsBareMatch() {
        // Single occurrence but with CQ prefix — should beat BASE.
        List<CallsignScorer.Candidate> out = CallsignScorer.score("CQ CQ DE W1AW");
        CallsignScorer.Candidate c = findCall(out, "W1AW");
        assertTrue(c.confidence > CallsignScorer.BASE_CONFIDENCE,
            "context should boost above bare base, got " + c.confidence);
        // Single occurrence so context label should reflect CQ/DE, not RPT.
        assertTrue(c.context.equals("CQ") || c.context.equals("DE"),
            "expected CQ or DE context label, got " + c.context);
    }

    @Test
    void qSignalsDoNotMatch() {
        // Q-signals are 3-letter strings without a district digit, so
        // the callsign regex shouldn't latch onto them.
        for (String q : new String[]{"QRZ", "QSL", "QSO", "QRT", "QSY", "QTH", "QRM", "QRN"}) {
            List<CallsignScorer.Candidate> out = CallsignScorer.score(q);
            assertTrue(out.isEmpty(),
                "expected no match for Q-signal " + q + ", got " + out);
        }
    }

    @Test
    void specialDigitPrefixMatches() {
        // 2E0XXX is a valid UK Intermediate licensee; 9A1A is Croatia.
        // Both have digit-led 2-character prefixes.
        for (String call : new String[]{"2E0ABC", "9A1A", "3D2AG", "4X4OK"}) {
            List<CallsignScorer.Candidate> out = CallsignScorer.score("DE " + call + " K");
            assertEquals(1, out.size(),
                "expected exactly one match for " + call + ", got " + out);
            assertEquals(call, out.get(0).callsign);
        }
    }

    @Test
    void portableSuffixMatches() {
        // /P, /M, /MM are all real portable suffixes; /4 is a district override.
        for (String call : new String[]{"W1AW/P", "K3LR/MM", "W1AW/4", "VE3ABC/QRP"}) {
            List<CallsignScorer.Candidate> out = CallsignScorer.score(call);
            assertEquals(1, out.size(), "expected match for " + call + ", got " + out);
            assertEquals(call, out.get(0).callsign);
        }
    }

    @Test
    void multipleCallsExtractedFromSameText() {
        // "W1AW DE K3LR K" — both calls real, neither repeated.
        List<CallsignScorer.Candidate> out = CallsignScorer.score("W1AW DE K3LR K");
        assertEquals(2, out.size());
        findCall(out, "W1AW");
        findCall(out, "K3LR");
    }

    @Test
    void garbageRejected() {
        // Random letter / digit soup with no valid call pattern.
        for (String junk : new String[]{
                "ABCDEFG", "12345", "ZZZZZ", "XYZ XYZ", "K K K K", "A1 B2 C3"}) {
            List<CallsignScorer.Candidate> out = CallsignScorer.score(junk);
            assertTrue(out.isEmpty(),
                "expected no match for garbage '" + junk + "', got " + out);
        }
    }

    @Test
    void repetitionAndContextBothScoreSeparately() {
        // "CQ W1AW W1AW" gets BOTH repetition AND CQ context — should be
        // a higher confidence than either bonus alone.
        double both    = findCall(CallsignScorer.score("CQ W1AW W1AW"), "W1AW").confidence;
        double repOnly = findCall(CallsignScorer.score("W1AW W1AW"),    "W1AW").confidence;
        double ctxOnly = findCall(CallsignScorer.score("CQ DE W1AW"),   "W1AW").confidence;
        assertTrue(both > repOnly, "both bonuses should beat rep-only");
        assertTrue(both > ctxOnly, "both bonuses should beat ctx-only");
    }

    @Test
    void confidenceCapped() {
        // A pathological string that would otherwise push confidence
        // past 1.0 — verify the cap holds.
        String spammy = "CQ DE W1AW W1AW W1AW W1AW W1AW K KN SK";
        CallsignScorer.Candidate c = findCall(CallsignScorer.score(spammy), "W1AW");
        assertTrue(c.confidence <= CallsignScorer.MAX_CONFIDENCE,
            "confidence must not exceed MAX_CONFIDENCE, got " + c.confidence);
    }

    @Test
    void sortedByConfidenceDescending() {
        // K3LR appears once bare; W1AW appears twice with CQ context —
        // W1AW should sort first.
        List<CallsignScorer.Candidate> out =
            CallsignScorer.score("CQ DE W1AW W1AW K3LR");
        assertEquals("W1AW", out.get(0).callsign);
        assertTrue(out.get(0).confidence > out.get(1).confidence);
    }
}
