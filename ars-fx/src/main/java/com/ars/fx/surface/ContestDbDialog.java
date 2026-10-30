package com.ars.fx.surface;

import com.ars.fx.data.ContestLogs;
import com.ars.fx.data.ContestLogs.Info;
import com.ars.fx.data.LogDbInfo;
import com.jlog.plugin.ContestPlugin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

import static com.ars.fx.shell.Shell.lbl;

/**
 * "Which database does this contest log to?" — continue one of the existing contest databases (each shown with the
 * contest it is for, the date it was started and its QSO count) or start a new one with an editable name.
 * Same overlay idiom as {@link Confirm}.
 */
final class ContestDbDialog {
    private ContestDbDialog() {}

    /** @param onChosen runs after a database is selected or created and the engine has switched to it
     *  @param onCancel runs when the dialog is dismissed without a choice (may be null) */
    static void show(Node source, ContestPlugin plugin, Runnable onChosen, Runnable onCancel) {
        Scene sc = source == null ? null : source.getScene();
        if (sc == null || !(sc.getRoot() instanceof Pane root) || plugin == null) { if (onCancel != null) onCancel.run(); return; }

        Label title = lbl("Contest database · " + plugin.getContestName(), "jhub-card-title");
        Label sub = lbl("QSOs for this contest are saved in the database you pick. Continue one you already have, or start a new one.", "jl-emeta");
        sub.setWrapText(true); sub.setMaxWidth(540);
        Label err = lbl("", "jl-emeta"); err.setStyle("-fx-text-fill:-ars-err;"); err.setWrapText(true); err.setMaxWidth(540);

        StackPane veil = new StackPane(); veil.setMinSize(0, 0);
        veil.setStyle("-fx-background-color:rgba(6,9,13,0.55);");
        veil.prefWidthProperty().bind(root.widthProperty());
        veil.prefHeightProperty().bind(root.heightProperty());
        Runnable close = () -> root.getChildren().remove(veil);
        boolean[] decided = {false};

        // ── existing databases (this contest's first) ───────────────────────
        List<Info> dbs = new ArrayList<>(ContestLogs.usable());
        dbs.sort((a, b) -> Boolean.compare(matches(b, plugin), matches(a, plugin)));
        Path last = ContestLogs.lastFor(plugin.getContestId());
        VBox rows = new VBox(6);
        for (Info i : dbs) {
            boolean same = matches(i, plugin);
            String tags = (i.current() ? "  · in use" : "") + (last != null && last.toAbsolutePath().equals(i.path().toAbsolutePath()) ? "  · last used for this contest" : "");
            Label nm = lbl(i.fileName() + tags, "jl-skim-cl");
            String what = i.contestName().isBlank() ? "no contest yet" : i.contestName();
            Label meta = lbl((same ? "★ " : "") + what + "  ·  started " + (i.started().isBlank() ? "unknown" : i.started())
                    + "  ·  " + i.qsos() + (i.qsos() == 1 ? " QSO" : " QSOs"), "jl-skim-meta");
            if (same) meta.setStyle("-fx-text-fill:-ars-accent;");
            VBox txt = new VBox(2, nm, meta);
            HBox row = new HBox(txt); row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-cursor:hand;-fx-background-color:-ars-surface-2;-fx-background-radius:8;-fx-border-color:"
                    + (same ? "-ars-accent" : "-ars-border") + ";-fx-border-width:1;-fx-border-radius:8;-fx-padding:9 12;");
            row.setOnMouseClicked(e -> {
                try { ContestLogs.use(i.path(), plugin.getContestId()); }
                catch (Exception ex) { err.setText("Could not open " + i.fileName() + ": " + ex.getMessage()); return; }
                decided[0] = true; close.run(); if (onChosen != null) onChosen.run();
            });
            rows.getChildren().add(row);
        }
        if (dbs.isEmpty()) rows.getChildren().add(lbl("No contest databases yet — start a new one below.", "jl-skim-meta"));
        ScrollPane sp = new ScrollPane(rows); sp.setFitToWidth(true); sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent;"); sp.setPrefViewportHeight(Math.min(230, 62.0 * Math.max(1, dbs.size())));
        sp.setMaxHeight(250);

        // ── new database ────────────────────────────────────────────────────
        TextField name = new TextField(ContestLogs.suggestName(plugin)); name.getStyleClass().add("jhub-mini-field");
        HBox.setHgrow(name, Priority.ALWAYS); name.setMaxWidth(Double.MAX_VALUE);
        Label create = lbl("Create & use", "jl-logbtn"); create.setStyle("-fx-cursor:hand;-fx-padding:7 16;");
        Runnable doCreate = () -> {
            try { ContestLogs.create(plugin, name.getText()); }
            catch (Exception ex) { err.setText(ex.getMessage()); return; }
            decided[0] = true; close.run(); if (onChosen != null) onChosen.run();
        };
        create.setOnMouseClicked(e -> doCreate.run());
        name.setOnAction(e -> doCreate.run());
        HBox newRow = new HBox(10, name, create); newRow.setAlignment(Pos.CENTER_LEFT);
        Label hint = lbl("Suggested name — change it as you like. Saved in " + LogDbInfo.pretty(ContestLogs.logsDir()) + "/", "jl-skim-meta");

        Label cancel = lbl("Cancel", "jl-clearbtn"); cancel.setStyle("-fx-cursor:hand;-fx-padding:7 16;");
        cancel.setOnMouseClicked(e -> { close.run(); if (!decided[0] && onCancel != null) onCancel.run(); });
        HBox bottom = new HBox(cancel); bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12, title, sub,
                lbl("CONTINUE AN EXISTING DATABASE", "jl-flabel"), sp,
                lbl("OR START A NEW ONE", "jl-flabel"), newRow, hint, err, bottom);
        card.setPadding(new Insets(20, 22, 18, 22)); card.setMaxSize(580, Region.USE_PREF_SIZE); card.setPrefWidth(580);
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-background-radius:12;"
                + "-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;");
        veil.getChildren().add(card);
        root.getChildren().add(veil);
        name.requestFocus(); name.selectAll();
    }

    private static boolean matches(Info i, ContestPlugin p) { return i.contestId().equals(p.getContestId()); }
}
