package com.hamradio.modem.tx;

import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.mode.CwMorse;

import java.util.ArrayList;
import java.util.List;

/**
 * CW (Morse code) transmitter.
 *
 * Converts plain text to CW audio using the ITU PARIS timing formula.
 * Raised-cosine keying envelopes are applied to each element to eliminate
 * key clicks.
 *
 * Timing (ITU PARIS):
 *   1 dit  = 1200 ms / WPM
 *   1 dah  = 3 dits
 *   inter-element space    = 1 dit
 *   inter-character space  = 3 dits
 *   inter-word space       = 7 dits
 *
 * Key click suppression:
 *   Each keyed element has a raised-cosine rise and fall of RAMP_MS.
 *   If an element is shorter than 2 × RAMP_MS, the ramp is shortened to
 *   half the element duration so it always has at least a brief plateau.
 */
public class CwTransmitter implements DigitalTransmitter {

    private static final double DEFAULT_WPM       = 20.0;
    private static final double DEFAULT_CARRIER   = 700.0;
    private static final double DEFAULT_AMPLITUDE = 0.50;
    private static final double RAMP_MS           = 5.0;   // key-click suppression ramp time

    private final double sampleRate;
    private final double wpm;
    private final double carrierHz;
    private final double amplitude;

    public CwTransmitter() {
        this(AudioEngine.SAMPLE_RATE, DEFAULT_WPM, DEFAULT_CARRIER, DEFAULT_AMPLITUDE);
    }

    public CwTransmitter(double sampleRate, double wpm, double carrierHz, double amplitude) {
        this.sampleRate = sampleRate;
        this.wpm        = Math.max(1.0, wpm);
        this.carrierHz  = carrierHz;
        this.amplitude  = amplitude;
    }

    @Override
    public String getName() {
        return "CW";
    }

    @Override
    public SampleSource createSampleSource(String text) {
        return new CwSampleSource(sampleRate, wpm, carrierHz, amplitude, text);
    }

    // =================================================================
    // SampleSource implementation
    // =================================================================

    private static final class CwSampleSource implements SampleSource {

        private final double sampleRate;
        private final double carrierHz;
        private final double amplitude;
        private final double phaseStep;
        private final int    rampSamples;

        /** List of (keyed, durationSamples) segments. */
        private final List<long[]> segments;  // [0]=keyed(1/0), [1]=durationSamples

        private boolean finished = false;
        private boolean closed   = false;

        private int    segIdx       = 0;
        private int    sampleInSeg  = 0;
        private double phase        = 0.0;

        private CwSampleSource(double sampleRate,
                               double wpm,
                               double carrierHz,
                               double amplitude,
                               String text) {
            this.sampleRate  = sampleRate;
            this.carrierHz   = carrierHz;
            this.amplitude   = amplitude;
            this.phaseStep   = (2.0 * Math.PI * carrierHz) / sampleRate;
            // Ramp in samples; minimum 1
            this.rampSamples = (int) Math.max(1.0, sampleRate * RAMP_MS / 1000.0);

            double ditSamples = sampleRate * 1200.0 / (1000.0 * wpm);
            this.segments = buildSegments(text, ditSamples);
        }

        // ── Segment builder ──────────────────────────────────────────

