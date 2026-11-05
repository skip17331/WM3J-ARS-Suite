package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class QslLabelTest {
    @Test void zplHasCallAndFramesAndPrintQuantity() {
        QslLabel.Model m = QslLabel.model("zebra-zd421");
        String z = QslLabel.zpl(QslLabel.sample(), m.sizes().get(3), 203, 2);   // 3x2 in
        assertTrue(z.startsWith("^XA"));
        assertTrue(z.contains("K1ABC"));
        assertTrue(z.contains("^PW609^LL406"));
        assertTrue(z.trim().endsWith("^PQ2^XZ"));
    }

    @Test void everyModelHasSizesAndPositiveDimensions() {
        for (QslLabel.Model m : QslLabel.MODELS) {
            assertFalse(m.sizes().isEmpty(), m.id());
            for (QslLabel.Size s : m.sizes()) assertTrue(s.wMm() > 0 && s.hMm() > 0, m.id() + "/" + s.id());
        }
    }

    @Test void unknownModelFallsBackToFirst() {
        assertEquals(QslLabel.MODELS.get(0), QslLabel.model("nope"));
    }
}
