package com.hamradio.modem.mode;

import com.hamradio.modem.model.DecodeMessage;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.SignalSnapshot;

import java.util.Optional;

/**
 * CW (Morse code) decoder — self-calibrating, 5–50 WPM.
 *
 * Pipeline (per audio frame):
 *   1. AFC    — snap carrier to FFT peak before lock (300–1500 Hz)
 *   2. BPF    — biquad bandpass (150 Hz BW) around carrier
 *   3. Envelope follower — rectify + one-pole LP, adaptive peak tracker
 *   4. Hysteretic slicer — threshold = fraction of tracked peak
 *   5. Transition detection — measure keyed/unkeyed run lengths in samples
 *   6. Speed adaptation — EMA of observed dot durations (PARIS formula)
 *   7. Morse decode — dots/dashes → CwMorse.decode() → character
 *   8. Emit — flush on word space or ≥ 6 pending characters
 *
 * Timing reference (ITU PARIS formula):
 *   1 dit = 1200 ms / WPM   (at 20 WPM: 60 ms = 480 samples at 8 kHz)
 *   1 dah = 3 dits
 *   inter-element space = 1 dit
 *   inter-character space = 3 dits
 *   inter-word space = 7 dits
 */
public class CwMode implements DigitalMode {

    // ── Default operating parameters ─────────────────────────────────
    private static final double DEFAULT_CARRIER_HZ  = 700.0;
    private static final double DEFAULT_WPM         = 20.0;
    private static final double PARIS_MS_PER_WPM    = 1200.0;  // ITU: 1 dit = 1200/WPM ms
    private static final double BP_BANDWIDTH_HZ     = 150.0;

    // ── AFC ───────────────────────────────────────────────────────────
    private static final double AFC_MIN_HZ          = 300.0;
    private static final double AFC_MAX_HZ          = 1500.0;
    private static final double AFC_MAX_JUMP_HZ     = 150.0;

    // ── Envelope follower ─────────────────────────────────────────────
    private static final double ENV_ATTACK          = 0.30;   // fast attack  (per sample)
    private static final double ENV_DECAY           = 0.003;  // slow decay   (per sample)
    /** Adaptive peak tracker — very slow decay keeps threshold stable across QSB. */
    private static final double PEAK_DECAY          = 0.0002; // per sample

    // ── Slicer thresholds (fraction of adaptive peak) ─────────────────
    private static final double ON_FRAC             = 0.45;
    private static final double OFF_FRAC            = 0.25;
    private static final double MIN_ON_THRESHOLD    = 0.018;  // absolute floor
    private static final double MIN_OFF_THRESHOLD   = 0.010;

    // ── Speed adaptation ──────────────────────────────────────────────
    private static final double SPEED_ALPHA         = 0.15;   // EMA coefficient
    private static final double MIN_WPM             = 5.0;
    private static final double MAX_WPM             = 55.0;

    // ── Element classification (multiples of estimated dot duration) ──
    private static final double DOT_DASH_RATIO      = 2.5;   // mark ≥ this → dah
    private static final double CHAR_SPACE_RATIO    = 2.2;   // space ≥ this → inter-char
    private static final double WORD_SPACE_RATIO    = 5.5;   // space ≥ this → inter-word

    // ── Decoder / output ─────────────────────────────────────────────
    private static final int    MAX_ELEMENT_DITS    = 8;     // sanity cap on dots/dahs per char
    private static final int    MIN_EMIT_CHARS      = 6;     // flush pending when ≥ this many chars
    private static final double MIN_RMS             = 0.002;

    private static final boolean DEBUG              = true;

    // =================================================================
    // Per-instance state
    // =================================================================

    // ── Init / sample rate ────────────────────────────────────────────
    private boolean initialized = false;
    private double  sampleRate  = 8000.0;

    // ── Carrier frequency ─────────────────────────────────────────────
    private double  carrierHz   = DEFAULT_CARRIER_HZ;

    // ── Biquad bandpass filter coefficients and history ───────────────
    private double bpB0, bpB2, bpA1, bpA2;   // bpB1 is always 0 for bandpass
    private double bpX1 = 0, bpX2 = 0, bpY1 = 0, bpY2 = 0;

    // ── Envelope follower ─────────────────────────────────────────────
    private double env     = 0.0;
    private double peakEnv = 0.0;

    // ── Hysteretic slicer ─────────────────────────────────────────────
    private boolean keyed     = false;
    private boolean prevKeyed = false;

    // ── Run-length counter (samples in current keyed/unkeyed run) ─────
    private int  runSamples = 0;

    // ── Adaptive dot duration (samples) ──────────────────────────────
    private double dotSamples;

    // ── Element accumulator ("." and "-" for current character) ───────
    private final StringBuilder element = new StringBuilder();

    /**
     * Set when the current element has been emitted as a character (by
     * timeout or by a rising edge's space-classification).  Reset on
     * each falling edge so new elements are accepted.
     */
    private boolean wordSpaceEmitted = false;

    // ── Text output ───────────────────────────────────────────────────
    private final StringBuilder pending = new StringBuilder();

