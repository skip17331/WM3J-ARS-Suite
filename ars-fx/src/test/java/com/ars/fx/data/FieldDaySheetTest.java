package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.Test;

class FieldDaySheetTest {
    @Test void emergencyPowerIs100PerTransmitterUpTo20() {
        assertEquals(0, FieldDaySheet.emergencyPower(0));
        assertEquals(300, FieldDaySheet.emergencyPower(3));
        assertEquals(2000, FieldDaySheet.emergencyPower(20));
        assertEquals(2000, FieldDaySheet.emergencyPower(35), "capped at 20 transmitters");
        assertEquals(0, FieldDaySheet.emergencyPower(-4));
    }

    @Test void theTotalIsQsoPointsTimesTheMultiplierPlusTheBonus() {
        assertEquals(1000L * 2 + 450, FieldDaySheet.total(1000, 2, 450));
        assertEquals(1000 + 0, FieldDaySheet.total(1000, 0, 0), "a multiplier below 1 counts as 1");
        assertEquals(0, FieldDaySheet.total(-5, 1, -5));
    }

    @Test void typedNumbersAreForgiving() {
        assertEquals(12, FieldDaySheet.number(" 12 "));
        assertEquals(0, FieldDaySheet.number(""));
        assertEquals(0, FieldDaySheet.number("abc"));
        assertEquals(0, FieldDaySheet.number("-3"));
        assertEquals(0, FieldDaySheet.number(null));
    }

    @Test void theCategoriesAreUniqueAndOnlyTheStatedValuesAreFilledIn() {
        var keys = new HashSet<String>();
        for (var c : FieldDaySheet.CATEGORIES) {
            assertTrue(keys.add(c.key()), c.key());
            if (c.points() >= 0) assertEquals("web", c.key(), "no value is filled in unless the sources state it");
        }
        assertTrue(FieldDaySheet.CATEGORIES.size() >= 15);
    }
}
