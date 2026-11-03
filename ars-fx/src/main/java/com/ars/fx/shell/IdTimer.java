package com.ars.fx.shell;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Shared station-ID timer — a single 10-minute FCC-ID countdown used on both the
 * top bar of every surface. One global 1 s ticker drives every on-screen button;
 * clicking any of them resets the countdown everywhere.
 */
public final class IdTimer {
    private IdTimer() {}

    private static int seconds = 600;
    private static Timeline ticker;
    private static final List<Runnable> updaters = new CopyOnWriteArrayList<>();

    /** Compact countdown for the top bar; click it after you identify to start the 10 minutes again. */
    public static Label button() {
        Label b = lbl("", "sx-theme", "sx-idbtn"); b.setStyle("-fx-cursor:hand;");
        Tips.tip(b, "Station ID: identify at least every 10 minutes. Click after you ID to reset the timer.");
        Runnable upd = () -> { b.setText(text()); applyClass(b); };
        upd.run();
        b.setOnMouseClicked(e -> reset());
        updaters.add(upd);
        b.sceneProperty().addListener((o, old, sc) -> { if (sc == null) updaters.remove(upd); });
        start();
        return b;
    }

    public static void reset() { seconds = 600; notifyUpd(); }

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
        else if (seconds <= 120) n.getStyleClass().add("warn");
        // blink: lit on even seconds, dimmed on odd (the 1 s ticker drives it)
        n.getStyleClass().remove("off");
        if (seconds <= 120 && (seconds & 1) == 1) n.getStyleClass().add("off");
    }
    private static String text() { return seconds <= 0 ? "ID NOW" : String.format("ID %d:%02d", seconds / 60, seconds % 60); }
}
