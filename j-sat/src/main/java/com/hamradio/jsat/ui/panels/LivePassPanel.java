package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatelliteDefinition;
import com.hamradio.jsat.model.SatelliteState;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Live pass display panel: prominent AZ/EL readout, polar plot, Doppler
 * frequencies, slant range, and sun status for the selected satellite.
 */
public class LivePassPanel extends VBox {

    private static final int POLAR_SIZE = 240;

    private final ServiceRegistry services;

    private final Label satNameLabel;
    private final Label azBigLabel, elBigLabel;
    private final Label azUnitLabel, elUnitLabel;
    private final Label rangeLabel, rateLabel;
    private final Label downlinkLabel, uplinkLabel;
    private final Label sunlitLabel, risingLabel;
    private final Label apogeeLabel, perigeeLabel;

    private double prevElevationDeg = Double.NaN;
    private final Canvas polarCanvas;
    private final Canvas elBarCanvas;

    public LivePassPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(6);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        satNameLabel = styledLabel("— NO SATELLITE SELECTED —", "#aabbdd", true, 12);

        // ── Big AZ / EL display ──────────────────────────────────────────────
        azBigLabel   = styledLabel("---", "#00e5ff", true, 28);
        elBigLabel   = styledLabel("---", "#69f0ae", true, 28);
        azUnitLabel  = styledLabel("AZ", "#445566", false, 10);
        elUnitLabel  = styledLabel("EL", "#445566", false, 10);

        VBox azBox = new VBox(2, azUnitLabel, azBigLabel);
        azBox.setStyle("-fx-alignment: center; -fx-padding: 0 12 0 0;");
        VBox elBox = new VBox(2, elUnitLabel, elBigLabel);
        elBox.setStyle("-fx-alignment: center;");

        HBox azelBox = new HBox(0, azBox, separator(), elBox);
        azelBox.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");

        // Elevation bar (0° → 90°, 280px wide, 12px tall)
        elBarCanvas = new Canvas(POLAR_SIZE, 14);
        drawElBarBackground(elBarCanvas.getGraphicsContext2D(), 0);

        // ── Telemetry grid ───────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(14); grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 4, 0));

        rangeLabel    = val("---");
        rateLabel     = val("---");
        downlinkLabel = val("---");
        uplinkLabel   = val("---");
        sunlitLabel   = val("---");
        risingLabel   = val("---");
        apogeeLabel   = val("---");
        perigeeLabel  = val("---");

        addRow(grid, 0, "Range",    rangeLabel,    "Rate",      rateLabel);
        addRow(grid, 1, "Downlink", downlinkLabel, "Uplink",    uplinkLabel);
        addRow(grid, 2, "Sunlit",   sunlitLabel,   "Pass",      risingLabel);
        addRow(grid, 3, "Apogee",   apogeeLabel,   "Perigee",   perigeeLabel);

        // ── Polar plot ───────────────────────────────────────────────────────
        polarCanvas = new Canvas(POLAR_SIZE, POLAR_SIZE);
        drawPolarBackground(polarCanvas.getGraphicsContext2D());

