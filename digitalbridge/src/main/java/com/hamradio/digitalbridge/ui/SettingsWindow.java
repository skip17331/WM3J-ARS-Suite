package com.hamradio.digitalbridge.ui;

import com.hamradio.digitalbridge.ConfigManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * SettingsWindow — modal settings dialog for Digital Bridge.
 *
 * Sections:
 *   Hub       — j-hub WebSocket address and port
 *   WSJT-X    — UDP port and bind address
 *   Display   — history length, SNR filter, auto-scroll, CQ-only, band filters
 *
 * Visual style: Catppuccin Mocha dark, matching j-hub HubStatusWindow.
 */
public class SettingsWindow {

    private static final String[] ALL_BANDS =
        {"160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m"};

    private final Stage stage;

    // Hub
    private final TextField hubAddressFld = tf();
    private final TextField hubPortFld    = tf();

    // WSJT-X
    private final TextField wsjtxPortFld  = tf();
    private final TextField wsjtxBindFld  = tf();

    // Display
    private final TextField  historyFld   = tf();
    private final TextField  minSnrFld    = tf();
    private final CheckBox   autoScrollChk = chk("Auto-scroll to newest decode");
    private final CheckBox   cqOnlyChk    = chk("Show CQ calls only");
    private final Map<String, CheckBox> bandChks = new LinkedHashMap<>();

    private Runnable onSettingsChanged;

    public SettingsWindow(Window owner) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("Digital Bridge — Settings");
        stage.setResizable(false);

        loadCurrentValues();
        buildUI();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadCurrentValues() {
        ConfigManager cfg = ConfigManager.getInstance();
        hubAddressFld.setText(cfg.getHubAddress());
        hubPortFld   .setText(String.valueOf(cfg.getHubPort()));
        wsjtxPortFld .setText(String.valueOf(cfg.getWsjtxUdpPort()));
        wsjtxBindFld .setText(cfg.getWsjtxBindAddress());
        historyFld   .setText(String.valueOf(cfg.getDecodeHistoryLength()));
        minSnrFld    .setText(String.valueOf(cfg.getMinimumSnr()));
        autoScrollChk.setSelected(cfg.isAutoScroll());
        cqOnlyChk    .setSelected(cfg.isShowCQOnly());

        Set<String> active = new HashSet<>(Arrays.asList(cfg.getBandFilters()));
        for (String b : ALL_BANDS) {
            CheckBox cb = chk(b);
            cb.setSelected(active.contains(b));
            bandChks.put(b, cb);
        }
    }

    // ── Build ─────────────────────────────────────────────────────────────────

    private void buildUI() {
        VBox content = new VBox(10,
            section("j-Hub Connection",
                row("Address:", hubAddressFld),
                row("Port:",    hubPortFld)),
            section("WSJT-X UDP",
                row("UDP Port:",     wsjtxPortFld),
                row("Bind Address:", wsjtxBindFld)),
            section("Display",
                row("History Length:", historyFld),
                row("Min SNR (dB):",   minSnrFld),
                autoScrollChk,
                cqOnlyChk,
                bandFilterPane()));

        content.setPadding(new Insets(14));
        content.setStyle("-fx-background-color: #1e1e2e;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(460);
        scroll.setStyle("-fx-background: #1e1e2e; -fx-background-color: #1e1e2e;");

        Button okBtn     = btn("OK",     "#a6e3a1", "#1e1e2e");
        Button cancelBtn = btn("Cancel", "#313244", "#cdd6f4");

        okBtn.setOnAction(e -> { if (save()) { stage.close(); if (onSettingsChanged != null) onSettingsChanged.run(); } });
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(8, cancelBtn, okBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 14, 10, 14));
        buttons.setStyle("-fx-background-color: #1e1e2e;");

        VBox root = new VBox(0, scroll, buttons);
        root.setStyle("-fx-background-color: #1e1e2e;");

        stage.setScene(new Scene(root, 430, 570));
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private boolean save() {
        try {
            String address = hubAddressFld.getText().trim();
            int    hubPort = Integer.parseInt(hubPortFld.getText().trim());
            int    udpPort = Integer.parseInt(wsjtxPortFld.getText().trim());
            String bind    = wsjtxBindFld.getText().trim();
            int    history = Integer.parseInt(historyFld.getText().trim());
            int    minSnr  = Integer.parseInt(minSnrFld.getText().trim());

            if (address.isEmpty()) throw new IllegalArgumentException("Hub address cannot be empty");
            if (hubPort < 1 || hubPort > 65535) throw new IllegalArgumentException("Hub port must be 1–65535");
            if (udpPort < 1 || udpPort > 65535) throw new IllegalArgumentException("UDP port must be 1–65535");
            if (history < 10 || history > 5000) throw new IllegalArgumentException("History must be 10–5000");

            ConfigManager cfg = ConfigManager.getInstance();
            cfg.setHubAddress(address);
            cfg.setHubPort(hubPort);
            cfg.setWsjtxUdpPort(udpPort);
            cfg.setWsjtxBindAddress(bind);
            cfg.setDecodeHistoryLength(history);
            cfg.setMinimumSnr(minSnr);
            cfg.setAutoScroll(autoScrollChk.isSelected());
            cfg.setShowCQOnly(cqOnlyChk.isSelected());
            cfg.setBandFilters(bandChks.entrySet().stream()
                    .filter(e -> e.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new));
            cfg.save();
            return true;

        } catch (NumberFormatException e) {
            alert("Invalid input", "Port and length fields must be integers.");
        } catch (IllegalArgumentException e) {
            alert("Validation error", e.getMessage());
        }
        return false;
    }

    // ── Public ────────────────────────────────────────────────────────────────

    public void show()                              { stage.show(); }
    public void setOnSettingsChanged(Runnable cb)   { this.onSettingsChanged = cb; }

    // ── Layout helpers ────────────────────────────────────────────────────────

    private VBox section(String title, javafx.scene.Node... children) {
        Label hdr = new Label(title);
        hdr.setStyle("-fx-text-fill: #cba6f7; -fx-font-size: 12px; -fx-font-weight: bold;");
        VBox box = new VBox(5, hdr);
        box.getChildren().addAll(children);
        box.setStyle("-fx-background-color: #181825; -fx-padding: 10; " +
                     "-fx-border-color: #313244; -fx-border-width: 1;");
        return box;
    }

    private HBox row(String label, javafx.scene.Node control) {
        Label lbl = new Label(label);
        lbl.setMinWidth(115);
        lbl.setStyle("-fx-text-fill: #a6adc8; -fx-font-size: 11px;");
        HBox row = new HBox(8, lbl, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private FlowPane bandFilterPane() {
        FlowPane fp = new FlowPane(6, 4);
        fp.getChildren().addAll(bandChks.values());
        return fp;
    }

    private TextField tf() {
        TextField f = new TextField();
        f.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                   "-fx-border-color: #45475a; -fx-font-size: 11px;");
        f.setPrefWidth(200);
        return f;
    }

    private CheckBox chk(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
        return cb;
    }

    private Button btn(String text, String bg, String fg) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg +
                   "; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 5 16 5 16;");
        return b;
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(stage);
        a.showAndWait();
    }
}
