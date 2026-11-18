package com.hamradio.modem.mode;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.FftAnalyzer;
import com.hamradio.modem.model.SignalSnapshot;
import com.hamradio.modem.tx.OliviaTransmitter;
import com.hamradio.modem.tx.SampleSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Olivia sent by the suite's transmitter and decoded by its receiver, over band noise, starting at awkward sample offsets. */
class OliviaRoundTripTest {

    private static final String TEXT = "CQ CQ DE W1AW W1AW K ";

    /** Decode {@code text} sent after {@code leadSamples} of noise; returns everything the decoder emitted. */
    static String roundTrip(String text, int leadSamples, double noise) { return roundTrip(text, leadSamples, noise, 1500.0); }

    /** The same, with the transmitter centred on {@code centreHz} (the receiver starts at 1500 Hz and has to find it). */
    static String roundTrip(String text, int leadSamples, double noise, double centreHz) {
        OliviaTransmitter tx = new OliviaTransmitter(AudioEngine.SAMPLE_RATE, 8, 500, centreHz, 0.5);
        OliviaMode rx = new OliviaMode();
        FftAnalyzer fft = new FftAnalyzer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
        SampleSource src = tx.createSampleSource(text + text + text);
        Random rnd = new Random(7);
        StringBuilder out = new StringBuilder();
        int n = AudioEngine.FRAME_SIZE, lead = leadSamples, tail = 0;
        while (tail < 40) {
            float[] f = new float[n];
            int i = 0;
            while (i < n && lead > 0) { i++; lead--; }                    // the lead-in: noise only
            int got = src.isFinished() ? 0 : src.read(f, i, n - i);
            if (got <= 0 && lead == 0) tail++;
            for (int k = 0; k < n; k++) f[k] += (float) (rnd.nextGaussian() * noise);
            FftAnalyzer.SpectrumResult r = fft.analyze(f);
            SignalSnapshot s = new SignalSnapshot(f, r.magnitudes(), r.rms(), r.peakFrequencyHz(), AudioEngine.SAMPLE_RATE);
            rx.process(s, 14_072_000L).ifPresent(m -> out.append(m.getText()));
        }
        src.close();
        return out.toString();
    }

    /**
     * Whether a transmission decoded used to depend on where in the audio stream it started (punch list 3-10): only 20 of 85
     * start offsets came out right, and the rest were garbage ("SASATUG1QG...") or a scrambled copy. Every offset here
     * failed before the fix: 0 / 5 / 97 (block sync locked 16 symbols off), 776 / 1843 (AFC retuned half a tone off),
     * 5820 / 7081 (symbol timing locked while the signal was still arriving), 3001. The text must also keep its word
     * spaces, which used to be trimmed off at block edges ("CQCQDE").
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 5, 77, 97, 123, 776, 1843, 3001, 5820, 7081})
    void decodesFromAnyStartingOffset(int lead) {
        String got = roundTrip(TEXT, lead, 0.02);
        assertTrue(got.contains("CQ CQ DE W1AW W1AW K"), "lead " + lead + ": decoded \"" + got + "\"");
    }

    /** A wider sweep: one start offset every 97 samples across a whole 64-symbol block (85 offsets). All must decode. */
    @Test
    void decodesFromEveryOffsetAcrossABlock() {
        StringBuilder bad = new StringBuilder();
        for (int lead = 0; lead < 8192; lead += 97)
            if (!roundTrip(TEXT, lead, 0.02).contains("CQ CQ DE W1AW W1AW K")) bad.append(lead).append(' ');
        assertTrue(bad.isEmpty(), "start offsets that did not decode: " + bad);
    }

    /** The reference recording made by tools/j-digi-test-signals (an independent Python encoder), through the same receiver. */
    static String decodeWav(java.nio.file.Path wav) throws Exception {
        OliviaMode rx = new OliviaMode();
        FftAnalyzer fft = new FftAnalyzer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
        byte[] all = java.nio.file.Files.readAllBytes(wav);
        int n = AudioEngine.FRAME_SIZE, total = (all.length - 44) / 2;
        StringBuilder out = new StringBuilder();
        for (int pos = 0; pos < total + 40 * n; pos += n) {
            float[] f = new float[n];
            for (int k = 0; k < n && pos + k < total; k++) {
                int i = 44 + 2 * (pos + k);
                f[k] = (short) ((all[i] & 0xFF) | (all[i + 1] << 8)) / 32768f;
            }
            FftAnalyzer.SpectrumResult r = fft.analyze(f);
            SignalSnapshot s = new SignalSnapshot(f, r.magnitudes(), r.rms(), r.peakFrequencyHz(), AudioEngine.SAMPLE_RATE);
            rx.process(s, 14_072_000L).ifPresent(m -> out.append(m.getText()));
        }
        return out.toString();
    }

