package com.jlog.ui.contest;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.*;

/**
 * Compact matrix of multipliers × modes. Each cell turns green once its
 * multiplier has been worked on that mode. Used by the ARRL 10M cockpit
 * (CW / Phone columns) but generic over any mode set.
 */
public class PerModeMultGridPane extends GridPane {

    private final List<String> modes;
    private final List<String> mults;
    private final Map<String, Label> cells = new HashMap<>();

    public PerModeMultGridPane(List<String> modes, List<String> mults) {
        this.modes = List.copyOf(modes);
        this.mults = List.copyOf(mults);
        getStyleClass().add("mult-grid");

        Label corner = new Label("");
        corner.getStyleClass().add("mult-grid-header");
        add(corner, 0, 0);
        for (int i = 0; i < modes.size(); i++) {
            Label h = new Label(modes.get(i));
            h.getStyleClass().add("mult-grid-header");
            add(h, i + 1, 0);
        }

        for (int r = 0; r < mults.size(); r++) {
            Label name = new Label(mults.get(r));
            name.getStyleClass().add("mult-grid-header");
            add(name, 0, r + 1);
            for (int c = 0; c < modes.size(); c++) {
                Label cell = new Label("");
                cell.getStyleClass().add("mult-grid-cell");
                add(cell, c + 1, r + 1);
                cells.put(cellKey(modes.get(c), mults.get(r)), cell);
            }
        }
    }

    public void setWorked(String mode, String mult, boolean worked) {
        Label cell = cells.get(cellKey(mode, mult));
        if (cell == null) return;
        if (worked) {
            cell.setText("✔");
            if (!cell.getStyleClass().contains("mult-grid-cell-worked"))
                cell.getStyleClass().add("mult-grid-cell-worked");
        } else {
            cell.setText("");
            cell.getStyleClass().remove("mult-grid-cell-worked");
        }
    }

    public void clearMode(String mode) {
        for (String m : mults) setWorked(mode, m, false);
    }

    public List<String> getModes() { return modes; }
    public List<String> getMults() { return mults; }

    private static String cellKey(String mode, String mult) { return mode + "|" + mult; }
}
