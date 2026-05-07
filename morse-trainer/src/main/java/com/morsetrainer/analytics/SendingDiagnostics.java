package com.morsetrainer.analytics;

import com.morsetrainer.decoder.DecodedElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Analyses elements produced by the user's keying for diagnostic purposes:
 * dit/dah length variance, spacing errors, achieved WPM, dit:dah ratio,
 * and a smoothness score derived from coefficient of variation.
 *
 * Pure data class — no I/O, fully unit-testable.
 */
public class SendingDiagnostics {

    public static class Result {
        public double avgDitMs;
        public double avgDahMs;
        public double ditStdDevMs;
        public double dahStdDevMs;
        public double avgIntraGapMs;
        public double avgCharGapMs;
        public double avgWordGapMs;
        public double intraGapStdDev;
        public double charGapStdDev;
        public double ditDahRatio;          // observed dah / dit (target 3.0)
        public double consistencyScore;     // 0..100 — higher is better
        public double smoothnessIndex;      // 0..100 — penalises CoV
        public double achievedWpm;
        public List<TimingIssue> issues = new ArrayList<>();
    }

    public record TimingIssue(String code, String description, long timestamp) {}

    public Result analyse(List<DecodedElement> elements) {
        Result r = new Result();
        if (elements == null || elements.isEmpty()) return r;

        List<Long> dits = new ArrayList<>();
        List<Long> dahs = new ArrayList<>();
        List<Long> intra = new ArrayList<>();
        List<Long> chars = new ArrayList<>();
        List<Long> words = new ArrayList<>();

        for (DecodedElement e : elements) {
            switch (e.kind()) {
                case DIT       -> dits.add(e.durationMillis());
                case DAH       -> dahs.add(e.durationMillis());
                case INTRA_GAP -> intra.add(e.durationMillis());
                case CHAR_GAP  -> chars.add(e.durationMillis());
                case WORD_GAP  -> words.add(e.durationMillis());
            }
        }

        r.avgDitMs       = mean(dits);
        r.avgDahMs       = mean(dahs);
        r.ditStdDevMs    = stdDev(dits, r.avgDitMs);
        r.dahStdDevMs    = stdDev(dahs, r.avgDahMs);
        r.avgIntraGapMs  = mean(intra);
        r.avgCharGapMs   = mean(chars);
        r.avgWordGapMs   = mean(words);
        r.intraGapStdDev = stdDev(intra, r.avgIntraGapMs);
        r.charGapStdDev  = stdDev(chars, r.avgCharGapMs);
        r.ditDahRatio    = r.avgDitMs == 0 ? 0 : r.avgDahMs / r.avgDitMs;

        // PARIS-based WPM derived from observed dit length: 1200 / ditMs
        r.achievedWpm    = r.avgDitMs > 0 ? 1200.0 / r.avgDitMs : 0;

        double ditCov = r.avgDitMs > 0 ? r.ditStdDevMs / r.avgDitMs : 1;
        double dahCov = r.avgDahMs > 0 ? r.dahStdDevMs / r.avgDahMs : 1;
        double avgCov = (ditCov + dahCov) / 2.0;
        r.consistencyScore = Math.max(0, 100 - avgCov * 100);
        r.smoothnessIndex  = Math.max(0, 100 - (ditCov + dahCov + (r.avgIntraGapMs > 0 ? r.intraGapStdDev / r.avgIntraGapMs : 0)) * 50);

        // Issue detection.
        if (r.ditDahRatio > 0 && Math.abs(r.ditDahRatio - 3.0) > 0.5)
            r.issues.add(new TimingIssue("BAD_DIT_DAH_RATIO",
                    String.format("Observed dit:dah ratio %.2f (target 3.0)", r.ditDahRatio), 0));

        for (Long g : chars)
            if (r.avgIntraGapMs > 0 && g < r.avgIntraGapMs * 1.5)
                r.issues.add(new TimingIssue("MERGED_CHARS",
                        "Inter-character gap too short — letters running together", 0));
        for (Long g : intra)
            if (r.avgDitMs > 0 && g > r.avgDitMs * 2)
                r.issues.add(new TimingIssue("LATE_ELEMENT",
                        "Excessive intra-character gap — element keyed too late", 0));

        return r;
    }

    /**
     * Per-character problem report for guided sending mode. Compares the user's
     * typed-out characters (from decoder output) to the expected text and
     * accumulates per-character mis-decode counts and timing variance.
     */
    public static class TroubleReport {
        public Map<Character, Integer> miscodes        = new LinkedHashMap<>();
        public Map<Character, Double>  avgVariance     = new LinkedHashMap<>();
        public List<Character>         worstByMiscode  = new ArrayList<>();
        public List<Character>         worstByTiming   = new ArrayList<>();
    }

    public TroubleReport buildTroubleReport(String expected, String decoded,
                                            Map<Character, List<Long>> ditDurationsByChar,
                                            Map<Character, List<Long>> dahDurationsByChar) {
        TroubleReport rep = new TroubleReport();

        int n = Math.min(expected.length(), decoded.length());
        for (int i = 0; i < n; i++) {
            char e = Character.toUpperCase(expected.charAt(i));
            char d = Character.toUpperCase(decoded.charAt(i));
            if (e != d) rep.miscodes.merge(e, 1, Integer::sum);
        }

        // Variance per character: combined dit + dah std-dev / mean
        for (var entry : ditDurationsByChar.entrySet()) {
            char c = entry.getKey();
            List<Long> dits = entry.getValue();
            List<Long> dahs = dahDurationsByChar.getOrDefault(c, List.of());
            double ditMean = mean(dits);
            double dahMean = mean(dahs);
            double cov = 0;
            int n2 = 0;
            if (ditMean > 0) { cov += stdDev(dits, ditMean) / ditMean; n2++; }
            if (dahMean > 0) { cov += stdDev(dahs, dahMean) / dahMean; n2++; }
            if (n2 > 0) rep.avgVariance.put(c, cov / n2);
        }

        rep.worstByMiscode = rep.miscodes.entrySet().stream()
                .sorted(Map.Entry.<Character, Integer>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        rep.worstByTiming = rep.avgVariance.entrySet().stream()
                .sorted(Map.Entry.<Character, Double>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toList();
        return rep;
    }

    private static double mean(List<Long> xs) {
        if (xs.isEmpty()) return 0.0;
        long sum = 0;
        for (long x : xs) sum += x;
        return (double) sum / xs.size();
    }

    private static double stdDev(List<Long> xs, double mean) {
        if (xs.size() < 2) return 0.0;
        double sumSq = 0;
        for (long x : xs) sumSq += (x - mean) * (x - mean);
        return Math.sqrt(sumSq / (xs.size() - 1));
    }
}
