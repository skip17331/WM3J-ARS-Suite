package com.ars.fx.shell;

import com.ars.fx.data.RigClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Canonical rig-control panel — one pinned outer drawer holding three
 * independently collapsible sub-drawers (Band &amp; Mode, Filter &amp; Functions,
 * DSP &amp; Levels). Drops down from the top-right RIG button at the standard
 * drawer width; replaces the old simple rig-control rail drawer.
 *
 * <p>Outer, always visible: header → meters strip (S-meter + SWR/PWR/ALC) →
 * frequency pane (FAST ▲▼ left · direct-entry centre · SLOW ▲▼ right, then
 * VFO A/B + SPLIT) → pinned ATU · TUNE · PTT footer. All readouts are
 * monospace; no blur/filters (JavaFX-safe). Live state comes from
 * {@link RigClient}; controls send best-effort CAT through it.
 */
public final class RigPanel {
    private RigPanel() {}

    public static final double WIDTH = 322;            // matches the rail/drawer column
    private static final long FAST_HZ = 1_000, SLOW_HZ = 100;
    private static final String[] BANDS  = {"160","80","40","30","20","17","15","12","10","6"};
    private static final String[] MODES  = {"LSB","USB","CW","CW-R","RTTY","DATA","AM","FM"};
    private static final String[] FILTERS = {"3.0k","2.4k","1.8k","500","250"};
    private static final String[] FUNCS   = {"NB","NR","ANF","COMP"};
    private static final String[] AGC     = {"FAST","MID","SLOW"};

    public static Region build() { return build(null); }

