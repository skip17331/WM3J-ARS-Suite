package com.hamradio.modem.dsp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SignalClassifier tests — feed synthetic audio that mimics the spectral and
 * temporal signature of each mode, then assert the classifier picks the right
 * label. We don't aim for real-world accuracy here; the goal is to keep the
 * decision tree honest as it evolves and to catch regressions in the
 * bandwidth / envelope helpers.
 */
class SignalClassifierTest {

    private static final float SAMPLE_RATE = 8000f;
    private static final int   FRAME       = 1024;

    private SignalClassifier classifier;
    private final Random rng = new Random(0xC1A551F1EDL);

    @BeforeEach
    void setUp() {
        classifier = new SignalClassifier(SAMPLE_RATE, FRAME);
    }

    // ── Bandwidth helper ────────────────────────────────────────────

    @Test
    void bandwidthOfNarrowToneIsSmall() {
        // Single 700 Hz tone, no noise — bandwidth at -10 dB should be small.
        float[] frame = sineFrame(700, 0.5);
        var spec = new FftAnalyzer(FRAME, SAMPLE_RATE).analyze(frame);
        int peakBin = (int) Math.round(700.0 / (SAMPLE_RATE / FRAME));
        double bw = SignalClassifier.bandwidthAtMinus10dB(spec.magnitudes(), peakBin, SAMPLE_RATE / FRAME);
        assertTrue(bw < 80, "narrow tone -10 dB bandwidth should be < 80 Hz, got " + bw);
    }

    @Test
    void rttySecondPeakIsDetected() {
        // Mark + space tones; classifier's peakAt() must spot the partner.
        float[] frame = addSines(700, 0.5, 870, 0.5);  // 170 Hz spacing
        var spec = new FftAnalyzer(FRAME, SAMPLE_RATE).analyze(frame);
        int peakBin = (int) Math.round(700.0 / (SAMPLE_RATE / FRAME));
        boolean found = SignalClassifier.peakAt(
                spec.magnitudes(), peakBin, 170.0, SAMPLE_RATE / FRAME, spec.magnitudes()[peakBin]);
        assertTrue(found, "should find RTTY mark/space partner ~170 Hz away");
    }

    // ── End-to-end classifier ───────────────────────────────────────

    @Test
    void emptyBufferReturnsUnknown() {
        var r = classifier.classify(700);
        assertEquals(SignalClassifier.ClassifiedMode.UNKNOWN, r.mode);
    }

    @Test
    void outOfBandClickReturnsUnknown() {
        feed(makeContinuousTone(1000, 0.4), 32);  // a real signal in the buffer…
        var r = classifier.classify(5000);        // …but the click is above Nyquist
        assertEquals(SignalClassifier.ClassifiedMode.UNKNOWN, r.mode);
    }

    @Test
    void steadyToneAt700IsClassifiedNarrowAndSteady() {
        // A continuous unkeyed carrier — narrow + steady envelope → PSK31 branch.
        feed(makeContinuousTone(700, 0.4), 32);
        var r = classifier.classify(700);
        assertNotEquals(SignalClassifier.ClassifiedMode.UNKNOWN, r.mode);
        // Narrow + steady → PSK31 in our decision tree (or CW if envelope CV ticks
        // up enough — accept either narrow-mode label, just not SSB/RTTY).
        assertTrue(r.mode == SignalClassifier.ClassifiedMode.PSK31
                || r.mode == SignalClassifier.ClassifiedMode.CW,
                "expected narrow-mode label, got " + r.mode);
        assertTrue(r.bandwidthHz < 200, "steady tone bandwidth should be narrow, got " + r.bandwidthHz);
    }

    @Test
    void keyedToneIsClassifiedAsCw() {
        // 700 Hz tone gated on/off every ~3 frames (~400 ms) — looks like CW.
        feed(makeKeyedTone(700, 0.5, 3), 32);
        var r = classifier.classify(700);
        assertEquals(SignalClassifier.ClassifiedMode.CW, r.mode,
                "expected CW for keyed narrow tone, got " + r);
    }

    @Test
    void twoTonesAt170HzApartIsClassifiedAsRtty() {
        feed(makeTwoTones(2125, 2295, 0.4), 32);
        var r = classifier.classify(2125);
        assertEquals(SignalClassifier.ClassifiedMode.RTTY, r.mode,
                "expected RTTY for 170 Hz dual-tone, got " + r);
    }

    @Test
    void ax25BellPairWithBurstyEnvelopeIsClassifiedAsAx25() {
        // 1200 + 2200 Hz tone pair (Bell 202), gated on/off in packet bursts.
        feed(makeKeyedTwoTones(1200, 2200, 0.45, 4), 32);
        var r = classifier.classify(1200);
        assertEquals(SignalClassifier.ClassifiedMode.AX25, r.mode,
                "expected AX25 for Bell 202 bursty pair, got " + r);
    }

    @Test
    void multiToneAround300HzWideIsClassifiedAsMfsk16() {
        // 16 evenly-spaced tones across ~300 Hz — MFSK16's signature shape.
        feed(makeMultiTone(1500, 16, 18, 0.4), 32);  // 18 Hz spacing × 16 tones ≈ 288 Hz
        var r = classifier.classify(1500);
        assertEquals(SignalClassifier.ClassifiedMode.MFSK16, r.mode,
                "expected MFSK16 for ~300 Hz multi-tone, got " + r);
    }

