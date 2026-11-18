package com.jlog.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** The shared models' small rules. */
class ModelTest {
    @Test
    void translationPicksTheLanguageAndFallsBackToEnglishOnlyForNoCode() {
        Translation t = new Translation();
        t.setEnglish("Thank you"); t.setSpanish("Gracias"); t.setGerman("Danke"); t.setPortuguese("Obrigado");
        assertEquals("Gracias", t.forLanguage("es"));
        assertEquals("Danke", t.forLanguage("de"));
        assertEquals("Obrigado", t.forLanguage("pt"));
        assertEquals("Thank you", t.forLanguage("en"));
        assertEquals("Thank you", t.forLanguage(null));
        assertNull(t.forLanguage("fr"), "an unknown language is empty, not English");
    }

    @Test
    void qtcCallsAreUpperCasedAndTrimmed() {
        QtcRecord q = new QtcRecord();
        q.setPartnerCall("  dl1abc "); q.setQsoCall("k1abc/p ");
        assertEquals("DL1ABC", q.getPartnerCall());
        assertEquals("K1ABC/P", q.getQsoCall());
        q.setQsoCall(null);
        assertEquals("", q.getQsoCall());
    }

    @Test
    void macroKeepsItsActionsInOrder() {
        Macro m = new Macro();
        m.setName("CQ"); m.setFKey(1);
        m.addAction(new Macro.MacroAction(Macro.ActionType.PTT_ON, null));
        m.addAction(new Macro.MacroAction(Macro.ActionType.DELAY_MS, 250));
        m.addAction(new Macro.MacroAction(Macro.ActionType.CW_TEXT, "CQ TEST"));
        assertEquals(3, m.getActions().size());
        assertEquals(250, m.getActions().get(1).getIntData());
        assertEquals("CQ TEST", m.getActions().get(2).getData());
        assertEquals("Macro{name=CQ, fKey=F1, actions=3}", m.toString());
    }
}
