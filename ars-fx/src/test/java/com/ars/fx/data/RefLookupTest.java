package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RefLookupTest {
    static final String POTA = "{\"parkId\": 1, \"reference\": \"US-0001\", \"name\": \"Acadia\", \"latitude\": 44.31, \"longitude\": -68.2034, "
            + "\"grid4\": \"FN54\", \"grid6\": \"FN54vh\", \"locationDesc\": \"US-ME\"}";
    static final String SOTA = "{\"associationName\":\"USA - Washington\",\"summitCode\":\"W7W/LC-001\",\"name\":\"Mount Saint Helens\","
            + "\"points\":10,\"altM\":2550,\"locator\":\"CN86ve\"}";
    // Japan (339): AS-007 around Tokyo, AS-008 far to the south-west; Germany (230): EU-001
    static final String IOTA = "[{\"refno\":\"AS-007\",\"name\":\"Honshu\",\"dxcc_num\":\"339\",\"latitude_min\":\"34.00\",\"latitude_max\":\"41.00\",\"longitude_min\":\"135.00\",\"longitude_max\":\"142.00\",\"sub_groups\":[]},"
            + "{\"refno\":\"AS-008\",\"name\":\"Okinawa group\",\"dxcc_num\":\"339\",\"latitude_min\":\"24.00\",\"latitude_max\":\"28.00\",\"longitude_min\":\"122.00\",\"longitude_max\":\"130.00\",\"sub_groups\":[]},"
            + "{\"refno\":\"EU-001\",\"name\":\"Borkum\",\"dxcc_num\":\"230\",\"latitude_min\":\"53.00\",\"latitude_max\":\"54.00\",\"longitude_min\":\"6.00\",\"longitude_max\":\"7.50\",\"sub_groups\":[]}]";

    @BeforeEach void setUp(@TempDir Path tmp) { FeedFetcher.setDirForTest(tmp); RefLookup.resetForTest(); }
    @AfterEach void reset() { FeedFetcher.setDirForTest(null); FeedFetcher.setHttpForTest(null); RefLookup.resetForTest(); }

    @Test void referenceShapes() {
        assertTrue(RefLookup.looksLikePota("us-0001")); assertTrue(RefLookup.looksLikePota("VE-0001"));
        assertFalse(RefLookup.looksLikePota("0001")); assertFalse(RefLookup.looksLikePota("W1AW"));
        assertTrue(RefLookup.looksLikeSota("W7W/LC-001")); assertFalse(RefLookup.looksLikeSota("W7W-001"));
        assertTrue(RefLookup.looksLikeIota("NA-005")); assertFalse(RefLookup.looksLikeIota("XX-005"));
    }

    @Test void parkAndSummitNamesComeFromTheServicesAndAreKeptSoTheyAreAskedOnce() {
        AtomicInteger hits = new AtomicInteger();
        FeedFetcher.setHttpForTest(u -> { hits.incrementAndGet(); return u.contains("pota") ? POTA : SOTA; });
        RefLookup.Ref p = RefLookup.pota("us-0001");
        assertEquals("Acadia", p.name()); assertEquals("FN54vh", p.grid()); assertEquals("US-ME", p.detail());
        assertEquals("Mount Saint Helens", RefLookup.sota("W7W/LC-001").name());
        assertTrue(RefLookup.sota("W7W/LC-001").detail().contains("10 pts"));
        assertEquals(2, hits.get(), "the second lookups came from the saved copies");
    }

    @Test void anUnknownParkOrABadShapeGivesNullWithoutAskingForNonsense() {
        FeedFetcher.setHttpForTest(u -> { assertFalse(u.contains("junk"), "no request for a reference of the wrong shape"); return "null"; });
        assertNull(RefLookup.pota("US-9999"), "the service answers null for a park it does not know");
        assertNull(RefLookup.pota("junk"));
        assertNull(RefLookup.sota("junk"));
        assertNull(RefLookup.iota(""));
    }

    @Test void anOfflineLookupIsJustNull() {
        FeedFetcher.setHttpForTest(u -> { throw new RuntimeException("offline"); });
        assertNull(RefLookup.pota("US-0001"));
        assertTrue(RefLookup.suggestIota(339, "PM95", 8).isEmpty());
    }

    @Test void iotaNamesAndIdeasFromTheEntityNarrowedByGrid() {
        FeedFetcher.setHttpForTest(u -> IOTA);
        assertEquals("Honshu", RefLookup.iota("as-007").name());
        List<RefLookup.IotaGroup> all = RefLookup.suggestIota(339, "", 8);
        assertEquals(List.of("AS-007", "AS-008"), all.stream().map(RefLookup.IotaGroup::ref).toList(), "both groups of Japan when the grid is unknown");
        List<RefLookup.IotaGroup> here = RefLookup.suggestIota(339, "pm95", 8);     // Tokyo
        assertEquals(List.of("AS-007"), here.stream().map(RefLookup.IotaGroup::ref).toList());
        assertEquals(2, RefLookup.suggestIota(339, "AA00", 8).size(), "a grid inside no group leaves the whole list for the entity");
        assertEquals(1, RefLookup.suggestIota(339, "", 1).size(), "capped");
        assertTrue(RefLookup.suggestIota(1, "", 8).isEmpty(), "no groups in that entity");
        assertTrue(RefLookup.suggestIota(0, "", 8).isEmpty());
    }
}
