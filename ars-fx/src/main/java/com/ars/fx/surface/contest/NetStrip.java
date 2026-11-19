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
    /** The messages: a card with a header that folds it away, and a two-line window onto the whole history that scrolls. */
    private final Label chatHead = lbl("", "ct-note");
    private final javafx.scene.control.ScrollPane chatScroll = new javafx.scene.control.ScrollPane(chatLog);
    private final VBox chatCard = new VBox(2, chatHead, chatScroll);
    private static final String KEY_CHAT_OPEN = "net.chat.open";
    private int chatSeen; private long chatShown = -1;                  // messages already read (collapsed: the rest are "new"), and how many the card last drew
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
        chatHead.setStyle("-fx-cursor:hand;-fx-font-weight:bold;");
        chatScroll.setFitToWidth(true); chatScroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        chatScroll.setPrefHeight(44); chatScroll.setMinHeight(44); chatScroll.setMaxHeight(44);
        chatScroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        chatCard.managedProperty().bind(chatCard.visibleProperty()); chatCard.setVisible(false);
        chatHead.setOnMouseClicked(e -> { com.ars.fx.data.HubConfig.setBool(KEY_CHAT_OPEN, !chatOpen()); chatShown = -1; refresh(); });
        getChildren().addAll(line, others, warn, ops, chatCard, chatIn);
        managedProperty().bind(visibleProperty());
        refresh();
        int[] beat = {0};
        com.ars.fx.shell.Fx.everySecondWhileShown(this, () -> { if ((beat[0]++ & 1) == 0) refresh(); });   // every 2 s, in the same frame as the other clocks
    }

    private static boolean chatOpen() { return com.ars.fx.data.HubConfig.getBool(KEY_CHAT_OPEN, true); }

    private void refresh() {
        if (ContestNetwork.mode().equals("Off")) { setVisible(false); line.setText(""); others.setText(""); warn.setText(""); ops.setText(""); chatLog.setText(""); chatCard.setVisible(false); chatIn.setManaged(false); chatIn.setVisible(false); return; }
        setVisible(true);
        NetNode n = ContestNetwork.node();
        if (n == null) { line.setText("NETWORK · not running — " + ContestNetwork.startError()); others.setText(""); warn.setText(""); ops.setText(""); chatLog.setText(""); chatCard.setVisible(false); chatIn.setManaged(false); chatIn.setVisible(false); return; }
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
        boolean open = chatOpen();
        if (open) chatSeen = chat.size();
        chatCard.setVisible(!chat.isEmpty());
        chatScroll.setVisible(open); chatScroll.setManaged(open);
        int fresh = Math.max(0, chat.size() - chatSeen);
        chatHead.setText((open ? "▾ " : "▸ ") + "MESSAGES · " + chat.size() + (fresh > 0 ? " · " + fresh + " new" : ""));
        long stamp = chat.size() * 31L + (chat.isEmpty() ? 0 : chat.get(chat.size() - 1).ts());      // the cap of 100 keeps the size still while the messages move on
        if (stamp != chatShown) {                                            // redraw only when a message came, so the scroll position is left alone
            chatShown = stamp;
            java.time.format.DateTimeFormatter hm = java.time.format.DateTimeFormatter.ofPattern("HHmm").withZone(java.time.ZoneOffset.UTC);
            chatLog.setText(chat.stream().map(c -> hm.format(java.time.Instant.ofEpochMilli(c.ts())) + "Z " + c.station() + ": " + c.text())
                    .collect(Collectors.joining("\n")));
            javafx.application.Platform.runLater(() -> chatScroll.setVvalue(1));      // the newest at the bottom
        }
    }
}
