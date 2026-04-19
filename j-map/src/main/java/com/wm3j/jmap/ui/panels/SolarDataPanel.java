package com.wm3j.jmap.ui.panels;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.solar.SolarData;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;

import java.time.Duration;
import java.time.Instant;

/**
 * Solar & space weather sidebar panel.
 * Shows SFI, Kp (with mini gauge), A-index, SSN, X-ray class,
 * solar wind speed/density, IMF Bz, and proton flux.
 */
public class SolarDataPanel extends VBox {

    private final ServiceRegistry services;

    private final Label sfiValue;
    private final Label kpValue;
    private final Label aValue;
    private final Label ssnValue;
    private final Label kpLabel;
    private final Label xrayLabel;
    private final Label swSpeedLabel;
    private final Label swDensLabel;
    private final Label bzLabel;
    private final Label bzValueLabel;
    private final Label protonLabel;
    private final Label lastUpdatedLabel;
    private final Canvas sunspotCanvas;
    private final Canvas kpBarCanvas;

    private static final int SUNSPOT_W = 200;
    private static final int SUNSPOT_H = 60;
    private static final int KPBAR_W   = 200;
    private static final int KPBAR_H   = 14;

    public SolarDataPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = new Label("☀  SOLAR & SPACE WEATHER");
        title.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #aabbdd; -fx-padding: 0 0 4 0;");
        getChildren().add(title);

        // ── Kp gauge bar ───────────────────────────────────────
        kpBarCanvas = new Canvas(KPBAR_W, KPBAR_H);
        getChildren().add(kpBarCanvas);

        // ── Main data grid ─────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 2, 0));

        sfiValue  = dataValue("---");
        kpValue   = dataValue("---");
        aValue    = dataValue("---");
        ssnValue  = dataValue("---");
        kpLabel   = new Label("---");
        kpLabel.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #44cc44; -fx-font-weight: bold;");
        xrayLabel = new Label("---");
        xrayLabel.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #cccccc;");

        grid.add(dataKey("SFI"),   0, 0); grid.add(sfiValue, 1, 0);
        grid.add(dataKey("Kp"),    0, 1); grid.add(kpValue,  1, 1); grid.add(kpLabel, 2, 1);
        grid.add(dataKey("A-idx"), 0, 2); grid.add(aValue,   1, 2);
        grid.add(dataKey("SSN"),   0, 3); grid.add(ssnValue, 1, 3);
        grid.add(dataKey("X-ray"), 0, 4); grid.add(xrayLabel,1, 4);
        getChildren().add(grid);

        // ── Solar wind section ─────────────────────────────────
        Label swTitle = new Label("SOLAR WIND  (DSCOVR)");
        swTitle.setStyle("-fx-font-size: 0.69em; -fx-text-fill: #556688; -fx-padding: 4 0 1 0;");
        getChildren().add(swTitle);

        GridPane swGrid = new GridPane();
        swGrid.setHgap(10);
        swGrid.setVgap(3);

        swSpeedLabel = dataValue("---");
        swDensLabel  = dataValue("---");
        bzValueLabel = dataValue("---");
        bzLabel      = new Label("---");
        bzLabel.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #44cc44;");

        swGrid.add(dataKey("Speed"),   0, 0); swGrid.add(swSpeedLabel, 1, 0);
        swGrid.add(dataKey("km/s"),    2, 0);
        swGrid.add(dataKey("Density"), 0, 1); swGrid.add(swDensLabel,  1, 1);
        swGrid.add(dataKey("p/cm³"),   2, 1);
        swGrid.add(dataKey("IMF Bz"),  0, 2); swGrid.add(bzValueLabel, 1, 2);
        swGrid.add(bzLabel,            2, 2);
        getChildren().add(swGrid);

        // ── Proton flux ────────────────────────────────────────
        GridPane protonGrid = new GridPane();
        protonGrid.setHgap(10);
        protonGrid.setVgap(2);
        protonGrid.setPadding(new Insets(4, 0, 2, 0));

        protonLabel = dataValue("---");
        Label protonEvent = new Label("NOMINAL");
        protonEvent.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #44cc44;");

