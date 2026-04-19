package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.ModemStatus;
import com.hamradio.modem.model.RotorStatus;
import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.tx.AudioTxEngine;
import com.hamradio.modem.tx.TxState;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * J-Digi main window — DM-780 layout, j-log shared theme (light / dark).
 *
 * Structural nodes carry CSS class names so that swapping the stylesheet
 * (via scene.getStylesheets().setAll()) is sufficient to retheme the app.
 * Only runtime-varying elements (SNR text color, TX state color, indicator
 * dot fills) are updated via applyThemeColors() after a theme switch.
 *
 * CSS class map:
 *   .jd-toolbar       toolbar HBox
 *   .jd-bezel         instrument bezel VBox
 *   .jd-freq-panel    frequency display dark-glass VBox
 *   .jd-ruler         audio-Hz ruler Pane
 *   .jd-rx-header     RX pane header HBox
 *   .jd-tx-header     TX pane header HBox
 *   .jd-statusbar     status bar HBox
 *   .jd-callsign      callsign label
 *   .jd-grid-label    grid-square label
 *   .jd-freq-display  frequency readout label
 *   .jd-audio-sub     audio-offset sub-label
 *   .jd-mode-tag      rig mode pill
 *   .jd-rotor         rotor bearing readout label
 *   .jd-inst-label    tiny bezel section labels (SIGNAL, BEARING, …)
 *   .jd-rx-sub        RX pane SNR/Peak sub-labels
 *   .jd-panel-title   RECEIVE / TRANSMIT nameplate labels
 *   .macro-button     macro bar buttons  (shared with j-log name)
 *   .primary-button   primary action buttons (shared with j-log name)
 */
public class MainWindow {

    // ── Runtime theme state ───────────────────────────────────────────
    private boolean darkTheme;
    private Scene   scene;
    private static final Preferences PREFS =
            Preferences.userNodeForPackage(MainWindow.class);

    // ── Connection indicator dots ────────────────────────────────────
    private final Circle hubDot = new Circle(5);
    private final Circle rxDot  = new Circle(5);
    private final Circle txDot  = new Circle(5);

    // ── Instrument displays ──────────────────────────────────────────
    private final Label callsignLabel = new Label("NOCALL");
    private final Label gridLabel     = new Label("");
    private final Label freqLabel     = new Label("——  ———  ———");
    private final Label audioOffLabel = new Label("——— Hz");
    private final Label modeTag       = new Label("---");
    private final Label rotorLabel    = new Label("000°");
    private final Canvas snrCanvas    = new Canvas(110, 10);

    // ── Toolbar controls ─────────────────────────────────────────────
    private final ComboBox<ModeType> modeBox      = new ComboBox<>();
    private final ToggleButton       afcBtn       = new ToggleButton("AFC");
    private final ToggleButton       sqlBtn       = new ToggleButton("SQL");
    private final Button             transmitBtn  = new Button("▶  Transmit");
    private final Button             cancelBtn    = new Button("■  Cancel");
    private final Button             saveTxWavBtn = new Button("WAV");
    private final Button             themeBtn     = new Button();   // ☀ / ☾

    // ── RX pane controls ─────────────────────────────────────────────
    private final Label    rx_snr        = new Label("SNR: —");
    private final Label    rx_peak       = new Label("Peak: —");
    private final Label    rx_afc        = new Label("AFC");
    private final CheckBox autoScrollBox = new CheckBox("Auto");
    private final Button   clearRxBtn   = new Button("Clear");
    private final Button   sendToLogBtn = new Button("→ Log");

    // ── TX pane controls ─────────────────────────────────────────────
    private final Label txStateLabel = new Label("IDLE");
    private final Label txCharCount  = new Label("0 ch");

    // ── Main text areas ──────────────────────────────────────────────
    private final TextArea rxArea = new TextArea();
    private final TextArea txArea = new TextArea();

    // ── Status-bar labels ────────────────────────────────────────────
    private final Label sb_snr   = new Label("SNR —");
    private final Label sb_peak  = new Label("Peak —");
    private final Label sb_mode  = new Label("—");
    private final Label sb_audio = new Label("Audio ○");
    private final Label sb_hub   = new Label("Hub ○");

    // ── Panel state ──────────────────────────────────────────────────
    private boolean rightPanelVisible = true;
    private static final double PANEL_OPEN  = 0.74;
    private static final double PANEL_CLOSE = 1.00;

    private SplitPane  mainSplit;
    private RightPanel rightPanel;

    // ── Signal display panes ─────────────────────────────────────────
    private final WaterfallPane waterfallPane = new WaterfallPane();
    private final SpectrumPane  spectrumPane  = new SpectrumPane();

    // ── Decode state ─────────────────────────────────────────────────
    private String      lastDecodeLine = "";
    private ModemStatus lastStatus;
    private static final Pattern CALLSIGN_RE =
            Pattern.compile("\\b([A-Z0-9]{1,3}[0-9][A-Z0-9/]{1,6})\\b");

    private final ModemService service;

    public MainWindow(ModemService service) { this.service = service; }

    // ================================================================
    // Entry point
    // ================================================================

    public void show(Stage stage) {
        darkTheme = PREFS.getBoolean("darkTheme", false);   // default: light
        rightPanel = new RightPanel(service);

        String prefsCall = service.getMyCall();
        callsignLabel.setText(prefsCall);
        rightPanel.getLogEntryPane().setStationCallsign(prefsCall);

        BorderPane root = new BorderPane();
        root.setTop(buildTop(stage));
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        configureTextAreas();
        wireModeBox();
        wireService();
        wireToggleButtons();

        scene = new Scene(root, 1440, 960);
        scene.getStylesheets().add(buildCombinedStylesheet());
        // Class-based theming: root carries .dark or .light
        scene.getRoot().getStyleClass().add(darkTheme ? "dark" : "light");

        stage.setTitle("J-Digi");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(720);
        stage.show();

        applyThemeColors();
    }

    // ================================================================
    // Theme switching
    // ================================================================

    private void toggleTheme() {
        darkTheme = !darkTheme;
        PREFS.putBoolean("darkTheme", darkTheme);
        // Swap CSS class on root — the combined stylesheet handles both
        var cls = scene.getRoot().getStyleClass();
        if (darkTheme) { cls.remove("light"); cls.add("dark"); }
        else           { cls.remove("dark");  cls.add("light"); }
        applyThemeColors();
        themeBtn.setText(darkTheme ? "☀" : "☾");
        drawSnrBar(lastStatus != null ? lastStatus.getSnr() : 0);
        redrawFreqRuler();
    }

    /** Finds the ruler Canvas and redraws it with current theme colors. */
    private void redrawFreqRuler() {
        javafx.scene.Node n = scene.lookup(".jd-ruler");
        if (n instanceof Pane p && !p.getChildren().isEmpty()
                && p.getChildren().get(0) instanceof Canvas c) {
            drawFreqRuler(c);
        }
    }

