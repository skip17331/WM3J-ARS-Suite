package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class CtyFallbackTest {
    private static JLogDb.QsoExport q(String call, String band, boolean conf, int cqz, int ituz) {
        return new JLogDb.QsoExport(call, "2026-01-02 15:30:00", band, "SSB", "14.2", 0, "59", "59", null, null, null, null, null, null,
                new JLogDb.Refs("", "", "", cqz, ituz, false, conf, false));
    }

    @Test void callsTheEnginePrefixTableMissAreStillPlacedByTheCountryFiles() {
        // all of these were unresolved by the engine table (see the audit): Hawaii AH6, Botswana 8O, China XS, Gibraltar ZG
        for (String call : List.of("AH6AA", "8O1AA", "XS1AA", "ZG1AA")) {
            DxccInfo.Info i = DxccInfo.of(call);
            assertNotNull(i, call + " should resolve through cty.dat");
            assertNotNull(i.country(), call);
            assertNotNull(i.continent(), call);
        }
        assertEquals("OC", DxccInfo.of("AH6AA").continent());
        assertEquals("AF", DxccInfo.of("8O1AA").continent());
    }

    @Test void anEntityKnownUnderAnotherPrefixKeepsTheEnginesNumberSoItIsCountedOnce() {
        DxccInfo.Info viaEngine = DxccInfo.of("KH6AA");     // Hawaii, resolved by the engine table
        DxccInfo.Info viaCty = DxccInfo.of("AH6AA");        // Hawaii, resolved by cty.dat
        assertNotNull(viaEngine);
        assertEquals(viaEngine.dxcc(), viaCty.dxcc(), "same entity, same number");
        assertEquals(Awards.entityKey(viaEngine), Awards.entityKey(viaCty));
    }

    @Test void junkIsNotPlacedInAnEntity() {
        assertNull(DxccInfo.of("TEST"));
        assertNull(DxccInfo.of("QRZ"));
        assertNull(DxccInfo.of(""));
        assertNull(DxccInfo.of(null));
        assertNull(DxccInfo.of("W1AW/MM"), "maritime mobile has no entity");
    }

    @Test void whatTheEngineAlreadyKnowsIsUnchanged() {
        DxccInfo.Info w = DxccInfo.of("W1AW");
        assertNotNull(w);
        assertEquals("NA", w.continent());
        assertTrue(w.dxcc() > 0);
    }

    @Test void zonesComeFromTheCallWhenTheQsoHasNone() {
        var s = Awards.compute(List.of(q("JA1XYZ", "20m", true, 0, 0), q("DL1ABC", "20m", false, 0, 0)));
        assertEquals(2, s.cqz.worked());
        assertTrue(s.cqz.workedSet().contains("25"), "Japan is CQ zone 25");
        assertTrue(s.cqz.workedSet().contains("14"), "Germany is CQ zone 14");
        assertEquals(1, s.cqz.confirmed());
        assertTrue(s.ituz.workedSet().contains("45"), "Japan is ITU zone 45");
    }

    @Test void aZoneOnTheQsoWinsOverTheCall() {
        var s = Awards.compute(List.of(q("JA1XYZ", "20m", false, 40, 90)));
        assertEquals(java.util.Set.of("40"), s.cqz.workedSet());
        assertEquals(java.util.Set.of("90"), s.ituz.workedSet());
    }
}
