package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.ViterbiK7;

/**
 * MFSK16 transmitter with K=7 rate-1/2 convolutional FEC.
 *
 * Parameters:
 *   16 tones, 15.625 Hz spacing, 15.625 baud
 *   Default centre : 1500 Hz  (tones at bins 89–104 at 8 kHz)
 *   Default amplitude: 0.45
 *
 * Encoding pipeline:
 *   1. Each ASCII byte → 8 data bits (LSB first).
 *   2. Each data bit → rate-1/2 K=7 convolution → 2 encoded bits.
 *      (16 encoded bits per byte.)
 *   3. Pack 4 encoded bits per nibble (MSB first within nibble).
 *      → 4 nibbles per byte.
 *   4. IFK+ mod 16: tx_tone = (prev + nibble + 1) mod 16.
 *      → 4 tones per byte → character rate 15.625/4 ≈ 3.9 chars/sec.
 *   5. Phase-continuous sine tone, 512 samples/tone at 8 kHz.
 *
 * Preamble (16 × 0x00) primes the receiver's Viterbi pipeline.
 * Postamble (8 × 0x00) drains it after the last data byte.
 */
public class Mfsk16Transmitter implements DigitalTransmitter {

    private static final int    NUM_TONES       = 16;
    private static final int    FIRST_BIN       = 89;
    private static final double SPACING         = 8000.0 / 512;  // 15.625 Hz
    private static final double DEFAULT_AMPLITUDE = 0.45;
    /** Number of idle (0x00) bytes sent before text, and after. */
    private static final int    PREAMBLE_BYTES  = 16;
    private static final int    POSTAMBLE_BYTES = 8;

    private final double   sampleRate;
    private final double[] toneFreqs;
    private final double   amplitude;

    // =================================================================
    // Constructors
    // =================================================================

    public Mfsk16Transmitter() {
        this(AudioEngine.SAMPLE_RATE, DEFAULT_AMPLITUDE);
    }

    public Mfsk16Transmitter(double sampleRate, double amplitude) {
        this.sampleRate = sampleRate;
        this.amplitude  = amplitude;
        double spacing  = sampleRate / 512;
        this.toneFreqs  = new double[NUM_TONES];
        for (int i = 0; i < NUM_TONES; i++) toneFreqs[i] = (FIRST_BIN + i) * spacing;
    }

    // =================================================================
    // DigitalTransmitter contract
    // =================================================================

    @Override
    public String getName() { return "MFSK16"; }

    @Override
    public SampleSource createSampleSource(String text) {
        int spS = (int) Math.round(sampleRate / SPACING);
        int[] toneStream = buildToneStream(text == null ? "" : text);
        return new Mfsk16SampleSource(toneStream, toneFreqs, spS, sampleRate, amplitude);
    }

    // =================================================================
    // Encoder
    // =================================================================

    /**
     * Encode text to a tone stream:
     *   bytes → K7 convolutional encode → nibbles → IFK+ tones.
     *
     * Structure: PREAMBLE_BYTES idle + text + POSTAMBLE_BYTES idle.
     * Each byte produces 4 nibbles (4 tones) due to rate-1/2 FEC.
     */
    private static int[] buildToneStream(String text) {
        int totalBytes = PREAMBLE_BYTES + text.length() + POSTAMBLE_BYTES;
        byte[] bytes = new byte[totalBytes];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r')        bytes[PREAMBLE_BYTES + i] = (byte) c;
            else if (c >= 0x20 && c <= 0x7E)   bytes[PREAMBLE_BYTES + i] = (byte) c;
            else                               bytes[PREAMBLE_BYTES + i] = (byte) ' ';
        }

        // K7 convolutional encoder: 1 data bit → 2 encoded bits
        // Each byte → 8 data bits → 16 encoded bits → 4 nibbles of 4 encoded bits
        int[] tones    = new int[totalBytes * 4];  // 4 tones per byte with FEC
        int   prevTone = 0;
        int   convState = 0;   // 6-bit encoder shift register (starts at 0)
        int   toneIdx  = 0;

        for (int i = 0; i < totalBytes; i++) {
            int b = bytes[i] & 0xFF;

            // Encode 8 data bits → 16 encoded bits
            int encoded = 0;        // holds 16 encoded bits, bit 15 = first out
            for (int bit = 0; bit < 8; bit++) {
                int u   = (b >> bit) & 1;  // LSB first
                int sr  = (u << (ViterbiK7.K - 1)) | convState;
                int o0  = Integer.bitCount(sr & ViterbiK7.G0) & 1;
                int o1  = Integer.bitCount(sr & ViterbiK7.G1) & 1;
                convState = sr >>> 1;
                // Pack encoded bits MSB-first: bit (15 - bit*2) = o0, (14 - bit*2) = o1
                encoded |= (o0 << (15 - bit * 2));
                encoded |= (o1 << (14 - bit * 2));
            }

            // Split 16 encoded bits into 4 nibbles and IFK+-encode each
            for (int n = 0; n < 4; n++) {
                int nibble = (encoded >>> (12 - n * 4)) & 0x0F;
                int tone   = (prevTone + nibble + 1) % NUM_TONES;
                tones[toneIdx++] = tone;
                prevTone = tone;
            }
        }
        return tones;
    }

    // =================================================================
    // SampleSource — phase-continuous MFSK audio
    // =================================================================

    private static final class Mfsk16SampleSource implements SampleSource {

        private final int[]    tones;
        private final double[] toneFreqs;
        private final int      spS;
        private final double   sampleRate;
        private final double   amplitude;

        private int    toneIdx     = 0;
        private int    sampleInSym = 0;
        private double phase       = 0.0;

        private boolean finished = false;
        private boolean closed   = false;

        Mfsk16SampleSource(int[] tones, double[] toneFreqs, int spS,
                           double sampleRate, double amplitude) {
            this.tones      = tones;
            this.toneFreqs  = toneFreqs;
            this.spS        = spS;
            this.sampleRate = sampleRate;
            this.amplitude  = amplitude;
        }

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (closed || finished || buffer == null || length <= 0) return -1;

            int written = 0;
            while (written < length) {
                if (toneIdx >= tones.length) { finished = true; break; }

                double freq      = toneFreqs[tones[toneIdx]];
                double phaseStep = 2.0 * Math.PI * freq / sampleRate;

                buffer[offset + written] = (float)(Math.sin(phase) * amplitude);
                phase += phaseStep;
                if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI;

                written++;
                if (++sampleInSym >= spS) {
                    sampleInSym = 0;
                    toneIdx++;
                }
            }
            return written > 0 ? written : -1;
        }

        @Override
        public boolean isFinished() { return finished || closed; }

        @Override
        public void close() { closed = true; finished = true; }
    }
}
