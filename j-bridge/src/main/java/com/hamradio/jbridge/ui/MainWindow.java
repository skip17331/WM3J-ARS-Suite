package com.hamradio.jbridge.ui;

import com.google.gson.JsonObject;
import com.hamradio.jbridge.*;
import com.hamradio.jbridge.model.BandActivity;
import com.hamradio.jbridge.model.WsjtxDecode;
import com.hamradio.jbridge.model.WsjtxQsoLogged;
import com.hamradio.jbridge.model.WsjtxStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MainWindow — primary application window.
 *
 * Layout:
 * ┌─────────────────────────────────────────────────────┐
 * │  Toolbar                                            │
 * ├──────────────────────────────────┬──────────────────┤
 * │                                  │  WsjtxStatusPanel│
 * │       DecodeTableView            │  HubStatusPanel  │
 * │                                  │  BandActivityPanel│
 * └──────────────────────────────────┴──────────────────┘
 *
 * Threading rules:
 *   • UDP and WebSocket callbacks arrive on background threads.
 *   • Every JavaFX node mutation is inside Platform.runLater().
 *   • Heavy processing (callsign parsing) happens on the calling thread
 *     before Platform.runLater() to keep the FX thread free.
 *
 * Hub protocol notes (from HubServer / MessageRouter):
 *   • WSJTX_STATUS is sent for every status change — j-hub does NOT
 *     currently have a special handler but rebroadcasts all unknown types,
 *     so j-log and HamClock will receive it and update their frequency display.
 *   • WSJTX_DECODE is handled by MessageRouter.handleWsjtxDecode → broadcastToAll.
 *   • Worked status comes from hub's WORKED_LIST replay on connect.
 *   • Station location comes from hub's RIG_STATUS / STATION_LOCATION replay.
 */
public class MainWindow {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    // ── Services ──────────────────────────────────────────────────────────────
    private final HubClient        hub;
    private final WsjtxUdpListener udp;
    private final MessagePublisher publisher;
    private final WorkedListManager worked  = WorkedListManager.getInstance();
    private final ConfigManager     cfg     = ConfigManager.getInstance();

    // ── UI panels ─────────────────────────────────────────────────────────────
    private final DecodeTableView   decodeTable;
    private final BandActivityPanel bandPanel;
    private final WsjtxStatusPanel  wsjtxPanel;
    private final HubStatusPanel    hubPanel;
    private final Stage             primaryStage;

    // ── State ─────────────────────────────────────────────────────────────────
    private volatile long   currentFrequency = 0;
    private volatile String currentMode      = "FT8";
    private final Map<String, BandActivity> bandActivity = new HashMap<>();

