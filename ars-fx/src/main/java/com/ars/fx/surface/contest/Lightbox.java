package com.ars.fx.surface.contest;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A grid of multiplier chips that "light up" as they're worked. With a fixed
 * universe (e.g. ARRL sections) every chip shows and worked ones light; without
 * one (open multiplier sets like DXCC) only worked chips are shown.
 */
public final class Lightbox extends VBox {

    private final Label header = lbl("", "ct-box-h");
    private final FlowPane flow = new FlowPane(5, 5);

    /** Optional grouping (call area -> sections): the universe is then shown one row per group. */
    private java.util.Map<String, List<String>> groups;

    public Lightbox() {
        super(6);
        getChildren().addAll(header, flow);
    }

    public Lightbox groups(java.util.Map<String, List<String>> g) { this.groups = g == null || g.isEmpty() ? null : g; return this; }

    public void set(List<String> universe, Collection<String> worked) {
        Set<String> w = new HashSet<>();
        if (worked != null) for (String s : worked) if (s != null && !s.isBlank()) w.add(s.trim().toUpperCase());
        flow.getChildren().clear();
        getChildren().removeIf(n -> n instanceof VBox);
        flow.setVisible(true); flow.setManaged(true);
        if (groups != null && universe != null && !universe.isEmpty()) {
            VBox rows = new VBox(6);
            int done = 0;
            for (var e : groups.entrySet()) {
                int n = 0; FlowPane fp = new FlowPane(5, 5);
                for (String u : e.getValue()) { boolean lit = w.contains(u.trim().toUpperCase()); if (lit) n++; fp.getChildren().add(chip(u, lit)); }
                done += n;
                Label h = lbl(e.getKey() + "  " + n + "/" + e.getValue().size(), "ct-box-h");
                if (n == e.getValue().size()) h.getStyleClass().add("lit");
                rows.getChildren().addAll(h, fp);
            }
            flow.setVisible(false); flow.setManaged(false);
            getChildren().add(rows);
            header.setText(done + " / " + universe.size() + " worked");
        } else if (universe != null && !universe.isEmpty()) {
            for (String u : universe) flow.getChildren().add(chip(u, w.contains(u.trim().toUpperCase())));
            header.setText(w.size() + " / " + universe.size() + " worked");
        } else {
            List<String> ws = new ArrayList<>(w); Collections.sort(ws);
            for (String u : ws) flow.getChildren().add(chip(u, true));
            header.setText(w.size() + " worked");
        }
    }

    private static Label chip(String t, boolean lit) {
        Label l = lbl(t, "ct-chip"); if (lit) l.getStyleClass().add("lit"); return l;
    }
}
