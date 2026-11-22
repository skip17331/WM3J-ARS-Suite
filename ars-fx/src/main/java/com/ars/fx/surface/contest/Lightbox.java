package com.ars.fx.surface.contest;

import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
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
        BOXES.add(new java.lang.ref.WeakReference<>(this));
    }

    private static final List<java.lang.ref.WeakReference<Lightbox>> BOXES = new ArrayList<>();

    /** Blink the chip for {@code code} in every grid that has one (the section the operator just typed), so it can be found at a glance. */
    public static void flash(String code) {
        if (code == null || code.isBlank()) return;
        BOXES.removeIf(r -> r.get() == null);
        for (java.lang.ref.WeakReference<Lightbox> r : new ArrayList<>(BOXES)) { Lightbox b = r.get(); if (b != null) b.blink(code.trim()); }
    }

    private void blink(String code) {
        List<Label> hits = new ArrayList<>();
        collectChips(this, code, hits);
        for (Label l : hits) {
            javafx.animation.Timeline t = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(180), e -> {
                if (l.getStyleClass().contains("blink")) l.getStyleClass().remove("blink"); else l.getStyleClass().add("blink");
            }));
            t.setCycleCount(8);                                   // on, off, on, off ... ends off
            t.setOnFinished(e -> l.getStyleClass().remove("blink"));
            t.play();
        }
    }

    private static void collectChips(javafx.scene.Parent p, String code, List<Label> out) {
        for (javafx.scene.Node n : p.getChildrenUnmodifiable()) {
            if (n instanceof Label l && l.getStyleClass().contains("ct-chip") && code.equalsIgnoreCase(l.getText())) out.add(l);
            else if (n instanceof javafx.scene.Parent c) collectChips(c, code, out);
        }
    }

    public Lightbox groups(java.util.Map<String, List<String>> g) { this.groups = g == null || g.isEmpty() ? null : g; return this; }

    public void set(List<String> universe, Collection<String> worked) {
        Set<String> w = new HashSet<>();
        if (worked != null) for (String s : worked) if (s != null && !s.isBlank()) w.add(s.trim().toUpperCase());
        flow.getChildren().clear();
        getChildren().removeIf(n -> n instanceof VBox);
        flow.setVisible(true); flow.setManaged(true);
        if (groups != null && universe != null && !universe.isEmpty()) {
            FlowPane boxes = new FlowPane(8, 8);               // one small box for each group, headed by its number (W7 -> 7)
            VBox rows = new VBox(boxes);
            int done = 0;
            for (var e : groups.entrySet()) {
                int n = 0; FlowPane fp = new FlowPane(4, 4); fp.setPrefWrapLength(150);
                for (String u : e.getValue()) { boolean lit = w.contains(u.trim().toUpperCase()); if (lit) n++; fp.getChildren().add(chip(u, lit)); }
                done += n;
                java.util.regex.Matcher dm = java.util.regex.Pattern.compile("(\\d+)").matcher(e.getKey());
                Label num = lbl(dm.find() ? dm.group(1) : e.getKey(), "ct-group-n");
                Label cnt = lbl(n + "/" + e.getValue().size(), "ct-box-h");
                if (n == e.getValue().size()) { num.getStyleClass().add("lit"); cnt.getStyleClass().add("lit"); }
                HBox gh = new HBox(8, num, cnt); gh.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                VBox gb = new VBox(4, gh, fp); gb.getStyleClass().add("ct-group");
                boxes.getChildren().add(gb);
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
        Label l = lbl(t, "ct-chip"); if (lit) l.getStyleClass().add("lit");
        String name = com.ars.fx.data.SectionNames.nameOf(t);                 // IA -> Iowa
        if (name != null) com.ars.fx.shell.Tips.tip(l, name);
        return l;
    }
}
