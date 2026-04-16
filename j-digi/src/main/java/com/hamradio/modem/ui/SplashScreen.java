package com.hamradio.modem.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * J-Digi splash screen — fades in, displays app identity for ~1.8 s,
 * then hands off to the main window and starts the hub connection
 * and audio engine in a background thread.
 */
public class SplashScreen {

    private final Stage primaryStage;
    private final ModemAppContext context;
    private Stage splashStage;

    public SplashScreen(Stage primaryStage, ModemAppContext context) {
        this.primaryStage = primaryStage;
        this.context = context;
    }

    public void show() {
        splashStage = new Stage(StageStyle.UNDECORATED);

        Label title = new Label("J-Digi");
        title.setStyle(
            "-fx-font-size: 48px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #00dcff;" +
            "-fx-font-family: 'DejaVu Sans', sans-serif;"
        );

        Label subtitle = new Label("ARS Digital Modem");
        subtitle.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #aaccff;" +
            "-fx-font-family: 'DejaVu Sans', sans-serif;"
        );

        Label version = new Label("v0.1.0");
        version.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #668899;"
        );

        VBox root = new VBox(12, title, subtitle, version);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50, 60, 50, 60));
        root.setStyle(
            "-fx-background-color: #0d1a2e;" +
            "-fx-border-color: #00aacc;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );

        Scene scene = new Scene(root, 440, 240);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        // Re-apply undecorated+transparent after scene set
        splashStage.show();

        ModemAppContext.applyIcon(splashStage);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(800), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            Platform.runLater(this::launchMainWindow);
        });
        fadeIn.play();
    }

    private void launchMainWindow() {
        splashStage.close();
        ModemAppContext.applyIcon(primaryStage);
        MainWindow window = new MainWindow(context.getModemService());
        window.show(primaryStage);

        // Connect to J-Hub and start audio in a background thread.
        // startupSafe() handles all exceptions and posts status to the decode log.
        Thread startupThread = new Thread(context.getModemService()::startupSafe, "jdigi-startup");
        startupThread.setDaemon(true);
        startupThread.start();
    }
}
