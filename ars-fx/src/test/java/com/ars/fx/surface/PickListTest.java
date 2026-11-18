package com.ars.fx.surface;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The type-ahead matcher behind the exchange fields' suggestion list. */
class PickListTest {
    private static final List<String> COUNTIES = List.of("Albemarle", "Berkshire", "Bergen", "Barnstable", "Hampshire", "Kent");

    @Test
    void matchesAnyCaseAndSorts() {
        assertEquals(List.of("Bergen", "Berkshire"), PickList.match(COUNTIES, "ber", 8));
        assertEquals(List.of("Hampshire"), PickList.match(COUNTIES, "HAMP", 8));
        assertEquals(List.of("Albemarle", "Barnstable"), PickList.match(COUNTIES, "ar", 8), "contains, sorted");
    }

    @Test
    void prefixesFirstThenSubstrings() {
        // "k": Kent starts with it; Berkshire contains it
        assertEquals(List.of("Kent", "Berkshire"), PickList.match(COUNTIES, "k", 8));
    }

    @Test
    void nothingForEmptyOrUnknownOrCompleteInput() {
        assertTrue(PickList.match(COUNTIES, "", 8).isEmpty());
        assertTrue(PickList.match(COUNTIES, null, 8).isEmpty());
        assertTrue(PickList.match(COUNTIES, "zzz", 8).isEmpty());
        assertTrue(PickList.match(List.of(), "a", 8).isEmpty(), "a free-text field has no list");
        assertTrue(PickList.match(COUNTIES, "kent", 8).isEmpty(), "already exactly what is typed: no popup in the way");
    }

    @Test
    void limitedAndDeduplicated() {
        List<String> many = List.of("AA1", "AA2", "AA3", "AA4", "AA5", "AA6", "AA7", "AA8", "AA9", "AA9");
        assertEquals(3, PickList.match(many, "aa", 3).size());
        assertEquals(9, PickList.match(many, "aa", 20).size());
    }
}
