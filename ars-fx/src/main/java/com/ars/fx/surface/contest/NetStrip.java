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
        Timeline tick = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
        tick.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((o, was, now) -> { if (now == null) tick.stop(); else tick.play(); });
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
        if (now - opsAt > 10_000) {                                                // the log is read from disk, so not on every tick
            opsAt = now;
            ops.setText(com.ars.fx.net.OperatorStats.line(com.ars.fx.net.OperatorStats.compute(com.ars.fx.data.ContestState.qsos())));
        }
        List<NetNode.ChatMsg> chat = n.chatHistory();
        chatLog.setText(chat.subList(Math.max(0, chat.size() - 3), chat.size()).stream()
                .map(c -> java.time.format.DateTimeFormatter.ofPattern("HHmm").withZone(java.time.ZoneOffset.UTC).format(java.time.Instant.ofEpochMilli(c.ts())) + "Z " + c.station() + ": " + c.text())
                .collect(Collectors.joining("\n")));
    }
}
