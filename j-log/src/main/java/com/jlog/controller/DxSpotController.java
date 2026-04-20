package com.jlog.controller;

import com.jlog.cluster.HubEngine;
import com.jlog.cluster.HubDiscoveryListener;
import com.jlog.db.DatabaseManager;
import com.jlog.i18n.I18n;
import com.jlog.model.DxSpot;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Controller for the DX Spotting panel (DxSpot.fxml).
 * Embedded inside NormalLog and ContestLog via fx:include.
 *
 * Spots flow from j-hub via WebSocket. The Connect/Disconnect buttons
 * open and close the DX cluster telnet session inside j-hub.
 */
public class DxSpotController implements Initializable {

    @FXML private TableView<DxSpot>           spotTable;
    @FXML private TableColumn<DxSpot, String> colSpotter;
    @FXML private TableColumn<DxSpot, String> colDx;
    @FXML private TableColumn<DxSpot, String> colFreq;
    @FXML private TableColumn<DxSpot, String> colBand;
    @FXML private TableColumn<DxSpot, String> colMode;
    @FXML private TableColumn<DxSpot, String> colCountry;
    @FXML private TableColumn<DxSpot, String> colComment;
    @FXML private TableColumn<DxSpot, String> colTime;

    @FXML private Button btnClusterConnect;
    @FXML private Button btnClusterDisconnect;
    @FXML private Label  lblClusterStatus;

    private static final String DEFAULT_HUB_URL = "ws://localhost:8080";
    private static final int    MAX_SPOTS        = 200;
    private static final String HUB_REST         = "http://localhost:8081";

    private final HubDiscoveryListener        discovery = new HubDiscoveryListener();
    private static final DateTimeFormatter    TIME_FMT  = DateTimeFormatter.ofPattern("HH:mm");
    private final ObservableList<DxSpot>      spots     = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initHubEngine();
        autoConnect();
        refreshClusterStatus();
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

        spotTable.setRowFactory(tv -> {
            TableRow<DxSpot> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty() && e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                    DxSpot spot = row.getItem();
                    HubEngine.getInstance().notifySpotSelected(spot);
                    HubEngine.getInstance().sendSpotSelected(spot);
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

        engine.setSpotListener(spot -> Platform.runLater(() -> {
            if (spots.size() >= MAX_SPOTS) spots.remove(spots.size() - 1);
            spots.add(0, spot);
        }));

        engine.setOnShutdown(() -> Platform.runLater(Platform::exit));
    }

    // ---------------------------------------------------------------
    // Auto-connect to j-hub WebSocket
    // ---------------------------------------------------------------

    private void autoConnect() {
        String savedUrl = DatabaseManager.getInstance().getConfig("hub.url", DEFAULT_HUB_URL);
        if (!savedUrl.isBlank()) {
            connectToHub(savedUrl);
        }

        discovery.setOnHubFound(wsUrl -> {
            if (!HubEngine.getInstance().isConnected()) {
                saveUrl(wsUrl);
                connectToHub(wsUrl);
            }
        });
        discovery.start();
    }

    private void connectToHub(String url) {
        Thread t = new Thread(() -> HubEngine.getInstance().connect(url), "hub-connect");
        t.setDaemon(true);
        t.start();
    }

    private void saveUrl(String url) {
        DatabaseManager.getInstance().setConfig("hub.url", url);
    }

    // ---------------------------------------------------------------
    // Cluster telnet session controls
    // ---------------------------------------------------------------

    @FXML private void doClusterConnect() {
        setClusterBusy("Connecting...");
        new Thread(() -> {
            try {
                hubPost("/api/cluster/connect", "");
                Platform.runLater(() -> setClusterConnected(true));
            } catch (Exception e) {
                Platform.runLater(() -> lblClusterStatus.setText("Error: " + e.getMessage()));
            }
        }, "cluster-connect").start();
    }

    @FXML private void doClusterDisconnect() {
        setClusterBusy("Disconnecting...");
        new Thread(() -> {
            try {
                hubPost("/api/cluster/disconnect", "");
                Platform.runLater(() -> setClusterConnected(false));
            } catch (Exception e) {
                Platform.runLater(() -> lblClusterStatus.setText("Error: " + e.getMessage()));
            }
        }, "cluster-disconnect").start();
    }

    private void refreshClusterStatus() {
        new Thread(() -> {
            try {
                String json = hubGet("/api/status");
                boolean connected = json.contains("\"clusterConnected\":true");
                Platform.runLater(() -> setClusterConnected(connected));
            } catch (Exception ignored) {}
        }, "cluster-status").start();
    }

    private void setClusterConnected(boolean connected) {
        btnClusterConnect   .setDisable(connected);
        btnClusterDisconnect.setDisable(!connected);
        lblClusterStatus.setText(connected ? "● Connected" : "○ Disconnected");
        lblClusterStatus.setStyle(connected
            ? "-fx-text-fill: #69f0ae;"
            : "-fx-text-fill: #888888;");
    }

    private void setClusterBusy(String msg) {
        btnClusterConnect   .setDisable(true);
        btnClusterDisconnect.setDisable(true);
        lblClusterStatus.setText(msg);
        lblClusterStatus.setStyle("-fx-text-fill: #ffdd00;");
    }

    // ---------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------

    @FXML private void doClearSpots() {
        spots.clear();
    }

    // ---------------------------------------------------------------
    // HTTP helpers (calls j-hub REST on localhost:8081)
    // ---------------------------------------------------------------

    private static String hubGet(String path) throws Exception {
        java.net.HttpURLConnection c =
            (java.net.HttpURLConnection) new java.net.URL(HUB_REST + path).openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(3000);
        return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void hubPost(String path, String body) throws Exception {
        java.net.HttpURLConnection c =
            (java.net.HttpURLConnection) new java.net.URL(HUB_REST + path).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(2000);
        c.setReadTimeout(3000);
        c.setRequestProperty("Content-Type", "application/json");
        if (!body.isEmpty())
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        c.getInputStream().readAllBytes();
    }
}