    private static String reference(String centreHz) throws Exception {
        java.nio.file.Path wav = java.nio.file.Path.of("../tools/j-digi-test-signals/olivia_test_suite/olivia_8_500_" + centreHz + "hz.wav");
        org.junit.jupiter.api.Assumptions.assumeTrue(java.nio.file.Files.exists(wav), "reference recording not present");
        return decodeWav(wav);
    }

    /** The independent reference recording, centred. */
    @Test
    void decodesTheReferenceRecording() throws Exception {
        String got = reference("1500");
        assertTrue(got.contains("CQ CQ CQ DE W3XYZ K"), "decoded \"" + got + "\"");
    }

    /**
     * The same, 300 Hz either side: AFC can only find the signal once it has heard much of it. These used to keep only
     * "YZ K" (punch list 3-11): the retune threw the buffered audio away and decoding began at the lock point. Now the
     * audio is kept and decoding steps back to where the signal began, so the whole message comes out.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1200", "1800"})
    void decodesAnOffCentreReferenceRecordingInFull(String centreHz) throws Exception {
        String got = reference(centreHz);
        assertTrue(got.contains("CQ CQ CQ DE W3XYZ K"), centreHz + " Hz: decoded \"" + got + "\"");
    }

    /** Our own transmitter 250 Hz off the receiver's centre, after some band noise: the whole text, from the start. */
    @ParameterizedTest
    @ValueSource(ints = {0, 3001})
    void decodesOurOwnOffCentreSignalInFull(int lead) {
        String got = roundTrip(TEXT, lead, 0.02, 1250.0);
        assertTrue(got.contains("CQ CQ DE W1AW W1AW K"), "lead " + lead + ": decoded \"" + got + "\"");
    }

    /**
     * Listening to band noise must not grow the audio buffer without end. Unlocked, it never shrank and doubled whenever it
     * filled (about 100 MB an hour); it now keeps the last 16 s.
     */
    @Test
    void longNoiseDoesNotGrowTheBuffer() throws Exception {
        OliviaMode rx = new OliviaMode();
        FftAnalyzer fft = new FftAnalyzer(AudioEngine.FRAME_SIZE, AudioEngine.SAMPLE_RATE);
        Random rnd = new Random(3);
        int frames = (int) (120 * AudioEngine.SAMPLE_RATE / AudioEngine.FRAME_SIZE);      // two minutes
        for (int i = 0; i < frames; i++) {
            float[] f = new float[AudioEngine.FRAME_SIZE];
            for (int k = 0; k < f.length; k++) f[k] = (float) (rnd.nextGaussian() * 0.05);
            FftAnalyzer.SpectrumResult r = fft.analyze(f);
            rx.process(new SignalSnapshot(f, r.magnitudes(), r.rms(), r.peakFrequencyHz(), AudioEngine.SAMPLE_RATE), 0L);
        }
        java.lang.reflect.Field fb = OliviaMode.class.getDeclaredField("sampleBuf"); fb.setAccessible(true);
        java.lang.reflect.Field fn = OliviaMode.class.getDeclaredField("nSamples"); fn.setAccessible(true);
        int held = fn.getInt(rx), capacity = ((float[]) fb.get(rx)).length;
        assertTrue(held <= 16 * 8000 + AudioEngine.FRAME_SIZE, "samples held after 2 min of noise: " + held);
        assertTrue(capacity <= 262_144, "buffer capacity after 2 min of noise: " + capacity);
    }

    @ParameterizedTest
    @ValueSource(ints = {77, 3001})
    void decodesThroughHeavierNoise(int lead) {
        String got = roundTrip(TEXT, lead, 0.08);
        assertTrue(got.contains("CQ CQ DE W1AW W1AW K"), "lead " + lead + ": decoded \"" + got + "\"");
    }
}
