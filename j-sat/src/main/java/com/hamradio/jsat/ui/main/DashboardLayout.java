package com.hamradio.jsat.ui.main;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.ui.canvas.SatTrackCanvas;
import com.hamradio.jsat.ui.panels.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Main dashboard layout for J-Sat.
 *
 *  ┌──────────────────────────────────────────────────────┐
 *  │  TOP BAR: title + clock + UTC                        │
 *  ├──────────────────────────────────┬───────────────────┤
 *  │                                  │  LIVE PASS        │
 *  │    WORLD MAP  +  TRACKS          │  PASS LIST        │
 *  │                                  │  SPACE WEATHER    │
 *  │                                  │  RIG / ROTOR      │
 *  └──────────────────────────────────┴───────────────────┘
 */
public class DashboardLayout {

    private static final DateTimeFormatter UTC_FMT = DateTimeFormatter.ofPattern("HH:mm:ss 'Z'");
    private static final DateTimeFormatter LOC_FMT = DateTimeFormatter.ofPattern("HH:mm:ss z");

    private final ServiceRegistry services;

    private SatTrackCanvas worldMap;
    private PassListPanel  passList;
    private LivePassPanel  livePass;
    private SpaceWeatherPanel swPanel;
    private RigRotorPanel  rigRotor;
    private Label          utcClock;
    private Label          locClock;

    public DashboardLayout(ServiceRegistry services) {
        this.services = services;
    }

    public BorderPane buildLayout() {
        JsatSettings s = services.getSettings();

        // ── Top bar ────────────────────────────────────────────────────────────
        Label title = styled("J-SAT", "#00e5ff", true, 16);
        Label callsign = styled(s.callsign, "#aabbdd", true, 14);
        utcClock = styled("--:--:-- Z", "#88bbff", false, 13);
        locClock = styled("--:--:--",   "#6688aa", false, 13);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, title, callsign, spacer, locClock, utcClock);
        topBar.setPadding(new Insets(8, 14, 8, 14));
        topBar.setStyle("-fx-background-color: #080c18; -fx-border-color: #1a2a4a; -fx-border-width: 0 0 1 0;");
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── World map ──────────────────────────────────────────────────────────
        worldMap = new SatTrackCanvas(services);

        // ── Right sidebar ──────────────────────────────────────────────────────
        livePass = new LivePassPanel(services);
        passList = new PassListPanel(services);
        swPanel  = new SpaceWeatherPanel(services);
        rigRotor = new RigRotorPanel(services);

        VBox sidebar = new VBox(8, livePass, passList);
        if (s.showSpaceWeather) sidebar.getChildren().add(swPanel);
        sidebar.getChildren().add(rigRotor);
        sidebar.setPadding(new Insets(8));
        sidebar.setPrefWidth(280);
        sidebar.setStyle("-fx-background-color: #08090f;");

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setPrefWidth(288);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setStyle("-fx-background-color: #08090f; -fx-border-color: #1a2a4a; "
                             + "-fx-border-width: 0 0 0 1;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(worldMap);
        root.setRight(sidebarScroll);
        root.setStyle("-fx-background-color: #0a0a0f; -fx-font-size: 13px; "
                    + "-fx-font-family: 'Liberation Mono', monospace;");

        return root;
    }

    /** Called from animation loop every second. */
    public void tick() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        utcClock.setText(UTC_FMT.format(now));
        locClock.setText(LOC_FMT.format(now.withZoneSameInstant(ZoneId.systemDefault())));
    }

    /** Called from animation loop — render world map. */
    public void renderMap() {
        worldMap.render();
    }

    /** Called from animation loop every second — refresh sidebar panels. */
    public void updatePanels() {
        livePass.update();
        swPanel.update();
        rigRotor.update();
    }

    /** Called from animation loop every 10 seconds — refresh pass list. */
    public void updatePassList() {
        passList.update();
    }

    private static Label styled(String text, String color, boolean bold, int size) {
        Label l = new Label(text);
        l.setStyle(String.format("-fx-text-fill: %s; -fx-font-family: 'Liberation Mono'; "
            + "-fx-font-size: %dpx;%s", color, size, bold ? " -fx-font-weight: bold;" : ""));
        return l;
    }
}
