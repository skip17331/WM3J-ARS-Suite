package com.morsetrainer.audio;

import com.morsetrainer.core.AppConfig;
import com.morsetrainer.decoder.MorseCode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Plays text as Morse code at configurable WPM with optional Farnsworth spacing.
 *
 * Standard timing (PARIS):
 *   dit             = 1200 / wpm  ms
 *   dah             = 3 * dit
 *   intra-element   = 1 * dit
 *   inter-character = 3 * dit
 *   inter-word      = 7 * dit
 *
 * Farnsworth: characters are still keyed at {@code wpm}, but the inter-character
 * and inter-word gaps are stretched so that the *overall* sending rate is
 * {@code farnsworthWpm}. This keeps individual letters fast while giving the
 * student more time to think between letters.
 */
public class MorsePlayer {

    private final AppConfig cfg;
    private final ToneGenerator tone;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "morse-player");
                t.setDaemon(true);
                return t;
            });

    public MorsePlayer(ToneGenerator tone, AppConfig cfg) {
        this.tone = tone;
        this.cfg = cfg;
    }

    public MorsePlayer(ToneGenerator tone) { this(tone, AppConfig.get()); }

    public CompletableFuture<Void> play(String text) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        scheduler.submit(() -> {
            try { playBlocking(text); done.complete(null); }
            catch (Throwable t) { done.completeExceptionally(t); }
        });
        return done;
    }

    public void playBlocking(String text) throws InterruptedException {
        long ditMs = (long) AppConfig.ditMillis(cfg.wpm);
        long dahMs = ditMs * 3;
        long intraGapMs = ditMs;

        // Farnsworth stretch — extra time only between characters and words.
        // Total time of a standard "PARIS " word at character-WPM:
        //   50 * ditMs
        // Required total at farnsworth-WPM:
        //   1200/farnsworth * 50 -> different value. Spread the difference across gaps.
        int effective = Math.min(cfg.farnsworthWpm, cfg.wpm);
        long charGapMs;
        long wordGapMs;
        if (effective < cfg.wpm) {
            // Total time per word = 60_000 / effectiveWpm * (5/25) basically — keep it simple:
            // distribute gap stretching using the standard ARRL formula.
            double ta = (60.0 * cfg.wpm - 37.2 * effective) / (effective * cfg.wpm); // sec / unit
            charGapMs = Math.round(3 * ta * 1000);
            wordGapMs = Math.round(7 * ta * 1000);
            charGapMs = Math.max(charGapMs, ditMs * 3L);
            wordGapMs = Math.max(wordGapMs, ditMs * 7L);
        } else {
            charGapMs = ditMs * 3L;
            wordGapMs = ditMs * 7L;
        }

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                Thread.sleep(wordGapMs);
                continue;
            }
            String pattern = MorseCode.forChar(c);
            if (pattern == null) continue;

            for (int k = 0; k < pattern.length(); k++) {
                char el = pattern.charAt(k);
                tone.keyDown();
                Thread.sleep(el == '.' ? ditMs : dahMs);
                tone.keyUp();
                if (k < pattern.length() - 1) Thread.sleep(intraGapMs);
            }

            // Inter-character gap unless next is a space (which contributes a full word gap).
            boolean nextIsSpace = i + 1 < text.length() && text.charAt(i + 1) == ' ';
            if (!nextIsSpace && i < text.length() - 1) Thread.sleep(charGapMs);
        }
        tone.keyUp();
    }

    public void shutdown() { scheduler.shutdownNow(); }
}