    @Test
    void multiToneAround500HzWideIsClassifiedAsDominoEx() {
        // ~500 Hz wide — falls in the DOMINOEX/Olivia-500 ambiguity zone.
        feed(makeMultiTone(1500, 12, 40, 0.4), 32);  // 40 Hz × 12 ≈ 480 Hz
        var r = classifier.classify(1500);
        assertEquals(SignalClassifier.ClassifiedMode.DOMINOEX, r.mode,
                "expected DOMINOEX for ~500 Hz multi-tone, got " + r);
    }

    @Test
    void multiToneAround1000HzWideIsClassifiedAsOlivia() {
        // ~1000 Hz wide multi-tone — Olivia 1000/32 territory.
        feed(makeMultiTone(1500, 16, 60, 0.35), 32);  // 60 Hz × 16 = 960 Hz
        var r = classifier.classify(1500);
        assertEquals(SignalClassifier.ClassifiedMode.OLIVIA, r.mode,
                "expected OLIVIA for ~1000 Hz multi-tone, got " + r);
    }

    @Test
    void wideNoiseIsClassifiedAsSsb() {
        // Band-limited noise across most of the audio passband — bandwidth
        // measurement will exceed 1500 Hz and trigger the SSB branch.
        feed(makeWideNoise(0.3), 32);
        var r = classifier.classify(1500);
        assertEquals(SignalClassifier.ClassifiedMode.SSB, r.mode,
                "expected SSB for wide noise, got " + r);
    }

    // ── Synthetic audio helpers ─────────────────────────────────────

    private void feed(float[][] frames, int n) {
        for (int i = 0; i < n; i++) classifier.accept(frames[i % frames.length]);
    }

    private static float[] sineFrame(double hz, double amp) {
        float[] f = new float[FRAME];
        for (int i = 0; i < FRAME; i++) {
            f[i] = (float) (amp * Math.sin(2 * Math.PI * hz * i / SAMPLE_RATE));
        }
        return f;
    }

    private static float[] addSines(double hz1, double a1, double hz2, double a2) {
        float[] f = new float[FRAME];
        for (int i = 0; i < FRAME; i++) {
            f[i] = (float) (a1 * Math.sin(2 * Math.PI * hz1 * i / SAMPLE_RATE)
                          + a2 * Math.sin(2 * Math.PI * hz2 * i / SAMPLE_RATE));
        }
        return f;
    }

    private static float[][] makeContinuousTone(double hz, double amp) {
        float[][] frames = new float[1][];
        frames[0] = sineFrame(hz, amp);
        return frames;
    }

    private static float[][] makeKeyedTone(double hz, double amp, int onFramesOff) {
        // Pattern: <onFramesOff> frames on, <onFramesOff> frames silent. Cycle.
        float[] on  = sineFrame(hz, amp);
        float[] off = new float[FRAME];
        float[][] cycle = new float[onFramesOff * 2][];
        for (int i = 0; i < onFramesOff; i++)              cycle[i] = on;
        for (int i = onFramesOff; i < onFramesOff * 2; i++) cycle[i] = off;
        return cycle;
    }

    private static float[][] makeTwoTones(double hz1, double hz2, double amp) {
        float[][] frames = new float[1][];
        frames[0] = addSines(hz1, amp, hz2, amp);
        return frames;
    }

    /** Two tones, gated on/off — packet-radio AX25 burst pattern. */
    private static float[][] makeKeyedTwoTones(double hz1, double hz2, double amp, int onFramesOff) {
        float[] on = addSines(hz1, amp, hz2, amp);
        float[] off = new float[FRAME];
        float[][] cycle = new float[onFramesOff * 2][];
        for (int i = 0; i < onFramesOff; i++)              cycle[i] = on;
        for (int i = onFramesOff; i < onFramesOff * 2; i++) cycle[i] = off;
        return cycle;
    }

    /** {@code count} evenly-spaced tones centered on {@code centerHz}, each
     *  {@code spacingHz} apart — synthetic stand-in for OLIVIA/MFSK/DOMINOEX. */
    private static float[][] makeMultiTone(double centerHz, int count, double spacingHz, double amp) {
        float[] f = new float[FRAME];
        double startHz = centerHz - (count - 1) * spacingHz / 2.0;
        double per = amp / Math.sqrt(count);
        for (int t = 0; t < count; t++) {
            double hz = startHz + t * spacingHz;
            for (int i = 0; i < FRAME; i++) {
                f[i] += (float) (per * Math.sin(2 * Math.PI * hz * i / SAMPLE_RATE));
            }
        }
        return new float[][] { f };
    }

    private float[][] makeWideNoise(double amp) {
        // Swept-frequency tone (chirp) covering the full SSB voice band
        // 300–2700 Hz over each frame. Each frame's FFT smears the energy
        // across that 2.4 kHz span — a faithful stand-in for the continuous
        // spectrum of voice SSB and the cleanest way to drive the classifier's
        // bandwidth-based SSB branch. White noise produces too many random
        // bin dips that break the -10 dB walk.
        float[][] frames = new float[8][];
        double startHz = 300, endHz = 2700;
        for (int fi = 0; fi < frames.length; fi++) {
            float[] f = new float[FRAME];
            for (int i = 0; i < FRAME; i++) {
                double t = i / (double) SAMPLE_RATE;
                double hz = startHz + (endHz - startHz) * (i / (double) FRAME);
                f[i] = (float) (amp * Math.sin(2 * Math.PI * hz * t));
            }
            frames[fi] = f;
        }
        return frames;
    }
}