    // Counter refresh timer
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "db-counter-refresh"); t.setDaemon(true); return t;
    });

    // ── Construction ──────────────────────────────────────────────────────────

    public MainWindow(HubClient hub, WsjtxUdpListener udp,
                      MessagePublisher publisher, Stage primaryStage) {
        this.hub          = hub;
        this.udp          = udp;
        this.publisher    = publisher;
        this.primaryStage = primaryStage;

        // Initialise band activity map
        for (String b : new String[]{"160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m"})
            bandActivity.put(b, new BandActivity(b));

        decodeTable = new DecodeTableView();
        decodeTable.setMaxHistory(cfg.getDecodeHistoryLength());
        decodeTable.setAutoScroll(cfg.isAutoScroll());

        bandPanel  = StatusPanels.newBandPanel();
        wsjtxPanel = StatusPanels.newWsjtxPanel(cfg.getWsjtxUdpPort());
        hubPanel   = StatusPanels.newHubPanel(cfg.getHubAddress(), cfg.getHubPort());

        wireUdpCallbacks();
        wireHubCallbacks();

        hubPanel.setOnReconnect(() -> {
            hub.disconnect();
            hub.connect(cfg.getHubUri());
        });

        // Refresh message counters every 2 s
        timer.scheduleAtFixedRate(this::refreshCounters, 2, 2, TimeUnit.SECONDS);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    public void show() {
        primaryStage.setTitle("J-Bridge  |  WM3j ARS Suite");
        primaryStage.setScene(buildScene());
        primaryStage.setWidth(1110);
        primaryStage.setHeight(680);
        primaryStage.setMinWidth(720);
        primaryStage.setMinHeight(420);
        primaryStage.addEventHandler(WindowEvent.WINDOW_CLOSE_REQUEST, e -> {
            timer.shutdown();
            udp.stop();
            hub.disconnect();
            Platform.exit();
        });
        primaryStage.show();
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    private Scene buildScene() {

        // ── Toolbar ───────────────────────────────────────────────────────────
        Label titleLbl = new Label("J-Bridge");
        titleLbl.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 16px; -fx-font-weight: bold;");
        Label subLbl = new Label("WSJT-X  ↔  j-Hub");
        subLbl.setStyle("-fx-text-fill: #6c7086; -fx-font-size: 11px;");
        VBox titleBox = new VBox(1, titleLbl, subLbl);

        CheckBox autoScrollChk = toolChk("Auto-scroll");
        autoScrollChk.setSelected(cfg.isAutoScroll());
        autoScrollChk.setOnAction(e -> {
            cfg.setAutoScroll(autoScrollChk.isSelected());
            decodeTable.setAutoScroll(autoScrollChk.isSelected());
        });

        CheckBox cqOnlyChk = toolChk("CQ only");
        cqOnlyChk.setSelected(cfg.isShowCQOnly());
        cqOnlyChk.setOnAction(e -> cfg.setShowCQOnly(cqOnlyChk.isSelected()));

        Button clearBtn    = toolBtn("Clear");
        Button settingsBtn = toolBtn("⚙  Settings");

        clearBtn.setOnAction(e -> decodeTable.clear());

        settingsBtn.setOnAction(e -> {
            SettingsWindow sw = new SettingsWindow(primaryStage);
            sw.setOnSettingsChanged(() -> {
                decodeTable.setMaxHistory(cfg.getDecodeHistoryLength());
                decodeTable.setAutoScroll(cfg.isAutoScroll());
                autoScrollChk.setSelected(cfg.isAutoScroll());
                cqOnlyChk.setSelected(cfg.isShowCQOnly());
                log.info("Settings applied");
                // Note: port changes require restart
            });
            sw.show();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(12, titleBox, spacer,
                                autoScrollChk, cqOnlyChk, clearBtn, settingsBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #181825; -fx-padding: 8 12 8 12; " +
                         "-fx-border-color: #313244; -fx-border-width: 0 0 1 0;");

        // ── Sidebar ───────────────────────────────────────────────────────────
        VBox sidebar = new VBox(0, wsjtxPanel, hubPanel, bandPanel);
        sidebar.setStyle("-fx-background-color: #181825; -fx-min-width: 178px; -fx-max-width: 178px;");
        VBox.setVgrow(bandPanel, Priority.ALWAYS);

        // ── Center ────────────────────────────────────────────────────────────
        BorderPane center = new BorderPane();
        center.setCenter(decodeTable);
        center.setRight(sidebar);
        center.setStyle("-fx-background-color: #1e1e2e;");

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(center);
        root.setStyle("-fx-background-color: #1e1e2e;");

        Scene scene = new Scene(root);
        java.net.URL css = getClass().getResource("/css/j-bridge.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        return scene;
    }

    // ── UDP callbacks ─────────────────────────────────────────────────────────

    private void wireUdpCallbacks() {

        // Heartbeat / Close
        udp.setOnConnectionChange((connected, version) -> Platform.runLater(() -> {
            wsjtxPanel.setConnected(connected, version);
            publisher.publishConnectionStatus(connected, version);
        }));

        // Status — update cached freq/mode then publish to hub
        udp.setOnStatus(status -> {
            currentFrequency = status.getDialFrequency();
            currentMode      = status.getMode() != null ? status.getMode() : currentMode;
            Platform.runLater(() -> {
                wsjtxPanel.setFrequency(status.getDialFrequency());
                wsjtxPanel.setMode(status.getMode());
                wsjtxPanel.setTransmitting(status.isTransmitting());
                wsjtxPanel.setDecoding(status.isDecoding());
            });
            publisher.publishStatus(status);
        });

        // Decode — the hot path
        udp.setOnDecode(decode -> {
            // Attach last-known frequency and mode from Status
            decode.setFrequency(currentFrequency);
            decode.setMode(currentMode);
            decode.setBand(BandUtils.frequencyToBand(currentFrequency));

            // SNR filter
            if (decode.getSnr() < cfg.getMinimumSnr()) return;

            // CQ-only filter
            if (cfg.isShowCQOnly() && !decode.isCqCall()) return;

            // Band filter
            String band = decode.getBand();
            if (!Arrays.asList(cfg.getBandFilters()).contains(band)) return;

            // Extract callsign
            String callsign = CallsignParser.extractDxCallsign(decode.getMessage());
            decode.setCallsign(callsign);

            // Worked status from local cache (hub will override on WSJTX_DECODE broadcast return)
            if (callsign != null) {
                decode.setWorkedStatus(worked.getWorkedStatus(callsign));
            }

            // Update band activity (done before Platform.runLater for thread safety on map)
            BandActivity ba = bandActivity.get(band);
            if (ba != null) {
                ba.incrementSpots();
                if (callsign != null) ba.updateTopDx(callsign, decode.getDistanceKm());
            }

            // Publish to hub (only if we have a callsign — noise-free)
            if (callsign != null) publisher.publishDecode(decode);

            // Update UI
            Platform.runLater(() -> {
                decodeTable.addDecode(decode);
                if (ba != null) bandPanel.update(ba);
            });
        });

        // QSO Logged
        udp.setOnQsoLogged(qso -> {
            if (qso.getDxCall() != null) {
                worked.markWorked(qso.getDxCall());
            }
            publisher.publishQsoLogged(qso);
            log.info("QSO logged: {}", qso.getDxCall());
        });

        // Clear
        udp.setOnClear(() -> Platform.runLater(() -> {
            decodeTable.clear();
            bandActivity.values().forEach(BandActivity::reset);
            bandPanel.reset();
        }));
    }

    // ── Hub callbacks ─────────────────────────────────────────────────────────

    private void wireHubCallbacks() {

        hub.setOnConnectionChange((connected, detail) -> Platform.runLater(() -> {
            hubPanel.setConnected(connected, detail);
            if (!connected) log.info("Hub disconnected: {}", detail);
        }));

        hub.setOnMessage(json -> {
            String type = json.has("type") ? json.get("type").getAsString() : "";

            switch (type) {

                // Hub tells us the current rig frequency/mode (from j-log CI-V)
                case "RIG_STATUS" -> {
                    if (json.has("frequency")) currentFrequency = json.get("frequency").getAsLong();
                    if (json.has("mode"))      currentMode      = json.get("mode").getAsString();
                }

                // Hub sends station lat/lon on connect (from hub.json station section)
                // J-Bridge does not compute enrichment locally — j-hub does it.
                // We store lat/lon only for display in the future if needed.
                case "STATION_LOCATION" -> {
                    if (json.has("lat") && json.has("lon")) {
                        log.info("Station location from hub: {}, {}",
                                json.get("lat").getAsDouble(),
                                json.get("lon").getAsDouble());
                    }
                }

                // Hub replays the worked callsign list on connect
                case "WORKED_LIST" -> {
                    if (json.has("callsigns")) {
                        java.util.List<String> calls = new java.util.ArrayList<>();
                        json.getAsJsonArray("callsigns")
                            .forEach(el -> calls.add(el.getAsString()));
                        worked.setWorkedList(calls);
                        log.info("Received worked list: {} callsigns", calls.size());
                    }
                }

                // Hub rebroadcasts our own WSJTX_DECODE back with any hub-side enrichment.
                // Update the local decode table row's enrichment fields if present.
                // (Hub currently just rebroadcasts — enrichment may be added server-side later.)
                case "WSJTX_DECODE" -> {
                    // Currently a no-op: we already displayed the row on inbound UDP decode.
                    // If hub adds enrichment (country, bearing, etc.) a future enhancement
                    // would update the existing table row here.
                }

                case "HUB_WELCOME" ->
                    log.info("Hub welcome received — state replay beginning");

                case "APP_LIST" ->
                    log.debug("APP_LIST: {}", json);

                default ->
                    log.debug("Hub message type '{}' not handled by bridge", type);
            }
        });
    }

    // ── Counter refresh ───────────────────────────────────────────────────────

    private void refreshCounters() {
        long s = hub.getMessagesSent();
        long r = hub.getMessagesReceived();
        Platform.runLater(() -> {
            hubPanel.setSentCount(s);
            hubPanel.setRecvCount(r);
        });
    }

    // ── Toolbar helpers ───────────────────────────────────────────────────────

    private Button toolBtn(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: #313244; -fx-text-fill: #cdd6f4; " +
                   "-fx-font-size: 11px; -fx-cursor: hand; -fx-padding: 4 10 4 10;");
        return b;
    }

    private CheckBox toolChk(String text) {
        CheckBox cb = new CheckBox(text);
        cb.setStyle("-fx-text-fill: #cdd6f4; -fx-font-size: 11px;");
        return cb;
    }
}
