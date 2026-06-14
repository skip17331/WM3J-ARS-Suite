package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import com.ars.fx.shell.Waterfall;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Bridge WSJT-X/FT8 bridge (handoff shot 05), mock data. */
public final class JBridgeView {
    private JBridgeView() {}

    public static Region build() {
        Region dock = Shell.dock("bridge");
        String[][] stats = {{"DIAL","14.074","bridge"},{"MODE","FT8"},{"DECODES","12"},{"WSJT-X","● linked","ok"}};
        HBox top = Shell.topBar("bridge","bridge","J-Bridge","WSJT-X bridge · FT8", stats, "17:00:34");

        VBox center = new VBox(13, wfHeader(), waterfall(), seqBar(), tables(), footer());
        center.setPadding(new Insets(16, 18, 16, 18)); center.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(center, Priority.ALWAYS);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, center, rail);
    }

    private static Region wfHeader() {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(lbl("WATERFALL","wf-eyebrow"), sp, lbl("200–2800 Hz · 14.074 MHz","wf-scale")); h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
    private static Region waterfall() {
        Canvas cv = Waterfall.canvas(1010, 130, "ft8",
            new double[][]{{0.18,8,0.9},{0.32,8,0.8},{0.45,9,1.0},{0.6,8,0.7},{0.72,8,0.85},{0.85,8,0.6}});
        StackPane wf = new StackPane(cv); wf.setMaxWidth(Double.MAX_VALUE);
        HBox scale = new HBox(); for (String s : Mock.BRIDGE_SCALE){ Label l = lbl(s,"wf-scale"); HBox.setHgrow(l,Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); scale.getChildren().add(l); }
        ((Label)scale.getChildren().get(scale.getChildren().size()-1)).setAlignment(Pos.CENTER_RIGHT);
        scale.getChildren().add(lbl("Hz","wf-scale"));
        return new VBox(4, wf, scale);
    }
    private static Region seqBar() {
        Region prog = progress(0.72);
        VBox lblBox = new VBox(0); HBox.setHgrow(lblBox, Priority.ALWAYS);
        HBox line = new HBox(6, lbl("15 s FT8 period ·","jb-seq-lbl"), lbl("EVEN","jb-seq-b"), lbl("slot","jb-seq-lbl")); line.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox under = new HBox(line, sp, lbl("11s to next","jb-seq-lbl")); under.setAlignment(Pos.CENTER_LEFT);
        lblBox.getChildren().addAll(prog, under); VBox.setMargin(under, new Insets(6,0,0,0));
        HBox slot = new HBox(2, slotBtn("Even",true), slotBtn("Odd",false)); slot.getStyleClass().add("jb-slot");
        HBox entx = new HBox(9, switchNode(false), lbl("Enable Tx","jb-entx-lbl")); entx.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(16, lblBox, slot, entx); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private static Label slotBtn(String t, boolean on){ Label l = lbl(t,"jb-slot-b"); if(on) l.getStyleClass().add("on"); return l; }

    private static Region tables() {
        HBox h = new HBox(0, tableCard("BAND ACTIVITY", Mock.BAND_ACTIVITY, true), tableCard("RX FREQUENCY", Mock.RX_FREQUENCY, false));
        h.setSpacing(0); HBox.setMargin(h.getChildren().get(1), new Insets(0,0,0,16)); VBox.setVgrow(h, Priority.ALWAYS); h.setFillHeight(true);
        return h;
    }
    private static final double[] TC = {44,40,40,52};
    private static Region tableCard(String title, String[][] rows, boolean collapseDash) {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox hd = new HBox(lbl(title,"jb-panel-hd"), sp, lbl("UTC · dB · DT · Hz","jb-panel-hd")); hd.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox(); tbl.getStyleClass().add("jb-tbl"); VBox.setVgrow(tbl, Priority.ALWAYS);
        for (String[] r : rows) {
            HBox row = new HBox(8); row.getStyleClass().add("jb-row"); if (r[5].equals("cq")) row.getStyleClass().add("cq"); row.setAlignment(Pos.CENTER_LEFT);
            for (int i=0;i<4;i++){ Label c = lbl(r[i],"jb-cell"); c.setMinWidth(TC[i]); row.getChildren().add(c); }
            Label msg = lbl(r[4],"jb-msg"); if (r[5].equals("cq")) msg.getStyleClass().add("cq"); else if (r[5].equals("me")) msg.getStyleClass().add("me");
            HBox.setHgrow(msg, Priority.ALWAYS); msg.setMaxWidth(Double.MAX_VALUE); row.getChildren().add(msg);
            tbl.getChildren().add(row);
        }
        VBox card = new VBox(8, hd, tbl); HBox.setHgrow(card, Priority.ALWAYS); card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private static Region footer() {
        VBox left = new VBox(2, lbl("No QSO","jb-noqso"), lbl("Double-click a CQ decode to call","jb-noqso-sub"));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox top = new HBox(left, sp, lbl("RECEIVING","jb-recv")); top.setAlignment(Pos.CENTER_LEFT);
        HBox tx = new HBox(8, lbl("Tx6","jb-txn-tag"), lbl("CQ WM3J FN…","jb-cell")); tx.getStyleClass().add("jb-txn"); tx.setAlignment(Pos.CENTER_LEFT); tx.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(11, top, tx);
        box.setPadding(new Insets(14,16,14,16)); box.setStyle("-fx-background-color:-ars-surface-1; -fx-border-color:-ars-border; -fx-border-width:1; -fx-background-radius:10; -fx-border-radius:10;");
        return box;
    }

    private static Node[] railDrawers() {
        VBox link = new VBox();
        for (String[] d : Mock.WSJTX_LINK) link.getChildren().add(JDigiView.kvRow(d[0], d[1], d[2].equals("y")));
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("WSJT-X link","bridge","bridge","Connected", true, link));
        rail.addAll(Shell.instruments(50));
        return rail.toArray(new Node[0]);
    }

    // ---- helpers -----------------------------------------------------------
    private static Region progress(double frac) {
        Region track = new Region(); track.getStyleClass().add("jb-seq-track");
        Region fill = new Region(); fill.getStyleClass().add("jb-seq-fill");
        Pane p = new Pane(track, fill); p.setMinHeight(6); p.setMaxHeight(6); p.setMaxWidth(Double.MAX_VALUE);
        p.widthProperty().addListener((o,ov,nv)->{ double w=nv.doubleValue(); track.resizeRelocate(0,0,w,6); fill.resizeRelocate(0,0,w*frac,6); });
        return p;
    }
    private static Region switchNode(boolean on) {
        Region knob = new Region(); knob.getStyleClass().add("jm-switch-knob");
        StackPane sw = new StackPane(knob); sw.getStyleClass().add("jm-switch"); if (on) sw.getStyleClass().add("on");
        sw.setPadding(new Insets(0,3,0,3)); StackPane.setAlignment(knob, on ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); sw.setMaxSize(36,20);
        return sw;
    }
}
