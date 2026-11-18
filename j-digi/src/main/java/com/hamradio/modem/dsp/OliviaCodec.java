package com.hamradio.modem.dsp;

/**
 * Standard Olivia block coding (Pawel Jalocha, SP9VRC), as fldigi and other Olivia programs use it (punch list 3-12).
 *
 * <p>A block is 64 symbols and carries one 7-bit ASCII character per bit of the symbol (3 characters for 8 tones). Each character
 * becomes a 64-chip Walsh function: chip vector +-1 at index c for c &lt; 64, or -1 at index c - 64, run through an inverse fast
 * Hadamard transform. The chips are scrambled with the 64-bit code {@link #SCRAMBLE}, rotated 13 bits further for each character
 * of the block, and spread diagonally: chip t of character f is bit (f + t) mod bitsPerSymbol of symbol t (a set bit where the
 * chip is negative). Each symbol is sent as tone Gray(symbol).
 *
 * <p>The receiver works backwards from soft bits: for each symbol, each bit's value is the tone energy of tones whose
 * (Gray-decoded) symbol has the bit clear minus that of tones where it is set, over the total; chip t of character f is the soft
 * bit (f + t) mod bitsPerSymbol of symbol t, descrambled; a fast Hadamard transform then peaks at the character (a negative peak
 * adds 64).
 *
 * <p>Written from the published description and fldigi's reference behaviour; no code was copied. Pure, so it can be tested.
 */
public final class OliviaCodec {
    private OliviaCodec() {}

    public static final int BLOCK = 64;
    /** The scrambling code for Olivia (Contestia uses a different one). */
    public static final long SCRAMBLE = 0xE257E6D0291574ECL;
    /** How far the scrambling code is rotated for each character of a block. */
    public static final int SHIFT = 13;

    public static int gray(int x) { return x ^ (x >>> 1); }
    public static int binary(int g) { int x = g; for (int s = g >>> 1; s != 0; s >>>= 1) x ^= s; return x; }

    private static boolean codeBit(int i) { return ((SCRAMBLE >>> (i & (BLOCK - 1))) & 1L) != 0; }

    /**
     * One block: {@code chars.length} (= bits per symbol) characters, each 0-127, into 64 tone indices (Gray-coded, 0 is the lowest
     * tone).
     */
    public static int[] encodeBlock(int[] chars) {
        int bps = chars.length;
        int[] sym = new int[BLOCK];
        int[] w = new int[BLOCK];
        for (int f = 0; f < bps; f++) {
            java.util.Arrays.fill(w, 0);
            int c = chars[f] & 0x7F;
            if (c < BLOCK) w[c] = 1; else w[c - BLOCK] = -1;
            inverseHadamard(w);
            for (int t = 0; t < BLOCK; t++) {
                int chip = codeBit(f * SHIFT + t) ? -w[t] : w[t];
                if (chip < 0) sym[t] |= 1 << ((f + t) % bps);
            }
        }
        for (int t = 0; t < BLOCK; t++) sym[t] = gray(sym[t]);
        return sym;
    }

    /** The soft bits of one symbol from its tone powers (lowest tone first): positive for a clear bit, negative for a set one. */
    public static double[] softBits(double[] tonePower, int bps) {
        double[] s = new double[bps];
        double total = 0;
        for (int tone = 0; tone < tonePower.length; tone++) {
            int v = binary(tone);
            double e = tonePower[tone];
            total += e;
            for (int b = 0; b < bps; b++) s[b] += ((v >>> b) & 1) != 0 ? -e : e;
        }
        if (total > 0) for (int b = 0; b < bps; b++) s[b] /= total;
        return s;
    }

    /** A decoded character with how sure it is: the peak over the runner-up of the Hadamard spectrum. */
    public record Decoded(int ch, double ratio, double peak, double noise) {}

    /** Decode character {@code f} of the block whose 64 symbols' soft bits start at {@code soft[start]}. */
    public static Decoded decodeChar(double[][] soft, int start, int f) {
        int bps = soft[start].length;
        double[] v = new double[BLOCK];
        for (int t = 0; t < BLOCK; t++) {
            double bit = soft[start + t][(f + t) % bps];
            v[t] = codeBit(f * SHIFT + t) ? -bit : bit;
        }
        hadamard(v);
        int pos = 0;
        double peak = 0, second = 0, sq = 0;
        for (int t = 0; t < BLOCK; t++) {
            double a = Math.abs(v[t]);
            sq += v[t] * v[t];
            if (a > Math.abs(peak)) { second = Math.abs(peak); peak = v[t]; pos = t; }
            else if (a > second) second = a;
        }
        int ch = peak < 0 ? pos + BLOCK : pos;
        double noise = (sq - peak * peak) / (BLOCK - 1);
        return new Decoded(ch, second > 1e-12 ? Math.abs(peak) / second : 10.0, Math.abs(peak), noise);
    }

    /** Fast Hadamard transform, in the reference's sign convention (pairs become b + a and b - a). */
    static void hadamard(double[] d) {
        int n = d.length;
        for (int step = 1; step < n; step <<= 1)
            for (int p = 0; p < n; p += 2 * step)
                for (int q = p; q < p + step; q++) {
                    double a = d[q], b = d[q + step];
                    d[q] = b + a; d[q + step] = b - a;
                }
    }

    /** Its inverse, up to scale (pairs become a - b and a + b, largest step first). */
    static void inverseHadamard(int[] d) {
        int n = d.length;
        for (int step = n / 2; step > 0; step >>= 1)
            for (int p = 0; p < n; p += 2 * step)
                for (int q = p; q < p + step; q++) {
                    int a = d[q], b = d[q + step];
                    d[q] = a - b; d[q + step] = a + b;
                }
    }
}
