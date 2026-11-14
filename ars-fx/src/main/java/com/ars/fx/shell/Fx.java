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

    /**
     * A text field with the focus blinks its caret twice a second, and every blink is a new frame of the whole window, for as long as the field
     * keeps the focus (the log screens focus the callsign box at once). That was about two of the ten frames a second on an otherwise still
     * screen. The caret stays on, solid, instead. Set {@code ui.caretBlink} true in the config to get the blink back.
     */
    public static void steadyCaret(javafx.scene.Scene scene) {
        scene.focusOwnerProperty().addListener((o, was, now) -> {
            if (now instanceof javafx.scene.control.TextInputControl t && !HubConfig.getBool("ui.caretBlink", false)) steady(t);
        });
        if (scene.getFocusOwner() instanceof javafx.scene.control.TextInputControl t && !HubConfig.getBool("ui.caretBlink", false)) steady(t);   // already focused
    }

    /**
     * Turn the blink off on one field, whenever its skin exists. A field can get the focus before its skin is built (the log screens focus the
     * callsign box as they open), and the skin is replaced when the theme changes, so this watches the skin as well as acting at once.
     */
    private static void steady(javafx.scene.control.TextInputControl t) {
        if (t.getProperties().putIfAbsent("fx.steadyCaret", Boolean.TRUE) != null) return;        // once per field
        Runnable apply = () -> { if (t.getSkin() instanceof javafx.scene.control.skin.TextInputControlSkin<?> sk) sk.setCaretAnimating(false); };
        apply.run();
        t.skinProperty().addListener((o, was, now) -> apply.run());
    }

    private static final java.util.List<Runnable> beat = new java.util.concurrent.CopyOnWriteArrayList<>();
    private static Timeline heartbeat;
    /**
     * Run {@code r} on the FX thread once a second, together with everything else that ticks each second. Separate one-second timelines start at
     * different moments, so each fires in its own frame; on this laptop every frame of a 4K window costs about the same whatever changed, so
     * three clocks were three frames a second. One shared timeline puts them all in the same frame. Returns what stops it. Call on the FX thread.
     */
    public static Runnable everySecond(Runnable r) {
        beat.add(r);
        if (heartbeat == null) {
            heartbeat = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                for (Runnable x : beat) { try { x.run(); } catch (RuntimeException ex) { /* one failing tick must not stop the others */ } }
            }));
            heartbeat.setCycleCount(Animation.INDEFINITE);
            heartbeat.play();
        }
        return () -> beat.remove(r);
    }
    /** Like {@link #everySecond} but only while {@code n} is in a window. */
    public static void everySecondWhileShown(Node n, Runnable r) {
        Runnable[] stop = { n.getScene() != null ? everySecond(r) : null };
        n.sceneProperty().addListener((o, was, sc) -> {
            if (sc == null) { if (stop[0] != null) { stop[0].run(); stop[0] = null; } }
            else if (stop[0] == null) stop[0] = everySecond(r);
        });
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
        if (!on || reduced() || !HubConfig.getBool("ui.pulse", false)) {   // off unless asked for: every frame of it is a whole-window present, a third of a core on a laptop
            if (current instanceof Animation a) a.stop();
            n.getProperties().remove("fx.pulse");
            n.setOpacity(1.0);
            return;
        }
        if (current != null) return;
        // Stepped, not a smooth fade: a smooth fade asks the renderer for a new frame 60 times a second for as long as a feed is connected,
        // which on a laptop GPU cost a third of a core for one small dot. Eight steps a cycle (about 7 a second) reads the same and costs a tenth.
        final int steps = 8;
        javafx.animation.Timeline breathe = new javafx.animation.Timeline();
        for (int i = 0; i <= steps; i++)
            breathe.getKeyFrames().add(new javafx.animation.KeyFrame(Duration.millis(1100.0 * i / steps),
                    new javafx.animation.KeyValue(n.opacityProperty(), 1.0 - 0.6 * i / steps, javafx.animation.Interpolator.DISCRETE)));
        breathe.setAutoReverse(true); breathe.setCycleCount(Animation.INDEFINITE);
        n.getProperties().put("fx.pulse", breathe);
        n.sceneProperty().addListener((o, was, now) -> { if (now == null) breathe.pause(); else breathe.play(); });
        if (n.getScene() != null) breathe.play();
    }
}
