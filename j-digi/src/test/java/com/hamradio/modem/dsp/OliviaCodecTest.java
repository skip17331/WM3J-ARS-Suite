package com.hamradio.modem.dsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Standard Olivia block coding (punch list 3-12). */
class OliviaCodecTest {

    /** Hard tone powers for one symbol: all energy in the one tone sent. */
    static double[][] clean(int[] tones, int numTones) {
        int bps = Integer.numberOfTrailingZeros(numTones);
        double[][] soft = new double[tones.length][];
        for (int t = 0; t < tones.length; t++) {
            double[] p = new double[numTones];
            p[tones[t]] = 1;
            soft[t] = OliviaCodec.softBits(p, bps);
        }
        return soft;
    }

    @Test void everyAsciiCharacterRoundTripsInEveryPlane() {
        for (int numTones : new int[]{4, 8, 16, 32}) {
            int bps = Integer.numberOfTrailingZeros(numTones);
            for (int c = 0; c < 128; c++) {
                int[] chars = new int[bps];
                for (int f = 0; f < bps; f++) chars[f] = (c + 37 * f) & 0x7F;
                double[][] soft = clean(OliviaCodec.encodeBlock(chars), numTones);
                for (int f = 0; f < bps; f++)
                    assertEquals(chars[f], OliviaCodec.decodeChar(soft, 0, f).ch(), numTones + " tones, char " + chars[f] + " plane " + f);
            }
        }
    }

    @Test void grayCodeIsItsOwnInverse() {
        for (int x = 0; x < 256; x++) assertEquals(x, OliviaCodec.binary(OliviaCodec.gray(x)));
        assertEquals(1, Integer.bitCount(OliviaCodec.gray(5) ^ OliviaCodec.gray(6)), "neighbouring symbols differ by one bit");
    }

    @Test void scramblingKeepsAnIdleBlockFromBeingOneSteadyTone() {
        // the old scheme sent a block of spaces as one tone for a whole second; standard Olivia's scrambling spreads even NULs
        int[] tones = OliviaCodec.encodeBlock(new int[]{0, 0, 0});
        assertTrue(java.util.Arrays.stream(tones).distinct().count() >= 6, "tones used: " + java.util.Arrays.toString(tones));
    }

    @Test void aShiftedWindowDoesNotDecodeConfidently() {
        // block sync depends on the wrong alignment looking like noise
        int[] a = OliviaCodec.encodeBlock(new int[]{'C', 'Q', ' '}), b = OliviaCodec.encodeBlock(new int[]{'D', 'E', ' '});
        int[] both = new int[128];
        System.arraycopy(a, 0, both, 0, 64); System.arraycopy(b, 0, both, 64, 64);
        double[][] soft = clean(both, 8);
        for (int f = 0; f < 3; f++) assertTrue(OliviaCodec.decodeChar(soft, 0, f).ratio() > 5, "aligned");
        double aligned = 0, worst = 0;
        for (int f = 0; f < 3; f++) aligned += Math.log(OliviaCodec.decodeChar(soft, 0, f).ratio()) / 3;
        for (int off = 1; off < 64; off++) {
            double m = 0;
            for (int f = 0; f < 3; f++) m += Math.log(OliviaCodec.decodeChar(soft, off, f).ratio()) / 3;
            worst = Math.max(worst, m);
        }
        assertTrue(worst < aligned - 1.0, "mean log-confidence aligned " + aligned + ", best misaligned " + worst);
    }

    @Test void survivesWrongSymbols() {
        int[] chars = {'K', '3', 'x'};
        int[] tones = OliviaCodec.encodeBlock(chars);
        java.util.Random r = new java.util.Random(7);
        for (int i = 0; i < 12; i++) tones[r.nextInt(64)] = r.nextInt(8);          // about a fifth of the block hit
        double[][] soft = clean(tones, 8);
        for (int f = 0; f < 3; f++) assertEquals(chars[f], OliviaCodec.decodeChar(soft, 0, f).ch());
    }
}
