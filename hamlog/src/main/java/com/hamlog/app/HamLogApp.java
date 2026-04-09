package com.hamlog.app;

import com.hamlog.db.DatabaseManager;
import com.hamlog.i18n.I18n;
import com.hamlog.plugin.PluginLoader;
import com.hamlog.util.AppConfig;
import com.hamlog.util.LoggingConfigurator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * HamLog — Amateur Radio Logging Application
 * Main JavaFX Application entry point.
 */
public class HamLogApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(HamLogApp.class);

    @Override
    public void init() throws Exception {
        // 1. Load application config (prefs / SQLite paths / theme)
        AppConfig.getInstance().load();

        // 2. Configure logging level
        LoggingConfigurator.configure(AppConfig.getInstance().isDebugMode());

        // 3. Initialise i18n
        I18n.load(AppConfig.getInstance().getLanguage());

        // 4. Initialise databases
        DatabaseManager.getInstance().initAll();

        // 5. Load contest plugins (bundled + user-installed)
        PluginLoader.getInstance().init();

        log.info("j-Log initialised — version 1.0.0");
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Show splash screen first
        SplashScreen splash = new SplashScreen(primaryStage);
        splash.show();
    }

    @Override
    public void stop() {
        log.info("j-Log shutting down");
        DatabaseManager.getInstance().closeAll();
    }

    /** Convenience: apply the currently-configured CSS theme and font size to any scene. */
    public static void applyTheme(Scene scene) {
        AppConfig cfg = AppConfig.getInstance();
        String theme = cfg.getTheme();
        scene.getStylesheets().clear();
        String base = Objects.requireNonNull(
            HamLogApp.class.getResource("/com/hamlog/css/base.css")).toExternalForm();
        java.net.URL themeFile = HamLogApp.class.getResource(
            "/com/hamlog/css/" + theme + ".css");
        scene.getStylesheets().add(base);
        if (themeFile != null) {
            scene.getStylesheets().add(themeFile.toExternalForm());
        }
        // Inline style on root takes priority over CSS, giving live font-size control
        scene.getRoot().setStyle("-fx-font-size: " + cfg.getFontSize() + "px;");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
