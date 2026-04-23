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
 * Per-band list of worked 4-character Maidenhead grids. Grids aren't
 * pre-enumerable (tens of thousands possible), so only worked grids are
 * displayed — they become the multiplier set dynamically. Each grid is a
 * small clickable chip; clicking emits the grid id so the caller can fill
 * the entry field (useful for re-logging or scrolling through worked mults).
 *
 * Per-band scoring is reflected by grouping: each band gets a header and a
 * wrapping FlowPane of chips. A "current" grid highlights in blue outline.
 */
public class WorkedGridsPane extends ScrollPane {

    private final VBox root = new VBox(6);
    private final Map<String, VBox>     bandBoxes  = new LinkedHashMap<>();
    private final Map<String, FlowPane> bandFlows  = new LinkedHashMap<>();
    private final Map<String, Label>    bandHeads  = new LinkedHashMap<>();
    // (band, grid) → chip
    private final Map<String, Map<String, Label>> chips = new LinkedHashMap<>();
    private String currentBand;
    private String currentGrid;
    private Consumer<String> onGridClicked = g -> {};

    public WorkedGridsPane(List<String> bands) {
        root.getStyleClass().add("pane-content");
        setContent(root);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        for (String b : bands) ensureBand(b);
    }

    private VBox ensureBand(String band) {
        return bandBoxes.computeIfAbsent(band, b -> {
            Label head = new Label(b + "  (0)");
            head.getStyleClass().add("zone-header");
            FlowPane flow = new FlowPane(4, 4);
            flow.getStyleClass().add("grid-flow");
            VBox box = new VBox(2, head, flow);
            bandFlows.put(b, flow);
            bandHeads.put(b, head);
            chips.put(b, new LinkedHashMap<>());
            root.getChildren().add(box);
            return box;
        });
    }

    /** Replace the worked set for a band. */
    public void setWorked(String band, Collection<String> grids) {
        ensureBand(band);
        FlowPane flow = bandFlows.get(band);
        Map<String, Label> bandChips = chips.get(band);
        List<String> sorted = new ArrayList<>(new LinkedHashSet<>(grids));
        Collections.sort(sorted);
        flow.getChildren().clear();
        bandChips.clear();
        for (String g : sorted) {
            Label chip = new Label(g);
            chip.getStyleClass().addAll("region-tile-label", "mult-grid-cell", "mult-grid-cell-worked");
            chip.setCursor(Cursor.HAND);
            Tooltip tip = new Tooltip(g + " on " + band);
            tip.setShowDelay(Duration.millis(150));
            Tooltip.install(chip, tip);
            chip.setOnMouseClicked(ev -> onGridClicked.accept(g));
            flow.getChildren().add(chip);
            bandChips.put(g, chip);
        }
        bandHeads.get(band).setText(band + "  (" + sorted.size() + ")");
        restyleCurrent();
    }

    public void setCurrent(String band, String grid) {
        this.currentBand = band;
        this.currentGrid = grid;
        restyleCurrent();
    }

    private void restyleCurrent() {
        chips.forEach((band, byGrid) -> byGrid.forEach((g, lbl) -> {
            boolean current = band.equals(currentBand) && g.equalsIgnoreCase(currentGrid);
            lbl.getStyleClass().remove("region-tile-current");
            if (current) lbl.getStyleClass().add("region-tile-current");
        }));
    }

    public void setOnGridClicked(Consumer<String> cb) {
        this.onGridClicked = cb != null ? cb : g -> {};
    }
}
