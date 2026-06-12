package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatellitePass;
import com.hamradio.jsat.model.TleSet;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Shows upcoming satellite passes sorted by AOS time.
 * Clicking a pass selects that satellite for tracking.
 */
public class PassListPanel extends VBox {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServiceRegistry services;
    private final ListView<SatellitePass> listView;
    private final int fz;

    public PassListPanel(ServiceRegistry services) {
        this.services = services;
        this.fz = services.getSettings().effective(services.getSettings().passListFontSize);

        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #1b2027; -fx-background-radius: 4; "
               + "-fx-border-color: #323b47; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = label("📡  UPCOMING PASSES", "#b6c2cf", true);
        listView = new ListView<>();
        listView.setPrefHeight(220);
        listView.setStyle("-fx-background-color: #11161d; -fx-border-color: #323b47;");
        listView.setCellFactory(lv -> new PassCell());

        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) services.tracker.setSelectedSatellite(sel.satName);
        });

        getChildren().addAll(title, listView);
    }

    public void update() {
        List<SatellitePass> passes = services.tracker.getAllPasses();
        listView.getItems().setAll(passes);
    }

    // ── Pass cell ──────────────────────────────────────────────────────────────

    private class PassCell extends ListCell<SatellitePass> {
        @Override
        protected void updateItem(SatellitePass pass, boolean empty) {
            super.updateItem(pass, empty);
            if (empty || pass == null) {
                setText(null); setGraphic(null);
                setStyle("-fx-background-color: transparent;");
                return;
            }

            java.time.Instant now = java.time.Instant.now();
            boolean active = pass.isActive(now);
            long secs = pass.secondsUntilAos(now);

            String timeStr = FMT.format(pass.aos.atZone(ZoneId.systemDefault()));
            String countdown = secs > 0
                ? String.format("T-%02d:%02d:%02d", secs/3600, (secs%3600)/60, secs%60)
                : "  ACTIVE  ";
            String line2 = String.format("MaxEl %4.1f°  AOS Az %3.0f°  %s",
                pass.maxElDeg, pass.aosAzDeg, countdown);

            String color = active ? "#4fc3f7" : (secs < 300 ? "#ffdd00" : "#dde3ea");
            String bg    = active ? "#1f3a52" : "#1b2027";

            // Build stale badge for this satellite's TLE
            Label staleBadge = buildStaleBadge(pass.satName);

            Label nameLabel = label(pass.satName, color, true);
            Label timeLabel = label(timeStr, color, false);
            HBox  titleRow  = new HBox(6, nameLabel, timeLabel);
            if (staleBadge != null) titleRow.getChildren().add(staleBadge);
            titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label l2 = label(line2, "#93a0ae", false);
            VBox  vb = new VBox(2, titleRow, l2);
            vb.setPadding(new Insets(4, 6, 4, 6));
            vb.setStyle("-fx-background-color: " + bg + ";");
            setGraphic(vb);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }
    }

    private Label buildStaleBadge(String satName) {
        TleSet tle = services.tleManager.findByName(satName);
        if (tle == null) return null;
        int threshold = services.getSettings().tleStaleThresholdHours;
        double ageHours = tle.ageInDays() * 24;
        if (ageHours <= threshold) return null;

        // Yellow badge for 48–96h, red badge beyond 96h
        String badgeColor  = ageHours > 96 ? "#ef5350" : "#ffca28";
        String badgeText   = ageHours > 96 ? "STALE" : "OLD";
        String tooltip     = String.format("TLE age: %.0fh (threshold: %dh)", ageHours, threshold);

        Label badge = new Label(badgeText);
        badge.setStyle(String.format(
            "-fx-background-color: %s; -fx-text-fill: #0b1620; "
          + "-fx-font-size: 9px; -fx-font-weight: bold; "
          + "-fx-padding: 1 4 1 4; -fx-background-radius: 3;", badgeColor));
        Tooltip.install(badge, new Tooltip(tooltip));
        return badge;
    }

    private Label label(String text, String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
                                + "-fx-font-size: %dpx;%s", color, fz - 1, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
