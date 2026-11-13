package com.ars.fx.shell;

import com.ars.fx.data.RigClient;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import static com.ars.fx.shell.Shell.lbl;

/**
 * Canonical rig-control panel — a 1:1 build of {@code design/ars-suite/Rig Control Drawer.html}.
 * One outer drawer (pinned header + pinned ATU/TUNE/PTT footer, scrolling middle) holding three
 * independently collapsible sub-drawers: Band &amp; Mode, Filter &amp; Functions, DSP &amp; Levels.
 *
 * <p>Middle, top→bottom: meters strip (segmented S-meter + SWR/PWR/ALC mini-bars) → VFO/frequency
 * pane (VFO A/B + SPLIT, FAST ▲▼ left · colored monospace direct-entry centre · SLOW ▲▼ right,
 * VFO-B line + step legend) → the three sub-drawers. Tokens/monospace only; no blur/filters.
 * Live state + commands go through {@link RigClient}.
 */
public final class RigPanel {
    private RigPanel() {}

    private static final long FAST_HZ = 1_000, SLOW_HZ = 10;
    private static final String[] BANDS = {"160","80","60","40","30","20","17","15","12","10","6","GEN"};
    private static final String[] MODES = {"USB","LSB","CW","CW-R","RTTY","FT8","AM","FM"};
    private static final String[] FUNCS = {"SPLIT","RIT","XIT","A=B","A/B","VOX","COMP","LOCK"};
    private static final String[] FUNC_SUB = {"TX","0","0","copy","swap","vox","comp","dial"};
    private static final String[] DSP = {"AGC","PRE","ATT","NB","NR","NOTCH"};
    private static final String[] DSP_VAL = {"MID","1","off","5","3","off"};

    /** Persisted open/closed state per sub-drawer title (survives panel rebuilds). */
    private static final Map<String,Boolean> SUB_STATE = new java.util.HashMap<>();

    /** Build the rig-control rail drawer (header + collapsible body). */
    public static Region build() {
        RigClient rig = RigClient.getInstance();

        // ── header (pinned) ─────────────────────────────────────────────────
        // header matches the other rail drawers (sx-dw): icon · title · summary-when-collapsed · chevron
        StackPane ic = Shell.iconTile("bridge", "bridge", 14, "sx-dw-ic", "bridge");
        Label title = lbl("Rig control", "sx-dw-t"); HBox.setHgrow(title, Priority.ALWAYS); title.setMaxWidth(Double.MAX_VALUE);
        Label headSum = lbl("", "sx-dw-sum");
        Label cv = lbl("›", "sx-dw-cv");
        HBox header = new HBox(9, ic, title, headSum, cv);
        header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("sx-dw-head");
        // rig model + CAT status sits at the top of the body
        Region connDot = new Region(); connDot.getStyleClass().add("rc-conn-d");
        Label connTx = lbl("CAT", "rc-conn-t");
        Region connSp = new Region(); HBox.setHgrow(connSp, Priority.ALWAYS);
        HBox statusRow = new HBox(6, lbl(rigLabel(), "rc-sub"), connSp, connDot, connTx);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        // ── meters strip ────────────────────────────────────────────────────
        Label sK = lbl("S-meter", "rc-meter-k");
        Label sV = lbl("—", "rc-meter-v");
        Region sSp = new Region(); HBox.setHgrow(sSp, Priority.ALWAYS);
        HBox sTop = new HBox(sK, sSp, sV); sTop.setAlignment(Pos.BOTTOM_LEFT);
        Region[] seg = new Region[22];
        HBox segBox = new HBox(2); segBox.setAlignment(Pos.BOTTOM_LEFT); segBox.getStyleClass().add("rc-seg");
        for (int i = 0; i < seg.length; i++) {
            double h = 6 + (i / 21.0) * 10;             // ramped S-meter scale
            seg[i] = new Region(); seg[i].getStyleClass().add("rc-seg-i");
            seg[i].setMinHeight(h); seg[i].setPrefHeight(h); seg[i].setMaxHeight(h);
            HBox.setHgrow(seg[i], Priority.ALWAYS); seg[i].setMaxWidth(Double.MAX_VALUE);
            segBox.getChildren().add(seg[i]);
        }
        HBox segLbl = new HBox(); segLbl.getStyleClass().add("rc-seg-lbl");
        String[] ticks = {"1","3","5","7","9","+20","+40"};
        for (int i = 0; i < ticks.length; i++) { if (i > 0) { Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS); segLbl.getChildren().add(sp); } segLbl.getChildren().add(lbl(ticks[i], "rc-seg-tick")); }
        VBox meter = new VBox(4, sTop, segBox, segLbl);

        MiniBar swr = new MiniBar("ok"), pwr = new MiniBar("accent"), alc = new MiniBar("warn");
        GridPane mini3 = new GridPane(); mini3.setHgap(10);
        for (int c = 0; c < 3; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(100.0 / 3); mini3.getColumnConstraints().add(cc); }
        mini3.add(miniCell("SWR", swr), 0, 0); mini3.add(miniCell("PWR", pwr), 1, 0); mini3.add(miniCell("ALC", alc), 2, 0);
        VBox.setMargin(mini3, new Insets(9, 0, 0, 0));
        VBox metstrip = new VBox(meter, mini3); metstrip.getStyleClass().add("rc-card");

