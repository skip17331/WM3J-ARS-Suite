package com.ars.fx.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ClusterClientInjectTest {

    private static ClusterClient.Spot spot(String call) {
        return new ClusterClient.Spot(14_200_000, call, "SSB", "20m", "W1AW", "", 1_000);
    }

    /** The hub repeats its state every second, and listeners repaint whole screens. */
    @Test
    void aRepeatOfTheSameSpotsIsNotWorthNotifying() {
        assertFalse(ClusterClient.worthNotifying(List.of(spot("K1ABC")), List.of(spot("K1ABC")), 1_000));
    }

    @Test
    void aNewSpotIsWorthNotifyingAtOnce() {
        assertTrue(ClusterClient.worthNotifying(List.of(spot("K1ABC")), List.of(spot("K2DEF"), spot("K1ABC")), 1_000));
    }

    @Test
    void anUnchangedListStillRefreshesSpotAgesNowAndThen() {
        assertTrue(ClusterClient.worthNotifying(List.of(spot("K1ABC")), List.of(spot("K1ABC")), 30_000));
    }
}
