package com.hamradio.digitalbridge.ui;

import com.hamradio.digitalbridge.model.BandActivity;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.LinkedHashMap;
import java.util.Map;

// ══════════════════════════════════════════════════════════════════════════════
// BandActivityPanel
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Side panel showing decode counts and best DX per band.
 * Colour palette matches j-hub HubStatusWindow (Catppuccin Mocha).
 */
class BandActivityPanel extends VBox {

    private static final String[] BANDS = {
        "160m","80m","40m","30m","20m","17m","15m","12m","10m","6m","2m"
    };

    private final Map<String, Label[]> rows = new LinkedHashMap<>();

    BandActivityPanel() {
        setSpacing(0);
        setPadding(new Insets(4));
        setStyle("-fx-background-color: #181825;");

        // Header row
        HBox hdr = rowBox(lbl("Band",50,true), lbl("Spots",42,true), lbl("Top DX",70,true));
        hdr.setStyle("-fx-background-color: #313244; -fx-padding: 3 2 3 2;");
        getChildren().add(hdr);

        for (int i = 0; i < BANDS.length; i++) {
            String band = BANDS[i];
            Label spotsL = lbl("0",  42, false);
            Label topL   = lbl("-",  70, false);
            rows.put(band, new Label[]{spotsL, topL});

            HBox row = rowBox(lbl(band, 50, false), spotsL, topL);
            row.setStyle(i % 2 == 0
                    ? "-fx-background-color: #1e1e2e; -fx-padding: 2 2 2 2;"
                    : "-fx-background-color: #181825; -fx-padding: 2 2 2 2;");
            getChildren().add(row);
        }
    }

    void update(BandActivity ba) {
        Label[] lbls = rows.get(ba.getBand());
        if (lbls == null) return;
        lbls[0].setText(String.valueOf(ba.getSpotCount()));
        lbls[1].setText(ba.getTopDxCall());
    }

    void reset() {
        rows.values().forEach(l -> { l[0].setText("0"); l[1].setText("-"); });
    }

    private HBox rowBox(Label... labels) {
        HBox b = new HBox(); b.getChildren().addAll(labels); return b;
    }

    private Label lbl(String text, double w, boolean header) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setStyle(header
                ? "-fx-text-fill: #cba6f7; -fx-font-size: 11px; -fx-font-weight: bold;"
                : "-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WsjtxStatusPanel
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Panel showing WSJT-X connection state, version, frequency, mode, TX/RX.
 * Matches the visual style of j-hub HubStatusWindow.
 */
class WsjtxStatusPanel extends VBox {

    private final Circle indicator  = new Circle(5);
    private final Label  statusLbl  = val("Disconnected");
    private final Label  versionLbl = val("-");
    private final Label  freqLbl    = val("-");
    private final Label  modeLbl    = val("-");
    private final Label  txLbl      = val("RX");
    private final Label  decodeLbl  = val("-");
    private final Label  portLbl;

    WsjtxStatusPanel(int udpPort) {
        portLbl = val(String.valueOf(udpPort));
        setSpacing(0);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #181825; -fx-border-color: #313244; " +
                 "-fx-border-width: 0 0 1 0;");

        Label hdr = new Label("WSJT-X");
        hdr.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 12px; -fx-font-weight: bold;");

        indicator.setFill(Color.web("#6c7086"));
        HBox statusRow = new HBox(5, indicator, statusLbl);
        statusRow.setStyle("-fx-alignment: center-left;");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 0, 0));

        int r = 0;
        grid.add(key("Status:"),  0,r); grid.add(statusRow,  1,r++);
        grid.add(key("Version:"), 0,r); grid.add(versionLbl, 1,r++);
        grid.add(key("Freq:"),    0,r); grid.add(freqLbl,    1,r++);
        grid.add(key("Mode:"),    0,r); grid.add(modeLbl,    1,r++);
        grid.add(key("TX/RX:"),   0,r); grid.add(txLbl,      1,r++);
        grid.add(key("Decode:"),  0,r); grid.add(decodeLbl,  1,r++);
        grid.add(key("UDP:"),     0,r); grid.add(portLbl,    1,r++);

