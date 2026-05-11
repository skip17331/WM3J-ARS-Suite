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
    private static final double AFC_WIDE_JUMP_HZ    = 500.0;  // cold-start / post-reset
    private static final double AFC_LOCKED_JUMP_HZ  = 60.0;   // once carrier locked

    // ── Adaptive-state reset ─────────────────────────────────────────
    /** After this much continuous silence, treat the next carrier as a
     *  fresh transmission: snap speed back to default so a different
     *  operator/WPM doesn't inherit the previous QSO's adaptive state. */
    private static final int    QUIET_RESET_SAMPLES = 8000;   // 1 second at 8 kHz

    // ── Envelope follower ─────────────────────────────────────────────
    private static final double ENV_ATTACK          = 0.30;   // fast attack  (per sample)
    private static final double ENV_DECAY           = 0.020;  // ~6 ms — must clear within an inter-element gap (60 ms @ 20 WPM)
    /** Adaptive peak tracker — very slow decay keeps threshold stable across QSB. */
    private static final double PEAK_DECAY          = 0.0002; // per sample
    /** How fast peakEnv tracks env upward. Must be slow enough that peakEnv
     *  doesn't latch onto a bandpass transient at mark onset (which would
     *  push OFF threshold above env and cause a phantom mid-mark split),
     *  but fast enough that peakEnv settles within a single dit. */
    private static final double PEAK_ATTACK         = 0.05;   // per sample (~60-sample settle)

    // ── Slicer thresholds (fraction of adaptive peak) ─────────────────
    // ON/OFF gap needs to be wide enough that mid-mark envelope wobble
    // doesn't trip a phantom falling edge. 0.55 / 0.30 gives a 0.25
    // hysteresis margin — empirically clean through the test suite.
    private static final double ON_FRAC             = 0.55;
    private static final double OFF_FRAC            = 0.30;
    private static final double MIN_ON_THRESHOLD    = 0.025;  // absolute floor
    private static final double MIN_OFF_THRESHOLD   = 0.015;

    // ── Speed adaptation ──────────────────────────────────────────────
    private static final double SPEED_ALPHA         = 0.15;   // EMA coefficient
    private static final double MIN_WPM             = 5.0;
    private static final double MAX_WPM             = 55.0;

    // ── Element classification (multiples of estimated dot duration) ──
    // 2.0 is the natural geometric mean of 1× dit and 3× dah, so initial
    // classification works for any speed within ~½× to 2× of the current
    // dotSamples estimate (15–40 WPM from the 20 WPM default).
    private static final double DOT_DASH_RATIO      = 2.0;   // mark ≥ this → dah
    private static final double CHAR_SPACE_RATIO    = 2.0;   // space ≥ this → inter-char (geom mean of 1× and 3×)
    private static final double WORD_SPACE_RATIO    = 5.0;   // space ≥ this → inter-word

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

    /** Set after a quiet-period reset has fired; cleared by the next
     *  rising edge so the reset can fire again on the next quiet period. */
    private boolean adaptiveReset = false;

    /** Current AFC max-jump window. Starts wide so a new QSO at a different
     *  audio offset is captured, narrows once a carrier locks. */
    private double afcMaxJumpHz = AFC_WIDE_JUMP_HZ;

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
            // Carrier-quiet frame: synthesize a falling edge if we were
            // keyed, then advance the run-length counter by the frame
            // size so word-space timeouts can fire while audio is silent.
            if (keyed) {
                onTransition(true, runSamples);
                keyed = false;
                prevKeyed = false;
                runSamples = 0;
            }
            // Zero the envelope and biquad state so a stale (high) env
            // from before the silence doesn't trip the slicer the instant
            // audio resumes — that was producing a phantom 35-sample dit
            // at the start of each post-silence mark.
            env = 0.0;
            bpX1 = bpX2 = bpY1 = bpY2 = 0.0;
            // Decay peakEnv each silent frame too, so by the time audio
            // resumes the OFF threshold is low and the slicer stays keyed
            // through the bandpass filter's settling transient.
            peakEnv *= 0.85;

            runSamples += snap.getSamples().length;
            checkTimeouts();
            // After a long quiet stretch, treat the next signal as a fresh
            // transmission — reset speed adaptation and partial state so a
            // different operator/WPM doesn't inherit the previous QSO.
            // Only fire once per quiet period; rearm on the next rising edge.
            if (!adaptiveReset && runSamples > QUIET_RESET_SAMPLES) {
                resetAdaptiveState();
                adaptiveReset = true;
            }
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

    /** Reset only the adaptive parts: speed estimate, partial element,
     *  envelope tracker. Keep the carrier lock and any pending output. */
    private void resetAdaptiveState() {
        dotSamples = sampleRate * PARIS_MS_PER_WPM / (1000.0 * DEFAULT_WPM);
        element.setLength(0);
        env     = 0.0;
        peakEnv = 0.0;
        afcMaxJumpHz = AFC_WIDE_JUMP_HZ;   // re-widen AFC for a possible new QSO
        debug(String.format("RESET adaptive state — dotSamp=%.1f (back to %.0f WPM default)",
                dotSamples, DEFAULT_WPM));
    }

    // =================================================================
    // AFC — snap carrier to FFT peak
    // =================================================================

    private void afcUpdate(double peakHz) {
        if (peakHz < AFC_MIN_HZ || peakHz > AFC_MAX_HZ) return;
        double jump = Math.abs(peakHz - carrierHz);
        // Deadband must exceed one FFT bin (~7.8 Hz at 8 kHz / 1024) so
        // we don't chase bin-to-bin jitter between adjacent buckets.
        if (jump < 25.0 || jump > afcMaxJumpHz) return;
        debug(String.format("AFC %.1f → %.1f Hz", carrierHz, peakHz));
        carrierHz = peakHz;
        buildBandpass(peakHz);
        // Narrow the window once we've locked, so spurious FFT bins during
        // intra-element gaps can't drag us away.
        afcMaxJumpHz = AFC_LOCKED_JUMP_HZ;
        // Note: do NOT reset envelope here. Zeroing env mid-mark creates
        // a phantom falling edge → false dits.
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
        // Starting a fresh unkeyed run — clear the word-space latch and
        // rearm the quiet-period reset for the next silence after this run.
        wordSpaceEmitted = false;
        adaptiveReset = false;

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
        if (pending.length() == 0) return Optional.empty();

        // Defer if the buffer is still only whitespace — a word-space
        // was queued but no character has arrived yet. Don't strip it,
        // it has to live in the buffer until a real char follows.
        int nonSpaceLen = pending.length();
        while (nonSpaceLen > 0 && pending.charAt(nonSpaceLen - 1) == ' ') nonSpaceLen--;
        if (nonSpaceLen == 0) return Optional.empty();

        // Emit on word boundary (current run ends in a space, i.e. a word
        // just finished) or when we've buffered at least MIN_EMIT_CHARS.
        boolean endsWithSpace = pending.charAt(pending.length() - 1) == ' ';
        if (!endsWithSpace && pending.length() < MIN_EMIT_CHARS) return Optional.empty();

        // Strip any leading word-space — it would render as a leading
        // blank in the RX pane.
        int start = 0;
        while (start < pending.length() && pending.charAt(start) == ' ') start++;
        String text = pending.substring(start);

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
