package com.hamlog.controller;

import com.hamlog.cluster.HubEngine;
import com.hamlog.cluster.HubDiscoveryListener;
import com.hamlog.db.DatabaseManager;
import com.hamlog.i18n.I18n;
import com.hamlog.model.DxSpot;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the DX Spotting panel (DxSpot.fxml).
 * Embedded inside NormalLog and ContestLog via fx:include.
 *
 * Receives enriched DX spots from the ham-radio-hub WebSocket server.
 *
 * Two tabs:
 *   Spots — parsed DxSpot rows with enrichment (mode, country, bearing)
 *   Raw   — raw JSON messages received from hub
 */
public class DxSpotController implements Initializable {

    @FXML private TabPane  tabPane;

    // Spots tab
    @FXML private TableView<DxSpot>           spotTable;
    @FXML private TableColumn<DxSpot, String> colSpotter;
    @FXML private TableColumn<DxSpot, String> colDx;
    @FXML private TableColumn<DxSpot, String> colFreq;
    @FXML private TableColumn<DxSpot, String> colBand;
    @FXML private TableColumn<DxSpot, String> colMode;
    @FXML private TableColumn<DxSpot, String> colCountry;
    @FXML private TableColumn<DxSpot, String> colComment;
    @FXML private TableColumn<DxSpot, String> colTime;

    // Raw tab
    @FXML private TextArea rawArea;

    // Toolbar controls
    @FXML private TextField tfHubUrl;
    @FXML private Button    btnConnect;
    @FXML private Button    btnDisconnect;
    @FXML private Label     lblConnStatus;

    private static final String DEFAULT_HUB_URL = "ws://localhost:8080";
    private static final int    MAX_SPOTS        = 200;

    private final HubDiscoveryListener discovery = new HubDiscoveryListener();
    private static final int    MAX_RAW_CHARS    = 20_000;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final ObservableList<DxSpot> spots = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initHubEngine();
        loadSavedUrl();
        autoConnect();
    }

    // ---------------------------------------------------------------
    // Table setup
    // ---------------------------------------------------------------

    private void initTable() {
        colSpotter .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpotter()));
        colDx      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDxCallsign()));
        colFreq    .setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f", c.getValue().getFrequencyKHz())));
        colBand    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBand()));
        colMode    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMode()));
        colCountry .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCountry()));
        colComment .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getComment()));
        colTime    .setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getTime() != null ? c.getValue().getTime().format(TIME_FMT) : ""));

        spotTable.setItems(spots);
        spotTable.setPlaceholder(new Label(I18n.get("dx.no.spots")));

        // Double-click on a spot row → populate entry bar immediately, then notify hub
        spotTable.setRowFactory(tv -> {
            TableRow<DxSpot> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                    DxSpot spot = row.getItem();
                    HubEngine.getInstance().notifySpotSelected(spot); // fills entry bar immediately
                    HubEngine.getInstance().sendSpotSelected(spot);   // broadcasts to hamclock etc.
                }
            });
            return row;
        });
    }

    // ---------------------------------------------------------------
    // Hub engine wiring
    // ---------------------------------------------------------------

    private void initHubEngine() {
        HubEngine engine = HubEngine.getInstance();

        engine.setSpotListener(spot -> {
            if (spots.size() >= MAX_SPOTS) spots.remove(spots.size() - 1);
            spots.add(0, spot);
        });

        engine.setRawLineListener(line -> {
            rawArea.appendText(line + "\n");
            if (rawArea.getLength() > MAX_RAW_CHARS) {
                rawArea.deleteText(0, 5000);
            }
        });

        engine.setOnConnected(() -> {
            lblConnStatus.setText(I18n.get("dx.connected"));
            btnConnect.setDisable(true);
            btnDisconnect.setDisable(false);
        });

        engine.setOnDisconnected(() -> {
            lblConnStatus.setText(I18n.get("dx.disconnected"));
            btnConnect.setDisable(false);
            btnDisconnect.setDisable(true);
        });
    }

    // ---------------------------------------------------------------
    // Hub URL persistence
    // ---------------------------------------------------------------

    private void loadSavedUrl() {
        String saved = DatabaseManager.getInstance().getConfig("hub.url", DEFAULT_HUB_URL);
        tfHubUrl.setText(saved);
    }

    private void autoConnect() {
        // 1. Try saved URL immediately
        String savedUrl = tfHubUrl.getText().trim();
        if (!savedUrl.isBlank()) {
            connectTo(savedUrl);
        }

        // 2. Start beacon listener — reconnects whenever hub is found and we're disconnected
        discovery.setOnHubFound(wsUrl -> {
            if (!HubEngine.getInstance().isConnected()) {
                Platform.runLater(() -> {
                    tfHubUrl.setText(wsUrl);
                    saveUrl(wsUrl);
                });
                connectTo(wsUrl);
            }
        });
        discovery.start();
    }

    private void connectTo(String url) {
        Platform.runLater(() -> lblConnStatus.setText(I18n.get("dx.connecting", url)));
        Thread t = new Thread(() -> HubEngine.getInstance().connect(url), "hub-connect");
        t.setDaemon(true);
        t.start();
    }

    private void saveUrl(String url) {
        DatabaseManager.getInstance().setConfig("hub.url", url);
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    @FXML private void doConnect() {
        String url = tfHubUrl.getText().trim();
        if (url.isBlank()) url = DEFAULT_HUB_URL;
        saveUrl(url);
        connectTo(url);
    }

    @FXML private void doDisconnect() {
        HubEngine.getInstance().disconnect();
    }

    @FXML private void doClearSpots() {
        spots.clear();
    }
}