        getChildren().addAll(hdr, grid);
    }

    void setConnected(boolean connected, String version) {
        if (connected) {
            indicator.setFill(Color.web("#a6e3a1"));
            statusLbl.setText("Connected");
            versionLbl.setText(version != null ? version : "?");
        } else {
            indicator.setFill(Color.web("#6c7086"));
            statusLbl.setText("Disconnected");
            versionLbl.setText("-");
            freqLbl.setText("-"); modeLbl.setText("-");
            txLbl.setText("RX"); txLbl.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
            decodeLbl.setText("-");
        }
    }

    void setFrequency(long hz)      { freqLbl.setText(hz > 0 ? String.format("%.3f MHz", hz/1_000_000.0) : "-"); }
    void setMode(String mode)       { modeLbl.setText(mode != null ? mode : "-"); }
    void setTransmitting(boolean tx){
        txLbl.setText(tx ? "TX" : "RX");
        txLbl.setStyle(tx
                ? "-fx-text-fill: #f38ba8; -fx-font-weight: bold;"
                : "-fx-text-fill: #a6e3a1;");
    }
    void setDecoding(boolean dec)   {
        decodeLbl.setText(dec ? "Decoding…" : "Idle");
        decodeLbl.setStyle(dec
                ? "-fx-text-fill: #f9e2af;"
                : "-fx-text-fill: #6c7086;");
    }

    private Label key(String t) {
        Label l = new Label(t);
        l.setMinWidth(60);
        l.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        return l;
    }
    private Label val(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// HubStatusPanel
// ══════════════════════════════════════════════════════════════════════════════

/**
 * Panel showing j-hub connection state, message counters, reconnect button.
 */
class HubStatusPanel extends VBox {

    private final Circle indicator   = new Circle(5);
    private final Label  statusLbl   = val("Disconnected");
    private final Label  addressLbl  = val("-");
    private final Label  sentLbl     = val("0");
    private final Label  recvLbl     = val("0");
    private final Button reconnectBtn;

    private Runnable onReconnect;

    HubStatusPanel(String address, int port) {
        setSpacing(0);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: #181825; -fx-border-color: #313244; " +
                 "-fx-border-width: 0 0 1 0;");

        Label hdr = new Label("j-Hub");
        hdr.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 12px; -fx-font-weight: bold;");

        addressLbl.setText(address + ":" + port);
        indicator.setFill(Color.web("#6c7086"));

        HBox statusRow = new HBox(5, indicator, statusLbl);
        statusRow.setStyle("-fx-alignment: center-left;");

        reconnectBtn = new Button("Reconnect");
        reconnectBtn.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                              "-fx-font-size: 11px; -fx-cursor: hand;");
        reconnectBtn.setPrefHeight(22);
        reconnectBtn.setOnAction(e -> { if (onReconnect != null) onReconnect.run(); });

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 0, 0));

        int r = 0;
        grid.add(key("Status:"),  0,r); grid.add(statusRow,    1,r++);
        grid.add(key("Address:"), 0,r); grid.add(addressLbl,   1,r++);
        grid.add(key("Sent:"),    0,r); grid.add(sentLbl,      1,r++);
        grid.add(key("Rcvd:"),    0,r); grid.add(recvLbl,      1,r++);
        grid.add(reconnectBtn,    0,r,2,1);

        getChildren().addAll(hdr, grid);
    }

    void setConnected(boolean connected, String detail) {
        if (connected) {
            indicator.setFill(Color.web("#a6e3a1"));
            statusLbl.setText("Connected");
            if (detail != null && !detail.isBlank()) addressLbl.setText(detail);
        } else {
            indicator.setFill(Color.web("#6c7086"));
            statusLbl.setText("Disconnected");
        }
    }

    void setSentCount(long n)   { sentLbl.setText(String.valueOf(n)); }
    void setRecvCount(long n)   { recvLbl.setText(String.valueOf(n)); }
    void setOnReconnect(Runnable r) { this.onReconnect = r; }

    private Label key(String t) {
        Label l = new Label(t);
        l.setMinWidth(60);
        l.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        return l;
    }
    private Label val(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Package-level export helper — makes the three panels accessible from MainWindow
// without exposing them as public top-level classes in separate files.
// ══════════════════════════════════════════════════════════════════════════════

/**
 * StatusPanels — factory exposing the three sidebar panels.
 * All three panels are package-private; only MainWindow uses them.
 */
public class StatusPanels {
    private StatusPanels() {}

    public static BandActivityPanel  newBandPanel()                      { return new BandActivityPanel(); }
    public static WsjtxStatusPanel   newWsjtxPanel(int udpPort)          { return new WsjtxStatusPanel(udpPort); }
    public static HubStatusPanel     newHubPanel(String addr, int port)  { return new HubStatusPanel(addr, port); }
}
