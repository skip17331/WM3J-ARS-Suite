package com.hamradio.jbridge.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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

        InputStream iconStream = getClass().getResourceAsStream("/icons/icon.png");
        if (iconStream == null) {
            if (onComplete != null) onComplete.run();
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
