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
        AppConfig.getInstance().setActiveContestId(id);
        HubConfig.set("contest.activeId", id);   // mirror for parity
        plugin = id.isBlank() ? null : PluginLoader.getInstance().getById(id);
        recompute();
    }

    /** Resolve the plugin from the persisted active id if not already loaded. */
    public static void ensureLoaded() {
        if (plugin == null) {
            String id = activeId();
            if (!id.isBlank()) plugin = PluginLoader.getInstance().getById(id);
        }
    }

    public static void addListener(Runnable r) { if (r != null) listeners.add(r); }
    public static void clearListeners() { listeners.clear(); }

    /** Operator facts for scoring, from ars-fx HubConfig. */
    public static StationContext ctx() {
        return StationContext.of(HubConfig.call(), HubConfig.grid(), HubConfig.get("contest.ss.section", ""));
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

    /**
     * "Start fresh": export the current contest log to Cabrillo as a backup, then
     * clear this contest's rows (raw two-table delete — the engine's
     * deleteAllForContest references a non-existent contest_qtc.qso_id column).
     */
    public static void backupAndStartFresh() {
        if (plugin == null) return;
        // backup → Cabrillo
        try {
            ContestConfigBridge.syncStationToEngine();
            java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), ".j-log", "exports");
            java.nio.file.Files.createDirectories(dir);
            String ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            com.jlog.export.CabrilloExporter.export(plugin,
                    dir.resolve(plugin.getContestId() + "_" + ts + "_backup.cbr"));
        } catch (Exception e) {
            System.err.println("[contest] backup before clear failed: " + e.getMessage());
        }
        // clear this contest's rows
        try {
            java.sql.Connection c = com.jlog.db.DatabaseManager.getInstance().getContestConnection();
            try (java.sql.PreparedStatement ps = c.prepareStatement("DELETE FROM contest_qtc WHERE contest_id=?")) { ps.setString(1, plugin.getContestId()); ps.executeUpdate(); }
            try (java.sql.PreparedStatement ps = c.prepareStatement("DELETE FROM contest_qso WHERE contest_id=?")) { ps.setString(1, plugin.getContestId()); ps.executeUpdate(); }
        } catch (Exception e) {
            System.err.println("[contest] clear failed: " + e.getMessage());
        }
        recompute();
    }

    /** Recompute the aggregate score from the contest log and notify listeners. */
    public static synchronized void recompute() {
        if (plugin == null) { score = ContestScore.of(0, 0, 0, 0); fire(); return; }
        try {
            List<QsoRecord> qsos = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
            LinkedHashSet<String> bands = new LinkedHashSet<>();
            for (QsoRecord q : qsos) if (q.getBand() != null && !q.getBand().isBlank()) bands.add(q.getBand());
            score = ContestScorer.score(plugin, qsos, ctx(), plugin.computeMultiplierDbColumn(),
                    new ArrayList<>(bands), 0);
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
