package com.hamradio.jbridge.ui;

import com.hamradio.jbridge.model.BandActivity;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.Map;

// ══════════════════════════════════════════════════════════════════════════════
// BandActivityPanel
// ══════════════════════════════════════════════════════════════════════════════

class BandActivityPanel extends VBox {

    private static final String[] BANDS = {
        "160m","80m","40m","30m","20m","17m","15m","12m","10m","6m","2m"
    };

    private final Map<String, Label[]> rows = new LinkedHashMap<>();

    BandActivityPanel() {
        setSpacing(0);
        getStyleClass().add("jb-band-panel");

        HBox hdr = rowBox(lbl("Band", 50, true), lbl("Spots", 42, true), lbl("Top DX", 70, true));
        hdr.getStyleClass().add("jb-band-hdr-row");
        getChildren().add(hdr);

        for (int i = 0; i < BANDS.length; i++) {
            String band = BANDS[i];
            Label spotsL = lbl("0",  42, false);
            Label topL   = lbl("-",  70, false);
            rows.put(band, new Label[]{spotsL, topL});

            HBox row = rowBox(lbl(band, 50, false), spotsL, topL);
            row.getStyleClass().addAll("jb-band-row", i % 2 == 0 ? "jb-band-row-even" : "jb-band-row-odd");
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
        HBox b = new HBox();
        b.getChildren().addAll(labels);
        return b;
    }

    private Label lbl(String text, double w, boolean header) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.getStyleClass().add(header ? "jb-band-hdr-lbl" : "jb-band-data-lbl");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// WsjtxStatusPanel
// ══════════════════════════════════════════════════════════════════════════════

class WsjtxStatusPanel extends VBox {

    private final Region indicator  = new Region();
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
        getStyleClass().add("jb-status-panel");

        Label hdr = new Label("WSJT-X");
        hdr.getStyleClass().add("jb-panel-hdr");

        indicator.getStyleClass().add("jb-indicator");
        HBox statusRow = new HBox(5, indicator, statusLbl);
        statusRow.setStyle("-fx-alignment: center-left;");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 0, 0));

        int r = 0;
        grid.add(key("Status:"),  0, r); grid.add(statusRow,  1, r++);
        grid.add(key("Version:"), 0, r); grid.add(versionLbl, 1, r++);
        grid.add(key("Freq:"),    0, r); grid.add(freqLbl,    1, r++);
        grid.add(key("Mode:"),    0, r); grid.add(modeLbl,    1, r++);
        grid.add(key("TX/RX:"),   0, r); grid.add(txLbl,      1, r++);
        grid.add(key("Decode:"),  0, r); grid.add(decodeLbl,  1, r++);
        grid.add(key("UDP:"),     0, r); grid.add(portLbl,    1, r++);

        txLbl.getStyleClass().add("tx-idle");
        decodeLbl.getStyleClass().add("decoding-idle");

        getChildren().addAll(hdr, grid);
    }

    void setConnected(boolean connected, String version) {
        if (connected) {
            indicator.getStyleClass().add("connected");
            statusLbl.setText("Connected");
            versionLbl.setText(version != null ? version : "?");
        } else {
            indicator.getStyleClass().remove("connected");
            statusLbl.setText("Disconnected");
            versionLbl.setText("-");
            freqLbl.setText("-");
            modeLbl.setText("-");
            setTransmitting(false);
            decodeLbl.setText("-");
        }
    }

    void setFrequency(long hz) {
        freqLbl.setText(hz > 0 ? String.format("%.3f MHz", hz / 1_000_000.0) : "-");
    }

    void setMode(String mode) {
        modeLbl.setText(mode != null ? mode : "-");
    }

    void setTransmitting(boolean tx) {
        txLbl.setText(tx ? "TX" : "RX");
        txLbl.getStyleClass().removeAll("tx-active", "tx-idle");
        txLbl.getStyleClass().add(tx ? "tx-active" : "tx-idle");
    }

    void setDecoding(boolean dec) {
        decodeLbl.setText(dec ? "Decoding\u2026" : "Idle");
        decodeLbl.getStyleClass().removeAll("decoding-active", "decoding-idle");
        decodeLbl.getStyleClass().add(dec ? "decoding-active" : "decoding-idle");
    }

    private Label key(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("jb-key-lbl");
        return l;
    }

    private Label val(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("jb-val-lbl");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// HubStatusPanel
// ══════════════════════════════════════════════════════════════════════════════

class HubStatusPanel extends VBox {

    private final Region indicator  = new Region();
    private final Label  statusLbl  = val("Disconnected");
    private final Label  addressLbl = val("-");
    private final Label  sentLbl    = val("0");
    private final Label  recvLbl    = val("0");
    private final Button reconnectBtn;

    private Runnable onReconnect;

    HubStatusPanel(String address, int port) {
        setSpacing(0);
        getStyleClass().add("jb-status-panel");

        Label hdr = new Label("j-Hub");
        hdr.getStyleClass().add("jb-panel-hdr");

        addressLbl.setText(address + ":" + port);
        indicator.getStyleClass().add("jb-indicator");

        HBox statusRow = new HBox(5, indicator, statusLbl);
        statusRow.setStyle("-fx-alignment: center-left;");

        reconnectBtn = new Button("Reconnect");
        reconnectBtn.getStyleClass().add("jb-cancel-btn");
        reconnectBtn.setPrefHeight(22);
        reconnectBtn.setOnAction(e -> { if (onReconnect != null) onReconnect.run(); });

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(3);
        grid.setPadding(new Insets(4, 0, 0, 0));

        int r = 0;
        grid.add(key("Status:"),  0, r); grid.add(statusRow,  1, r++);
        grid.add(key("Address:"), 0, r); grid.add(addressLbl, 1, r++);
        grid.add(key("Sent:"),    0, r); grid.add(sentLbl,    1, r++);
        grid.add(key("Rcvd:"),    0, r); grid.add(recvLbl,    1, r++);
        grid.add(reconnectBtn,    0, r, 2, 1);

        getChildren().addAll(hdr, grid);
    }

    void setConnected(boolean connected, String detail) {
        if (connected) {
            indicator.getStyleClass().add("connected");
            statusLbl.setText("Connected");
            if (detail != null && !detail.isBlank()) addressLbl.setText(detail);
        } else {
            indicator.getStyleClass().remove("connected");
            statusLbl.setText("Disconnected");
        }
    }

    void setSentCount(long n) { sentLbl.setText(String.valueOf(n)); }
    void setRecvCount(long n) { recvLbl.setText(String.valueOf(n)); }
    void setOnReconnect(Runnable r) { this.onReconnect = r; }

    private Label key(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("jb-key-lbl");
        return l;
    }

    private Label val(String t) {
        Label l = new Label(t);
        l.getStyleClass().add("jb-val-lbl");
        return l;
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Factory
// ══════════════════════════════════════════════════════════════════════════════

public class StatusPanels {
    private StatusPanels() {}

    public static BandActivityPanel  newBandPanel()                     { return new BandActivityPanel(); }
    public static WsjtxStatusPanel   newWsjtxPanel(int udpPort)         { return new WsjtxStatusPanel(udpPort); }
    public static HubStatusPanel     newHubPanel(String addr, int port) { return new HubStatusPanel(addr, port); }
}
