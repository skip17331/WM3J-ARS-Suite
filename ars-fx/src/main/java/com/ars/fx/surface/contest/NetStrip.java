package com.ars.fx.surface.contest;

import com.ars.fx.net.ContestNetwork;
import com.ars.fx.net.NetConflicts;
import com.ars.fx.net.NetNode;
import com.ars.fx.net.NetStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The contest network at a glance, on the contest page: whether this station is hosting or joined, the other stations with what each is on,
 * and a warning when one is in a different contest or on the same band and mode. Nothing shows while the network is off.
 */
public final class NetStrip extends VBox {
    private final Label line = lbl("", "ct-note"), others = lbl("", "ct-note"), warn = lbl("", "ct-note"), ops = lbl("", "ct-note"), chatLog = lbl("", "ct-note");
    private final TextField chatIn = new TextField();
    private long opsAt;
    private volatile boolean opsBusy;

    public NetStrip() {
        super(2);
        for (Label l : new Label[]{line, others, warn, ops, chatLog}) { l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE); l.managedProperty().bind(l.textProperty().isNotEmpty()); l.visibleProperty().bind(l.textProperty().isNotEmpty()); }
        line.setStyle("-fx-font-weight:bold;");
        warn.setStyle("-fx-text-fill:-ars-warn;-fx-font-weight:bold;");
        ops.setStyle("-fx-opacity:0.85;");
        chatIn.setPromptText("message to all stations - Enter to send"); chatIn.getStyleClass().add("jhub-mini-field");
        chatIn.setOnAction(e -> { NetNode n = ContestNetwork.node(); if (n != null) n.sendChat(chatIn.getText()); chatIn.clear(); refresh(); });
        getChildren().addAll(line, others, warn, ops, chatLog, chatIn);
        managedProperty().bind(visibleProperty());
        refresh();
        int[] beat = {0};
        com.ars.fx.shell.Fx.everySecondWhileShown(this, () -> { if ((beat[0]++ & 1) == 0) refresh(); });   // every 2 s, in the same frame as the other clocks
    }

    private void refresh() {
        if (ContestNetwork.mode().equals("Off")) { setVisible(false); line.setText(""); others.setText(""); warn.setText(""); ops.setText(""); chatLog.setText(""); chatIn.setManaged(false); chatIn.setVisible(false); return; }
        setVisible(true);
        NetNode n = ContestNetwork.node();
        if (n == null) { line.setText("NETWORK · not running — " + ContestNetwork.startError()); others.setText(""); warn.setText(""); ops.setText(""); chatLog.setText(""); chatIn.setManaged(false); chatIn.setVisible(false); return; }
        chatIn.setManaged(true); chatIn.setVisible(true);
        List<NetStatus> roster = n.roster();
        line.setText("NETWORK · " + n.summary() + (n.online() ? "" : ""));
        line.setStyle("-fx-font-weight:bold;-fx-text-fill:" + (n.online() ? "-ars-ok" : "-ars-warn") + ";");
        others.setText(roster.isEmpty() ? "" : roster.stream().map(s -> s.label() + (s.band() == null || s.band().isEmpty() ? "" : " " + s.band() + " " + s.mode()))
                .collect(Collectors.joining("   ·   ")));
        warn.setText(String.join("\n", NetConflicts.warnings(ContestNetwork.myStatus(), roster)));
        long now = System.currentTimeMillis();
        if (now - opsAt > 10_000 && !opsBusy) {                                    // the log is read from disk and counted: off the FX thread (60 ms at 10,000 QSOs)
            opsAt = now; opsBusy = true;
            Thread t = new Thread(() -> {
                String text = null;                                               // null: it failed, keep what was shown
                try { text = com.ars.fx.net.OperatorStats.line(com.ars.fx.net.OperatorStats.compute(com.ars.fx.data.ContestState.qsos())); }
                catch (RuntimeException ignored) { }
                String done = text;
                javafx.application.Platform.runLater(() -> { if (done != null) ops.setText(done); opsBusy = false; });
            }, "net-ops-stats");
            t.setDaemon(true); t.start();
        }
        List<NetNode.ChatMsg> chat = n.chatHistory();
        chatLog.setText(chat.subList(Math.max(0, chat.size() - 3), chat.size()).stream()
                .map(c -> java.time.format.DateTimeFormatter.ofPattern("HHmm").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(c.ts())) + "Z " + c.station() + ": " + c.text())
                .collect(Collectors.joining("\n")));
    }
}
