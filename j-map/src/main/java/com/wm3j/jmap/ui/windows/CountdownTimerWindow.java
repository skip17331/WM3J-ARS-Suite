package com.wm3j.jmap.ui.windows;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * 10:00 countdown timer — counts down, flashes at zero, auto-restarts.
 * Font sizes use em so they scale with the root font size setting.
 */
public class CountdownTimerWindow extends FloatingWindow {

    private static final int TOTAL_SECONDS = 600;
    private static final int FLASH_CYCLES  = 12;   // 3 seconds × 4 toggles/sec

    private int     remainingSeconds = TOTAL_SECONDS;
    private boolean flashing    = false;
    private boolean flashVisible = true;
    private int     flashCount  = 0;

    private final Label timeDisplay;
    private final Label statusLabel;
    private Timeline flashTimeline;

    public CountdownTimerWindow() {
        super("⏱  ID TIMER", 180);

        timeDisplay = new Label("10:00");
        // 2.77em ≈ 36px at 13px base — large, readable display
        timeDisplay.setStyle("-fx-font-size: 2.77em; -fx-font-weight: bold; -fx-text-fill: #00cc66; -fx-alignment: center;");
        timeDisplay.setMaxWidth(Double.MAX_VALUE);

        statusLabel = new Label("RUNNING");
        statusLabel.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #4a5580; -fx-alignment: center;");
        statusLabel.setMaxWidth(Double.MAX_VALUE);

        contentBox.setStyle("-fx-alignment: center;");
        contentBox.getChildren().addAll(timeDisplay, statusLabel);
    }

    @Override
    public void update() {
        if (flashing) return;

        if (remainingSeconds > 0) {
            remainingSeconds--;
            renderTime();
        }

        if (remainingSeconds == 0) startFlash();
    }

    private void renderTime() {
        int m = remainingSeconds / 60;
        int s = remainingSeconds % 60;
        timeDisplay.setText(String.format("%d:%02d", m, s));

        String color;
        if (remainingSeconds > 60)      color = "#00cc66";
        else if (remainingSeconds > 10) color = "#ffcc00";
        else                            color = "#ff4455";

        timeDisplay.setStyle("-fx-font-size: 2.77em; -fx-font-weight: bold; -fx-text-fill: " + color + "; -fx-alignment: center;");
    }

    private void startFlash() {
        flashing   = true;
        flashCount = 0;
        statusLabel.setText("ZERO!");

        flashTimeline = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            flashVisible = !flashVisible;
            if (flashVisible) {
                timeDisplay.setText("0:00");
                timeDisplay.setStyle("-fx-font-size: 2.77em; -fx-font-weight: bold; -fx-text-fill: #ff4455; -fx-alignment: center;");
                setStyle("-fx-background-color: #1a0505; -fx-border-color: #ff4455; -fx-border-width: 2;");
            } else {
                timeDisplay.setStyle("-fx-font-size: 2.77em; -fx-font-weight: bold; -fx-text-fill: transparent; -fx-alignment: center;");
                setStyle("-fx-background-color: #080c1a; -fx-border-color: #1e2d50; -fx-border-width: 1;");
            }

            if (++flashCount >= FLASH_CYCLES) stopFlash();
        }));
        flashTimeline.setCycleCount(Timeline.INDEFINITE);
        flashTimeline.play();
    }

    private void stopFlash() {
        if (flashTimeline != null) flashTimeline.stop();
        flashing         = false;
        flashVisible     = true;
        remainingSeconds = TOTAL_SECONDS;
        statusLabel.setText("RUNNING");
        setStyle("-fx-background-color: #080c1a; -fx-border-color: #1e2d50; -fx-border-width: 1;");
        renderTime();
    }

    public void reset() {
        if (flashTimeline != null) flashTimeline.stop();
        flashing         = false;
        remainingSeconds = TOTAL_SECONDS;
        statusLabel.setText("RUNNING");
        setStyle("-fx-background-color: #080c1a; -fx-border-color: #1e2d50; -fx-border-width: 1;");
        renderTime();
    }
}