    // ── Diagnostics ───────────────────────────────────────────────────
    private double lastSnr  = 0.0;
    private double lastConf = 0.0;

    // =================================================================
    // DigitalMode contract
    // =================================================================

    @Override
    public String getName() {
        return "CW";
    }

    @Override
    public Optional<DecodeMessage> process(SignalSnapshot snap, long rigHz) {
        if (snap == null || snap.getSamples() == null || snap.getSamples().length == 0)
            return Optional.empty();

        initIfNeeded(snap.getSampleRate());

        if (snap.getRms() < MIN_RMS) {
            // Signal gone — flush any pending character
            if (element.length() > 0) emitCharacter();
            return emitPending(rigHz);
        }

        // AFC: only update when signal is unlocked (or very weak lock)
        afcUpdate(snap.getPeakFrequencyHz());

        // Per-sample decode pipeline
        processSamples(snap.getSamples());

        // Timeout: emit pending char / word space after long silence
        checkTimeouts();

        return emitPending(rigHz);
    }

    // =================================================================
    // Initialisation
    // =================================================================

    private void initIfNeeded(double sr) {
        if (initialized && Math.abs(sr - sampleRate) < 0.5) return;
        sampleRate = sr;
        dotSamples = sr * PARIS_MS_PER_WPM / (1000.0 * DEFAULT_WPM);
        buildBandpass(DEFAULT_CARRIER_HZ);
        resetState();
        initialized = true;
        debug(String.format("init sr=%.0f dotSamples=%.1f (%.0f WPM)",
                sr, dotSamples, wpm()));
    }

    private void buildBandpass(double centerHz) {
        double q     = Math.max(0.5, centerHz / BP_BANDWIDTH_HZ);
        double w0    = 2.0 * Math.PI * centerHz / sampleRate;
        double alpha = Math.sin(w0) / (2.0 * q);
        double cosW0 = Math.cos(w0);
        double a0    = 1.0 + alpha;

        bpB0 =  alpha  / a0;
        // bpB1 = 0 always for a BPF
        bpB2 = -alpha  / a0;
        bpA1 = (-2.0 * cosW0) / a0;
        bpA2 = (1.0 - alpha)  / a0;

        bpX1 = bpX2 = bpY1 = bpY2 = 0.0;
    }

    private void resetState() {
        env      = 0.0;
        peakEnv  = 0.0;
        keyed    = false;
        prevKeyed= false;
        runSamples = 0;
        element.setLength(0);
        pending.setLength(0);
        wordSpaceEmitted = false;
        lastSnr  = 0.0;
        lastConf = 0.0;
    }

    // =================================================================
    // AFC — snap carrier to FFT peak
    // =================================================================

    private void afcUpdate(double peakHz) {
        if (peakHz < AFC_MIN_HZ || peakHz > AFC_MAX_HZ) return;
        double jump = Math.abs(peakHz - carrierHz);
        if (jump < 5.0 || jump > AFC_MAX_JUMP_HZ) return;
        debug(String.format("AFC %.1f → %.1f Hz", carrierHz, peakHz));
        carrierHz = peakHz;
        buildBandpass(peakHz);
        // Reset envelope/slicer so the new frequency settles cleanly
        env = 0.0;  bpX1 = bpX2 = bpY1 = bpY2 = 0.0;
    }

    // =================================================================
    // Per-sample envelope and slicer
    // =================================================================

    private void processSamples(float[] samples) {
        for (float s : samples) {

            // ── 1. Bandpass filter ────────────────────────────────────
            double x = s;
            double y = bpB0*x + bpB2*bpX2 - bpA1*bpY1 - bpA2*bpY2;
            bpX2 = bpX1;  bpX1 = x;
            bpY2 = bpY1;  bpY1 = y;

            // ── 2. Envelope follower (abs + asymmetric one-pole) ──────
            double absY = Math.abs(y);
            if (absY > env) env += ENV_ATTACK * (absY - env);
            else            env += ENV_DECAY  * (absY - env);

            // ── 3. Adaptive peak tracker ─────────────────────────────
            if (env > peakEnv) peakEnv = env;
            else               peakEnv *= (1.0 - PEAK_DECAY);

            // ── 4. Hysteretic slicer ─────────────────────────────────
            double onThr  = Math.max(MIN_ON_THRESHOLD,  peakEnv * ON_FRAC);
            double offThr = Math.max(MIN_OFF_THRESHOLD, peakEnv * OFF_FRAC);

            prevKeyed = keyed;
            if (!keyed && env >= onThr)  keyed = true;
            if ( keyed && env <  offThr) keyed = false;

            // ── 5. Transition detection ───────────────────────────────
            if (keyed != prevKeyed) {
                onTransition(prevKeyed, runSamples);
                runSamples = 0;
            } else {
                runSamples++;
            }
        }

        // Update diagnostics from envelope level
        if (peakEnv > 1e-9) {
            lastSnr  = 20.0 * Math.log10(peakEnv / Math.max(1e-9, MIN_ON_THRESHOLD));
            lastConf = Math.min(0.99, peakEnv / (peakEnv + 0.08));
        }
    }

