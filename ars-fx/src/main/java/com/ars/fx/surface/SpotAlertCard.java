package com.ars.fx.surface;

import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.SpotAlerts;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The spot-alert card: a small card in the bottom-left corner of the docked window listing the latest alerted spots (watch-list calls
 * and needed spots, {@link SpotAlerts}), with a chime. Clicking a spot uses it, as a click in the spot list does (fills the log entry,
 * tunes the rig). It hides itself a minute after the last alert, or on Dismiss. Like the update card it is an overlay on the host
 * {@link StackPane}, which is rebuilt on every navigation, so {@link #attach} puts it back.
 */
public final class SpotAlertCard {
    private SpotAlertCard() {}

    private static final int MAX_LINES = 4;
    private static final Duration SHOW_FOR = Duration.seconds(60);
    private static final Deque<SpotAlerts.Alert> recent = new ArrayDeque<>();
    private static StackPane host;
    private static VBox card;
    private static PauseTransition hide;

    /** Start watching the cluster for alerts. Call once, after the first window is up. */
    public static void install(StackPane hostPane) {
        host = hostPane;
        ClusterClient.getInstance().addListener("spot-alerts", c -> {
            List<SpotAlerts.Alert> got = SpotAlerts.checkNow(c.spots(), JHubDashboard::needLabel);
            if (got.isEmpty()) return;
            for (SpotAlerts.Alert a : got) { recent.addFirst(a); while (recent.size() > MAX_LINES) recent.removeLast(); }
            SpotAlerts.chime();
            show();
        });
    }

    /** Re-add the card (if alerts are showing) after the host's children were replaced. */
    public static void attach(StackPane hostPane) {
        if (hostPane != null) host = hostPane;
        if (card == null || host == null || host.getChildren().contains(card)) return;
        host.getChildren().add(card);
    }

    private static void show() {
        if (host == null) return;
        if (card != null) host.getChildren().remove(card);
        Label title = lbl("🔔 Spot alert", "ub-title");
        VBox lines = new VBox(3);
        for (SpotAlerts.Alert a : recent) {
            ClusterClient.Spot s = a.spot();
            Label l = lbl(s.call() + "  " + ClusterClient.fmtFreq(s.freqHz()) + "  " + (s.mode() == null ? "" : s.mode())
                    + "  · " + a.reason(), "ub-sub");
            l.setStyle("-fx-cursor:hand;");
            com.ars.fx.shell.Tips.tip(l, "Click to use this spot: fills the log entry and tunes the rig");
            l.setOnMouseClicked(e -> SpotAction.onSpot(l, s.call(), s.freqHz(), s.mode(), s.band()));
            lines.getChildren().add(l);
        }
        Label dismiss = lbl("Dismiss", "ub-link"); dismiss.setStyle("-fx-cursor:hand;");
        dismiss.setOnMouseClicked(e -> close());
        Label settings = lbl("Alert settings", "ub-link"); settings.setStyle("-fx-cursor:hand;");
        settings.setOnMouseClicked(e -> com.ars.fx.shell.Shell.navigate("setup:cluster"));
        VBox c = new VBox(5, title, lines, new HBox(14, dismiss, settings));
        c.getStyleClass().add("ub-card");
        c.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        StackPane.setAlignment(c, Pos.BOTTOM_LEFT);
        StackPane.setMargin(c, new Insets(0, 0, 16, 16));
        card = c;
        host.getChildren().add(card);
        if (hide != null) hide.stop();
        hide = new PauseTransition(SHOW_FOR);
        hide.setOnFinished(e -> close());
        hide.play();
    }

    private static void close() {
        if (host != null && card != null) host.getChildren().remove(card);
        card = null; recent.clear();
        if (hide != null) hide.stop();
    }
}
