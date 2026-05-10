package com.wm3j.jmap.ui.panels;

import com.wm3j.jmap.service.dx.DxClusterClient;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/**
 * One-line banner that surfaces the WebSocket connection state to J-Hub.
 *
 * Shown only when the connection is down — when the connection is healthy
 * the bar is hidden ({@code visible=false; managed=false}) so the layout
 * collapses cleanly.
 *
 * Polled at 1 Hz from the dashboard's render loop via {@link #update()};
 * no observable bindings, no extra threads.
 *
 * Designed for the "remote display" use case (j-map on a Pi pointing at a
 * main station's j-hub over the LAN) where a dropped link is the most
 * common operational failure and needs to be visible at a glance from
 * across the room.
 */
public class HubStatusBar extends HBox {

    private final DxClusterClient client;
    private final Label dot;
    private final Label text;

    public HubStatusBar(DxClusterClient client) {
        this.client = client;

        getStyleClass().add("jmap-hub-status-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(4, 10, 4, 10));
        // Subtle red wash so it reads at peripheral glance without competing
        // with the map. Inline-styled so we don't need to add CSS for a
        // single conditional element.
        setStyle("-fx-background-color: rgba(220, 50, 60, 0.18); "
               + "-fx-border-color: rgba(220, 50, 60, 0.55); "
               + "-fx-border-width: 0 0 1 0;");

        dot = new Label("●");
        dot.setStyle("-fx-text-fill: #ff4455; -fx-font-size: 11px;");

        text = new Label("Disconnected from j-hub");
        text.setStyle("-fx-text-fill: #ffd0d4; -fx-font-size: 12px; -fx-font-weight: 600;");

        getChildren().addAll(dot, text);

        // Start hidden; updateVisibility() flips it as soon as it's polled.
        setVisible(false);
        setManaged(false);
    }

    /** Refresh the banner from the current client state. Call from the FX
     *  application thread (the dashboard render loop already runs there). */
    public void update() {
        long secs = client.getDisconnectedSeconds();
        boolean down = secs >= 0;
        if (down) {
            String url = client.getHubUrl();
            String human = secs < 60
                ? secs + "s"
                : (secs / 60) + "m " + (secs % 60) + "s";
            text.setText("Disconnected from j-hub" + (url != null ? " (" + url + ")" : "")
                       + " — " + human);
        }
        if (isVisible() != down) {
            setVisible(down);
            setManaged(down);
        }
    }
}
