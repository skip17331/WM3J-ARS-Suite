package com.jlog.app;

import com.jlog.i18n.I18n;
import com.jlog.util.AppConfig;
import javafx.fxml.FXMLLoader;
import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Splash screen that fades in, shows the app name, then presents
 * the mode chooser (Normal Log / Contest Log).
 *
 * When {@code launchedByHub} is {@code true} the splash animation is skipped
 * and the mode chooser appears immediately.  This is triggered by the
 * {@code --launched-by-hub} command-line flag that J-Hub passes when it
 * starts J-Log as a child process.
 */
public class SplashScreen {

    private static final Logger log = LoggerFactory.getLogger(SplashScreen.class);

    private final Stage primaryStage;
    private final boolean launchedByHub;
    private Stage splashStage;

    public SplashScreen(Stage primaryStage) {
        this(primaryStage, false);
    }

    public SplashScreen(Stage primaryStage, boolean launchedByHub) {
        this.primaryStage  = primaryStage;
        this.launchedByHub = launchedByHub;
    }

    public void show() {
        if (launchedByHub) {
            // Skip splash when launched by J-Hub
            javafx.application.Platform.runLater(this::showModeChooser);
            return;
        }

        splashStage = new Stage(StageStyle.UNDECORATED);

        Label title   = new Label("j-Log");
        title.getStyleClass().add("splash-title");

        Label subtitle = new Label(I18n.get("splash.subtitle"));
        subtitle.getStyleClass().add("splash-subtitle");

        Label version = new Label("v1.0.0");
        version.getStyleClass().add("splash-version");

        VBox root = new VBox(16, title, subtitle, version);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.getStyleClass().add("splash-root");

        Scene scene = new Scene(root, 480, 280);
        JLogApp.applyTheme(scene);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        // Fade in then show mode chooser after 1.8 s
        FadeTransition ft = new FadeTransition(Duration.millis(900), root);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.setOnFinished(e -> {
            try {
                Thread.sleep(900);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            javafx.application.Platform.runLater(this::showModeChooser);
        });
        ft.play();
    }

    private void showModeChooser() {
        if (splashStage != null) {
            splashStage.close();
        }

        Stage chooser = new Stage();
        chooser.setTitle(I18n.get("mode.chooser.title"));
        JLogApp.applyIcon(chooser);

        Label prompt = new Label(I18n.get("mode.chooser.prompt"));
        prompt.getStyleClass().add("chooser-prompt");

        Button normalBtn  = new Button(I18n.get("mode.normal"));
        Button contestBtn = new Button(I18n.get("mode.contest"));

        normalBtn.setPrefWidth(200);
        contestBtn.setPrefWidth(200);
        normalBtn.getStyleClass().add("chooser-button");
        contestBtn.getStyleClass().add("chooser-button");

        normalBtn.setOnAction(e -> {
            launchNormalLog();
            chooser.close();
        });
        contestBtn.setOnAction(e -> {
            launchContestLog();
            chooser.close();
        });

        VBox box = new VBox(20, prompt, normalBtn, contestBtn);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        Scene scene = new Scene(box, 340, 220);
        JLogApp.applyTheme(scene);
        chooser.setScene(scene);
        chooser.centerOnScreen();
        chooser.show();
    }

    private void launchNormalLog() {
        try {
            AppConfig.getInstance().setCurrentMode("NORMAL");
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/jlog/fxml/NormalLog.fxml"),
                I18n.getBundle());
            Scene scene = new Scene(loader.load());
            JLogApp.applyTheme(scene);
            restoreWindow(primaryStage, scene, "NormalLog");
            primaryStage.setTitle("j-Log — Normal Log");
            primaryStage.show();
            log.info("Normal Log window opened");
        } catch (Exception ex) {
            log.error("Failed to open Normal Log", ex);
        }
    }

    private void launchContestLog() {
        try {
            AppConfig.getInstance().setCurrentMode("CONTEST");
            // Show contest chooser dialog before opening main window
            ContestChooser cc = new ContestChooser(primaryStage);
            cc.show();
        } catch (Exception ex) {
            log.error("Failed to open Contest Log", ex);
        }
    }

    /** Restore saved window geometry, or use defaults. */
    private void restoreWindow(Stage stage, Scene scene, String key) {
        AppConfig cfg = AppConfig.getInstance();
        stage.setScene(scene);
        stage.setWidth(cfg.getWindowWidth(key, 1280));
        stage.setHeight(cfg.getWindowHeight(key, 900));
        stage.setX(cfg.getWindowX(key, 50));
        stage.setY(cfg.getWindowY(key, 50));

        // Save on close
        stage.widthProperty().addListener((o, ov, nv) -> cfg.setWindowWidth(key, nv.intValue()));
        stage.heightProperty().addListener((o, ov, nv) -> cfg.setWindowHeight(key, nv.intValue()));
        stage.xProperty().addListener((o, ov, nv) -> cfg.setWindowX(key, nv.intValue()));
        stage.yProperty().addListener((o, ov, nv) -> cfg.setWindowY(key, nv.intValue()));
    }
}