        // ── VFO + frequency pane ────────────────────────────────────────────
        Label vfoA = vfoBtn("VFO A"), vfoB = vfoBtn("VFO B");
        Label splitTag = lbl("SPLIT", "rc-tag", "split");
        Region vfoSp = new Region(); HBox.setHgrow(vfoSp, Priority.ALWAYS);
        HBox vfoTop = new HBox(6, vfoA, vfoB, vfoSp, splitTag); vfoTop.setAlignment(Pos.CENTER_LEFT);
        vfoA.setOnMouseClicked(e -> rig.setVfo("VFOA"));
        vfoB.setOnMouseClicked(e -> rig.setVfo("VFOB"));
        splitTag.setStyle("-fx-cursor:hand;");
        splitTag.setOnMouseClicked(e -> rig.setSplit(!rig.last().split()));

        Label fMhz = lbl("—", "rc-freq-mhz"), fDot1 = lbl(".", "rc-freq-dot"), fKhz = lbl("—", "rc-freq-khz"),
              fDot2 = lbl(".", "rc-freq-dot"), fHz = lbl("—", "rc-freq-hz");
        HBox freqDisp = new HBox(2, fMhz, fDot1, fKhz, fDot2, fHz); freqDisp.setAlignment(Pos.BASELINE_CENTER);
        freqDisp.setStyle("-fx-cursor:text;");
        TextField freqEdit = new TextField(); freqEdit.getStyleClass().add("rc-freqin"); freqEdit.setAlignment(Pos.CENTER);
        freqEdit.setManaged(false); freqEdit.setVisible(false);
        StackPane freqStack = new StackPane(freqDisp, freqEdit);
        Label freqUnit = lbl("MHz", "rc-freq-u");
        VBox freqMid = new VBox(3, freqStack, freqUnit); freqMid.setAlignment(Pos.CENTER); HBox.setHgrow(freqMid, Priority.ALWAYS);

        Runnable enterEdit = () -> {
            long hz = rig.last().freqHz();
            freqEdit.setText(hz > 0 ? RigClient.fmtFreqFull(hz) : "");
            freqDisp.setVisible(false); freqDisp.setManaged(false);
            freqEdit.setManaged(true); freqEdit.setVisible(true);
            freqEdit.requestFocus(); freqEdit.positionCaret(freqEdit.getText().length());
        };
        Runnable exitEdit = () -> { freqEdit.setManaged(false); freqEdit.setVisible(false); freqDisp.setVisible(true); freqDisp.setManaged(true); };
        Runnable commit = () -> { long hz = parseFreqInput(freqEdit.getText()); if (hz > 0) rig.setFreqHz(hz); exitEdit.run(); };
        freqDisp.setOnMouseClicked(e -> enterEdit.run());
        freqEdit.setOnAction(e -> commit.run());
        freqEdit.focusedProperty().addListener((o, was, is) -> { if (!is) commit.run(); });

        VBox fastCol = nudgeCol("Fast", true, () -> rig.nudgeFreq(FAST_HZ), () -> rig.nudgeFreq(-FAST_HZ));
        VBox slowCol = nudgeCol("Slow", false, () -> rig.nudgeFreq(SLOW_HZ), () -> rig.nudgeFreq(-SLOW_HZ));
        HBox freqRow = new HBox(8, fastCol, freqMid, slowCol); freqRow.setAlignment(Pos.CENTER);

        Label vfoBLine = lbl("", "rc-vfo-b"); vfoBLine.setMaxWidth(Double.MAX_VALUE); vfoBLine.setStyle("-fx-cursor:hand;");
        Tips.tip(vfoBLine, "Split: the transmit frequency. Click to type it, scroll to step it by 1 kHz (Shift: 100 Hz).");
        TextField txEdit = new TextField(); txEdit.getStyleClass().add("rc-freqin"); txEdit.setAlignment(Pos.CENTER); txEdit.setManaged(false); txEdit.setVisible(false);
        StackPane txStack = new StackPane(vfoBLine, txEdit);
        Runnable txExit = () -> { txEdit.setManaged(false); txEdit.setVisible(false); vfoBLine.setVisible(true); vfoBLine.setManaged(true); };
        Runnable txCommit = () -> { long hz = parseFreqInput(txEdit.getText()); if (hz > 0) rig.setSplitFreqHz(hz); txExit.run(); };
        vfoBLine.setOnMouseClicked(e -> {
            if (!rig.last().split()) return;
            long hz = rig.splitFreqHz(); txEdit.setText(hz > 0 ? RigClient.fmtFreqFull(hz) : "");
            vfoBLine.setVisible(false); vfoBLine.setManaged(false); txEdit.setManaged(true); txEdit.setVisible(true);
            txEdit.requestFocus(); txEdit.positionCaret(txEdit.getText().length());
        });
        txEdit.setOnAction(e -> txCommit.run());
        txEdit.focusedProperty().addListener((o, was, is) -> { if (!is) txCommit.run(); });
        vfoBLine.setOnScroll(e -> {
            if (!rig.last().split() || e.getDeltaY() == 0) return;
            long base = rig.splitFreqHz(); if (base <= 0) return;
            rig.setSplitFreqHz(base + (e.getDeltaY() > 0 ? 1 : -1) * (e.isShiftDown() ? 100L : 1000L)); e.consume();
        });
        // quick offsets from the receive frequency, for working a station that is listening up
        HBox txChips = new HBox(6); txChips.setAlignment(Pos.CENTER);
        txChips.getChildren().add(cellBtn("=RX", "rc-mode", () -> rig.setSplitFreqHz(rig.last().freqHz())));
        for (int k : new int[]{1, 2, 3, 5}) txChips.getChildren().add(cellBtn("+" + k, "rc-mode", () -> rig.setSplitFreqHz(rig.last().freqHz() + k * 1000L)));
        for (Node n : txChips.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);
        Region rateSp = new Region(); HBox.setHgrow(rateSp, Priority.ALWAYS);
        HBox rate = new HBox(legend("Fast step", "1 kHz"), rateSp, legend("Slow step", "10 Hz"));
        VBox.setMargin(vfoBLine, new Insets(6, 0, 0, 0)); VBox.setMargin(rate, new Insets(9, 0, 0, 0));
        VBox vfoCard = new VBox(9, vfoTop, freqRow, txStack, txChips, rate); vfoCard.getStyleClass().add("rc-card");