        getChildren().addAll(satNameLabel, azelBox, elBarCanvas, grid, polarCanvas);
    }

    public void update() {
        SatelliteState state = services.tracker.getSelectedState();
        if (state == null) {
            satNameLabel.setText("— NO SATELLITE SELECTED —");
            setLabelsEmpty();
            drawPolarBackground(polarCanvas.getGraphicsContext2D());
            drawElBarBackground(elBarCanvas.getGraphicsContext2D(), 0);
            return;
        }

        satNameLabel.setText("◉ " + state.name.toUpperCase());

        // Big AZ/EL readout
        azBigLabel.setText(String.format("%.1f°", state.azimuthDeg));
        double elDeg = state.elevationDeg;
        elBigLabel.setText(String.format("%.1f°", elDeg));
        elBigLabel.setStyle(elBigLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + (elDeg > 0 ? "#69f0ae" : "#ff4444") + ";"));

        // Elevation bar
        drawElBar(elBarCanvas.getGraphicsContext2D(), elDeg);

        // Telemetry
        rangeLabel.setText(String.format("%.0f km", state.slantRangeKm));
        rateLabel.setText(String.format("%+.2f km/s", state.rangeRateKmSec));
        rateLabel.setStyle(rateLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + (state.rangeRateKmSec < 0 ? "#69f0ae" : "#ff8844") + ";"));

        SatelliteDefinition def = services.satRegistry.findByName(state.name);
        if (def != null && def.downlinkHz > 0) {
            downlinkLabel.setText(formatFreq(state.correctedDownlinkHz)
                + " (" + formatHz(state.downlinkDopplerHz) + ")");
            if (def.uplinkHz > 0)
                uplinkLabel.setText(formatFreq(state.correctedUplinkHz)
                    + " (" + formatHz(state.uplinkDopplerHz) + ")");
        }

        sunlitLabel.setText(state.inSunlight ? "☀ YES" : "NO");
        sunlitLabel.setStyle(sunlitLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + (state.inSunlight ? "#ffdd00" : "#445566") + ";"));

        // Rising / falling — compare elevation to previous tick
        double elDiff = Double.isNaN(prevElevationDeg) ? 0 : (elDeg - prevElevationDeg);
        prevElevationDeg = elDeg;
        String passText; String passColor;
        if (elDeg <= 0) {
            passText  = "below horizon"; passColor = "#445566";
        } else if (elDiff > 0.01) {
            passText  = "↑ Rising";      passColor = "#69f0ae";
        } else if (elDiff < -0.01) {
            passText  = "↓ Falling";     passColor = "#ff8844";
        } else {
            passText  = "→ Near peak";   passColor = "#ffdd00";
        }
        risingLabel.setText(passText);
        risingLabel.setStyle(risingLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + passColor + ";"));

        // Apogee / perigee
        apogeeLabel.setText(String.format("%.0f km", state.apogeeKm));
        perigeeLabel.setText(String.format("%.0f km", state.perigeeKm));

        drawPolarPlot(polarCanvas.getGraphicsContext2D(), state);
    }

    // ── Elevation bar ────────────────────────────────────────────────────────

    private void drawElBarBackground(GraphicsContext gc, double el) {
        double w = POLAR_SIZE, h = 14;
        gc.setFill(Color.web("#0a1020"));
        gc.fillRect(0, 0, w, h);
        gc.setStroke(Color.web("#1a2a4a"));
        gc.setLineWidth(1.0);
        gc.strokeRect(0, 0, w - 1, h - 1);
        // tick at 0, 30, 60, 90
        for (double deg : new double[]{0, 30, 60, 90}) {
            double x = deg / 90.0 * (w - 2) + 1;
            gc.setStroke(Color.web("#1a3055"));
            gc.strokeLine(x, 0, x, h);
        }
        gc.setFill(Color.web("#334455"));
        gc.setFont(Font.font("Liberation Mono", 8));
        gc.fillText("0°", 2, h - 2);
        gc.fillText("90°", w - 18, h - 2);
    }

    private void drawElBar(GraphicsContext gc, double el) {
        drawElBarBackground(gc, el);
        double w = POLAR_SIZE, h = 14;
        if (el > 0) {
            double fillW = Math.min(el, 90) / 90.0 * (w - 2);
            String color = el > 45 ? "#00e5ff" : el > 15 ? "#69f0ae" : "#ffdd00";
            gc.setFill(Color.web(color, 0.7));
            gc.fillRect(1, 1, fillW, h - 2);
        }
        // Marker line
        double markerX = Math.max(0, Math.min(el, 90)) / 90.0 * (w - 2) + 1;
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeLine(markerX, 0, markerX, h);
    }

    // ── Polar AZ/EL plot ─────────────────────────────────────────────────────

    private void drawPolarBackground(GraphicsContext gc) {
        double cx = POLAR_SIZE / 2.0, cy = POLAR_SIZE / 2.0, r = POLAR_SIZE / 2.0 - 12;
        gc.setFill(Color.web("#080c18"));
        gc.fillRect(0, 0, POLAR_SIZE, POLAR_SIZE);

        gc.setStroke(Color.web("#1a2a4a"));
        gc.setLineWidth(1.0);
        for (double el : new double[]{0, 30, 60, 90}) {
            double pr = r * (1.0 - el / 90.0);
            gc.strokeOval(cx - pr, cy - pr, pr * 2, pr * 2);
        }
        gc.setStroke(Color.web("#1a3055"));
        gc.strokeLine(cx, cy - r - 5, cx, cy + r + 5);
        gc.strokeLine(cx - r - 5, cy, cx + r + 5, cy);

        gc.setFill(Color.web("#445566"));
        gc.setFont(Font.font("Liberation Mono", 10));
        gc.fillText("N", cx - 4, cy - r - 2);
        gc.fillText("S", cx - 4, cy + r + 13);
        gc.fillText("E", cx + r + 3, cy + 4);
        gc.fillText("W", cx - r - 14, cy + 4);

        gc.setFill(Color.web("#334455"));
        gc.setFont(Font.font("Liberation Mono", 8));
        gc.fillText("30°", cx + 2, cy - r * (30.0 / 90.0) - 2);
        gc.fillText("60°", cx + 2, cy - r * (60.0 / 90.0) - 2);
    }

    private void drawPolarPlot(GraphicsContext gc, SatelliteState state) {
        drawPolarBackground(gc);
        if (state.elevationDeg < -5) return;

        double cx = POLAR_SIZE / 2.0, cy = POLAR_SIZE / 2.0, r = POLAR_SIZE / 2.0 - 12;
        double az  = Math.toRadians(state.azimuthDeg - 90);
        double el  = Math.max(0, state.elevationDeg);
        double pr  = r * (1.0 - el / 90.0);
        double px  = cx + pr * Math.cos(az);
        double py  = cy + pr * Math.sin(az);

        // Shadow for visibility
        gc.setFill(Color.web("#000000", 0.4));
        gc.fillOval(px - 7, py - 7, 14, 14);

        Color dotColor = state.elevationDeg > 0
            ? Color.web("#00e5ff") : Color.web("#445566");
        gc.setFill(dotColor);
        gc.fillOval(px - 6, py - 6, 12, 12);
        gc.setStroke(Color.web("#ffffff", 0.7));
        gc.setLineWidth(1.5);
        gc.strokeOval(px - 8, py - 8, 16, 16);

        // AZ/EL text near dot
        gc.setFill(Color.web("#ccd6f6"));
        gc.setFont(Font.font("Liberation Mono", FontWeight.BOLD, 9));
        gc.fillText(String.format("%.0f°/%.0f°", state.azimuthDeg, state.elevationDeg),
            px + 10, py - 2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setLabelsEmpty() {
        azBigLabel.setText("---");
        elBigLabel.setText("---");
        prevElevationDeg = Double.NaN;
        for (Label l : new Label[]{rangeLabel, rateLabel, downlinkLabel, uplinkLabel,
                                   sunlitLabel, risingLabel, apogeeLabel, perigeeLabel})
            l.setText("---");
    }

    private static javafx.scene.layout.Region separator() {
        javafx.scene.layout.Region sep = new javafx.scene.layout.Region();
        sep.setStyle("-fx-background-color: #1a2a5a; -fx-min-width: 1; -fx-max-width: 1; -fx-min-height: 40;");
        sep.setPadding(new Insets(0, 8, 0, 8));
        return sep;
    }

    private static String formatFreq(long hz) {
        if (hz <= 0) return "---";
        return String.format("%.3f MHz", hz / 1e6);
    }

    private static String formatHz(long hz) {
        return String.format("%+d Hz", hz);
    }

    private static void addRow(GridPane g, int row, String k1, Label v1, String k2, Label v2) {
        g.add(key(k1), 0, row); g.add(v1, 1, row);
        g.add(key(k2), 2, row); g.add(v2, 3, row);
    }

    private static Label key(String text) {
        return styledLabel(text, "#556688", false, 10);
    }

    private static Label val(String text) {
        return styledLabel(text, "#ccd6f6", true, 11);
    }

    private static Label styledLabel(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
