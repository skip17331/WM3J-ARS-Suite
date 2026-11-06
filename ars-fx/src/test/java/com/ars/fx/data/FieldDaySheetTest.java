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

    private static FieldDaySheet.Category cat(String key) {
        return FieldDaySheet.CATEGORIES.stream().filter(c -> c.key().equals(key)).findFirst().orElseThrow();
    }

    @Test void theCategoriesAreUnique() {
        var keys = new HashSet<String>();
        for (var c : FieldDaySheet.CATEGORIES) assertTrue(keys.add(c.key()), c.key());
        assertEquals(18, FieldDaySheet.CATEGORIES.size());
    }

    @Test void aFlatBonusIsClaimedOnceWhateverTheCount() {
        assertEquals(100, cat("media").points(1));
        assertEquals(100, cat("media").points(5), "a flat bonus is earned once");
        assertEquals(0, cat("media").points(0));
        assertEquals(0, cat("media").points(-2));
        assertEquals(50, cat("web").points(1));
        assertEquals(50, cat("site").points(1));
    }

    @Test void messagesAreTenEachUpToAHundred() {
        assertEquals(30, cat("nts").points(3));
        assertEquals(100, cat("nts").points(10));
        assertEquals(100, cat("nts").points(25), "ten messages at most");
    }

    @Test void youthIsTwentyEachUpToAHundred() {
        assertEquals(40, cat("youth").points(2));
        assertEquals(100, cat("youth").points(9));
    }

    @Test void gotaContactsAreFiveEachWithNoLimit() {
        assertEquals(5, cat("gota").points(1));
        assertEquals(1500, cat("gota").points(300));
    }

    @Test void thePowerMultipliersAreFiveTwoOne() {
        assertArrayEquals(new int[]{5, 2, 1}, FieldDaySheet.POWER_MULTIPLIERS);
    }
}
