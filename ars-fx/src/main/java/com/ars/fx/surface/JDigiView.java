package com.ars.fx.surface;

import com.ars.fx.data.DigiEngine;
import com.ars.fx.shell.Shell;
import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.ModemStatus;
import com.hamradio.modem.model.SignalSnapshot;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

import static com.ars.fx.shell.Shell.lbl;

/** J-Digi digital-modes decode — bound to the real modem engine (com.hamradio:j-digi ModemService). */
public final class JDigiView {
    private JDigiView() {}

    // display label → ModeType enum name
    private static final String[][] TABS = {
            {"CW", "CW"}, {"RTTY", "RTTY"}, {"PSK31", "PSK31"}, {"Olivia", "OLIVIA"},
            {"MFSK16", "MFSK16"}, {"DominoEX", "DOMINOEX"}, {"AX.25", "AX25"}};
    private static final String[][] MACROS = {
            {"CQ", "CQ CQ CQ DE WM3J WM3J WM3J K"}, {"TU", "TU 73 DE WM3J"}, {"Name", "MY NAME IS MIKE"},
            {"QTH", "MY QTH IS FM19"}, {"73", "73 GL DE WM3J K"}, {"BRB", "BRB BK"}};

    public static Region build() {
        Region dock = Shell.dock("digi");
        ModemService svc = DigiEngine.get();
        DigiEngine.start();
        ModemStatus st0 = svc != null ? svc.getStatus() : null;

        String engine = svc != null ? "● engine" : "○ no audio";
        String[][] stats = {{"MODE", st0 != null ? st0.getMode().name() : "CW", "digi"},
                {"PEAK", st0 != null ? Math.round(st0.getPeakFrequencyHz()) + " Hz" : "—"},
                {"SNR", st0 != null ? String.format("%.0f dB", st0.getSnr()) : "—", "digi"},
                {"ENGINE", engine, svc != null ? "ok" : ""}};
        HBox top = Shell.topBar("digi", "digi", "J-Digi", "Digital modes decode", stats, null);

        // live waterfall fed by the spectrum listener
        WaterfallView wf = new WaterfallView();

        // mode tabs + status strip
        Label[] tabs = new Label[TABS.length];
        HBox modeRow = new HBox(8); modeRow.setAlignment(Pos.CENTER_LEFT);
        for (int i = 0; i < TABS.length; i++) {
            Label t = lbl(TABS[i][0], "dg-tab"); tabs[i] = t; t.setStyle("-fx-cursor:hand;");
            final ModeType m = ModeType.valueOf(TABS[i][1]);
            t.setOnMouseClicked(e -> { if (svc != null) svc.setMode(m); highlight(tabs, TABS, m.name()); });
            modeRow.getChildren().add(t);
        }
        if (st0 != null) highlight(tabs, TABS, st0.getMode().name());
        Region msp = new Region(); HBox.setHgrow(msp, Priority.ALWAYS);
        Label statusStrip = lbl(statusText(st0, svc), "wf-scale");
        modeRow.getChildren().addAll(msp, statusStrip);

        // decode log
        TextArea decode = new TextArea(); decode.setEditable(false); decode.setWrapText(true); decode.setPrefRowCount(11);
        decode.getStyleClass().add("dg-decode");
        decode.setStyle("-fx-control-inner-background:#0b0f14;-fx-background-color:#0b0f14;-fx-text-fill:-ars-ok;-fx-font-family:'JetBrains Mono';-fx-font-size:13px;-fx-border-color:-ars-border;-fx-border-radius:6;-fx-background-radius:6;");
        decode.setText(svc != null ? "" : "Modem engine unavailable in this environment (no audio device).\nDecoding runs when J-Digi is launched on the station.\n");
        VBox.setVgrow(decode, Priority.ALWAYS);

        // TX row
        TextField tx = new TextField(); tx.setPromptText("type to transmit…"); tx.getStyleClass().add("jhub-mini-field");
        HBox.setHgrow(tx, Priority.ALWAYS); tx.setMaxWidth(Double.MAX_VALUE);
        Label stop = lbl("Stop", "jl-clearbtn"); stop.setStyle("-fx-cursor:hand;");
        Label send = lbl("Send ▶", "dg-send"); send.setStyle("-fx-cursor:hand;");
        Runnable doSend = () -> { if (svc != null && !tx.getText().isBlank()) { svc.transmitText(tx.getText()); tx.clear(); } };
        send.setOnMouseClicked(e -> doSend.run());
        tx.setOnAction(e -> doSend.run());
        stop.setOnMouseClicked(e -> { if (svc != null) svc.cancelTransmit(); });
        HBox txRow = new HBox(12, tx, stop, send); txRow.setAlignment(Pos.CENTER_LEFT);

        // macros → transmit
        HBox macros = new HBox(6); macros.setAlignment(Pos.CENTER_LEFT);
        for (String[] mc : MACROS) {
            VBox b = new VBox(2, lbl(mc[0], "jl-macro-fk"), lbl(mc[1].length() > 14 ? mc[1].substring(0, 14) + "…" : mc[1], "jl-macro-ml"));
            b.getStyleClass().add("jl-macro"); b.setStyle("-fx-cursor:hand;"); HBox.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE);
            final String text = mc[1];
            b.setOnMouseClicked(e -> { if (svc != null) svc.transmitText(text); });
            macros.getChildren().add(b);
        }

        // bind live listeners (latest build wins)
        if (svc != null) {
            svc.setSpectrumListener(s -> Platform.runLater(() -> wf.push(s.getMagnitudes())));
            svc.setDecodeListener(line -> Platform.runLater(() -> { decode.appendText(line.endsWith("\n") ? line : line + "\n"); }));
            svc.setStatusListener(s -> Platform.runLater(() -> { highlight(tabs, TABS, s.getMode().name()); statusStrip.setText(statusText(s, svc)); }));
        }

