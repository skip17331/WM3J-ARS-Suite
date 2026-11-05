package com.ars.fx.surface;

import com.ars.fx.data.DxccInfo;
import com.ars.fx.data.RefLookup;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Under the IOTA / SOTA / POTA fields of a QSO form: the name of the reference just typed ("POTA US-0001: Acadia"), and, while
 * the IOTA field is empty, clickable ideas for it taken from the other station's DXCC entity and grid. Lookups run in the
 * background when a field is left; nothing is ever filled in without a click.
 */
final class RefHints {
    private RefHints() {}

    static Node attach(TextField call, TextField grid, TextField iota, TextField sota, TextField pota) {
        Label info = lbl("", "jl-emeta"); info.setWrapText(true);
        info.managedProperty().bind(info.textProperty().isNotEmpty()); info.visibleProperty().bind(info.textProperty().isNotEmpty());
        FlowPane ideas = new FlowPane(8, 4); ideas.setPadding(new Insets(0));
        ideas.managedProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(ideas.getChildren()));
        ideas.visibleProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(ideas.getChildren()));

        pota.focusedProperty().addListener((o, was, now) -> { if (!now) name(info, "POTA", pota.getText(), RefLookup::looksLikePota, RefLookup::pota, "a park reference looks like US-0001"); });
        sota.focusedProperty().addListener((o, was, now) -> { if (!now) name(info, "SOTA", sota.getText(), RefLookup::looksLikeSota, RefLookup::sota, "a summit reference looks like W7W/LC-001"); });
        iota.focusedProperty().addListener((o, was, now) -> { if (!now) { name(info, "IOTA", iota.getText(), RefLookup::looksLikeIota, RefLookup::iota, "an island reference looks like NA-005"); ideas.getChildren().clear(); } });
        call.focusedProperty().addListener((o, was, now) -> { if (!now) suggest(ideas, call, grid, iota); });
        grid.focusedProperty().addListener((o, was, now) -> { if (!now) suggest(ideas, call, grid, iota); });
        call.textProperty().addListener((o, a, b) -> { if (b == null || b.isBlank()) { info.setText(""); ideas.getChildren().clear(); } });
        iota.textProperty().addListener((o, a, b) -> { if (b != null && !b.isBlank()) ideas.getChildren().clear(); });
        return new VBox(4, info, ideas);
    }

    /** Look the reference up (off the FX thread) and show its name, or say it was not found. */
    private static void name(Label info, String what, String text, java.util.function.Predicate<String> looksRight,
                             java.util.function.Function<String, RefLookup.Ref> lookup, String shape) {
        String ref = RefLookup.normalise(text);
        if (ref.isEmpty()) { info.setText(""); return; }
        if (!looksRight.test(ref)) { info.setText(what + " " + ref + ": not a " + what + " reference (" + shape + ")"); return; }
        info.setText(what + " " + ref + ": looking up…");
        Thread t = new Thread(() -> {
            RefLookup.Ref r = lookup.apply(ref);
            String msg = r == null ? what + " " + ref + ": not found (or the lookup is not reachable)"
                    : what + " " + ref + ": " + r.name() + (r.grid().isEmpty() ? "" : "  ·  " + r.grid()) + (r.detail().isEmpty() ? "" : "  ·  " + r.detail());
            Platform.runLater(() -> info.setText(msg));
        }, "ref-lookup");
        t.setDaemon(true); t.start();
    }

    /** While the IOTA field is empty: groups for the call's entity (and grid), as small links. */
    private static void suggest(FlowPane ideas, TextField call, TextField grid, TextField iota) {
        String c = call.getText() == null ? "" : call.getText().trim();
        if (c.length() < 3 || (iota.getText() != null && !iota.getText().isBlank())) { ideas.getChildren().clear(); return; }
        String g = grid.getText() == null ? "" : grid.getText().trim();
        Thread t = new Thread(() -> {
            DxccInfo.Info info = DxccInfo.of(c);
            List<RefLookup.IotaGroup> groups = info == null ? List.of() : RefLookup.suggestIota(info.dxcc(), g, 8);
            Platform.runLater(() -> {
                ideas.getChildren().clear();
                if (groups.isEmpty() || (iota.getText() != null && !iota.getText().isBlank())) return;
                ideas.getChildren().add(lbl("IOTA?", "jl-flabel"));
                for (RefLookup.IotaGroup grp : groups) {
                    Label l = lbl(grp.ref(), "jl-clearbtn"); l.setStyle("-fx-cursor:hand;");
                    com.ars.fx.shell.Tips.tip(l, grp.name() + " - click to use");
                    l.setOnMouseClicked(e -> { iota.setText(grp.ref()); ideas.getChildren().clear(); });
                    ideas.getChildren().add(l);
                }
            });
        }, "iota-ideas");
        t.setDaemon(true); t.start();
    }
}