        // ── sub-drawer 1: Band & Mode ───────────────────────────────────────
        Map<String,Label> bandBtns = new LinkedHashMap<>();
        GridPane bandGrid = grid(6, 5);
        for (int i = 0; i < BANDS.length; i++) { String b = BANDS[i]; Label l = cellBtn(b, "rc-band", () -> rig.setBand(b)); bandBtns.put(b, l); bandGrid.add(l, i % 6, i / 6); }
        Map<String,Label> modeBtns = new LinkedHashMap<>();
        GridPane modeGrid = grid(4, 5);
        for (int i = 0; i < MODES.length; i++) { String m = MODES[i]; Label l = cellBtn(m, "rc-mode", () -> rig.setMode(catMode(m))); modeBtns.put(m, l); modeGrid.add(l, i % 4, i / 4); }
        VBox bmBody = new VBox(11, section("Band", bandGrid), section("Mode", modeGrid));
        Label bmSum = lbl("20m · USB", "rc-subdw-sum");
        VBox sub1 = subdw("Band & Mode", bmSum, false, bmBody);

        // ── sub-drawer 2: Filter & Functions ────────────────────────────────
        Region pbPass = new Region(); pbPass.getStyleClass().add("rc-pb-pass");
        javafx.beans.property.DoubleProperty pbFrac = new SimpleDoubleProperty(0.42);
        Pane pb = passband(pbPass, pbFrac);
        Label bwVal = lbl("—", "rc-filt-v");
        // NR and notch are in DSP & Levels, and the IC-7300 has no IF shift over CAT, so the bandwidth is the one filter control here
        GridPane filt = grid(1, 6);
        VBox bwTile = filtTile("BW", bwVal);
        Tips.tip(bwTile, "Receive bandwidth: click to step through the usual widths, scroll to change it (50 Hz a notch below 1 kHz, else 100 Hz).");
        bwTile.setOnMouseClicked(e -> { RigClient.State st = rig.last(); if (st.connected() && st.passbandHz() > 0) rig.setPassbandHz(nextBw(st.mode(), st.passbandHz())); });
        bwTile.setOnScroll(e -> {
            RigClient.State st = rig.last(); if (!st.connected() || st.passbandHz() <= 0 || e.getDeltaY() == 0) return;
            long cur = st.passbandHz(), step = cur < 1000 ? 50 : 100;
            rig.setPassbandHz(cur + (e.getDeltaY() > 0 ? step : -step)); e.consume();
        });
        filt.add(bwTile, 0, 0);
        VBox filterSec = section("Filter · passband", new VBox(7, pb, filt));
        Map<String,Label> fnBtns = new LinkedHashMap<>();
        GridPane fnGrid = grid(4, 5);
        for (int i = 0; i < FUNCS.length; i++) {
            String fn = FUNCS[i]; Label l = fnTile(fn, FUNC_SUB[i]); fnBtns.put(fn, l);
            l.setOnMouseClicked(e -> {
                RigClient.State cur = rig.last();
                if (("RIT".equals(fn) || "XIT".equals(fn)) && e.getButton() == javafx.scene.input.MouseButton.SECONDARY) {   // right-click: back to zero
                    if ("RIT".equals(fn)) rig.setRitHz(0); else rig.setXitHz(0);
                    return;
                }
                switch (fn) {
                    case "SPLIT" -> rig.setSplit(!cur.split());
                    case "VOX", "LOCK", "COMP", "RIT", "XIT" -> rig.setFunc(fn, !cur.func(fn));
                    case "A=B" -> rig.vfoOp("CPY");
                    case "A/B" -> rig.vfoOp("XCHG");
                    default -> { }
                }
            });
            if ("RIT".equals(fn) || "XIT".equals(fn)) {           // scroll for the offset: 10 Hz a notch, Shift for 100 Hz
                String key = fn + "_HZ";
                l.setOnScroll(e -> {
                    if (e.getDeltaY() == 0) return;
                    double c = rig.last().level(key); long cur = Double.isNaN(c) ? 0 : Math.round(c);
                    long next = cur + (e.getDeltaY() > 0 ? 1 : -1) * (e.isShiftDown() ? 100L : 10L);
                    if ("RIT".equals(fn)) rig.setRitHz(next); else rig.setXitHz(next);
                    e.consume();
                });
                Tips.tip(l, fn + ": click to switch on or off, scroll to change the offset (10 Hz a notch, Shift for 100 Hz), right-click for zero.");
            }
            fnGrid.add(l, i % 4, i / 4);
        }
        VBox ffBody = new VBox(11, filterSec, section("Functions", fnGrid));
        Label ffSum = lbl("—", "rc-subdw-sum");
        VBox sub2 = subdw("Filter & Functions", ffSum, false, ffBody);

