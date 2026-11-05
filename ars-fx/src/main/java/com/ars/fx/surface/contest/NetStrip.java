package com.ars.fx.surface.contest;

import com.ars.fx.net.ContestNetwork;
import com.ars.fx.net.NetConflicts;
import com.ars.fx.net.NetNode;
import com.ars.fx.net.NetStatus;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Label;
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
    private final Label line = lbl("", "ct-note"), others = lbl("", "ct-note"), warn = lbl("", "ct-note");

    public NetStrip() {
        super(2);
        for (Label l : new Label[]{line, others, warn}) { l.setWrapText(true); l.setMaxWidth(Double.MAX_VALUE); l.managedProperty().bind(l.textProperty().isNotEmpty()); l.visibleProperty().bind(l.textProperty().isNotEmpty()); }
        line.setStyle("-fx-font-weight:bold;");
        warn.setStyle("-fx-text-fill:-ars-warn;-fx-font-weight:bold;");
        getChildren().addAll(line, others, warn);
        managedProperty().bind(visibleProperty());
        refresh();
        Timeline tick = new Timeline(new KeyFrame(Duration.seconds(2), e -> refresh()));
        tick.setCycleCount(Animation.INDEFINITE);
        sceneProperty().addListener((o, was, now) -> { if (now == null) tick.stop(); else tick.play(); });
    }

    private void refresh() {
        if (ContestNetwork.mode().equals("Off")) { setVisible(false); line.setText(""); others.setText(""); warn.setText(""); return; }
        setVisible(true);
        NetNode n = ContestNetwork.node();
        if (n == null) { line.setText("NETWORK · not running — " + ContestNetwork.startError()); others.setText(""); warn.setText(""); return; }
        List<NetStatus> roster = n.roster();
        line.setText("NETWORK · " + n.summary() + (n.online() ? "" : ""));
        line.setStyle("-fx-font-weight:bold;-fx-text-fill:" + (n.online() ? "-ars-ok" : "-ars-warn") + ";");
        others.setText(roster.isEmpty() ? "" : roster.stream().map(s -> s.label() + (s.band() == null || s.band().isEmpty() ? "" : " " + s.band() + " " + s.mode()))
                .collect(Collectors.joining("   ·   ")));
        warn.setText(String.join("\n", NetConflicts.warnings(ContestNetwork.myStatus(), roster)));
    }
}
