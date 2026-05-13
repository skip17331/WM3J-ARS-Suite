package com.hamradio.modem.dsp;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Phase C publisher. All hub-side and rig-side
 * dependencies are injected so the suite never needs network or audio.
 */
class SkimmerSpotPublisherTest {

    private static final long RIG_HZ_20M = 14_025_000L;     // CW segment
    private static final long RIG_HZ_40M =  7_025_000L;

    /** Build a publisher backed by a list sink and configurable rig
     *  frequency. Rig frequency is held in an atomic-style box so tests
     *  can change it between events. */
    private static class Harness {
        final List<JsonObject> sent = new ArrayList<>();
        long[] rigBox  = { RIG_HZ_20M };
        String[] callBox = { "WM3J" };
        final SkimmerSpotPublisher pub;

        Harness() {
            pub = new SkimmerSpotPublisher(sent::add,
                () -> rigBox[0],
                () -> callBox[0]);
        }
    }

    private static MultiCarrierDecoder.ScoredCallsign sc(
            double audioHz, String call, double snr, long t) {
        return new MultiCarrierDecoder.ScoredCallsign(
            audioHz, snr, call, 0.85, "CQ", 2, t);
    }

    @Test
    void emitsOneSpotForOneScoring() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        assertEquals(1, h.sent.size());
        JsonObject s = h.sent.get(0);
        assertEquals("SPOT",          s.get("type").getAsString());
        assertEquals("W1AW",          s.get("spotted").getAsString());
        assertEquals("WM3J (skimmer)", s.get("spotter").getAsString());
        assertEquals("CW",            s.get("mode").getAsString());
        assertEquals("LOCAL_SKIMMER", s.get("source").getAsString());
        assertEquals(RIG_HZ_20M + 700L, s.get("frequency").getAsLong());
        assertEquals(18,              s.get("snrDb").getAsInt());
    }

    @Test
    void noSpotWhenRigFrequencyIsZero() {
        Harness h = new Harness();
        h.rigBox[0] = 0;
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        assertTrue(h.sent.isEmpty(),
            "publisher must skip when there's no rig frequency to anchor the spot to");
        assertEquals(0, h.pub.getSpotsEmittedTotal());
    }

    @Test
    void dedupsSameCallSameKhzWithinWindow() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        // Second event 1 second later — must not spot again.
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 19.0, 2_000));
        // Third event 4:59 later — still inside the 5-min window.
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 19.0, 1_000 + 4 * 60_000 + 59_000));
        assertEquals(1, h.sent.size());
    }

    @Test
    void allowsRespotJustPastDedupWindow() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        long pastWindow = 1_000 + SkimmerSpotPublisher.DEDUP_WINDOW_MS + 1;
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, pastWindow));
        assertEquals(2, h.sent.size());
    }

    @Test
    void differentCallsignsAreIndependentlyDeduped() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        h.pub.onScoredCallsign(sc(700.0, "K3LR", 16.0, 1_500));
        assertEquals(2, h.sent.size());
        assertEquals("W1AW", h.sent.get(0).get("spotted").getAsString());
        assertEquals("K3LR", h.sent.get(1).get("spotted").getAsString());
    }

    @Test
    void differentKhzBucketsAreIndependentlyDeduped() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));   // 14025.7 kHz → 14025 kHz bucket
        // Move rig 2 kHz — same call, new kHz bucket → second spot.
        h.rigBox[0] = RIG_HZ_20M + 2_000;
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 2_000));
        assertEquals(2, h.sent.size());
    }

    @Test
    void roundsToKhzBucketSoTinyOffsetWiggleStillDedups() {
        Harness h = new Harness();
        // First spot at audio 700 Hz → 14025.7 kHz → bucket 14025.
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        // Second event at audio 715 Hz (15 Hz wiggle from decoder
        // re-spawn) → still bucket 14025 → must dedup.
        h.pub.onScoredCallsign(sc(715.0, "W1AW", 18.0, 2_000));
        assertEquals(1, h.sent.size());
    }

    @Test
    void unknownStationCallFallsBackToLocal() {
        Harness h = new Harness();
        h.callBox[0] = "";
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        assertEquals("LOCAL (skimmer)", h.sent.get(0).get("spotter").getAsString());
    }

    @Test
    void prunesStaleDedupEntriesAfterIntervals() {
        Harness h = new Harness();
        // Land two spots well apart so dedup table holds two entries.
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));
        h.pub.onScoredCallsign(sc(900.0, "K3LR", 18.0, 1_000));
        assertEquals(2, h.pub.dedupTableSize());

        // Fire a third event well past the dedup window AND past the
        // prune interval — should prune the two stale entries before
        // adding its own.
        long farFuture = 1_000 + SkimmerSpotPublisher.DEDUP_WINDOW_MS
                              + SkimmerSpotPublisher.PRUNE_INTERVAL_MS + 1;
        h.pub.onScoredCallsign(sc(1100.0, "VE3ABC", 18.0, farFuture));
        assertEquals(1, h.pub.dedupTableSize(),
            "stale entries should have been pruned; only the fresh entry remains");
    }

    @Test
    void totalSpotsEmittedCounterReflectsActualEmits() {
        Harness h = new Harness();
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_000));   // emit
        h.pub.onScoredCallsign(sc(700.0, "W1AW", 18.0, 1_500));   // dedup
        h.pub.onScoredCallsign(sc(900.0, "K3LR", 18.0, 2_000));   // emit
        assertEquals(2L, h.pub.getSpotsEmittedTotal());
        assertEquals(2,  h.sent.size());
    }
}
