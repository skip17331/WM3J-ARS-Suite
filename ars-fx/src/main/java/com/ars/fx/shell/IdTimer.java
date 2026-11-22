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
 * clicking any of them resets the countdown everywhere. Everywhere includes the other windows of a loose / undocked
 * layout, which are separate processes: the time of the last ID is kept in {@code ~/.j-hub/id-reset} and each window reads it every
 * second, so the countdown is worked out from that time (not counted down in memory) and they agree.
 */
public final class IdTimer {
    private IdTimer() {}

    static final int PERIOD = 600;
    private static int seconds = PERIOD;
    /** When the station last identified (a reset in any window), epoch ms; the app's own start counts as an ID, as it always has. */
    private static volatile long lastIdMs = System.currentTimeMillis();
    private static java.nio.file.Path shared = java.nio.file.Path.of(System.getProperty("user.home"), ".j-hub", "id-reset");
    private static Runnable ticker;
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

    public static void reset() {
        lastIdMs = System.currentTimeMillis();
        try { java.nio.file.Files.createDirectories(shared.getParent()); java.nio.file.Files.writeString(shared, Long.toString(lastIdMs)); }
        catch (java.io.IOException ignored) { /* this window still resets; only the other windows will not hear of it */ }
        seconds = PERIOD; notifyUpd();
    }

    /** Seconds left, from the time of the last ID: {@code now} and {@code lastId} in epoch ms. */
    static int remaining(long now, long lastId) { return PERIOD - (int) Math.max(0, (now - lastId) / 1000); }

    /** The latest ID time written by any window, or 0. */
    static long readShared() {
        try { return Long.parseLong(java.nio.file.Files.readString(shared).trim()); } catch (Exception e) { return 0; }
    }

    private static void start() {
        if (ticker != null) return;
        ticker = Fx.everySecond(() -> {
            long f = readShared();
            if (f > lastIdMs) lastIdMs = f;                  // another window identified
            seconds = remaining(System.currentTimeMillis(), lastIdMs);
            notifyUpd();
        });
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
