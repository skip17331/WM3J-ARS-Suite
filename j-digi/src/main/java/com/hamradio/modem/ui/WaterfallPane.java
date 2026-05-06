package com.hamradio.modem.ui;

import com.hamradio.modem.model.SignalSnapshot;
import javafx.animation.PauseTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleConsumer;

public class WaterfallPane extends Region {
    private static final int MAX_LINES = 420;

    private final Canvas canvas = new Canvas(600, 320);
    private final Deque<double[]> lines = new ArrayDeque<>();

    /** Audio Nyquist used to map clickedX → Hz. Defaults to 4000 Hz so an
     *  8 kHz capture pipeline works without configuration; override via
     *  {@link #setNyquistHz(double)} for other rates. */
    private double nyquistHz = 4000.0;

    /** Optional click callback — receives the clicked frequency in Hz. */
    private DoubleConsumer onFrequencyClicked;

    /** Set when an operator has clicked; the marker stays painted on the
     *  waterfall for a few seconds so the result-toast and the bar visually
     *  line up. -1 = nothing to draw. */
    private double markerHz = -1;
    private String markerLabel = "";
    private final PauseTransition markerFade = new PauseTransition(Duration.seconds(4));

    public WaterfallPane() {
        getChildren().add(canvas);
        setPrefSize(600, 320);
        markerFade.setOnFinished(e -> { markerHz = -1; markerLabel = ""; redraw(); });

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleClick);
    }

    /** Notify the controller of clicked-frequency events (in Hz). */
    public void setOnFrequencyClicked(DoubleConsumer cb) { this.onFrequencyClicked = cb; }

    /** Override the audio Nyquist used for x → Hz mapping. */
    public void setNyquistHz(double hz) { this.nyquistHz = Math.max(1.0, hz); }

    /** Show {@code label} next to the click marker for the duration of the
     *  current marker fade. Used by the click-to-identify flow to display
     *  the classifier result inline with the operator's selection. */
    public void setMarkerLabel(String label) {
        this.markerLabel = label == null ? "" : label;
        redraw();
    }

    private void handleClick(MouseEvent e) {
        double w = canvas.getWidth();
        if (w <= 0) return;
        double freqHz = (e.getX() / w) * nyquistHz;
        markerHz = freqHz;
        markerFade.playFromStart();
        redraw();
        if (onFrequencyClicked != null) onFrequencyClicked.accept(freqHz);
    }

    public void update(SignalSnapshot snapshot) {
        if (snapshot == null || snapshot.getMagnitudes() == null) {
            return;
        }

        lines.addLast(snapshot.getMagnitudes().clone());
        while (lines.size() > MAX_LINES) {
            lines.removeFirst();
        }
        redraw();
    }

    private void redraw() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.rgb(5, 6, 10));
        gc.fillRect(0, 0, w, h);

        if (lines.isEmpty()) {
            return;
        }

        int rows = lines.size();
        double rowHeight = h / Math.max(1, rows);

        int row = 0;
        for (double[] line : lines) {
            double max = 1e-9;
            for (double v : line) {
                if (v > max) max = v;
            }

            double y = row * rowHeight;

            for (int x = 0; x < (int) w; x++) {
                int idx = (int) ((x / w) * line.length);
                if (idx < 0) idx = 0;
                if (idx >= line.length) idx = line.length - 1;

                double norm = line[idx] / max;
                norm = Math.max(0.0, Math.min(1.0, norm));

                gc.setStroke(colorFor(norm));
                gc.strokeLine(x, y, x, y + rowHeight + 1);
            }

            row++;
        }

        drawOverlay(gc, w, h);
    }

    private Color colorFor(double v) {
        if (v < 0.08) {
            return Color.rgb(8, 8, 18);
        } else if (v < 0.18) {
            return Color.rgb(20, 24, 60);
        } else if (v < 0.32) {
            return Color.rgb(20, 70, 120);
        } else if (v < 0.48) {
            return Color.rgb(30, 140, 160);
        } else if (v < 0.65) {
            return Color.rgb(80, 200, 120);
        } else if (v < 0.82) {
            return Color.rgb(220, 210, 70);
        } else {
            return Color.rgb(255, 140, 80);
        }
    }

    private void drawOverlay(GraphicsContext gc, double w, double h) {
        gc.setStroke(Color.rgb(70, 70, 80, 0.6));
        gc.setLineWidth(1.0);

        for (int i = 0; i <= 10; i++) {
            double x = i * (w / 10.0);
            gc.strokeLine(x, 0, x, h);
        }

        // Click marker — vertical line at the most recently classified freq.
        if (markerHz >= 0 && nyquistHz > 0) {
            double mx = (markerHz / nyquistHz) * w;
            gc.setStroke(Color.rgb(255, 200, 80, 0.85));
            gc.setLineWidth(1.5);
            gc.strokeLine(mx, 0, mx, h);
            if (!markerLabel.isEmpty()) {
                // Anchor the label inside the canvas so it stays visible even
                // when the click was near the right edge.
                double tx = Math.min(mx + 6, w - 220);
                gc.setFill(Color.rgb(0, 0, 0, 0.65));
                gc.fillRect(tx - 3, 4, 220, 18);
                gc.setFill(Color.rgb(255, 220, 120, 0.95));
                gc.fillText(markerLabel, tx, 17);
            }
        }
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }
}
