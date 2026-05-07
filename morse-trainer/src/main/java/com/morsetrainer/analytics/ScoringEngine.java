package com.morsetrainer.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.morsetrainer.core.AppConfig;
import com.morsetrainer.core.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Tracks correctness for receiving-mode sessions: character-level scoring,
 * word-level scoring, per-character stats, and recommendations for which
 * characters the user should drill next.
 */
public class ScoringEngine {

    private final Map<Character, CharStats> stats = new HashMap<>();
    private final List<String> sessionLog = new ArrayList<>();
    private final long sessionStartMillis = System.currentTimeMillis();
    private long lastPresentedMillis = -1;
    private int totalCharacters = 0;
    private int totalErrors     = 0;
    private int wordsPresented  = 0;
    private int wordsCorrect    = 0;
    private final StringBuilder currentExpectedWord = new StringBuilder();
    private final StringBuilder currentTypedWord    = new StringBuilder();

    public void presented(char expected) {
        lastPresentedMillis = System.currentTimeMillis();
        currentExpectedWord.append(expected);
    }

    public void typed(char expected, char typed) {
        long now = System.currentTimeMillis();
        long response = lastPresentedMillis < 0 ? 0 : now - lastPresentedMillis;
        currentTypedWord.append(typed);
        totalCharacters++;
        CharStats cs = stats.computeIfAbsent(Character.toUpperCase(expected), CharStats::new);
        if (Character.toUpperCase(typed) == Character.toUpperCase(expected)) {
            cs.recordCorrect(response);
        } else {
            cs.recordError();
            totalErrors++;
            sessionLog.add(String.format("ERR: expected=%c typed=%c rt=%dms", expected, typed, response));
        }
    }

    public void wordBoundary() {
        if (currentExpectedWord.length() == 0) return;
        wordsPresented++;
        if (currentExpectedWord.toString().equalsIgnoreCase(currentTypedWord.toString())) wordsCorrect++;
        currentExpectedWord.setLength(0);
        currentTypedWord.setLength(0);
    }

    // ------------------------------------------------------------------ getters

    public double overallAccuracy() {
        return totalCharacters == 0 ? 0.0 : 1.0 - ((double) totalErrors / totalCharacters);
    }

    public double wordAccuracy() {
        return wordsPresented == 0 ? 0.0 : (double) wordsCorrect / wordsPresented;
    }

    public int totalCharacters() { return totalCharacters; }
    public int totalErrors()     { return totalErrors; }

    public Map<Character, CharStats> perCharacter() { return Map.copyOf(stats); }

    /** Achieved WPM during the receive session, computed from elapsed time. */
    public double achievedWpm() {
        long elapsedMs = System.currentTimeMillis() - sessionStartMillis;
        if (elapsedMs <= 0 || totalCharacters == 0) return 0.0;
        // 5 chars per "word" by convention.
        double minutes = elapsedMs / 60_000.0;
        return (totalCharacters / 5.0) / minutes;
    }

    /** Heatmap: each tracked character mapped to its error rate (0..1). */
    public Map<Character, Double> errorHeatmap() {
        Map<Character, Double> map = new TreeMap<>();
        for (CharStats s : stats.values()) map.put(s.character, s.errorRate());
        return map;
    }

    /** Top N characters with the worst error rate that the user should drill next. */
    public List<Character> recommendedDrills(int n) {
        return stats.values().stream()
                .filter(s -> s.presented >= 2)
                .filter(s -> s.errors > 0)
                .sorted(Comparator.comparingDouble(CharStats::errorRate).reversed())
                .limit(n)
                .map(s -> s.character)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ export

    public Map<String, Object> snapshot() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("timestamp", Instant.ofEpochMilli(sessionStartMillis).toString());
        root.put("totalCharacters", totalCharacters);
        root.put("totalErrors", totalErrors);
        root.put("overallAccuracy", overallAccuracy());
        root.put("wordAccuracy", wordAccuracy());
        root.put("achievedWpm", achievedWpm());
        root.put("recommended", recommendedDrills(5));

        List<Map<String, Object>> chars = new ArrayList<>();
        for (CharStats s : new TreeMap<>(stats).values()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("char", String.valueOf(s.character));
            cm.put("presented", s.presented);
            cm.put("correct", s.correct);
            cm.put("errors", s.errors);
            cm.put("errorRate", s.errorRate());
            cm.put("avgResponseMs", s.avgResponseMillis());
            cm.put("slowestResponseMs", s.slowestResponseMillis);
            chars.add(cm);
        }
        root.put("characters", chars);
        root.put("errorLog", sessionLog);
        return root;
    }

    public Path exportJson() {
        AppConfig cfg = AppConfig.get();
        try {
            Path dir = Paths.get(cfg.sessionLogDir);
            Files.createDirectories(dir);
            Path file = dir.resolve("session-" + sessionStartMillis + ".json");
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(file.toFile(), snapshot());
            return file;
        } catch (IOException e) {
            Logger.warn("Failed to export session: %s", e.getMessage());
            return null;
        }
    }
}
