package com.hamclock.ui.rotor;

import com.hamclock.app.ServiceRegistry;
import com.hamclock.service.config.Settings;
import com.hamclock.service.rotor.RotorData;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Small great-circle rotor map in the lower-right corner.
 *
 * Renders an azimuthal equidistant projection centered on the home QTH.
 * Overlays:
 *   - Green line  = short-path bearing (current rotor azimuth)
 *   - Red line    = long-path bearing  (azimuth + 180°)
 *   - Cyan arc    = beam-width cone (configurable degrees)
 *   - Numeric azimuth readout
 *   - "ROTOR" label + connected/disconnected indicator
 */
public class RotorMapPane extends StackPane {

    private final ServiceRegistry services;
    private final Canvas canvas;
    private Image worldMapImage;

    private static final int MAP_SIZE = 210;

    public RotorMapPane(ServiceRegistry services) {
        this.services = services;

        canvas = new Canvas(MAP_SIZE, MAP_SIZE);
        try {
            worldMapImage = new Image(
                getClass().getResourceAsStream("/images/world_map.jpg"),
                MAP_SIZE * 2, MAP_SIZE, true, true);
        } catch (Exception ignored) {}
        setPadding(new Insets(4));
        setStyle("-fx-background-color: #08090f; -fx-background-radius: 6; " +
                 "-fx-border-color: #1a2a4a; -fx-border-width: 1; -fx-border-radius: 6;");
        setPrefSize(MAP_SIZE + 8, MAP_SIZE + 8);
        setMaxSize(MAP_SIZE + 8, MAP_SIZE + 8);

        getChildren().add(canvas);
        drawBaseMap();
    }

    private void drawBaseMap() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = MAP_SIZE, h = MAP_SIZE;
        double cx = w / 2, cy = h / 2;
        double r = Math.min(w, h) / 2 - 8;

        // Background
        gc.setFill(Color.web("#08090f"));
        gc.fillRect(0, 0, w, h);

