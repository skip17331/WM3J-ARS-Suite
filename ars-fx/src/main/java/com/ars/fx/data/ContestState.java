package com.ars.fx.data;

import com.jlog.db.ContestQsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.ContestScorer;
import com.jlog.scoring.StationContext;
import com.jlog.util.AppConfig;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Active-contest state for J-Log contest mode: the selected {@link ContestPlugin},
 * the latest aggregate {@link ContestScore}, and the running serial. Recomputes
 * the score from {@code contest.db} after each save (and on the surface's timer)
 * and notifies listeners (score readout, trackers, maps) to repaint.
 */
public final class ContestState {
    private ContestState() {}

    private static volatile ContestPlugin plugin;
    private static volatile ContestScore score = ContestScore.of(0, 0, 0, 0);
    private static final List<Runnable> listeners = new ArrayList<>();

    public static ContestPlugin plugin() { return plugin; }
    public static ContestScore score()  { return score; }

    public static String activeId() {
        String id = AppConfig.getInstance().getActiveContestId();
        return id == null ? "" : id;
    }

    /** Make {@code contestId} the active contest, persist it, and recompute. {@code null} clears it. */
    public static void setActive(String contestId) {
        String id = contestId == null ? "" : contestId;
        String prev = activeId();
        AppConfig.getInstance().setActiveContestId(id);
        HubConfig.set("contest.activeId", id);   // mirror for parity
        if (!id.isBlank()) HubConfig.set("contest.last", id);   // survives "choose another contest", which clears the active id
        if (!id.equals(prev)) ContestLogs.resetConfirmation();  // a different contest asks which database again
        plugin = id.isBlank() ? null : PluginLoader.getInstance().getById(id);
        recompute();
    }

    /** The contest chosen most recently, even if the active contest was cleared to pick another. */
    public static String lastId() {
        String l = HubConfig.get("contest.last", "");
        return l.isBlank() ? activeId() : l;
    }

    /** Resolve the plugin from the persisted active id if not already loaded. */
    public static void ensureLoaded() {
        ContestConfigBridge.bootstrap();   // idempotent; AppConfig/DatabaseManager must be up before reading the active id
        if (plugin == null) {
            String id = activeId();
            if (!id.isBlank()) plugin = PluginLoader.getInstance().getById(id);
        }
    }

    public static void addListener(Runnable r) { if (r != null) listeners.add(r); }
    public static void clearListeners() { listeners.clear(); }

    /** Operator facts for scoring, from ars-fx HubConfig. */
    public static StationContext ctx() {
        // QSO parties score in-state and out-of-state entrants differently, by the sent QTH (county or state): that, not the SS section
        String qth = plugin != null && QsoPartySide.isParty(plugin) ? QsoPartySide.sentQth(plugin) : HubConfig.get("contest.ss.section", "");
        return StationContext.of(HubConfig.call(), HubConfig.grid(), qth);
    }

    /**
     * The set of worked multiplier values for lighting trackers/maps. Unions what
     * the engine reports (sections/zones/per-mode/per-band) with the distinct
     * logged values of the multiplier field column (states/counties/grids/etc.) —
     * the latter covers contests whose painter is NONE (count only).
     */
    public static java.util.Set<String> workedMults() {
        java.util.Set<String> w = new java.util.HashSet<>();
        if (plugin == null) return w;
        ContestScore s = score;
        w.addAll(s.worked());
        s.workedByMode().values().forEach(w::addAll);
        s.workedByBand().values().forEach(w::addAll);
        w.addAll(s.zonesWorked());
        try {
            String col = plugin.computeMultiplierDbColumn();
            if (col != null && col.startsWith("field"))
                w.addAll(ContestQsoDao.getInstance().distinctFieldByColumn(plugin.getContestId(), col));
        } catch (Exception ignored) {}
        return w;
    }

    public static int nextSerial() {
        if (plugin == null) return 1;
        try { return ContestQsoDao.getInstance().maxSerialSent(plugin.getContestId()) + 1; }
        catch (Exception e) { return 1; }
    }

    public static List<QsoRecord> qsos() {
        if (plugin == null) return List.of();
        try { return ContestQsoDao.getInstance().fetchByContest(plugin.getContestId()); }
        catch (Exception e) { return List.of(); }
    }

    /** Recompute the aggregate score from the contest log and notify listeners. */
    public static synchronized void recompute() {
        if (plugin == null) { score = ContestScore.of(0, 0, 0, 0); fire(); return; }
        try {
            List<QsoRecord> qsos = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
            LinkedHashSet<String> bands = new LinkedHashSet<>();
            for (QsoRecord q : qsos) if (q.getBand() != null && !q.getBand().isBlank()) bands.add(q.getBand());
            int qtc = 0;
            try { qtc = com.jlog.db.ContestQtcDao.getInstance().totalQtcPointsByContest(plugin.getContestId()); } catch (Exception ignored) { /* none logged */ }
            score = ContestScorer.score(plugin, qsos, ctx(), plugin.computeMultiplierDbColumn(),
                    new ArrayList<>(bands), qtc);
        } catch (Exception e) {
            score = ContestScore.of(0, 0, 0, 0);
        }
        fire();
    }

    private static void fire() {
        for (Runnable r : new ArrayList<>(listeners)) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }
}
