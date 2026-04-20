package com.hamradio.jsat.ui.main;

import com.hamradio.jsat.app.ServiceRegistry;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main J-Sat application window.
 * Full-screen, cockpit-style satellite tracking dashboard.
 */
public class MainWindow {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final Stage stage;
    private final ServiceRegistry services;
    private DashboardLayout dashboard;
    private AnimationTimer renderTimer;

    public MainWindow(Stage stage, ServiceRegistry services) {
        this.stage    = stage;
        this.services = services;
    }

    public void show() {
        dashboard = new DashboardLayout(services);
        BorderPane root = dashboard.buildLayout();

        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/css/j-sat.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11 || e.getCode() == KeyCode.F)
                stage.setFullScreen(!stage.isFullScreen());
        });

        String cs = services.getSettings().callsign;
        stage.setTitle("J-Sat — " + cs + " — Amateur Satellite Tracking");
        stage.setScene(scene);
        stage.setFullScreenExitHint("F / F11: toggle fullscreen  |  Configure via J-Hub");

        javafx.geometry.Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setWidth(screen.getWidth());
        stage.setHeight(screen.getHeight());
        stage.setMaximized(true);
        stage.show();
        stage.setFullScreen(true);

        startRenderLoop();
    }

    private void startRenderLoop() {
        renderTimer = new AnimationTimer() {
            private long lastSecond    = 0;
            private long lastTenSec    = 0;
            private long lastMapRender = 0;

            @Override
            public void handle(long now) {
                // Map render: ~15 fps (every 67ms)
                if (now - lastMapRender > 67_000_000L) {
                    lastMapRender = now;
                    try { dashboard.renderMap(); } catch (Exception e) { log.trace("Map render: {}", e.getMessage()); }
                }

                // Clock + panels: every second
                if (now - lastSecond > 1_000_000_000L) {
                    lastSecond = now;
                    try { dashboard.tick(); }         catch (Exception e) { log.trace("Tick: {}", e.getMessage()); }
                    try { dashboard.updatePanels(); }  catch (Exception e) { log.trace("Panels: {}", e.getMessage()); }
                }

                // Pass list refresh: every 10 seconds
                if (now - lastTenSec > 10_000_000_000L) {
                    lastTenSec = now;
                    try { dashboard.updatePassList(); } catch (Exception e) { log.trace("Passes: {}", e.getMessage()); }
                }
            }
        };
        renderTimer.start();
    }

    public void stop() {
        if (renderTimer != null) renderTimer.stop();
    }
}
