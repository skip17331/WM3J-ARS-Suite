package com.jlog.scoring;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The country files' exact-call entries win over prefixes. These all used to score as the prefix's country. */
class DxccResolverExactCallTest {

    private static final DxccResolver R = DxccResolver.getInstance();

    private static void is(String call, String dxcc, String continent) {
        DxccResolver.Entity e = R.resolve(call);
        assertEquals(dxcc + "/" + continent, e == null ? null : e.id() + "/" + e.continent(), call);
    }

    @Test void unitedNationsAndItu() {
        is("4U1UN", "289", "NA");     // was Italy, EU
        is("4U1ITU", "117", "EU");    // was Italy
    }

    @Test void otherEntitiesOnlyAnExactCallReaches() {
        is("BS7H", "506", "AS");      // Scarborough Reef, was China
        is("TX5S", "36", "NA");       // Clipperton, was France, EU
        is("KC4USV", "13", "SA");     // Antarctica, was the USA, NA
    }

    @Test void portableSuffixStillFindsTheExactCall() {
        is("4U1UN/P", "289", "NA");
        is("4u1un", "289", "NA");
    }

    @Test void slashCallsAsWritten() {
        is("KC4/W1WCN", "13", "SA");
    }

    @Test void ordinaryCallsAreUnchanged() {
        is("W1AW", "291", "NA");
        is("I2ABC", "248", "EU");
        is("4U1ABC", "248", "EU");    // only the listed calls move; the 4U prefix itself is untouched
        is("BY1AA", "318", "AS");
    }
}