    /** Re-applies the inline styles that vary at runtime AND are theme-sensitive. */
    private void applyThemeColors() {
        themeBtn.setText(darkTheme ? "☀" : "☾");

        // Dots (re-driven by last status; seed defaults here)
        hubDot.setFill(Color.web(err()));
        rxDot.setFill(Color.web(sub()));
        txDot.setFill(Color.web(sub()));

        // AFC indicator in RX header
        rx_afc.setStyle("-fx-text-fill: " + sub() + ";");
        rx_peak.setStyle("-fx-text-fill: " + sub() + ";");
        rx_snr.setStyle("-fx-text-fill: " + teal() + ";");

        // TX state label
        txStateLabel.setStyle(
            "-fx-font-family: monospace; -fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: " + sub() + ";");

        // Status bar labels (set default; onStatusUpdate will override with live values)
        sb_audio.setStyle("-fx-text-fill: " + sub() + ";");
        sb_hub.setStyle("-fx-text-fill: " + sub() + ";");
        sb_snr.setStyle("-fx-text-fill: " + text() + ";");
        sb_peak.setStyle("-fx-text-fill: " + text() + ";");
        sb_mode.setStyle("-fx-text-fill: " + text() + ";");

        drawSnrBar(0);
    }

    // Theme-aware color helpers (always check darkTheme at call time)
    private String accent() { return darkTheme ? "#4fc3f7" : "#1565c0"; }
    private String text()   { return darkTheme ? "#e0e0e0" : "#212121"; }
    private String sub()    { return darkTheme ? "#888888" : "#666666"; }
    private String teal()   { return darkTheme ? "#80cbc4" : "#00695c"; }
    private String ok()     { return darkTheme ? "#80cbc4" : "#388e3c"; }
    private String warn()   { return darkTheme ? "#80deea" : "#0277bd"; }
    private String err()    { return darkTheme ? "#ef9a9a" : "#c62828"; }

    // ================================================================
    // Top
    // ================================================================

    private VBox buildTop(Stage stage) {
        return new VBox(0,
            buildMenuBar(stage),
            buildToolBar(),
            buildMacroRow()
        );
    }

    private MenuBar buildMenuBar(Stage stage) {
        Menu fileMenu = new Menu("File");
        MenuItem setupItem = new MenuItem("Setup…");
        setupItem.setOnAction(e -> showSetupDialog());
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());
        fileMenu.getItems().addAll(setupItem, new SeparatorMenuItem(), exitItem);

        Menu viewMenu = new Menu("View");
        MenuItem toggleLog   = new MenuItem("Toggle Log Panel");
        MenuItem toggleSpots = new MenuItem("Toggle Spots Panel");
        toggleLog.setOnAction(e   -> toggleRightPanel());
        toggleSpots.setOnAction(e -> { showRightPanel(); rightPanel.showSpotsTab(); });
        viewMenu.getItems().addAll(toggleLog, toggleSpots);

