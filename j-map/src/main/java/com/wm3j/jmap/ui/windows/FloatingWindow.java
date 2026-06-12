package com.wm3j.jmap.ui.windows;

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

    /** Frame-only style (colors, borders); font-size is appended separately so
     *  subclasses can flip frame state (e.g. countdown flash) without losing
     *  the per-window font-size override. */
    private String frameStyle = "-fx-background-color: #11161d; -fx-border-color: #323b47; -fx-border-width: 1;";
    private int    baseFontSizePx = 0;   // 0 = inherit from scene root

    public void setOnPositionSaved(Runnable callback) { this.onPositionSaved = callback; }

    /** Set the base font size for this window in px. Children using em-relative
     *  sizes scale from here. Pass 0 to inherit from the scene root. */
    public void setBaseFontSize(int px) {
        this.baseFontSizePx = px;
        refreshFrameStyle();
    }

    /** Subclasses use this instead of {@code setStyle(...)} when they need to
     *  flip the frame colors transiently (e.g. the countdown flash states),
     *  so the per-window font-size override is preserved. */
    protected void setFrameStyle(String s) {
        this.frameStyle = s;
        refreshFrameStyle();
    }

    private void refreshFrameStyle() {
        String full = frameStyle;
        if (baseFontSizePx > 0) full += " -fx-font-size: " + baseFontSizePx + "px;";
        setStyle(full);
    }

    public FloatingWindow(String title, double prefWidth) {
        refreshFrameStyle();
        setPrefWidth(prefWidth);
        setMaxWidth(prefWidth);

        // ── Title bar ──────────────────────────────────
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #1c2530; -fx-border-color: #323b47 #323b47 #11161d #323b47;");
        titleBar.setPadding(new Insets(5, 8, 5, 8));
        titleBar.setSpacing(6);
        titleBar.setCursor(Cursor.MOVE);

        titleLabel = new Label(title);
        // em-based: 0.85em ≈ 11px at 13px base
        titleLabel.setStyle("-fx-font-size: 0.85em; -fx-font-weight: bold; -fx-text-fill: #ffd700;");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label closeBtn = new Label("✕");
        closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #6f7d8c; -fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> setVisible(false));
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #ff4455; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #6f7d8c; -fx-cursor: hand;"));

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
