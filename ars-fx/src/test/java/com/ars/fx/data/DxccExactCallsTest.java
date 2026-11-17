package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Exact-call entries in the country files (cty.dat "=CALL") win over the prefix table. */
class DxccExactCallsTest {

    @Test void unAndItuHeadquartersAreTheirOwnEntities() {
        DxccInfo.Info un = DxccInfo.of("4U1UN"), itu = DxccInfo.of("4U1ITU");
        assertNotNull(un); assertNotNull(itu);
        assertNotEquals("Italy", un.country(), "4U1UN used to resolve to Italy via the 4U prefix");
        assertNotEquals("Italy", itu.country());
        assertEquals("NA", un.continent(), "the UN HQ is in New York");
        assertEquals("EU", itu.continent(), "the ITU HQ is in Geneva");
        assertEquals(289, un.dxcc(), "United Nations HQ is DXCC #289");
        assertEquals(117, itu.dxcc(), "ITU HQ is DXCC #117");
    }

    @Test void everyExactOnlyEntityHasItsNumberAndName() {
        assertEquals(206, DxccInfo.of("4U1VIC").dxcc(), "Vienna International Centre");
        assertEquals(506, DxccInfo.of("BS7H").dxcc(), "Scarborough Reef");
        assertEquals(111, DxccInfo.of("VK0EK").dxcc(), "Heard Island");
        assertEquals(153, DxccInfo.of("VK0MQ").dxcc(), "Macquarie Island");
        assertFalse(DxccInfo.of("4U1UN").country().matches("[0-9]+"), "a name, not the bare number");
    }

    @Test void ordinaryCallsAreUnchanged() {
        assertEquals(291, DxccInfo.of("W1AW").dxcc());
        assertEquals(110, DxccInfo.of("KH6ABC").dxcc());
        assertEquals(110, DxccInfo.of("W1AW/KH6").dxcc());
        assertEquals(1, DxccInfo.of("VE3/K1ABC").dxcc());
        assertEquals(248, DxccInfo.of("IT9AA").dxcc(), "Sicily counts as Italy");
        assertNull(DxccInfo.of("W1AW/MM"), "maritime mobile is no entity");
    }
}
