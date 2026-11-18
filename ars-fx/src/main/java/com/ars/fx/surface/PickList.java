package com.ars.fx.surface;

import com.ars.fx.data.QsoPartySide;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.ContestPlugin.FieldDef;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Type-ahead suggestions under an exchange field that takes a fixed list (a section, a county, a state or province): what is typed narrows the
 * list, and Down / Up then Enter or Tab picks one. Nothing is in the way otherwise: with no row chosen, Enter / Tab / Esc do what they always did.
 * The lists are the ones {@link ContestValidation} checks against.
 */
final class PickList {
    private PickList() {}

    static final int LIMIT = 8;

    /** The values {@code fd} accepts, or an empty list when it is free text (or not an exchange field). */
    static List<String> universe(ContestPlugin p, FieldDef fd) {
        String id = fd.getId();
        if (fd.getEntryRow() == 1 || "callsign".equals(id) || "band".equals(id) || "mode".equals(id)) return List.of();
        if (fd.getOptions() != null && !fd.getOptions().isEmpty()) return fd.getOptions();
        List<String> mv = ContestValidation.multiplierValues(p, id);
        if (mv != null && !mv.isEmpty()) return mv;
        if (ContestValidation.isStateField(p, id)) {
            QsoPartySide.Side side = QsoPartySide.isParty(p) ? QsoPartySide.side(p) : null;
            if (side != null) return new ArrayList<>(QsoPartySide.receivable(p, side));
            List<String> u = com.ars.fx.surface.contest.TrackerFactory.multiplierUniverse(p);
            if (u != null) return u;
        }
        return List.of();
    }

    /** Up to {@code limit} values for what is typed: those that start with it first, then those that contain it; none when it is empty or already complete. */
    static List<String> match(List<String> universe, String typed, int limit) {
        String t = typed == null ? "" : typed.trim().toUpperCase(Locale.ROOT);
        if (t.isEmpty() || universe == null || universe.isEmpty()) return List.of();
        Set<String> starts = new LinkedHashSet<>(), contains = new LinkedHashSet<>();
        for (String o : universe) {
            String u = o.trim().toUpperCase(Locale.ROOT);
            if (u.startsWith(t)) starts.add(o.trim()); else if (u.contains(t)) contains.add(o.trim());
        }
        List<String> out = new ArrayList<>(starts.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        out.addAll(contains.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        if (out.size() == 1 && out.get(0).equalsIgnoreCase(t)) return List.of();       // already what is typed
        return out.size() > limit ? new ArrayList<>(out.subList(0, limit)) : out;
    }

    /** Show suggestions under {@code field} as it is typed in. {@code universe} is read each time (the contest side can change). */
    static void attach(TextField field, java.util.function.Supplier<List<String>> universe) {
        ListView<String> lv = new ListView<>();
        lv.setFocusTraversable(false);
        lv.setPrefWidth(180);
        lv.getStyleClass().add("jl-picklist");
        Popup pop = new Popup();
        pop.setAutoHide(false);
        pop.getContent().add(lv);
        com.ars.fx.Theme.apply(pop.getScene(), false);
        boolean[] quiet = {false};                                                  // the text is being set by a pick, not typed
        Runnable accept = () -> {
            String v = lv.getSelectionModel().getSelectedItem();
            if (v == null) return;
            quiet[0] = true;
            try { field.setText(v); field.positionCaret(v.length()); } finally { quiet[0] = false; }
            pop.hide();
        };
        field.textProperty().addListener((o, a, b) -> {
            if (quiet[0]) return;
            List<String> m = field.isFocused() ? match(universe.get(), b, LIMIT) : List.of();
            if (m.isEmpty()) { pop.hide(); return; }
            lv.getItems().setAll(m);
            lv.getSelectionModel().clearSelection();
            lv.setPrefHeight(m.size() * 24 + 4);
            if (!pop.isShowing() && field.getScene() != null && field.getScene().getWindow() != null) {
                var bd = field.localToScreen(field.getBoundsInLocal());
                if (bd != null) pop.show(field, bd.getMinX(), bd.getMaxY());
            }
        });
        field.focusedProperty().addListener((o, was, now) -> { if (!now) pop.hide(); });
        lv.setOnMouseClicked(e -> accept.run());
        field.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!pop.isShowing()) return;
            int sel = lv.getSelectionModel().getSelectedIndex(), n = lv.getItems().size();
            switch (e.getCode()) {
                case DOWN -> { lv.getSelectionModel().select(Math.min(n - 1, sel + 1)); e.consume(); }
                case UP -> { if (sel > 0) lv.getSelectionModel().select(sel - 1); e.consume(); }
                case ENTER, TAB -> { if (sel >= 0) { accept.run(); e.consume(); } }
                case ESCAPE -> { pop.hide(); e.consume(); }
                default -> { }
            }
        });
    }
}
