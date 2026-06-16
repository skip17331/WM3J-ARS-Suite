package com.ars.fx.shell;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Shared station-ID timer — a single 10-minute FCC-ID countdown used on both the
 * J-Log surface and the J-Hub dashboard. One global 1 s ticker drives every
 * on-screen frame; clicking any frame resets the countdown everywhere.
 */
public final class IdTimer {
    private IdTimer() {}

    private static int seconds = 600;
    private static Timeline ticker;
    private static final List<Runnable> updaters = new CopyOnWriteArrayList<>();

    /** Framed, clickable-to-reset countdown that fills its container. */
    public static Region frame() {
        Label timer = lbl(text(), "jl-idtimer"); timer.setMaxWidth(Double.MAX_VALUE); timer.setAlignment(Pos.CENTER);
        Label cap = lbl("click anywhere to identify & reset", "jl-skim-meta"); cap.setMaxWidth(Double.MAX_VALUE); cap.setAlignment(Pos.CENTER);
        VBox box = new VBox(6, timer, cap); box.getStyleClass().add("jl-idframe"); box.setAlignment(Pos.CENTER); box.setMaxWidth(Double.MAX_VALUE);
        Runnable upd = () -> { timer.setText(text()); applyClass(timer); applyClass(box); };
        upd.run();
        box.setOnMouseClicked(e -> reset());
        updaters.add(upd);
        box.sceneProperty().addListener((o, old, sc) -> { if (sc == null) updaters.remove(upd); });
        start();
        return box;
    }

    /** The framed timer wrapped in a "Station ID" drawer with a hint line. */
    public static Node drawer() {
        Label hint = lbl("Identify your station on air at least every 10 minutes.", "jl-skim-meta"); hint.setWrapText(true);
        VBox v = new VBox(10, frame(), hint); v.setPadding(new Insets(4, 2, 4, 2));
        return Shell.drawer("Station ID", "log", "log", text(), true, v);
    }

    public static void reset() { seconds = 600; notifyUpd(); }
    public static String summary() { return text(); }

    private static void start() {
        if (ticker != null) return;
        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> { seconds--; notifyUpd(); }));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }
    private static void notifyUpd() { for (Runnable r : updaters) r.run(); }
    private static void applyClass(Node n) {
        n.getStyleClass().removeAll("warn", "over");
        if (seconds <= 0) n.getStyleClass().add("over");
        else if (seconds <= 60) n.getStyleClass().add("warn");
    }
    private static String text() { return seconds <= 0 ? "ID NOW" : String.format("%d:%02d", seconds / 60, seconds % 60); }
}