    /** @param onClose optional handler for the header ✕ (used by the dropdown overlay). */
    public static Region build(Runnable onClose) {
        RigClient rig = RigClient.getInstance();

        // ── header ──────────────────────────────────────────────────────────
        StackPane hic = Shell.iconTile("bridge", "bridge", 14, "sx-dw-ic", "bridge");
        Label htitle = lbl("Rig Control", "rig-h-title"); HBox.setHgrow(htitle, Priority.ALWAYS); htitle.setMaxWidth(Double.MAX_VALUE);
        Region statusDot = new Region(); statusDot.getStyleClass().add("rig-dot");
        Label statusTx = lbl("offline", "rig-h-status");
        HBox header = new HBox(9, hic, htitle, statusDot, statusTx);
        header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("rig-head");
        if (onClose != null) {
            Label x = lbl("✕", "rig-x"); x.setStyle("-fx-cursor:hand;");
            x.setOnMouseClicked(e -> onClose.run());
            header.getChildren().add(x);
        }

        // ── meters strip ────────────────────────────────────────────────────
        Region[] seg = new Region[16];
        HBox segBox = new HBox(2);
        for (int i = 0; i < seg.length; i++) {
            seg[i] = new Region(); seg[i].getStyleClass().add("rig-seg");
            if (i >= 9) seg[i].getStyleClass().add("hi");        // S9+ segments tint red
            HBox.setHgrow(seg[i], Priority.ALWAYS); seg[i].setMaxWidth(Double.MAX_VALUE);
            segBox.getChildren().add(seg[i]);
        }
        HBox.setHgrow(segBox, Priority.ALWAYS);
        Label sRead = lbl("—", "rig-s-read");
        HBox sRow = new HBox(8, lbl("S", "rig-s-k"), segBox, sRead); sRow.setAlignment(Pos.CENTER_LEFT);
        Bar swr = new Bar("warn"), pwr = new Bar("ok"), alc = new Bar("accent");
        HBox meterBars = new HBox(10, barCell("SWR", swr), barCell("PWR", pwr), barCell("ALC", alc));
        VBox meters = new VBox(8, sRow, meterBars); meters.getStyleClass().add("rig-meters");

        // ── frequency pane ──────────────────────────────────────────────────
        TextField freqField = new TextField("—.—.—");
        freqField.getStyleClass().add("rig-freq-field"); freqField.setAlignment(Pos.CENTER);
        Label mhz = lbl("MHz", "rig-freq-unit");
        Label modeChip = lbl("—", "rig-mode-chip");
        HBox freqSub = new HBox(8, mhz, Shell.lbl("", "rig-freq-unit"), modeChip);
        freqSub.setAlignment(Pos.CENTER); HBox.setHgrow(freqSub.getChildren().get(1), Priority.ALWAYS);
        ((Label) freqSub.getChildren().get(1)).setMaxWidth(Double.MAX_VALUE);
        VBox freqCenter = new VBox(4, freqField, freqSub); freqCenter.setAlignment(Pos.CENTER);
        HBox.setHgrow(freqCenter, Priority.ALWAYS);
        freqField.setOnAction(e -> { long hz = parseFreqInput(freqField.getText()); if (hz > 0) rig.setFreqHz(hz); });
        // don't select-all on focus (it would paint the whole readout as a highlight block)
        freqField.focusedProperty().addListener((o, was, is) -> { if (is) Platform.runLater(freqField::deselect); });

        VBox fast = stepCol("FAST", () -> rig.nudgeFreq(FAST_HZ), () -> rig.nudgeFreq(-FAST_HZ));
        VBox slow = stepCol("SLOW", () -> rig.nudgeFreq(SLOW_HZ), () -> rig.nudgeFreq(-SLOW_HZ));
        HBox freqRow = new HBox(8, fast, freqCenter, slow); freqRow.setAlignment(Pos.CENTER);

        Label vfoA = toggle("VFO A", () -> rig.setVfo("VFOA"));
        Label vfoB = toggle("VFO B", () -> rig.setVfo("VFOB"));
        boolean[] split = {false};
        Label splitB = toggle("SPLIT", null);
        splitB.setOnMouseClicked(e -> { split[0] = !split[0]; rig.setSplit(split[0]); splitB.getStyleClass().remove("on"); if (split[0]) splitB.getStyleClass().add("on"); });
        HBox vfoRow = new HBox(6, vfoA, vfoB, splitB);
        for (Node n : vfoRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        VBox freqPane = new VBox(9, freqRow, vfoRow); freqPane.getStyleClass().add("rig-freq");

        // ── pinned ATU · TUNE · PTT footer ──────────────────────────────────
        boolean[] atu = {false};
        Label atuB = footBtn("ATU", null);
        atuB.setOnMouseClicked(e -> { atu[0] = !atu[0]; rig.setFunc("TUNER", atu[0]); atuB.getStyleClass().remove("on"); if (atu[0]) atuB.getStyleClass().add("on"); });
        Label tuneB = footBtn("TUNE", rig::antTune);
        boolean[] ptt = {false};
        Label pttB = footBtn("PTT", null); pttB.getStyleClass().add("ptt");
        pttB.setOnMouseClicked(e -> { ptt[0] = !ptt[0]; rig.setPtt(ptt[0]); pttB.getStyleClass().remove("tx"); if (ptt[0]) pttB.getStyleClass().add("tx"); });
        HBox foot = new HBox(6, atuB, tuneB, pttB); foot.getStyleClass().add("rig-foot");
        for (Node n : foot.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── sub-drawer 1: Band & Mode ───────────────────────────────────────
        Map<String,Label> bandBtns = new LinkedHashMap<>();
        GridPane bandGrid = grid(5);
        int i = 0; for (String b : BANDS) { Label l = keyBtn(b + "m", () -> rig.setBand(b)); bandBtns.put(b, l); bandGrid.add(l, i % 5, i / 5); i++; }
        Map<String,Label> modeBtns = new LinkedHashMap<>();
        GridPane modeGrid = grid(4);
        i = 0; for (String m : MODES) { Label l = keyBtn(m, () -> rig.setMode(catMode(m))); modeBtns.put(m, l); modeGrid.add(l, i % 4, i / 4); i++; }
        VBox bmBody = new VBox(9, lbl("BAND", "rig-sec-k"), bandGrid, lbl("MODE", "rig-sec-k"), modeGrid);
        Label bmSum = lbl("", "rig-sub-sum");
        VBox sub1 = sub("Band & Mode", "log", bmSum, true, bmBody);

        // ── sub-drawer 2: Filter & Functions ────────────────────────────────
        Map<String,Label> filtBtns = new LinkedHashMap<>();
        String[] filtSel = {"2.4k"};
        GridPane filtGrid = grid(5);
        i = 0; for (String f : FILTERS) { Label l = keyBtn(f, null); filtBtns.put(f, l);
            l.setOnMouseClicked(e -> { filtSel[0] = f; filtBtns.forEach((k, v) -> { v.getStyleClass().remove("on"); if (k.equals(f)) v.getStyleClass().add("on"); }); });
            filtGrid.add(l, i % 5, i / 5); i++; }
        Map<String,Label> funcBtns = new LinkedHashMap<>();
        java.util.Set<String> funcsOn = new java.util.LinkedHashSet<>();
        GridPane funcGrid = grid(4);
        i = 0; for (String fn : FUNCS) { Label l = keyBtn(fn, null); funcBtns.put(fn, l);
            l.setOnMouseClicked(e -> { boolean on = !funcsOn.contains(fn); if (on) funcsOn.add(fn); else funcsOn.remove(fn);
                l.getStyleClass().remove("on"); if (on) l.getStyleClass().add("on"); rig.setFunc(fn, on); });
            funcGrid.add(l, i % 4, i / 4); i++; }
        VBox ffBody = new VBox(9, lbl("FILTER WIDTH", "rig-sec-k"), filtGrid, lbl("FUNCTIONS", "rig-sec-k"), funcGrid);
        Label ffSum = lbl("", "rig-sub-sum");
        VBox sub2 = sub("Filter & Functions", "digi", ffSum, false, ffBody);

        // ── sub-drawer 3: DSP & Levels ──────────────────────────────────────
        String[] agcSel = {"MID"};
        Map<String,Label> agcBtns = new LinkedHashMap<>();
        GridPane agcGrid = grid(3);
        i = 0; for (String a : AGC) { Label l = keyBtn(a, null); agcBtns.put(a, l);
            l.setOnMouseClicked(e -> { agcSel[0] = a; agcBtns.forEach((k, v) -> { v.getStyleClass().remove("on"); if (k.equals(a)) v.getStyleClass().add("on"); }); });
            agcGrid.add(l, i, 0); i++; }
        agcBtns.get("MID").getStyleClass().add("on");
        VBox levels = new VBox(7,
                levelRow(rig, "AF",  "AF",       35),
                levelRow(rig, "RF",  "RF",       100),
                levelRow(rig, "SQL", "SQL",      0),
                levelRow(rig, "MIC", "MICGAIN",  40),
                levelRow(rig, "PWR", "RFPOWER",  100));
        VBox dspBody = new VBox(9, lbl("AGC", "rig-sec-k"), agcGrid, lbl("LEVELS", "rig-sec-k"), levels);
        Label dlSum = lbl("", "rig-sub-sum");
        VBox sub3 = sub("DSP & Levels", "sat", dlSum, false, dspBody);

        // ── assemble ────────────────────────────────────────────────────────
        VBox panel = new VBox(0, header, meters, freqPane, foot, sub1, sub2, sub3);
        panel.getStyleClass().add("rig-panel");
        panel.setMinWidth(WIDTH); panel.setPrefWidth(WIDTH); panel.setMaxWidth(WIDTH);

        // ── live render ─────────────────────────────────────────────────────
        Runnable render = () -> {
            RigClient.State st = rig.last();
            boolean live = st != null && st.connected();
            statusDot.getStyleClass().removeAll("on"); if (live) statusDot.getStyleClass().add("on");
            statusTx.setText(live ? "rigctld · live" : "offline");

            String freq = live && st.freqHz() > 0 ? RigClient.fmtFreq(st.freqHz()) : "—.—.—";
            if (!freqField.isFocused()) freqField.setText(freq);
            String mode = live && st.mode() != null && !st.mode().isBlank() ? st.mode() : "—";
            modeChip.setText(mode);

            int lit = live ? RigClient.sMeterSegments(st.sMeterDb()) : 0;
            for (int s = 0; s < seg.length; s++) { seg[s].getStyleClass().remove("on"); if (s < lit) seg[s].getStyleClass().add("on"); }
            sRead.setText(live ? RigClient.sMeterText(st.sMeterDb()) : "—");
            swr.set(live && st.swr() >= 1 ? Math.min(1, (st.swr() - 1) / 2.0) : 0, live && st.swr() >= 1 ? trim(st.swr()) + ":1" : "—");
            pwr.set(live && st.rfPowerFrac() >= 0 ? st.rfPowerFrac() : 0, live && st.rfPowerFrac() >= 0 ? Math.round(st.rfPowerFrac() * 100) + "%" : "—");
            alc.set(0, "—");   // no CAT source yet — honest placeholder

            String band = live && st.freqHz() > 0 ? bandOf(st.freqHz()) : null;
            highlight(bandBtns, band);
            highlight(modeBtns, uiMode(mode));
            bmSum.setText((band != null ? band + "m" : "—") + " · " + mode);
            ffSum.setText(filtSel[0] + (funcsOn.isEmpty() ? "" : " · " + String.join(" ", funcsOn)));
            dlSum.setText("AGC " + agcSel[0]);
        };
        rig.setListener(s -> Platform.runLater(render));
        rig.start();
        render.run();
        return panel;
    }

    // ── small builders ──────────────────────────────────────────────────────
    /** A vertical FAST/SLOW stepper: ▲ up + ▼ down, each press-and-hold repeats. */
    private static VBox stepCol(String label, Runnable up, Runnable down) {
        Label u = holdBtn("▲", up), d = holdBtn("▼", down);
        VBox v = new VBox(4, u, lbl(label, "rig-step-k"), d); v.setAlignment(Pos.CENTER);
        return v;
    }
    private static Label holdBtn(String text, Runnable action) {
        Label b = lbl(text, "rig-step"); b.setStyle("-fx-cursor:hand;");
        Timeline t = new Timeline(new KeyFrame(Duration.millis(90), e -> action.run()));
        t.setCycleCount(Animation.INDEFINITE);
        b.setOnMousePressed(e -> { action.run(); t.playFromStart(); });
        b.setOnMouseReleased(e -> t.stop());
        b.setOnMouseExited(e -> t.stop());
        return b;
    }
    private static Label toggle(String text, Runnable action) {
        Label b = lbl(text, "rig-vfo"); b.setAlignment(Pos.CENTER); b.setMaxWidth(Double.MAX_VALUE); b.setStyle("-fx-cursor:hand;");
        if (action != null) b.setOnMouseClicked(e -> action.run());
        return b;
    }
    private static Label footBtn(String text, Runnable action) {
        Label b = lbl(text, "rig-foot-btn"); b.setAlignment(Pos.CENTER); b.setMaxWidth(Double.MAX_VALUE); b.setStyle("-fx-cursor:hand;");
        if (action != null) b.setOnMouseClicked(e -> action.run());
        return b;
    }
    private static Label keyBtn(String text, Runnable action) {
        Label b = lbl(text, "rig-key"); b.setAlignment(Pos.CENTER); GridPane.setHgrow(b, Priority.ALWAYS); b.setMaxWidth(Double.MAX_VALUE); b.setStyle("-fx-cursor:hand;");
        if (action != null) b.setOnMouseClicked(e -> action.run());
        return b;
    }
    private static GridPane grid(int cols) {
        GridPane g = new GridPane(); g.setHgap(5); g.setVgap(5);
        for (int c = 0; c < cols; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(100.0 / cols); g.getColumnConstraints().add(cc); }
        return g;
    }
    private static HBox levelRow(RigClient rig, String label, String catName, double init) {
        Label k = lbl(label, "rig-lvl-k"); k.setMinWidth(34);
        Slider s = new Slider(0, 100, init); s.getStyleClass().add("rig-slider"); HBox.setHgrow(s, Priority.ALWAYS); s.setMaxWidth(Double.MAX_VALUE);
        Label v = lbl((int) init + "", "rig-lvl-v"); v.setMinWidth(30); v.setAlignment(Pos.CENTER_RIGHT);
        s.valueProperty().addListener((o, a, b) -> v.setText(String.valueOf((int) Math.round(b.doubleValue()))));
        s.setOnMouseReleased(e -> rig.setLevel(catName, s.getValue() / 100.0));
        HBox row = new HBox(8, k, s, v); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** A nested collapsible sub-drawer with a one-line summary shown when closed. */
    private static VBox sub(String title, String hue, Label summary, boolean open, Node body) {
        VBox dw = new VBox(); dw.getStyleClass().add("rig-sub"); if (open) dw.getStyleClass().add("open");
        StackPane ic = Shell.iconTile(hue, hue, 12, "rig-sub-ic", hue);
        Label t = lbl(title, "rig-sub-t"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
        Label cv = lbl("›", "rig-sub-cv"); cv.setRotate(open ? 90 : 0);
        HBox head = new HBox(8, ic, t, summary, cv); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("rig-sub-head");
        VBox bodyBox = new VBox(body); bodyBox.getStyleClass().add("rig-sub-body");
        bodyBox.setManaged(open); bodyBox.setVisible(open);
        summary.setVisible(!open); summary.setManaged(!open);
        head.setOnMouseClicked(e -> {
            boolean now = !dw.getStyleClass().contains("open");
            dw.getStyleClass().remove("open"); if (now) dw.getStyleClass().add("open");
            bodyBox.setManaged(now); bodyBox.setVisible(now);
            summary.setVisible(!now); summary.setManaged(!now); cv.setRotate(now ? 90 : 0);
        });
        dw.getChildren().addAll(head, bodyBox);
        return dw;
    }
    private static HBox barCell(String k, Bar bar) {
        VBox v = new VBox(2, lbl(k, "rig-bar-k"), bar.node, bar.val); v.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(v, Priority.ALWAYS); return new HBox(v);
    }
    private static void highlight(Map<String,Label> btns, String on) {
        btns.forEach((k, v) -> { v.getStyleClass().remove("on"); if (k.equals(on)) v.getStyleClass().add("on"); });
    }

    /** A thin horizontal bar (track + left-anchored fill) with a value caption. */
    private static final class Bar {
        final StackPane node; final Region fill; final Label val;
        Bar(String fillHue) {
            Region track = new Region(); track.getStyleClass().add("rig-bar-track");
            fill = new Region(); fill.getStyleClass().addAll("rig-bar-fill", fillHue); fill.setMinWidth(0); fill.setPrefWidth(0); fill.setMaxWidth(0);
            node = new StackPane(track, fill); node.setAlignment(Pos.CENTER_LEFT);
            node.setMinHeight(6); node.setMaxHeight(6); node.setMaxWidth(Double.MAX_VALUE);
            val = lbl("—", "rig-bar-v");
        }
        void set(double frac, String text) {
            double w = node.getWidth() > 0 ? node.getWidth() : 84;
            double f = Math.max(0, Math.min(1, frac)) * w;
            fill.setMinWidth(f); fill.setPrefWidth(f); fill.setMaxWidth(f);
            val.setText(text);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────
    /** Parse "14.074.00" / "14.074" (MHz) / "14074" (kHz) / "14074000" (Hz) → Hz, or -1. */
    static long parseFreqInput(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().replace(" ", "");
        if (s.isEmpty() || s.contains("—")) return -1;
        int dots = (int) s.chars().filter(c -> c == '.').count();
        try {
            if (dots >= 2) {                                   // MHz.kHz.tens display form
                String[] p = s.split("\\.");
                long mhz = Long.parseLong(p[0]);
                long khz = p.length > 1 && !p[1].isEmpty() ? Long.parseLong(p[1]) : 0;
                long tens = p.length > 2 && !p[2].isEmpty() ? Long.parseLong((p[2] + "00").substring(0, 2)) : 0;
                return mhz * 1_000_000 + khz * 1_000 + tens * 10;
            }
            double d = Double.parseDouble(s);
            if (dots == 1) return Math.round(d * 1_000_000);   // MHz
            if (d < 1_000) return Math.round(d * 1_000_000);   // bare MHz (e.g. "14")
            if (d < 100_000) return Math.round(d * 1_000);     // kHz
            return Math.round(d);                              // Hz
        } catch (NumberFormatException e) { return -1; }
    }
    private static String trim(double swr) { return String.format("%.1f", swr); }
    /** UI mode token → Hamlib mode passed to {@code M}. */
    private static String catMode(String ui) {
        return switch (ui) { case "CW-R" -> "CWR"; case "DATA" -> "PKTUSB"; default -> ui; };
    }
    /** Live rig mode → the UI button it should light. */
    private static String uiMode(String m) {
        if (m == null) return "";
        String u = m.trim().toUpperCase();
        return switch (u) {
            case "CWR" -> "CW-R";
            case "PKTUSB", "PKTLSB", "PKT", "FT8", "FT4", "DIGI" -> "DATA";
            case "FSK", "FSKR" -> "RTTY";
            default -> u;
        };
    }
    /** Frequency (Hz) → ham band token ("20"), or null off-band. */
    private static String bandOf(long hz) {
        double m = hz / 1_000_000.0;
        if (m >= 1.8 && m <= 2.0)     return "160";
        if (m >= 3.5 && m <= 4.0)     return "80";
        if (m >= 7.0 && m <= 7.3)     return "40";
        if (m >= 10.1 && m <= 10.15)  return "30";
        if (m >= 14.0 && m <= 14.35)  return "20";
        if (m >= 18.068 && m <= 18.168) return "17";
        if (m >= 21.0 && m <= 21.45)  return "15";
        if (m >= 24.89 && m <= 24.99) return "12";
        if (m >= 28.0 && m <= 29.7)   return "10";
        if (m >= 50.0 && m <= 54.0)   return "6";
        return null;
    }
}
