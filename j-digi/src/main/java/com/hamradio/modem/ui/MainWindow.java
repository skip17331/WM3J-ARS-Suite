package com.hamradio.modem.ui;

import com.hamradio.modem.ModemService;
import com.hamradio.modem.audio.AudioEngine;
import com.hamradio.modem.model.ModeType;
import com.hamradio.modem.model.ModemStatus;
import com.hamradio.modem.tx.AudioTxEngine;
import com.hamradio.modem.tx.TxState;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow {
    private final ModemService service;

    private final Circle hubStatusDot = new Circle(6, Color.RED);
    private final Label hubLabel = new Label("Disconnected");
    private final Label audioLabel = new Label("Stopped");
    private final Label modeLabel = new Label("RTTY");
    private final Label rigFreqLabel = new Label("--");
    private final Label rigModeLabel = new Label("--");
    private final Label rmsLabel = new Label("0.0000");
    private final Label peakLabel = new Label("0.0 Hz");
    private final Label snrLabel = new Label("0.0 dB");
    private final Label offsetLabel = new Label("0.0 Hz");
    private final Label tuningHintLabel = new Label("No signal");
    private final Label logCountLabel = new Label("0 lines");

    private final Label rttyMarkLabel = new Label("0.000");
    private final Label rttySpaceLabel = new Label("0.000");
    private final Label rttyDomLabel = new Label("0.00");
    private final CheckBox rttyReverseBox = new CheckBox("RTTY Reverse");

    private VBox rttyCard;

    private final Circle txStatusDot = new Circle(6, Color.DARKGRAY);
    private final Label txStateLabel = new Label("Idle");
    private final Label txModeLabel = new Label("RTTY");
    private final Label txPreviewLabel = new Label("--");

    private final TextArea decodeArea = new TextArea();
    private final TextArea txTextArea = new TextArea();

    private final ComboBox<ModeType> modeBox = new ComboBox<>();
    private final CheckBox autoScrollBox = new CheckBox("Auto-scroll log");

    private final Button transmitButton = new Button("Transmit");
    private final Button cancelTxButton = new Button("Cancel TX");
    private final Button saveTxWavButton = new Button("Save TX WAV");

    private final SpectrumPane spectrumPane = new SpectrumPane();
    private final WaterfallPane waterfallPane = new WaterfallPane();

    private final Label draftCallsignLabel = new Label("--");
    private final Label draftExchangeLabel = new Label("--");
    private final Label draftBandLabel = new Label("--");
    private final Label draftFreqLabel = new Label("--");

    private String lastNonEmptyDecodeLine = "";
    private ModemStatus lastStatus;

    private static final Pattern CALLSIGN_PATTERN =
            Pattern.compile("\\b([A-Z0-9]{1,3}[0-9][A-Z0-9/]{1,6})\\b");

    public MainWindow(ModemService service) {
        this.service = service;
    }

    public void show(Stage stage) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setTop(buildTopArea(stage));
        root.setCenter(buildMainBody(stage));

        configureControls(stage);
        wireService();

        Scene scene = new Scene(root, 1260, 860);
        stage.setTitle("J-Digi");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.show();
    }

    private void configureControls(Stage stage) {
        decodeArea.setEditable(false);
        decodeArea.setWrapText(false);
        decodeArea.setFont(Font.font("Consolas", 13));
        decodeArea.setPrefRowCount(8);

        txTextArea.setWrapText(true);
        txTextArea.setFont(Font.font("Consolas", 13));
        txTextArea.setPrefRowCount(5);
        txTextArea.setPromptText("Enter text to transmit as RTTY");

        autoScrollBox.setSelected(true);

        modeBox.getItems().addAll(ModeType.values());
        modeBox.setValue(ModeType.RTTY);

        rttyReverseBox.setOnAction(e -> service.setRttyReverse(rttyReverseBox.isSelected()));

        transmitButton.setOnAction(e -> service.transmitText(txTextArea.getText()));
        cancelTxButton.setOnAction(e -> service.cancelTransmit());
        saveTxWavButton.setOnAction(e -> saveTxWav(stage));

        cancelTxButton.setDisable(true);
    }

    private void wireService() {
        service.setSpectrumListener(snapshot -> {
            spectrumPane.update(snapshot);
            waterfallPane.update(snapshot);
            updateTuningStrip(snapshot.getPeakFrequencyHz());
        });

        service.setDecodeListener(this::appendLogLine);
        service.setStatusListener(this::updateStatus);
    }

    private SplitPane buildMainBody(Stage stage) {
        SplitPane horizontal = new SplitPane();
        horizontal.setOrientation(Orientation.HORIZONTAL);

        SplitPane vertical = new SplitPane();
        vertical.setOrientation(Orientation.VERTICAL);

        Pane visualPane = buildVisualPane();
        Pane decodePane = buildDecodePane();

        vertical.getItems().addAll(visualPane, decodePane);
        vertical.setDividerPositions(0.68);

        ScrollPane rightScroll = new ScrollPane(buildRightPane(stage));
        rightScroll.setFitToWidth(true);
        rightScroll.setFitToHeight(true);
        rightScroll.setPannable(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rightScroll.setPrefWidth(345);
        rightScroll.setMinWidth(300);

        horizontal.getItems().addAll(vertical, rightScroll);
        horizontal.setDividerPositions(0.74);

        return horizontal;
    }

    private Pane buildTopArea(Stage stage) {
        VBox box = new VBox(10, buildMenuBar(stage), buildTopBar(), buildSummaryBar());
        return box;
    }

    private MenuBar buildMenuBar(Stage stage) {
        Menu fileMenu = new Menu("File");

        MenuItem setupItem = new MenuItem("Setup");
        setupItem.setOnAction(e -> showSetupDialog());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(setupItem, new SeparatorMenuItem(), exitItem);

        return new MenuBar(fileMenu);
    }

    private Pane buildTopBar() {
        Button startAudioButton = new Button("Start Audio");
        Button stopAudioButton = new Button("Stop Audio");
        Button clearLogButton = new Button("Clear Log");
        Button sendToLogButton = new Button("Send To Log");

        startAudioButton.setOnAction(e -> {
            try {
                service.startAudio();
            } catch (Exception ex) {
                appendLogLine("Audio failed: " + ex.getMessage());
            }
        });

        stopAudioButton.setOnAction(e -> service.stopAudio());

        clearLogButton.setOnAction(e -> {
            decodeArea.clear();
            lastNonEmptyDecodeLine = "";
            clearDraftPreview();
            updateLogCount();
        });

        sendToLogButton.setOnAction(e -> sendCurrentDraftToLog());

        modeBox.setOnAction(e -> {
            service.setMode(modeBox.getValue());
            updateModeControls(modeBox.getValue());
        });

        HBox hubIndicator = new HBox(6, hubStatusDot, new Label("Hub"));
        hubIndicator.setAlignment(Pos.CENTER_LEFT);

        HBox txIndicator = new HBox(6, txStatusDot, new Label("TX"));
        txIndicator.setAlignment(Pos.CENTER_LEFT);

        HBox bar = new HBox(
                10,
                hubIndicator,
                txIndicator,
                new Separator(),
                new Label("Mode:"),
                modeBox,
                new Separator(),
                rttyReverseBox,
                startAudioButton,
                stopAudioButton,
                clearLogButton,
                sendToLogButton
        );
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPrefHeight(36);
        return bar;
    }

    private Pane buildSummaryBar() {
        HBox row = new HBox(
                10,
                buildStatusChip("Hub", hubLabel),
                buildStatusChip("Audio", audioLabel),
                buildStatusChip("Mode", modeLabel),
                buildStatusChip("Rig Freq", rigFreqLabel),
                buildStatusChip("Rig Mode", rigModeLabel),
                buildStatusChip("TX State", txStateLabel)
        );
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox buildStatusChip(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");
        valueLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        VBox box = new VBox(3, titleLabel, valueLabel);
        box.setPadding(new Insets(8));
        box.setMinWidth(110);
        box.setStyle(
                "-fx-background-color: #f4f4f4;" +
                "-fx-border-color: #d0d0d0;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return box;
    }

    private Pane buildVisualPane() {
        VBox center = new VBox(10);

        VBox spectrumBox = new VBox(
                6,
                sectionTitle("Spectrum"),
                spectrumPane,
                buildTuningStrip()
        );

        VBox waterfallBox = new VBox(
                6,
                sectionTitle("Waterfall"),
                waterfallPane
        );

        spectrumPane.setPrefHeight(210);
        spectrumPane.setMinHeight(180);

        waterfallPane.setPrefHeight(260);
        waterfallPane.setMinHeight(180);

        VBox.setVgrow(spectrumPane, Priority.ALWAYS);
        VBox.setVgrow(waterfallPane, Priority.ALWAYS);

        center.getChildren().addAll(spectrumBox, waterfallBox);
        VBox.setVgrow(spectrumBox, Priority.ALWAYS);
        VBox.setVgrow(waterfallBox, Priority.ALWAYS);

        return center;
    }

    private Pane buildTuningStrip() {
        Label offsetTitle = new Label("Audio Peak Offset:");
        offsetTitle.setStyle("-fx-font-weight: bold;");
        offsetLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        Label hintTitle = new Label("Tuning:");
        hintTitle.setStyle("-fx-font-weight: bold;");
        tuningHintLabel.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 13px;");

        HBox box = new HBox(14, offsetTitle, offsetLabel, new Separator(), hintTitle, tuningHintLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(8));
        box.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return box;
    }

    private Pane buildRightPane(Stage stage) {
        VBox pane = new VBox(
                12,
                buildMetricsCard(),
                buildRttyCard(),
                buildTxCard(stage),
                buildDraftCard(),
                buildLogOptionsCard()
        );
        pane.setPadding(new Insets(0, 0, 0, 10));
        pane.setPrefWidth(320);
        return pane;
    }

    private Pane buildMetricsCard() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        addMetricRow(grid, 0, "RMS", rmsLabel);
        addMetricRow(grid, 1, "Peak", peakLabel);
        addMetricRow(grid, 2, "SNR", snrLabel);

        VBox card = new VBox(8, sectionTitle("Live Metrics"), grid);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return card;
    }

    private Pane buildRttyCard() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        addMetricRow(grid, 0, "Mark", rttyMarkLabel);
        addMetricRow(grid, 1, "Space", rttySpaceLabel);
        addMetricRow(grid, 2, "Dominance", rttyDomLabel);

        rttyCard = new VBox(8, sectionTitle("RTTY Diagnostics"), grid);
        rttyCard.setPadding(new Insets(10));
        rttyCard.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return rttyCard;
    }

    private Pane buildTxCard(Stage stage) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        addMetricRow(grid, 0, "State", txStateLabel);
        addMetricRow(grid, 1, "Mode", txModeLabel);
        addMetricRow(grid, 2, "Preview", txPreviewLabel);

        HBox buttonRow = new HBox(10, transmitButton, cancelTxButton, saveTxWavButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(
                8,
                sectionTitle("Transmit"),
                new Label("TX Text"),
                txTextArea,
                buttonRow,
                new Separator(),
                grid
        );
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return card;
    }

    private Pane buildDraftCard() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        addMetricRow(grid, 0, "Callsign", draftCallsignLabel);
        addMetricRow(grid, 1, "Exchange", draftExchangeLabel);
        addMetricRow(grid, 2, "Band", draftBandLabel);
        addMetricRow(grid, 3, "Freq", draftFreqLabel);

        VBox card = new VBox(8, sectionTitle("Log Draft Preview"), grid);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return card;
    }

    private Pane buildLogOptionsCard() {
        VBox card = new VBox(10, sectionTitle("Log View"), autoScrollBox, logCountLabel);
        card.setPadding(new Insets(10));
        card.setStyle(
                "-fx-background-color: #f8f8f8;" +
                "-fx-border-color: #d8d8d8;" +
                "-fx-border-radius: 6;" +
                "-fx-background-radius: 6;"
        );
        return card;
    }

    private void addMetricRow(GridPane grid, int row, String name, Label value) {
        Label key = new Label(name + ":");
        key.setStyle("-fx-font-weight: bold;");
        value.setStyle("-fx-font-family: 'Consolas';");
        value.setMaxWidth(220);
        grid.add(key, 0, row);
        grid.add(value, 1, row);
    }

    private Pane buildDecodePane() {
        VBox box = new VBox(6, sectionTitle("Decoded / Event Log"), decodeArea);
        box.setPadding(new Insets(4, 0, 0, 0));
        decodeArea.setMinHeight(160);
        VBox.setVgrow(decodeArea, Priority.ALWAYS);
        return box;
    }

    private Label sectionTitle(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

    private void appendLogLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }

        decodeArea.appendText(line + System.lineSeparator());
        lastNonEmptyDecodeLine = line;

        updateDraftPreview(line);

        if (autoScrollBox.isSelected()) {
            decodeArea.positionCaret(decodeArea.getText().length());
        }
        updateLogCount();
    }

    private void updateLogCount() {
        String text = decodeArea.getText();
        if (text == null || text.isEmpty()) {
            logCountLabel.setText("0 lines");
            return;
        }

        int lines = text.split("\\R", -1).length;
        if (text.endsWith("\n") || text.endsWith("\r")) {
            lines -= 1;
        }
        if (lines < 0) {
            lines = 0;
        }

        logCountLabel.setText(lines + " line" + (lines == 1 ? "" : "s"));
    }

    private void updateStatus(ModemStatus status) {
        lastStatus = status;

        boolean connected = status.isHubConnected();
        hubStatusDot.setFill(connected ? Color.LIMEGREEN : Color.RED);

        hubLabel.setText(connected ? "Connected" : "Disconnected");
        audioLabel.setText(status.isAudioRunning() ? "Running" : "Stopped");
        modeLabel.setText(String.valueOf(status.getMode()));
        rigFreqLabel.setText(status.getRigFrequencyHz() > 0 ? status.getRigFrequencyHz() + " Hz" : "--");
        rigModeLabel.setText(
                status.getRigMode() != null && !status.getRigMode().isBlank()
                        ? status.getRigMode()
                        : "--"
        );

        rmsLabel.setText("%.4f".formatted(status.getRms()));
        peakLabel.setText("%.1f Hz".formatted(status.getPeakFrequencyHz()));
        snrLabel.setText("%.1f dB".formatted(status.getSnr()));

        rttyMarkLabel.setText("%.3f".formatted(status.getRttyMarkPower()));
        rttySpaceLabel.setText("%.3f".formatted(status.getRttySpacePower()));
        rttyDomLabel.setText("%.2f".formatted(status.getRttyDominance()));

        rttyReverseBox.setSelected(status.isRttyReverse());

        if (modeBox.getValue() != status.getMode()) {
            modeBox.setValue(status.getMode());
            updateModeControls(status.getMode());
        }

        updateTxState(status);

        if (lastNonEmptyDecodeLine != null && !lastNonEmptyDecodeLine.isBlank()) {
            updateDraftPreview(lastNonEmptyDecodeLine);
        }
    }

    private void updateTxState(ModemStatus status) {
        TxState state = status.getTxState();
        if (state == null) {
            state = TxState.IDLE;
        }

        txStateLabel.setText(status.getTxStatusText() != null && !status.getTxStatusText().isBlank()
                ? status.getTxStatusText()
                : state.name());

        txModeLabel.setText(status.getTxMode() != null ? status.getTxMode().name() : "--");

        String preview = status.getTxTextPreview();
        txPreviewLabel.setText(preview != null && !preview.isBlank() ? preview : "--");

        boolean active = status.isTransmitting();
        transmitButton.setDisable(active);
        cancelTxButton.setDisable(!active);
        saveTxWavButton.setDisable(active);
        modeBox.setDisable(active);

        switch (state) {
            case TRANSMITTING -> txStatusDot.setFill(Color.RED);
            case STARTING, STOPPING -> txStatusDot.setFill(Color.ORANGE);
            case COMPLETE -> txStatusDot.setFill(Color.LIMEGREEN);
            case CANCELLED -> txStatusDot.setFill(Color.DARKGRAY);
            case ERROR -> txStatusDot.setFill(Color.DARKRED);
            default -> txStatusDot.setFill(Color.DARKGRAY);
        }
    }

    private void updateModeControls(ModeType mode) {
        boolean isRtty = mode == ModeType.RTTY;
        rttyReverseBox.setDisable(!isRtty);
        if (rttyCard != null) {
            rttyCard.setVisible(isRtty);
            rttyCard.setManaged(isRtty);
        }
        String prompt = switch (mode) {
            case RTTY     -> "Enter text to transmit as RTTY";
            case PSK31    -> "Enter text to transmit as PSK31";
            case CW       -> "Enter text to transmit as CW";
            case OLIVIA   -> "Enter text to transmit as Olivia";
            case MFSK16   -> "Enter text to transmit as MFSK16";
            case DOMINOEX -> "Enter text to transmit as DominoEX";
            case AX25     -> "Enter AX.25 packet text";
        };
        txTextArea.setPromptText(prompt);
    }

    private void updateTuningStrip(double peakHz) {
        offsetLabel.setText("%.1f Hz".formatted(peakHz));

        ModeType mode = modeBox.getValue();
        String hint;

        if (peakHz <= 0.0) {
            hint = "No signal";
        } else if (mode == ModeType.RTTY) {
            hint = rttyHint(peakHz);
        } else if (mode == ModeType.PSK31) {
            hint = pskHint(peakHz);
        } else if (mode == ModeType.AX25) {
            hint = ax25Hint(peakHz);
        } else if (mode == ModeType.CW) {
            hint = cwHint(peakHz);
        } else if (mode == ModeType.OLIVIA) {
            hint = oliviaHint(peakHz);
        } else if (mode == ModeType.MFSK16) {
            hint = mfsk16Hint(peakHz);
        } else if (mode == ModeType.DOMINOEX) {
            hint = dominoExHint(peakHz);
        } else {
            hint = "Monitor signal";
        }

        tuningHintLabel.setText(hint);
        spectrumPane.setStatusMode(mode);
        spectrumPane.setPeakFrequencyHz(peakHz);
    }

    private String rttyHint(double peakHz) {
        double markError = Math.abs(peakHz - 2125.0);
        double spaceError = Math.abs(peakHz - 2295.0);
        double nearest = Math.min(markError, spaceError);

        if (nearest < 25.0) return "Well tuned";
        if (nearest < 80.0) return "Close - fine tune";
        return "Far off expected RTTY tones";
    }

    private String pskHint(double peakHz) {
        if (peakHz >= 400.0 && peakHz <= 2500.0) return "Usable PSK audio range";
        return "Peak outside normal PSK audio range";
    }

    private String ax25Hint(double peakHz) {
        double e1 = Math.abs(peakHz - 1200.0);
        double e2 = Math.abs(peakHz - 2200.0);
        double nearest = Math.min(e1, e2);

        if (nearest < 40.0) return "Near Bell 202 tones";
        if (nearest < 120.0) return "Close to AX.25 tones";
        return "Far from Bell 202 tones";
    }

    private String cwHint(double peakHz) {
        double error = Math.abs(peakHz - 700.0);
        if (error < 30.0) return "Near CW carrier (700 Hz)";
        if (error < 100.0) return "Close - fine tune to 700 Hz";
        if (peakHz >= 300.0 && peakHz <= 1500.0) return "Signal present - tune to 700 Hz";
        return "Peak outside CW audio range";
    }

    private String oliviaHint(double peakHz) {
        double error = Math.abs(peakHz - 1500.0);
        if (error < 50.0) return "Well centered for Olivia";
        if (error < 200.0) return "Close to Olivia center (1500 Hz)";
        return "Center signal on 1500 Hz for Olivia";
    }

    private String mfsk16Hint(double peakHz) {
        if (peakHz >= 1350.0 && peakHz <= 1680.0) return "Within MFSK16 tone range";
        double error = Math.abs(peakHz - 1500.0);
        if (error < 200.0) return "Near MFSK16 range - center on 1500 Hz";
        return "Center signal on 1500 Hz for MFSK16";
    }

    private String dominoExHint(double peakHz) {
        double error = Math.abs(peakHz - 1500.0);
        if (error < 80.0) return "Well centered for DominoEX";
        if (error < 200.0) return "Close to DominoEX center (1500 Hz)";
        return "Center signal on 1500 Hz for DominoEX";
    }

    private void showSetupDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Setup");
        dialog.setHeaderText("Modem Setup");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField hubUrlField = new TextField(service.getHubUrl());
        hubUrlField.setPrefWidth(420);

        List<AudioEngine.AudioInputDevice> inputDevices = service.getAvailableAudioInputDevices();
        ComboBox<AudioEngine.AudioInputDevice> audioInputBox =
                new ComboBox<>(FXCollections.observableArrayList(inputDevices));
        audioInputBox.setPrefWidth(420);

        String selectedInputId = service.getSelectedAudioInputDevice();
        for (AudioEngine.AudioInputDevice device : inputDevices) {
            if (device.id().equals(selectedInputId)) {
                audioInputBox.setValue(device);
                break;
            }
        }
        if (audioInputBox.getValue() == null && !inputDevices.isEmpty()) {
            audioInputBox.setValue(inputDevices.get(0));
        }

        List<AudioTxEngine.AudioOutputDevice> outputDevices = service.getAvailableAudioOutputDevices();
        ComboBox<AudioTxEngine.AudioOutputDevice> audioOutputBox =
                new ComboBox<>(FXCollections.observableArrayList(outputDevices));
        audioOutputBox.setPrefWidth(420);

        String selectedOutputId = service.getSelectedAudioOutputDevice();
        for (AudioTxEngine.AudioOutputDevice device : outputDevices) {
            if (device.id().equals(selectedOutputId)) {
                audioOutputBox.setValue(device);
                break;
            }
        }
        if (audioOutputBox.getValue() == null && !outputDevices.isEmpty()) {
            audioOutputBox.setValue(outputDevices.get(0));
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Hub WebSocket URL:"), 0, 0);
        grid.add(hubUrlField, 1, 0);

        grid.add(new Label("Audio Input Device:"), 0, 1);
        grid.add(audioInputBox, 1, 1);

        grid.add(new Label("TX Output Device:"), 0, 2);
        grid.add(audioOutputBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                service.reconnectHubFromSetup(hubUrlField.getText().trim());

                AudioEngine.AudioInputDevice selectedInput = audioInputBox.getValue();
                if (selectedInput != null) {
                    service.setAudioInputDevice(selectedInput.id());
                }

                AudioTxEngine.AudioOutputDevice selectedOutput = audioOutputBox.getValue();
                if (selectedOutput != null) {
                    service.setAudioOutputDevice(selectedOutput.id());
                }
            }
        });
    }

    private void saveTxWav(Stage stage) {
        String text = txTextArea.getText();
        if (text == null || text.isBlank()) {
            appendLogLine("No TX text entered");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save TX WAV");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("WAV Audio (*.wav)", "*.wav")
        );
        chooser.setInitialFileName(defaultTxFilename());

        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            service.saveTransmitWav(text, file.toPath());
        } catch (Exception e) {
            appendLogLine("Save TX WAV failed: " + e.getMessage());
        }
    }

    private String defaultTxFilename() {
        String mode = modeBox.getValue() != null ? modeBox.getValue().name().toLowerCase() : "tx";
        return mode + "_tx_test.wav";
    }

    private void sendCurrentDraftToLog() {
        String selected = decodeArea.getSelectedText();
        String sourceLine = (selected != null && !selected.isBlank())
                ? selected.trim()
                : lastNonEmptyDecodeLine;

        if (sourceLine == null || sourceLine.isBlank()) {
            appendLogLine("No decode text available to send");
            return;
        }

        String callsign = extractCallsign(sourceLine);
        String exchange = extractExchange(sourceLine, callsign);
        String mode = modeBox.getValue() != null ? modeBox.getValue().name() : "RTTY";

        long frequency = (lastStatus != null) ? lastStatus.getRigFrequencyHz() : 0L;
        String band = frequency > 0 ? bandFromFrequencyHz(frequency) : "";
        double confidence = 0.0;

        service.sendLogDraft(
                callsign,
                mode,
                band,
                frequency,
                "599",
                "599",
                exchange,
                sourceLine,
                confidence
        );

        appendLogLine("Sent log draft: " +
                (callsign.isBlank() ? "<no callsign>" : callsign) +
                (exchange.isBlank() ? "" : " / " + exchange));
    }

    private void updateDraftPreview(String line) {
        if (line == null || line.isBlank()) {
            clearDraftPreview();
            return;
        }

        String callsign = extractCallsign(line);
        String exchange = extractExchange(line, callsign);
        long frequency = lastStatus != null ? lastStatus.getRigFrequencyHz() : 0L;
        String band = frequency > 0 ? bandFromFrequencyHz(frequency) : "";

        draftCallsignLabel.setText(callsign.isBlank() ? "--" : callsign);
        draftExchangeLabel.setText(exchange.isBlank() ? "--" : exchange);
        draftBandLabel.setText(band.isBlank() ? "--" : band);
        draftFreqLabel.setText(frequency > 0 ? String.valueOf(frequency) : "--");
    }

    private void clearDraftPreview() {
        draftCallsignLabel.setText("--");
        draftExchangeLabel.setText("--");
        draftBandLabel.setText("--");
        draftFreqLabel.setText("--");
    }

    private String extractCallsign(String text) {
        String upper = text.toUpperCase();
        Matcher m = CALLSIGN_PATTERN.matcher(upper);

        while (m.find()) {
            String candidate = m.group(1);
            if (looksLikeCallsign(candidate)) {
                return candidate;
            }
        }
        return "";
    }

    private boolean looksLikeCallsign(String candidate) {
        if (candidate == null || candidate.length() < 3) return false;
        return candidate.chars().anyMatch(Character::isDigit)
                && candidate.chars().anyMatch(Character::isLetter);
    }

    private String extractExchange(String text, String callsign) {
        if (text == null || text.isBlank()) return "";
        String upper = text.toUpperCase();

        if (callsign != null && !callsign.isBlank()) {
            upper = upper.replace(callsign, " ");
        }

        upper = upper.replaceAll("\\b(CQ|DE|TEST|QRZ|K)\\b", " ");
        upper = upper.replaceAll("\\s+", " ").trim();

        return upper;
    }

    private String bandFromFrequencyHz(long hz) {
        long khz = hz / 1000;
        if (khz >= 1800 && khz <= 2000) return "160m";
        if (khz >= 3500 && khz <= 4000) return "80m";
        if (khz >= 5330 && khz <= 5410) return "60m";
        if (khz >= 7000 && khz <= 7300) return "40m";
        if (khz >= 10100 && khz <= 10150) return "30m";
        if (khz >= 14000 && khz <= 14350) return "20m";
        if (khz >= 18068 && khz <= 18168) return "17m";
        if (khz >= 21000 && khz <= 21450) return "15m";
        if (khz >= 24890 && khz <= 24990) return "12m";
        if (khz >= 28000 && khz <= 29700) return "10m";
        if (khz >= 50000 && khz <= 54000) return "6m";
        if (khz >= 144000 && khz <= 148000) return "2m";
        if (khz >= 420000 && khz <= 450000) return "70cm";
        return "";
    }
}
