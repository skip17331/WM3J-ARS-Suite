package com.jlog.controller;

import com.jlog.model.DxSpot;
import com.jlog.model.PriorityCallsign;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * PriorityBanner — a transient toast overlay rendered on top of the
 * j-log main window when {@link PrioritySpotAlertService} fires an
 * alert. Wired by passing the main scene's root {@link StackPane} (or
 * by wrapping the scene's root in a StackPane at startup); the banner
 * floats top-centre, fades in, holds for 6 seconds, then fades out and
 * removes itself.
 *
 * <p>Multiple concurrent alerts stack vertically — each banner is
 * independent of the others.
 */
public final class PriorityBanner {

    private static final Duration FADE_IN  = Duration.millis(180);
    private static final Duration HOLD     = Duration.seconds(6);
    private static final Duration FADE_OUT = Duration.millis(450);

    /** Wrap a scene's existing root in a StackPane and register the
     *  banner as an alert listener. Idempotent — safe to call once per
     *  scene at startup. */
    public static void attach(Scene scene) {
        if (scene == null) return;
        StackPane overlay;
        if (scene.getRoot() instanceof StackPane sp && sp.getId() != null
            && sp.getId().equals("priority-banner-root")) {
            overlay = sp;
        } else {
            var original = scene.getRoot();
            overlay = new StackPane(original);
            overlay.setId("priority-banner-root");
            scene.setRoot(overlay);
        }
        // Top-centre column for stacking toasts
        VBox column = new VBox(6);
        column.setId("priority-banner-column");
        column.setPickOnBounds(false);
        column.setMouseTransparent(false);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPadding(new Insets(8, 0, 0, 0));
        column.setMaxHeight(Region.USE_PREF_SIZE);
        overlay.getChildren().add(column);
        StackPane.setAlignment(column, Pos.TOP_CENTER);

        PrioritySpotAlertService.getInstance().addListener(alert -> Platform.runLater(() -> {
            Region toast = buildToast(alert.spot, alert.entry);
            column.getChildren().add(toast);
            playLifecycle(toast, column);
        }));
    }

    // ---------------------------------------------------------------
    // Toast construction
    // ---------------------------------------------------------------

    private static Region buildToast(DxSpot spot, PriorityCallsign entry) {
        Label icon = new Label("🚨");
        icon.setStyle("-fx-font-size: 18px;");

        Label call = new Label(spot.getDxCallsign().toUpperCase());
        call.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f9e2af;");

        String freq = String.format("%.1f kHz", spot.getFrequencyKHz());
        String mode = spot.getMode() == null || spot.getMode().isBlank() ? "" : "  " + spot.getMode();
        Label detail = new Label(spot.getBand() + "  " + freq + mode);
        detail.setStyle("-fx-font-size: 13px; -fx-text-fill: #cdd6f4;");

        String note = entry.getNote() == null || entry.getNote().isBlank() ? "" : "  ·  " + entry.getNote();
        Label sub = new Label("de " + safeUpper(spot.getSpotter()) + note);
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #a6adc8;");

        VBox lines = new VBox(2, call, detail, sub);

        HBox row = new HBox(10, icon, lines);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 16, 10, 14));
        row.setStyle("-fx-background-color: rgba(20,28,46,0.95); " +
                     "-fx-border-color: #f9e2af; -fx-border-width: 2; " +
                     "-fx-background-radius: 6; -fx-border-radius: 6;");
        row.setCursor(Cursor.HAND);

        // Click to dismiss
        row.setOnMouseClicked(e -> {
            if (row.getParent() instanceof VBox parent) parent.getChildren().remove(row);
        });

        row.setOpacity(0);
        return row;
    }

    private static void playLifecycle(Region toast, VBox column) {
        FadeTransition fadeIn = new FadeTransition(FADE_IN, toast);
        fadeIn.setFromValue(0); fadeIn.setToValue(1.0);
        PauseTransition hold = new PauseTransition(HOLD);
        FadeTransition fadeOut = new FadeTransition(FADE_OUT, toast);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0);
        SequentialTransition seq = new SequentialTransition(fadeIn, hold, fadeOut);
        seq.setOnFinished(e -> column.getChildren().remove(toast));
        seq.play();
    }

    private static String safeUpper(String s) {
        return s == null ? "?" : s.toUpperCase();
    }

    private PriorityBanner() {}
}
