package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.service.spaceweather.SpaceWeatherData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Space weather panel — three columns (Kp, Solar Wind, Activity) for the bottom bar.
 */
public class SpaceWeatherPanel extends HBox {

    private static final int BAR_W = 160, BAR_H = 11;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ServiceRegistry services;

    private final Label kpValue;
    private final Label swSpeedLabel, swDensLabel;
    private final Label bzLabel;
    private final Label xrayLabel;
    private final Label protonLabel;
    private final Label updatedLabel;
    private final Canvas kpBar;

    public SpaceWeatherPanel(ServiceRegistry services) {
        this.services = services;
        int fz = services.getSettings().effective(services.getSettings().spaceWeatherFontSize);

        setSpacing(0);
        setPadding(new Insets(6, 8, 6, 8));
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        // ── Column 1: Kp index ────────────────────────────────────────────────
        kpValue = styledLabel("---", "#44cc44", true, fz + 1);
        kpBar   = new Canvas(BAR_W, BAR_H);
        Label kpHead = styledLabel("☀ KP INDEX", "#556688", false, fz - 3);
        VBox col1 = col(kpHead, kpValue, kpBar);

        // ── Column 2: Solar wind ──────────────────────────────────────────────
        swSpeedLabel = styledLabel("--- km/s",  "#ccd6f6", false, fz - 1);
        swDensLabel  = styledLabel("--- n/cm³", "#ccd6f6", false, fz - 1);
        bzLabel      = styledLabel("---",       "#ccd6f6", true,  fz - 1);
        Label windHead = styledLabel("SOLAR WIND", "#556688", false, fz - 3);
        VBox col2 = col(windHead,
            row("Speed",   swSpeedLabel, fz),
            row("Density", swDensLabel,  fz),
            row("IMF Bz",  bzLabel,      fz));

        // ── Column 3: Activity ────────────────────────────────────────────────
        xrayLabel    = styledLabel("---",     "#44cc44", true,  fz - 1);
        protonLabel  = styledLabel("--- pfu", "#ccd6f6", false, fz - 1);
        updatedLabel = styledLabel("",        "#445566", false, fz - 3);
        Label actHead = styledLabel("ACTIVITY", "#556688", false, fz - 3);
        VBox col3 = col(actHead,
            row("X-Ray",   xrayLabel,   fz),
            row("Protons", protonLabel, fz),
            updatedLabel);

        getChildren().addAll(col1, vSep(), col2, vSep(), col3);
    }

    public void update() {
        SpaceWeatherData d = services.spaceWeather.getCached();
        if (d == null) return;

        kpValue.setText(String.format("%.1f", d.kp));
        kpValue.setStyle(kpValue.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.kpColor() + ";"));

        swSpeedLabel.setText(String.format("%.0f km/s", d.solarWindSpeedKmS));
        swDensLabel.setText(String.format("%.1f n/cm³", d.solarWindDensity));

        bzLabel.setText(String.format("%+.1f nT", d.imfBz));
        bzLabel.setStyle(bzLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.bzColor() + ";"));

        xrayLabel.setText(d.xrayClass);
        xrayLabel.setStyle(xrayLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.xrayColor() + ";"));

        protonLabel.setText(d.protonFlux > 10
            ? String.format("⚠ %.0f pfu", d.protonFlux)
            : String.format("%.1f pfu", d.protonFlux));

        if (services.spaceWeather.getLastUpdated() != null) {
            updatedLabel.setText("upd " +
                FMT.format(services.spaceWeather.getLastUpdated().atZone(ZoneId.systemDefault())));
        }

        drawKpBar(kpBar.getGraphicsContext2D(), d.kp);
    }

    private void drawKpBar(GraphicsContext gc, double kp) {
        gc.clearRect(0, 0, BAR_W, BAR_H);
        Color[] colors = {
            Color.web("#004400"), Color.web("#006600"),
            Color.web("#228800"), Color.web("#aacc00"),
            Color.web("#ffdd00"), Color.web("#ff8800"),
            Color.web("#ff4400"), Color.web("#cc0000"),
            Color.web("#880000")
        };
        double segW = (double) BAR_W / 9;
        for (int i = 0; i < 9; i++) {
            gc.setFill(i < (int) kp ? colors[i] : Color.web("#111825"));
            gc.fillRect(i * segW + 1, 0, segW - 2, BAR_H);
            gc.setFill(Color.web("#1a2a4a"));
            gc.fillRect(i * segW, 0, 1, BAR_H);
        }
        double kpX = Math.min(kp / 9.0, 1.0) * BAR_W;
        gc.setFill(Color.WHITE);
        gc.fillRect(kpX - 1, 0, 2, BAR_H);
    }

    private static VBox col(Node... children) {
        VBox box = new VBox(2);
        box.getChildren().addAll(children);
        box.setPadding(new Insets(0, 10, 0, 10));
        return box;
    }

    private static HBox row(String keyText, Label val, int fz) {
        Label k = styledLabel(keyText, "#556688", false, fz - 3);
        HBox h = new HBox(6, k, val);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Region vSep() {
        Region sep = new Region();
        sep.setStyle("-fx-background-color: #1a2a5a;");
        sep.setPrefWidth(1);
        sep.setMaxHeight(Double.MAX_VALUE);
        return sep;
    }

    private static Label styledLabel(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
