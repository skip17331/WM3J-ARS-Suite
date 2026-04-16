package com.wm3j.jmap.ui.main;

import com.wm3j.jmap.app.ServiceRegistry;
import com.wm3j.jmap.service.config.Settings;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window — full-screen, operator-friendly dashboard.
 * Contains the world map, all overlays, side panels, and rotor map.
 * All configuration is handled by the separate web-based Setup Page.
 */
public class MainWindow {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    private final Stage stage;
    private final ServiceRegistry services;
    private DashboardLayout dashboard;
    private AnimationTimer renderTimer;
    private BorderPane root;

    public MainWindow(Stage stage, ServiceRegistry services) {
        this.stage = stage;
        this.services = services;
    }

    public void show() {
        Settings settings = services.getSettings();

        // Root layout
        dashboard = new DashboardLayout(services);
        root = dashboard.buildLayout();
        applyRootFontSize(root, settings.getFontSize());

        // Scene
        Scene scene = new Scene(root);
        // Load CSS (null-safe in case resource path differs by platform)
        java.net.URL cssUrl = getClass().getResource("/css/j-map.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        // Keyboard shortcuts
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11 || e.getCode() == KeyCode.F) {
                stage.setFullScreen(!stage.isFullScreen());
            }
            if (e.getCode() == KeyCode.ESCAPE && !stage.isFullScreen()) {
                // Don't exit on ESC unless not full screen
            }
        });

        // Configure stage
        stage.setTitle("j-Map — " + settings.getCallsign());
        stage.setScene(scene);
        stage.setFullScreenExitHint("Press F or F11 to toggle full screen | Setup: http://localhost:8080/setup");

        // Full screen by default
        javafx.geometry.Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setMaximized(true);

        stage.show();
        stage.setFullScreen(true);

        // Start animation loop — drives all real-time updates
        startRenderLoop();

        // Listen for settings changes from web Setup Page
        services.setSettingsChangedCallback(() -> javafx.application.Platform.runLater(() -> {
            applyRootFontSize(root, services.getSettings().getFontSize());
            dashboard.applySettings();
        }));

        log.info("Main window displayed. Setup: http://localhost:{}/setup",
            settings.getWebServerPort());
    }

    private void startRenderLoop() {
        renderTimer = new AnimationTimer() {
            private long lastFullUpdate = 0;
            private long lastRotorUpdate = 0;

            @Override
            public void handle(long now) {
                // Rotor update: every 500ms
                if (now - lastRotorUpdate > 500_000_000L) {
                    lastRotorUpdate = now;
                    try { dashboard.updateRotor(); } catch (Exception e) { log.warn("Rotor update error", e); }
                }
                // Full UI update: every second
                if (now - lastFullUpdate > 1_000_000_000L) {
                    lastFullUpdate = now;  // advance first so exceptions don't cause retry storms
                    try { dashboard.updateTime(); }     catch (Exception e) { log.warn("Time update error", e); }
                    try { dashboard.updateGrayline(); } catch (Exception e) { log.warn("Grayline update error", e); }
                    try { dashboard.updateTimer(); }    catch (Exception e) { log.warn("Timer update error", e); }
                }
            }
        };
        renderTimer.start();
    }

    public void stop() {
        if (renderTimer != null) renderTimer.stop();
    }

    private static void applyRootFontSize(BorderPane root, int fontSize) {
        root.setStyle(String.format(
            "-fx-background-color: #0a0a0f; -fx-font-size: %dpx; -fx-font-family: 'Liberation Mono', monospace;",
            fontSize));
    }
}
