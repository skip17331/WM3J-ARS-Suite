package com.ars.fx.surface;

import com.ars.fx.data.BridgeService;
import com.ars.fx.shell.Shell;
import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Bridge — live WSJT-X monitor over the real UDP listener (com.hamradio:j-bridge). */
public final class JBridgeView {
    private JBridgeView() {}

    private static final DateTimeFormatter HMS = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneOffset.UTC);

    public static Region build() {
        Region dock = Shell.dock("bridge");
        BridgeService.start();
        WsjtxStatus st0 = BridgeService.status();

        String[][] stats = {{"DIAL", dialMHz(st0), "bridge"}, {"MODE", st0 != null ? nz(st0.getMode()) : "—"},
                {"DECODES", String.valueOf(BridgeService.decodes().size())},
                {"WSJT-X", BridgeService.connected() ? "● linked" : "○ waiting", BridgeService.connected() ? "ok" : ""}};
        HBox top = Shell.topBar("bridge", "bridge", "J-Bridge", "WSJT-X bridge · UDP " + BridgeService.port(), stats, null);

        // connection + status line
        Label conn = lbl("", "jb-seq-lbl");
        Label seqLbl = lbl("", "jb-seq-lbl");
        Label nextLbl = lbl("", "jb-seq-lbl");
        Region prog = progress();
        Label slotEven = slotBtn("Even"), slotOdd = slotBtn("Odd");
        Label txState = lbl("RECEIVING", "jb-recv");

        HBox seqLine = new HBox(6, lbl("15 s period ·", "jb-seq-lbl"), seqLbl, nextLbl); seqLine.setAlignment(Pos.CENTER_LEFT);
        VBox progBox = new VBox(6, prog, seqLine); HBox.setHgrow(progBox, Priority.ALWAYS);
        HBox slot = new HBox(2, slotEven, slotOdd); slot.getStyleClass().add("jb-slot");
        HBox seqRow = new HBox(16, progBox, slot, txState); seqRow.setAlignment(Pos.CENTER_LEFT);

        // decode table host (rebuilt on each WSJT-X event)
        VBox tableHost = new VBox(); VBox.setVgrow(tableHost, Priority.ALWAYS);

        VBox footer = new VBox(); footer.setPadding(new Insets(14, 16, 14, 16));
        footer.setStyle("-fx-background-color:-ars-surface-1;-fx-border-color:-ars-border;-fx-border-width:1;-fx-background-radius:10;-fx-border-radius:10;");

        VBox linkBody = new VBox();   // rail "WSJT-X link" drawer body (live)

        Runnable refresh = () -> {
            WsjtxStatus s = BridgeService.status();
            conn.setText(BridgeService.connected()
                    ? "● " + (BridgeService.sourceApp().isBlank() ? "WSJT-X" : BridgeService.sourceApp())
                        + (BridgeService.version().isBlank() ? "" : " " + BridgeService.version()) + " · UDP " + BridgeService.port()
                    : "○ waiting for WSJT-X on UDP " + BridgeService.port() + " — start WSJT-X (Reporting ▸ accept UDP requests)");
            conn.setStyle(BridgeService.connected() ? "-fx-text-fill:-ars-ok;" : "-fx-text-fill:-ars-t4;");
            txState.setText(s != null && s.isTransmitting() ? "TRANSMITTING" : (s != null && s.isTxEnabled() ? "TX ENABLED" : "RECEIVING"));
            txState.setStyle(s != null && s.isTransmitting() ? "-fx-text-fill:-ars-err;" : "");
            fillDecodes(tableHost);
            fillFooter(footer, s);
            fillLink(linkBody);
            updateTopStats(top);
        };
        BridgeService.setSink(() -> Platform.runLater(refresh));
        fillLink(linkBody);
        refresh.run();

        // local FT8 sequence clock (WSJT-X UDP carries no spectrum/period; derive from UTC)
        Timeline seq = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long sec = java.time.Instant.now().getEpochSecond();
            boolean even = (sec % 30) < 15;
            int into = (int) (sec % 15), left = 15 - into;
            seqLbl.setText(even ? "EVEN slot" : "ODD slot");
            nextLbl.setText("· " + left + "s to next");
            slotEven.getStyleClass().remove("on"); slotOdd.getStyleClass().remove("on");
            (even ? slotEven : slotOdd).getStyleClass().add("on");
            ((Region) prog.getChildrenUnmodifiable().get(1)).setMaxWidth(prog.getWidth() * (into / 15.0));
        }));
        seq.setCycleCount(Timeline.INDEFINITE); seq.play();

        VBox center = new VBox(13, conn, seqRow, tableHost, footer);
        center.setPadding(new Insets(16, 18, 16, 18)); center.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(center, Priority.ALWAYS);
        Region rail = Shell.rail(railDrawers(linkBody));
        return Shell.frame(dock, top, center, rail);
    }

    private static void updateTopStats(HBox top) {
        List<Label> vs = new ArrayList<>(); collectStatValues(top, vs);
        if (vs.size() < 4) return;
        WsjtxStatus s = BridgeService.status();
        vs.get(0).setText(dialMHz(s));
        vs.get(1).setText(s != null ? nz(s.getMode()) : "—");
        vs.get(2).setText(String.valueOf(BridgeService.decodes().size()));
        vs.get(3).setText(BridgeService.connected() ? "● linked" : "○ waiting");
    }
    private static void collectStatValues(Node n, List<Label> out) {
        if (n instanceof Label l && l.getStyleClass().contains("sx-stat-v")) out.add(l);
        if (n instanceof javafx.scene.Parent p) for (Node c : p.getChildrenUnmodifiable()) collectStatValues(c, out);
    }
    private static void fillLink(VBox link) {
        WsjtxStatus s = BridgeService.status();
        link.getChildren().setAll(
                JDigiView.kvRow("Listener", BridgeService.running() ? "● UDP " + BridgeService.port() : "stopped", BridgeService.running()),
                JDigiView.kvRow("Upstream", BridgeService.connected() ? nz2(BridgeService.sourceApp(), "WSJT-X") + " " + BridgeService.version() : "waiting", BridgeService.connected()),
                JDigiView.kvRow("Dial", s != null ? dialMHz(s) : "—", false),
                JDigiView.kvRow("Mode", s != null ? nz(s.getMode()) : "—", false),
                JDigiView.kvRow("Tx enabled", s != null && s.isTxEnabled() ? "yes" : "no", s != null && s.isTxEnabled()));
    }

    private static final double[] DC = {62, 44, 44, 56};
    private static void fillDecodes(VBox host) {
        host.getChildren().clear();
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox hd = new HBox(8, cell(lbl("UTC", "jb-panel-hd"), 0), cell(lbl("dB", "jb-panel-hd"), 1),
                cell(lbl("DT", "jb-panel-hd"), 2), cell(lbl("ΔF", "jb-panel-hd"), 3), msgCell(lbl("MESSAGE", "jb-panel-hd")));
        hd.setAlignment(Pos.CENTER_LEFT);
        VBox tbl = new VBox(); tbl.getStyleClass().add("jb-tbl"); VBox.setVgrow(tbl, Priority.ALWAYS);
        List<WsjtxDecode> ds = BridgeService.decodes();
        if (ds.isEmpty()) {
            Label none = lbl("No decodes yet — waiting for WSJT-X traffic.", "jb-noqso-sub"); none.setPadding(new Insets(16, 4, 16, 4));
            tbl.getChildren().add(none);
        } else {
            for (WsjtxDecode d : ds) tbl.getChildren().add(decodeRow(d));
        }
        host.getChildren().addAll(hd, tbl);
    }
    private static HBox decodeRow(WsjtxDecode d) {
        HBox row = new HBox(8); row.getStyleClass().add("jb-row"); if (d.isCqCall()) row.getStyleClass().add("cq"); row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().add(cell(lbl(d.getTimestamp() != null ? HMS.format(d.getTimestamp()) : "—", "jb-cell"), 0));
        row.getChildren().add(cell(lbl(String.valueOf(d.getSnr()), "jb-cell"), 1));
        row.getChildren().add(cell(lbl(String.format("%+.1f", d.getDeltaTime()), "jb-cell"), 2));
        row.getChildren().add(cell(lbl(String.valueOf(d.getDeltaFrequency()), "jb-cell"), 3));
        Label msg = lbl(nz(d.getMessage()), "jb-msg"); if (d.isCqCall()) msg.getStyleClass().add("cq");
        HBox.setHgrow(msg, Priority.ALWAYS); msg.setMaxWidth(Double.MAX_VALUE);
        String tail = (d.getCountry() != null && !d.getCountry().isBlank() ? d.getCountry() : "")
                + (d.getDistanceKm() > 0 ? "  " + Math.round(d.getDistanceKm()) + " km" : "");
        HBox m = new HBox(10, msg); m.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(m, Priority.ALWAYS);
        if (!tail.isBlank()) m.getChildren().add(lbl(tail.trim(), "jb-panel-hd"));
        row.getChildren().add(m);
        return row;
    }

    private static void fillFooter(VBox footer, WsjtxStatus s) {
        footer.getChildren().clear();
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        boolean haveDx = s != null && s.getDxCall() != null && !s.getDxCall().isBlank();
        VBox left = haveDx
                ? new VBox(2, lbl(s.getDxCall() + (s.getDxGrid() != null && !s.getDxGrid().isBlank() ? "  " + s.getDxGrid() : ""), "jb-noqso"),
                        lbl("report " + nz(s.getReport()) + (s.getBand() != null ? " · " + s.getBand() : ""), "jb-noqso-sub"))
                : new VBox(2, lbl("No DX selected", "jb-noqso"), lbl("Double-click a station in WSJT-X to set the DX call", "jb-noqso-sub"));
        Label rx = lbl(s != null && s.isTransmitting() ? "TRANSMITTING" : "RECEIVING", "jb-recv");
        HBox topRow = new HBox(left, sp, rx); topRow.setAlignment(Pos.CENTER_LEFT);
        footer.getChildren().add(topRow);
    }

    private static Node[] railDrawers(VBox linkBody) {
        List<Node> rail = new ArrayList<>();
        rail.addAll(Shell.leadDrawers(50));     // Station ID, Rig control, Rotor control
        rail.add(Shell.drawer("WSJT-X link", "bridge", "bridge", BridgeService.connected() ? "connected" : "waiting", true, linkBody));
        rail.addAll(Shell.instruments(50));
        return rail.toArray(new Node[0]);
    }

    // ---- helpers -----------------------------------------------------------
    private static Pane progress() {
        Region track = new Region(); track.getStyleClass().add("jb-seq-track");
        Region fill = new Region(); fill.getStyleClass().add("jb-seq-fill");
        Pane p = new Pane(track, fill); p.setMinHeight(6); p.setMaxHeight(6); p.setMaxWidth(Double.MAX_VALUE);
        p.widthProperty().addListener((o, ov, nv) -> { double w = nv.doubleValue(); track.resizeRelocate(0, 0, w, 6); fill.resizeRelocate(0, 0, fill.getMaxWidth(), 6); });
        fill.maxWidthProperty().addListener((o, ov, nv) -> fill.resizeRelocate(0, 0, nv.doubleValue(), 6));
        return p;
    }
    private static Label slotBtn(String t) { Label l = lbl(t, "jb-slot-b"); return l; }
    private static Region cell(Node n, int i) {
        HBox c = new HBox(n); c.setAlignment(Pos.CENTER_LEFT); c.setMinWidth(DC[i]); c.setPrefWidth(DC[i]); c.setMaxWidth(DC[i]); return c;
    }
    private static Region msgCell(Node n) { HBox c = new HBox(n); c.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(c, Priority.ALWAYS); c.setMaxWidth(Double.MAX_VALUE); return c; }
    private static String dialMHz(WsjtxStatus s) { return s == null || s.getDialFrequency() <= 0 ? "—" : String.format("%.3f", s.getDialFrequency() / 1_000_000.0); }
    private static String nz(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private static String nz2(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
}
