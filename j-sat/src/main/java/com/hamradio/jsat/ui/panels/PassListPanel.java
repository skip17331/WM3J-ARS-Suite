package com.hamradio.jsat.ui.panels;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.model.SatellitePass;
import javafx.geometry.Insets;
import javafx.scene.control.*;
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

    public PassListPanel(ServiceRegistry services) {
        this.services = services;

        setSpacing(4);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #0d1020; -fx-background-radius: 4; "
               + "-fx-border-color: #1a2a5a; -fx-border-radius: 4; -fx-border-width: 1;");

        Label title = label("📡  UPCOMING PASSES", "#aabbdd", true);
        listView = new ListView<>();
        listView.setPrefHeight(220);
        listView.setStyle("-fx-background-color: #08090f; -fx-border-color: #1a2a4a;");
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
            String line1 = String.format("%-14s  %s", pass.satName, timeStr);
            String countdown = secs > 0
                ? String.format("T-%02d:%02d:%02d", secs/3600, (secs%3600)/60, secs%60)
                : "  ACTIVE  ";
            String line2 = String.format("MaxEl %4.1f°  AOS Az %3.0f°  %s",
                pass.maxElDeg, pass.aosAzDeg, countdown);

            String color = active ? "#00e5ff" : (secs < 300 ? "#ffdd00" : "#ccd6f6");
            String bg    = active ? "#001830" : "#0d1020";

            Label l1 = label(line1, color, true);
            Label l2 = label(line2, "#7a8aaa", false);
            VBox  vb = new VBox(2, l1, l2);
            vb.setPadding(new Insets(4, 6, 4, 6));
            vb.setStyle("-fx-background-color: " + bg + ";");
            setGraphic(vb);
            setText(null);
            setStyle("-fx-background-color: transparent; -fx-padding: 2 0 2 0;");
        }
    }

    private static Label label(String text, String color, boolean bold) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
                                + "-fx-font-size: 11px;%s", color, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
