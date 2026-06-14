package com.ars.fx.surface;

import com.ars.fx.mock.Mock;
import com.ars.fx.shell.Shell;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Log contest cockpit (handoff shot 02), mock data. */
public final class JLogCockpit {
    private JLogCockpit() {}

    public static Region build() {
        Region dock = Shell.dock("log");

        // top bar = scoreboard + contest selector
        String[][] stats = {{"QSOS","1,284"},{"MULTS","13"},{"SCORE","16,692","amber"}};
        HBox top = Shell.topBar("log","log","J-Log","CQ WW DX · Zones", stats, "17:00:03");
        Label sel = lbl("CQ WW DX  ▾", "sx-clock-sel"); HBox.setMargin(sel, new Insets(0,0,0,16));
        top.getChildren().add(sel);

        // center: exchange + contest log
        VBox center = new VBox(exchange(), logHeader(), logTable());
        center.setStyle("-fx-background-color:-ars-bg;");
        ScrollPane centerScroll = new ScrollPane(center);
        centerScroll.setFitToWidth(true); centerScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        centerScroll.setStyle("-fx-background-color:-ars-bg;");
        HBox.setHgrow(centerScroll, Priority.ALWAYS);

        HBox body = new HBox(centerScroll, bandMap());   // center + band-map column
        body.setFillHeight(true); HBox.setHgrow(centerScroll, Priority.ALWAYS);

        Region rail = Shell.rail(railDrawers());
        Region bottom = bottomBar();
        return Shell.frame(dock, top, body, rail, bottom);
    }

    // ---- exchange card -----------------------------------------------------
    private static VBox exchange() {
        Label eyebrow = lbl("EXCHANGE","jl-eyebrow");
        HBox runsp = new HBox(2, runBtn("Run", true), runBtn("S&P", false)); runsp.getStyleClass().add("jl-runsp");
        Label mult = lbl("Mult: Zones","jl-emeta");
        Region sp0 = new Region(); HBox.setHgrow(sp0, Priority.ALWAYS);
        HBox er0 = new HBox(12, eyebrow, runsp, sp0, mult);
        er0.setAlignment(Pos.CENTER_LEFT);

        VBox call = field("CALLSIGN", input("—", "jl-ein-call", 178));
        VBox snt  = labeled("SNT", displayBox("59 05"));
        VBox rst  = field("RST", input("59", "jl-fin", 70));
        VBox zone = field("CQ ZONE", input("", "jl-fin", 90));
        Label log = lbl("Log  ⏎", "jl-logbtn");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox er1 = new HBox(12, call, snt, rst, zone, sp, log); er1.setAlignment(Pos.BOTTOM_LEFT);

        Label cp = lbl("Check-partial — start typing a callsign…","jl-cp-empty");
        Label disc = lbl("›  Full QSO details","jl-disc");

        VBox box = new VBox(13, er0, er1, cp, disc); box.getStyleClass().add("jl-entry");
        return box;
    }
    private static Label runBtn(String t, boolean on){ Label l = lbl(t, "jl-runsp-btn"); if(on) l.getStyleClass().add("on"); return l; }
    private static VBox field(String label, Region inp){ VBox v = new VBox(5, lbl(label,"jl-flabel"), inp); v.setAlignment(Pos.TOP_LEFT); return v; }
    private static VBox labeled(String label, Region n){ return field(label, n); }
    private static Label input(String text, String cls, double w){ Label l = lbl(text, cls); l.setMinWidth(w); l.setPrefWidth(w); l.setAlignment(Pos.CENTER_LEFT); return l; }
    private static Label displayBox(String text){ Label l = lbl(text, "jl-snt"); return l; }

