package com.morsetrainer.trainer.groups;

import com.morsetrainer.core.AppConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Letter Group Trainer: produces space-separated random groups of length [min..max].
 * Supports custom alphabets (e.g. only call-sign characters).
 */
public class GroupSession {

    private final List<Character> alphabet;
    private final int minSize;
    private final int maxSize;
    private final int totalGroups;
    private final Random random;
    private int produced = 0;

    public GroupSession(List<Character> alphabet, int minSize, int maxSize, int totalGroups, Random random) {
        if (minSize < 1 || maxSize < minSize)
            throw new IllegalArgumentException("invalid group sizes");
        this.alphabet = List.copyOf(alphabet);
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.totalGroups = totalGroups;
        this.random = random;
    }

    public GroupSession(List<Character> alphabet, int totalGroups) {
        this(alphabet,
             AppConfig.get().groupMinSize,
             AppConfig.get().groupMaxSize,
             totalGroups,
             new Random());
    }

    public boolean hasNext() { return produced < totalGroups; }

    public String next() {
        produced++;
        int len = minSize + random.nextInt(maxSize - minSize + 1);
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(alphabet.get(random.nextInt(alphabet.size())));
        return sb.toString();
    }

    public List<String> generateAll() {
        List<String> groups = new ArrayList<>(totalGroups);
        while (hasNext()) groups.add(next());
        return groups;
    }

    public int total() { return totalGroups; }
    public int produced() { return produced; }
}
