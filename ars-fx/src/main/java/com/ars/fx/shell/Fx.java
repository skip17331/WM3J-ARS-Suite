package com.ars.fx.shell;

import com.ars.fx.data.HubConfig;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.Objects;

/**
 * Small, shared "something just happened" effects: a new spot slides in and glows, a changed count flashes, a live status light breathes.
 * Every one is short or tied to the node being on screen, and all of them switch off with Setup > Appearance > Reduce effects.
 */
public final class Fx {
    private Fx() {}

    /** How long a new list row is still "new": a strong glow for the first part, a faint one for the rest. */
    private static final long FRESH_MS = 2600, FRESH_STRONG_MS = 1000, SLIDE_MS = 400;

    /** The "Reduce effects" setting (Setup > Appearance): no motion and no glow on arrivals. */
    public static boolean reduced() { return HubConfig.getBool("ui.reduceEffects", false); }

    /**
     * A list row for something that arrived at {@code arrivalMs}: it slides in, glows in the accent colour, then settles. How far along it is comes
     * from the arrival time, so a list that is rebuilt a moment later shows the same row part-way through, not starting again; a row older than a
     * couple of seconds is left alone, so opening a screen does not light up old news.
     */
    public static void fresh(Node row, long arrivalMs) {
        if (reduced()) return;
        long age = System.currentTimeMillis() - arrivalMs;
        if (age < 0 || age >= FRESH_MS) return;
        if (age < SLIDE_MS) {                                            // only a row that really is new moves
            TranslateTransition slide = new TranslateTransition(Duration.millis(260), row);
            slide.setFromX(18); slide.setToX(0);
            FadeTransition fade = new FadeTransition(Duration.millis(260), row);
            fade.setFromValue(0); fade.setToValue(1);
            slide.play(); fade.play();
        }
        boolean strong = age < FRESH_STRONG_MS;
        row.getStyleClass().add(strong ? "fresh-a" : "fresh-b");
        Timeline steps = new Timeline();
        if (strong) steps.getKeyFrames().add(new KeyFrame(Duration.millis(FRESH_STRONG_MS - age), e -> {
            row.getStyleClass().remove("fresh-a"); row.getStyleClass().add("fresh-b");
        }));
        steps.getKeyFrames().add(new KeyFrame(Duration.millis(FRESH_MS - age), e -> {
            row.getStyleClass().removeAll("fresh-a", "fresh-b");
        }));
        steps.play();
    }

    /** Set a label's text; if it changed (and wasn't empty before), pop it. */
    public static void setAndFlash(Label l, String text) {
        String was = l.getText();
        if (Objects.equals(was, text)) return;
        l.setText(text);
        if (was != null && !was.isEmpty()) flash(l);
    }

    /** The value's colour turns to the accent for a moment and it swells slightly and settles. */
    public static void flash(Label l) {
        if (reduced()) return;
        if (!l.getStyleClass().contains("flash")) l.getStyleClass().add("flash");
        ScaleTransition pop = new ScaleTransition(Duration.millis(140), l);
        pop.setToX(1.12); pop.setToY(1.12); pop.setAutoReverse(true); pop.setCycleCount(2);
        pop.play();
        PauseTransition off = (PauseTransition) l.getProperties().computeIfAbsent("fx.flashOff", k -> new PauseTransition(Duration.millis(900)));
        off.setOnFinished(e -> l.getStyleClass().remove("flash"));
        off.playFromStart();                                             // a second change within the time restarts it
    }

    /**
     * A live status light breathes while {@code on}. The animation runs only while the node is in a window, and is paused when it is taken out, so
     * a light that is rebuilt a lot (a screen that refreshes itself) never leaves animations running behind it. Safe to call again with the same value.
     */
    public static void pulse(Node n, boolean on) {
        Object current = n.getProperties().get("fx.pulse");
        if (!on || reduced()) {
            if (current instanceof Animation a) a.stop();
            n.getProperties().remove("fx.pulse");
            n.setOpacity(1.0);
            return;
        }
        if (current != null) return;
        FadeTransition breathe = new FadeTransition(Duration.millis(1100), n);
        breathe.setFromValue(1.0); breathe.setToValue(0.4);
        breathe.setAutoReverse(true); breathe.setCycleCount(Animation.INDEFINITE);
        n.getProperties().put("fx.pulse", breathe);
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) breathe.pause(); else breathe.play(); });
        if (n.getScene() != null) breathe.play();
    }
}
