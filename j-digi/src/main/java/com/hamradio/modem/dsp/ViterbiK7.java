package com.hamradio.modem.dsp;

import java.util.Arrays;

/**
 * Streaming K=7, rate-1/2 convolutional Viterbi decoder.
 *
 * Generator polynomials (standard K=7, also used by ViterbiK7Encoder):
 *   G0 = 0b1011011 = 91  (bits 6,4,3,1,0 tapped)
 *   G1 = 0b1111001 = 121 (bits 6,5,4,3,0 tapped)
 *
 * State = K−1 = 6 bits: state[5] = most-recently-seen past input,
 *   state[0] = oldest stored input.
 *
 * For input bit u from state s:
 *   full_sr  = (u << 6) | s
 *   out0     = popcount(full_sr & G0) mod 2
 *   out1     = popcount(full_sr & G1) mod 2
 *   newState = full_sr >> 1   (= (u << 5) | (s >> 1))
 *
 * Decoding algorithm: path register exchange (PRE) method.
 *   Each state s maintains a DEPTH-bit path register pathReg[s].
 *   On each symbol the register is updated:
 *     pathReg[ns] = ((pathReg[prev] << 1) | u) & PATH_MASK
 *   The decoded output bit at each step is the MSB (bit DEPTH−1) of the
 *   best-metric state's register.  The output lags the input by exactly
 *   DEPTH symbols.
 *
 * Usage:
 *   ViterbiK7 v = new ViterbiK7();
 *   for each pair of received bits (r0, r1):
 *       int decoded = v.feedSymbol(r0, r1);
 *       if (decoded >= 0) processDecodedBit(decoded);
 *   // Flush: after all data, feed DEPTH pairs of (0,0)
 *   //        to drain the remaining DEPTH bits.
 */
public final class ViterbiK7 {

    // ── Code parameters (public so encoder can reference them) ────────
    public  static final int K      = 7;
    public  static final int G0     = 0b1011011;   // 91
    public  static final int G1     = 0b1111001;   // 121
    private static final int STATES = 1 << (K - 1); // 64
    private static final int DEPTH  = 5 * K;        // 35 — traceback depth

    private static final long PATH_MASK = (1L << DEPTH) - 1L;

    // ── Precomputed trellis tables ─────────────────────────────────────
    // nextState[s][u] and output[s][u] — indexed by state × input
    private static final int[] NEXT = new int[STATES * 2]; // [s*2+u]
    private static final int[] OUT  = new int[STATES * 2]; // 2-bit output

    static {
        for (int s = 0; s < STATES; s++) {
            for (int u = 0; u < 2; u++) {
                int sr = (u << (K - 1)) | s;
                NEXT[s * 2 + u] = sr >>> 1;
                int o0 = Integer.bitCount(sr & G0) & 1;
                int o1 = Integer.bitCount(sr & G1) & 1;
                OUT [s * 2 + u] = (o0 << 1) | o1;
            }
        }
    }

    // ── Decoder state ─────────────────────────────────────────────────
    private int[]  metric  = new int[STATES];
    private long[] pathReg = new long[STATES];
    private int    filled  = 0;

    public ViterbiK7() {
        reset();
    }

    public void reset() {
        Arrays.fill(metric, Integer.MAX_VALUE / 2);
        metric[0] = 0;
        Arrays.fill(pathReg, 0L);
        filled = 0;
    }

    /**
     * Feed one coded symbol (the two output bits produced by the rate-1/2
     * encoder for a single data bit).
     *
     * @param r0 first received bit (0 or 1)
     * @param r1 second received bit (0 or 1)
     * @return the decoded data bit (0 or 1) once {@code DEPTH} symbols have
     *         been fed, or −1 during the initial fill phase.
     */
    public int feedSymbol(int r0, int r1) {
        int[]  newMetric  = new int[STATES];
        long[] newPathReg = new long[STATES];
        Arrays.fill(newMetric, Integer.MAX_VALUE / 2);

        for (int s = 0; s < STATES; s++) {
            if (metric[s] == Integer.MAX_VALUE / 2) continue;
            for (int u = 0; u < 2; u++) {
                int  ns   = NEXT[s * 2 + u];
                int  exp  = OUT [s * 2 + u];
                int  dist = ((exp >> 1) ^ r0) + ((exp & 1) ^ r1);
                int  nm   = metric[s] + dist;
                if (nm < newMetric[ns]) {
                    newMetric [ns] = nm;
                    newPathReg[ns] = ((pathReg[s] << 1) | u) & PATH_MASK;
                }
            }
        }

        metric  = newMetric;
        pathReg = newPathReg;
        filled++;

        // Periodic normalization — prevents metric integer overflow
        if ((filled & 0xFF) == 0) normalizeMetrics();

        if (filled < DEPTH) return -1;

        // Identify best state and output its oldest decoded bit
        int best = 0;
        for (int s = 1; s < STATES; s++) {
            if (metric[s] < metric[best]) best = s;
        }
        return (int)((pathReg[best] >>> (DEPTH - 1)) & 1L);
    }

    private void normalizeMetrics() {
        int min = metric[0];
        for (int s = 1; s < STATES; s++) if (metric[s] < min) min = metric[s];
        if (min > 0) {
            for (int s = 0; s < STATES; s++) {
                if (metric[s] < Integer.MAX_VALUE / 2) metric[s] -= min;
            }
        }
    }
}