        VBox center = new VBox(12, wfHeader(), wf.node(), modeRow, decode, txRow, macros);
        center.setPadding(new Insets(16, 18, 16, 18)); center.setStyle("-fx-background-color:-ars-bg;"); HBox.setHgrow(center, Priority.ALWAYS);
        Region rail = Shell.rail(railDrawers(svc));
        return Shell.frame(dock, top, center, rail);
    }

    private static void highlight(Label[] tabs, String[][] defs, String activeEnum) {
        for (int i = 0; i < tabs.length; i++) { tabs[i].getStyleClass().remove("on"); if (defs[i][1].equals(activeEnum)) tabs[i].getStyleClass().add("on"); }
    }
    private static String statusText(ModemStatus s, ModemService svc) {
        if (svc == null) return "engine offline";
        if (s == null) return "starting…";
        String tx = s.getTxStatusText();
        return String.format("%s · %.0f Hz · %.0f dB%s", s.getMode().name(), s.getPeakFrequencyHz(), s.getSnr(),
                (tx != null && !tx.isBlank()) ? "  ·  TX: " + tx : "");
    }

    private static Region wfHeader() {
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox h = new HBox(lbl("WATERFALL", "wf-eyebrow"), sp, lbl("live · audio spectrum", "wf-scale")); h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Node[] railDrawers(ModemService svc) {
        VBox dec = new VBox();
        ModemStatus s = svc != null ? svc.getStatus() : null;
        dec.getChildren().add(kvRow("Mode", s != null ? s.getMode().name() : "—", true));
        dec.getChildren().add(kvRow("Rig freq", s != null && s.getRigFrequencyHz() > 0 ? fmtMHz(s.getRigFrequencyHz()) : "—", false));
        dec.getChildren().add(kvRow("Peak", s != null ? Math.round(s.getPeakFrequencyHz()) + " Hz" : "—", false));
        dec.getChildren().add(kvRow("SNR", s != null ? String.format("%.0f dB", s.getSnr()) : "—", false));
        dec.getChildren().add(kvRow("Engine", svc != null ? "running" : "unavailable", svc != null));
        List<Node> rail = new ArrayList<>();
        rail.add(Shell.drawer("Decoder", "digi", "digi", s != null ? s.getMode().name() : "—", true, dec));
        rail.addAll(Shell.instruments(50));
        return rail.toArray(new Node[0]);
    }
    static HBox kvRow(String k, String v, boolean green) {
        var kl = lbl(k); kl.getStyleClass().add("k"); HBox.setHgrow(kl, Priority.ALWAYS); kl.setMaxWidth(Double.MAX_VALUE);
        var vl = lbl(v); vl.getStyleClass().addAll("ars-mono", "v"); if (green) vl.setStyle("-fx-text-fill:-ars-ok;");
        HBox row = new HBox(kl, vl); row.getStyleClass().add("sx-kv"); row.setAlignment(Pos.CENTER_LEFT); return row;
    }
    private static String fmtMHz(long hz) { return String.format("%.3f MHz", hz / 1_000_000.0); }

    // ---- live waterfall ----------------------------------------------------
    private static final class WaterfallView {
        private static final int W = 1010, H = 130;
        private final Canvas canvas = new Canvas(W, H);
        private final WritableImage img = new WritableImage(W, H);
        private final int[] scratch = new int[W * (H - 1)];
        private final javafx.scene.image.WritablePixelFormat<java.nio.IntBuffer> FMT = PixelFormat.getIntArgbInstance();

        WaterfallView() {
            var pw = img.getPixelWriter();
            for (int y = 0; y < H; y++) for (int x = 0; x < W; x++) pw.setArgb(x, y, 0xff05080c);
            redraw();
        }
        Region node() {
            Region cursor = new Region(); cursor.getStyleClass().add("wf-cursor"); cursor.setMinWidth(2); cursor.setMaxWidth(2); cursor.setMaxHeight(Double.MAX_VALUE);
            StackPane wf = new StackPane(canvas, cursor); StackPane.setAlignment(cursor, Pos.CENTER); wf.setMaxWidth(Double.MAX_VALUE);
            return wf;
        }
        void push(double[] mag) {
            if (mag == null || mag.length == 0) return;
            var pr = img.getPixelReader(); var pw = img.getPixelWriter();
            pr.getPixels(0, 1, W, H - 1, FMT, scratch, 0, W);   // scroll up one row
            pw.setPixels(0, 0, W, H - 1, FMT, scratch, 0, W);
            for (int x = 0; x < W; x++) {
                int bin = (int) ((double) x / W * mag.length);
                pw.setArgb(x, H - 1, color(mag[Math.min(bin, mag.length - 1)]));
            }
            redraw();
        }
        private void redraw() { canvas.getGraphicsContext2D().drawImage(img, 0, 0, canvas.getWidth(), canvas.getHeight()); }
        private int color(double m) {
            double db = 20 * Math.log10(m + 1e-9);
            double t = Math.max(0, Math.min(1, (db + 90) / 60));   // -90..-30 dB → 0..1
            int r, g, b;
            if (t < 0.5) { double u = t / 0.5; r = 0; g = (int) (u * 200); b = (int) (60 + u * 195); }   // blue→cyan
            else { double u = (t - 0.5) / 0.5; r = (int) (u * 255); g = (int) (200 + u * 55); b = (int) (255 - u * 255); }  // cyan→yellow→white
            return 0xff000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
        }
        private int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }
    }
}
