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
    static String roundTrip(String text, int leadSamples, double noise) {
        OliviaTransmitter tx = new OliviaTransmitter();
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
     * The same, 300 Hz either side: AFC has to find the signal. It can only do that once enough of the content has been
     * heard, and the retune starts acquisition again, so this short recording loses everything before "YZ K" (punch list
     * 3-11; the decoder did the same before the 3-10 fix). This checks AFC still finds and decodes it.
     */
    @ParameterizedTest
    @ValueSource(strings = {"1200", "1800"})
    void findsAnOffCentreReferenceRecording(String centreHz) throws Exception {
        String got = reference(centreHz);
        assertTrue(got.contains("YZ K"), centreHz + " Hz: decoded \"" + got + "\"");
    }

    @ParameterizedTest
    @ValueSource(ints = {77, 3001})
    void decodesThroughHeavierNoise(int lead) {
        String got = roundTrip(TEXT, lead, 0.08);
        assertTrue(got.contains("CQ CQ DE W1AW W1AW K"), "lead " + lead + ": decoded \"" + got + "\"");
    }
}
