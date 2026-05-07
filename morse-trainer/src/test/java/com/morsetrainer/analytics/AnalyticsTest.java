package com.morsetrainer.analytics;

import com.morsetrainer.decoder.DecodedElement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AnalyticsTest {

    @Test
    void scoringEngineTracksAccuracy() {
        ScoringEngine s = new ScoringEngine();
        s.presented('A'); s.typed('A', 'A');
        s.presented('B'); s.typed('B', 'X');
        s.presented('A'); s.typed('A', 'A');

        assertEquals(3, s.totalCharacters());
        assertEquals(1, s.totalErrors());
        assertEquals(2.0 / 3.0, s.overallAccuracy(), 0.001);

        var perChar = s.perCharacter();
        assertEquals(1.0, perChar.get('A').accuracy(), 0.001);
        assertEquals(0.0, perChar.get('B').accuracy(), 0.001);
    }

    @Test
    void recommendsWorstCharacters() {
        ScoringEngine s = new ScoringEngine();
        for (int i = 0; i < 5; i++) { s.presented('Q'); s.typed('Q', 'X'); }
        for (int i = 0; i < 5; i++) { s.presented('E'); s.typed('E', 'E'); }
        List<Character> rec = s.recommendedDrills(3);
        assertTrue(rec.contains('Q'));
        assertFalse(rec.contains('E'));
    }

    @Test
    void diagnosticsComputesDitDahRatio() {
        SendingDiagnostics d = new SendingDiagnostics();
        var elements = List.of(
                new DecodedElement(DecodedElement.Kind.DIT, 60, 0),
                new DecodedElement(DecodedElement.Kind.DIT, 62, 0),
                new DecodedElement(DecodedElement.Kind.DAH, 180, 0),
                new DecodedElement(DecodedElement.Kind.DAH, 182, 0)
        );
        SendingDiagnostics.Result r = d.analyse(elements);
        assertEquals(3.0, r.ditDahRatio, 0.1);
        assertEquals(20.0, r.achievedWpm, 1.0);
        assertTrue(r.consistencyScore > 90);
    }

    @Test
    void diagnosticsFlagsBadRatio() {
        SendingDiagnostics d = new SendingDiagnostics();
        var elements = List.of(
                new DecodedElement(DecodedElement.Kind.DIT, 60, 0),
                new DecodedElement(DecodedElement.Kind.DAH, 100, 0)
        );
        SendingDiagnostics.Result r = d.analyse(elements);
        assertTrue(r.issues.stream().anyMatch(i -> i.code().equals("BAD_DIT_DAH_RATIO")));
    }

    @Test
    void troubleReportRanksWorstChars() {
        SendingDiagnostics d = new SendingDiagnostics();
        var rep = d.buildTroubleReport(
                "AABBBC", "AAXXBC",
                Map.of('A', List.of(60L, 62L)),
                Map.of('A', List.of(180L, 182L)));
        assertTrue(rep.worstByMiscode.contains('B'));
    }
}