        protonGrid.add(dataKey("Proton"), 0, 0); protonGrid.add(protonLabel, 1, 0);
        protonGrid.add(dataKey("pfu"),    2, 0);
        getChildren().add(protonGrid);

        // ── Sunspot graphic ────────────────────────────────────
        if (services.getSettings().isShowSunspotGraphic()) {
            Label ssTitle2 = new Label("SUNSPOT ACTIVITY");
            ssTitle2.setStyle("-fx-font-size: 0.69em; -fx-text-fill: #556688; -fx-padding: 4 0 1 0;");
            sunspotCanvas = new Canvas(SUNSPOT_W, SUNSPOT_H);
            getChildren().addAll(ssTitle2, sunspotCanvas);
        } else {
            sunspotCanvas = new Canvas(0, 0);
        }

        lastUpdatedLabel = new Label("last update: --");
        lastUpdatedLabel.setStyle("-fx-font-size: 0.69em; -fx-text-fill: #444466;");
        getChildren().add(lastUpdatedLabel);

        update();
    }

    public void update() {
        SolarData data = services.solarDataProvider.getCached();

        if (data == null) {
            Thread t = new Thread(() -> {
                try { services.solarDataProvider.fetch(); }
                catch (Exception ignored) {}
                javafx.application.Platform.runLater(this::update);
            });
            t.setDaemon(true);
            t.start();
            return;
        }

        // Core solar data
        sfiValue.setText(String.format("%.0f", data.getSfi()));
        sfiValue.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: " + sfiColor(data.getSfi()) + ";");
        kpValue.setText(String.format("%.1f", data.getKp()));
        aValue.setText(String.valueOf(data.getAIndex()));
        ssnValue.setText(String.valueOf(data.getSunspotNumber()));
        kpLabel.setText(data.getKpLabel());
        kpLabel.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: " + kpColor(data.getKp()) + ";");

        // X-ray
        String xray = data.getXrayClass();
        if (xray != null && !xray.isBlank()) {
            xrayLabel.setText(xray);
            xrayLabel.setStyle("-fx-font-size: 0.77em; -fx-text-fill: " + xrayColor(xray) + ";");
        }

        // Solar wind
        if (data.getSolarWindSpeed() > 0) {
            swSpeedLabel.setText(String.format("%.0f", data.getSolarWindSpeed()));
            swSpeedLabel.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: "
                + swSpeedColor(data.getSolarWindSpeed()) + ";");
        }
        if (data.getSolarWindDensity() > 0) {
            swDensLabel.setText(String.format("%.1f", data.getSolarWindDensity()));
        }

        // IMF Bz
        double bz = data.getBzField();
        bzValueLabel.setText(String.format("%+.1f nT", bz));
        bzValueLabel.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: " + data.getBzColor() + ";");
        bzLabel.setText(data.getBzLabel());
        bzLabel.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: " + data.getBzColor() + ";");

        // Proton flux
        double pf = data.getProtonFlux();
        protonLabel.setText(pf >= 10 ? String.format("%.0f", pf) : String.format("%.2f", pf));
        protonLabel.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: "
            + protonColor(pf) + ";");

        // Timestamp
        if (data.getObservationTime() != null) {
            long ago = Duration.between(data.getObservationTime(), Instant.now()).toMinutes();
            lastUpdatedLabel.setText(String.format("updated %dm ago", ago));
            lastUpdatedLabel.setStyle("-fx-font-size: 0.69em; -fx-text-fill: "
                + (ago > 30 ? "#ff4444" : "#444466") + ";");
        }

        // Graphics
        drawKpBar(data.getKp());
        if (sunspotCanvas.getWidth() > 0) drawSunspotGraphic(data.getSunspotNumber());
    }

    // ── Kp gauge bar ───────────────────────────────────────────

    private void drawKpBar(double kp) {
        GraphicsContext gc = kpBarCanvas.getGraphicsContext2D();
        double w = kpBarCanvas.getWidth();
        double h = kpBarCanvas.getHeight();

        // Background gradient: green → yellow → red
        gc.setFill(Color.web("#0d1020"));
        gc.fillRect(0, 0, w, h);

        // Draw 9 segments
        double segW = w / 9.0;
        String[] colors = {"#22cc22","#44cc00","#88cc00","#cccc00","#ddaa00",
                           "#ff8800","#ff5500","#ff2200","#cc0000"};
        for (int i = 0; i < 9; i++) {
            double x = i * segW;
            gc.setGlobalAlpha(0.3);
            gc.setFill(Color.web(colors[i]));
            gc.fillRect(x, 0, segW - 1, h);
        }

        // Filled portion
        double fillW = (kp / 9.0) * w;
        for (int i = 0; i < 9 && i * segW < fillW; i++) {
            double x = i * segW;
            double segEnd = Math.min((i + 1) * segW - 1, fillW);
            gc.setGlobalAlpha(1.0);
            gc.setFill(Color.web(colors[i]));
            gc.fillRect(x, 1, segEnd - x, h - 2);
        }

        // Label
        int fs = services.getSettings().getFontSize();
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.web("#ffffff", 0.85));
        gc.setFont(Font.font("Liberation Mono", fs * 0.75));
        gc.fillText(String.format("Kp %.1f", kp), 3, h - 2);
    }

    // ── Sunspot graphic ─────────────────────────────────────────

    private void drawSunspotGraphic(int ssn) {
        GraphicsContext gc = sunspotCanvas.getGraphicsContext2D();
        double w = sunspotCanvas.getWidth();
        double h = sunspotCanvas.getHeight();

        gc.setFill(Color.web("#070710"));
        gc.fillRect(0, 0, w, h);

        double cx = w / 2, cy = h / 2;
        double r = Math.min(w, h) * 0.4;

        RadialGradient sunGrad = new RadialGradient(0, 0, 0.5, 0.5, 0.5, true,
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#ffe080")),
            new Stop(0.7, Color.web("#ff8800")),
            new Stop(1.0, Color.web("#cc4400")));
        gc.setFill(sunGrad);
        gc.fillOval(cx - r, cy - r, r * 2, r * 2);

        java.util.Random rand = new java.util.Random(ssn * 17L);
        int numDots = Math.min(ssn / 5, 25);
        gc.setFill(Color.web("#1a0500", 0.85));
        for (int i = 0; i < numDots; i++) {
            double dotR  = 1.5 + rand.nextDouble() * 3.5;
            double angle = rand.nextDouble() * Math.PI * 2;
            double dist  = rand.nextDouble() * (r * 0.85);
            gc.fillOval(cx + dist * Math.cos(angle) - dotR,
                        cy + dist * Math.sin(angle) - dotR,
                        dotR * 2, dotR * 2);
        }

        int fs = services.getSettings().getFontSize();
        gc.setFill(Color.web("#ffffff", 0.7));
        gc.setFont(Font.font("Liberation Mono", fs * 0.7));
        gc.fillText("SSN " + ssn, 4, h - 4);
    }

    // ── Color helpers ───────────────────────────────────────────

    private String kpColor(double kp) {
        if (kp >= 7) return "#ff2222";
        if (kp >= 5) return "#ff8800";
        if (kp >= 4) return "#ffcc00";
        if (kp >= 3) return "#88ff00";
        return "#44cc44";
    }

    private String sfiColor(double sfi) {
        if (sfi >= 200) return "#00ff88";
        if (sfi >= 150) return "#44cc44";
        if (sfi >= 120) return "#88cc00";
        if (sfi >= 100) return "#cccc00";
        return "#cc8800";
    }

    private String xrayColor(String xray) {
        if (xray.startsWith("X")) return "#ff4444";
        if (xray.startsWith("M")) return "#ff8800";
        if (xray.startsWith("C")) return "#ffcc00";
        return "#88aacc";
    }

    private String swSpeedColor(double speed) {
        if (speed >= 700) return "#ff2222";
        if (speed >= 500) return "#ff8800";
        if (speed >= 400) return "#ffcc00";
        return "#44cc44";
    }

    private String protonColor(double pfu) {
        if (pfu >= 1000) return "#ff2222";
        if (pfu >= 100)  return "#ff8800";
        if (pfu >= 10)   return "#ffcc00";
        return "#44cc44";
    }

    private Label dataKey(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 0.77em; -fx-text-fill: #556688;");
        return l;
    }

    private Label dataValue(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 1.08em; -fx-font-weight: bold; -fx-text-fill: #ccddff;");
        return l;
    }
}
