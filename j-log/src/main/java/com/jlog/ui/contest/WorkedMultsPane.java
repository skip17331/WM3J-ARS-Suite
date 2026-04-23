package com.jlog.ui.contest;

import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Flow of chip-labels showing every multiplier value actually worked in this
 * contest. Unlike {@link DxccListPane} — which ships a static list that the
 * controller then highlights — this pane is fully DB-driven: the chips ARE the
 * worked set. Useful for QSO parties and other contests where the multiplier
 * universe (counties, sections, etc.) isn't practical to pre-enumerate.
 */
public class WorkedMultsPane extends ScrollPane {

    private final Label    header = new Label("0 worked");
    private final FlowPane flow   = new FlowPane(4, 4);
    private final Map<String, Label> chips = new LinkedHashMap<>();
    private final List<String> universe = new ArrayList<>();
    private final Set<String>  worked   = new LinkedHashSet<>();
    private Consumer<String> onClicked = v -> {};

    public WorkedMultsPane() {
        header.getStyleClass().add("zone-header");
        flow.getStyleClass().add("mult-grid");
        VBox root = new VBox(4, header, flow);
        root.getStyleClass().add("pane-content");
        setContent(root);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
    }

    /** Seed the full multiplier universe (e.g. all counties in a state).
     *  When set, chips are always rendered — unworked in gray, worked in
     *  green. Without a universe, only worked entries are shown. */
    public void setUniverse(Collection<String> entries) {
        universe.clear();
        if (entries != null) {
            LinkedHashSet<String> dedup = new LinkedHashSet<>(entries);
            List<String> sorted = new ArrayList<>(dedup);
            Collections.sort(sorted);
            universe.addAll(sorted);
        }
        rebuildChips();
    }

    /** Mark the given values as worked (the rest are drawn as unworked when
     *  a universe was seeded; otherwise only the worked entries render). */
    public void setWorked(Collection<String> values) {
        worked.clear();
        if (values != null) for (String v : values) if (v != null) worked.add(v);
        rebuildChips();
    }

    private void rebuildChips() {
        flow.getChildren().clear();
        chips.clear();
        Collection<String> toRender;
        if (!universe.isEmpty()) {
            toRender = universe;
        } else {
            List<String> sorted = new ArrayList<>(worked);
            Collections.sort(sorted);
            toRender = sorted;
        }
        for (String v : toRender) {
            Label chip = new Label(v);
            boolean isWorked = caseInsensitiveContains(worked, v);
            chip.getStyleClass().add("mult-grid-cell");
            if (isWorked) chip.getStyleClass().add("mult-grid-cell-worked");
            chip.setCursor(Cursor.HAND);
            Tooltip tip = new Tooltip(isWorked ? v + " — worked" : v);
            tip.setShowDelay(Duration.millis(120));
            Tooltip.install(chip, tip);
            chip.setOnMouseClicked(ev -> onClicked.accept(v));
            flow.getChildren().add(chip);
            chips.put(v, chip);
        }
        int workedCount = 0;
        for (String v : toRender) if (caseInsensitiveContains(worked, v)) workedCount++;
        int total = toRender.size();
        header.setText(universe.isEmpty()
            ? (workedCount + " worked")
            : (workedCount + " / " + total + " worked"));
    }

    private static boolean caseInsensitiveContains(Set<String> set, String v) {
        if (v == null) return false;
        for (String s : set) if (v.equalsIgnoreCase(s)) return true;
        return false;
    }

    public void setOnMultClicked(Consumer<String> cb) {
        this.onClicked = cb != null ? cb : v -> {};
    }
}
