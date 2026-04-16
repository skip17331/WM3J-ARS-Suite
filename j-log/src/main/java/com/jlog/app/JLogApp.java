package com.jlog.app;

import com.jlog.db.DatabaseManager;
import com.jlog.i18n.I18n;
import com.jlog.plugin.PluginLoader;
import com.jlog.util.AppConfig;
import com.jlog.util.LoggingConfigurator;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * J-Log — Amateur Radio Logging Application
 * Main JavaFX Application entry point.
 */
public class JLogApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(JLogApp.class);

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

    /** App icon loaded once and shared across all stages. */
    private static Image appIcon;

    /** Returns the app icon, loading it on first call. May return null if resource is missing. */
    public static Image getAppIcon() {
        if (appIcon == null) {
            var url = JLogApp.class.getResourceAsStream("/com/jlog/icons/icon.png");
            if (url != null) {
                appIcon = new Image(url);
            }
        }
        return appIcon;
    }

    /** Convenience: apply icon to a stage if one is available. */
    public static void applyIcon(Stage stage) {
        Image icon = getAppIcon();
        if (icon != null) {
            stage.getIcons().clear();
            stage.getIcons().add(icon);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        applyIcon(primaryStage);
        // When launched by J-Hub the --launched-by-hub flag is present.
        // In that case skip the splash and go directly to the mode chooser.
        boolean launchedByHub = getParameters().getRaw().contains("--launched-by-hub");
        SplashScreen splash = new SplashScreen(primaryStage, launchedByHub);
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
            JLogApp.class.getResource("/com/jlog/css/base.css")).toExternalForm();
        java.net.URL themeFile = JLogApp.class.getResource(
            "/com/jlog/css/" + theme + ".css");
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
