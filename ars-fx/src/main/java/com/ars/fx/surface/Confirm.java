package com.ars.fx.surface;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A small on-brand confirm card overlaid on the current surface (same idiom as
 * the spot-click rotate prompt). Each {@link Choice} is a button; clicking it
 * closes the overlay and runs the action. Clicking the scrim dismisses.
 */
public final class Confirm {
    private Confirm() {}

    public record Choice(String label, boolean primary, Runnable action) {}

    public static void show(Node source, String title, String message, Choice... choices) {
        Scene sc = source == null ? null : source.getScene();
        if (sc == null || !(sc.getRoot() instanceof Pane root)) {
            for (Choice c : choices) if (c.primary() && c.action() != null) { c.action().run(); return; }
            return;
        }
        Label t = lbl(title, "jhub-card-title");
        Label m = lbl(message, "jl-emeta"); m.setWrapText(true); m.setMaxWidth(380);
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox btns = new HBox(10, gap); btns.setAlignment(Pos.CENTER_RIGHT);
        VBox card = new VBox(12, t, m, btns);
        card.setPadding(new Insets(20, 22, 18, 22)); card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color:-ars-surface-1;-fx-background-radius:12;"
                + "-fx-border-color:-ars-border;-fx-border-width:1;-fx-border-radius:12;");

        StackPane veil = new StackPane(card); veil.setMinSize(0, 0);
        veil.setStyle("-fx-background-color:rgba(6,9,13,0.55);");
        veil.prefWidthProperty().bind(root.widthProperty());
        veil.prefHeightProperty().bind(root.heightProperty());
        Runnable close = () -> root.getChildren().remove(veil);

        for (Choice c : choices) {
            Label b = lbl(c.label(), c.primary() ? "jl-logbtn" : "jl-clearbtn");
            b.setStyle("-fx-cursor:hand;-fx-padding:7 16;");
            b.setOnMouseClicked(e -> { close.run(); if (c.action() != null) c.action().run(); });
            btns.getChildren().add(b);
        }
        veil.setOnMouseClicked(e -> { if (e.getTarget() == veil) close.run(); });
        root.getChildren().add(veil);
    }
}
