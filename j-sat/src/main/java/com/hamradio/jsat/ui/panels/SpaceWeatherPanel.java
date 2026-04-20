package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.service.spaceweather.SpaceWeatherData;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Compact space weather sidebar panel.
 * Displays Kp, solar wind, IMF Bz, X-ray class, proton flux,
 * and a Kp forecast bar.
 */
public class SpaceWeatherPanel extends VBox {

    private static final int BAR_W = 190, BAR_H = 14;
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

        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = styledLabel("☀  SPACE WEATHER", "#aabbdd", true, 12);

        kpValue     = styledLabel("---",  "#44cc44", true, 20);
        swSpeedLabel= styledLabel("--- km/s", "#ccd6f6", false, 11);
        swDensLabel = styledLabel("--- n/cm³", "#ccd6f6", false, 11);
        bzLabel     = styledLabel("Bz ---", "#ccd6f6", true, 11);
        xrayLabel   = styledLabel("---",   "#44cc44", true, 11);
        protonLabel = styledLabel("--- pfu", "#ccd6f6", false, 11);
        updatedLabel= styledLabel("",      "#445566", false, 10);

        kpBar = new Canvas(BAR_W, BAR_H);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 4, 0));
        grid.add(key("Solar Wind"), 0, 0); grid.add(swSpeedLabel, 1, 0);
        grid.add(key("Density"),    0, 1); grid.add(swDensLabel,  1, 1);
        grid.add(key("IMF Bz"),     0, 2); grid.add(bzLabel,      1, 2);
        grid.add(key("X-Ray"),      0, 3); grid.add(xrayLabel,    1, 3);
        grid.add(key("Protons"),    0, 4); grid.add(protonLabel,  1, 4);

        getChildren().addAll(title,
            styledLabel("Kp Index", "#7a8aaa", false, 10),
            kpValue,
            kpBar,
            grid,
            updatedLabel);
    }

    public void update() {
        SpaceWeatherData d = services.spaceWeather.getCached();
        if (d == null) return;

        kpValue.setText(String.format("%.1f", d.kp));
        kpValue.setStyle(kpValue.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.kpColor() + ";"));

        swSpeedLabel.setText(String.format("%.0f km/s", d.solarWindSpeedKmS));
        swDensLabel.setText(String.format("%.1f n/cm³", d.solarWindDensity));

        bzLabel.setText(String.format("Bz %+.1f nT", d.imfBz));
        bzLabel.setStyle(bzLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.bzColor() + ";"));

        xrayLabel.setText(d.xrayClass);
        xrayLabel.setStyle(xrayLabel.getStyle().replaceAll("-fx-text-fill: [^;]+;",
            "-fx-text-fill: " + d.xrayColor() + ";"));

        protonLabel.setText(d.protonFlux > 10
            ? String.format("⚠ %.0f pfu", d.protonFlux)
            : String.format("%.1f pfu", d.protonFlux));

        if (services.spaceWeather.getLastUpdated() != null) {
            updatedLabel.setText("Updated " +
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
        // Kp tick
        double kpX = Math.min(kp / 9.0, 1.0) * BAR_W;
        gc.setFill(Color.WHITE);
        gc.fillRect(kpX - 1, 0, 2, BAR_H);
    }

    private static Label key(String text) {
        return styledLabel(text, "#556688", false, 11);
    }

    private static Label styledLabel(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
