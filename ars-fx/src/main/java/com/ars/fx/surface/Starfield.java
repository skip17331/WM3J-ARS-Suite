package com.ars.fx.surface;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Random;

/**
 * The night sky behind the Modern (space age) dashboard: a fixed scatter of stars, and every so often a shooting star across the top of the
 * page. Never takes the mouse. The stars are drawn from a fixed seed so the sky does not reshuffle when the window is resized, and the
 * shooting star is skipped when Setup > Appearance > Reduce effects is on.
 */
final class Starfield extends Pane {
    private final Canvas cv = new Canvas();
    private final Region streak = new Region();
    private final Timeline loop;
    private double lastW, lastH;

    Starfield() {
        setMouseTransparent(true);
        setMinSize(0, 0);
        streak.setPrefSize(130, 2);
        streak.setStyle("-fx-background-color: linear-gradient(to right, rgba(255,255,255,0), #ffffff); -fx-background-radius: 2;"
                + "-fx-effect: dropshadow(gaussian, rgba(120,220,255,0.9), 6, 0.3, 0, 0);");
        streak.setRotate(28); streak.setOpacity(0); streak.setManaged(false);
        getChildren().addAll(cv, streak);
        loop = new Timeline(new KeyFrame(Duration.seconds(23), e -> shoot()));
        loop.setCycleCount(Timeline.INDEFINITE);
        sceneProperty().addListener((o, was, sc) -> { if (sc == null) loop.stop(); else if (!com.ars.fx.shell.Fx.reduced()) loop.play(); });
    }

    @Override protected void layoutChildren() {
        double w = getWidth(), h = getHeight();
        if (w < 10 || h < 10 || (w == lastW && h == lastH)) return;
        lastW = w; lastH = h;
        cv.setWidth(w); cv.setHeight(h);
        GraphicsContext g = cv.getGraphicsContext2D();
        g.clearRect(0, 0, w, h);
        Random r = new Random(7);
        int n = (int) (w * h / 5200);
        for (int i = 0; i < n; i++) {
            double x = r.nextDouble() * w, y = r.nextDouble() * h, k = r.nextDouble();
            double s = k > 0.97 ? 2.2 : k > 0.85 ? 1.5 : 0.9;
            Color c = k > 0.93 ? Color.rgb(150, 235, 255) : k > 0.88 ? Color.rgb(200, 170, 255) : Color.WHITE;
            g.setGlobalAlpha(0.18 + 0.7 * r.nextDouble());
            g.setFill(c);
            g.fillOval(x, y, s, s);
        }
        g.setGlobalAlpha(1);
    }

    private void shoot() {
        double w = getWidth(), h = getHeight();
        if (w < 200 || h < 200) return;
        Random r = new Random();
        streak.setLayoutX(0); streak.setLayoutY(0);
        streak.setTranslateX(w * (0.15 + 0.6 * r.nextDouble())); streak.setTranslateY(h * 0.30 * r.nextDouble());
        TranslateTransition mv = new TranslateTransition(Duration.millis(900), streak);
        mv.setByX(300 * Math.cos(Math.toRadians(28))); mv.setByY(300 * Math.sin(Math.toRadians(28))); mv.setInterpolator(Interpolator.EASE_IN);
        FadeTransition fade = new FadeTransition(Duration.millis(900), streak);
        fade.setFromValue(0.95); fade.setToValue(0);
        new ParallelTransition(mv, fade).play();
    }
}
