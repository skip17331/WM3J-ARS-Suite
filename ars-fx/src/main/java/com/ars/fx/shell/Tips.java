package com.ars.fx.shell;

import javafx.animation.PauseTransition;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.Map;

/**
 * Tooltips for the whole app from ONE place. A single shared tooltip follows the pointer: whatever node is under it
 * is resolved to text when the pointer settles, so it covers every button, including rows and labels rebuilt later by
 * a data refresh. Text comes from, in order: a {@code "tip"} property on the node or one of its parents
 * ({@code node.getProperties().put("tip", "…")}); the full text of a label that is being cut short with "…"; or, for a
 * clickable label, the description in {@link #HELP} for its caption.
 */
public final class Tips {
    private Tips() {}

    /** Put an explicit tip on any node (or one of its parents). */
    public static <T extends Node> T tip(T node, String text) { node.getProperties().put("tip", text); return node; }

    /** What the buttons do, keyed by their caption. Only clickable labels are matched, so plain text is never mistaken for a button. */
    private static final Map<String, String> HELP = Map.ofEntries(
            Map.entry("Connect", "Connect to the server and start receiving"),
            Map.entry("Disconnect", "Disconnect and stop trying to reconnect"),
            Map.entry("Stop", "Stop trying to connect"),
            Map.entry("Console", "Open the raw feed and a command line in a pop-up window"),
            Map.entry("Open J-Log →", "Open the full J-Log"),
            Map.entry("⇄ Contest mode", "Switch to contest logging — you pick the contest and which database it logs to"),
            Map.entry("Normal log", "Leave contest mode and go back to normal logging"),
            Map.entry("← Normal log", "Go back to normal logging"),
            Map.entry("Change contest", "Pick a different contest (this contest's QSOs stay logged)"),
            Map.entry("Export Cabrillo ⤓", "Export this contest's log as a Cabrillo file for submission"),
            Map.entry("Database tools ›", "Open J-Hub ▸ Data: database info, backup, export, upload and archive"),
            Map.entry("Switch database", "Choose a different contest database, or start a new one"),
            Map.entry("Log QSO ⏎", "Save this QSO to the log (Enter)"),
            Map.entry("Clear", "Clear the entry fields"),
            Map.entry("Select shown", "Tick every satellite in the list as it is filtered now"),
            Map.entry("Clear shown", "Untick every satellite in the list as it is filtered now"),
            Map.entry("Reset to defaults", "Forget your choice and use the default satellites"),
            Map.entry("Refresh now", "Download fresh orbital data from Celestrak and AMSAT"),
            Map.entry("Archive", "Move this database into storage (kept, and restorable)"),
            Map.entry("Restore", "Move this database back to the usable list"),
            Map.entry("Open folder", "Open the folder in your file manager"),
            Map.entry("Open archive folder", "Open the archive folder in your file manager"),
            Map.entry("Open J-Log", "Open J-Log"),
            Map.entry("Export ADIF", "Export every QSO as an ADIF file"),
            Map.entry("Export CSV", "Export every QSO as a CSV file for a spreadsheet"),
            Map.entry("Upload", "Upload your log to this service"),
            Map.entry("Apply server", "Use the server and login typed above and reconnect"),
            Map.entry("+ Spot", "Send a spot to the cluster"),
            Map.entry("Create & use", "Create this database and log to it"),
            Map.entry("Cancel", "Close without changing anything"),
            Map.entry("Send", "Send the line to the server (Enter)"),
            Map.entry("Follow", "Keep the newest line in view"),
            Map.entry("Choose satellites in J-Hub ›", "Choose which satellites J-Sat tracks, and refresh orbital data"),
            Map.entry("← Dashboard", "Back to the J-Hub dashboard"),
            Map.entry("Stay", "Stay in contest mode"),
            Map.entry("Leave", "Leave contest mode"),
            Map.entry("›", "Expand or collapse"),
            Map.entry("‹", "Collapse this panel"));

    private static boolean looksLikeButton(Label l) {
        if (l.getOnMouseClicked() != null || l.getOnMousePressed() != null) return true;
        for (String c : l.getStyleClass()) if (c.contains("btn") || c.equals("jhub-open") || c.equals("cfg-select") || c.equals("sx-theme")) return true;
        return false;
    }

    /** True when the label is too narrow for its text, so JavaFX shows "…". Measured: Label has no truncation flag. */
    private static boolean isCut(Label l, String text) {
        if (l.isWrapText() || l.getWidth() <= 0) return false;
        javafx.scene.text.Text probe = new javafx.scene.text.Text(l.getText());
        probe.setFont(l.getFont());
        double room = l.getWidth() - l.getInsets().getLeft() - l.getInsets().getRight()
                - (l.getGraphic() != null ? l.getGraphic().prefWidth(-1) + l.getGraphicTextGap() : 0);
        return probe.getLayoutBounds().getWidth() > room + 0.5;
    }

    /** The tip for a node under the pointer, or null. Walks up through parents so a tip on a row covers its icon and caption. */
    static String resolve(Node n) {
        for (int depth = 0; n != null && depth < 8; n = n.getParent(), depth++) {
            Object t = n.getProperties().get("tip");
            if (t instanceof String s && !s.isBlank()) return s;
            if (n instanceof Label l) {
                String text = l.getText() == null ? "" : l.getText().trim();
                boolean cut = !text.isEmpty() && isCut(l, text);
                String help = looksLikeButton(l) ? HELP.get(text) : null;
                if (help != null) return cut ? text + " — " + help : help;
                if (cut) return text;
                if (l.getOnMouseClicked() != null && l.getStyleClass().contains("sx-theme")) return "Switch the colour scheme";
            }
        }
        return null;
    }

    /** Install on a scene. Safe to call more than once for different scenes. */
    public static void install(Scene scene) {
        Tooltip tip = new Tooltip();
        tip.setWrapText(true); tip.setMaxWidth(340);
        tip.setStyle("-fx-background-color:#11161d;-fx-text-fill:#e7ecf2;-fx-font-size:12px;-fx-background-radius:6;-fx-padding:6 10;"
                + "-fx-border-color:#2c3540;-fx-border-radius:6;-fx-border-width:1;");
        PauseTransition delay = new PauseTransition(Duration.millis(550));
        Node[] over = {null}; double[] at = {0, 0};

        Runnable hide = () -> { delay.stop(); tip.hide(); };
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            Node target = e.getPickResult().getIntersectedNode();
            at[0] = e.getScreenX(); at[1] = e.getScreenY();
            if (target == over[0] && tip.isShowing()) return;
            if (target != over[0]) { hide.run(); over[0] = target; }
            delay.setOnFinished(ev -> {
                String text = resolve(over[0]);
                if (text == null || over[0] == null || scene.getWindow() == null) return;
                tip.setText(text);
                tip.show(scene.getWindow(), at[0] + 12, at[1] + 18);
            });
            delay.playFromStart();
        });
        for (EventType<MouseEvent> t : java.util.List.of(MouseEvent.MOUSE_PRESSED, MouseEvent.MOUSE_EXITED, MouseEvent.MOUSE_DRAGGED))
            scene.addEventFilter(t, e -> { hide.run(); if (t == MouseEvent.MOUSE_EXITED) over[0] = null; });
        scene.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, e -> hide.run());
    }
}