    // ---- contest log -------------------------------------------------------
    private static final double[] COLS = {54, -1, 44, 56, 70, 70, 40}; // -1 = grow
    private static VBox logHeader() {
        Label h = lbl("☰  CONTEST LOG"); h.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:-ars-t3;");
        Label n = lbl("· 10 QSOs shown"); n.setStyle("-fx-font-size:11px; -fx-text-fill:-ars-t4;");
        HBox title = new HBox(8, h, n); title.setAlignment(Pos.CENTER_LEFT); title.getStyleClass().add("jl-loghd");
        HBox cols = row(new String[]{"UTC","CALL","BND","MODE","SNT","RCV","M"}, "jl-th-c", "jl-th");
        return new VBox(title, cols);
    }
    private static VBox logTable() {
        VBox v = new VBox();
        for (String[] q : Mock.QSOS) {
            HBox r = new HBox(); r.getStyleClass().add("jl-tr"); r.setAlignment(Pos.CENTER_LEFT); r.setSpacing(10);
            r.getChildren().addAll(
                col(lbl(q[0],"jl-tr-mono"), 0),
                col(lbl(q[1],"jl-tr-call"), 1),
                col(lbl(q[2],"jl-tr-mono"), 2),
                col(modeChip(q[3]), 3),
                col(lbl(q[4],"jl-tr-mono"), 4),
                col(lbl(q[5],"jl-tr-mono"), 5),
                col(multDot(q[6].equals("y")), 6));
            v.getChildren().add(r);
        }
        return v;
    }
    private static HBox row(String[] cells, String cellClass, String rowClass) {
        HBox r = new HBox(10); r.getStyleClass().add(rowClass); r.setAlignment(Pos.CENTER_LEFT);
        for (int i=0;i<cells.length;i++) r.getChildren().add(col(lbl(cells[i], cellClass), i));
        return r;
    }
    private static Region col(Node n, int i) {
        HBox cell = new HBox(n); cell.setAlignment(i>=2 ? Pos.CENTER_LEFT : Pos.CENTER_LEFT);
        if (i==6) cell.setAlignment(Pos.CENTER);
        if (COLS[i] < 0) { HBox.setHgrow(cell, Priority.ALWAYS); cell.setMaxWidth(Double.MAX_VALUE); }
        else { cell.setMinWidth(COLS[i]); cell.setPrefWidth(COLS[i]); cell.setMaxWidth(COLS[i]); }
        return cell;
    }
    private static Label modeChip(String m){ Label l = lbl(m, "jl-mode"); return l; }
    private static Region multDot(boolean on){ Region r = new Region(); if(on){ r.getStyleClass().add("jl-multdot"); } else { r.setMinSize(9,9); r.setPrefSize(9,9);} return r; }

    // ---- band map column ---------------------------------------------------
    private static Region bandMap() {
        Label ph = lbl("⊕  Band map", "jl-bm-ph"); HBox.setHgrow(ph, Priority.ALWAYS); ph.setMaxWidth(Double.MAX_VALUE);
        Label phm = lbl("20m · SSB", "jl-bm-tickfq");
        HBox header = new HBox(ph, phm); header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("jl-bm-ph");

        double H = 620, min = 14.150, max = 14.350;
        Pane scale = new Pane(); scale.setMinHeight(H); scale.setPrefHeight(H);
        for (String t : Mock.BM_TICKS) {
            double y = (Double.parseDouble(t)-min)/(max-min)*H;
            Label fq = lbl(t, "jl-bm-tickfq"); fq.setLayoutX(8); fq.setLayoutY(y-7);
            Region ln = new Region(); ln.getStyleClass().add("jl-bm-tickln"); ln.setLayoutX(56); ln.setLayoutY(y); ln.setPrefWidth(170); ln.setMinHeight(1);
            scale.getChildren().addAll(ln, fq);
        }
        for (String[] s : Mock.BANDMAP) {
            boolean me = s[2].equals("y");
            double y = (Double.parseDouble(s[0])-min)/(max-min)*H;
            Region dot = new Region(); dot.getStyleClass().add("jl-bm-dot"); if(me) dot.getStyleClass().add("me");
            Label cl = lbl(me ? s[1]+" ◄" : s[1], "jl-bm-cl"); if(me) cl.getStyleClass().add("me");
            HBox row = new HBox(7, dot, cl); row.setAlignment(Pos.CENTER_LEFT);
            row.setLayoutX(60); row.setLayoutY(y-8);
            scale.getChildren().add(row);
        }
        VBox col = new VBox(header, scale); col.getStyleClass().add("jl-bm-col");
        col.setMinWidth(248); col.setPrefWidth(248); col.setMaxWidth(248);
        return col;
    }

