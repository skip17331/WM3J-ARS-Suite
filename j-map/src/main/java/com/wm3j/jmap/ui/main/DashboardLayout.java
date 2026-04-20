package com.wm3j.jmap.ui.main;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.config.Settings;
import com.wm3j.jmap.service.config.SettingsLoader;
import com.wm3j.jmap.ui.overlays.WorldMapCanvas;
import com.wm3j.jmap.ui.panels.*;
import com.wm3j.jmap.ui.rotor.RotorMapPane;
import com.wm3j.jmap.ui.windows.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Orchestrates the full dashboard layout:
 *
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  TIME PANEL (top bar)                                        │
 *  ├──────────────────────────────────────┬───────────────────────┤
 *  │                                      │  SOLAR DATA           │
 *  │     WORLD MAP (main canvas)          │  PROPAGATION          │
 *  │      + floating windows overlay      │  BAND CONDITIONS      │
 *  │                          ┌───────────┤                       │
 *  │                          │ ROTOR MAP │                       │
 *  └──────────────────────────┴───────────┴───────────────────────┘
 *
 * Floating windows (movable, over the map):
 *  - CountdownTimerWindow
 *  - ContestListWindow
 *  - DEInfoWindow
 *  - DXInfoWindow
 */
public class DashboardLayout {

    private final ServiceRegistry services;

    // Fixed panels
    private TimePanel timePanel;
    private WorldMapCanvas worldMap;
    private SolarDataPanel solarPanel;
    private PropagationPanel propagationPanel;
    private BandConditionsPanel bandPanel;
    private RotorMapPane rotorMap;
    private SetupHintBar setupHint;
    private com.wm3j.jmap.ui.panels.DxScrollerBar dxScroller;

    // Floating windows
    private ContestListWindow    contestList;
    private DEInfoWindow         deWindow;
    private DXInfoWindow         dxWindow;

    // ID timer state (displayed in TimePanel)
    private static final int TIMER_TOTAL_SECONDS = 600;
    private static final int TIMER_FLASH_CYCLES  = 12;
    private int      timerRemaining  = TIMER_TOTAL_SECONDS;
    private boolean  timerFlashing   = false;
    private boolean  timerFlashVisible = true;
    private int      timerFlashCount = 0;
    private Timeline timerFlashTimeline;

    // The Pane that holds the map + all floating windows
    private Pane mapOverlayPane;

    public DashboardLayout(ServiceRegistry services) {
        this.services = services;
    }

    public BorderPane buildLayout() {
        Settings s = services.getSettings();

        // ── Top bar ────────────────────────────────────────────────────────
        timePanel = new TimePanel(s);

        // ── Right sidebar ──────────────────────────────────────────────────
        VBox rightSidebar = buildRightSidebar(s);

        // ── World map canvas ───────────────────────────────────────────────
        worldMap = new WorldMapCanvas(services);

        // ── Rotor map (lower-right corner overlay) ─────────────────────────
        rotorMap = new RotorMapPane(services);
        rotorMap.setVisible(s.isShowRotorMap());
        rotorMap.setPrefSize(220, 220);

        // ── Floating windows ───────────────────────────────────────────────
        contestList = new ContestListWindow(services);
        contestList.setLayoutX(s.getContestListX());
        contestList.setLayoutY(s.getContestListY());
        contestList.setVisible(s.isShowContestList());
        contestList.setOnPositionSaved(() -> {
            s.setContestListX(contestList.getLayoutX());
            s.setContestListY(contestList.getLayoutY());
            SettingsLoader.save(s);
        });

        deWindow = new DEInfoWindow(services);
        deWindow.setLayoutX(s.getDeWindowX());
        deWindow.setLayoutY(s.getDeWindowY());
        deWindow.setVisible(s.isShowDeWindow());
        deWindow.setOnPositionSaved(() -> {
            s.setDeWindowX(deWindow.getLayoutX());
            s.setDeWindowY(deWindow.getLayoutY());
            SettingsLoader.save(s);
        });

        dxWindow = new DXInfoWindow(services);
        dxWindow.setLayoutX(s.getDxWindowX());
        dxWindow.setLayoutY(s.getDxWindowY());
        dxWindow.setVisible(s.isShowDxWindow());
        dxWindow.setOnPositionSaved(() -> {
            s.setDxWindowX(dxWindow.getLayoutX());
            s.setDxWindowY(dxWindow.getLayoutY());
            SettingsLoader.save(s);
        });

        // Wire DX spot clicks → DX window + publish to hub for other apps
        worldMap.setDxSpotClickCallback(spot -> {
            dxWindow.showSpot(spot);
            if (s.isShowDxWindow()) dxWindow.setVisible(true);
            services.dxClusterClient.sendSpotSelected(spot);
        });

        // Incoming SPOT_SELECTED from hub (e.g. HamLog clicked a spot) → DX window
        services.dxClusterClient.setSpotSelectedListener(spot ->
            javafx.application.Platform.runLater(() -> {
                dxWindow.showSpot(spot);
                if (s.isShowDxWindow()) dxWindow.setVisible(true);
            }));

        // ── Map overlay AnchorPane — all layers fill 100% ─────────────────
        AnchorPane mapStack = new AnchorPane();

        // World map fills entire area
        AnchorPane.setTopAnchor(worldMap,    0.0);
        AnchorPane.setBottomAnchor(worldMap, 0.0);
        AnchorPane.setLeftAnchor(worldMap,   0.0);
        AnchorPane.setRightAnchor(worldMap,  0.0);

        // Rotor map pinned bottom-right
        AnchorPane.setBottomAnchor(rotorMap, 8.0);
        AnchorPane.setRightAnchor(rotorMap,  8.0);

        // Free-layout pane for draggable windows — also fills entire area
        mapOverlayPane = new Pane();
        mapOverlayPane.setMouseTransparent(false);
        mapOverlayPane.setPickOnBounds(false);
        mapOverlayPane.getChildren().addAll(contestList, deWindow, dxWindow);
        AnchorPane.setTopAnchor(mapOverlayPane,    0.0);
        AnchorPane.setBottomAnchor(mapOverlayPane, 0.0);
        AnchorPane.setLeftAnchor(mapOverlayPane,   0.0);
        AnchorPane.setRightAnchor(mapOverlayPane,  0.0);

        mapStack.getChildren().addAll(worldMap, rotorMap, mapOverlayPane);

        // ── DX scroller + hint bar ─────────────────────────────────────────
        setupHint = new SetupHintBar(services.getSettings().getWebServerPort());
        dxScroller = new com.wm3j.jmap.ui.panels.DxScrollerBar(services);

        VBox bottomBar = new VBox(0);
        bottomBar.getChildren().addAll(dxScroller, setupHint);

        // ── Root layout ────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0f;");
        root.setTop(timePanel);
        root.setCenter(mapStack);
        root.setRight(rightSidebar);
        root.setBottom(bottomBar);
        BorderPane.setMargin(rightSidebar, new Insets(0, 0, 0, 2));

        return root;
    }

