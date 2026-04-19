package com.hamradio.modem.ui;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.InputStream;

public class SplashScreen {

    private final Stage primaryStage;
    private final ModemAppContext context;
    private final boolean launchedByHub;
    private Stage splashStage;

    public SplashScreen(Stage primaryStage, ModemAppContext context) {
        this(primaryStage, context, false);
    }

    public SplashScreen(Stage primaryStage, ModemAppContext context, boolean launchedByHub) {
        this.primaryStage  = primaryStage;
        this.context       = context;
        this.launchedByHub = launchedByHub;
    }

    public void show() {
        if (launchedByHub) {
            Platform.runLater(this::launchMainWindow);
            return;
        }

        splashStage = new Stage(StageStyle.TRANSPARENT);

        InputStream iconStream = getClass().getResourceAsStream("/com/hamradio/modem/icons/icon.png");
        if (iconStream == null) {
            Platform.runLater(this::launchMainWindow);
            return;
        }

        Image img = new Image(iconStream);
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(img.getWidth());
        iv.setFitHeight(img.getHeight());

        StackPane root = new StackPane(iv);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root, img.getWidth(), img.getHeight());
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

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
