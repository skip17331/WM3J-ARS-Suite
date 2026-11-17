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

    private static ClusterClient.Spot at(String call, long hz, String band, String de) {
        return new ClusterClient.Spot(hz, call, "CW", band, de, "", 1_000);
    }

    @Test
    void aReSpottedStationHoldsOneLineTheNewest() {
        java.util.Deque<ClusterClient.Spot> list = new java.util.ArrayDeque<>();
        ClusterClient.addDeduped(list, at("3Y0J", 14_025_000, "20m", "K1TTT"), 40);
        ClusterClient.addDeduped(list, at("K1ABC", 14_030_000, "20m", "W3LPL"), 40);
        ClusterClient.addDeduped(list, at("3Y0J", 14_025_500, "20m", "DL1AA"), 40);     // the same station spotted again
        org.junit.jupiter.api.Assertions.assertEquals(2, list.size());
        org.junit.jupiter.api.Assertions.assertEquals("DL1AA", list.getFirst().de(), "the newest spot, at the top");
        ClusterClient.addDeduped(list, at("3Y0J", 21_025_000, "15m", "K1TTT"), 40);     // another band: its own line
        ClusterClient.addDeduped(list, at("3Y0J", 14_195_000, "20m", "K1TTT"), 40);     // far away on the same band (phone): its own line
        org.junit.jupiter.api.Assertions.assertEquals(4, list.size());
    }

    @Test
    void theListStaysBounded() {
        java.util.Deque<ClusterClient.Spot> list = new java.util.ArrayDeque<>();
        for (int i = 0; i < 50; i++) ClusterClient.addDeduped(list, at("K" + i + "AA", 14_000_000 + i * 10_000L, "20m", "X"), 40);
        org.junit.jupiter.api.Assertions.assertEquals(40, list.size());
        org.junit.jupiter.api.Assertions.assertEquals("K49AA", list.getFirst().call());
    }
}
