package com.ars.fx.shell;

import com.ars.fx.AppVersion;
import com.ars.fx.data.UpdateChecker;
import com.ars.fx.data.UpdateChecker.Release;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A small "update available" card in the bottom-right corner of the docked window. It is an overlay on the
 * host {@link StackPane}, which {@code Launcher.show()} rebuilds on every navigation — so call
 * {@link #attach} after each rebuild to put the card back.
 */
public final class UpdateBanner {
    private UpdateBanner() {}

    private static HostServices hostServices;
    private static StackPane host;
    private static volatile Release shown;
    private static VBox card;

    /** Start listening for update results. Call once, after the first window is up. */
    public static void install(StackPane hostPane, HostServices hs) {
        host = hostPane; hostServices = hs;
        UpdateChecker.addListener(r -> Platform.runLater(() -> { shown = r; card = build(r); attach(host); }));
    }

    /** Re-add the card (if an update is pending) after the host's children were replaced. */
    public static void attach(StackPane hostPane) {
        if (card == null || hostPane == null || hostPane.getChildren().contains(card)) return;
        hostPane.getChildren().add(card);
    }

    private static void dismiss() {
        if (host != null && card != null) host.getChildren().remove(card);
        card = null; shown = null;
    }

    private static VBox build(Release r) {
        Label title = lbl("Update available — " + r.tag(), "ub-title");
        Label sub = lbl("You have v" + AppVersion.current() + ".", "ub-sub");
        Label notes = lbl("What's new", "ub-link"); notes.setStyle("-fx-cursor:hand;");
        notes.setOnMouseClicked(e -> { if (hostServices != null && !r.url().isEmpty()) hostServices.showDocument(r.url()); });
        Label skip = lbl("Skip this version", "ub-link"); skip.setStyle("-fx-cursor:hand;");
        skip.setOnMouseClicked(e -> { UpdateChecker.skip(r); dismiss(); });
        Label later = lbl("Later", "ub-link"); later.setStyle("-fx-cursor:hand;");
        later.setOnMouseClicked(e -> dismiss());
        HBox actions = new HBox(14, notes, skip, later); actions.setAlignment(Pos.CENTER_LEFT);
        VBox c = new VBox(4, title, sub, actions);
        c.getStyleClass().add("ub-card");
        c.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(c, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(c, new Insets(0, 16, 16, 0));
        return c;
    }
}