    // =================================================================
    // Transition handler
    // =================================================================

    /**
     * Called on every keyed↔unkeyed edge.
     *
     * @param wasKeyed  the state that just ended
     * @param runLen    how many samples were spent in that state
     */
    private void onTransition(boolean wasKeyed, int runLen) {
        if (wasKeyed) {
            onMarkEnd(runLen);   // falling edge — classify the mark
        } else {
            onSpaceEnd(runLen);  // rising edge — classify the space
        }
    }

    /** Falling edge: classify mark duration as dit or dah and append to element. */
    private void onMarkEnd(int markSamples) {
        // Starting a fresh unkeyed run — clear the word-space latch
        wordSpaceEmitted = false;

        if (element.length() >= MAX_ELEMENT_DITS) {
            // Element string too long — almost certainly noise; discard
            debug("NOISE: element too long, resetting");
            element.setLength(0);
            return;
        }

        char sym;
        if (markSamples < DOT_DASH_RATIO * dotSamples) {
            sym = '.';
            // Speed tracking: update EMA from dot durations only
            dotSamples = (1.0 - SPEED_ALPHA) * dotSamples + SPEED_ALPHA * markSamples;
            // Clamp to valid WPM range
            double maxDot = sampleRate * PARIS_MS_PER_WPM / (1000.0 * MIN_WPM);
            double minDot = sampleRate * PARIS_MS_PER_WPM / (1000.0 * MAX_WPM);
            dotSamples = Math.max(minDot, Math.min(maxDot, dotSamples));
        } else {
            sym = '-';
        }

        element.append(sym);
        debug(String.format("MARK %d samp → '%c'  dotSamp=%.1f  WPM=%.1f",
                markSamples, sym, dotSamples, wpm()));
    }

    /** Rising edge: classify the preceding space and decide what to do. */
    private void onSpaceEnd(int spaceSamples) {
        if (spaceSamples < CHAR_SPACE_RATIO * dotSamples) {
            // ── Inter-element gap: keep accumulating ──────────────────
            debug(String.format("GAP %d samp (inter-element)", spaceSamples));

        } else if (spaceSamples < WORD_SPACE_RATIO * dotSamples) {
            // ── Inter-character space ─────────────────────────────────
            if (element.length() > 0) emitCharacter();
            debug(String.format("CHAR_SPACE %d samp", spaceSamples));

        } else {
            // ── Inter-word space ──────────────────────────────────────
            if (element.length() > 0) emitCharacter();
            if (!wordSpaceEmitted) {
                pending.append(' ');
                wordSpaceEmitted = true;
                debug(String.format("WORD_SPACE %d samp", spaceSamples));
            }
        }
    }

    // =================================================================
    // Timeout check — called once per frame after processSamples()
    // =================================================================

    /**
     * Emit pending character and/or word space if the unkeyed run has
     * exceeded the relevant threshold.  Handles end-of-transmission
     * where no further rising edge will arrive.
     */
    private void checkTimeouts() {
        if (keyed) return;

        // Char-space timeout: we have elements but no rising edge yet
        if (element.length() > 0 && runSamples > CHAR_SPACE_RATIO * dotSamples) {
            emitCharacter();
        }

        // Word-space timeout: char already emitted, still quiet
        if (element.length() == 0 && !wordSpaceEmitted
                && runSamples > WORD_SPACE_RATIO * dotSamples) {
            pending.append(' ');
            wordSpaceEmitted = true;
            debug("WORD_SPACE (timeout)");
        }
    }

    // =================================================================
    // Morse decode
    // =================================================================

    private void emitCharacter() {
        String code = element.toString();
        element.setLength(0);
        if (code.isEmpty()) return;

        Character ch = CwMorse.decode(code);
        if (ch != null) {
            debug("CHAR '" + ch + "' ← " + code);
            pending.append(ch);
        } else {
            debug("UNKNOWN ← " + code);
            pending.append('?');
        }
    }

    // =================================================================
    // Text emission
    // =================================================================

    private Optional<DecodeMessage> emitPending(long rigHz) {
        // Trim trailing spaces before emitting
        while (pending.length() > 0 && pending.charAt(pending.length() - 1) == ' ')
            pending.deleteCharAt(pending.length() - 1);

        if (pending.length() == 0) return Optional.empty();

        // Emit on word boundary (contains space) or when buffer is large enough
        boolean hasSpace = pending.indexOf(" ") >= 0;
        if (!hasSpace && pending.length() < MIN_EMIT_CHARS) return Optional.empty();

        String text = pending.toString();
        pending.setLength(0);
        debug("EMIT '" + text + "'");

        return Optional.of(new DecodeMessage(
                ModeType.CW, text, rigHz, carrierHz, lastSnr, lastConf));
    }

    // =================================================================
    // Helpers
    // =================================================================

    private double wpm() {
        return (dotSamples > 0) ? sampleRate * PARIS_MS_PER_WPM / (1000.0 * dotSamples) : 0.0;
    }

    private static void debug(String msg) {
        if (DEBUG) System.out.println("[CW] " + msg);
    }
}
