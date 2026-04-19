package com.hamradio.jbridge.ui;

import com.hamradio.jbridge.ConfigManager;
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
 * SettingsWindow — modal settings dialog for J-Bridge.
 * Sections: Hub, WSJT-X, Display.
 * Theme matches MainWindow (reads MainWindow.isDark).
 */
public class SettingsWindow {

    private static final String[] ALL_BANDS =
        {"160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m"};

    private final Stage stage;

    private final TextField hubAddressFld = tf();
    private final TextField hubPortFld    = tf();
    private final TextField wsjtxPortFld  = tf();
    private final TextField wsjtxBindFld  = tf();
    private final TextField historyFld    = tf();
    private final TextField minSnrFld     = tf();
    private final CheckBox  autoScrollChk = new CheckBox("Auto-scroll to newest decode");
    private final CheckBox  cqOnlyChk     = new CheckBox("Show CQ calls only");
    private final Map<String, CheckBox> bandChks = new LinkedHashMap<>();

    private Runnable onSettingsChanged;

    public SettingsWindow(Window owner) {
        stage = new Stage();
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initOwner(owner);
        stage.setTitle("J-Bridge \u2014 Settings");
        stage.setResizable(false);

        loadCurrentValues();
        buildUI();
    }

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
            CheckBox cb = new CheckBox(b);
            cb.setSelected(active.contains(b));
            bandChks.put(b, cb);
        }
    }

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

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(460);

        Button okBtn     = new Button("OK");
        Button cancelBtn = new Button("Cancel");
        okBtn    .getStyleClass().add("jb-ok-btn");
        cancelBtn.getStyleClass().add("jb-cancel-btn");

        okBtn.setOnAction(e -> {
            if (save()) { stage.close(); if (onSettingsChanged != null) onSettingsChanged.run(); }
        });
        cancelBtn.setOnAction(e -> stage.close());

        HBox buttons = new HBox(8, cancelBtn, okBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(8, 14, 10, 14));

        VBox root = new VBox(0, scroll, buttons);

        Scene sc = new Scene(root, 430, 570);
        java.net.URL css = getClass().getResource("/css/j-bridge.css");
        if (css != null) sc.getStylesheets().add(css.toExternalForm());
        sc.getRoot().getStyleClass().add(MainWindow.isDark ? "dark" : "light");

        stage.setScene(sc);
    }

    private boolean save() {
        try {
            String address = hubAddressFld.getText().trim();
            int    hubPort = Integer.parseInt(hubPortFld.getText().trim());
            int    udpPort = Integer.parseInt(wsjtxPortFld.getText().trim());
            String bind    = wsjtxBindFld.getText().trim();
            int    history = Integer.parseInt(historyFld.getText().trim());
            int    minSnr  = Integer.parseInt(minSnrFld.getText().trim());

            if (address.isEmpty()) throw new IllegalArgumentException("Hub address cannot be empty");
            if (hubPort < 1 || hubPort > 65535) throw new IllegalArgumentException("Hub port must be 1\u201365535");
            if (udpPort < 1 || udpPort > 65535) throw new IllegalArgumentException("UDP port must be 1\u201365535");
            if (history < 10 || history > 5000) throw new IllegalArgumentException("History must be 10\u20135000");

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

    public void show()                            { stage.show(); }
    public void setOnSettingsChanged(Runnable cb) { this.onSettingsChanged = cb; }

    private VBox section(String title, javafx.scene.Node... children) {
        Label hdr = new Label(title);
        hdr.getStyleClass().add("jb-section-hdr");
        VBox box = new VBox(5, hdr);
        box.getChildren().addAll(children);
        box.getStyleClass().add("jb-section-card");
        return box;
    }

    private HBox row(String label, javafx.scene.Node control) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("jb-row-lbl");
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
        f.setPrefWidth(200);
        return f;
    }

    private void alert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.initOwner(stage);
        a.showAndWait();
    }
}
