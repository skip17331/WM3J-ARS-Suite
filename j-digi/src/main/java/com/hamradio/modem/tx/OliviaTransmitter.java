package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.dsp.OliviaCodec;

import java.util.Arrays;

/**
 * Olivia MFSK transmitter: standard Olivia, so fldigi and other Olivia programs can read it (punch list 3-12).
 *
 * Default: 8 tones, 500 Hz bandwidth, 1500 Hz centre, 8000 Hz sample rate.
 *
 * Coding is {@link OliviaCodec}: 7-bit ASCII, one character per bit of the symbol, each a scrambled 64-chip Walsh function spread
 * diagonally across the block's symbols, Gray-coded onto the tones. A few blocks of NUL characters go before and after the text
 * (the receiver uses them to find the block timing; Olivia programs show nothing for them).
 *
 * Each symbol is a tone shaped by a raised cosine two symbol periods long, overlapping its neighbours by half, with the phase
 * stepped by a quarter turn either way between symbols, as the reference modulator does: a clean, narrow spectrum.
 *
 * Tone frequencies:
 *   lowestTone = centre - BW/2 + spacing/2
 *   tone[i]    = lowestTone + i * spacing
 */
public class OliviaTransmitter implements DigitalTransmitter {

    private static final int    DEFAULT_TONES      = 8;
    private static final int    DEFAULT_BANDWIDTH  = 500;
    private static final double DEFAULT_CENTER     = 1500.0;
    private static final double DEFAULT_AMPLITUDE  = 0.45;

    private static final int    BLOCK_SYMBOLS      = 64;
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
        int[] codes = normalizeText(text);
        int contentBlocks = (codes.length + bitsPerSymbol - 1) / bitsPerSymbol;
        int totalBlocks   = LEAD_BLOCKS + contentBlocks + LEAD_BLOCKS;
        int[] padded = new int[totalBlocks * bitsPerSymbol];           // zero-filled = NUL
        System.arraycopy(codes, 0, padded, LEAD_BLOCKS * bitsPerSymbol, codes.length);
        int[] symbols = new int[totalBlocks * BLOCK_SYMBOLS];
        for (int blk = 0; blk < totalBlocks; blk++) {
            int[] chars = java.util.Arrays.copyOfRange(padded, blk * bitsPerSymbol, (blk + 1) * bitsPerSymbol);
            System.arraycopy(OliviaCodec.encodeBlock(chars), 0, symbols, blk * BLOCK_SYMBOLS, BLOCK_SYMBOLS);
        }
        return symbols;
    }

    /** 7-bit ASCII as typed; a line break goes as CR LF, as Olivia programs expect; anything else outside ASCII as '?'. */
    static int[] normalizeText(String text) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') continue;
            if (c == '\n') { b.append("\r\n"); continue; }
            b.append(c >= 0x20 && c < 0x7F || c == '\t' ? c : '?');
        }
        int[] codes = new int[b.length()];
        for (int i = 0; i < codes.length; i++) codes[i] = b.charAt(i);
        return codes;
    }

    // =================================================================
    // SampleSource — phase-continuous MFSK audio
    // =================================================================

    /** Overlapping raised-cosine symbols, two periods long, the phase stepped a quarter turn either way between them. */
    private static final class OliviaSampleSource implements SampleSource {
        private final int[] symbols;
        private final double[] toneFreqs;
        private final int spS;
        private final double sampleRate, amplitude;
        private final java.util.Random rnd = new java.util.Random();
        /** Samples of the current period: the second half of symbol k-1 plus the first half of symbol k. */
        private final float[] period;
        private final float[] tail;          // the second half of the symbol last added, for the next period
        private int periodIdx = 0, posInPeriod;
        private double phase = 0;
        private boolean finished = false, closed = false;

        OliviaSampleSource(int[] symbols, double[] toneFreqs, int spS, double sampleRate, double amplitude) {
            this.symbols = symbols; this.toneFreqs = toneFreqs; this.spS = spS; this.sampleRate = sampleRate; this.amplitude = amplitude;
            period = new float[spS]; tail = new float[spS];
            posInPeriod = spS;                                    // nothing made yet
        }

        /** Period p holds the tail of symbol p-1 and the head of symbol p (none past the last: the final tail rings out). */
        private boolean nextPeriod() {
            if (periodIdx > symbols.length) return false;
            System.arraycopy(tail, 0, period, 0, spS);
            java.util.Arrays.fill(tail, 0f);
            if (periodIdx < symbols.length) {
                double w = 2 * Math.PI * toneFreqs[symbols[periodIdx]] / sampleRate;
                for (int n = 0; n < 2 * spS; n++) {
                    double shape = 0.5 * (1 - Math.cos(Math.PI * n / spS));       // 0 .. 1 .. 0 over two periods
                    float v = (float) (amplitude * shape * Math.cos(phase + w * n));
                    if (n < spS) period[n] += v; else tail[n - spS] = v;
                }
                phase += w * spS + (rnd.nextBoolean() ? Math.PI / 2 : -Math.PI / 2);
                phase %= 2 * Math.PI;
            }
            periodIdx++;
            posInPeriod = 0;
            return true;
        }

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (closed || finished || buffer == null || length <= 0) return -1;
            int written = 0;
            while (written < length) {
                if (posInPeriod >= spS && !nextPeriod()) { finished = true; break; }
                buffer[offset + written++] = period[posInPeriod++];
            }
            return written > 0 ? written : -1;
        }

        @Override public boolean isFinished() { return finished || closed; }
        @Override public void close() { closed = true; finished = true; }
    }
}