        // ── sub-drawer 3: DSP & Levels ──────────────────────────────────────
        Map<String,Label> dspHosts = new LinkedHashMap<>(), dspVals = new LinkedHashMap<>();
        GridPane dspGrid = grid(3, 5);
        for (int i = 0; i < DSP.length; i++) {
            Label v = lbl("—", "rc-dsp-v"); Label host = dspTile(DSP[i], v);
            dspHosts.put(DSP[i], host); dspVals.put(DSP[i], v);
            dspGrid.add(host, i % 3, i / 3);
            wireDsp(rig, DSP[i], host);
        }
        Map<String,Slider> sliders = new LinkedHashMap<>(); Map<String,Label> sliderVals = new LinkedHashMap<>();
        VBox levels = new VBox(9,
                level(rig, "PWR", "RFPOWER", sliders, sliderVals),
                level(rig, "MIC", "MICGAIN", sliders, sliderVals),
                level(rig, "RF G", "RF", sliders, sliderVals),
                level(rig, "AF", "AF", sliders, sliderVals));
        VBox dspBody = new VBox(11, section("DSP & front end", dspGrid), section("Levels", levels));
        Label dlSum = lbl("—", "rc-subdw-sum");
        VBox sub3 = subdw("DSP & Levels", dlSum, false, dspBody);

        // ── footer action bar (ATU · TUNE · PTT) ────────────────────────────
        Label atuB = actBtn("ATU", "atu", null);
        atuB.setOnMouseClicked(e -> rig.setFunc("TUNER", !atuB.getStyleClass().contains("on")));
        Label tuneB = actBtn("TUNE", "tune", rig::antTune);
        Label pttB = actBtn("● PTT", "ptt", null);
        // push-to-talk: keyed while the button is held, released when it is let go, when the pointer slips off it, or when the window loses focus
        boolean[] held = {false};
        Runnable release = () -> { if (held[0]) { held[0] = false; rig.setPtt(false); } };
        pttB.setOnMousePressed(e -> { if (e.isPrimaryButtonDown() && !held[0]) { held[0] = true; rig.setPtt(true); } });
        pttB.setOnMouseReleased(e -> release.run());
        pttB.setOnMouseExited(e -> release.run());
        pttB.sceneProperty().addListener((o, was, now) -> { if (now != null) now.windowProperty().addListener((ow, wo, wn) -> { if (wn != null) wn.focusedProperty().addListener((of, f0, f1) -> { if (!f1) release.run(); }); }); });
        GridPane foot = new GridPane(); foot.setHgap(8); foot.getStyleClass().add("rc-foot");
        double[] fw = {29, 29, 42};
        for (int c = 0; c < 3; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(fw[c]); foot.getColumnConstraints().add(cc); }
        foot.add(atuB, 0, 0); foot.add(tuneB, 1, 0); foot.add(pttB, 2, 0);

        // ── assemble as a rail drawer (sx-dw idiom): click header to open/collapse ──
        VBox content = new VBox(11, statusRow, metstrip, vfoCard, sub1, sub2, sub3, foot);
        VBox bodyBox = new VBox(content); bodyBox.getStyleClass().add("sx-dw-body");
        VBox panel = new VBox(header, bodyBox); panel.getStyleClass().add("sx-dw"); panel.setMaxWidth(Double.MAX_VALUE);

        boolean open = SUB_STATE.getOrDefault("Rig control", false);
        if (open) panel.getStyleClass().add("open");
        cv.setRotate(open ? 90 : 0); headSum.setVisible(!open); headSum.setManaged(!open);
        bodyBox.setManaged(open); bodyBox.setVisible(open);
        header.setOnMouseClicked(e -> {
            boolean now = !panel.getStyleClass().contains("open");
            panel.getStyleClass().remove("open"); if (now) panel.getStyleClass().add("open");
            bodyBox.setManaged(now); bodyBox.setVisible(now);
            headSum.setVisible(!now); headSum.setManaged(!now); cv.setRotate(now ? 90 : 0);
            SUB_STATE.put("Rig control", now);
        });

