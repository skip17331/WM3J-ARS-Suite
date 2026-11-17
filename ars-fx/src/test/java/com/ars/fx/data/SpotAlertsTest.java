package com.ars.fx.data;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SpotAlertsTest {
    private static final long NOW = 10_000_000L;
    private static ClusterClient.Spot spot(String call, String band, long age) {
        return new ClusterClient.Spot(14_025_000L, call, "CW", band, "K1TTT", "", NOW - age);
    }
    private static final SpotAlerts.Settings ON = new SpotAlerts.Settings(true, true, SpotAlerts.parse("VP8*, 3y0j"));

    @Test void watchListMatchesCallsPrefixesAndPortables() {
        List<String> w = SpotAlerts.parse(" VP8* ; 3y0j  K1ABC,*");
        assertEquals(List.of("VP8*", "3Y0J", "K1ABC"), w, "a lone * is ignored");
        assertTrue(SpotAlerts.watched("VP8PJ", w));
        assertTrue(SpotAlerts.watched("3Y0J", w));
        assertTrue(SpotAlerts.watched("k1abc/p", w), "portable suffix");
        assertTrue(SpotAlerts.watched("VP2E/K1ABC", w), "portable prefix");
        assertFalse(SpotAlerts.watched("VP2PJ", w));
        assertFalse(SpotAlerts.watched("K1ABCD", w), "an exact call is exact");
    }

    @Test void alertsOnWatchedAndNeededSpotsOnce() {
        Map<String, Long> seen = new HashMap<>();
        List<ClusterClient.Spot> spots = List.of(spot("VP8PJ", "20m", 0), spot("W1AW", "20m", 0), spot("JA1XYZ", "20m", 0));
        List<SpotAlerts.Alert> a = SpotAlerts.check(spots, s -> s.call().equals("JA1XYZ") ? "NEW BAND" : "", ON, NOW, seen);
        assertEquals(2, a.size());
        assertEquals("watch list", a.get(0).reason());
        assertEquals("NEW BAND", a.get(1).reason());
        assertTrue(SpotAlerts.check(spots, s -> s.call().equals("JA1XYZ") ? "NEW BAND" : "", ON, NOW + 60_000, seen).isEmpty(),
                "the cluster repeating the spot does not alert again");
        long later = SpotAlerts.REPEAT_MS + 1;            // spotted afresh half an hour later (a negative age: it arrives at NOW + later)
        assertEquals(2, SpotAlerts.check(List.of(spot("VP8PJ", "20m", -later), spot("JA1XYZ", "20m", -later)),
                s -> "NEW BAND", ON, NOW + later, seen).size(), "after 30 minutes it may alert again");
    }

    @Test void aNewBandIsANewAlertAndOldSpotsNever() {
        Map<String, Long> seen = new HashMap<>();
        SpotAlerts.check(List.of(spot("VP8PJ", "20m", 0)), s -> "", ON, NOW, seen);
        assertEquals(1, SpotAlerts.check(List.of(spot("VP8PJ", "15m", 0)), s -> "", ON, NOW, seen).size(), "same call, another band");
        assertTrue(SpotAlerts.check(List.of(spot("3Y0J", "20m", SpotAlerts.FRESH_MS + 1)), s -> "", ON, NOW, seen).isEmpty(),
                "a stale spot (handed back on reconnect) does not alert");
    }

    @Test void offOrNeededOffMeanQuiet() {
        Map<String, Long> seen = new HashMap<>();
        assertTrue(SpotAlerts.check(List.of(spot("VP8PJ", "20m", 0)), s -> "NEW ONE",
                new SpotAlerts.Settings(false, true, List.of("VP8*")), NOW, seen).isEmpty(), "switched off");
        List<SpotAlerts.Alert> a = SpotAlerts.check(List.of(spot("JA1XYZ", "20m", 0), spot("VP8PJ", "20m", 0)), s -> "NEW ONE",
                new SpotAlerts.Settings(true, false, List.of("VP8*")), NOW, seen);
        assertEquals(1, a.size(), "needed alerts off: the watch list still works");
        assertEquals("VP8PJ", a.get(0).spot().call());
    }

    @Test void theChimeIsShortPcm() {
        byte[] pcm = SpotAlerts.tone(22050f, new double[]{880, 1320}, 0.12);
        assertEquals(2 * (int) (22050 * 0.12) * 2, pcm.length);
        assertEquals(0, pcm[0], "fades in from silence");
    }
}
