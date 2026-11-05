package com.ars.fx.surface.contest;

import com.ars.fx.data.ContestState;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.QsoParty;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The bonus stations of a QSO party: the calls the party pays extra points for, what each is worth, and whether you have worked
 * them (and in which modes where the bonus is per mode). The scoring engine already counts the bonuses; this shows what is still to work.
 */
public final class BonusPane {
    private BonusPane() {}

    /** One bonus station: its call, points, and how the bonus is earned. */
    public record Bonus(String call, int points, String scope) {}

    /** The bonus stations a party defines, soonest-to-read order; empty when it has none. */
    public static List<Bonus> bonuses(ContestPlugin plugin) {
        List<Bonus> out = new ArrayList<>();
        if (plugin == null || plugin.getScoringRules() == null || plugin.getScoringRules().getQsoParty() == null) return out;
        ContestPlugin.QsoPartyConfig c = plugin.getScoringRules().getQsoParty();
        add(out, c.getBonusStations(), "each QSO");
        add(out, c.getBonusStationsOnce(), "once");
        add(out, c.getBonusStationsPerMode(), "once per mode");
        return out;
    }
    private static void add(List<Bonus> out, Map<String, Integer> m, String scope) {
        if (m == null) return;
        new TreeMap<>(m).forEach((call, pts) -> out.add(new Bonus(call.trim().toUpperCase(Locale.ROOT), pts == null ? 0 : pts, scope)));
    }

    /** For each bonus call, the modes it has been worked in (empty = not worked). */
    public static Map<String, Set<String>> worked(List<Bonus> bonuses, List<QsoRecord> log) {
        Map<String, Set<String>> w = new TreeMap<>();
        Set<String> wanted = new TreeSet<>();
        for (Bonus b : bonuses) wanted.add(b.call());
        for (QsoRecord q : log) {
            if (q.isDupe()) continue;
            String base = QsoParty.baseCall(q.getCallsign()).toUpperCase(Locale.ROOT);
            if (wanted.contains(base)) w.computeIfAbsent(base, k -> new TreeSet<>()).add(QsoParty.modeClass(q.getMode()));
        }
        return w;
    }

    public static boolean has(ContestPlugin plugin) { return !bonuses(plugin).isEmpty(); }

    public static Region build(ContestPlugin plugin) {
        List<Bonus> bonuses = bonuses(plugin);
        VBox rows = new VBox(3);
        Runnable fill = () -> {
            Map<String, Set<String>> w = worked(bonuses, ContestState.qsos());
            rows.getChildren().clear();
            int got = 0;
            for (Bonus b : bonuses) {
                Set<String> modes = w.getOrDefault(b.call(), Set.of());
                boolean done = !modes.isEmpty();
                if (done) got++;
                Label mark = lbl(done ? "✔" : "○", "ct-note"); mark.setMinWidth(18);
                mark.setStyle("-fx-text-fill:" + (done ? "-ars-ok" : "-ars-t4") + ";");
                Label call = lbl(b.call(), "jhub-card-title"); call.setMinWidth(90);
                Label pts = lbl(b.points() + " pts · " + b.scope(), "ct-note");
                Label md = lbl(modes.isEmpty() ? "" : String.join(" ", modes), "ct-note");
                HBox r = new HBox(8, mark, call, pts, md); r.setAlignment(Pos.CENTER_LEFT);
                rows.getChildren().add(r);
            }
            rows.getChildren().add(0, lbl(got + " of " + bonuses.size() + " worked", "ct-note"));
        };
        fill.run();
        ContestState.addListener(() -> Platform.runLater(fill));
        VBox box = new VBox(4, rows); box.setPadding(new Insets(2));
        return box;
    }
}
