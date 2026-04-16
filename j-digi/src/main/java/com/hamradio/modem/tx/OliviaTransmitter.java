package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;

import java.util.Arrays;

/**
 * Olivia MFSK transmitter.
 *
 * Default: 8 tones, 500 Hz bandwidth, 1500 Hz centre, 8000 Hz sample rate.
 *
 * Encoding pipeline:
 *   1. Map each input character to a 6-bit Olivia code (ASCII − 0x20, clamped
 *      to 0–63; lowercase mapped to uppercase; unknowns → space).
 *   2. Pad with lead-in / lead-out space blocks.
 *   3. For each 64-symbol block, take log2(numTones) characters (one per bit plane).
 *   4. For each symbol position k (0–63) and bit plane b:
 *        walshBit = popcount(charCode_b & k) & 1
 *        bit b of symbol[k] = walshBit
 *      The symbol index selects which of the N tones to transmit.
 *   5. Generate phase-continuous MFSK audio at constant amplitude.
 *
 * Tone frequencies:
 *   lowestTone = centre − BW/2 + spacing/2
 *   tone[i]    = lowestTone + i × spacing
 */
public class OliviaTransmitter implements DigitalTransmitter {

    private static final int    DEFAULT_TONES      = 8;
    private static final int    DEFAULT_BANDWIDTH  = 500;
    private static final double DEFAULT_CENTER     = 1500.0;
    private static final double DEFAULT_AMPLITUDE  = 0.45;

    private static final int    BLOCK_SYMBOLS      = 64;
    private static final int    CHAR_SET_SIZE      = 64;
    private static final char   CHAR_OFFSET        = 0x20;
    /** Lead-in / lead-out blocks of space characters (give the RX time to sync). */
    private static final int    LEAD_BLOCKS        = 3;

    private final double   sampleRate;
    private final int      numTones;
    private final int      bitsPerSymbol;
    private final double   toneSpacing;
    private final double[] toneFreqs;
    private final double   amplitude;

    // =================================================================
    // Constructors
    // =================================================================

    public OliviaTransmitter() {
        this(AudioEngine.SAMPLE_RATE, DEFAULT_TONES, DEFAULT_BANDWIDTH,
             DEFAULT_CENTER, DEFAULT_AMPLITUDE);
    }

    public OliviaTransmitter(double sampleRate, int numTones, int bandwidthHz,
                             double centerHz, double amplitude) {
        if (numTones < 2 || (numTones & (numTones - 1)) != 0)
            throw new IllegalArgumentException("numTones must be a power of 2, got " + numTones);

        this.sampleRate    = sampleRate;
        this.numTones      = numTones;
        this.bitsPerSymbol = Integer.numberOfTrailingZeros(numTones);
        this.toneSpacing   = (double) bandwidthHz / numTones;
        this.amplitude     = amplitude;

        double lowestTone = centerHz - (double) bandwidthHz / 2.0 + toneSpacing / 2.0;
        this.toneFreqs = new double[numTones];
        for (int i = 0; i < numTones; i++) toneFreqs[i] = lowestTone + i * toneSpacing;
    }

    // =================================================================
    // DigitalTransmitter contract
    // =================================================================

    @Override
    public String getName() { return "OLIVIA"; }

    @Override
    public SampleSource createSampleSource(String text) {
        int spS = Math.max(1, (int) Math.round(sampleRate / toneSpacing));
        int[] symbols = buildSymbolStream(text);
        return new OliviaSampleSource(symbols, toneFreqs, spS, sampleRate, amplitude);
    }

    // =================================================================
    // Encoder
    // =================================================================

    /**
     * Convert text to a flat array of tone indices (0..numTones-1).
     * Padded with LEAD_BLOCKS of spaces before and after the actual content.
     */
    private int[] buildSymbolStream(String text) {
        if (text == null) text = "";

        // Map to 6-bit Olivia character codes
        int[] codes = normalizeText(text);

        // Pad to a whole number of blocks (bitsPerSymbol chars per block)
        int contentBlocks = (codes.length + bitsPerSymbol - 1) / bitsPerSymbol;
        int totalBlocks   = LEAD_BLOCKS + contentBlocks + LEAD_BLOCKS;
        int totalChars    = totalBlocks * bitsPerSymbol;

        int[] padded = new int[totalChars]; // zero-filled = space
        int contentStart = LEAD_BLOCKS * bitsPerSymbol;
        System.arraycopy(codes, 0, padded, contentStart,
                Math.min(codes.length, totalChars - contentStart));

        // Encode each block into BLOCK_SYMBOLS tone indices
        int[] symbols = new int[totalBlocks * BLOCK_SYMBOLS];
        for (int blk = 0; blk < totalBlocks; blk++) {
            for (int k = 0; k < BLOCK_SYMBOLS; k++) {
                int sym = 0;
                for (int b = 0; b < bitsPerSymbol; b++) {
                    int charCode   = padded[blk * bitsPerSymbol + b];
                    int walshBit   = Integer.bitCount(charCode & k) & 1;
                    sym           |= (walshBit << b);
                }
                symbols[blk * BLOCK_SYMBOLS + k] = sym;
            }
        }
        return symbols;
    }

    /**
     * Map each character in text to a 6-bit Olivia code (0–63 = ASCII 0x20–0x5F).
     * Lowercase → uppercase.  Out-of-range → space (0).
     */
    private static int[] normalizeText(String text) {
        int[] codes = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'a' && c <= 'z') c = (char)(c - 32); // to uppercase
            int code = c - CHAR_OFFSET;
            codes[i] = (code >= 0 && code < CHAR_SET_SIZE) ? code : 0;
        }
        return codes;
    }

    // =================================================================
    // SampleSource — phase-continuous MFSK audio
    // =================================================================

    private static final class OliviaSampleSource implements SampleSource {

        private final int[]    symbols;
        private final double[] toneFreqs;
        private final int      spS;
        private final double   sampleRate;
        private final double   amplitude;

        private int    symIdx      = 0;
        private int    sampleInSym = 0;
        /** Running phase (radians); kept continuous across tone changes. */
        private double phase       = 0.0;

        private boolean finished = false;
        private boolean closed   = false;

        OliviaSampleSource(int[] symbols, double[] toneFreqs, int spS,
                           double sampleRate, double amplitude) {
            this.symbols    = symbols;
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
                if (symIdx >= symbols.length) { finished = true; break; }

                double freq      = toneFreqs[symbols[symIdx]];
                double phaseStep = 2.0 * Math.PI * freq / sampleRate;

                buffer[offset + written] = (float)(Math.sin(phase) * amplitude);
                phase += phaseStep;
                // Keep phase in [0, 2π) to avoid floating-point drift
                if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI;

                written++;
                if (++sampleInSym >= spS) {
                    sampleInSym = 0;
                    symIdx++;
                    // Phase is already continuous — no reset needed
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
