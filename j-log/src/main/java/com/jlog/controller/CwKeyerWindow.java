package com.jlog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jlog.app.JLogApp;
import com.jlog.cluster.HubEngine;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Transmit/receive panel for the j-digi modem. j-log doesn't generate audio
 * itself — it sends MODEM_TX messages to j-digi via j-hub, and surfaces
 * decoded MODEM_DECODE text as it arrives. Works for CW, RTTY, PSK31, FT8,
 * and any other mode j-digi supports (mode switched per-transmit).
 *
 * The pane is a separate non-modal window opened from the Normal Log menu;
 * close the window to stop listening. Open/close is idempotent — the same
 * HubEngine listener registration is replaced each time the window opens.
 */
public class CwKeyerWindow {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Stage    owner;
    private final Stage    dialog = new Stage();
    private final TextArea rxArea = new TextArea();
    private final TextArea txArea = new TextArea();
    private final ComboBox<String> modeBox = new ComboBox<>();
    private final Label    status = new Label("ready");

    public CwKeyerWindow(Stage owner) { this.owner = owner; }

    public void show() {
        dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);
        dialog.setTitle("CW / Digital Keyer");

        // Mode selector — values match j-digi's ModeType enum.
        modeBox.setItems(FXCollections.observableArrayList(
            "CW", "RTTY", "PSK31", "PSK63", "FT8", "FT4"));
        modeBox.getSelectionModel().select("CW");
        modeBox.setPrefWidth(100);

        // ---------- RX area ----------
        rxArea.setEditable(false);
        rxArea.setWrapText(true);
        rxArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        rxArea.setPrefRowCount(12);
        Button clearRx = new Button("Clear");
        clearRx.setOnAction(e -> rxArea.clear());
        HBox rxBar = new HBox(8, new Label("Received:"), clearRx);
        rxBar.setAlignment(Pos.CENTER_LEFT);

        // ---------- TX area ----------
        txArea.setWrapText(true);
        txArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        txArea.setPrefRowCount(4);
        txArea.setPromptText("Type and press Send (or Ctrl+Enter)");

        Button sendBtn = new Button("Send");
        sendBtn.setDefaultButton(true);
        sendBtn.setOnAction(e -> doSend());

        Button clearTx = new Button("Clear");
        clearTx.setOnAction(e -> txArea.clear());

        Label modeLbl = new Label("Mode:");
        HBox txBar = new HBox(8, new Label("Transmit:"), modeLbl, modeBox, sendBtn, clearTx, status);
        txBar.setAlignment(Pos.CENTER_LEFT);

        // Ctrl+Enter in txArea = Send
        txArea.setOnKeyPressed(ev -> {
            if (ev.isControlDown() && ev.getCode() == javafx.scene.input.KeyCode.ENTER) {
                doSend();
                ev.consume();
            }
        });

        VBox root = new VBox(6, rxBar, rxArea, txBar, txArea);
        root.setPadding(new Insets(12));
        VBox.setVgrow(rxArea, Priority.ALWAYS);

        Scene scene = new Scene(root, 640, 460);
        JLogApp.applyTheme(scene);
        dialog.setScene(scene);

        // Hook MODEM_DECODE listener — overwrites any prior binding, so
        // reopening the window simply points the stream at the new instance.
        HubEngine.getInstance().setModemDecodeListener(node ->
            Platform.runLater(() -> onDecode(node)));
        dialog.setOnCloseRequest(e -> HubEngine.getInstance().setModemDecodeListener(null));

        dialog.show();
    }

    private void doSend() {
        String text = txArea.getText();
        if (text == null || text.isBlank()) return;
        String mode = modeBox.getValue();
        HubEngine.getInstance().sendModemTx(mode, text);
        String line = String.format("[%s TX %s] %s%n",
            LocalTime.now().format(TIME_FMT), mode, text.trim());
        rxArea.appendText(line);
        status.setText("sent " + text.length() + " chars");
        txArea.clear();
        txArea.requestFocus();
    }

    private void onDecode(JsonNode node) {
        String text = node.path("text").asText("");
        String mode = node.path("mode").asText("");
        double snr  = node.path("snr").asDouble(Double.NaN);
        if (text == null || text.isBlank()) return;
        String prefix = String.format("[%s RX %s%s] ",
            LocalTime.now().format(TIME_FMT),
            mode.isBlank() ? "?" : mode,
            Double.isNaN(snr) ? "" : String.format(" %+.1fdB", snr));
        rxArea.appendText(prefix + text + "\n");
        // Auto-scroll
        rxArea.setScrollTop(Double.MAX_VALUE);
    }
}
