package com.jlog.scoring;

import java.util.List;
import java.util.Map;

/**
 * Result of an aggregate contest score computation — the pure output of
 * {@link ContestScorer#score}. Carries the four cockpit numbers, a
 * {@link Painter} tag telling the UI which multiplier-display model applies,
 * and the worked-multiplier token collections that model needs. Every token
 * collection is non-null (empty when the chosen painter does not use it).
 *
 * <p>The {@link Painter} keeps painting faithful to the old per-branch logic
 * without the controller having to re-derive the contest family:
 * <ul>
 *   <li>{@code WORKED} — flat distinct list paints section labels <em>and</em>
 *       every "all worked" map/table pane (the default multiplier model).</li>
 *   <li>{@code SECTIONS} — flat list paints section labels only (Field-Day /
 *       points-only families).</li>
 *   <li>{@code PER_MODE} — {@code workedByMode} paints the per-mode grid and the
 *       union onto the maps.</li>
 *   <li>{@code PER_BAND} — {@code workedByBand} paints the worked-grids pane.</li>
 *   <li>{@code ZONES} — {@code zonesWorked} paints the CQ zone map.</li>
 *   <li>{@code NONE} — labels only (callsign-derived DX multiplier families).</li>
 * </ul>
 */
public record ContestScore(
        int qsoCount,
        int points,
        int mults,
        int score,
        Painter painter,
        List<String> worked,
        Map<String, List<String>> workedByMode,
        Map<String, List<String>> workedByBand,
        List<String> zonesWorked) {

    public enum Painter { NONE, WORKED, SECTIONS, PER_MODE, PER_BAND, ZONES }

    public ContestScore {
        painter      = painter      == null ? Painter.NONE : painter;
        worked       = worked       == null ? List.of() : worked;
        workedByMode = workedByMode == null ? Map.of()  : workedByMode;
        workedByBand = workedByBand == null ? Map.of()  : workedByBand;
        zonesWorked  = zonesWorked  == null ? List.of() : zonesWorked;
    }

    /** Labels-only result — no painter (callsign-derived DX multiplier families). */
    public static ContestScore of(int qsoCount, int points, int mults, int score) {
        return new ContestScore(qsoCount, points, mults, score, Painter.NONE, null, null, null, null);
    }

    /** Flat worked list paints section labels + all map/table panes (default model). */
    public static ContestScore withWorked(int qsoCount, int points, int mults, int score, List<String> worked) {
        return new ContestScore(qsoCount, points, mults, score, Painter.WORKED, worked, null, null, null);
    }

    /** Flat worked list paints section labels only (Field-Day / points-only). */
    public static ContestScore sectionsOnly(int qsoCount, int points, int mults, int score, List<String> worked) {
        return new ContestScore(qsoCount, points, mults, score, Painter.SECTIONS, worked, null, null, null);
    }

    /** Per-mode distinct values paint the per-mode grid and the map union. */
    public static ContestScore perMode(int qsoCount, int points, int mults, int score,
                                       Map<String, List<String>> workedByMode) {
        return new ContestScore(qsoCount, points, mults, score, Painter.PER_MODE, null, workedByMode, null, null);
    }

    /** Per-band distinct values paint the worked-grids pane. */
    public static ContestScore perBand(int qsoCount, int points, int mults, int score,
                                       Map<String, List<String>> workedByBand) {
        return new ContestScore(qsoCount, points, mults, score, Painter.PER_BAND, null, null, workedByBand, null);
    }

    /** Distinct zones across bands paint the CQ zone map. */
    public static ContestScore zones(int qsoCount, int points, int mults, int score, List<String> zonesWorked) {
        return new ContestScore(qsoCount, points, mults, score, Painter.ZONES, null, null, null, zonesWorked);
    }
}
