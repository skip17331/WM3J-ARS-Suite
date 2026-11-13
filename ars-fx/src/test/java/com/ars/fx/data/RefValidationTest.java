package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import com.ars.fx.data.RefLookup.Kind;
import com.ars.fx.data.RefValidation.Context;
import com.ars.fx.data.RefValidation.Level;
import com.ars.fx.data.RefValidation.Result;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RefValidationTest {
    static final String POTA = "{\"name\": \"Acadia\", \"grid4\": \"FN54\", \"grid6\": \"FN54vh\", \"locationDesc\": \"US-ME\"}";
    static final String SOTA = "{\"name\":\"Mount Saint Helens\",\"points\":10,\"altM\":2550,\"associationName\":\"USA - Washington\",\"locator\":\"CN86ve\"}";
    static final String IOTA = "[{\"refno\":\"AS-007\",\"name\":\"Honshu\",\"dxcc_num\":\"339\",\"latitude_min\":\"34.00\",\"latitude_max\":\"41.00\",\"longitude_min\":\"135.00\",\"longitude_max\":\"142.00\"}]";

    @BeforeEach void setUp(@TempDir Path tmp) { FeedFetcher.setDirForTest(tmp); RefLookup.resetForTest(); }
    @AfterEach void reset() { FeedFetcher.setDirForTest(null); FeedFetcher.setHttpForTest(null); RefLookup.resetForTest(); }

    private static void serve(String pota, String sota, String iota) {
        FeedFetcher.setHttpForTest(u -> u.contains("pota") ? pota : u.contains("sota") ? sota : iota);
    }

    // ---- shape: offline and instant -----------------------------------------------------------------------

    @Test void anEmptyFieldHasNothingToSay() {
        assertNull(RefValidation.shape(Kind.POTA, ""));
        assertNull(RefValidation.shape(Kind.SOTA, "   "));
        assertNull(RefValidation.shape(Kind.IOTA, null));
    }

    @Test void goodShapesPassAndLowerCaseIsAccepted() {
        for (String r : new String[]{"US-0001", "K-0001", "VE-12345", "g-0100"}) assertEquals(Level.OK, RefValidation.shape(Kind.POTA, r).level(), r);
        for (String r : new String[]{"W7W/LC-001", "G/LD-001", "hb/zh-001"}) assertEquals(Level.OK, RefValidation.shape(Kind.SOTA, r).level(), r);
        for (String r : new String[]{"NA-005", "EU-001", "oc-019", "AN-011"}) assertEquals(Level.OK, RefValidation.shape(Kind.IOTA, r).level(), r);
    }

    @Test void badShapesAreRefusedWithTheShapeToUse() {
        assertEquals(Level.BAD, RefValidation.shape(Kind.POTA, "US-001").level(), "a park number has four or five digits");
        assertEquals(Level.BAD, RefValidation.shape(Kind.POTA, "US-123456").level());
        assertEquals(Level.BAD, RefValidation.shape(Kind.SOTA, "W7W-LC-001").level());
        assertEquals(Level.BAD, RefValidation.shape(Kind.IOTA, "XX-005").level());
        assertEquals(Level.BAD, RefValidation.shape(Kind.IOTA, "NA-05").level());
        assertTrue(RefValidation.shape(Kind.IOTA, "XX-005").message().contains("NA-005"), "it says what a good one looks like");
    }

    @Test void nearMissesAreAnsweredWithTheCorrectedReference() {
        assertTrue(RefValidation.shape(Kind.POTA, "US0001").message().contains("US-0001"));
        assertTrue(RefValidation.shape(Kind.SOTA, "W7W-LC-001").message().contains("W7W/LC-001"));
        assertTrue(RefValidation.shape(Kind.SOTA, "W7W LC 001").message().contains("W7W/LC-001"));
        assertTrue(RefValidation.shape(Kind.IOTA, "NA005").message().contains("NA-005"));
        assertNull(RefValidation.nearMiss(Kind.IOTA, "NA5"), "no guess when the digits are not all there");
    }

    @Test void onlyPotaMayListSeveralReferences() {
        assertEquals(Level.OK, RefValidation.shape(Kind.POTA, "US-0001, US-0002").level(), "a two-fer");
        assertEquals(2, RefValidation.tokens(Kind.POTA, "us-0001; US-0002").size());
        assertEquals(Level.BAD, RefValidation.shape(Kind.SOTA, "W7W/LC-001, W7W/LC-002").level());
        assertEquals(Level.BAD, RefValidation.shape(Kind.IOTA, "NA-005, NA-006").level());
        assertEquals(Level.BAD, RefValidation.shape(Kind.POTA, "US-0001, junk").level(), "one bad park in the list is a bad field");
    }

    // ---- existence ----------------------------------------------------------------------------------------

    @Test void aKnownReferenceIsOkAndNamed() {
        serve(POTA, SOTA, IOTA);
        Result r = RefValidation.validate(Kind.POTA, "us-0001", Context.NONE);
        assertEquals(Level.OK, r.level()); assertTrue(r.message().contains("Acadia")); assertEquals("FN54vh", r.ref().grid());
        assertTrue(RefValidation.validate(Kind.SOTA, "W7W/LC-001", Context.NONE).message().contains("Mount Saint Helens"));
        assertTrue(RefValidation.validate(Kind.IOTA, "AS-007", Context.NONE).message().contains("Honshu"));
    }

    @Test void aReferenceTheServiceDoesNotKnowIsBad() {
        serve("null", "", IOTA);
        assertEquals(Level.BAD, RefValidation.validate(Kind.POTA, "US-9999", Context.NONE).level());
        assertEquals(Level.BAD, RefValidation.validate(Kind.SOTA, "W7W/LC-999", Context.NONE).level());
        assertEquals(Level.BAD, RefValidation.validate(Kind.IOTA, "AS-999", Context.NONE).level());
    }

    @Test void anHttp404MeansNotKnownButOtherFailuresMeanCouldNotAsk() {
        FeedFetcher.setHttpForTest(u -> { throw new RuntimeException("HTTP 404"); });
        assertEquals(Level.BAD, RefValidation.validate(Kind.SOTA, "W7W/LC-999", Context.NONE).level());
        FeedFetcher.setDirForTest(null); FeedFetcher.setDirForTest(java.nio.file.Path.of(System.getProperty("java.io.tmpdir"), "refval-" + System.nanoTime()));
        FeedFetcher.setHttpForTest(u -> { throw new RuntimeException("offline"); });
        Result r = RefValidation.validate(Kind.POTA, "US-0001", Context.NONE);
        assertEquals(Level.UNKNOWN, r.level(), "offline is not an error in the reference");
        assertTrue(r.message().contains("could not be looked up"));
        assertEquals(Level.UNKNOWN, RefValidation.validate(Kind.IOTA, "NA-005", Context.NONE).level(), "no IOTA list to check against");
    }

    @Test void aBadShapeIsNeverLookedUp() {
        FeedFetcher.setHttpForTest(u -> { fail("no request for a reference of the wrong shape: " + u); return ""; });
        assertEquals(Level.BAD, RefValidation.validate(Kind.POTA, "junk", Context.NONE).level());
        assertNull(RefValidation.validate(Kind.POTA, "", Context.NONE));
    }

    // ---- sense --------------------------------------------------------------------------------------------

    @Test void aParkFarFromTheQsoGridIsAWarningNotAnError() {
        serve(POTA, SOTA, IOTA);
        Result near = RefValidation.validate(Kind.POTA, "US-0001", new Context("FN54", 291));
        assertEquals(Level.OK, near.level());
        Result far = RefValidation.validate(Kind.POTA, "US-0001", new Context("DM13", 291));
        assertEquals(Level.WARN, far.level());
        assertTrue(far.message().contains("DM13") && far.message().contains("km"));
        assertEquals(Level.OK, RefValidation.validate(Kind.POTA, "US-0001", new Context("", 291)).level(), "no grid on the QSO, nothing to compare");
        assertEquals(Level.OK, RefValidation.validate(Kind.POTA, "US-0001", new Context("not a grid", 291)).level());
    }

    @Test void aSummitIsComparedWithTheQsoGridToo() {
        serve(POTA, SOTA, IOTA);
        assertEquals(Level.OK, RefValidation.validate(Kind.SOTA, "W7W/LC-001", new Context("CN86", 291)).level());
        assertEquals(Level.WARN, RefValidation.validate(Kind.SOTA, "W7W/LC-001", new Context("FN31", 291)).level());
    }

    @Test void onlyTheFirstParkOfATwoFerIsComparedWithTheGrid() {
        serve(POTA, SOTA, IOTA);
        Result r = RefValidation.validate(Kind.POTA, "US-0001, US-0002", new Context("FN54", 291));
        assertEquals(Level.OK, r.level());
        assertTrue(r.message().contains("US-0001") && r.message().contains("US-0002"));
    }

    @Test void anIslandGroupInAnotherEntityThanTheCallIsAWarning() {
        serve(POTA, SOTA, IOTA);
        assertEquals(Level.OK, RefValidation.validate(Kind.IOTA, "AS-007", new Context("", 339)).level(), "Japan on a Japanese island");
        Result other = RefValidation.validate(Kind.IOTA, "AS-007", new Context("", 291));
        assertEquals(Level.WARN, other.level());
        assertTrue(other.message().contains("339") && other.message().contains("291"));
        assertEquals(Level.OK, RefValidation.validate(Kind.IOTA, "AS-007", Context.NONE).level(), "no entity for the call, nothing to compare");
    }
}
