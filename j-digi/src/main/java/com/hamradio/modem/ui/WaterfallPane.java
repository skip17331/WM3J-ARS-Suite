package com.hamradio.modem.ui;

import com.hamradio.modem.model.SignalSnapshot;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.util.ArrayDeque;
import java.util.Deque;

public class WaterfallPane extends Region {
    private static final int MAX_LINES = 420;

    private final Canvas canvas = new Canvas(600, 320);
    private final Deque<double[]> lines = new ArrayDeque<>();

    public WaterfallPane() {
        getChildren().add(canvas);
        setPrefSize(600, 320);
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
    }

    @Override
    protected void layoutChildren() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        redraw();
    }
}
