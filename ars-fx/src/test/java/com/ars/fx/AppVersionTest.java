package com.ars.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AppVersionTest {

    @Test
    void stampedVersionIsResolvedNotPlaceholder() {
        String v = AppVersion.current();
        assertNotEquals(AppVersion.UNKNOWN, v, "version.properties was not filtered by Maven");
        assertFalse(v.contains("$"), v);
        assertNotNull(AppVersion.parse(v), "stamped version must be parseable: " + v);
    }

    @Test
    void newerComparesNumericallyNotLexically() {
        assertTrue(AppVersion.isNewer("1.12.0", "1.11.0"));
        assertTrue(AppVersion.isNewer("1.10.0", "1.9.5"));   // 10 > 9, not string order
        assertTrue(AppVersion.isNewer("2.0.0", "1.99.99"));
        assertTrue(AppVersion.isNewer("1.11.1", "1.11"));    // missing parts are zero
    }

    @Test
    void sameOrOlderIsNotNewer() {
        assertFalse(AppVersion.isNewer("1.11.0", "1.11.0"));
        assertFalse(AppVersion.isNewer("1.11", "1.11.0"));
        assertFalse(AppVersion.isNewer("1.9.5", "1.10.0"));
    }

    @Test
    void tagPrefixAndSuffixAreTolerated() {
        assertTrue(AppVersion.isNewer("v1.12.0", "1.11.0"));
        assertFalse(AppVersion.isNewer("v1.11.0", "1.11.0"));
        assertTrue(AppVersion.isNewer("1.12.0-rc1", "1.11.0"));
    }

    @Test
    void garbageIsNeverNewer() {
        assertFalse(AppVersion.isNewer(null, "1.11.0"));
        assertFalse(AppVersion.isNewer("", "1.11.0"));
        assertFalse(AppVersion.isNewer("latest", "1.11.0"));
        assertFalse(AppVersion.isNewer("1.12.0", "garbage"));
    }
}