        // Clip to circle, draw world map inside it
        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, r, r, 0, 360);
        gc.closePath();
        gc.clip();

        if (worldMapImage != null && !worldMapImage.isError()) {
            // Draw the flat world map scaled to fill the circle area
            gc.drawImage(worldMapImage, cx - r, cy - r, r * 2, r * 2);
            // Dark tint so overlays are readable
            gc.setFill(Color.web("#000010", 0.45));
            gc.fillRect(cx - r, cy - r, r * 2, r * 2);
        } else {
            gc.setFill(Color.web("#0d1a2e"));
            gc.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        gc.restore();

        // Range circles at 30°, 60°, 90°
        gc.setStroke(Color.web("#ffffff", 0.15));
        gc.setLineWidth(0.5);
        for (int deg = 30; deg <= 90; deg += 30) {
            double pr = r * deg / 90.0;
            gc.strokeOval(cx - pr, cy - pr, pr * 2, pr * 2);
        }

        // Cardinal direction lines
        gc.setStroke(Color.web("#ffffff", 0.12));
        gc.strokeLine(cx, cy - r, cx, cy + r);
        gc.strokeLine(cx - r, cy, cx + r, cy);

        // N/S/E/W labels
        gc.setFill(Color.web("#99aabb"));
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("N", cx, cy - r - 2);
        gc.fillText("S", cx, cy + r + 10);
        gc.fillText("E", cx + r + 4, cy + 4);
        gc.fillText("W", cx - r - 4, cy + 4);

        // Outer ring
        gc.setStroke(Color.web("#1a2a4a"));
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        // Home QTH dot
        gc.setFill(Color.GOLD);
        gc.fillOval(cx - 3, cy - 3, 6, 6);
    }

    public void update() {
        Settings s = services.getSettings();
        // Use cached data only - fetch() is called by background scheduler
        RotorData rotor = services.rotorProvider.getCached();

        // Redraw
        drawBaseMap();
        if (rotor != null) {
            drawRotorOverlay(rotor, s);
        }
        drawLabels(rotor, s);
    }

    private void drawRotorOverlay(RotorData rotor, Settings s) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = MAP_SIZE, h = MAP_SIZE;
        double cx = w / 2, cy = h / 2;
        double r = Math.min(w, h) / 2 - 8;

        double az = rotor.getAzimuth();
        double longPathAz = rotor.getLongPathAzimuth();

        // Beam-width arc (cyan, behind the lines)
        if (s.isShowBeamWidthArc()) {
            double halfBeam = s.getBeamWidthDegrees() / 2.0;
            double startDeg = az - halfBeam - 90;  // JavaFX arc starts East=0
            gc.save();
            gc.setFill(Color.web("#00aaff", 0.12));
            gc.fillArc(cx - r, cy - r, r * 2, r * 2, -startDeg, -s.getBeamWidthDegrees(),
                javafx.scene.shape.ArcType.ROUND);
            gc.setStroke(Color.web("#00aaff", 0.3));
            gc.setLineWidth(1.0);
            gc.strokeArc(cx - r, cy - r, r * 2, r * 2, -startDeg, -s.getBeamWidthDegrees(),
                javafx.scene.shape.ArcType.ROUND);
            gc.restore();
        }

        // Short-path line (green)
        gc.save();
        gc.setStroke(Color.web("#00ff44", 0.9));
        gc.setLineWidth(2.0);
        double azRad = Math.toRadians(az - 90); // -90 to align North=up
        gc.strokeLine(cx, cy,
            cx + r * Math.cos(azRad),
            cy + r * Math.sin(azRad));
        gc.restore();

        // Long-path line (red)
        if (s.isShowLongPath()) {
            gc.save();
            gc.setStroke(Color.web("#ff3333", 0.7));
            gc.setLineWidth(1.5);
            gc.setLineDashes(5, 4);
            double lpRad = Math.toRadians(longPathAz - 90);
            gc.strokeLine(cx, cy,
                cx + r * Math.cos(lpRad),
                cy + r * Math.sin(lpRad));
            gc.restore();
        }

        // Arrowhead at end of short-path line
        gc.save();
        gc.setFill(Color.web("#00ff44"));
        double tipX = cx + r * Math.cos(Math.toRadians(az - 90));
        double tipY = cy + r * Math.sin(Math.toRadians(az - 90));
        double arrowSize = 5;
        double arrowAng = Math.toRadians(az - 90);
        gc.fillPolygon(
            new double[]{tipX,
                tipX - arrowSize * Math.cos(arrowAng - 0.4),
                tipX - arrowSize * Math.cos(arrowAng + 0.4)},
            new double[]{tipY,
                tipY - arrowSize * Math.sin(arrowAng - 0.4),
                tipY - arrowSize * Math.sin(arrowAng + 0.4)},
            3);
        gc.restore();
    }

    private void drawLabels(RotorData rotor, Settings s) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = MAP_SIZE;

        // Panel title
        gc.setFill(Color.web("#334455"));
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 9));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("ROTOR MAP", 4, 12);

        // Connected indicator
        boolean connected = rotor != null && rotor.isConnected();
        gc.setFill(connected ? Color.web("#00ff44") : Color.web("#ff4444"));
        gc.fillOval(w - 14, 4, 8, 8);
        gc.setFill(Color.web("#556677"));
        gc.setFont(Font.font("Liberation Mono", 8));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText(connected ? "LIVE" : "DEMO", w - 16, 12);

        if (rotor == null) return;

        // Azimuth readout
        double az = rotor.getAzimuth();
        gc.setFill(Color.web("#00ff44", 0.9));
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 14));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(String.format("%.1f°", az), w / 2, MAP_SIZE - 22);

        // Short/long path labels
        gc.setFont(Font.font("Liberation Mono", 9));
        gc.setFill(Color.web("#00ff44", 0.7));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(String.format("SP: %.0f°", az), 4, MAP_SIZE - 22);
        if (s.isShowLongPath()) {
            gc.setFill(Color.web("#ff5555", 0.7));
            gc.fillText(String.format("LP: %.0f°", rotor.getLongPathAzimuth()), 4, MAP_SIZE - 10);
        }

        // Moving indicator
        if (rotor.isInMotion()) {
            gc.setFill(Color.web("#ffcc00"));
            gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 9));
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.fillText("▶ ROTATING", w - 4, MAP_SIZE - 22);
        }
    }
}
