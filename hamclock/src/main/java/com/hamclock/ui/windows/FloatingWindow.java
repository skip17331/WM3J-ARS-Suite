package com.hamclock.ui.windows;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

/**
 * Base class for all draggable floating windows overlaid on the map.
 *
 * All font sizes use em units — scales automatically with the root font size setting.
 */
public abstract class FloatingWindow extends VBox {

    private double dragStartX, dragStartY;
    private Runnable onPositionSaved;

    protected final Label titleLabel;
    protected final VBox  contentBox;

    public void setOnPositionSaved(Runnable callback) { this.onPositionSaved = callback; }

    public FloatingWindow(String title, double prefWidth) {
        setStyle("-fx-background-color: #080c1a; -fx-border-color: #1e2d50; -fx-border-width: 1;");
        setPrefWidth(prefWidth);
        setMaxWidth(prefWidth);

        // ── Title bar ──────────────────────────────────
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #0d1530; -fx-border-color: #1e2d50 #1e2d50 #0a1020 #1e2d50;");
        titleBar.setPadding(new Insets(5, 8, 5, 8));
        titleBar.setSpacing(6);
        titleBar.setCursor(Cursor.MOVE);

        titleLabel = new Label(title);
        // em-based: 0.85em ≈ 11px at 13px base
        titleLabel.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: #ffd700;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #4a5580; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> setVisible(false));
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #ff4455; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #4a5580; -fx-cursor: hand;"));

        titleBar.getChildren().addAll(titleLabel, closeBtn);

        // ── Content area ───────────────────────────────
        contentBox = new VBox(4);
        contentBox.setPadding(new Insets(8));

        getChildren().addAll(titleBar, contentBox);

        // ── Drag behavior ──────────────────────────────
        titleBar.setOnMousePressed(e -> {
            dragStartX = e.getSceneX() - getLayoutX();
            dragStartY = e.getSceneY() - getLayoutY();
        });
        titleBar.setOnMouseDragged(e -> {
            double newX = e.getSceneX() - dragStartX;
            double newY = e.getSceneY() - dragStartY;
            if (getParent() instanceof Pane parent) {
                newX = Math.max(0, Math.min(newX, parent.getWidth()  - getPrefWidth()));
                newY = Math.max(0, Math.min(newY, parent.getHeight() - getHeight()));
            }
            setLayoutX(newX);
            setLayoutY(newY);
        });
        titleBar.setOnMouseReleased(e -> {
            if (onPositionSaved != null) onPositionSaved.run();
        });
    }

    /** Called by the animation loop to refresh displayed data. */
    public abstract void update();
}
