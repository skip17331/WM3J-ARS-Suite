package com.hamradio.modem.dsp;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Phase A multi-channel CW decoder. Skimmer snapshots are
 * constructed directly (no actual FFT pipeline), and audio frames are
 * either zeros or synthesised CW tones — the goal is to prove the
 * decoder fleet's lifecycle and per-channel decode work, not to
 * regression-test the underlying CwMode.
 */
class MultiCarrierDecoderTest {

    private static final double SAMPLE_RATE = 8000.0;
    private static final int    FRAME_SIZE  = 256;
    private static final float[] SILENCE    = new float[FRAME_SIZE];

    /** Build a snapshot with one peak at the given frequency. */
    private static LocalSkimmer.Snapshot snapOnePeak(double freqHz, long tMs) {
        return new LocalSkimmer.Snapshot(tMs, List.of(
            new LocalSkimmer.Peak(freqHz, 20.0, 50.0)));
    }

    private static LocalSkimmer.Snapshot snapEmpty(long tMs) {
        return new LocalSkimmer.Snapshot(tMs, Collections.emptyList());
    }

    @Test
    void newPeakSpawnsOneChannel() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        d.processFrame(SILENCE, snapOnePeak(700.0, 1_000), 14_074_000L);
        assertEquals(1, d.getChannels().size());
        assertEquals(700.0, d.getChannels().get(0).centerHz, 0.001);
    }

    @Test
    void repeatedPeakDoesNotDuplicate() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        d.processFrame(SILENCE, snapOnePeak(700.0, 1_000), 0);
        d.processFrame(SILENCE, snapOnePeak(700.0, 1_200), 0);
        d.processFrame(SILENCE, snapOnePeak(700.0, 1_400), 0);
        assertEquals(1, d.getChannels().size(), "same carrier should stay one channel");
        assertEquals(1_400L, d.getChannels().get(0).lastSeenMs, "lastSeenMs should track latest snap");
    }

    @Test
    void closePeaksMergeIntoOneChannel() {
        // 700 Hz and 720 Hz are within CHANNEL_MERGE_HZ (80) so they
        // should land on the same decoder.
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        d.processFrame(SILENCE, snapOnePeak(700.0, 0), 0);
        d.processFrame(SILENCE, snapOnePeak(720.0, 100), 0);
        assertEquals(1, d.getChannels().size());
    }

    @Test
    void farPeaksSpawnSeparateChannels() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        // 700 and 900 Hz are 200 Hz apart — well beyond the 80 Hz merge.
        LocalSkimmer.Snapshot snap = new LocalSkimmer.Snapshot(0L, List.of(
            new LocalSkimmer.Peak(700.0, 20.0, 50.0),
            new LocalSkimmer.Peak(900.0, 18.0, 50.0)));
        d.processFrame(SILENCE, snap, 0);
        assertEquals(2, d.getChannels().size());
    }

    @Test
    void channelReapedAfterIdleTimeout() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        d.processFrame(SILENCE, snapOnePeak(700.0, 0), 0);
        assertEquals(1, d.getChannels().size());
        // Empty snap arriving past the reap threshold sweeps it.
        d.processFrame(SILENCE,
            snapEmpty(MultiCarrierDecoder.CHANNEL_REAP_MS + 1), 0);
        assertTrue(d.getChannels().isEmpty(), "stale channel should be reaped");
    }

    @Test
    void channelCappedAtMaxChannels() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        List<LocalSkimmer.Peak> peaks = new ArrayList<>();
        // 20 peaks, each well outside the 80 Hz merge band of the others.
        for (int i = 0; i < 20; i++) {
            peaks.add(new LocalSkimmer.Peak(400.0 + i * 100.0, 20.0, 50.0));
        }
        d.processFrame(SILENCE, new LocalSkimmer.Snapshot(0L, peaks), 0);
        assertEquals(MultiCarrierDecoder.MAX_CHANNELS, d.getChannels().size());
    }

    @Test
    void nullSnapshotIsSafeNoChannels() {
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        // Should not throw and should not invent channels out of thin air.
        d.processFrame(SILENCE, null, 0);
        assertTrue(d.getChannels().isEmpty());
    }

    @Test
    void callsignListenerStaysSilentForNonCallsignText() {
        // Synthesise a single "T" character at 700 Hz. The text
        // listener should fire; the callsign listener should NOT —
        // "T" isn't a valid callsign and the scorer should reject
        // it via the regex (no district digit).
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        AtomicReference<String> textSeen = new AtomicReference<>("");
        AtomicReference<String> callSeen = new AtomicReference<>("");
        d.setListener(f -> textSeen.updateAndGet(prev -> prev + f.text));
        d.setCallsignListener(c -> callSeen.set(c.callsign));

        d.processFrame(SILENCE, snapOnePeak(700.0, 0), 0);
        int wpm = 20;
        int dit = (int) Math.round(SAMPLE_RATE * 1200.0 / 1000.0 / wpm);
        float[] morseT = synthesizeMarkSpace(
            700.0, new int[]{3 * dit}, new int[]{8 * dit});
        feedInFrames(d, morseT, 700.0);

        assertTrue(textSeen.get().contains("T"),
            "text listener should still see decoded 'T'");
        assertEquals("", callSeen.get(),
            "callsign listener should not fire for a bare 'T' — scorer rejects it");
    }

    @Test
    void listenerFiresOnDecodedText() {
        // Feed a synthetic CW "T" (one dah) at 700 Hz; the per-channel
        // CwMode should slice it, decode it, and the decoder fleet's
        // listener should fire with the resulting text.
        MultiCarrierDecoder d = new MultiCarrierDecoder(SAMPLE_RATE);
        AtomicReference<String> seenText = new AtomicReference<>("");
        d.setListener(f -> seenText.updateAndGet(prev -> prev + f.text));

        // Spawn the channel up front so the decoder's BPF is settled
        // before the tone arrives.
        d.processFrame(SILENCE, snapOnePeak(700.0, 0), 0);

        // Synthesise a clean 20 WPM "T" (one dah = 180 ms = 1440 samples)
        // surrounded by enough silence to flush a word-space.
        int wpm   = 20;
        int dit   = (int) Math.round(SAMPLE_RATE * 1200.0 / 1000.0 / wpm);   // 480 samples
        int dah   = 3 * dit;
        int gap   = 8 * dit;
        float[] morseT = synthesizeMarkSpace(700.0, new int[]{dah}, new int[]{gap});

        feedInFrames(d, morseT, 700.0);

        String got = seenText.get();
        assertFalse(got.isEmpty(),
            "expected at least one character decoded, got: '" + got + "'");
        assertTrue(got.contains("T"),
            "expected decoded text to contain 'T', got: '" + got + "' " +
            "(speed-adapt may have classified the dah differently — investigate " +
            "if this stays flaky on the build machine)");
    }

    // ── Test helpers ──────────────────────────────────────────────────

    /**
     * Build a CW audio buffer alternating marks (tone bursts) and
     * spaces (silence). Marks use raised-cosine on/off ramps so the
     * envelope follower doesn't trip on a step transient.
     *
     * @param freqHz       tone frequency
     * @param markSamples  duration of each mark, in samples
     * @param spaceSamples duration of each space (silence) AFTER each
     *                     mark, in samples; same length as markSamples
     */
    private static float[] synthesizeMarkSpace(double freqHz,
                                               int[] markSamples,
                                               int[] spaceSamples) {
        int total = 0;
        for (int m : markSamples)  total += m;
        for (int s : spaceSamples) total += s;
        float[] buf = new float[total];
        int idx = 0;
        int rampSamples = Math.min(96, markSamples[0] / 4);   // 12 ms @ 8 kHz
        for (int i = 0; i < markSamples.length; i++) {
            int m = markSamples[i];
            for (int n = 0; n < m; n++) {
                double envelope;
                if (n < rampSamples) {
                    envelope = 0.5 * (1.0 - Math.cos(Math.PI * n / rampSamples));
                } else if (n > m - rampSamples) {
                    envelope = 0.5 * (1.0 - Math.cos(Math.PI * (m - n) / rampSamples));
                } else {
                    envelope = 1.0;
                }
                buf[idx++] = (float) (0.5 * envelope *
                        Math.sin(2.0 * Math.PI * freqHz * (idx - 1) / SAMPLE_RATE));
            }
            int s = spaceSamples[i];
            idx += s; // already zero-filled
        }
        return buf;
    }

    /** Slice an audio buffer into FRAME_SIZE chunks and feed each to
     *  the decoder, refreshing the snapshot every frame so the channel
     *  doesn't reap mid-tone. */
    private static void feedInFrames(MultiCarrierDecoder d, float[] audio, double freqHz) {
        long t = 1_000;
        for (int p = 0; p < audio.length; p += FRAME_SIZE) {
            float[] frame = new float[FRAME_SIZE];
            int copy = Math.min(FRAME_SIZE, audio.length - p);
            System.arraycopy(audio, p, frame, 0, copy);
            d.processFrame(frame, snapOnePeak(freqHz, t), 0);
            t += 32;   // ~31 fps cadence
        }
        // Trailing silent frames so the decoder can flush its pending
        // character on a word-space timeout.
        for (int i = 0; i < 30; i++) {
            d.processFrame(new float[FRAME_SIZE], snapOnePeak(freqHz, t), 0);
            t += 32;
        }
    }
}
