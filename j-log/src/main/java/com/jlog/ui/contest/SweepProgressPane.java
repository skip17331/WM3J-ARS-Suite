package com.jlog.ui.contest;

import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Shows Sweepstakes "clean sweep" progress — worked sections out of the total
 * available. Turns gold when the target is reached.
 */
public class SweepProgressPane extends VBox {

    private final Label       countLabel = new Label("0 / 0");
    private final ProgressBar bar        = new ProgressBar(0.0);
    private int total;

    public SweepProgressPane(int totalSections) {
        this.total = totalSections;
        getStyleClass().addAll("pane-content", "sweep-progress");
        countLabel.getStyleClass().add("stat-value");
        bar.getStyleClass().add("sweep-bar");
        bar.setPrefWidth(180);
        setSpacing(4);
        getChildren().addAll(countLabel, bar);
        updateCount(0);
    }

    public void setTotal(int total) {
        this.total = Math.max(0, total);
        updateCount(Math.min(currentWorked(), this.total));
    }

    public void setWorked(int worked) {
        updateCount(Math.min(Math.max(0, worked), total));
    }

    private int currentWorked() {
        // Parse back from label — cheap and avoids extra state
        try { return Integer.parseInt(countLabel.getText().split("/")[0].trim()); }
        catch (Exception e) { return 0; }
    }

    private void updateCount(int worked) {
        countLabel.setText(worked + " / " + total);
        bar.setProgress(total == 0 ? 0 : (double) worked / total);
        boolean done = worked >= total && total > 0;
        countLabel.getStyleClass().remove("sweep-complete");
        bar.getStyleClass().remove("sweep-complete");
        if (done) {
            countLabel.getStyleClass().add("sweep-complete");
            bar.getStyleClass().add("sweep-complete");
            countLabel.setText("✦ CLEAN SWEEP ✦  " + worked + " / " + total);
        }
    }
}