        // ── live render ─────────────────────────────────────────────────────
        Consumer<RigClient.State> render = st -> {
            boolean live = st != null && st.connected();
            connDot.getStyleClass().remove("off"); if (!live) connDot.getStyleClass().add("off");
            connTx.getStyleClass().remove("off"); if (!live) connTx.getStyleClass().add("off");
            connTx.setText(live ? "CAT" : "no CAT");

            long hz = live ? st.freqHz() : 0;
            if (!freqEdit.isVisible()) {
                if (hz > 0) { fMhz.setText(String.valueOf(hz / 1_000_000)); fKhz.setText(String.format("%03d", (hz / 1000) % 1000)); fHz.setText(String.format("%03d", hz % 1000)); }
                else { fMhz.setText("—"); fKhz.setText("—"); fHz.setText("—"); }
            }
            boolean tx = live && st.ptt();
            long bHz = live ? st.vfoBHz() : 0;
            boolean spl = live && st.split();
            vfoBLine.setText(!live ? "" : spl ? "TX  " + (bHz > 0 ? RigClient.fmtFreqFull(bHz) : "—") + (bHz > 0 && hz > 0 ? "  (" + fmtOffset(bHz - hz) + ")" : "") : bHz > 0 ? "B  " + RigClient.fmtFreqFull(bHz) + "  ·  split off" : "Split off");
            txChips.setVisible(spl); txChips.setManaged(spl);

            // meters
            int lit = live ? (int) Math.round(Math.max(0, Math.min(22, (st.sMeterDb() + 54.0) / 94.0 * 22))) : 0;
            for (int s = 0; s < seg.length; s++) { seg[s].getStyleClass().removeAll("lit", "hi"); if (s < lit) { seg[s].getStyleClass().add("lit"); if (s >= 15) seg[s].getStyleClass().add("hi"); } }
            sV.setText(live ? RigClient.sMeterText(st.sMeterDb()) : "—");
            swr.set(live && st.swr() >= 1 ? Math.min(1, (st.swr() - 1) / 2.0) : 0, live && st.swr() >= 1 ? String.format("%.1f:1", st.swr()) : "—");
            pwr.set(live && st.rfPowerFrac() >= 0 ? st.rfPowerFrac() : 0, live && st.rfPowerFrac() >= 0 ? Math.round(st.rfPowerFrac() * 100) + "W" : "—");
            double a = live ? st.alc() : -1; alc.set(a > 0 ? a : 0, a > 0 ? String.valueOf(Math.round(a * 100)) : "—");

            // band + mode
            String band = live && hz > 0 ? bandOf(hz) : null;
            highlight(bandBtns, band);
            String mode = live && st.mode() != null && !st.mode().isBlank() ? st.mode() : null;
            highlight(modeBtns, mode == null ? null : uiMode(mode));
            bmSum.setText((band != null ? band : "—") + " · " + (mode != null ? uiMode(mode) : "—"));
            headSum.setText(live && hz > 0 ? RigClient.fmtFreqShort(hz) + " · " + (mode != null ? uiMode(mode) : "—") : "offline");

            // tx state: split tag, PTT, ATU
            toggleClass(splitTag, live && st.split());
            pttB.getStyleClass().remove("tx"); if (tx) pttB.getStyleClass().add("tx");
            toggleClass(atuB, live && st.func("TUNER"));

            // readable function toggles
            toggleClass(fnBtns.get("SPLIT"), live && st.split());
            toggleClass(fnBtns.get("VOX"), live && st.func("VOX"));
            toggleClass(fnBtns.get("LOCK"), live && st.func("LOCK"));
            toggleClass(fnBtns.get("COMP"), live && st.func("COMP"));
            toggleClass(fnBtns.get("RIT"), live && st.func("RIT"));
            toggleClass(fnBtns.get("XIT"), live && st.func("XIT"));
            setSub(fnBtns.get("RIT"), live ? fmtOffset(Math.round(nz(st.level("RIT_HZ")))) : "0");
            setSub(fnBtns.get("XIT"), live ? fmtOffset(Math.round(nz(st.level("XIT_HZ")))) : "0");
            setSub(fnBtns.get("SPLIT"), live && st.split() && st.vfoBHz() > 0 && hz > 0 ? fmtOffset(st.vfoBHz() - hz) : "TX");

            // filter / passband
            long pbHz = live ? st.passbandHz() : 0;
            bwVal.setText(pbHz > 0 ? fmtBw(pbHz) : "—");
            pbFrac.set(pbHz > 0 ? Math.max(0.08, Math.min(0.92, pbHz / 5000.0)) : 0.42);

            // DSP grid
            dspSet(dspHosts, dspVals, "AGC", live && st.level("AGC") > 0, live ? agcShort(st.level("AGC")) : "—");
            dspSet(dspHosts, dspVals, "PRE", live && st.level("PREAMP") > 0, live && st.level("PREAMP") > 0 ? String.valueOf((int) st.level("PREAMP")) : "off");
            dspSet(dspHosts, dspVals, "ATT", live && st.level("ATT") > 0, live && st.level("ATT") > 0 ? (int) st.level("ATT") + "" : "off");
            dspSet(dspHosts, dspVals, "NB", live && st.func("NB"), live && st.func("NB") ? lvlInt(st, "NB", "on") : "off");
            dspSet(dspHosts, dspVals, "NR", live && st.func("NR"), live && st.func("NR") ? lvlInt(st, "NR", "on") : "off");
            dspSet(dspHosts, dspVals, "NOTCH", live && st.func("ANF"), live && st.func("ANF") ? "auto" : "off");

            // level sliders (skip ones the user is dragging)
            levelSet(sliders, sliderVals, "PWR", live ? st.level("RFPOWER") : Double.NaN, true);
            levelSet(sliders, sliderVals, "MIC", live ? st.level("MICGAIN") : Double.NaN, false);
            levelSet(sliders, sliderVals, "RF G", live ? st.level("RF") : Double.NaN, false);
            levelSet(sliders, sliderVals, "AF", live ? st.level("AF") : Double.NaN, false);

            // summaries
            ffSum.setText((pbHz > 0 ? fmtBw(pbHz) : "—") + " · " + (live && st.split() ? "SPLIT" : live && st.func("VOX") ? "VOX" : "—"));
            double pw = live ? st.level("RFPOWER") : Double.NaN;
            dlSum.setText("AGC " + (live ? agcShort(st.level("AGC")) : "—") + " · " + (Double.isNaN(pw) ? "—" : Math.round(pw * 100) + "W"));
        };
        // subscribe; drop our listener when the panel leaves the scene (overlay closed) and take it up again when the panel comes back,
        // otherwise a panel that is pinned / unpinned / re-docked stays frozen on its last values
        Consumer<RigClient.State> l = s -> Platform.runLater(() -> render.accept(s));
        panel.sceneProperty().addListener((o, a, b) -> {
            rig.removeListener(l);
            if (b != null) rig.addListener(l);   // addListener also pushes the current state straight away
        });
        rig.addListener(l);
        rig.start();
        return panel;
    }

    /** The rig as set up in J-Hub (model, and the interface when there is one). */
    private static String rigLabel() {
        String m = com.ars.fx.data.HubConfig.get("rig.model", "").trim(), i = com.ars.fx.data.HubConfig.get("rig.interface", "").trim();
        if (m.isEmpty()) return "Rig";
        return i.isEmpty() ? m : m + " · " + i;
    }

    // ── builders ────────────────────────────────────────────────────────────
    private static Label vfoBtn(String t) { Label l = lbl(t, "rc-vfobtn"); l.setStyle("-fx-cursor:hand;"); return l; }
    private static VBox nudgeCol(String label, boolean fast, Runnable up, Runnable down) {
        Label l = lbl(label, "rc-nudge-lbl");
        Label u = arrow("▲", fast, up), d = arrow("▼", fast, down);
        VBox.setVgrow(u, Priority.ALWAYS); VBox.setVgrow(d, Priority.ALWAYS);
        VBox col = new VBox(4, l, u, d); col.setAlignment(Pos.TOP_CENTER);
        col.setMinWidth(42); col.setPrefWidth(42); col.setMaxWidth(42);
        return col;
    }
    private static Label arrow(String glyph, boolean fast, Runnable action) {
        Label b = lbl(glyph, "rc-arr"); if (fast) b.getStyleClass().add("fast");
        b.setAlignment(Pos.CENTER); b.setMaxWidth(Double.MAX_VALUE); b.setMaxHeight(Double.MAX_VALUE); b.setStyle("-fx-cursor:hand;");
        Timeline t = new Timeline(new KeyFrame(Duration.millis(90), e -> action.run())); t.setCycleCount(Animation.INDEFINITE);
        b.setOnMousePressed(e -> { action.run(); t.playFromStart(); });
        b.setOnMouseReleased(e -> t.stop()); b.setOnMouseExited(e -> t.stop());
        return b;
    }
    private static HBox legend(String k, String v) {
        HBox h = new HBox(5, lbl(k, "rc-leg-k"), lbl(v, "rc-leg-v")); h.setAlignment(Pos.CENTER_LEFT); return h;
    }
    private static VBox section(String label, Node body) {
        VBox v = new VBox(6, lbl(label, "rc-sec-lbl"), body); return v;
    }
    private static GridPane grid(int cols, double hvgap) {
        GridPane g = new GridPane(); g.setHgap(hvgap); g.setVgap(hvgap);
        for (int c = 0; c < cols; c++) { ColumnConstraints cc = new ColumnConstraints(); cc.setPercentWidth(100.0 / cols); g.getColumnConstraints().add(cc); }
        return g;
    }
    private static Label cellBtn(String text, String styleClass, Runnable action) {
        Label b = lbl(text, styleClass); b.setAlignment(Pos.CENTER); b.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(b, Priority.ALWAYS); b.setStyle("-fx-cursor:hand;");
        if (action != null) b.setOnMouseClicked(e -> action.run());
        return b;
    }
    private static VBox filtTile(String k, Label vLabel) {
        VBox t = new VBox(2, lbl(k, "rc-filt-k"), vLabel); t.setAlignment(Pos.CENTER); t.getStyleClass().add("rc-filt");
        GridPane.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE); t.setStyle("-fx-cursor:hand;");
        return t;
    }
    private static Label fnTile(String name, String subLabel) {
        Label t = lbl(name, "rc-fn-t"); Label s = lbl(subLabel, "rc-fn-s");
        VBox box = new VBox(3, t, s); box.setAlignment(Pos.CENTER);
        // a Label host is convenient for style toggling; wrap via graphic
        Label host = new Label(); host.setGraphic(box); host.getStyleClass().add("rc-fn"); host.setAlignment(Pos.CENTER);
        host.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(host, Priority.ALWAYS); host.setStyle("-fx-cursor:hand;");
        return host;
    }
    private static Label dspTile(String name, Label vLabel) {
        Label n = lbl(name, "rc-dsp-n"); Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox row = new HBox(n, sp, vLabel); row.setAlignment(Pos.CENTER_LEFT);
        Label host = new Label(); host.setGraphic(row); host.getStyleClass().add("rc-dsp"); host.setMaxWidth(Double.MAX_VALUE);
        host.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        GridPane.setHgrow(host, Priority.ALWAYS); host.setStyle("-fx-cursor:hand;");
        row.prefWidthProperty().bind(host.widthProperty().subtract(20));
        return host;
    }
    private static HBox level(RigClient rig, String label, String catName, Map<String,Slider> sliders, Map<String,Label> vals) {
        Label k = lbl(label, "rc-lvl-k"); k.setMinWidth(34);
        Slider s = new Slider();
        Label v = lbl("—", "rc-lvl-v"); v.setMinWidth(44); v.setAlignment(Pos.CENTER_RIGHT);
        s.onCommit = () -> rig.setLevel(catName, s.frac.get());
        sliders.put(label, s); vals.put(label, v);
        HBox row = new HBox(10, k, s.node, v); row.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(s.node, Priority.ALWAYS);
        return row;
    }
    /** Passband visualizer — carrier marker on the left with the passband block extending right,
     *  width driven by {@code wfrac} (filter bandwidth fraction). */
    private static Pane passband(Region pass, javafx.beans.property.DoubleProperty wfrac) {
        Pane pb = new Pane(); pb.getStyleClass().add("rc-pb"); pb.setMinHeight(30); pb.setPrefHeight(30); pb.setMaxHeight(30);
        Region carrier = new Region(); carrier.getStyleClass().add("rc-pb-carrier");
        pb.getChildren().addAll(pass, carrier);
        Runnable lay = () -> {
            double W = pb.getWidth(); if (W <= 0) return;
            double cx = W * 0.28;
            double passW = Math.max(8, Math.min(W * Math.max(0.08, Math.min(0.92, wfrac.get())), W - cx - W * 0.05));
            pass.setPrefSize(passW, 22); pass.setMinSize(passW, 22); pass.setMaxSize(passW, 22); pass.relocate(cx, 4);
            carrier.setPrefSize(1.5, 30); carrier.setMinSize(1.5, 30); carrier.setMaxSize(1.5, 30); carrier.relocate(cx, 0);
        };
        pb.widthProperty().addListener((o, a, w) -> lay.run());
        wfrac.addListener((o, a, w) -> lay.run());
        return pb;
    }

    /** Collapsible nested sub-drawer; the one-line summary stays visible in both states. */
    private static VBox subdw(String title, Label summary, boolean openDefault, Node body) {
        boolean open = SUB_STATE.getOrDefault(title, openDefault);
        VBox dw = new VBox(); dw.getStyleClass().add("rc-subdw"); if (open) dw.getStyleClass().add("open");
        Label t = lbl(title.toUpperCase(), "rc-subdw-t"); HBox.setHgrow(t, Priority.ALWAYS); t.setMaxWidth(Double.MAX_VALUE);
        Label cv = lbl("›", "rc-subdw-cv"); cv.setRotate(open ? 90 : 0);
        HBox head = new HBox(8, t, summary, cv); head.setAlignment(Pos.CENTER_LEFT); head.getStyleClass().add("rc-subdw-head");
        VBox bodyBox = new VBox(body); bodyBox.getStyleClass().add("rc-subdw-body");
        bodyBox.setManaged(open); bodyBox.setVisible(open);
        head.setOnMouseClicked(e -> {
            boolean now = !dw.getStyleClass().contains("open");
            dw.getStyleClass().remove("open"); if (now) dw.getStyleClass().add("open");
            bodyBox.setManaged(now); bodyBox.setVisible(now); cv.setRotate(now ? 90 : 0);
            SUB_STATE.put(title, now);
        });
        dw.getChildren().addAll(head, bodyBox);
        return dw;
    }
    private static VBox miniCell(String k, MiniBar bar) {
        VBox v = new VBox(4, lbl(k, "rc-mini-k"), bar.val, bar.node); v.setAlignment(Pos.TOP_LEFT);
        GridPane.setHgrow(v, Priority.ALWAYS); return v;
    }
    private static Label actBtn(String text, String kind, Runnable action) {
        Label b = lbl(text, "rc-act", kind); b.setAlignment(Pos.CENTER); b.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(b, Priority.ALWAYS); b.setStyle("-fx-cursor:hand;");
        if (action != null) b.setOnMouseClicked(e -> action.run());
        return b;
    }
    private static void highlight(Map<String,Label> btns, String on) {
        btns.forEach((k, v) -> { v.getStyleClass().remove("on"); if (k.equals(on)) v.getStyleClass().add("on"); });
    }
    private static void toggleClass(javafx.scene.Node n, boolean on) { if (n == null) return; n.getStyleClass().remove("on"); if (on) n.getStyleClass().add("on"); }
    private static void dspSet(Map<String,Label> hosts, Map<String,Label> vals, String key, boolean on, String val) {
        toggleClass(hosts.get(key), on); Label v = vals.get(key); if (v != null) v.setText(val);
    }
    private static void levelSet(Map<String,Slider> sliders, Map<String,Label> vals, String key, double v, boolean watts) {
        Slider s = sliders.get(key); Label vl = vals.get(key);
        boolean ok = !Double.isNaN(v) && v >= 0;
        if (s != null && ok && !s.held) s.frac.set(Math.max(0, Math.min(1, v)));
        if (vl != null) vl.setText(ok ? (watts ? Math.round(v * 100) + " W" : String.valueOf(Math.round(v * 100))) : "—");
    }
    private static String fmtBw(long hz) { return hz >= 1000 ? String.format("%.1fk", hz / 1000.0) : hz + ""; }
    private static String lvlInt(RigClient.State st, String level, String fallback) {
        double v = st.level(level); if (Double.isNaN(v)) return fallback;
        return String.valueOf((int) Math.round(v <= 1 ? v * 15 : v));
    }
    /** Hamlib's AGC values: 0 off, 1 super-fast, 2 fast, 3 slow, 4 user, 5 medium, 6 auto. */
    private static String agcShort(double agc) {
        if (Double.isNaN(agc) || agc <= 0) return "off";
        return switch ((int) Math.round(agc)) { case 1, 2 -> "FST"; case 3 -> "SLW"; case 4 -> "USR"; case 5 -> "MID"; case 6 -> "AUT"; default -> "ON"; };
    }
    /** The next usual width above the current one for the mode, wrapping round to the narrowest. */
    private static long nextBw(String mode, long cur) {
        String m = mode == null ? "" : mode.toUpperCase();
        long[] presets = m.startsWith("CW") || m.startsWith("RTTY") ? new long[]{250, 500, 1200, 2400} : new long[]{1800, 2400, 3000, 3600};
        for (long p : presets) if (p > cur + 20) return p;
        return presets[0];
    }
    private static double nz(double v) { return Double.isNaN(v) ? 0 : v; }
    /** "+1.0k" / "-120" / "0": a signed offset in Hz. */
    private static String fmtOffset(long hz) {
        if (hz == 0) return "0";
        String sign = hz > 0 ? "+" : "-"; long a = Math.abs(hz);
        return a >= 1000 ? sign + String.format(java.util.Locale.ROOT, "%.1f", a / 1000.0) + "k" : sign + a;
    }
    private static void setSub(Label host, String text) {
        if (host != null && host.getGraphic() instanceof VBox box && box.getChildren().size() > 1 && box.getChildren().get(1) instanceof Label sub) sub.setText(text);
    }
    /** Click the DSP tiles to change the radio; scroll NB / NR for their level. IC-7300 values: preamp 0-2, attenuator 0 or 20 dB, AGC fast / mid / slow. */
    private static void wireDsp(RigClient rig, String name, Label host) {
        host.setOnMouseClicked(e -> {
            RigClient.State st = rig.last();
            if (st == null || !st.connected()) return;
            switch (name) {
                case "AGC" -> { long c = Math.round(nz(st.level("AGC"))); rig.setLevel("AGC", c == 2 ? 5 : c == 5 ? 3 : 2); }          // fast -> mid -> slow
                case "PRE" -> rig.setLevel("PREAMP", (Math.round(nz(st.level("PREAMP"))) + 1) % 3);                                   // off -> 1 -> 2
                case "ATT" -> rig.setLevel("ATT", nz(st.level("ATT")) > 0 ? 0 : 20);
                case "NB"  -> rig.setFunc("NB", !st.func("NB"));
                case "NR"  -> rig.setFunc("NR", !st.func("NR"));
                case "NOTCH" -> rig.setFunc("ANF", !st.func("ANF"));
                default -> { }
            }
        });
        if ("NB".equals(name) || "NR".equals(name)) {
            host.setOnScroll(e -> {
                if (e.getDeltaY() == 0) return;
                double c = nz(rig.last().level(name)); double next = Math.max(0, Math.min(1, c + (e.getDeltaY() > 0 ? 1 : -1) / 15.0));
                rig.setLevel(name, next); if (next > 0 && !rig.last().func(name)) rig.setFunc(name, true); e.consume();
            });
            Tips.tip(host, name + ": click to switch on or off, scroll to change the level.");
        }
    }

    /** A thin track + accent fill + knob, fraction-driven; {@code held} is true while the user drags. */
    private static final class Slider {
        final StackPane node; final SimpleDoubleProperty frac = new SimpleDoubleProperty(0);
        volatile boolean held; Runnable onCommit = () -> {};
        Slider() {
            Region track = new Region(); track.getStyleClass().add("rc-lvl-track");
            Region fill = new Region(); fill.getStyleClass().add("rc-lvl-fill");
            Region knob = new Region(); knob.getStyleClass().add("rc-lvl-knob"); knob.setMinSize(13, 13); knob.setMaxSize(13, 13);
            node = new StackPane(track, fill, knob); node.setAlignment(Pos.CENTER_LEFT); node.setMinHeight(13); node.setMaxHeight(13); node.setMaxWidth(Double.MAX_VALUE);
            fill.maxWidthProperty().bind(node.widthProperty().multiply(frac)); fill.minWidthProperty().bind(fill.maxWidthProperty()); fill.prefWidthProperty().bind(fill.maxWidthProperty());
            fill.setMaxHeight(5); fill.setMinHeight(5);
            StackPane.setMargin(knob, new Insets(0)); knob.translateXProperty().bind(node.widthProperty().multiply(frac).subtract(6.5));
            node.setOnMousePressed(e -> { held = true; setFromX(e.getX()); });
            node.setOnMouseDragged(e -> setFromX(e.getX()));
            node.setOnMouseReleased(e -> { setFromX(e.getX()); held = false; onCommit.run(); });
        }
        private void setFromX(double x) { frac.set(Math.max(0, Math.min(1, x / Math.max(1, node.getWidth())))); }
    }

    /** Fraction-driven mini-bar (S-strip SWR/PWR/ALC). */
    private static final class MiniBar {
        final StackPane node; final SimpleDoubleProperty frac = new SimpleDoubleProperty(0); final Label val;
        MiniBar(String fillHue) {
            Region track = new Region(); track.getStyleClass().add("rc-minibar-track");
            Region fill = new Region(); fill.getStyleClass().addAll("rc-minibar-fill", fillHue);
            node = new StackPane(track, fill); node.setAlignment(Pos.CENTER_LEFT); node.setMaxWidth(Double.MAX_VALUE); node.setMinHeight(5); node.setMaxHeight(5);
            fill.maxWidthProperty().bind(node.widthProperty().multiply(frac)); fill.minWidthProperty().bind(fill.maxWidthProperty()); fill.prefWidthProperty().bind(fill.maxWidthProperty());
            val = lbl("—", "rc-mini-v");
        }
        void set(double f, String text) { frac.set(Math.max(0, Math.min(1, f))); val.setText(text); }
    }

    // ── helpers ───────────────────────────────────────────────────────────
    static long parseFreqInput(String raw) {
        if (raw == null) return -1;
        String s = raw.trim().replace(" ", "");
        if (s.isEmpty() || s.contains("—")) return -1;
        int dots = (int) s.chars().filter(c -> c == '.').count();
        try {
            if (dots >= 2) {
                String[] p = s.split("\\.");
                long mhz = Long.parseLong(p[0]);
                long khz = p.length > 1 && !p[1].isEmpty() ? Long.parseLong(p[1]) : 0;
                long hz = p.length > 2 && !p[2].isEmpty() ? Long.parseLong((p[2] + "000").substring(0, 3)) : 0;
                return mhz * 1_000_000 + khz * 1_000 + hz;
            }
            double d = Double.parseDouble(s);
            if (dots == 1) return Math.round(d * 1_000_000);
            if (d < 1_000) return Math.round(d * 1_000_000);
            if (d < 100_000) return Math.round(d * 1_000);
            return Math.round(d);
        } catch (NumberFormatException e) { return -1; }
    }
    private static String catMode(String ui) { return switch (ui) { case "CW-R" -> "CWR"; case "FT8" -> "PKTUSB"; default -> ui; }; }
    private static String uiMode(String m) {
        if (m == null) return "";
        String u = m.trim().toUpperCase();
        return switch (u) { case "CWR" -> "CW-R"; case "PKTUSB", "PKTLSB", "PKT", "FT4", "DIGI" -> "FT8"; case "FSK", "FSKR" -> "RTTY"; default -> u; };
    }
    private static String bandOf(long hz) { return com.ars.fx.data.Bands.number(hz); }   // shared band plan; null out of band
}
