package com.ars.fx.surface.contest;

import com.ars.fx.data.ContestState;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.PaneDef;
import com.jlog.plugin.MultiplierLists;
import com.jlog.scoring.ContestScore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Builds a contest multiplier-tracker pane for a plugin {@link PaneDef}, driven
 * by the live {@link ContestScore}. Score-driven trackers ("light-up boxes"):
 * statistics, worked_mults/custom, section_tracker, dxcc_list, county_list.
 * Map pane types are handled in Phase C (returns null here). Each pane registers
 * a {@link ContestState} listener so it repaints when the score recomputes.
 */
public final class TrackerFactory {
    private TrackerFactory() {}

    /** A titled tracker card, or null when the paneType isn't a Phase-B tracker (e.g. maps). */
    public static Region paneFor(PaneDef pd, ContestPlugin plugin) {
        String type = pd.getPaneType() == null ? "" : pd.getPaneType();
        String title = pd.getTitle() == null || pd.getTitle().isBlank() ? prettify(type) : pd.getTitle();
        Region inner = switch (type) {
            case "statistics"             -> statsPane();
            case "worked_mults", "custom" -> lightbox(universeFor(plugin));
            case "section_tracker"        -> lightbox(plugin.getSections());
            case "dxcc_list"              -> lightbox(null);
            case "county_list"            -> lightbox(universeFor(plugin));
            case "worked_before"          -> note("Worked-before shows by the call field.");
            case "dupe_checker"           -> note("Dupe check shows by the call field.");
            case "qtc"                    -> note("WAE QTC entry — not yet in ars-fx.");
            default                       -> null;     // maps → Phase C
        };
        return inner == null ? null : card(title, inner);
    }

    // ---- score-driven lightbox -------------------------------------------
    private static Region lightbox(List<String> universe) {
        Lightbox box = new Lightbox();
        Runnable upd = () -> box.set(universe, allWorked(ContestState.score()));
        ContestState.addListener(() -> Platform.runLater(upd));
        upd.run();
        return box;
    }

    /** Union of every worked-multiplier collection so the chips light regardless of painter. */
    public static Set<String> allWorked(ContestScore s) {
        Set<String> w = new HashSet<>(s.worked());
        s.workedByMode().values().forEach(w::addAll);
        s.workedByBand().values().forEach(w::addAll);
        w.addAll(s.zonesWorked());
        return w;
    }

    private static List<String> universeFor(ContestPlugin p) {
        String list = p.getMultiplierList();
        if (list != null && !list.isBlank()) {
            try { List<String> u = MultiplierLists.load(list); if (u != null && !u.isEmpty()) return u; } catch (Exception ignored) {}
        }
        List<String> sections = p.getSections();
        return (sections != null && !sections.isEmpty()) ? sections : null;
    }

    // ---- statistics ------------------------------------------------------
    private static Region statsPane() {
        VBox v = new VBox(4);
        Runnable upd = () -> {
            ContestScore s = ContestState.score();
            v.getChildren().setAll(
                    kv("QSOs", String.valueOf(s.qsoCount())),
                    kv("Mults", String.valueOf(s.mults())),
                    kv("Points", String.valueOf(s.points())),
                    kv("Score", String.format("%,d", s.score())));
        };
        ContestState.addListener(() -> Platform.runLater(upd));
        upd.run();
        return v;
    }
    private static Node kv(String k, String val) {
        Label kl = lbl(k, "ct-kv-k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        HBox r = new HBox(8, kl, lbl(val, "ct-kv-v")); return r;
    }

    private static Region note(String text) {
        Label l = lbl(text, "ct-note"); l.setWrapText(true); l.setMaxWidth(220);
        return new VBox(l);
    }

    // ---- titled card -----------------------------------------------------
    private static Region card(String title, Region body) {
        VBox card = new VBox(7, lbl(title.toUpperCase(), "ct-card-h"), body);
        card.getStyleClass().add("ct-card");
        card.setPadding(new Insets(10, 12, 12, 12));
        card.setMinWidth(200); card.setPrefWidth(240);
        return card;
    }

    private static String prettify(String type) {
        String t = type.replace('_', ' ');
        return t.isEmpty() ? "Tracker" : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    /** Build all row-placement tracker cards for a plugin (null-safe, maps skipped). */
    public static List<Region> rowPanes(ContestPlugin plugin) { return panes(plugin, "row"); }
    /** Build all column-placement tracker cards for a plugin. */
    public static List<Region> columnPanes(ContestPlugin plugin) { return panes(plugin, "column"); }

    private static List<Region> panes(ContestPlugin plugin, String placement) {
        List<Region> out = new ArrayList<>();
        List<PaneDef> defs = plugin.getRow2Panes();
        if (defs == null) return out;
        for (PaneDef pd : defs) {
            if (!placement.equals(pd.getPlacement())) continue;
            Region r = paneFor(pd, plugin);
            if (r != null) out.add(r);
        }
        return out;
    }
}
