package com.ars.fx.data;

import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScorer;
import com.jlog.scoring.StationContext;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Whether working a station would add a multiplier. It asks the scoring engine itself - the log scored with and without the
 * candidate QSO - so every contest's own rules decide (per band or not, zones, countries, sections, prefixes, states...), with
 * no per-contest code here. A dupe never adds one.
 */
public final class MultCheck {
    private MultCheck() {}

    /** Multipliers the log has now, as the engine counts them. */
    static int mults(ContestPlugin plugin, List<QsoRecord> qsos, StationContext ctx) {
        Set<String> bands = new LinkedHashSet<>();
        for (QsoRecord q : qsos) if (q.getBand() != null && !q.getBand().isBlank()) bands.add(q.getBand());
        return ContestScorer.score(plugin, qsos, ctx, plugin.computeMultiplierDbColumn(), new ArrayList<>(bands), 0).mults();
    }

    /** True when {@code candidate} (call, band, mode and exchange filled in; not yet logged) would raise the multiplier count. */
    public static boolean isNewMult(ContestPlugin plugin, List<QsoRecord> log, QsoRecord candidate, StationContext ctx) {
        if (plugin == null || candidate == null || candidate.getCallsign() == null || candidate.getCallsign().isBlank()) return false;
        List<QsoRecord> prior = new ArrayList<>();
        String call = candidate.getCallsign().trim().toUpperCase(Locale.ROOT);
        for (QsoRecord q : log) if (q.getCallsign() != null && q.getCallsign().trim().equalsIgnoreCase(call)) prior.add(q);
        if (ContestScorer.isDupe(plugin, candidate, prior)) return false;
        List<QsoRecord> with = new ArrayList<>(log.size() + 1);
        with.addAll(log); with.add(candidate);
        return mults(plugin, with, ctx) > mults(plugin, log, ctx);
    }

    /**
     * Can this contest's multiplier be told from the callsign alone (DXCC countries, prefixes, zones from the call)? Then spots in
     * the cluster can be flagged before any exchange is known. Found by trying a few calls from different parts of the world.
     */
    public static boolean multFromCallAlone(ContestPlugin plugin, StationContext ctx) {
        for (String call : new String[]{"JA1AAA", "DL1AAA", "VK2AAA", "PY2AAA", "ZS6AAA"}) {
            QsoRecord q = candidateOf(plugin, call, "20m", "CW");
            if (isNewMult(plugin, List.of(), q, ctx)) return true;
        }
        return false;
    }

    /** A bare candidate: call, band and mode only. */
    public static QsoRecord candidateOf(ContestPlugin plugin, String call, String band, String mode) {
        QsoRecord q = new QsoRecord();
        q.setContestId(plugin.getContestId());
        q.setCallsign(call.trim().toUpperCase(Locale.ROOT));
        q.setBand(band == null ? "" : band.trim().toLowerCase(Locale.ROOT));
        q.setMode(mode == null ? "" : mode.trim().toUpperCase(Locale.ROOT));
        q.setDateTimeUtc(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        return q;
    }
}
