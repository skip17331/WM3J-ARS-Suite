package com.ars.fx.surface;

import com.ars.fx.data.DxccInfo;
import com.ars.fx.data.RefLookup;
import com.ars.fx.data.RefValidation;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Under the IOTA / SOTA / POTA fields of a QSO form: for each reference typed, one line saying what it is ("✓ POTA US-0001: Acadia · FN54vh") or
 * what is wrong with it, with the field itself outlined green / amber / red to match; and, while the IOTA field is empty, clickable ideas for it
 * taken from the other station's DXCC entity and grid. The checks are {@link RefValidation}'s: the shape is checked as you type, the lookups run in
 * the background a moment after you stop (or when you leave the field) and again when the call or grid changes. Nothing is ever filled in without a
 * click, and nothing is ever refused: {@link Attached#problems} is for the form to ask "save anyway?".
 */
final class RefHints {
    private RefHints() {}

    /** {@code problems} lists, right now, the references that are wrong (bad shape, or looked up and not known); empty when all is well or unknown. */
    record Attached(Node node, Supplier<List<String>> problems) {}

    private static final String[] LINE_CLASSES = {"ref-line-ok", "ref-line-warn", "ref-line-bad", "ref-line-unknown"};

    static Node attach(TextField call, TextField grid, TextField iota, TextField sota, TextField pota) {
        return attachChecked(call, grid, iota, sota, pota).node();
    }

    static Attached attachChecked(TextField call, TextField grid, TextField iota, TextField sota, TextField pota) {
        FlowPane ideas = new FlowPane(8, 4); ideas.setPadding(new Insets(0));
        ideas.managedProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(ideas.getChildren()));
        ideas.visibleProperty().bind(javafx.beans.binding.Bindings.isNotEmpty(ideas.getChildren()));

        Checker[] cs = {new Checker(RefLookup.Kind.POTA, pota, call, grid), new Checker(RefLookup.Kind.SOTA, sota, call, grid),
                        new Checker(RefLookup.Kind.IOTA, iota, call, grid)};
        iota.focusedProperty().addListener((o, was, now) -> { if (!now) ideas.getChildren().clear(); });
        call.focusedProperty().addListener((o, was, now) -> { if (!now) { suggest(ideas, call, grid, iota); recheck(cs); } });
        grid.focusedProperty().addListener((o, was, now) -> { if (!now) { suggest(ideas, call, grid, iota); recheck(cs); } });
        call.textProperty().addListener((o, a, b) -> { if (b == null || b.isBlank()) ideas.getChildren().clear(); });
        iota.textProperty().addListener((o, a, b) -> { if (b != null && !b.isBlank()) ideas.getChildren().clear(); });

        VBox box = new VBox(3);
        for (Checker c : cs) box.getChildren().add(c.line);
        box.getChildren().add(ideas);
        return new Attached(box, () -> {
            List<String> out = new ArrayList<>();
            for (Checker c : cs) { String p = c.problem(); if (p != null) out.add(p); }
            return out;
        });
    }

    /** The call or grid changed: the checks that depend on them (grid and entity sense) run again for the fields that hold a reference. */
    private static void recheck(Checker[] cs) {
        for (Checker c : cs) if (c.field.getText() != null && !c.field.getText().isBlank()) c.run();
    }

    /** The status of one field: its line, its outline, and the last answer. */
    private static final class Checker {
        final RefLookup.Kind kind; final TextField field, call, grid; final Label line = lbl("", "jl-emeta");
        private final PauseTransition settle = new PauseTransition(Duration.millis(700));
        private volatile RefValidation.Result last; private volatile String lastText = "";

        Checker(RefLookup.Kind kind, TextField field, TextField call, TextField grid) {
            this.kind = kind; this.field = field; this.call = call; this.grid = grid;
            line.setWrapText(true);
            line.managedProperty().bind(line.textProperty().isNotEmpty()); line.visibleProperty().bind(line.textProperty().isNotEmpty());
            settle.setOnFinished(e -> run());
            field.textProperty().addListener((o, a, b) -> {
                shapeNow();                                                  // the shape is instant; the lookup waits until typing pauses
                settle.playFromStart();
            });
            field.focusedProperty().addListener((o, was, now) -> { if (!now) { settle.stop(); run(); } });
        }

        private String text() { return field.getText() == null ? "" : field.getText(); }

        /** Shape only, no network: a bad shape is shown at once; otherwise a stale mark is cleared while the text is changing. */
        private void shapeNow() {
            RefValidation.Result s = RefValidation.shape(kind, text());
            if (s == null) { show(null); return; }
            if (s.level() == RefValidation.Level.BAD) show(s); else { last = null; line.setText(""); line.getStyleClass().removeAll(LINE_CLASSES); mark(null); }
        }

        /** The whole check, with the lookup on a background thread. */
        void run() {
            String text = text();
            RefValidation.Result s = RefValidation.shape(kind, text);
            if (s == null) { show(null); return; }
            if (s.level() == RefValidation.Level.BAD) { show(s); return; }
            last = null; mark(null); line.getStyleClass().removeAll(LINE_CLASSES);
            line.setText(kind + " " + RefLookup.normalise(text) + ": looking up…");
            String g = grid.getText() == null ? "" : grid.getText().trim();
            String c = call.getText() == null ? "" : call.getText().trim();
            Thread t = new Thread(() -> {
                DxccInfo.Info info = kind == RefLookup.Kind.IOTA && c.length() >= 3 ? DxccInfo.of(c) : null;
                RefValidation.Result r = RefValidation.validate(kind, text, new RefValidation.Context(g, info == null ? 0 : info.dxcc()));
                Platform.runLater(() -> { if (text.equals(text())) show(r); });    // an answer for text that has since changed is dropped
            }, "ref-check");
            t.setDaemon(true); t.start();
        }

        private void show(RefValidation.Result r) {
            last = r; lastText = text();
            line.getStyleClass().removeAll(LINE_CLASSES);
            if (r == null) { line.setText(""); mark(null); return; }
            String msg = r.message();
            switch (r.level()) {
                case OK -> { line.setText("✓ " + kind + " " + msg); line.getStyleClass().add("ref-line-ok"); mark("ref-ok"); }
                case WARN -> { line.setText("⚠ " + kind + " " + msg); line.getStyleClass().add("ref-line-warn"); mark("warnfield"); }
                case BAD -> { line.setText("✗ " + msg); line.getStyleClass().add("ref-line-bad"); mark("invalid"); }
                case UNKNOWN -> { line.setText("? " + kind + " " + msg); line.getStyleClass().add("ref-line-unknown"); mark(null); }
            }
        }

        private void mark(String cls) {
            field.getStyleClass().removeAll("ref-ok", "warnfield", "invalid");
            if (cls != null) field.getStyleClass().add(cls);
        }

        /** A message for the "save anyway?" prompt if this field is wrong right now, else null. The shape is always re-checked; the last lookup is trusted only while the text is unchanged. */
        String problem() {
            RefValidation.Result s = RefValidation.shape(kind, text());
            if (s == null) return null;
            if (s.level() == RefValidation.Level.BAD) return s.message();
            RefValidation.Result l = last;
            return l != null && l.level() == RefValidation.Level.BAD && text().equals(lastText) ? l.message() : null;
        }
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
