package com.morsetrainer.trainer.sendpractice;

import com.morsetrainer.analytics.SendingDiagnostics;
import com.morsetrainer.decoder.DecodedElement;
import com.morsetrainer.decoder.KeyEvent;
import com.morsetrainer.decoder.TimingDecoder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Drives the Sending Practice mode. Receives KeyEvents from any source
 * (keyboard, Arduino, Pi Zero), feeds the decoder, and accumulates per-
 * character timing samples for diagnostics.
 *
 * Two modes:
 *  - GUIDED: a target string is given; decoded output is compared character
 *    by character.
 *  - FREE:   no target; the user just sends, the system decodes and reports.
 */
public class SendPracticeSession {

    public enum Mode { GUIDED, FREE }

    private final Mode mode;
    private final String target;
    private final TimingDecoder decoder;
    private final StringBuilder decoded = new StringBuilder();
    private final List<DecodedElement> elements = new ArrayList<>();
    private final Map<Character, List<Long>> ditByChar = new HashMap<>();
    private final Map<Character, List<Long>> dahByChar = new HashMap<>();
    private final List<DecodedElement> currentCharBuffer = new ArrayList<>();
    private Consumer<Character> charListener;
    private Consumer<DecodedElement> elementListener;

    public SendPracticeSession(Mode mode, String target, TimingDecoder decoder) {
        this.mode = mode;
        this.target = target == null ? "" : target.toUpperCase();
        this.decoder = decoder;

        decoder.onElement(el -> {
            elements.add(el);
            if (el.isMark()) currentCharBuffer.add(el);
            if (elementListener != null) elementListener.accept(el);
        });
        decoder.onCharacter(c -> {
            decoded.append(c);
            // Bind buffered marks to this character for variance analysis.
            char key = Character.toUpperCase(c);
            for (DecodedElement el : currentCharBuffer) {
                if (el.kind() == DecodedElement.Kind.DIT)
                    ditByChar.computeIfAbsent(key, k -> new ArrayList<>()).add(el.durationMillis());
                else if (el.kind() == DecodedElement.Kind.DAH)
                    dahByChar.computeIfAbsent(key, k -> new ArrayList<>()).add(el.durationMillis());
            }
            currentCharBuffer.clear();
            if (charListener != null) charListener.accept(c);
        });
        decoder.onWordBoundary(() -> { /* word handling delegated to UI */ });
    }

    public void onCharacter(Consumer<Character> l) { this.charListener = l; }
    public void onElement(Consumer<DecodedElement> l) { this.elementListener = l; }

    public void accept(KeyEvent ev) { decoder.accept(ev); }
    public void tick(long now) { decoder.tick(now); }

    public Mode mode()        { return mode; }
    public String target()    { return target; }
    public String decoded()   { return decoded.toString(); }

    public SendingDiagnostics.Result diagnostics() {
        return new SendingDiagnostics().analyse(elements);
    }

    public SendingDiagnostics.TroubleReport troubleReport() {
        return new SendingDiagnostics()
                .buildTroubleReport(target, decoded.toString(), ditByChar, dahByChar);
    }

    /** Guided-mode character-by-character correctness ratio. */
    public double accuracy() {
        if (mode != Mode.GUIDED || target.isEmpty()) return 0.0;
        int n = Math.min(target.length(), decoded.length());
        int correct = 0;
        for (int i = 0; i < n; i++) {
            if (target.charAt(i) == Character.toUpperCase(decoded.charAt(i))) correct++;
        }
        return n == 0 ? 0.0 : (double) correct / target.length();
    }
}
