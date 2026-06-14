package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import com.ars.fx.shell.Waterfall;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Digi digital-modes decode (handoff shot 04), mock data. */
public final class JDigiView {
    private JDigiView() {}

    public static Region build() {
        Region dock = Shell.dock("digi");
        String[][] stats = {{"MODE","CW","digi"},{"SPEED","28 WPM"},{"FREQ","14.070","digi"},{"AFC","● lock","ok"}};
        HBox top = Shell.topBar("digi","digi","J-Digi","Digital modes decode", stats, "17:00:24");

        VBox center = new VBox(12, wfHeader(), waterfall(), tabs(), decode(), txRow(), macros());
        center.setPadding(new Insets(16, 18, 16, 18)); center.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(center, Priority.ALWAYS);
        Region rail = Shell.rail(railDrawers());
        return Shell.frame(dock, top, center, rail);
    }

    private static Region wfHeader() {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(lbl("WATERFALL","wf-eyebrow"), sp, lbl("14.070 MHz · CW","wf-scale")); h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
    private static Region waterfall() {
        Canvas cv = Waterfall.canvas(1010, 130, "cw", new double[][]{{0.5, 6, 0.95}});
        Region cursor = new Region(); cursor.getStyleClass().add("wf-cursor"); cursor.setMinWidth(2); cursor.setMaxWidth(2); cursor.setMaxHeight(Double.MAX_VALUE);
        StackPane wf = new StackPane(cv, cursor); StackPane.setAlignment(cursor, Pos.CENTER); wf.setMaxWidth(Double.MAX_VALUE);
        HBox scale = new HBox(); for (String s : Mock.DIGI_SCALE){ var l = lbl(s,"wf-scale"); HBox.setHgrow(l,Priority.ALWAYS); l.setMaxWidth(Double.MAX_VALUE); scale.getChildren().add(l); }
        var last = (javafx.scene.control.Label) scale.getChildren().get(scale.getChildren().size()-1); last.setAlignment(Pos.CENTER_RIGHT);
        scale.getChildren().add(lbl("Hz","wf-scale"));
        return new VBox(4, wf, scale);
    }
    private static Region tabs() {
        HBox left = new HBox(8, tab("CW",true), tab("RTTY",false), tab("PSK31",false));
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox right = new HBox(8, dbtn("AFC",true), dbtn("SQL",true), dbtn("RxID",false));
        HBox row = new HBox(left, sp, right); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private static javafx.scene.control.Label tab(String t, boolean on){ var l = lbl(t,"dg-tab"); if(on) l.getStyleClass().add("on"); return l; }
    private static javafx.scene.control.Label dbtn(String t, boolean on){ var l = lbl(t,"dg-btn"); if(on) l.getStyleClass().add("on"); return l; }

    private static Region decode() {
        VBox box = new VBox(8); box.getStyleClass().add("dg-decode"); VBox.setVgrow(box, Priority.ALWAYS);
        for (String[] d : Mock.DIGI_DECODE) {
            var ts = lbl(d[0],"dg-ts"); ts.setMinWidth(52);
            var line = lbl(d[1],"dg-line"); line.setWrapText(true); HBox.setHgrow(line, Priority.ALWAYS); line.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(10, ts, line); row.setAlignment(Pos.TOP_LEFT);
            if (d[2].equals("y")) { Region cur = new Region(); cur.getStyleClass().add("dg-cursor-blk"); row.getChildren().add(cur); }
            box.getChildren().add(row);
        }
        return box;
    }
    private static Region txRow() {
        var tx = lbl("DL8WPX DE WM3J","dg-tx"); HBox.setHgrow(tx, Priority.ALWAYS); tx.setMaxWidth(Double.MAX_VALUE); tx.setAlignment(Pos.CENTER_LEFT);
        HBox row = new HBox(12, tx, lbl("Send ▶","dg-send")); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
    private static Region macros() {
        HBox m = new HBox(6); m.setAlignment(Pos.CENTER_LEFT);
        for (String[] mc : Mock.DIGI_MACROS){ VBox b = new VBox(2, lbl(mc[0],"jl-macro-fk"), lbl(mc[1],"jl-macro-ml")); b.getStyleClass().add("jl-macro"); m.getChildren().add(b); }
        return m;
    }
    private static Node[] railDrawers() {
        VBox dec = new VBox();
        for (String[] d : Mock.DIGI_DECODER) dec.getChildren().add(kvRow(d[0], d[1], d[2].equals("y")));
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("Decoder","digi","digi","CW · 28 WPM", true, dec));
        rail.addAll(Shell.instruments(50));
        return rail.toArray(new Node[0]);
    }
    static HBox kvRow(String k, String v, boolean green) {
        var kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        var vl = lbl(v); vl.getStyleClass().addAll("ars-mono","v"); if (green) vl.setStyle("-fx-text-fill:-ars-ok;");
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT); return row;
    }
}
