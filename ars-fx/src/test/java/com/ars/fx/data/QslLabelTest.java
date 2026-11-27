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

    @Test void theQslLabelIsATableThenTheThanks() {
        var lines = QslLabel.layout(QslLabel.sample());
        assertEquals(java.util.List.of("To callsign", "Date", "UTC", "Band", "Mode", "RST"), lines.get(0).cells());
        assertEquals("K1ABC", lines.get(1).cells().get(0));
        assertEquals("20M", lines.get(1).cells().get(3));
        assertEquals("FT8", lines.get(1).cells().get(4));
        assertEquals("-10", lines.get(1).cells().get(5));
        assertEquals("73 and Good DX!", lines.get(2).text());
        assertTrue(lines.get(2).right());
        assertEquals(0.5, lines.get(2).scale(), 1e-9);
        double sum = 0; for (double c : lines.get(0).cols()) sum += c;
        assertEquals(1.0, sum, 1e-9);
    }

    @Test void aNormalLabelHasNoExchangeLine() {
        assertEquals("", QslLabel.sample().exchange());
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

    @Test void anAddressLabelHasNameAddressCityStateZip() {
        var lines = QslLabel.addressLayout(new CallsignLookup.Address("W1AW", "Hiram Maxim", "225 Main St", "Newington", "CT", "06111", "United States"));
        assertEquals("Hiram Maxim", lines.get(0).text());
        assertEquals("ARS W1AW", lines.get(1).text());
        assertEquals("225 Main St", lines.get(2).text());
        assertEquals("Newington, CT 06111", lines.get(3).text());
        assertEquals(4, lines.size());           // no country line for a US address
    }

    @Test void aForeignAddressLabelEndsWithTheCountry() {
        var lines = QslLabel.addressLayout(new CallsignLookup.Address("DL1ABC", "Hans", "Hauptstr 1", "Berlin", "", "10115", "Germany"));
        assertEquals("GERMANY", lines.get(lines.size() - 1).text());
        assertEquals("Berlin 10115", lines.get(3).text());
    }
}