        /**
         * Build the full list of keyed/unkeyed segments for the text.
         *
         * <p>The builder works element by element, accumulating:
         * <ul>
         *   <li>A keyed segment for each dit/dah</li>
         *   <li>An unkeyed inter-element gap (1 dit) between elements</li>
         *   <li>An unkeyed inter-character gap after the last element of each char
         *       (the 1-dit gap already present is extended to 3 dits by adding 2 more)</li>
         *   <li>A further extension to 7 dits for word spaces (add 4 more dits)</li>
         * </ul>
         * Consecutive unkeyed segments are merged to keep the list compact.
         */
        private static List<long[]> buildSegments(String text, double ditSamples) {
            List<long[]> raw = new ArrayList<>();

            // Lead-in silence
            raw.add(seg(false, Math.round(2 * ditSamples)));

            String normalized = text == null ? "" : text.trim();

            for (int ci = 0; ci < normalized.length(); ci++) {
                char c = normalized.charAt(ci);

                if (c == ' ') {
                    // Word space: extend trailing char-gap by 4 more dits
                    raw.add(seg(false, Math.round(4 * ditSamples)));
                    continue;
                }

                String code = CwMorse.encode(c);
                if (code == null || code.isEmpty()) continue;

                for (int ei = 0; ei < code.length(); ei++) {
                    boolean isDit = (code.charAt(ei) == '.');
                    long elemSamples = Math.round(isDit ? ditSamples : 3.0 * ditSamples);
                    raw.add(seg(true, elemSamples));

                    if (ei < code.length() - 1) {
                        // Inter-element gap
                        raw.add(seg(false, Math.round(ditSamples)));
                    }
                }

                // Inter-character gap: 2 more dits (the inter-element gap already gave 1)
                raw.add(seg(false, Math.round(2 * ditSamples)));
            }

            // Lead-out silence
            raw.add(seg(false, Math.round(4 * ditSamples)));

            // Merge adjacent unkeyed segments
            List<long[]> merged = new ArrayList<>();
            for (long[] s : raw) {
                if (!merged.isEmpty()) {
                    long[] prev = merged.get(merged.size() - 1);
                    if (prev[0] == s[0]) {
                        prev[1] += s[1];
                        continue;
                    }
                }
                merged.add(new long[]{ s[0], s[1] });
            }

            return merged;
        }

        private static long[] seg(boolean keyed, long durationSamples) {
            return new long[]{ keyed ? 1L : 0L, durationSamples };
        }

        // ── SampleSource contract ────────────────────────────────────

        @Override
        public int read(float[] buffer, int offset, int length) {
            if (closed || finished || buffer == null || length <= 0) return -1;

            int written = 0;

            while (written < length) {
                if (segIdx >= segments.size()) {
                    finished = true;
                    break;
                }

                long[] seg  = segments.get(segIdx);
                boolean on  = (seg[0] == 1L);
                long    dur = seg[1];

                float sample;
                if (on) {
                    // Keyed: sine wave with raised-cosine envelope
                    int effectiveRamp = (int) Math.min(rampSamples, dur / 2);
                    double env = keyingEnvelope(sampleInSeg, (int) dur, effectiveRamp);
                    sample = (float) (Math.sin(phase) * amplitude * env);
                    phase += phaseStep;
                    if (phase >= 2.0 * Math.PI) phase -= 2.0 * Math.PI;
                } else {
                    // Unkeyed: silence
                    sample = 0.0f;
                }

                buffer[offset + written] = sample;
                written++;
                sampleInSeg++;

                if (sampleInSeg >= dur) {
                    sampleInSeg = 0;
                    segIdx++;
                }
            }

            return written > 0 ? written : -1;
        }

        /**
         * Raised-cosine keying envelope.
         *
         * <pre>
         *   Rise:  0 → 1 over samples [0, ramp)
         *   Hold:  1    over samples [ramp, total−ramp)
         *   Fall:  1 → 0 over samples [total−ramp, total)
         * </pre>
         */
        private static double keyingEnvelope(int pos, int total, int ramp) {
            if (ramp <= 0) return 1.0;

            if (pos < ramp) {
                double u = (double) pos / ramp;
                return 0.5 * (1.0 - Math.cos(Math.PI * u));
            }

            int fallStart = total - ramp;
            if (pos >= fallStart) {
                double u = (double) (pos - fallStart) / ramp;
                return 0.5 * (1.0 + Math.cos(Math.PI * u));
            }

            return 1.0;
        }

        @Override
        public boolean isFinished() {
            return finished || closed;
        }

        @Override
        public void close() {
            closed   = true;
            finished = true;
        }
    }
}
