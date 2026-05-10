package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatelliteDefinition;
import com.hamradio.jsat.model.SatellitePass;
import com.hamradio.jsat.model.SatelliteState;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.Instant;
import java.util.List;

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
    private final Label altLabel, apogeeLabel, perigeeLabel;

    private double prevElevationDeg = Double.NaN;
    private final Canvas polarCanvas;
    private final Canvas elBarCanvas;

    private final Label countdownLabel;
    private final Label passLine1Label;
    private final Label passLine2Label;

    // Bottom-bar panes — built in constructor, placed by DashboardLayout
    private final VBox passPredictionPane;
    private final VBox rangeRatePane;

    public LivePassPanel(ServiceRegistry services) {
        this.services = services;
        int fz = services.getSettings().effective(services.getSettings().livePassFontSize);

        setSpacing(6);
        setStyle(cardStyle());

        satNameLabel = styledLabel("— NO SATELLITE SELECTED —", "#aabbdd", true, fz + 1);

        // ── Big AZ / EL display ──────────────────────────────────────────────
        azBigLabel   = styledLabel("---", "#4fc3f7", true, fz + 13);
        elBigLabel   = styledLabel("---", "#69f0ae", true, fz + 13);
        azUnitLabel  = styledLabel("AZ", "#445566", false, fz - 3);
        elUnitLabel  = styledLabel("EL", "#445566", false, fz - 3);

        VBox azBox = new VBox(2, azUnitLabel, azBigLabel);
        azBox.setStyle("-fx-alignment: center; -fx-padding: 0 12 0 0;");
        VBox elBox = new VBox(2, elUnitLabel, elBigLabel);
        elBox.setStyle("-fx-alignment: center;");

        HBox azelBox = new HBox(0, azBox, separator(), elBox);
        azelBox.setStyle("-fx-alignment: center-left; -fx-padding: 4 0 4 0;");

        // Elevation bar (0° → 90°, 280px wide, 12px tall)
        elBarCanvas = new Canvas(POLAR_SIZE, 14);
        drawElBarBackground(elBarCanvas.getGraphicsContext2D(), 0);

        // ── AOS / LOS countdown (bottom-bar pane) ────────────────────────────
        countdownLabel = styledLabel("—",  "#445566", true,  fz + 3);
        passLine1Label = styledLabel("",   "#7799bb", false, fz - 1);
        passLine2Label = styledLabel("",   "#7799bb", false, fz - 1);
        Label passHeader = styledLabel("PASS PREDICTION", "#334466", false, fz - 3);
        passPredictionPane = new VBox(2, passHeader, countdownLabel, passLine1Label, passLine2Label);
        passPredictionPane.setStyle(cardStyle());
        passPredictionPane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        passPredictionPane.setMaxWidth(Double.MAX_VALUE);

        // ── Range / Rate (bottom-bar pane) ───────────────────────────────────
        rangeLabel = styledLabel("---", "#4fc3f7", true, fz + 3);
        rateLabel  = styledLabel("---", "#69f0ae", true, fz + 3);
        Label rrHeader   = styledLabel("RANGE / RATE",   "#334466", false, fz - 3);
        Label rangeKey   = styledLabel("Range",  "#556688", false, fz - 3);
        Label rateKey    = styledLabel("Rate",   "#556688", false, fz - 3);
        VBox  rangeBlock = new VBox(1, rangeKey, rangeLabel);
        VBox  rateBlock  = new VBox(1, rateKey,  rateLabel);
        HBox  rrRow      = new HBox(20, rangeBlock, rateBlock);
        rangeRatePane = new VBox(2, rrHeader, rrRow);
        rangeRatePane.setStyle(cardStyle());
        rangeRatePane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        rangeRatePane.setMaxWidth(Double.MAX_VALUE);

        // ── Telemetry grid (right sidebar — no range/rate) ───────────────────
        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(4);
        grid.setPadding(new Insets(4, 0, 4, 0));

        ColumnConstraints keyCol = new ColumnConstraints();
        keyCol.setMinWidth(Region.USE_PREF_SIZE);
        keyCol.setHgrow(Priority.NEVER);
        ColumnConstraints valCol = new ColumnConstraints();
        valCol.setHgrow(Priority.ALWAYS);
        valCol.setFillWidth(true);
        grid.getColumnConstraints().addAll(keyCol, valCol);

        downlinkLabel = val("---", fz);
        uplinkLabel   = val("---", fz);
        sunlitLabel   = val("---", fz);
        risingLabel   = val("---", fz);
        altLabel      = val("---", fz);
        apogeeLabel   = val("---", fz);
        perigeeLabel  = val("---", fz);

        int r = 0;
        grid.add(key("Downlink", fz), 0, r); grid.add(downlinkLabel, 1, r++);
        grid.add(key("Uplink",   fz), 0, r); grid.add(uplinkLabel,   1, r++);
        grid.add(key("Sunlit",   fz), 0, r); grid.add(sunlitLabel,   1, r++);
        grid.add(key("Pass",     fz), 0, r); grid.add(risingLabel,   1, r++);
        grid.add(key("Altitude", fz), 0, r); grid.add(altLabel,      1, r++);
        grid.add(key("Apogee",   fz), 0, r); grid.add(apogeeLabel,   1, r++);
        grid.add(key("Perigee",  fz), 0, r); grid.add(perigeeLabel,  1, r);

        // ── Polar plot ───────────────────────────────────────────────────────
        polarCanvas = new Canvas(POLAR_SIZE, POLAR_SIZE);
        drawPolarBackground(polarCanvas.getGraphicsContext2D());

        getChildren().addAll(satNameLabel, azelBox, elBarCanvas, grid);
    }

    public VBox getPassPredictionPane() { return passPredictionPane; }
    public VBox getRangeRatePane()      { return rangeRatePane; }

    /** Returns the polar plot wrapped in a styled card that floats over the world map. */
    public VBox buildPolarCard(int fz) {
        Label header = styledLabel("POLAR PLOT", "#334466", false, fz - 3);
        VBox card = new VBox(4, header, polarCanvas);
        card.setAlignment(javafx.geometry.Pos.CENTER);
        card.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        card.setStyle("-fx-background-color: rgba(13,16,32,0.88); -fx-background-radius: 6; "
                    + "-fx-border-color: #1a2a5a; -fx-border-radius: 6; -fx-border-width: 1; "
                    + "-fx-padding: 8; -fx-effect: dropshadow(gaussian, #000000aa, 10, 0.4, 0, 2);");
        return card;
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

        // AOS / LOS countdown
        updateCountdown(state);

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

        // Altitude / apogee / perigee
        altLabel.setText(String.format("%.0f km", state.altKm));
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
            String color = el > 45 ? "#4fc3f7" : el > 15 ? "#69f0ae" : "#ffdd00";
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
            ? Color.web("#4fc3f7") : Color.web("#445566");
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

    // ── AOS / LOS countdown ──────────────────────────────────────────────────

    private void updateCountdown(SatelliteState state) {
        List<SatellitePass> passes = services.tracker.getPassesFor(state.name);
        if (passes == null || passes.isEmpty()) {
            countdownLabel.setText("No pass data");
            countdownLabel.setStyle(countdownLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: #445566;"));
            passLine1Label.setText("Prediction pending…");
            passLine2Label.setText("");
            return;
        }

        Instant now = Instant.now();

        // Find an active pass first, then fall back to next upcoming
        SatellitePass active = null;
        SatellitePass next   = null;
        for (SatellitePass p : passes) {
            if (p.isActive(now)) { active = p; break; }
            if (p.aos.isAfter(now) && (next == null || p.aos.isBefore(next.aos))) { next = p; }
        }

        if (active != null) {
            long secsLeft = active.los.getEpochSecond() - now.getEpochSecond();
            countdownLabel.setText("LOS  " + formatCountdown(secsLeft));
            countdownLabel.setStyle(countdownLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                secsLeft < 60 ? "-fx-text-fill: #ff4444;" : "-fx-text-fill: #ff8844;"));
            passLine1Label.setText(String.format("Max El  %.0f°", active.maxElDeg));
            passLine2Label.setText(String.format("AOS Az  %.0f°   LOS Az  %.0f°",
                active.aosAzDeg, active.losAzDeg));
        } else if (next != null) {
            long secsUntil = next.aos.getEpochSecond() - now.getEpochSecond();
            countdownLabel.setText("AOS  " + formatCountdown(secsUntil));
            countdownLabel.setStyle(countdownLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
                secsUntil < 300 ? "-fx-text-fill: #69f0ae;" : "-fx-text-fill: #00b0ff;"));
            passLine1Label.setText(String.format("Max El  %.0f°", next.maxElDeg));
            passLine2Label.setText(String.format("AOS Az  %.0f°   Dur  %s",
                next.aosAzDeg, formatCountdown(next.durationSeconds())));
        } else {
            countdownLabel.setText("No upcoming pass");
            countdownLabel.setStyle(countdownLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: #445566;"));
            passLine1Label.setText("");
            passLine2Label.setText("");
        }
    }

    private static String formatCountdown(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%d:%02d:%02d", h, m, s);
        return String.format("%02d:%02d", m, s);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setLabelsEmpty() {
        azBigLabel.setText("---");
        elBigLabel.setText("---");
        prevElevationDeg = Double.NaN;
        countdownLabel.setText("—");
        countdownLabel.setStyle(countdownLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: #445566;"));
        passLine1Label.setText("");
        passLine2Label.setText("");
        rangeLabel.setText("---");
        rateLabel.setText("---");
        for (Label l : new Label[]{downlinkLabel, uplinkLabel,
                                   sunlitLabel, risingLabel, altLabel, apogeeLabel, perigeeLabel})
            l.setText("---");
    }

    private static String cardStyle() {
        return "-fx-background-color: #0d1020; -fx-background-radius: 4; "
             + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1; "
             + "-fx-padding: 6 10 6 10;";
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

    private static Label key(String text, int fz) {
        return styledLabel(text, "#556688", false, fz - 3);
    }

    private static Label val(String text, int fz) {
        return styledLabel(text, "#ccd6f6", true, fz - 2);
    }

    private static Label styledLabel(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
