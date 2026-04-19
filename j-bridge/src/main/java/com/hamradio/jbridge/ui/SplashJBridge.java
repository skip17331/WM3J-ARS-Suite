package com.hamradio.jbridge.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.InputStream;

public class SplashJBridge {

    private final Runnable onComplete;
    private Stage splashStage;

    public SplashJBridge(Runnable onComplete) {
        this.onComplete = onComplete;
    }

    public void show() {
        splashStage = new Stage(StageStyle.TRANSPARENT);

        VBox root = new VBox(12);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40, 70, 40, 70));
        root.setStyle(
            "-fx-background-color: #1e1e2e;" +
            "-fx-border-color: #cba6f7;" +
            "-fx-border-width: 2px;" +
            "-fx-border-radius: 8px;" +
            "-fx-background-radius: 8px;"
        );

        // App icon (skipped gracefully if not yet placed)
        InputStream iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream != null) {
            ImageView iv = new ImageView(new Image(iconStream));
            iv.setFitWidth(72);
            iv.setFitHeight(72);
            iv.setPreserveRatio(true);
            root.getChildren().add(iv);
        }

        Label title = new Label("J-Bridge");
        title.setStyle(
            "-fx-font-size: 48px;" +
            "-fx-font-weight: bold;" +
            "-fx-text-fill: #cba6f7;" +
            "-fx-font-family: 'DejaVu Sans', sans-serif;"
        );

        Label subtitle = new Label("WSJT-X \u2194 ARS Suite Bridge");
        subtitle.setStyle(
            "-fx-font-size: 16px;" +
            "-fx-text-fill: #89b4fa;" +
            "-fx-font-family: 'DejaVu Sans', sans-serif;"
        );

        Label version = new Label("v1.0.0");
        version.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #6c7086;"
        );

        root.getChildren().addAll(title, subtitle, version);

        Scene scene = new Scene(root, 480, 260);
        scene.setFill(Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        applyIcon(splashStage);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(700), root);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.setOnFinished(e -> {
            PauseTransition hold = new PauseTransition(Duration.seconds(1.0));
            hold.setOnFinished(ev -> {
                splashStage.close();
                if (onComplete != null) onComplete.run();
            });
            hold.play();
        });
        fadeIn.play();
    }

    public static void applyIcon(Stage stage) {
        InputStream is = SplashJBridge.class.getResourceAsStream("/icons/icon.png");
        if (is != null) {
            stage.getIcons().add(new Image(is));
        }
    }
}
