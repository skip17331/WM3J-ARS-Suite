package com.hamradio.jsat.ui.main;

import com.hamradio.jsat.app.ServiceRegistry;
import com.hamradio.jsat.service.config.JsatSettings;
import com.hamradio.jsat.ui.canvas.SatTrackCanvas;
import com.hamradio.jsat.ui.panels.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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
 *  ┌─────────────────────────────────────────────────────────────────────┐
 *  │  TOP BAR: title + clock + UTC                                       │
 *  ├────────────────────────────────────────────┬────────────────────────┤
 *  │                                            │  LIVE PASS             │
 *  │    WORLD MAP  +  TRACKS                    │  PASS LIST             │
 *  │    [POLAR PLOT — floating overlay]         │                        │
 *  ├────────────────────────────────────────────┴────────────────────────┤
 *  │  PASS PREDICTION │ RANGE/RATE │ SPACE WEATHER │ RIG / ROTOR         │
 *  └─────────────────────────────────────────────────────────────────────┘
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
    private EmePanel        emePanel;
    private Label          utcClock;
    private Label          locClock;

    public DashboardLayout(ServiceRegistry services) {
        this.services = services;
    }

    public BorderPane buildLayout() {
        JsatSettings s = services.getSettings();

        // ── Top bar ────────────────────────────────────────────────────────────
        int topFz = s.effective(s.topBarFontSize);
        Label title = styled("J-SAT", "#4fc3f7", true, Math.round(topFz * 1.23f));
        Label callsign = styled(s.callsign, "#b6c2cf", true, Math.round(topFz * 1.08f));
        utcClock = styled("--:--:-- Z", "#4fc3f7", false, topFz);
        locClock = styled("--:--:--",   "#93a0ae", false, topFz);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(12, title, callsign, spacer, locClock, utcClock);
        topBar.setPadding(new Insets(8, 14, 8, 14));
        topBar.setStyle("-fx-background-color: #11161d; -fx-border-color: #323b47; -fx-border-width: 0 0 1 0;");
        topBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // ── World map ──────────────────────────────────────────────────────────
        worldMap = new SatTrackCanvas(services);

        // ── Panels ─────────────────────────────────────────────────────────────
        livePass = new LivePassPanel(services);
        passList = new PassListPanel(services);
        swPanel  = new SpaceWeatherPanel(services);
        rigRotor = new RigRotorPanel(services);

        // ── Center: world map with floating polar plot overlay ─────────────────
        VBox polarCard = livePass.buildPolarCard(s.effective(s.livePassFontSize));
        StackPane.setAlignment(polarCard, Pos.BOTTOM_LEFT);
        StackPane.setMargin(polarCard, new Insets(0, 0, 12, 12));
        StackPane centerPane = new StackPane(worldMap, polarCard);

        // ── Right sidebar: live pass data + upcoming passes ─────────────────────
        VBox rightSidebar = new VBox(8, livePass, passList);
        rightSidebar.setPadding(new Insets(8));
        rightSidebar.setPrefWidth(340);
        rightSidebar.setStyle("-fx-background-color: #11161d;");

        ScrollPane rightScroll = new ScrollPane(rightSidebar);
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(348);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setStyle("-fx-background-color: #11161d; -fx-border-color: #323b47; "
                           + "-fx-border-width: 0 0 0 1;");

        // ── Bottom bar: pass prediction + range/rate + space weather + rig/rotor
        VBox passPred  = livePass.getPassPredictionPane();
        VBox rangeRate = livePass.getRangeRatePane();
        HBox.setHgrow(passPred,  Priority.ALWAYS);
        HBox.setHgrow(rangeRate, Priority.ALWAYS);

        HBox bottomBar = new HBox(8, passPred, rangeRate);
        if (s.showSpaceWeather) {
            HBox.setHgrow(swPanel, Priority.ALWAYS);
            swPanel.setMaxWidth(Double.MAX_VALUE);
            bottomBar.getChildren().add(swPanel);
        }
        if (s.showEmePanel) {
            emePanel = new EmePanel(services);
            HBox.setHgrow(emePanel, Priority.ALWAYS);
            emePanel.setMaxWidth(Double.MAX_VALUE);
            bottomBar.getChildren().add(emePanel);
        }
        HBox.setHgrow(rigRotor, Priority.ALWAYS);
        rigRotor.setMaxWidth(Double.MAX_VALUE);
        bottomBar.getChildren().add(rigRotor);
        bottomBar.setPadding(new Insets(6, 8, 6, 8));
        bottomBar.setStyle("-fx-background-color: #11161d; -fx-border-color: #323b47; "
                         + "-fx-border-width: 1 0 0 0;");

        BorderPane root = new BorderPane();
        root.setTop(topBar);
        root.setCenter(centerPane);
        root.setRight(rightScroll);
        root.setBottom(bottomBar);
        root.setStyle("-fx-background-color: #15191f; -fx-font-size: " + s.fontSize + "px; "
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
        if (emePanel != null) emePanel.update();
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
