package com.ars.fx.surface;

import com.ars.fx.Theme;
import com.ars.fx.Themes;
import com.ars.fx.data.ClusterClient;
import com.ars.fx.data.TelnetFeed;
import com.ars.fx.shell.FeedUi;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The DX cluster's raw console as a popup window: everything the cluster sends and everything we send, plus a
 * command line (e.g. {@code SH/DX 20}, {@code SET/FT8}). It reads the same raw log in the hub or in a window
 * attached to the hub, so it works from any module. One console at a time.
 */
final class ClusterConsole {
    private ClusterConsole() {}

    private static Stage stage;

    static void show(Node owner) {
        if (stage != null) { stage.show(); stage.toFront(); stage.requestFocus(); return; }
        ClusterClient cc = ClusterClient.getInstance();

        // ── status strip ────────────────────────────────────────────────────
        Region dot = new Region(); dot.setMinSize(9, 9); dot.setMaxSize(9, 9);
        Label state = lbl("", "jhub-card-title");
        Label detail = lbl("", "jl-emeta"); detail.setWrapText(true);
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        Label toggle = lbl("", "jl-logbtn"); toggle.setStyle("-fx-cursor:hand;-fx-padding:6 14;");
        toggle.setOnMouseClicked(e -> FeedUi.toggle(cc));
        HBox top = new HBox(8, dot, state, gap, toggle); top.setAlignment(Pos.CENTER_LEFT);

        // ── raw log ─────────────────────────────────────────────────────────
        TextArea log = new TextArea(); log.setEditable(false); log.setWrapText(false);
        log.setStyle("-fx-control-inner-background:#0b0f14;-fx-background-color:#0b0f14;-fx-text-fill:-ars-ok;"
                + "-fx-font-family:'JetBrains Mono';-fx-font-size:12px;-fx-border-color:-ars-border;-fx-border-radius:6;-fx-background-radius:6;");
        VBox.setVgrow(log, Priority.ALWAYS);

        // ── command line ────────────────────────────────────────────────────
        TextField cmd = new TextField(); cmd.getStyleClass().add("jhub-mini-field");
        cmd.setPromptText("cluster command, e.g. SH/DX 20 — Enter to send   (↑ ↓ for history)");
        HBox.setHgrow(cmd, Priority.ALWAYS); cmd.setMaxWidth(Double.MAX_VALUE);
        Label send = lbl("Send", "jl-logbtn"); send.setStyle("-fx-cursor:hand;-fx-padding:6 16;");
        List<String> history = new ArrayList<>(); int[] hpos = {0};
        Runnable doSend = () -> {
            String t = cmd.getText().trim();
            if (t.isEmpty()) return;
            cc.sendCommand(t);
            if (history.isEmpty() || !history.get(history.size() - 1).equals(t)) history.add(t);
            hpos[0] = history.size(); cmd.clear();
        };
        send.setOnMouseClicked(e -> doSend.run());
        cmd.setOnAction(e -> doSend.run());
        cmd.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.UP && !history.isEmpty()) { hpos[0] = Math.max(0, hpos[0] - 1); cmd.setText(history.get(hpos[0])); cmd.positionCaret(cmd.getText().length()); e.consume(); }
            else if (e.getCode() == KeyCode.DOWN && !history.isEmpty()) {
                hpos[0] = Math.min(history.size(), hpos[0] + 1);
                cmd.setText(hpos[0] >= history.size() ? "" : history.get(hpos[0])); cmd.positionCaret(cmd.getText().length()); e.consume();
            }
        });
        HBox cmdRow = new HBox(8, lbl(">", "jhub-card-title"), cmd, send); cmdRow.setAlignment(Pos.CENTER_LEFT);

        // common commands fill the line (nothing is sent until Enter)
        HBox chips = new HBox(6); chips.setAlignment(Pos.CENTER_LEFT);
        for (String c : new String[]{"SH/DX 20", "SET/FT8", "SET/SKIMMER", "SET/NOCW", "HELP"}) {
            Label chip = lbl(c, "jl-clearbtn"); chip.setStyle("-fx-cursor:hand;");
            chip.setOnMouseClicked(e -> { cmd.setText(c); cmd.requestFocus(); cmd.positionCaret(c.length()); });
            chips.getChildren().add(chip);
        }
        Region chipGap = new Region(); HBox.setHgrow(chipGap, Priority.ALWAYS);
        CheckBox follow = new CheckBox("Follow"); follow.setSelected(true);
        Label clear = lbl("Clear", "jl-clearbtn"); clear.setStyle("-fx-cursor:hand;");
        chips.getChildren().addAll(chipGap, follow, clear);

        VBox root = new VBox(10, top, detail, log, cmdRow, chips);
        root.setPadding(new Insets(14, 16, 14, 16));
        root.setStyle("-fx-background-color:-ars-bg;");

        Scene scene = new Scene(root, 800, 540);
        Theme.apply(scene, false);
        com.ars.fx.shell.Tips.install(scene);
        Themes.applyTo(root, "hub");

        Stage st = new Stage();
        st.setTitle("DX Cluster — raw console");
        com.ars.fx.AppIcons.apply(st, null);
        st.setScene(scene);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        stage = st;

        // ── live refresh ────────────────────────────────────────────────────
        long[] last = {0}; int[] resets = {cc.rawResets()};
        clear.setOnMouseClicked(e -> { log.clear(); last[0] = cc.rawSeq(); });
        Timeline tick = new Timeline(new KeyFrame(Duration.millis(250), e -> {
            TelnetFeed.Status s = cc.status();
            dot.setStyle(FeedUi.dot(s));
            state.setText(FeedUi.line(cc));
            detail.setText(s == TelnetFeed.Status.CONNECTED ? "Logged in as " + cc.loginCall() + ". Spots keep flowing to the other windows." : cc.detail());
            detail.setManaged(!detail.getText().isEmpty()); detail.setVisible(!detail.getText().isEmpty());
            toggle.setText(FeedUi.action(cc));

            if (cc.rawResets() != resets[0]) { resets[0] = cc.rawResets(); log.clear(); last[0] = 0; }   // hub restarted / log replaced
            List<TelnetFeed.RawLine> lines = cc.rawSince(last[0]);
            if (lines.isEmpty()) return;
            StringBuilder sb = new StringBuilder();
            for (TelnetFeed.RawLine l : lines) { sb.append(l.text()).append('\n'); last[0] = l.seq(); }
            log.appendText(sb.toString());
            if (log.getLength() > 90_000) log.deleteText(0, log.getLength() - 60_000);   // keep the area light
            if (follow.isSelected()) log.positionCaret(log.getLength());
        }));
        tick.setCycleCount(Animation.INDEFINITE);
        tick.play();
        st.setOnHidden(e -> { tick.stop(); stage = null; });

        st.show();
        cmd.requestFocus();
    }
}