        return new MenuBar(fileMenu, viewMenu);
    }

    private HBox buildToolBar() {

        // ── Station ──────────────────────────────────────────────────
        callsignLabel.setFont(Font.font("monospace", FontWeight.BOLD, 18));
        callsignLabel.getStyleClass().add("jd-callsign");
        gridLabel.getStyleClass().add("jd-grid-label");
        VBox stationBox = new VBox(1, callsignLabel, gridLabel);
        stationBox.setAlignment(Pos.CENTER_LEFT);

        // ── Frequency panel ──────────────────────────────────────────
        freqLabel.setFont(Font.font("monospace", FontWeight.BOLD, 26));
        freqLabel.getStyleClass().add("jd-freq-display");
        audioOffLabel.getStyleClass().add("jd-audio-sub");
        VBox freqContent = new VBox(1, freqLabel, audioOffLabel);
        freqContent.setAlignment(Pos.CENTER_LEFT);
        freqContent.setPadding(new Insets(3, 8, 3, 8));
        freqContent.getStyleClass().add("jd-freq-panel");

        // ── Mode combo + tag ─────────────────────────────────────────
        modeBox.setItems(FXCollections.observableArrayList(ModeType.values()));
        modeBox.setValue(ModeType.RTTY);
        modeBox.setPrefWidth(108);
        modeTag.setFont(Font.font("monospace", FontWeight.BOLD, 10));
        modeTag.getStyleClass().add("jd-mode-tag");
        Label modeLbl = instLabel("MODE");
        VBox modeVBox = new VBox(2, modeLbl, new HBox(4, modeBox, modeTag));
        ((HBox) modeVBox.getChildren().get(1)).setAlignment(Pos.CENTER_LEFT);
        modeVBox.setAlignment(Pos.CENTER_LEFT);

        // ── AFC / SQL toggles ────────────────────────────────────────
        afcBtn.setPrefSize(50, 22);
        sqlBtn.setPrefSize(50, 22);
        afcBtn.getStyleClass().add("afc-btn");
        sqlBtn.getStyleClass().add("sql-btn");
        VBox guardContent = new VBox(3, afcBtn, sqlBtn);
        guardContent.setAlignment(Pos.CENTER);

        // ── Transmit / Cancel ────────────────────────────────────────
        transmitBtn.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, 12));
        transmitBtn.getStyleClass().add("xmit-btn");
        transmitBtn.setPrefSize(116, 36);
        cancelBtn.setFont(Font.font("DejaVu Sans", FontWeight.BOLD, 12));
        cancelBtn.getStyleClass().add("cancel-btn");
        cancelBtn.setPrefSize(100, 36);
        cancelBtn.setDisable(true);
        transmitBtn.setOnAction(e -> service.transmitText(txArea.getText()));
        cancelBtn.setOnAction(e   -> service.cancelTransmit());

        txStateLabel.setFont(Font.font("monospace", 9));
        VBox xmitContent = new VBox(4,
            new HBox(6, transmitBtn, cancelBtn), txStateLabel);
        xmitContent.setAlignment(Pos.CENTER_LEFT);

        // ── WAV / Theme ───────────────────────────────────────────────
        saveTxWavBtn.getStyleClass().add("tb-btn");
        saveTxWavBtn.setOnAction(e -> saveTxWav((Stage) txArea.getScene().getWindow()));
        themeBtn.getStyleClass().add("tb-btn");
        themeBtn.setFont(Font.font(14));
        themeBtn.setTooltip(new Tooltip("Toggle light / dark theme"));
        themeBtn.setOnAction(e -> toggleTheme());

        // ── SNR VU meter ──────────────────────────────────────────────
        Label snrSectionLbl = instLabel("SIGNAL");
        rx_snr.getStyleClass().add("jd-rx-sub");
        drawSnrBar(0);
        VBox snrContent = new VBox(3, snrSectionLbl, snrCanvas, rx_snr);
        snrContent.setAlignment(Pos.CENTER_LEFT);

        // ── Rotor bearing ─────────────────────────────────────────────
        Label rotorSectionLbl = instLabel("BEARING");
        rotorLabel.setFont(Font.font("monospace", FontWeight.BOLD, 18));
        rotorLabel.getStyleClass().add("jd-rotor");
        VBox rotorContent = new VBox(2, rotorSectionLbl, rotorLabel);
        rotorContent.setAlignment(Pos.CENTER_LEFT);

        // ── Indicator dots ────────────────────────────────────────────
        VBox hubGrp = dotGroup(hubDot, "HUB");
        VBox rxGrp  = dotGroup(rxDot,  "RX");
        VBox txGrp  = dotGroup(txDot,  "TX");
        HBox dotsContent = new HBox(10, hubGrp, rxGrp, txGrp);
        dotsContent.setAlignment(Pos.CENTER);

        // ── Panel toggle buttons ──────────────────────────────────────
        Button logBtn   = new Button("Log");
        Button spotsBtn = new Button("Spots");
        logBtn.getStyleClass().add("tb-btn");
        spotsBtn.getStyleClass().add("tb-btn");
        logBtn.setOnAction(e   -> { showRightPanel(); rightPanel.showLogTab();   });
        spotsBtn.setOnAction(e -> { showRightPanel(); rightPanel.showSpotsTab(); });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(6,
            bezel("STATION",  stationBox),   tbSep(),
            bezel("FREQUENCY", freqContent), tbSep(),
            bezel("MODE",     modeVBox),     tbSep(),
            bezel("GUARD SW", guardContent), tbSep(),
            bezel("CONTROL",  xmitContent),  tbSep(),
            bezel("AUX",      new HBox(4, saveTxWavBtn, themeBtn)), tbSep(),
            bezel("SNR",      snrContent),   tbSep(),
            bezel("ROTOR",    rotorContent), tbSep(),
            bezel("STATUS",   dotsContent),
            spacer,
            logBtn, spotsBtn
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(5, 8, 5, 8));
        bar.getStyleClass().add("jd-toolbar");
        return bar;
    }

    private HBox buildMacroRow() {
        MacroBar macroBar = new MacroBar(
            service,
            () -> rightPanel.getLogEntryPane().getCallsign(),
            () -> rightPanel.getLogEntryPane().getRst()
        );
        return macroBar;
    }

    // ================================================================
    // Center — DM-780 layout: waterfall on top, RX/TX below
    // ================================================================

    private SplitPane buildCenter() {
        spectrumPane.setPrefHeight(65);
        spectrumPane.setMinHeight(50);
        spectrumPane.setMaxHeight(80);

        waterfallPane.setPrefHeight(160);
        waterfallPane.setMinHeight(100);
        waterfallPane.setMaxHeight(220);

        SplitPane rxTxSplit = new SplitPane();
        rxTxSplit.setOrientation(Orientation.VERTICAL);
        rxTxSplit.getItems().addAll(buildRxPane(), buildTxPane());
        rxTxSplit.setDividerPositions(0.65);
        VBox.setVgrow(rxTxSplit, Priority.ALWAYS);

        VBox leftColumn = new VBox(0,
            spectrumPane,
            waterfallPane,
            buildFreqRuler(),
            rxTxSplit
        );

        mainSplit = new SplitPane();
        mainSplit.setOrientation(Orientation.HORIZONTAL);
        mainSplit.getItems().addAll(leftColumn, rightPanel);
        mainSplit.setDividerPositions(PANEL_OPEN);
        return mainSplit;
    }

    private Pane buildFreqRuler() {
        Canvas canvas = new Canvas(800, 20);
        Pane wrapper = new Pane(canvas);
        wrapper.getStyleClass().add("jd-ruler");
        wrapper.setPrefHeight(20);
        wrapper.setMaxHeight(20);
        wrapper.widthProperty().addListener((obs, o, w) -> {
            canvas.setWidth(w.doubleValue());
            drawFreqRuler(canvas);
        });
        return wrapper;
    }

    private void drawFreqRuler(Canvas c) {
        if (c.getWidth() == 0) return;
        double w = c.getWidth(), h = c.getHeight();
        GraphicsContext gc = c.getGraphicsContext2D();

        gc.setFill(Color.web(darkTheme ? "#1a1a1a" : "#f4f4f4"));
        gc.fillRect(0, 0, w, h);

        String tickColor  = darkTheme ? "#3a5a7a" : "#9ab4d0";
        String labelColor = darkTheme ? "#888888" : "#666666";
        gc.setFont(Font.font("monospace", 8));

        int[] ticks = {0,250,500,750,1000,1250,1500,1750,2000,
                        2250,2500,2750,3000,3250,3500,3750,4000};
        for (int hz : ticks) {
            double x = (hz / 4000.0) * w;
            boolean major = hz % 1000 == 0;
            gc.setStroke(Color.web(major ? tickColor : (darkTheme ? "#2a3a4a" : "#ccd8e8")));
            gc.setLineWidth(0.5);
            gc.strokeLine(x, 0, x, major ? 10 : 5);
            if (major) {
                gc.setFill(Color.web(labelColor));
                gc.fillText(hz == 0 ? "0" : (hz / 1000) + "k", x + 2, h - 1);
            }
        }
    }

    // ── RX pane ──────────────────────────────────────────────────────

    private VBox buildRxPane() {
        rxArea.setEditable(false);
        rxArea.setWrapText(false);
        rxArea.setFont(Font.font("monospace", 13));
        rxArea.getStyleClass().add("rx-area");

        rx_peak.getStyleClass().add("jd-rx-sub");
        rx_afc.getStyleClass().add("jd-rx-sub");
        autoScrollBox.setSelected(true);
        autoScrollBox.getStyleClass().add("cockpit-check");

        clearRxBtn.getStyleClass().add("tb-btn");
        sendToLogBtn.getStyleClass().add("secondary-button");
        clearRxBtn.setOnAction(e  -> { rxArea.clear(); lastDecodeLine = ""; });
        sendToLogBtn.setOnAction(e -> sendToLog());

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox header = new HBox(10,
            panelTitle("RECEIVE"), tbSep(),
            rx_snr, rx_peak, rx_afc,
            sp,
            autoScrollBox, clearRxBtn, sendToLogBtn
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 8));
        header.getStyleClass().add("jd-rx-header");

        VBox pane = new VBox(0, header, rxArea);
        VBox.setVgrow(rxArea, Priority.ALWAYS);
        return pane;
    }

    // ── TX pane ──────────────────────────────────────────────────────

    private VBox buildTxPane() {
        txArea.setWrapText(true);
        txArea.setFont(Font.font("monospace", 13));
        txArea.setPromptText("Enter text to transmit…");
        txArea.getStyleClass().add("tx-area");
        txArea.textProperty().addListener((obs, o, n) ->
            txCharCount.setText(n.length() + " ch"));

        txStateLabel.setFont(Font.font("monospace", FontWeight.BOLD, 10));
        txCharCount.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        txCharCount.getStyleClass().add("jd-rx-sub");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        HBox header = new HBox(10,
            panelTitle("TRANSMIT"), tbSep(),
            txStateLabel, sp, txCharCount
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(4, 8, 4, 8));
        header.getStyleClass().add("jd-tx-header");

        VBox pane = new VBox(0, header, txArea);
        VBox.setVgrow(txArea, Priority.ALWAYS);
        return pane;
    }

    // ================================================================
    // Status bar
    // ================================================================

    private HBox buildStatusBar() {
        Label sbCall = new Label();
        sbCall.textProperty().bind(callsignLabel.textProperty());
        sbCall.getStyleClass().add("jd-callsign");
        sbCall.setStyle("-fx-font-weight: bold;");

        HBox bar = new HBox(0,
            sbItem(sbCall,   null),
            sbItem(sb_snr,   null),
            sbItem(sb_peak,  null),
            sbItem(sb_mode,  "MODE"),
            sbItem(sb_audio, null),
            sbItem(sb_hub,   null)
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");
        bar.setMinHeight(24);
        bar.setMaxHeight(24);
        bar.getStyleClass().add("jd-statusbar");
        return bar;
    }

    // ================================================================
    // Service wiring
    // ================================================================

    private void wireService() {
        service.setSpectrumListener(snap -> {
            spectrumPane.update(snap);
            waterfallPane.update(snap);
            spectrumPane.setPeakFrequencyHz(snap.getPeakFrequencyHz());
        });
        service.setDecodeListener(this::onDecodeReceived);
        service.setStatusListener(this::onStatusUpdate);
        service.setRotorListener(this::onRotorUpdate);

        service.setStationListener(parts -> Platform.runLater(() -> {
            String call = parts.length > 0 ? parts[0] : "NOCALL";
            String grid = parts.length > 1 ? parts[1] : "";
            callsignLabel.setText(call);
            gridLabel.setText(grid);
            rightPanel.getLogEntryPane().setStationCallsign(call);
        }));

        service.setSpotSelectedListener(spot ->
            Platform.runLater(() -> rightPanel.onHubSpotSelected(spot)));
    }

    private void wireToggleButtons() {
        afcBtn.setOnAction(e -> {
            boolean on = afcBtn.isSelected();
            rx_afc.setStyle("-fx-text-fill: " + (on ? accent() : sub()) + ";");
        });
    }

    private void onDecodeReceived(String line) {
        if (line == null || line.isBlank()) return;
        if (sqlBtn.isSelected() && !line.contains(callsignLabel.getText())) return;
        rxArea.appendText(line + System.lineSeparator());
        lastDecodeLine = line;
        if (autoScrollBox.isSelected())
            rxArea.positionCaret(rxArea.getText().length());
    }

    private void onStatusUpdate(ModemStatus st) {
        lastStatus = st;

        boolean hubOk    = st.isHubConnected();
        boolean audioOk  = st.isAudioRunning();
        boolean txActive = st.isTransmitting();

        // Indicator dots
        hubDot.setFill(hubOk  ? Color.web(ok())  : Color.web(err()));
        rxDot.setFill(audioOk ? Color.web(ok())  : Color.web(sub()));
        TxState txState = st.getTxState() != null ? st.getTxState() : TxState.IDLE;
        txDot.setFill(switch (txState) {
            case TRANSMITTING       -> Color.web(err());
            case STARTING, STOPPING -> Color.web(warn());
            case COMPLETE           -> Color.web(ok());
            default                 -> Color.web(sub());
        });

        // TX state label
        String stateTxt = switch (txState) {
            case TRANSMITTING -> "TRANSMITTING";
            case STARTING     -> "STARTING…";
            case STOPPING     -> "STOPPING…";
            case COMPLETE     -> "COMPLETE";
            default           -> "IDLE";
        };
        txStateLabel.setText(stateTxt);
        txStateLabel.setStyle(
            "-fx-font-family: monospace; -fx-font-size: 10; -fx-font-weight: bold;" +
            "-fx-text-fill: " + (txActive ? err() : sub()) + ";");

        // Frequency
        long hz = st.getRigFrequencyHz();
        freqLabel.setText(hz > 0 ? formatFreq(hz) : "——  ———  ———");
        double audioHz = st.getPeakFrequencyHz();
        audioOffLabel.setText(audioHz > 0 ? "%.0f Hz".formatted(audioHz) : "——— Hz");
        modeTag.setText(st.getRigMode() != null && !st.getRigMode().isBlank()
                        ? st.getRigMode() : "---");

        // SNR
        double snr = st.getSnr();
        drawSnrBar(snr);
        String snrColor = snr > 20 ? err() : snr > 10 ? warn() : teal();
        rx_snr.setStyle("-fx-font-family: monospace; -fx-font-size: 9; -fx-text-fill: " + snrColor + ";");
        rx_snr.setText("SNR %.1fdB".formatted(snr));
        rx_peak.setText("Pk %.0fHz".formatted(audioHz));

        // Status bar
        sb_snr.setText("SNR %.1f".formatted(snr));
        sb_peak.setText("%.0fHz".formatted(audioHz));
        sb_mode.setText(st.getMode() != null ? st.getMode().name() : "—");
        sb_audio.setText(audioOk ? "Audio ●" : "Audio ○");
        sb_audio.setStyle("-fx-text-fill: " + (audioOk ? ok() : err()) + ";");
        sb_hub.setText(hubOk ? "Hub ●" : "Hub ○");
        sb_hub.setStyle("-fx-text-fill: " + (hubOk ? ok() : err()) + ";");

        // Button states
        transmitBtn.setDisable(txActive);
        cancelBtn.setDisable(!txActive);
        saveTxWavBtn.setDisable(txActive);
        modeBox.setDisable(txActive);

        if (st.getMode() != null && modeBox.getValue() != st.getMode()) {
            modeBox.setValue(st.getMode());
            updateTxPrompt(st.getMode());
        }
        spectrumPane.setStatusMode(st.getMode());
    }

    private void onRotorUpdate(RotorStatus rotor) {
        Platform.runLater(() ->
            rotorLabel.setText("%.0f°".formatted(rotor.bearing)));
    }

    // ================================================================
    // Mode selector
    // ================================================================

    private void wireModeBox() {
        modeBox.setOnAction(e -> {
            service.setMode(modeBox.getValue());
            updateTxPrompt(modeBox.getValue());
        });
    }

    private void updateTxPrompt(ModeType mode) {
        if (mode == null) return;
        txArea.setPromptText("Enter text to transmit as " + mode.name() + "…");
    }

    // ================================================================
    // Panel toggle
    // ================================================================

    private void toggleRightPanel() {
        rightPanelVisible = !rightPanelVisible;
        mainSplit.setDividerPositions(rightPanelVisible ? PANEL_OPEN : PANEL_CLOSE);
    }

    private void showRightPanel() {
        rightPanelVisible = true;
        mainSplit.setDividerPositions(PANEL_OPEN);
    }

    // ================================================================
    // Transmit helpers
    // ================================================================

    private void configureTextAreas() {
        rxArea.setPrefRowCount(14);
        txArea.setPrefRowCount(5);
    }

    private void saveTxWav(Stage stage) {
        String text = txArea.getText();
        if (text == null || text.isBlank()) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save TX WAV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV (*.wav)", "*.wav"));
        fc.setInitialFileName(modeBox.getValue().name().toLowerCase() + "_tx.wav");
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        try {
            service.saveTransmitWav(text, f.toPath());
        } catch (Exception ex) {
            onDecodeReceived("[ERR] WAV: " + ex.getMessage());
        }
    }

    // ================================================================
    // Send-to-log
    // ================================================================

    private void sendToLog() {
        String source = rxArea.getSelectedText();
        if (source == null || source.isBlank()) source = lastDecodeLine;
        if (source == null || source.isBlank()) return;

        String call = extractCallsign(source);
        long   hz   = lastStatus != null ? lastStatus.getRigFrequencyHz() : 0L;
        String band = hz > 0 ? bandFromHz(hz) : "";
        String mode = modeBox.getValue() != null ? modeBox.getValue().name() : "RTTY";

        service.sendLogDraft(call, mode, band, hz, "599", "599",
                             extractExchange(source, call), source, 0.0);
        rightPanel.getLogEntryPane().prefillCallsign(call);
        showRightPanel();
        rightPanel.showLogTab();
    }

    private String extractCallsign(String text) {
        Matcher m = CALLSIGN_RE.matcher(text.toUpperCase());
        while (m.find()) {
            String c = m.group(1);
            if (c.chars().anyMatch(Character::isDigit)
                    && c.chars().anyMatch(Character::isLetter))
                return c;
        }
        return "";
    }

    private String extractExchange(String text, String callsign) {
        String s = text.toUpperCase();
        if (!callsign.isBlank()) s = s.replace(callsign, " ");
        s = s.replaceAll("\\b(CQ|DE|TEST|QRZ|K)\\b", " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    // ================================================================
    // Setup dialog
    // ================================================================

    private void showSetupDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("J-Digi Setup");
        dialog.setHeaderText(null);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField hubField = new TextField(service.getHubUrl());

        List<AudioEngine.AudioInputDevice> inputs = service.getAvailableAudioInputDevices();
        ComboBox<AudioEngine.AudioInputDevice> inputBox =
            new ComboBox<>(FXCollections.observableArrayList(inputs));
        String curInput = service.getSelectedAudioInputDevice();
        inputs.stream().filter(d -> d.id().equals(curInput)).findFirst()
              .ifPresent(inputBox::setValue);
        if (inputBox.getValue() == null && !inputs.isEmpty()) inputBox.setValue(inputs.get(0));

        List<AudioTxEngine.AudioOutputDevice> outputs = service.getAvailableAudioOutputDevices();
        ComboBox<AudioTxEngine.AudioOutputDevice> outputBox =
            new ComboBox<>(FXCollections.observableArrayList(outputs));
        String curOutput = service.getSelectedAudioOutputDevice();
        outputs.stream().filter(d -> d.id().equals(curOutput)).findFirst()
               .ifPresent(outputBox::setValue);
        if (outputBox.getValue() == null && !outputs.isEmpty()) outputBox.setValue(outputs.get(0));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new Insets(10));
        int r = 0;
        grid.add(new Label("Hub WebSocket URL:"), 0, r); grid.add(hubField,  1, r++);
        grid.add(new Label("Audio Input:"),       0, r); grid.add(inputBox,  1, r++);
        grid.add(new Label("Audio Output:"),      0, r); grid.add(outputBox, 1, r);
        hubField.setPrefWidth(360);
        inputBox.setPrefWidth(360);
        outputBox.setPrefWidth(360);

        Label hint = new Label("Callsign and macros are managed in J-Hub → http://hub:8081");
        hint.setStyle("-fx-font-style: italic; -fx-font-size: 11;");
        hint.getStyleClass().add("jd-rx-sub");

        dialog.getDialogPane().setContent(new VBox(10, grid, hint));

        dialog.showAndWait().ifPresent(result -> {
            if (result == saveType) {
                service.reconnectHubFromSetup(hubField.getText().trim());
                AudioEngine.AudioInputDevice selIn = inputBox.getValue();
                if (selIn != null) service.setAudioInputDevice(selIn.id());
                AudioTxEngine.AudioOutputDevice selOut = outputBox.getValue();
                if (selOut != null) service.setAudioOutputDevice(selOut.id());
            }
        });
    }

    // ================================================================
    // Visual helpers
    // ================================================================

    /** Wraps content in a labeled bezel box. CSS class controls all colors. */
    private VBox bezel(String sectionName, javafx.scene.Node content) {
        Label lbl = instLabel(sectionName);
        VBox box = new VBox(3, lbl, content);
        box.setAlignment(Pos.TOP_LEFT);
        box.setPadding(new Insets(4, 8, 4, 8));
        box.getStyleClass().add("jd-bezel");
        return box;
    }

    /** Tiny all-caps section label for bezel headers and dot groups. */
    private Label instLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("jd-inst-label");
        return l;
    }

    /** RECEIVE / TRANSMIT nameplate label. */
    private Label panelTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("jd-panel-title");
        return l;
    }

    /** Indicator dot + label below. */
    private VBox dotGroup(Circle dot, String label) {
        dot.setRadius(5);
        VBox box = new VBox(2, dot, instLabel(label));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Separator tbSep() {
        Separator s = new Separator(Orientation.VERTICAL);
        s.setPrefHeight(44);
        return s;
    }

    private HBox sbItem(Label value, String keyPrefix) {
        HBox item = new HBox(0);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(0, 10, 0, 10));
        item.getStyleClass().add("jd-sb-item");
        item.setMinHeight(24);
        if (keyPrefix != null) {
            Label key = new Label(keyPrefix + ": ");
            key.getStyleClass().add("jd-rx-sub");
            item.getChildren().addAll(key, value);
        } else {
            item.getChildren().add(value);
        }
        return item;
    }

    /** Segmented VU-meter bar — segment colors are theme-aware. */
    private void drawSnrBar(double snrDb) {
        GraphicsContext gc = snrCanvas.getGraphicsContext2D();
        double w = snrCanvas.getWidth(), h = snrCanvas.getHeight();
        int    segs    = 22;
        double segW    = (w - segs + 1.0) / segs;
        double fill    = Math.min(1.0, Math.max(0, snrDb / 30.0));
        int    litSegs = (int) (fill * segs);

        gc.setFill(Color.web(darkTheme ? "#1a1a1a" : "#e8eef8"));
        gc.fillRect(0, 0, w, h);

        for (int i = 0; i < segs; i++) {
            double x = i * (segW + 1.0);
            boolean lit = i < litSegs;
            String color;
            if (i < segs * 0.55) {
                color = lit ? (darkTheme ? "#80cbc4" : "#00897b")
                            : (darkTheme ? "#0d2a28" : "#c8e6e2");
            } else if (i < segs * 0.82) {
                color = lit ? (darkTheme ? "#80deea" : "#0277bd")
                            : (darkTheme ? "#0d2030" : "#c8dff0");
            } else {
                color = lit ? (darkTheme ? "#ef9a9a" : "#c62828")
                            : (darkTheme ? "#2a0d0d" : "#f0c8c8");
            }
            gc.setFill(Color.web(color));
            gc.fillRect(x, 0, segW, h);
        }
    }

    private String formatFreq(long hz) {
        return "%d  %03d  %03d".formatted(hz / 1_000_000, (hz % 1_000_000) / 1_000, hz % 1_000);
    }

    private String bandFromHz(long hz) {
        long k = hz / 1000;
        if (k >= 1800  && k <= 2000)    return "160m";
        if (k >= 3500  && k <= 4000)    return "80m";
        if (k >= 7000  && k <= 7300)    return "40m";
        if (k >= 10100 && k <= 10150)   return "30m";
        if (k >= 14000 && k <= 14350)   return "20m";
        if (k >= 18068 && k <= 18168)   return "17m";
        if (k >= 21000 && k <= 21450)   return "15m";
        if (k >= 24890 && k <= 24990)   return "12m";
        if (k >= 28000 && k <= 29700)   return "10m";
        if (k >= 50000 && k <= 54000)   return "6m";
        if (k >= 144000 && k <= 148000) return "2m";
        if (k >= 420000 && k <= 450000) return "70cm";
        return "";
    }

    // ================================================================
    // CSS — single combined stylesheet; .dark / .light on root node
    // controls which theme rules win via descendant-selector specificity.
    // ================================================================

    /** Writes the combined CSS file once at startup. */
    private String buildCombinedStylesheet() {
        String css = buildCss();
        try {
            java.nio.file.Path tmp =
                java.nio.file.Files.createTempFile("jdigi-theme-", ".css");
            java.nio.file.Files.writeString(tmp, css);
            tmp.toFile().deleteOnExit();
            return tmp.toUri().toString();
        } catch (Exception e) {
            // fallback: inline (JavaFX doesn't support data: URIs for stylesheets,
            // so if this path is taken the app will have no custom styling)
            return "";
        }
    }

    private String buildCss() {
        return
        // ── Shared structural rules (no colors) ──────────────────────
        """
        .root {
            -fx-font-family: 'DejaVu Sans', 'Liberation Sans', sans-serif;
            -fx-font-size: 13;
        }
        .rx-area, .tx-area { -fx-font-family: monospace; -fx-font-size: 13; }

        .jd-inst-label  { -fx-font-size: 8;  -fx-font-weight: bold; }
        .jd-panel-title { -fx-font-size: 11; -fx-font-weight: bold; }
        .jd-freq-display{ -fx-font-family: monospace; -fx-font-size: 26; -fx-font-weight: bold; }
        .jd-audio-sub   { -fx-font-family: monospace; -fx-font-size: 11; }
        .jd-grid-label  { -fx-font-family: monospace; -fx-font-size: 10; }
        .jd-callsign    { -fx-font-family: monospace; -fx-font-size: 18; -fx-font-weight: bold; }
        .jd-rotor       { -fx-font-family: monospace; -fx-font-size: 18; -fx-font-weight: bold; }
        .jd-mode-tag    { -fx-font-family: monospace; -fx-font-size: 10; -fx-font-weight: bold;
                          -fx-padding: 1 6 1 6; -fx-background-radius: 3; -fx-border-radius: 3;
                          -fx-border-width: 1; }
        .jd-rx-sub      { -fx-font-family: monospace; -fx-font-size: 10; }
        .jd-section-label { -fx-font-size: 13; -fx-font-weight: bold; -fx-padding: 2 0 6 0; }
        .jd-status-label  { -fx-font-style: italic; -fx-font-size: 11; }
        .jd-entry-pane    { }

        .jd-bezel      { -fx-border-radius: 3; -fx-background-radius: 3; -fx-border-width: 1; }
        .jd-freq-panel { -fx-border-radius: 3; -fx-background-radius: 3; -fx-border-width: 1; }
        .jd-rx-header  { -fx-border-width: 0 0 1 0; }
        .jd-tx-header  { -fx-border-width: 0 0 1 0; }
        .jd-statusbar  { -fx-border-width: 1 0 0 0; }
        .jd-toolbar    { -fx-border-width: 0 0 1 0; }
        .jd-sb-item    { -fx-border-width: 0 1 0 0; }

        .afc-btn, .sql-btn { -fx-font-size: 10; -fx-font-weight: bold;
                             -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3;
                             -fx-padding: 3 8 3 8; }
        .xmit-btn   { -fx-font-weight: bold; -fx-background-radius: 4;
                      -fx-border-width: 0; -fx-text-fill: white; }
        .cancel-btn { -fx-background-radius: 4; -fx-border-width: 1; }
        .tb-btn     { -fx-font-size: 11; -fx-font-weight: bold;
                      -fx-background-radius: 4; -fx-border-width: 1;
                      -fx-padding: 5 12 5 12; }
        .macro-button, .macro-button-programmable, .macro-button-disabled {
            -fx-font-size: 11; -fx-min-width: 44px;
            -fx-padding: 3 6 3 6; -fx-background-radius: 4; }
        .macro-button-disabled { -fx-opacity: 0.45; }
        .primary-button   { -fx-background-radius: 4; -fx-font-weight: bold; -fx-text-fill: white; }
        .secondary-button { -fx-background-radius: 4; }
        .entry-label      { -fx-font-weight: bold; -fx-font-size: 11; }
        .cockpit-check .box { -fx-border-width: 1; -fx-background-radius: 3; -fx-border-radius: 3; }
        """
        // ── Dark theme — root carries .dark class ────────────────────
        + """
        .dark.root {
            -fx-base: #2b2b2b;
            -fx-background: #1e1e1e;
            -fx-control-inner-background: #2d2d2d;
            -fx-accent: #4fc3f7;
            -fx-focus-color: #4fc3f7;
            -fx-faint-focus-color: transparent;
        }
        .dark .label { -fx-text-fill: #e0e0e0; }
        .dark .jd-toolbar    { -fx-background-color: #1a2a3a; -fx-border-color: #555555; }
        .dark .jd-bezel      { -fx-background-color: #252535; -fx-border-color: #555555; }
        .dark .jd-freq-panel { -fx-background-color: #1a1a1a; -fx-border-color: #555555; }
        .dark .jd-ruler      { -fx-background-color: #1a1a1a; }
        .dark .jd-rx-header  { -fx-background-color: #1a2a4a; -fx-border-color: #555555; }
        .dark .jd-tx-header  { -fx-background-color: #252535; -fx-border-color: #555555; }
        .dark .jd-statusbar  { -fx-background-color: #0d1b2a; -fx-border-color: #555555; }
        .dark .jd-sb-item    { -fx-border-color: #555555; }
        .dark .jd-callsign   { -fx-text-fill: #4fc3f7; }
        .dark .jd-grid-label { -fx-text-fill: #888888; }
        .dark .jd-freq-display { -fx-text-fill: #4fc3f7; }
        .dark .jd-audio-sub  { -fx-text-fill: #80cbc4; }
        .dark .jd-mode-tag   { -fx-text-fill: #90caf9; -fx-background-color: #1a2a4a; -fx-border-color: #555555; }
        .dark .jd-rotor      { -fx-text-fill: #80deea; }
        .dark .jd-inst-label { -fx-text-fill: #888888; }
        .dark .jd-rx-sub     { -fx-text-fill: #888888; }
        .dark .jd-panel-title{ -fx-text-fill: #90caf9; }
        .dark .jd-section-label { -fx-text-fill: #90caf9; }
        .dark .jd-status-label  { -fx-text-fill: #888888; }
        .dark .entry-label   { -fx-text-fill: #90caf9; }
        .dark .rx-area { -fx-control-inner-background: #1a1a1a; -fx-text-fill: #80cbc4;
                         -fx-highlight-fill: #1a3a5a; -fx-highlight-text-fill: #e0e0e0; }
        .dark .rx-area .content { -fx-background-color: #1a1a1a; }
        .dark .tx-area { -fx-control-inner-background: #2d2d2d; -fx-text-fill: #e0e0e0;
                         -fx-highlight-fill: #1a3a5a; -fx-highlight-text-fill: #e0e0e0;
                         -fx-prompt-text-fill: #555555; }
        .dark .tx-area .content { -fx-background-color: #2d2d2d; }
        .dark .afc-btn          { -fx-background-color: #2d2d2d; -fx-text-fill: #555555; -fx-border-color: #555555; }
        .dark .afc-btn:selected { -fx-background-color: #1a2a4a; -fx-text-fill: #4fc3f7; -fx-border-color: #4fc3f7; }
        .dark .afc-btn:hover    { -fx-background-color: #3a3a3a; }
        .dark .sql-btn          { -fx-background-color: #2d2d2d; -fx-text-fill: #555555; -fx-border-color: #555555; }
        .dark .sql-btn:selected { -fx-background-color: #0d2a28; -fx-text-fill: #80cbc4; -fx-border-color: #80cbc4; }
        .dark .sql-btn:hover    { -fx-background-color: #3a3a3a; }
        .dark .xmit-btn          { -fx-background-color: #014479; }
        .dark .xmit-btn:hover    { -fx-background-color: #0277bd; }
        .dark .xmit-btn:disabled { -fx-background-color: #2d2d2d; -fx-text-fill: #555555; }
        .dark .cancel-btn          { -fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0; -fx-border-color: #555555; }
        .dark .cancel-btn:hover    { -fx-background-color: #4a4a4a; -fx-text-fill: #ef9a9a; -fx-border-color: #ef9a9a; }
        .dark .cancel-btn:disabled { -fx-background-color: #2d2d2d; -fx-text-fill: #555555; -fx-border-color: #3a3a3a; }
        .dark .tb-btn      { -fx-background-color: #2d2d2d; -fx-text-fill: #888888; -fx-border-color: #555555; }
        .dark .tb-btn:hover{ -fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0; }
        .dark .macro-button              { -fx-background-color: #014479; -fx-text-fill: #b3e5fc; }
        .dark .macro-button:hover        { -fx-background-color: #0277bd; }
        .dark .macro-button-programmable { -fx-background-color: #0d3a5a; -fx-text-fill: #b3e5fc; }
        .dark .macro-button-programmable:hover { -fx-background-color: #0277bd; }
        .dark .macro-button-disabled     { -fx-background-color: #2d2d2d; -fx-text-fill: #555555; }
        .dark .primary-button      { -fx-background-color: #014479; }
        .dark .primary-button:hover{ -fx-background-color: #0277bd; }
        .dark .secondary-button      { -fx-background-color: #2d2d2d; -fx-text-fill: #90caf9; -fx-border-color: #555555; -fx-border-width: 1; }
        .dark .secondary-button:hover{ -fx-background-color: #3a3a3a; }
        .dark .cockpit-check .text { -fx-fill: #888888; -fx-font-weight: bold; }
        .dark .cockpit-check .box  { -fx-background-color: #3a3a3a; -fx-border-color: #555555; }
        .dark .cockpit-check:selected .mark { -fx-background-color: #4fc3f7; }
        .dark .split-pane { -fx-background-color: #1e1e1e; }
        .dark .split-pane > .split-pane-divider { -fx-background-color: #2d2d2d; -fx-padding: 0 1 0 1; }
        .dark .split-pane > .split-pane-divider:hover { -fx-background-color: #4fc3f7; }
        .dark .tab-pane { -fx-background-color: #1e1e1e; }
        .dark .tab-pane > .tab-header-area { -fx-background-color: #2a2a2a; }
        .dark .tab { -fx-background-color: #383838; -fx-padding: 4 14 4 14; -fx-background-radius: 0; }
        .dark .tab:selected { -fx-background-color: #1a3a5a; }
        .dark .tab .tab-label { -fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 11; }
        .dark .tab:selected .tab-label { -fx-text-fill: #4fc3f7; }
        .dark .combo-box { -fx-background-color: #3a3a3a; -fx-border-color: #555555; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3; }
        .dark .combo-box .list-cell { -fx-text-fill: #e0e0e0; -fx-background-color: #3a3a3a; }
        .dark .combo-box-popup .list-view { -fx-background-color: #2b2b2b; -fx-border-color: #555555; }
        .dark .combo-box-popup .list-cell { -fx-text-fill: #e0e0e0; -fx-background-color: #2b2b2b; }
        .dark .combo-box-popup .list-cell:hover { -fx-background-color: #1a3a5a; }
        .dark .scroll-bar { -fx-background-color: #1e1e1e; }
        .dark .scroll-bar .thumb { -fx-background-color: #3a3a3a; -fx-background-radius: 3; }
        .dark .scroll-bar .thumb:hover { -fx-background-color: #4a4a4a; }
        .dark .scroll-bar .increment-button, .dark .scroll-bar .decrement-button { -fx-background-color: #2b2b2b; }
        .dark .menu-bar { -fx-background-color: #2d2d2d; }
        .dark .menu-bar .label { -fx-text-fill: #e0e0e0; }
        .dark .menu-bar .menu:hover, .dark .menu-bar .menu:showing { -fx-background-color: #3a3a3a; }
        .dark .context-menu { -fx-background-color: #3a3a3a; -fx-border-color: #555555; }
        .dark .menu-item .label { -fx-text-fill: #e0e0e0; }
        .dark .menu-item:hover  { -fx-background-color: #1a3a5a; }
        .dark .table-view { -fx-background-color: #2a2a2a; -fx-control-inner-background: #2a2a2a; }
        .dark .table-view .column-header { -fx-background-color: #1a2a3a; -fx-border-color: #555555; -fx-border-width: 0 0 1 0; }
        .dark .table-view .column-header .label { -fx-text-fill: #90caf9; -fx-font-weight: bold; }
        .dark .table-row-cell { -fx-background-color: #2a2a2a; -fx-text-fill: #e0e0e0; }
        .dark .table-row-cell:odd { -fx-background-color: #303030; }
        .dark .table-row-cell:selected { -fx-background-color: #1a3a5a; }
        .dark .table-cell { -fx-text-fill: #e0e0e0; -fx-font-size: 11; }
        .dark .dialog-pane { -fx-background-color: #2b2b2b; }
        .dark .dialog-pane .label { -fx-text-fill: #e0e0e0; }
        .dark .dialog-pane .button-bar .button { -fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0; -fx-background-radius: 4; }
        .dark .text-field { -fx-background-color: #3a3a3a; -fx-text-fill: #e0e0e0;
                            -fx-border-color: #555555; -fx-border-width: 1;
                            -fx-border-radius: 3; -fx-background-radius: 3;
                            -fx-prompt-text-fill: #555555; }
        .dark .text-field:focused { -fx-border-color: #4fc3f7; }
        .dark .text-area .content { -fx-background-color: #2d2d2d; }
        """
        // ── Light theme — root carries .light class ──────────────────
        + """
        .light.root {
            -fx-base: #ececec;
            -fx-background: #f4f4f4;
            -fx-control-inner-background: #ffffff;
            -fx-accent: #1565c0;
            -fx-focus-color: #1565c0;
            -fx-faint-focus-color: transparent;
        }
        .light .jd-toolbar    { -fx-background-color: #dce4f0; -fx-border-color: #c8d0e8; }
        .light .jd-bezel      { -fx-background-color: #eef2fb; -fx-border-color: #c8d0e8; }
        .light .jd-freq-panel { -fx-background-color: #ffffff; -fx-border-color: #c8d0e8; }
        .light .jd-ruler      { -fx-background-color: #f4f4f4; }
        .light .jd-rx-header  { -fx-background-color: #c5cfe8; -fx-border-color: #b0bcd8; }
        .light .jd-tx-header  { -fx-background-color: #dde3f4; -fx-border-color: #b0bcd8; }
        .light .jd-statusbar  { -fx-background-color: #e8ecf4; -fx-border-color: #c8d0e8; }
        .light .jd-sb-item    { -fx-border-color: #c8d0e8; }
        .light .jd-callsign   { -fx-text-fill: #1565c0; }
        .light .jd-grid-label { -fx-text-fill: #666666; }
        .light .jd-freq-display { -fx-text-fill: #1565c0; }
        .light .jd-audio-sub  { -fx-text-fill: #00695c; }
        .light .jd-mode-tag   { -fx-text-fill: #1565c0; -fx-background-color: #e3f2fd; -fx-border-color: #90caf9; }
        .light .jd-rotor      { -fx-text-fill: #0277bd; }
        .light .jd-inst-label { -fx-text-fill: #666666; }
        .light .jd-rx-sub     { -fx-text-fill: #666666; }
        .light .jd-panel-title{ -fx-text-fill: #1565c0; }
        .light .jd-section-label { -fx-text-fill: #1565c0; }
        .light .jd-status-label  { -fx-text-fill: #666666; }
        .light .entry-label   { -fx-text-fill: #1565c0; }
        .light .rx-area { -fx-control-inner-background: #f0f8f4; -fx-text-fill: #004d40;
                          -fx-highlight-fill: #bbdefb; -fx-highlight-text-fill: #212121; }
        .light .rx-area .content { -fx-background-color: #f0f8f4; }
        .light .tx-area { -fx-control-inner-background: #ffffff; -fx-text-fill: #212121;
                          -fx-highlight-fill: #bbdefb; -fx-highlight-text-fill: #212121;
                          -fx-prompt-text-fill: #aaaaaa; }
        .light .tx-area .content { -fx-background-color: #ffffff; }
        .light .afc-btn          { -fx-background-color: #e8e8e8; -fx-text-fill: #aaaaaa; -fx-border-color: #c8d0e8; }
        .light .afc-btn:selected { -fx-background-color: #bbdefb; -fx-text-fill: #1565c0; -fx-border-color: #1565c0; }
        .light .afc-btn:hover    { -fx-background-color: #dde3f4; }
        .light .sql-btn          { -fx-background-color: #e8e8e8; -fx-text-fill: #aaaaaa; -fx-border-color: #c8d0e8; }
        .light .sql-btn:selected { -fx-background-color: #c8e6c9; -fx-text-fill: #2e7d32; -fx-border-color: #2e7d32; }
        .light .sql-btn:hover    { -fx-background-color: #dde3f4; }
        .light .xmit-btn          { -fx-background-color: #1565c0; }
        .light .xmit-btn:hover    { -fx-background-color: #1e88e5; }
        .light .xmit-btn:disabled { -fx-background-color: #e0e0e0; -fx-text-fill: #aaaaaa; }
        .light .cancel-btn          { -fx-background-color: #f5f5f5; -fx-text-fill: #212121; -fx-border-color: #c8d0e8; }
        .light .cancel-btn:hover    { -fx-background-color: #ffcdd2; -fx-text-fill: #c62828; -fx-border-color: #c62828; }
        .light .cancel-btn:disabled { -fx-background-color: #f5f5f5; -fx-text-fill: #aaaaaa; -fx-border-color: #e0e0e0; }
        .light .tb-btn      { -fx-background-color: #f4f4f4; -fx-text-fill: #555555; -fx-border-color: #c8d0e8; }
        .light .tb-btn:hover{ -fx-background-color: #dce4f0; -fx-text-fill: #1565c0; }
        .light .macro-button              { -fx-background-color: #1565c0; -fx-text-fill: white; }
        .light .macro-button:hover        { -fx-background-color: #1e88e5; }
        .light .macro-button-programmable { -fx-background-color: #1976d2; -fx-text-fill: white; }
        .light .macro-button-programmable:hover { -fx-background-color: #1e88e5; }
        .light .macro-button-disabled     { -fx-background-color: #e0e0e0; -fx-text-fill: #aaaaaa; }
        .light .primary-button      { -fx-background-color: #1565c0; }
        .light .primary-button:hover{ -fx-background-color: #1e88e5; }
        .light .secondary-button      { -fx-background-color: #f4f4f4; -fx-text-fill: #1565c0; -fx-border-color: #c8d0e8; -fx-border-width: 1; }
        .light .secondary-button:hover{ -fx-background-color: #dce4f0; }
        .light .cockpit-check .text { -fx-fill: #555555; -fx-font-weight: bold; }
        .light .cockpit-check .box  { -fx-background-color: #ffffff; -fx-border-color: #c8d0e8; }
        .light .cockpit-check:selected .mark { -fx-background-color: #1565c0; }
        .light .split-pane { -fx-background-color: #f4f4f4; }
        .light .split-pane > .split-pane-divider { -fx-background-color: #dce4f0; -fx-padding: 0 1 0 1; }
        .light .split-pane > .split-pane-divider:hover { -fx-background-color: #1565c0; }
        .light .tab-pane { -fx-background-color: #f4f4f4; }
        .light .tab-pane > .tab-header-area { -fx-background-color: #e0e4ee; }
        .light .tab { -fx-background-color: #eef2fb; -fx-padding: 4 14 4 14; -fx-background-radius: 0; }
        .light .tab:selected { -fx-background-color: #bbdefb; }
        .light .tab .tab-label { -fx-text-fill: #212121; -fx-font-weight: bold; -fx-font-size: 11; }
        .light .tab:selected .tab-label { -fx-text-fill: #1565c0; }
        .light .combo-box { -fx-background-color: #ffffff; -fx-border-color: #c8d0e8; -fx-border-width: 1; -fx-border-radius: 3; -fx-background-radius: 3; }
        .light .combo-box .list-cell { -fx-text-fill: #212121; -fx-background-color: #ffffff; }
        .light .combo-box-popup .list-view { -fx-background-color: #ffffff; -fx-border-color: #c8d0e8; }
        .light .combo-box-popup .list-cell { -fx-text-fill: #212121; -fx-background-color: #ffffff; }
        .light .combo-box-popup .list-cell:hover { -fx-background-color: #bbdefb; }
        .light .scroll-bar { -fx-background-color: #f4f4f4; }
        .light .scroll-bar .thumb { -fx-background-color: #c8d0e8; -fx-background-radius: 3; }
        .light .scroll-bar .thumb:hover { -fx-background-color: #90caf9; }
        .light .scroll-bar .increment-button, .light .scroll-bar .decrement-button { -fx-background-color: #eef2fb; }
        .light .menu-bar { -fx-background-color: #e8e8e8; }
        .light .menu-bar .label { -fx-text-fill: #212121; }
        .light .menu-bar .menu:hover, .light .menu-bar .menu:showing { -fx-background-color: #dce4f0; }
        .light .context-menu { -fx-background-color: #ffffff; -fx-border-color: #c8d0e8; }
        .light .menu-item .label { -fx-text-fill: #212121; }
        .light .menu-item:hover  { -fx-background-color: #bbdefb; }
        .light .table-view { -fx-background-color: white; -fx-control-inner-background: white; }
        .light .table-view .column-header { -fx-background-color: #c8d8f0; -fx-border-color: #b0c4e0; -fx-border-width: 0 0 1 0; }
        .light .table-view .column-header .label { -fx-text-fill: #212121; -fx-font-weight: bold; }
        .light .table-row-cell { -fx-background-color: white; -fx-text-fill: #212121; }
        .light .table-row-cell:odd { -fx-background-color: #f8faff; }
        .light .table-row-cell:selected { -fx-background-color: #bbdefb; }
        .light .table-cell { -fx-text-fill: #212121; -fx-font-size: 11; }
        .light .dialog-pane { -fx-background-color: #f4f4f4; }
        .light .dialog-pane .label { -fx-text-fill: #212121; }
        .light .dialog-pane .button-bar .button { -fx-background-color: #e0e0e0; -fx-text-fill: #212121; -fx-background-radius: 4; }
        .light .text-field { -fx-background-color: #ffffff; -fx-text-fill: #212121;
                             -fx-border-color: #c8d0e8; -fx-border-width: 1;
                             -fx-border-radius: 3; -fx-background-radius: 3;
                             -fx-prompt-text-fill: #aaaaaa; }
        .light .text-field:focused { -fx-border-color: #1565c0; }
        """;
    }
}
