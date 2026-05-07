package com.morsetrainer.trainer.letters;

import com.morsetrainer.core.AppConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates random characters for the Letter Trainer. Supports the Koch method:
 * the user starts with two characters, then unlocks one at a time. Also supports
 * arbitrary user-specified character sets.
 */
public class LetterSession {

    private final List<Character> alphabet;
    private final Random random;
    private final int sessionLength;
    private final List<Character> queue = new ArrayList<>();
    private int index = 0;

    public LetterSession(List<Character> alphabet, int sessionLength, Random random) {
        if (alphabet == null || alphabet.isEmpty())
            throw new IllegalArgumentException("alphabet must be non-empty");
        this.alphabet = List.copyOf(alphabet);
        this.sessionLength = sessionLength;
        this.random = random;
        rebuild();
    }

    public LetterSession(List<Character> alphabet, int sessionLength) {
        this(alphabet, sessionLength, new Random());
    }

    public static LetterSession kochLevel(int level, int sessionLength) {
        return kochLevel(level, sessionLength, AppConfig.get(), new Random());
    }

    /** Build a Koch session using the configured Koch order, taking the first {@code level} chars. */
    public static LetterSession kochLevel(int level, int sessionLength, AppConfig cfg, Random rng) {
        String order = cfg.kochOrder;
        level = Math.max(2, Math.min(level, order.length()));
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < level; i++) chars.add(order.charAt(i));
        return new LetterSession(chars, sessionLength, rng);
    }

    private void rebuild() {
        queue.clear();
        for (int i = 0; i < sessionLength; i++) queue.add(alphabet.get(random.nextInt(alphabet.size())));
        // Guard against runs of identical characters at very small alphabets — re-shuffle a copy.
        Collections.shuffle(queue, random);
        index = 0;
    }

    public boolean hasNext() { return index < queue.size(); }
    public char next() { return queue.get(index++); }
    public int remaining() { return queue.size() - index; }
    public List<Character> alphabet() { return alphabet; }
    public int sessionLength() { return sessionLength; }
    public List<Character> snapshot() { return List.copyOf(queue); }
}