    private VBox buildRightSidebar(Settings s) {
        solarPanel      = new SolarDataPanel(services);
        propagationPanel= new PropagationPanel(services);
        bandPanel       = new BandConditionsPanel(services);

        VBox sidebar = new VBox(4);
        sidebar.setPadding(new Insets(4, 4, 4, 4));
        sidebar.setPrefWidth(253);
        sidebar.setStyle("-fx-background-color: #090912;");

        if (s.isShowSolarData())       sidebar.getChildren().add(solarPanel);
        if (s.isShowPropagationData()) sidebar.getChildren().add(propagationPanel);
        if (s.isShowBandConditions())  sidebar.getChildren().add(bandPanel);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        return sidebar;
    }

    // ── Update methods called by the animation loop ───────────────────────

    public void updateTime() {
        if (timePanel != null) timePanel.update();
    }

    public void updateGrayline() {
        if (worldMap    != null) worldMap.redraw();
        if (solarPanel  != null) solarPanel.update();
        if (propagationPanel != null) propagationPanel.update();
        if (bandPanel   != null) bandPanel.update();

        // Update floating windows (these throttle internally)
        if (deWindow    != null && deWindow.isVisible())    deWindow.update();
        if (dxWindow    != null && dxWindow.isVisible())    dxWindow.update();
        if (contestList != null && contestList.isVisible()) contestList.update();
        if (dxScroller  != null) dxScroller.update();
    }

    public void updateRotor() {
        if (rotorMap != null && rotorMap.isVisible()) rotorMap.update();
    }

    /** Called every second by the animation loop for the countdown timer. */
    public void updateTimer() {
        if (timePanel == null || !services.getSettings().isShowCountdownTimer()) return;
        if (timerFlashing) return;

        if (timerRemaining > 0) {
            timerRemaining--;
            renderTimerDisplay();
        }

        if (timerRemaining == 0) startTimerFlash();
    }

    private void renderTimerDisplay() {
        int m = timerRemaining / 60;
        int s = timerRemaining % 60;
        String timeStr = String.format("%d:%02d", m, s);
        String color;
        if      (timerRemaining > 60) color = "#00cc66";
        else if (timerRemaining > 10) color = "#ffcc00";
        else                          color = "#ff4455";
        timePanel.updateTimer(timeStr, color, "RUNNING");
    }

    private void startTimerFlash() {
        timerFlashing    = true;
        timerFlashCount  = 0;
        timerFlashVisible = true;
        timePanel.updateTimer("0:00", "#ff4455", "ZERO!");

        timerFlashTimeline = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            timerFlashVisible = !timerFlashVisible;
            timePanel.flashTimerText(timerFlashVisible);
            if (++timerFlashCount >= TIMER_FLASH_CYCLES) stopTimerFlash();
        }));
        timerFlashTimeline.setCycleCount(Timeline.INDEFINITE);
        timerFlashTimeline.play();
    }

    private void stopTimerFlash() {
        if (timerFlashTimeline != null) timerFlashTimeline.stop();
        timerFlashing  = false;
        timerRemaining = TIMER_TOTAL_SECONDS;
        timePanel.flashTimerText(true);
        renderTimerDisplay();
        timePanel.updateTimer("10:00", "#00cc66", "RUNNING");
    }

    /** Called when settings change from Setup Page */
    public void applySettings() {
        Settings s = services.getSettings();

        if (rotorMap    != null) rotorMap.setVisible(s.isShowRotorMap());
        if (worldMap    != null) worldMap.settingsChanged();
        if (timePanel   != null) timePanel.settingsChanged(s);
        if (contestList != null) contestList.setVisible(s.isShowContestList());
        if (deWindow       != null) deWindow.setVisible(s.isShowDeWindow());
        if (dxWindow       != null) {
            if (!s.isShowDxWindow()) dxWindow.setVisible(false);
        }
    }
}
