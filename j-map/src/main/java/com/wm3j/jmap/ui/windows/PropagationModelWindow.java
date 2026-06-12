package com.wm3j.jmap.ui.windows;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.propagation.PropagationModelData;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Floating window showing HF propagation model: polar directional plot + band status pills.
 */
public class PropagationModelWindow extends FloatingWindow {

    private static final double CANVAS_SIZE = 200;

    // [band label, threshold MHz]
    private static final Object[][] BANDS = {
        {"80m",  3.8},
        {"40m",  7.2},
        {"20m", 14.3},
        {"15m", 21.2},
        {"10m", 28.5},
        {"6m",  50.5},
    };

    private static final String[] COMPASS_16 = {
        "N","NNE","NE","ENE","E","ESE","SE","SSE",
        "S","SSW","SW","WSW","W","WNW","NW","NNW"
    };

    private final ServiceRegistry services;
    private final Canvas polarCanvas;
    private final Label[] pillLabels = new Label[BANDS.length];

    public PropagationModelWindow(ServiceRegistry services) {
        super("HF Propagation", 220);
        this.services = services;

        polarCanvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        contentBox.getChildren().add(polarCanvas);

        VBox bandBox = new VBox(3);
        bandBox.setPadding(new Insets(4, 0, 0, 0));
        for (int i = 0; i < BANDS.length; i++) {
            String bandName = (String) BANDS[i][0];
            HBox row = new HBox(6);
            Label nameLabel = new Label(bandName);
            nameLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #93a0ae; -fx-min-width: 32;");

            Label pill = new Label("---");
            pill.setStyle(pillStyle("#444"));
            pillLabels[i] = pill;
            row.getChildren().addAll(nameLabel, pill);
            bandBox.getChildren().add(row);
        }
        contentBox.getChildren().add(bandBox);

        update();
    }

    @Override
    public void update() {
        PropagationModelData data = services.propagationModelData;
        drawPolarPlot(data);
        updateBandPills(data);
    }

    private void drawPolarPlot(PropagationModelData data) {
        GraphicsContext gc = polarCanvas.getGraphicsContext2D();
        double size = CANVAS_SIZE;
        double cx = size / 2, cy = size / 2;
        double maxR = size / 2 - 10;

        gc.setFill(Color.web("#11161d"));
        gc.fillRect(0, 0, size, size);

        // Range rings
        gc.setStroke(Color.web("#323b47", 0.8));
        gc.setLineWidth(0.8);
        gc.strokeOval(cx - maxR * 0.5, cy - maxR * 0.5, maxR, maxR);
        gc.strokeOval(cx - maxR, cy - maxR, maxR * 2, maxR * 2);

        // 16 radial lines + directional MUF color wedges
        double muf20 = 14.3;
        for (int dir = 0; dir < 16; dir++) {
            double angleDeg = dir * 22.5 - 90; // 0=N, clockwise
            double angleRad = Math.toRadians(angleDeg);
            double ex = cx + maxR * Math.cos(angleRad);
            double ey = cy + maxR * Math.sin(angleRad);

            // Sample MUF in this direction at ~2500 km range
            double muf = sampleMufInDirection(data, dir);

            Color wedgeColor;
            if (muf >= muf20 * 2)     wedgeColor = Color.rgb(0,  200, 0,  0.5);
            else if (muf >= muf20)    wedgeColor = Color.rgb(0,  200, 0,  0.35);
            else if (muf >= muf20 * 0.5) wedgeColor = Color.rgb(255, 200, 0,  0.35);
            else                      wedgeColor = Color.rgb(100, 0,  0,  0.3);

            // Draw a filled arc wedge (approximated with a triangle for simplicity)
            double prevAngle = Math.toRadians((dir - 0.5) * 22.5 - 90);
            double nextAngle = Math.toRadians((dir + 0.5) * 22.5 - 90);
            gc.setFill(wedgeColor);
            gc.beginPath();
            gc.moveTo(cx, cy);
            gc.lineTo(cx + maxR * Math.cos(prevAngle), cy + maxR * Math.sin(prevAngle));
            gc.arc(cx, cy, maxR, maxR,
                Math.toDegrees(-prevAngle), -22.5);
            gc.lineTo(cx, cy);
            gc.closePath();
            gc.fill();

            // Radial spoke
            gc.setStroke(Color.web("#323b47", 0.6));
            gc.setLineWidth(0.5);
            gc.strokeLine(cx, cy, ex, ey);
        }

        // Compass labels N/E/S/W
        gc.setFill(Color.web("#ffd700", 0.9));
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 9));
        double labelR = maxR + 6;
        gc.fillText("N", cx - 3,  cy - labelR + 8);
        gc.fillText("S", cx - 2,  cy + labelR + 2);
        gc.fillText("E", cx + labelR - 4, cy + 4);
        gc.fillText("W", cx - labelR - 2, cy + 4);
    }

    private double sampleMufInDirection(PropagationModelData data, int dir) {
        if (data == null || data.getMufGrid() == null) return 0;
        // 16 directions, 22.5° each; ~2500 km ≈ 22.5° great circle arc
        double bearingDeg = dir * 22.5;
        double bearingRad = Math.toRadians(bearingDeg);
        double qthLat = data.getQthLat();
        double qthLon = data.getQthLon();

        // Project ~2500 km in this bearing direction
        double R = 6371.0;
        double d = 2500.0 / R;
        double latRad = Math.toRadians(qthLat);
        double lonRad = Math.toRadians(qthLon);

        double destLat = Math.asin(Math.sin(latRad) * Math.cos(d)
                       + Math.cos(latRad) * Math.sin(d) * Math.cos(bearingRad));
        double destLon = lonRad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(d) * Math.cos(latRad),
            Math.cos(d) - Math.sin(latRad) * Math.sin(destLat));

        double targetLat = Math.toDegrees(destLat);
        double targetLon = Math.toDegrees(destLon);

        // Map to grid indices
        int li = (int) Math.floor((targetLon + 180.0) / 5.0);
        int lj = (int) Math.floor((targetLat + 90.0)  / 5.0);
        li = Math.max(0, Math.min(71, li));
        lj = Math.max(0, Math.min(35, lj));

        return data.getMufGrid()[li][lj];
    }

    private void updateBandPills(PropagationModelData data) {
        for (int i = 0; i < BANDS.length; i++) {
            double thresh = (double) BANDS[i][1];
            double avgMuf = averageMufNearby(data, thresh);
            String status;
            String color;
            if      (avgMuf >= thresh * 1.05) { status = "OPEN";     color = "#00cc66"; }
            else if (avgMuf >= thresh * 0.85) { status = "MARGINAL"; color = "#ffcc00"; }
            else                              { status = "CLOSED";   color = "#444"; }
            pillLabels[i].setText(status);
            pillLabels[i].setStyle(pillStyle(color));
        }
    }

    private double averageMufNearby(PropagationModelData data, double threshMhz) {
        if (data == null || data.getMufGrid() == null) return 0;
        double[][] grid = data.getMufGrid();
        double sum = 0;
        int count = 0;
        for (int li = 0; li < grid.length; li++) {
            for (int lj = 0; lj < grid[li].length; lj++) {
                sum += grid[li][lj];
                count++;
            }
        }
        return count > 0 ? sum / count : 0;
    }

    private String pillStyle(String color) {
        return "-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: " + color
             + "; -fx-border-color: " + color + "; -fx-border-width: 1; "
             + "-fx-padding: 0 4 0 4; -fx-border-radius: 3;";
    }
}
