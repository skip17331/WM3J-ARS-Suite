package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScorer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which QSOs of a contest log are dupes, decided oldest first by the contest's own dupe rule. Used after a QSO is edited or erased:
 * erasing the first contact with a station frees a later one that had been marked a dupe, and editing a band or call can turn a
 * later QSO into one.
 */
public final class DupeFlags {
    private DupeFlags() {}

    /** Re-decide every record's dupe flag; returns the records whose flag changed (already updated in place). */
    public static List<QsoRecord> recompute(ContestPlugin plugin, List<QsoRecord> all) {
        List<QsoRecord> sorted = new ArrayList<>(all);
        sorted.sort(Comparator.comparing(QsoRecord::getDateTimeUtc, Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())));
        Map<String, List<QsoRecord>> earlier = new HashMap<>();
        List<QsoRecord> changed = new ArrayList<>();
        for (QsoRecord q : sorted) {
            String call = q.getCallsign() == null ? "" : q.getCallsign().trim().toUpperCase(Locale.ROOT);
            List<QsoRecord> prior = earlier.computeIfAbsent(call, k -> new ArrayList<>());
            boolean d = ContestScorer.isDupe(plugin, q, prior);
            if (d != q.isDupe()) { q.setDupe(d); changed.add(q); }
            prior.add(q);                       // isDupe ignores earlier dupes itself
        }
        return changed;
    }
}
