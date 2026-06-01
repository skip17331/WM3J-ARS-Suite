package com.jlog.controller;

import com.jlog.cluster.HubEngine;
import com.jlog.db.DatabaseManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Shared Rig Control front-panel component (RigControl.fxml), fx:included by
 * both NormalLog.fxml and ContestLog.fxml so the two modes share a single
 * source of truth for the VFO display, S-meter, mode/band keypad, tuning
 * chevrons, RIT/XIT, power slider, ANT and split/VFO controls.
 *
 * <p>All method bodies are carried verbatim from the original NormalLog rig
 * pane (commit d72e77e, version 1.1.1). The only changes versus the inline
 * original are the two host-specific seams behind {@link RigHost}: the
 * auto-track entry-form write and the visibility-driven drag-handle toggle
 * (host-owned layout), plus the per-mode keypad-expanded config key. The host
 * calls {@link #attach(RigHost)} from its own initialize so this controller's
 * FXML fields are already injected (fx:include loads the nested controller
 * during the host FXML load).</p>
 */
public class RigControlController implements Initializable {

    /** Host-specific seam. Implemented by NormalLogController / ContestLogController. */
    public interface RigHost {
        /** Auto-track: push the rig's freq (Hz) / mode into the host entry form. */
        void applyRigToEntry(long freqHz, String mode);
        /** Visibility changed — host toggles its own layout (e.g. the drag handle). */
        default void onRigVisibilityChanged(boolean visible) {}
        /** Per-mode DatabaseManager config key for the keypad-pane expanded state. */
        default String keypadExpandedKey() { return "rig.keypadExpanded"; }
    }

    private RigHost host;

    // ---- FXML: rig control pane ----
    @FXML private TitledPane rigControlPane;
    @FXML private TitledPane rigKeypadPane;
    @FXML private CheckBox   cbRigAutoTrack;
    @FXML private Label      lblRigDisplayBand;
    @FXML private Label      lblRigDisplayFreq;
    @FXML private Label      lblRigDisplayMode;
    @FXML private Button     btnRigAnt1, btnRigAnt2;
    @FXML private Button     btnRigModeSsb, btnRigModeCwRtty, btnRigModeAmFm;
    @FXML private Button     btnRigFInp, btnRigTs, btnRigSplit;
    @FXML private Button     btnTuneDownFast, btnTuneDownSlow,
                             btnTuneUpSlow, btnTuneUpFast;
    @FXML private Button     btnKey1, btnKey2, btnKey3, btnKey4, btnKey5,
                             btnKey6, btnKey7, btnKey8, btnKey9, btnKey0,
                             btnKeyGen, btnKeyEnt;
    @FXML private Button     btnRigVfoSwap;
    @FXML private Button     btnBand2200, btnBand630, btnBand60, btnBand222,
                             btnBand70cm, btnBand33cm, btnBand23cm;
    @FXML private Button     btnRigRit, btnRigXit, btnRitDown, btnRitUp,
                             btnXitDown, btnXitUp, btnRitXitClear;
    @FXML private Label      lblRitOffset, lblXitOffset, lblRfPower;
    @FXML private Slider     sliderRfPower;
    @FXML private Region     rigLiveDot;
    @FXML private Label      lblRigLive;
    @FXML private HBox       rigTitleBar;
    @FXML private HBox       sMeterSegments;

    // ---- rig pane state (verbatim from the original) ----
    private volatile boolean splitOn = false;
    private java.util.Map<Button, Long> bandOnlyKeys;
    private volatile com.fasterxml.jackson.databind.JsonNode lastRigCaps;
    private volatile boolean ritOn = false, xitOn = false;
    private volatile int     ritOffset = 0, xitOffset = 0;
    private static final int RIT_STEP_HZ = 10;
    private boolean updatingPowerFromRig = false;
    private final java.util.List<Region> sMeterSegs = new java.util.ArrayList<>();
    private volatile boolean keypadMode = false;
    private final StringBuilder keypadBuffer = new StringBuilder();
    private static final long[] TS_STEPS_HZ = {10, 50, 100, 500, 1_000, 5_000, 10_000, 25_000};
    private volatile int tsStepIdx = 2; // start at 100 Hz
    private static final java.util.Map<String, Long> KEYPAD_BAND_HZ = java.util.Map.ofEntries(
        java.util.Map.entry("1", 1_810_000L),   // 1.8 = 160m
        java.util.Map.entry("2", 3_750_000L),   // 3.5 = 80m
        java.util.Map.entry("3", 7_200_000L),   // 7   = 40m
        java.util.Map.entry("4", 10_130_000L),  // 10  = 30m
        java.util.Map.entry("5", 14_250_000L),  // 14  = 20m
        java.util.Map.entry("6", 18_140_000L),  // 18  = 17m
        java.util.Map.entry("7", 21_300_000L),  // 21  = 15m
        java.util.Map.entry("8", 24_940_000L),  // 24  = 12m
        java.util.Map.entry("9", 28_400_000L),  // 28  = 10m
        java.util.Map.entry("0", 50_150_000L),  // 50  = 6m
        java.util.Map.entry("ENT", 146_500_000L)// 144 = 2m
    );
    private volatile com.fasterxml.jackson.databind.JsonNode lastRigStatus;
    private volatile long lastRigStatusMs = 0;
    private static final long RIG_LIVE_THRESHOLD_MS = 3000;

    // -----------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Host-independent wiring only. Host-dependent setup waits for attach().
        initRigLiveIndicator();
        initRigKeypad();
        initRigControls();
        initRigTitleBar();
        initSMeter();

        HubEngine hub = HubEngine.getInstance();
        hub.setRigStatusListener(node -> Platform.runLater(() -> updateRigStatus(node)));
        hub.setRigCapsListener(node -> Platform.runLater(() -> applyRigCaps(node)));
        hub.setRigControlVisibleListener(visible -> Platform.runLater(() -> setRigControlVisible(visible)));
    }

    /** Wire the host seam + run host-dependent setup. Call from host initialize(). */
    public void attach(RigHost host) {
        this.host = host;
        initKeypadPaneState();
    }

    /** Exposed so the host can place / size / persist the pane width. */
    public TitledPane getPane() { return rigControlPane; }

    // -----------------------------------------------------------------
    // Status + caps (verbatim)
    // -----------------------------------------------------------------

    private void updateRigStatus(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || lblRigDisplayFreq == null) return;
        lastRigStatus = node;
        lastRigStatusMs = System.currentTimeMillis();

        if (!keypadMode) repaintBigDisplay(node);

        setSMeterLevel(sMeterDbToSegments(node.path("sMeterDb").asInt(Integer.MIN_VALUE)));

        splitOn = node.path("split").asBoolean(false);
        if (btnRigSplit != null) {
            btnRigSplit.getStyleClass().remove("rig-keypad-active");
            if (splitOn) btnRigSplit.getStyleClass().add("rig-keypad-active");
        }

        ritOffset = node.path("rit").asInt(0);
        xitOffset = node.path("xit").asInt(0);
        ritOn     = node.path("ritOn").asBoolean(false);
        xitOn     = node.path("xitOn").asBoolean(false);
        if (lblRitOffset != null) lblRitOffset.setText(formatOffset(ritOffset));
        if (lblXitOffset != null) lblXitOffset.setText(formatOffset(xitOffset));
        styleToggle(btnRigRit, ritOn);
        styleToggle(btnRigXit, xitOn);

        double pwr = node.path("rfPower").asDouble(-1.0);
        if (pwr >= 0) {
            if (lblRfPower != null) lblRfPower.setText(Math.round(pwr * 100) + "%");
            if (sliderRfPower != null && !sliderRfPower.isValueChanging()) {
                updatingPowerFromRig = true;
                sliderRfPower.setValue(pwr * 100);
                updatingPowerFromRig = false;
            }
        } else if (lblRfPower != null) {
            lblRfPower.setText("—");
        }

        if (cbRigAutoTrack != null && cbRigAutoTrack.isSelected() && host != null) {
            long   freqHz = node.path("frequency").asLong(0);
            String mode   = node.path("mode").asText("");
            host.applyRigToEntry(freqHz, mode);
        }
    }

    private static String formatOffset(int hz) {
        if (hz == 0) return "0";
        return (hz > 0 ? "+" : "−") + Math.abs(hz);
    }

    private static void styleToggle(Button btn, boolean on) {
        if (btn == null) return;
        btn.getStyleClass().remove("rig-keypad-active");
        if (on) btn.getStyleClass().add("rig-keypad-active");
    }

    private void applyRigCaps(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null) return;
        lastRigCaps = node;
        boolean known = node.path("known").asBoolean(false);

        setDisabled(btnRigSplit,   !capFlag(node, "splitVfo"));
        setDisabled(btnRigVfoSwap, !capFlag(node, "setVfo"));

        boolean rit = !known || node.path("ritOffset").asBoolean(false) || node.path("ritFunc").asBoolean(false);
        boolean xit = !known || node.path("xitOffset").asBoolean(false) || node.path("xitFunc").asBoolean(false);
        for (Button b : new Button[]{btnRigRit, btnRitUp, btnRitDown}) setDisabled(b, !rit);
        for (Button b : new Button[]{btnRigXit, btnXitUp, btnXitDown}) setDisabled(b, !xit);
        setDisabled(btnRitXitClear, !rit && !xit);

        if (sliderRfPower != null) sliderRfPower.setDisable(known && !node.path("setRfPower").asBoolean(false));

        java.util.Set<String> modes = new java.util.HashSet<>();
        if (node.path("modes").isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode m : node.path("modes")) modes.add(m.asText().toUpperCase());
        }
        boolean gateModes = known && !modes.isEmpty();
        setDisabled(btnRigModeSsb,    gateModes && !(modes.contains("USB") || modes.contains("LSB")));
        setDisabled(btnRigModeCwRtty, gateModes && !(modes.contains("CW")  || modes.contains("RTTY")));
        setDisabled(btnRigModeAmFm,   gateModes && !(modes.contains("AM")  || modes.contains("FM")));

        applyKeypadGating(node);
    }

    private static boolean capFlag(com.fasterxml.jackson.databind.JsonNode caps, String field) {
        if (!caps.path("known").asBoolean(false)) return true;
        return caps.path(field).asBoolean(false);
    }

    private void applyKeypadGating(com.fasterxml.jackson.databind.JsonNode caps) {
        com.fasterxml.jackson.databind.JsonNode ranges = caps.path("txRanges");
        boolean gate = caps.path("known").asBoolean(false) && ranges.isArray() && ranges.size() > 0;
        java.util.Map<String, Button> keys = java.util.Map.ofEntries(
            java.util.Map.entry("1", btnKey1), java.util.Map.entry("2", btnKey2),
            java.util.Map.entry("3", btnKey3), java.util.Map.entry("4", btnKey4),
            java.util.Map.entry("5", btnKey5), java.util.Map.entry("6", btnKey6),
            java.util.Map.entry("7", btnKey7), java.util.Map.entry("8", btnKey8),
            java.util.Map.entry("9", btnKey9), java.util.Map.entry("0", btnKey0),
            java.util.Map.entry("ENT", btnKeyEnt));
        for (java.util.Map.Entry<String, Button> e : keys.entrySet()) {
            Long hz = KEYPAD_BAND_HZ.get(e.getKey());
            boolean ok = !gate || hz == null || txCanTransmit(ranges, hz);
            setDisabled(e.getValue(), !ok);
        }
        if (bandOnlyKeys != null) {
            for (java.util.Map.Entry<Button, Long> e : bandOnlyKeys.entrySet()) {
                boolean ok = !gate || e.getValue() == null || txCanTransmit(ranges, e.getValue());
                setDisabled(e.getKey(), !ok);
            }
        }
    }

    private static boolean txCanTransmit(com.fasterxml.jackson.databind.JsonNode ranges, long hz) {
        for (com.fasterxml.jackson.databind.JsonNode r : ranges) {
            if (r.isArray() && r.size() >= 2 && hz >= r.get(0).asLong() && hz <= r.get(1).asLong()) return true;
        }
        return false;
    }

    private static void setDisabled(javafx.scene.control.Control c, boolean disabled) {
        if (c != null) c.setDisable(disabled);
    }

    private void repaintBigDisplay(com.fasterxml.jackson.databind.JsonNode node) {
        if (lblRigDisplayFreq == null) return;
        long freqHz = node.path("frequency").asLong(0);
        String mode = node.path("mode").asText("");
        String band = node.path("band").asText("");
        lblRigDisplayBand.setText("Band: " + (band.isEmpty() ? "—" : band));
        lblRigDisplayFreq.setText(freqHz > 0
            ? String.format("%.3f MHz", freqHz / 1_000_000.0) : "—");
        lblRigDisplayMode.setText("Mode: " + (mode.isEmpty() ? "—" : mode));
    }

    // -----------------------------------------------------------------
    // S-meter (verbatim)
    // -----------------------------------------------------------------

    private static final int S_METER_SEGMENTS = 13;
    private static final int S_METER_GREEN    = 9;   // S1 through S9

    private void initSMeter() {
        if (sMeterSegments == null) return;
        sMeterSegs.clear();
        for (int i = 0; i < S_METER_SEGMENTS; i++) {
            Region seg = new Region();
            seg.getStyleClass().add("rig-meter-seg");
            sMeterSegs.add(seg);
            sMeterSegments.getChildren().add(seg);
        }
        setSMeterLevel(0);   // dark until the first reading
    }

    private void setSMeterLevel(int litTo) {
        int clamped = Math.max(0, Math.min(S_METER_SEGMENTS, litTo));
        for (int i = 0; i < sMeterSegs.size(); i++) {
            Region seg = sMeterSegs.get(i);
            seg.getStyleClass().removeAll("rig-meter-seg-on", "rig-meter-seg-mid",
                    "rig-meter-seg-hi", "rig-meter-seg-off", "rig-meter-seg-off-hi");
            boolean isHi  = i >= S_METER_GREEN;
            boolean isMid = !isHi && i >= S_METER_GREEN - 2;  // S8-9 tint to yellow
            if (i < clamped) {
                seg.getStyleClass().add(isHi ? "rig-meter-seg-hi"
                                       : isMid ? "rig-meter-seg-mid"
                                              : "rig-meter-seg-on");
            } else {
                seg.getStyleClass().add(isHi ? "rig-meter-seg-off-hi" : "rig-meter-seg-off");
            }
        }
    }

    private static int sMeterDbToSegments(int db) {
        if (db == Integer.MIN_VALUE) return 0;
        double seg = (db <= 0) ? 9.0 + db / 6.0 : 9.0 + db / 15.0;
        return (int) Math.round(seg);
    }

    // -----------------------------------------------------------------
    // Title bar / liveness / visibility (verbatim, drag-handle delegated)
    // -----------------------------------------------------------------

    private void initRigTitleBar() {
        if (rigTitleBar == null || rigControlPane == null) return;
        rigTitleBar.prefWidthProperty().bind(
            rigControlPane.prefWidthProperty().subtract(36));
    }

    private void initRigLiveIndicator() {
        if (rigLiveDot == null || lblRigLive == null) return;
        refreshRigLiveIndicator();
        Timeline t = new Timeline(new KeyFrame(Duration.seconds(1),
            e -> refreshRigLiveIndicator()));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }

    private void refreshRigLiveIndicator() {
        if (rigLiveDot == null || lblRigLive == null) return;
        boolean live = lastRigStatusMs > 0
            && (System.currentTimeMillis() - lastRigStatusMs) < RIG_LIVE_THRESHOLD_MS;
        rigLiveDot.getStyleClass().removeAll("rig-dot-live", "rig-dot-stale");
        rigLiveDot.getStyleClass().add(live ? "rig-dot-live" : "rig-dot-stale");
        if (lastRigStatusMs == 0) {
            lblRigLive.setText("No rig");
        } else if (live) {
            lblRigLive.setText("Live");
        } else {
            long ageS = (System.currentTimeMillis() - lastRigStatusMs) / 1000;
            lblRigLive.setText("Offline (" + ageS + "s ago)");
        }
    }

    private void setRigControlVisible(boolean visible) {
        if (rigControlPane == null) return;
        rigControlPane.setVisible(visible);
        rigControlPane.setManaged(visible);
        if (host != null) host.onRigVisibilityChanged(visible);
    }

    // -----------------------------------------------------------------
    // Radio-panel button handlers (verbatim)
    // -----------------------------------------------------------------

    @FXML private void rigAnt1() { HubEngine.getInstance().sendAntOverride("1", 1); }
    @FXML private void rigAnt2() { HubEngine.getInstance().sendAntOverride("1", 2); }

    @FXML private void rigModeSsb() {
        long freq = lastRigStatus != null ? lastRigStatus.path("frequency").asLong(0) : 0;
        HubEngine.getInstance().sendSetMode(freq >= 10_000_000L ? "USB" : "LSB");
    }
    @FXML private void rigModeCwRtty() {
        String cur = lastRigStatus != null ? lastRigStatus.path("mode").asText("") : "";
        HubEngine.getInstance().sendSetMode("RTTY".equalsIgnoreCase(cur) ? "CW" : "RTTY");
    }
    @FXML private void rigModeAmFm() {
        String cur = lastRigStatus != null ? lastRigStatus.path("mode").asText("") : "";
        HubEngine.getInstance().sendSetMode("AM".equalsIgnoreCase(cur) ? "FM" : "AM");
    }

    @FXML private void rigVfoSwap() { HubEngine.getInstance().sendSwapVfo(); }

    /** SET → open j-hub's Rig Control settings tab in the system browser. */
    @FXML private void rigSet() {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081#rig").start(); }
        catch (Exception ignored) {}
    }

    @FXML private void rigSplitToggle() {
        splitOn = !splitOn;
        HubEngine.getInstance().sendSetSplit(splitOn);
        styleToggle(btnRigSplit, splitOn);
    }

    @FXML private void rigRitToggle() {
        ritOn = !ritOn;
        HubEngine.getInstance().sendSetRitEnabled(ritOn);
        styleToggle(btnRigRit, ritOn);
    }
    @FXML private void rigXitToggle() {
        xitOn = !xitOn;
        HubEngine.getInstance().sendSetXitEnabled(xitOn);
        styleToggle(btnRigXit, xitOn);
    }

    @FXML private void ritDown() { nudgeRit(-RIT_STEP_HZ); }
    @FXML private void ritUp()   { nudgeRit(+RIT_STEP_HZ); }
    @FXML private void xitDown() { nudgeXit(-RIT_STEP_HZ); }
    @FXML private void xitUp()   { nudgeXit(+RIT_STEP_HZ); }

    private void nudgeRit(int deltaHz) {
        ritOffset += deltaHz;
        HubEngine.getInstance().sendSetRit(ritOffset);
        if (lblRitOffset != null) lblRitOffset.setText(formatOffset(ritOffset));
    }
    private void nudgeXit(int deltaHz) {
        xitOffset += deltaHz;
        HubEngine.getInstance().sendSetXit(xitOffset);
        if (lblXitOffset != null) lblXitOffset.setText(formatOffset(xitOffset));
    }
    @FXML private void ritXitClear() {
        ritOffset = 0; xitOffset = 0;
        HubEngine.getInstance().sendSetRit(0);
        HubEngine.getInstance().sendSetXit(0);
        if (lblRitOffset != null) lblRitOffset.setText("0");
        if (lblXitOffset != null) lblXitOffset.setText("0");
    }

    private void initRigControls() {
        if (sliderRfPower == null) return;
        sliderRfPower.valueProperty().addListener((obs, ov, nv) -> {
            if (updatingPowerFromRig) return;
            if (lblRfPower != null) lblRfPower.setText(Math.round(nv.doubleValue()) + "%");
            if (!sliderRfPower.isValueChanging()) {
                HubEngine.getInstance().sendSetRfPower(nv.doubleValue() / 100.0);
            }
        });
        sliderRfPower.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing && !updatingPowerFromRig) {
                HubEngine.getInstance().sendSetRfPower(sliderRfPower.getValue() / 100.0);
            }
        });
    }

    private void initKeypadPaneState() {
        if (rigKeypadPane == null) return;
        String key = host != null ? host.keypadExpandedKey() : "rig.keypadExpanded";
        boolean expanded = Boolean.parseBoolean(
            DatabaseManager.getInstance().getConfig(key, "true"));
        rigKeypadPane.setExpanded(expanded);
        rigKeypadPane.expandedProperty().addListener((o, ov, nv) ->
            DatabaseManager.getInstance().setConfig(key, String.valueOf(nv)));
    }

    private void initRigKeypad() {
        wireKeypadBtn(btnKey1,   "1",   KEYPAD_BAND_HZ.get("1"));
        wireKeypadBtn(btnKey2,   "2",   KEYPAD_BAND_HZ.get("2"));
        wireKeypadBtn(btnKey3,   "3",   KEYPAD_BAND_HZ.get("3"));
        wireKeypadBtn(btnKey4,   "4",   KEYPAD_BAND_HZ.get("4"));
        wireKeypadBtn(btnKey5,   "5",   KEYPAD_BAND_HZ.get("5"));
        wireKeypadBtn(btnKey6,   "6",   KEYPAD_BAND_HZ.get("6"));
        wireKeypadBtn(btnKey7,   "7",   KEYPAD_BAND_HZ.get("7"));
        wireKeypadBtn(btnKey8,   "8",   KEYPAD_BAND_HZ.get("8"));
        wireKeypadBtn(btnKey9,   "9",   KEYPAD_BAND_HZ.get("9"));
        wireKeypadBtn(btnKey0,   "0",   KEYPAD_BAND_HZ.get("0"));
        wireKeypadBtn(btnKeyGen, ".",   null);    // GENE band-jump TBD; "." still works in keypad mode
        wireKeypadBtn(btnKeyEnt, "ENT", KEYPAD_BAND_HZ.get("ENT"));

        bandOnlyKeys = new java.util.LinkedHashMap<>();
        bandOnlyKeys.put(btnBand2200,         137_500L);   // 2200 m
        bandOnlyKeys.put(btnBand630,          475_000L);   // 630 m
        bandOnlyKeys.put(btnBand60,         5_358_500L);   // 60 m
        bandOnlyKeys.put(btnBand222,      222_100_000L);   // 1.25 m
        bandOnlyKeys.put(btnBand70cm,     432_100_000L);   // 70 cm
        bandOnlyKeys.put(btnBand33cm,     902_100_000L);   // 33 cm
        bandOnlyKeys.put(btnBand23cm,   1_296_100_000L);   // 23 cm
        bandOnlyKeys.forEach(this::wireBandKey);

        refreshTsTooltip();
    }

    private void wireBandKey(Button btn, Long bandHz) {
        if (btn == null) return;
        btn.setOnAction(e -> {
            if (!keypadMode && bandHz != null && bandHz > 0) {
                HubEngine.getInstance().sendSetFreq(bandHz);
            }
        });
    }

    private void wireKeypadBtn(Button btn, String secondary, Long primaryBandHz) {
        if (btn == null) return;
        btn.setOnAction(e -> {
            if (keypadMode) {
                if ("ENT".equals(secondary)) commitKeypadBuffer();
                else { keypadBuffer.append(secondary); paintBigDisplayFromBuffer(); }
            } else {
                if (primaryBandHz != null && primaryBandHz > 0) {
                    HubEngine.getInstance().sendSetFreq(primaryBandHz);
                }
            }
        });
    }

    @FXML private void rigFInpToggle() {
        keypadMode = !keypadMode;
        keypadBuffer.setLength(0);
        if (btnRigFInp != null) {
            if (keypadMode) btnRigFInp.getStyleClass().add("rig-keypad-active");
            else            btnRigFInp.getStyleClass().remove("rig-keypad-active");
        }
        if (keypadMode) paintBigDisplayFromBuffer();
        else if (lastRigStatus != null) repaintBigDisplay(lastRigStatus);
    }

    private void commitKeypadBuffer() {
        String s = keypadBuffer.toString();
        if (!s.isEmpty()) {
            try {
                long freqHz = Math.round(Double.parseDouble(s) * 1_000_000.0);
                if (freqHz > 0) HubEngine.getInstance().sendSetFreq(freqHz);
            } catch (NumberFormatException ignored) {}
        }
        keypadBuffer.setLength(0);
        keypadMode = false;
        if (btnRigFInp != null) btnRigFInp.getStyleClass().remove("rig-keypad-active");
        if (lastRigStatus != null) repaintBigDisplay(lastRigStatus);
    }

    private void paintBigDisplayFromBuffer() {
        if (lblRigDisplayFreq == null) return;
        lblRigDisplayFreq.setText(keypadBuffer.length() > 0 ? keypadBuffer + "_" : "_");
    }

    @FXML private void rigTsCycle() {
        tsStepIdx = (tsStepIdx + 1) % TS_STEPS_HZ.length;
        refreshTsTooltip();
    }

    private void refreshTsTooltip() {
        if (btnRigTs == null) return;
        long s = TS_STEPS_HZ[tsStepIdx];
        String slow = formatStepHz(s);
        String fast = formatStepHz(s * 10);
        btnRigTs.setTooltip(new Tooltip("Tuning step: " + slow));
        if (btnTuneDownSlow != null) btnTuneDownSlow.setTooltip(new Tooltip("−" + slow));
        if (btnTuneUpSlow   != null) btnTuneUpSlow  .setTooltip(new Tooltip("+" + slow));
        if (btnTuneDownFast != null) btnTuneDownFast.setTooltip(new Tooltip("−" + fast));
        if (btnTuneUpFast   != null) btnTuneUpFast  .setTooltip(new Tooltip("+" + fast));
    }

    private static String formatStepHz(long hz) {
        return (hz % 1000 == 0) ? (hz / 1000) + " kHz" : hz + " Hz";
    }

    private void tuneStep(long deltaHz) {
        if (lastRigStatus == null) return;
        long cur = lastRigStatus.path("frequency").asLong(0);
        if (cur <= 0) return;
        HubEngine.getInstance().sendSetFreq(cur + deltaHz);
    }
    @FXML private void tuneDownFast() { tuneStep(-TS_STEPS_HZ[tsStepIdx] * 10); }
    @FXML private void tuneDownSlow() { tuneStep(-TS_STEPS_HZ[tsStepIdx]);      }
    @FXML private void tuneUpSlow()   { tuneStep(+TS_STEPS_HZ[tsStepIdx]);      }
    @FXML private void tuneUpFast()   { tuneStep(+TS_STEPS_HZ[tsStepIdx] * 10); }
}
