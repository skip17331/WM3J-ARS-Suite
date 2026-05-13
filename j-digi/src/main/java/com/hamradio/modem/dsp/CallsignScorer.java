package com.hamradio.modem.dsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stateless callsign extractor + confidence scorer.
 *
 * <p>Given the rolling decoded-text buffer from one
 * {@link MultiCarrierDecoder} channel, this returns a list of
 * {@link Candidate} callsigns sorted by descending confidence.
 *
 * <p>Scoring leans hardest on <b>repetition</b>: CW operators
 * routinely send their callsign twice in a row ("CQ CQ DE W1AW W1AW
 * K"), and a second matching token in a short window is the single
 * strongest signal that the regex match is real rather than a
 * garbled coincidence. Context tokens ("CQ", "DE", "TU") next to
 * the match also bump confidence; bare matches with no context and
 * no repetition stay low so Phase C's threshold filters them out.
 *
 * <p>Format match (ITU amateur callsign — at least one letter in
 * the prefix, mandatory district digit, 1–3-letter suffix, optional
 * <code>/P</code>, <code>/M</code>, <code>/4</code> portable
 * suffix):
 * <pre>
 *   W1AW            1×2  (USA)
 *   KK7XYZ          2×3
 *   2E0ABC          UK special prefix
 *   9A1A            Croatia 1×1
 *   W1AW/P          portable
 *   W1AW/4          district override
 * </pre>
 *
 * <p>Common Q-signals (QRZ, QSL, QSO, QRT, QSY, QTH, …) are
 * 3-letter strings with no district digit, so the regex doesn't
 * match them — no separate denylist needed.
 */
public final class CallsignScorer {

    // ─────────────────────────────────────────────────────────────────
    // Tunables
    // ─────────────────────────────────────────────────────────────────

    /** Confidence floor for a bare match with no repetition or context. */
    public static final double BASE_CONFIDENCE = 0.40;
    /** Bonus when the same callsign appears two or more times in the buffer. */
    public static final double REPETITION_BONUS = 0.45;
    /** Bonus when the match is preceded by "CQ", "DE", or "TU" within a few tokens. */
    public static final double CONTEXT_BONUS = 0.30;
    /** Bonus when the match is immediately followed by "K" or "KN" (end-of-tx). */
    public static final double EOT_BONUS = 0.10;
    /** Confidence ceiling (a single match can't break this). */
    public static final double MAX_CONFIDENCE = 0.98;

    /** Window (chars) on either side of a match within which CQ/DE/TU
     *  context tokens count as evidence. */
    private static final int CONTEXT_WINDOW = 12;

    /** ITU amateur callsign pattern. Prefix is 1–2 chars with at least
     *  one letter (handles US/EU/JA + the 2-letter special prefixes
     *  like 2E0/9A0/3D2 that start with a digit), then a single
     *  district digit, then 1–3 suffix letters, with an optional
     *  <code>/X</code> portable suffix. */
    private static final Pattern CALL_RE = Pattern.compile(
        "(?<![A-Z0-9/])" +
        "(" +
        "  [A-Z][A-Z0-9] " +      // 2-char letter-led prefix (KK, VK, VE, …)
        "| [A-Z]         " +      // 1-char letter prefix (W, K, N, F, G, …)
        "| [2-9][A-Z]    " +      // digit-led special (2E, 9A, 3D, …)
        ")" +
        "[0-9]" +                 // mandatory district digit
        "[A-Z]{1,3}" +            // 1–3 letter suffix
        "(?:/[A-Z0-9]{1,3})?" +   // optional portable
        "(?![A-Z0-9])",
        Pattern.COMMENTS);

    /** Context tokens that, when adjacent to a match, boost confidence. */
    private static final Pattern CONTEXT_RE = Pattern.compile(
        "\\b(CQ|DE|TU|RST|QRZ\\?)\\b");

    /** End-of-transmission tokens that, when following a match, boost
     *  confidence. "K", "KN", "SK", "AR" are the common ones. */
    private static final Pattern EOT_RE = Pattern.compile(
        "\\b(K|KN|SK|AR)\\b");

    private CallsignScorer() {}

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Scan {@code text} for callsign candidates. Returns a list sorted
     * by descending confidence — caller filters by threshold.
     */
    public static List<Candidate> score(String text) {
        if (text == null || text.isEmpty()) return Collections.emptyList();
        String norm = text.toUpperCase();

        // Pass 1: find every regex match, count occurrences per call.
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, int[]> firstAt = new HashMap<>();
        Matcher m = CALL_RE.matcher(norm);
        while (m.find()) {
            String call = m.group();
            counts.merge(call, 1, Integer::sum);
            firstAt.computeIfAbsent(call, k -> new int[]{ m.start(), m.end() });
        }
        if (counts.isEmpty()) return Collections.emptyList();

        // Pass 2: score each unique call.
        List<Candidate> out = new ArrayList<>(counts.size());
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String call = e.getKey();
            int reps = e.getValue();
            int[] span = firstAt.get(call);

            double conf = BASE_CONFIDENCE;
            String context = "BARE";

            if (reps >= 2) {
                conf += REPETITION_BONUS;
                context = "RPT";
            }

            // Look at characters near the first appearance for CQ/DE/TU
            // and at characters after for end-of-tx tokens.
            int ctxStart = Math.max(0, span[0] - CONTEXT_WINDOW);
            String beforeWindow = norm.substring(ctxStart, span[0]);
            if (CONTEXT_RE.matcher(beforeWindow).find()) {
                conf += CONTEXT_BONUS;
                // Override the context label so the consumer can see
                // why we promoted — RPT loses to CQ/DE since the
                // operator-intent signal is stronger.
                context = pickContextLabel(beforeWindow);
            }

            int ctxEnd = Math.min(norm.length(), span[1] + CONTEXT_WINDOW);
            String afterWindow = norm.substring(span[1], ctxEnd);
            if (EOT_RE.matcher(afterWindow).find()) {
                conf += EOT_BONUS;
            }

            conf = Math.min(conf, MAX_CONFIDENCE);
            out.add(new Candidate(call, conf, context, reps));
        }

        out.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        return out;
    }

    private static String pickContextLabel(String window) {
        // Most informative context wins — CQ > DE > TU > RST.
        if (window.contains("CQ"))  return "CQ";
        if (window.contains("DE"))  return "DE";
        if (window.contains("TU"))  return "TU";
        if (window.contains("RST")) return "RST";
        if (window.contains("QRZ")) return "QRZ";
        return "CTX";
    }

    // ─────────────────────────────────────────────────────────────────
    // Result record
    // ─────────────────────────────────────────────────────────────────

    /** A single scored callsign. */
    public static final class Candidate {
        /** The callsign as it appeared in the buffer, uppercase. */
        public final String callsign;
        /** Confidence in [0.0, MAX_CONFIDENCE]. */
        public final double confidence;
        /** Why the score is what it is: "CQ", "DE", "TU", "RST", "QRZ",
         *  "RPT" (repeated), or "BARE" (single match with no context). */
        public final String context;
        /** How many times this callsign appeared in the scanned buffer. */
        public final int repetitions;

        public Candidate(String callsign, double confidence, String context, int repetitions) {
            this.callsign    = callsign;
            this.confidence  = confidence;
            this.context     = context;
            this.repetitions = repetitions;
        }

        @Override
        public String toString() {
            return String.format("Candidate{call=%s conf=%.2f ctx=%s reps=%d}",
                                 callsign, confidence, context, repetitions);
        }
    }
}
