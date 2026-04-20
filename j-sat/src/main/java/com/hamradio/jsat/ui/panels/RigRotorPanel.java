package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatelliteState;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Rig and rotor status panel.
 * Rig and rotor are controlled via J-Hub; this panel shows the status
 * received back from J-Hub (RIG_STATUS / ROTOR_STATUS messages).
 */
public class RigRotorPanel extends VBox {

    private static final int COMPASS_SIZE = 120;

    private final ServiceRegistry services;

    private final Label rigStatusLabel;
    private final Label rotorStatusLabel;
    private final Label rigFreqLabel;
    private final Label rotorPosLabel;
    private final Label rotorTargetLabel;
    private final Canvas compassCanvas;

    public RigRotorPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(5);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = styled("🎛  RIG / ROTOR (via J-Hub)", "#aabbdd", true, 12);

        rigStatusLabel   = styled("Rig: —", "#445566", false, 11);
        rotorStatusLabel = styled("Rotor: —", "#445566", false, 11);
        rigFreqLabel     = styled("DL: ---  UL: ---", "#ccd6f6", true, 11);
        rotorPosLabel    = styled("AZ ---°  EL ---°", "#ccd6f6", false, 11);
        rotorTargetLabel = styled("→ ---°  ---°", "#7a8aaa", false, 11);

        compassCanvas = new Canvas(COMPASS_SIZE, COMPASS_SIZE);
        drawCompassBackground(compassCanvas.getGraphicsContext2D());

        getChildren().addAll(title,
            rigStatusLabel, rigFreqLabel,
            rotorStatusLabel, rotorPosLabel, rotorTargetLabel,
            compassCanvas);
    }

    public void update() {
        boolean hubConn = services.hubClient.isConnected();

        // ── Rig ──────────────────────────────────────────────────────────────
        if (!services.getSettings().rigControlEnabled) {
            rigStatusLabel.setText("Doppler: DISABLED");
            rigStatusLabel.setStyle(rigStatusLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                "-fx-text-fill: #445566;"));
            rigFreqLabel.setText("---");
        } else {
            String rigSt = hubConn ? "● Via J-Hub" : "○ J-Hub offline";
            rigStatusLabel.setText("Doppler: " + rigSt);
            rigStatusLabel.setStyle(rigStatusLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                "-fx-text-fill: " + (hubConn ? "#44cc44" : "#ff8844") + ";"));
            long hz = services.hubRigFreqHz;
            rigFreqLabel.setText(hz > 0
                ? String.format("%.3f MHz", hz / 1e6)
                : "Awaiting rig data");
        }

        // ── Rotor ─────────────────────────────────────────────────────────────
        if (!services.getSettings().rotorControlEnabled) {
            rotorStatusLabel.setText("Rotor: DISABLED");
            rotorStatusLabel.setStyle(rotorStatusLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                "-fx-text-fill: #445566;"));
            rotorPosLabel.setText("---");
            rotorTargetLabel.setText("---");
        } else {
            String rotSt = hubConn ? "● Via J-Hub" : "○ J-Hub offline";
            rotorStatusLabel.setText("Rotor: " + rotSt);
            rotorStatusLabel.setStyle(rotorStatusLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                "-fx-text-fill: " + (hubConn ? "#44cc44" : "#ff8844") + ";"));
            rotorPosLabel.setText(String.format("AZ %.0f°  EL %.0f°",
                services.hubRotorAz, services.hubRotorEl));

            SatelliteState sel = services.tracker.getSelectedState();
            if (sel != null && sel.isVisible()) {
                rotorTargetLabel.setText(String.format("→ %.0f°  %.0f°",
                    sel.azimuthDeg, sel.elevationDeg));
                drawCompassWithPointer(compassCanvas.getGraphicsContext2D(),
                    services.hubRotorAz, services.hubRotorEl,
                    sel.azimuthDeg, sel.elevationDeg);
            } else {
                rotorTargetLabel.setText("No satellite visible");
                drawCompassWithPointer(compassCanvas.getGraphicsContext2D(),
                    services.hubRotorAz, services.hubRotorEl, -1, -1);
            }
        }
    }

    // ── Compass drawing ────────────────────────────────────────────────────────

    private void drawCompassBackground(GraphicsContext gc) {
        double cx = COMPASS_SIZE / 2.0, cy = COMPASS_SIZE / 2.0, r = cx - 8;
        gc.setFill(Color.web("#080c18"));
        gc.fillRect(0, 0, COMPASS_SIZE, COMPASS_SIZE);
        gc.setStroke(Color.web("#1a2a4a"));
        gc.setLineWidth(1.5);
        gc.strokeOval(cx - r, cy - r, r * 2, r * 2);

        gc.setFill(Color.web("#445566"));
        gc.setFont(javafx.scene.text.Font.font("Liberation Mono", 10));
        gc.fillText("N", cx - 4, cy - r + 12);
        gc.fillText("S", cx - 4, cy + r + 2);
        gc.fillText("E", cx + r - 10, cy + 4);
        gc.fillText("W", cx - r + 2, cy + 4);

        gc.setStroke(Color.web("#334455"));
        gc.setLineWidth(1.0);
        for (int deg = 0; deg < 360; deg += 30) {
            double a = Math.toRadians(deg - 90);
            double x1 = cx + (r - 6) * Math.cos(a);
            double y1 = cy + (r - 6) * Math.sin(a);
            double x2 = cx + r * Math.cos(a);
            double y2 = cy + r * Math.sin(a);
            gc.strokeLine(x1, y1, x2, y2);
        }
    }

    private void drawCompassWithPointer(GraphicsContext gc,
                                        double curAz, double curEl,
                                        double satAz, double satEl) {
        drawCompassBackground(gc);
        double cx = COMPASS_SIZE / 2.0, cy = COMPASS_SIZE / 2.0, r = cx - 8;

        drawPointer(gc, cx, cy, r - 10, curAz, Color.web("#ccd6f6"), 2.5);

        if (satAz >= 0) {
            drawPointer(gc, cx, cy, r - 4, satAz, Color.web("#00e5ff"), 1.5);
        }

        double elFrac = Math.max(0, Math.min(curEl, 90)) / 90.0;
        if (elFrac > 0) {
            gc.setStroke(Color.web("#2a8899", 0.5));
            gc.setLineWidth(3);
            gc.strokeArc(cx - 15, cy - 15, 30, 30,
                90 - curEl * 2, curEl * 4,
                javafx.scene.shape.ArcType.OPEN);
        }
    }

    private static void drawPointer(GraphicsContext gc, double cx, double cy,
                                    double len, double azDeg, Color color, double width) {
        double a = Math.toRadians(azDeg - 90);
        gc.setStroke(color);
        gc.setLineWidth(width);
        gc.strokeLine(cx, cy, cx + len * Math.cos(a), cy + len * Math.sin(a));
        gc.setFill(color);
        gc.fillOval(cx + len * Math.cos(a) - 3, cy + len * Math.sin(a) - 3, 6, 6);
    }

    private static Label styled(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
