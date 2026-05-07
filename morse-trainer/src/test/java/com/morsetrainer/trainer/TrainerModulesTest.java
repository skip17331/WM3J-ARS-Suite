package com.morsetrainer.trainer;

import com.morsetrainer.core.AppConfig;
import com.morsetrainer.trainer.groups.GroupSession;
import com.morsetrainer.trainer.letters.LetterSession;
import com.morsetrainer.trainer.qso.QsoGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class TrainerModulesTest {

    @Test
    void letterSessionRespectsAlphabet() {
        LetterSession s = new LetterSession(List.of('A', 'B'), 50, new Random(42));
        while (s.hasNext()) {
            char c = s.next();
            assertTrue(c == 'A' || c == 'B', "Unexpected char " + c);
        }
    }

    @Test
    void kochLevelUsesPrefixOfOrder() {
        AppConfig cfg = new AppConfig();
        cfg.kochOrder = "KMURESN";
        LetterSession s = LetterSession.kochLevel(3, 30, cfg, new Random(1));
        assertEquals(List.of('K', 'M', 'U'), s.alphabet());
    }

    @Test
    void groupSessionLengthsInRange() {
        GroupSession s = new GroupSession(
                List.of('A', 'B', 'C'), 2, 4, 30, new Random(7));
        for (String g : s.generateAll()) {
            assertTrue(g.length() >= 2 && g.length() <= 4);
            for (char c : g.toCharArray()) assertTrue("ABC".indexOf(c) >= 0);
        }
    }

    @Test
    void qsoGeneratorProducesUppercaseTokens() {
        QsoGenerator g = new QsoGenerator(new Random(1));
        for (var d : QsoGenerator.Difficulty.values()) {
            String qso = g.generate(d);
            assertNotNull(qso);
            assertFalse(qso.isEmpty());
            assertEquals(qso.toUpperCase(), qso, "QSO should be all uppercase");
        }
    }

    @Test
    void contestQsoHasSerialNumber() {
        QsoGenerator g = new QsoGenerator(new Random(1));
        String qso = g.generate(QsoGenerator.Difficulty.CONTEST);
        assertTrue(qso.contains("5NN"));
    }
}