    // ---- right rail --------------------------------------------------------
    private static Node[] railDrawers() {
        // Zones
        FlowPane zones = new FlowPane(5,5);
        for (String z : Mock.ZONES) zones.getChildren().add(lbl(z, "jl-mchip"));
        // Rate
        HBox bars = new HBox(3); bars.setAlignment(Pos.BOTTOM_LEFT); bars.setFillHeight(false); bars.setMinHeight(42); bars.setPrefHeight(42);
        for (int r : Mock.RATE){ double h = r/7.0*42; Region b = new Region(); b.getStyleClass().add("jl-rate-bar"); HBox.setHgrow(b,Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); b.setMinHeight(h); b.setPrefHeight(h); b.setMaxHeight(h); bars.getChildren().add(b); }
        Label rl = lbl("last 10 min", "jl-rate-foot"); HBox.setHgrow(rl, Priority.ALWAYS); rl.setMaxWidth(Double.MAX_VALUE);
        HBox rfoot = new HBox(rl, lbl("52 Q · 9 mult/hr","jl-rate-foot"));
        VBox rate = new VBox(8, bars, rfoot);
        // DX Cluster
        VBox clu = new VBox();
        for (String[] c : Mock.CLUSTER) {
            Label fq = lbl(c[0],"jl-clu-fq"); fq.setMinWidth(54);
            Label cl = lbl(c[1],"jl-clu-cl"); HBox.setHgrow(cl, Priority.ALWAYS); cl.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(9, fq, cl);
            if (c[2].equals("y")) row.getChildren().add(lbl("MULT","jl-clu-mult"));
            row.getChildren().add(lbl(c[3],"jl-clu-age"));
            row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(7,0,7,0));
            clu.getChildren().add(row);
        }
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("Zones","log","vault","13 wkd", true, zones));
        rail.add(Shell.drawer("Rate","accent","digi","38/hr", true, rate));
        rail.add(Shell.drawer("DX Cluster","map","map","live", true, clu));
        List<Node> inst = Shell.instruments(45);
        rail.add(inst.get(0)); rail.add(inst.get(1)); rail.add(inst.get(2));   // rotor, prop, space-wx
        return rail.toArray(new Node[0]);
    }

    // ---- bottom bar --------------------------------------------------------
    private static Region bottomBar() {
        HBox fq = new HBox(3, lbl("14.250","jl-rig-fq"), lbl("MHz","jl-rig-u")); fq.setAlignment(Pos.BOTTOM_LEFT);
        HBox chips = new HBox(6, lbl("SSB","jl-chip","accent"), lbl("20m","jl-chip"));
        HBox rig = new HBox(16, fq, chips); rig.setAlignment(Pos.CENTER_LEFT); rig.setPadding(new Insets(0,18,0,0));
        rig.setStyle("-fx-border-color: transparent -ars-border transparent transparent; -fx-border-width: 0 1 0 0;");

        HBox macros = new HBox(6); macros.setAlignment(Pos.CENTER_LEFT); macros.setPadding(new Insets(0,0,0,16));
        for (String[] m : Mock.MACROS) {
            VBox mb = new VBox(2, lbl(m[0],"jl-macro-fk"), lbl(m[1],"jl-macro-ml")); mb.getStyleClass().add("jl-macro");
            macros.getChildren().add(mb);
        }
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label theme = lbl("◑ Theme","jl-theme");
        HBox bar = new HBox(rig, macros, sp, theme); bar.getStyleClass().add("jl-bottom"); bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }
}
