package com.hamradio.jbridge.ui;

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
import java.util.prefs.Preferences;

public class MainWindow {

    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);

    /** Package-private — read by SettingsWindow to apply the same theme. */
    static boolean isDark = false;

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(MainWindow.class);

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

    // ── Theme toggle button ───────────────────────────────────────────────────
    private final Button themeBtn = new Button();
    private Scene scene;

    // ── State ─────────────────────────────────────────────────────────────────
    private volatile long   currentFrequency = 0;
    private volatile String currentMode      = "FT8";
    private final Map<String, BandActivity> bandActivity = new HashMap<>();

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

        isDark = PREFS.getBoolean("darkTheme", false);   // default: light

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

        timer.scheduleAtFixedRate(this::refreshCounters, 2, 2, TimeUnit.SECONDS);
    }

    // ── Show ──────────────────────────────────────────────────────────────────

    public void show() {
        primaryStage.setTitle("J-Bridge  |  ARS Suite");
        scene = buildScene();
        scene.getRoot().getStyleClass().add(isDark ? "dark" : "light");
        primaryStage.setScene(scene);
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

    // ── Theme toggle ──────────────────────────────────────────────────────────

    private void toggleTheme() {
        isDark = !isDark;
        PREFS.putBoolean("darkTheme", isDark);
        var cls = scene.getRoot().getStyleClass();
        if (isDark) { cls.remove("light"); cls.add("dark"); }
        else        { cls.remove("dark");  cls.add("light"); }
        themeBtn.setText(isDark ? "☀" : "☾");
    }

    // ── Scene ─────────────────────────────────────────────────────────────────

    private Scene buildScene() {

        // ── Toolbar ───────────────────────────────────────────────────────────
        Label titleLbl = new Label("J-Bridge");
        titleLbl.getStyleClass().add("jb-title");
        Label subLbl = new Label("WSJT-X  ↔  j-Hub");
        subLbl.getStyleClass().add("jb-sub");
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
            });
            sw.show();
        });

        themeBtn.getStyleClass().addAll("tb-btn");
        themeBtn.setText(isDark ? "☀" : "☾");
        themeBtn.setTooltip(new Tooltip("Toggle light / dark theme"));
        themeBtn.setOnAction(e -> toggleTheme());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(12, titleBox, spacer,
                autoScrollChk, cqOnlyChk, clearBtn, settingsBtn, themeBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("jb-toolbar");

        // ── Sidebar ───────────────────────────────────────────────────────────
        VBox sidebar = new VBox(0, wsjtxPanel, hubPanel, bandPanel);
        sidebar.getStyleClass().add("jb-sidebar");
        VBox.setVgrow(bandPanel, Priority.ALWAYS);

        // ── Center ────────────────────────────────────────────────────────────
        BorderPane center = new BorderPane();
        center.setCenter(decodeTable);
        center.setRight(sidebar);
        center.getStyleClass().add("jb-content");

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(center);
        root.getStyleClass().add("jb-content");

        Scene sc = new Scene(root);
        java.net.URL css = getClass().getResource("/css/j-bridge.css");
        if (css != null) sc.getStylesheets().add(css.toExternalForm());
        return sc;
    }

    // ── UDP callbacks ─────────────────────────────────────────────────────────

    private void wireUdpCallbacks() {

        udp.setOnConnectionChange((connected, version) -> Platform.runLater(() -> {
            wsjtxPanel.setConnected(connected, version);
            publisher.publishConnectionStatus(connected, version);
        }));

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

        udp.setOnDecode(decode -> {
            decode.setFrequency(currentFrequency);
            decode.setMode(currentMode);
            decode.setBand(BandUtils.frequencyToBand(currentFrequency));

            if (decode.getSnr() < cfg.getMinimumSnr()) return;
            if (cfg.isShowCQOnly() && !decode.isCqCall()) return;

            String band = decode.getBand();
            if (!Arrays.asList(cfg.getBandFilters()).contains(band)) return;

            String callsign = CallsignParser.extractDxCallsign(decode.getMessage());
            decode.setCallsign(callsign);

            if (callsign != null) decode.setWorkedStatus(worked.getWorkedStatus(callsign));

            BandActivity ba = bandActivity.get(band);
            if (ba != null) {
                ba.incrementSpots();
                if (callsign != null) ba.updateTopDx(callsign, decode.getDistanceKm());
            }

            if (callsign != null) publisher.publishDecode(decode);

            Platform.runLater(() -> {
                decodeTable.addDecode(decode);
                if (ba != null) bandPanel.update(ba);
            });
        });

        udp.setOnQsoLogged(qso -> {
            if (qso.getDxCall() != null) worked.markWorked(qso.getDxCall());
            publisher.publishQsoLogged(qso);
            log.info("QSO logged: {}", qso.getDxCall());
        });

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
                case "RIG_STATUS" -> {
                    if (json.has("frequency")) currentFrequency = json.get("frequency").getAsLong();
                    if (json.has("mode"))      currentMode      = json.get("mode").getAsString();
                }
                case "STATION_LOCATION" -> {
                    if (json.has("lat") && json.has("lon"))
                        log.info("Station location from hub: {}, {}",
                                json.get("lat").getAsDouble(), json.get("lon").getAsDouble());
                }
                case "WORKED_LIST" -> {
                    if (json.has("callsigns")) {
                        java.util.List<String> calls = new java.util.ArrayList<>();
                        json.getAsJsonArray("callsigns").forEach(el -> calls.add(el.getAsString()));
                        worked.setWorkedList(calls);
                        log.info("Received worked list: {} callsigns", calls.size());
                    }
                }
                case "WSJTX_DECODE"  -> { /* hub rebroadcast — future enrichment hook */ }
                case "HUB_WELCOME"   -> log.info("Hub welcome received");
                case "APP_LIST"      -> log.debug("APP_LIST: {}", json);
                default              -> log.debug("Hub msg '{}' not handled", type);
            }
        });
    }

    // ── Counter refresh ───────────────────────────────────────────────────────

    private void refreshCounters() {
        long s = hub.getMessagesSent();
        long r = hub.getMessagesReceived();
        Platform.runLater(() -> { hubPanel.setSentCount(s); hubPanel.setRecvCount(r); });
    }

    // ── Toolbar helpers ───────────────────────────────────────────────────────

    private Button toolBtn(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("tb-btn");
        return b;
    }

    private CheckBox toolChk(String text) {
        CheckBox cb = new CheckBox(text);
        return cb;
    }
}
