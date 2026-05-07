package com.morsetrainer.decoder;

import com.morsetrainer.core.AppConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Streaming Morse decoder. Consumes KeyEvents, emits decoded characters and timing
 * elements. Fully deterministic — given the same event stream it produces identical
 * output, which makes the unit tests reliable.
 *
 * The decoder tracks an estimated dit length. If {@code adaptive} is enabled, the
 * estimate is continuously updated from observed marks so a slowly drifting sender
 * is still decoded correctly. Otherwise a fixed value (derived from configured WPM)
 * is used, which matches a strict rhythmic test.
 */
public class TimingDecoder {

    private final AppConfig cfg;
    private final boolean adaptive;
    private double estimatedDitMs;

    private long lastDownTime = -1;
    private long lastUpTime   = -1;
    private boolean keyDown   = false;
    private final StringBuilder currentPattern = new StringBuilder();
    private final List<DecodedElement> sessionElements = new ArrayList<>();

    private Consumer<Character> charListener;
    private Consumer<DecodedElement> elementListener;
    private Runnable wordBoundaryListener;

    public TimingDecoder() { this(AppConfig.get()); }

    public TimingDecoder(AppConfig cfg) {
        this.cfg = cfg;
        this.adaptive = cfg.adaptiveDecoder;
        this.estimatedDitMs = AppConfig.ditMillis(cfg.wpm);
    }

    public void onCharacter(Consumer<Character> l)        { this.charListener = l; }
    public void onElement(Consumer<DecodedElement> l)     { this.elementListener = l; }
    public void onWordBoundary(Runnable r)                { this.wordBoundaryListener = r; }

    /** Reset all state — useful between training sessions. */
    public void reset() {
        lastDownTime = -1;
        lastUpTime   = -1;
        keyDown      = false;
        currentPattern.setLength(0);
        sessionElements.clear();
        estimatedDitMs = AppConfig.ditMillis(cfg.wpm);
    }

    public List<DecodedElement> sessionElements() { return List.copyOf(sessionElements); }
    public double estimatedDitMillis() { return estimatedDitMs; }

    /** Force the dit estimate (for predictable behaviour in tests / fixed-rate mode). */
    public void setEstimatedDitMs(double v) { this.estimatedDitMs = v; }

    /**
     * Feed a single key event. May emit a character if a character-gap is detected.
     * Long gaps trigger a word boundary callback.
     */
    public void accept(KeyEvent ev) {
        switch (ev.type()) {
            case DOWN -> handleDown(ev.timestampMillis());
            case UP   -> handleUp(ev.timestampMillis());
        }
    }

    /**
     * Call regularly (e.g. every 50 ms) so that long pauses with no further events
     * still flush a pending character / emit a word boundary.
     */
    public void tick(long nowMillis) {
        if (keyDown) return;
        if (lastUpTime < 0) return;
        long gap = nowMillis - lastUpTime;
        double charThreshold = estimatedDitMs * cfg.charGapThresholdRatio;
        double wordThreshold = estimatedDitMs * cfg.wordGapThresholdRatio;

        if (currentPattern.length() > 0 && gap > charThreshold) {
            flushCharacter(lastUpTime);
        }
        if (gap > wordThreshold && wordBoundaryListener != null) {
            // Fire only once per word boundary.
            wordBoundaryListener.run();
            lastUpTime = Long.MIN_VALUE / 2; // suppress repeated word-boundary fires
        }
    }

    private void handleDown(long t) {
        if (keyDown) return; // spurious double-down
        keyDown = true;

        if (lastUpTime >= 0) {
            long gap = t - lastUpTime;
            DecodedElement.Kind gapKind = classifyGap(gap);
            DecodedElement gapEl = new DecodedElement(gapKind, gap, lastUpTime);
            sessionElements.add(gapEl);
            if (elementListener != null) elementListener.accept(gapEl);

            if (gapKind == DecodedElement.Kind.CHAR_GAP) {
                flushCharacter(lastUpTime);
            } else if (gapKind == DecodedElement.Kind.WORD_GAP) {
                flushCharacter(lastUpTime);
                if (wordBoundaryListener != null) wordBoundaryListener.run();
            }
        }
        lastDownTime = t;
    }

    private void handleUp(long t) {
        if (!keyDown) return;
        keyDown = false;
        long markLen = Math.max(0, t - lastDownTime);
        DecodedElement.Kind kind = classifyMark(markLen);
        DecodedElement el = new DecodedElement(kind, markLen, lastDownTime);
        sessionElements.add(el);
        if (elementListener != null) elementListener.accept(el);

        currentPattern.append(kind == DecodedElement.Kind.DIT ? '.' : '-');
        lastUpTime = t;

        if (adaptive) updateDitEstimate(markLen, kind);
    }

    private DecodedElement.Kind classifyMark(long markMs) {
        return markMs < estimatedDitMs * cfg.ditDahThresholdRatio
                ? DecodedElement.Kind.DIT
                : DecodedElement.Kind.DAH;
    }

    private DecodedElement.Kind classifyGap(long gapMs) {
        if (gapMs > estimatedDitMs * cfg.wordGapThresholdRatio) return DecodedElement.Kind.WORD_GAP;
        if (gapMs > estimatedDitMs * cfg.charGapThresholdRatio) return DecodedElement.Kind.CHAR_GAP;
        return DecodedElement.Kind.INTRA_GAP;
    }

    private void updateDitEstimate(long markMs, DecodedElement.Kind kind) {
        // Exponential moving average. Dits weight directly, dahs are scaled to dit-equivalent.
        double observed = kind == DecodedElement.Kind.DIT ? markMs : markMs / 3.0;
        estimatedDitMs = 0.85 * estimatedDitMs + 0.15 * observed;
    }

    private void flushCharacter(long endTime) {
        if (currentPattern.length() == 0) return;
        Character c = MorseCode.forPattern(currentPattern.toString());
        if (charListener != null) charListener.accept(c == null ? '?' : c);
        currentPattern.setLength(0);
    }
}
