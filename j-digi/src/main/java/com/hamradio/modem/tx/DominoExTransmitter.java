package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.mode.DominoExMode;

/**
 * DominoEX MFSK transmitter (Greenman ZL1BPU / Wassilieff ZL2AFP).
 *
 * Supports the same three variants as {@link DominoExMode}:
 * DominoEX4 (3.90625 baud), DominoEX8 (7.8125 baud), DominoEX16 (15.625 baud).
 * Default: DominoEX8.
 *
 * Encoding:
 *   1. Each character is split into two 4-bit nibbles, high nibble first.
 *   2. IFK+ with modulus 18: tx_tone = (prev + nibble + 1) mod 18.
 *      Because nibble ≤ 15, the jump is 1–16, never 0 — tone always changes.
 *   3. A preamble of PREAMBLE_BYTES idle bytes (0x00) is sent first.
 *   4. A postamble of POSTAMBLE_BYTES idle bytes flushes the receiver pipeline.
 *
 * Audio: phase-continuous MFSK, constant amplitude.
 * Tone frequencies: 18 exact FFT-bin-aligned tones centred near 1500 Hz.
 */
public class DominoExTransmitter implements DigitalTransmitter {

    private static final int    NUM_TONES       = 18;
    private static final double DEFAULT_AMPL    = 0.45;
    private static final int    PREAMBLE_BYTES  = 16;
    private static final int    POSTAMBLE_BYTES = 8;

    private final double   sampleRate;
    private final double   symbolRate;
    private final double[] toneFreqs;
    private final double   amplitude;
    private final String   modeName;

    // =================================================================
    // Constructors
    // =================================================================

    public DominoExTransmitter() {
        this(AudioEngine.SAMPLE_RATE, DominoExMode.Variant.DOMINOEX8, DEFAULT_AMPL);
    }

    public DominoExTransmitter(double sampleRate, DominoExMode.Variant variant, double amplitude) {
        this.sampleRate = sampleRate;
        this.symbolRate = variant.symbolRate;
        this.amplitude  = amplitude;
        this.modeName   = variant.name();

        int spS = Math.max(1, (int) Math.round(sampleRate / symbolRate));
        double binWidth  = sampleRate / spS;
        int firstBin = (int) Math.round(1500.0 / binWidth) - 8;
        this.toneFreqs = new double[NUM_TONES];
        for (int i = 0; i < NUM_TONES; i++) toneFreqs[i] = (firstBin + i) * binWidth;
    }

    // =================================================================
    // DigitalTransmitter contract
    // =================================================================

    @Override
    public String getName() { return modeName.replace("DOMINOEX", "DominoEX "); }

    @Override
    public SampleSource createSampleSource(String text) {
        int spS = Math.max(1, (int) Math.round(sampleRate / symbolRate));
        int[] toneStream = buildToneStream(text == null ? "" : text);
        return new DominoSampleSource(toneStream, toneFreqs, spS, sampleRate, amplitude);
    }

    // =================================================================
    // Encoder
    // =================================================================

    private static int[] buildToneStream(String text) {
        int totalBytes = PREAMBLE_BYTES + text.length() + POSTAMBLE_BYTES;
        byte[] bytes = new byte[totalBytes];  // preamble + postamble = 0x00 (idle)
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            bytes[PREAMBLE_BYTES + i] = (byte)(
                (c == '\n' || c == '\r') ? c :
                (c >= 0x20 && c <= 0x7E) ? c : ' '
            );
        }

        // Convert bytes → nibbles → IFK+ tones (mod 18)
        int[] tones = new int[totalBytes * 2];
        int prevTone = 0;
        for (int i = 0; i < totalBytes; i++) {
            int b  = bytes[i] & 0xFF;
            int hi = (b >> 4) & 0x0F;
            int lo =  b       & 0x0F;

            int t0 = (prevTone + hi + 1) % NUM_TONES;  tones[i * 2]     = t0;  prevTone = t0;
            int t1 = (prevTone + lo + 1) % NUM_TONES;  tones[i * 2 + 1] = t1;  prevTone = t1;
        }
        return tones;
    }

    // =================================================================
    // SampleSource — phase-continuous MFSK audio
    // =================================================================

    private static final class DominoSampleSource implements SampleSource {

        private final int[]    tones;
        private final double[] toneFreqs;
        private final int      spS;
        private final double   sampleRate;
        private final double   amplitude;

        private int    toneIdx     = 0;
        private int    sampleInSym = 0;
        private double phase       = 0.0;
        private boolean finished   = false;
        private boolean closed     = false;

        DominoSampleSource(int[] tones, double[] toneFreqs, int spS,
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
                if (++sampleInSym >= spS) { sampleInSym = 0; toneIdx++; }
            }
            return written > 0 ? written : -1;
        }

        @Override public boolean isFinished() { return finished || closed; }
        @Override public void    close()      { closed = true; finished = true; }
    }
}
