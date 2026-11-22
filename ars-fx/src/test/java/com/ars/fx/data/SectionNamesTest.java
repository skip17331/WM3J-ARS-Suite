package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SectionNamesTest {
    @Test void statesAndSections() {
        assertEquals("Iowa", SectionNames.nameOf("IA"));
        assertEquals("Iowa", SectionNames.nameOf(" ia "));
        assertEquals("Western Washington", SectionNames.nameOf("WWA"));
        assertEquals("Ontario East", SectionNames.nameOf("ONE"));
    }
    @Test void unknownIsNull() {
        assertNull(SectionNames.nameOf("ZZZ"));
        assertNull(SectionNames.nameOf(null));
    }
}
