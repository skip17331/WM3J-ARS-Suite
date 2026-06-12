package com.wm3j.jmap.ui.windows;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.astronomy.LunarData;
import com.wm3j.jmap.service.astronomy.PlanetData;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Floating window displaying current Moon phase/position and planet positions.
 */
public class LunarPlanetaryWindow extends FloatingWindow {

    private static final String[][] PLANET_SYMBOLS = {
        {"Venus",   "♀"},
        {"Mars",    "♂"},
        {"Jupiter", "♃"},
        {"Saturn",  "♄"},
    };

    private final ServiceRegistry services;

    private final Label illuminationLabel;
    private final Label phaseLabel;
    private final Label moonAzAltLabel;
    private final Label[] planetLabels = new Label[4];

    public LunarPlanetaryWindow(ServiceRegistry services) {
        super("Moon & Planets", 210);
        this.services = services;

        // Moon section header
        Label moonHeader = new Label(com.wm3j.jmap.i18n.I18n.get("lunar.moon"));
        moonHeader.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #ffd700; "
                          + "-fx-padding: 0 0 2 0;");
        contentBox.getChildren().add(moonHeader);

        HBox illumRow = new HBox(6);
        illuminationLabel = new Label("●");
        illuminationLabel.setStyle("-fx-font-size: 1.0em; -fx-text-fill: #93a0ae;");
        phaseLabel = new Label("---");
        phaseLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #dde3ea;");
        illumRow.getChildren().addAll(illuminationLabel, phaseLabel);
        contentBox.getChildren().add(illumRow);

        moonAzAltLabel = new Label("Az: ---  Alt: ---");
        moonAzAltLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: #93a0ae;");
        contentBox.getChildren().add(moonAzAltLabel);

        // Planets section header
        Label planetsHeader = new Label(com.wm3j.jmap.i18n.I18n.get("lunar.planets"));
        planetsHeader.setStyle("-fx-font-size: 0.77em; -fx-font-weight: bold; -fx-text-fill: #ffd700; "
                             + "-fx-padding: 4 0 2 0;");
        contentBox.getChildren().add(planetsHeader);

        for (int i = 0; i < 4; i++) {
            planetLabels[i] = new Label(PLANET_SYMBOLS[i][0] + " ---");
            planetLabels[i].setStyle("-fx-font-size: 0.85em; -fx-text-fill: #dde3ea;");
            contentBox.getChildren().add(planetLabels[i]);
        }

        update();
    }

    @Override
    public void update() {
        double lat = services.getSettings().getQthLat();
        double lon = services.getSettings().getQthLon();
        ZonedDateTime utc = ZonedDateTime.now(ZoneOffset.UTC);

        LunarData moon;
        try {
            moon = services.lunarPlanetaryService.compute(utc, lat, lon);
        } catch (Exception e) {
            return;
        }

        // Moon illumination disc color
        double illum = moon.getIllumination();
        String discColor = illumColor(illum);
        illuminationLabel.setStyle("-fx-font-size: 1.0em; -fx-text-fill: " + discColor + ";");

        String phaseText = String.format("%.0f%%  %s", illum * 100, moon.getPhaseName());
        phaseLabel.setText(phaseText);

        moonAzAltLabel.setText(String.format("Az: %.0f°  Alt: %+.0f°",
            moon.getAzimuth(), moon.getAltitude()));
        String altColor = moon.getAltitude() > 5 ? "#dde3ea" : "#6f7d8c";
        moonAzAltLabel.setStyle("-fx-font-size: 0.85em; -fx-text-fill: " + altColor + ";");

        // Planets
        List<PlanetData> planets = moon.getPlanets();
        for (int i = 0; i < 4 && planets != null && i < planets.size(); i++) {
            PlanetData p = planets.get(i);
            String symbol = PLANET_SYMBOLS[i][1];
            String name   = PLANET_SYMBOLS[i][0];
            String text;
            String style;
            if (p.isVisible()) {
                text  = symbol + " " + name + "  Az:" + String.format("%.0f°", p.azimuth())
                      + " Alt:" + String.format("%+.0f°", p.altitude());
                style = "-fx-font-size: 0.85em; -fx-text-fill: #dde3ea;";
            } else {
                text  = symbol + " " + name + "  below horizon";
                style = "-fx-font-size: 0.85em; -fx-text-fill: #6f7d8c;";
            }
            planetLabels[i].setText(text);
            planetLabels[i].setStyle(style);
        }
    }

    private String illumColor(double illum) {
        // Scale from dim gray (new) to bright white (full)
        int v = (int) (80 + illum * 175);
        return String.format("rgb(%d,%d,%d)", v, v, v);
    }
}
