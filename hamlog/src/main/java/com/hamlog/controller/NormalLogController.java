package com.hamlog.controller;

import com.hamlog.app.HamLogApp;
import com.hamlog.cluster.HubEngine;
import com.hamlog.model.DxSpot;
import com.hamlog.civ.CivEngine;
import com.hamlog.db.QsoDao;
import com.hamlog.i18n.I18n;
import com.hamlog.macro.MacroEngine;
import com.hamlog.model.QsoRecord;
import com.hamlog.util.AppConfig;
import com.hamlog.util.QrzLookup;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Controller for the Normal Log window (NormalLog.fxml).
 *
 * Layout rows:
 *   Row 1 — Menu bar + UTC/Local clock
 *   Row 2 — Data entry pane + pane1/2/3/4 (callsign info, bearing, etc.)
 *   Row 3 — QSO database table
 *   Row 4 — DX Spotting window
 */
public class NormalLogController implements Initializable {

    // ---- Data entry fields ----
    @FXML private TextField tfCallsign;
    @FXML private DatePicker dpDate;
    @FXML private ComboBox<String> cbBand;
    @FXML private ComboBox<String> cbMode;
    @FXML private TextField tfPower;
    @FXML private TextField tfRstSent;
    @FXML private TextField tfRstReceived;
    @FXML private TextField tfCountry;
    @FXML private TextField tfOperatorName;
    @FXML private TextField tfState;
    @FXML private TextField tfCounty;
    @FXML private TextField tfFrequency;
    @FXML private TextField tfNotes;
    @FXML private CheckBox  cbQslSent;
    @FXML private CheckBox  cbQslReceived;
    @FXML private TextField tfUtcTime;
    @FXML private TextField tfLocalTime;

    // ---- Info panes ----
    @FXML private Label lblCountry;
    @FXML private Label lblContinent;
    @FXML private Label lblBearing;
    @FXML private Label lblDistance;

    // ---- QSO table ----
    @FXML private TableView<QsoRecord>      qsoTable;
    @FXML private TableColumn<QsoRecord,String> colCallsign;
    @FXML private TableColumn<QsoRecord,String> colDate;
    @FXML private TableColumn<QsoRecord,String> colBand;
    @FXML private TableColumn<QsoRecord,String> colMode;
    @FXML private TableColumn<QsoRecord,String> colRstSent;
    @FXML private TableColumn<QsoRecord,String> colRstRcvd;
    @FXML private TableColumn<QsoRecord,String> colCountry;
    @FXML private TableColumn<QsoRecord,String> colNotes;

    // ---- DX Spotting ----
    @FXML private SplitPane    mainSplitPane;
    @FXML private TitledPane   dxPane;
    @FXML private AnchorPane   dxContainer;

    // ---- Status / clock bar ----
    @FXML private Label lblStatus;
    @FXML private Label lblCivStatus;
    @FXML private Label lblStationCall;

    // ---- Macro buttons ----
    @FXML private HBox macroButtonBar;

    private final ObservableList<QsoRecord> qsoData = FXCollections.observableArrayList();
    private ScheduledExecutorService clockService;
    private QrzLookup qrzLookup;
    private int qsoPageOffset = 0;
    private static final int PAGE_SIZE = 20;

    private DxSpotController dxSpotController;

    private static final DateTimeFormatter UTC_FMT   = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'");
    private static final DateTimeFormatter LOCAL_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss z");
    private static final DateTimeFormatter TABLE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initComboBoxes();
        initTable();
        initClock();
        initCivListeners();
        initMacroBar();
        initKeyHandlers();
        loadQsos();
        initQrzLookup();

        // Load divider positions
        Platform.runLater(this::restoreDividers);

        // CI-V auto-connect if configured
        if (AppConfig.getInstance().getCivAutoConnect()) {
            connectCiv();
        }

        // Incoming SPOT_SELECTED from hub (HamClock or HamLog spot click) → fill entry bar
        HubEngine.getInstance().setSpotSelectedListener(spot ->
            Platform.runLater(() -> fillFromSpot(spot)));

        setStatus(I18n.get("status.ready"));
        updateStationCallLabel();
    }

    // ---------------------------------------------------------------
    // Initialisation helpers
    // ---------------------------------------------------------------

    private void initComboBoxes() {
        cbBand.setItems(FXCollections.observableArrayList(
            "160m","80m","60m","40m","30m","20m","17m","15m","12m","10m","6m","2m","70cm"));
        cbMode.setItems(FXCollections.observableArrayList(
            "CW","USB","LSB","AM","FM","RTTY","FT8","FT4","PSK31","OLIVIA","DV"));
        cbBand.setValue("20m");
        cbMode.setValue("CW");

        // Default RST
        tfRstSent.setText("599");
        tfRstReceived.setText("599");

        // Default power
        tfPower.setText(AppConfig.getInstance().getDefaultPower());
    }

    private void initTable() {
        colCallsign.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCallsign()));
        colDate    .setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateTimeUtc() != null ? c.getValue().getDateTimeUtc().format(TABLE_FMT) : ""));
        colBand    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBand()));
        colMode    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMode()));
        colRstSent .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstSent()));
        colRstRcvd .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstReceived()));
        colCountry .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCountry()));
        colNotes   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));

        qsoTable.setItems(qsoData);
        qsoTable.setRowFactory(tv -> {
            TableRow<QsoRecord> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    populateFromRecord(row.getItem());
                }
            });
            return row;
        });
    }

    private void initClock() {
        clockService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "clock");
            t.setDaemon(true);
            return t;
        });
        clockService.scheduleAtFixedRate(() -> {
            ZonedDateTime utc   = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime local = ZonedDateTime.now();
            Platform.runLater(() -> {
                tfUtcTime.setText(UTC_FMT.format(utc));
                tfLocalTime.setText(LOCAL_FMT.format(local));
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void initCivListeners() {
        CivEngine civ = CivEngine.getInstance();
        civ.setFrequencyListener(hz -> Platform.runLater(() -> {
            tfFrequency.setText(String.format("%.3f", hz / 1_000_000.0));
            String band = CivEngine.freqToBand(hz);
            cbBand.setValue(band);
        }));
        civ.setModeListener(mode -> Platform.runLater(() -> cbMode.setValue(mode)));
    }

    private void initMacroBar() {
        MacroEngine engine = MacroEngine.getInstance();
        engine.setExchangeInsertHandler(text -> {
            if (tfNotes.isFocused()) tfNotes.setText(text);
        });
        engine.setAutofillHandler(this::doCallsignLookup);

        // Build F1-F12 buttons
        macroButtonBar.getChildren().clear();
        for (int fk = 1; fk <= 12; fk++) {
            final int fkey = fk;
            Button btn = new Button("F" + fk);
            btn.getStyleClass().add("macro-button");
            btn.setOnAction(e -> MacroEngine.getInstance().triggerFKey(fkey));
            macroButtonBar.getChildren().add(btn);
        }
    }

    private void initKeyHandlers() {
        // F1-F12 global key handler on the scene
        Platform.runLater(() -> {
            if (tfCallsign.getScene() != null) {
                tfCallsign.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                    if (e.getCode().isFunctionKey()) {
                        int fk = e.getCode().ordinal() - KeyCode.F1.ordinal() + 1;
                        if (fk >= 1 && fk <= 12) {
                            MacroEngine.getInstance().triggerFKey(fk);
                            e.consume();
                        }
                    }
                    if (e.getCode() == KeyCode.ENTER && tfCallsign.isFocused()) {
                        doSave();
                        e.consume();
                    }
                });
            }
        });

        // Callsign field — lookup on TAB or Enter
        tfCallsign.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB || e.getCode() == KeyCode.ENTER) {
                doCallsignLookup();
            }
        });
    }

    private void initQrzLookup() {
        String user = AppConfig.getInstance().getQrzUsername();
        String pass = AppConfig.getInstance().getQrzPassword();
        if (user != null && !user.isBlank()) {
            qrzLookup = new QrzLookup(user, pass);
        }
    }

    private void restoreDividers() {
        AppConfig cfg = AppConfig.getInstance();
        double d0 = cfg.getDivider("normalLog.div0", 0.5);
        mainSplitPane.setDividerPositions(d0);
        mainSplitPane.getDividers().forEach(div ->
            div.positionProperty().addListener((o, ov, nv) ->
                cfg.setDivider("normalLog.div0", mainSplitPane.getDividerPositions()[0])));
    }

    // ---------------------------------------------------------------
    // Data entry actions
    // ---------------------------------------------------------------

    @FXML private void doSave() {
        String call = tfCallsign.getText().trim().toUpperCase();
        if (call.isEmpty()) {
            setStatus(I18n.get("error.callsign.required"));
            return;
        }
        QsoRecord q = buildRecord();
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                QsoDao.getInstance().insert(q);
                return null;
            }
            @Override protected void succeeded() {
                setStatus(I18n.get("status.saved", q.getCallsign()));
                doClear();
                loadQsos();
            }
            @Override protected void failed() {
                setStatus(I18n.get("error.save.failed") + ": " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    @FXML private void doClear() {
        tfCallsign.clear();
        tfRstSent.setText("599");
        tfRstReceived.setText("599");
        tfCountry.clear(); tfOperatorName.clear();
        tfState.clear();   tfCounty.clear();
        tfNotes.clear();
        cbQslSent.setSelected(false);
        cbQslReceived.setSelected(false);
        lblCountry.setText(""); lblContinent.setText("");
        lblBearing.setText(""); lblDistance.setText("");
        tfCallsign.requestFocus();
    }

    @FXML private void doCallsignLookup() {
        String call = tfCallsign.getText().trim().toUpperCase();
        if (call.isEmpty() || qrzLookup == null) return;

        Task<Map<String, String>> task = new Task<>() {
            @Override protected Map<String,String> call() {
                return qrzLookup.lookup(call);
            }
            @Override protected void succeeded() {
                Map<String,String> data = getValue();
                if (!data.isEmpty()) {
                    tfOperatorName.setText(data.getOrDefault("fullname", ""));
                    tfCountry.setText(data.getOrDefault("country", ""));
                    tfState.setText(data.getOrDefault("state", ""));
                    tfCounty.setText(data.getOrDefault("county", ""));
                    lblCountry.setText(data.getOrDefault("country", ""));

                    // Bearing / distance
                    try {
                        double myLat = Double.parseDouble(AppConfig.getInstance().getLatitude());
                        double myLon = Double.parseDouble(AppConfig.getInstance().getLongitude());
                        double dxLat = Double.parseDouble(data.getOrDefault("lat", "0"));
                        double dxLon = Double.parseDouble(data.getOrDefault("lon", "0"));
                        double brg   = QrzLookup.bearing(myLat, myLon, dxLat, dxLon);
                        double dist  = QrzLookup.distanceKm(myLat, myLon, dxLat, dxLon);
                        lblBearing.setText(String.format("%.0f°", brg));
                        lblDistance.setText(String.format("%.0f km", dist));
                    } catch (Exception ignored) {}
                }
            }
        };
        new Thread(task).start();
    }

    // ---------------------------------------------------------------
    // Table navigation
    // ---------------------------------------------------------------

    @FXML private void doPageForward() {
        try {
            int total = QsoDao.getInstance().count();
            if (qsoPageOffset + PAGE_SIZE < total) {
                qsoPageOffset += PAGE_SIZE;
                loadQsos();
            }
        } catch (SQLException ex) {
            setStatus(ex.getMessage());
        }
    }

    @FXML private void doPageBack() {
        if (qsoPageOffset > 0) {
            qsoPageOffset -= PAGE_SIZE;
            loadQsos();
        }
    }

    @FXML private void doEditSelected() {
        QsoRecord sel = qsoTable.getSelectionModel().getSelectedItem();
        if (sel != null) populateFromRecord(sel);
    }

    @FXML private void doSaveEdited() {
        QsoRecord sel = qsoTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        applyFieldsTo(sel);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                QsoDao.getInstance().update(sel);
                return null;
            }
            @Override protected void succeeded() {
                setStatus(I18n.get("status.updated"));
                loadQsos();
            }
        };
        new Thread(task).start();
    }

    // ---------------------------------------------------------------
    // Menu actions
    // ---------------------------------------------------------------

    @FXML private void menuExit() {
        Platform.exit();
    }

    @FXML private void menuExportAdif() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ADIF Files", "*.adi"));
        File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.hamlog.export.AdifExporter.exportAdif(f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuExportCsv() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.hamlog.export.AdifExporter.exportCsv(f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuSetup() {
        openSetup();
        updateStationCallLabel();
    }

    @FXML private void menuDxSpotting() {
        dxPane.setExpanded(true);
    }

    @FXML private void menuConnectCiv() {
        connectCiv();
    }

    @FXML private void menuDisconnectCiv() {
        CivEngine.getInstance().disconnect();
        lblCivStatus.setText(I18n.get("civ.disconnected"));
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void loadQsos() {
        Task<List<QsoRecord>> task = new Task<>() {
            @Override protected List<QsoRecord> call() throws Exception {
                return QsoDao.getInstance().fetchPage(qsoPageOffset, PAGE_SIZE);
            }
            @Override protected void succeeded() {
                qsoData.setAll(getValue());
            }
        };
        new Thread(task).start();
    }

    private QsoRecord buildRecord() {
        QsoRecord q = new QsoRecord();
        q.setCallsign(tfCallsign.getText().trim().toUpperCase());
        q.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
        q.setBand(cbBand.getValue());
        q.setMode(cbMode.getValue());
        q.setFrequency(tfFrequency.getText());
        try { q.setPowerWatts(Integer.parseInt(tfPower.getText())); } catch (Exception e) { q.setPowerWatts(0); }
        q.setRstSent(tfRstSent.getText());
        q.setRstReceived(tfRstReceived.getText());
        q.setCountry(tfCountry.getText());
        q.setOperatorName(tfOperatorName.getText());
        q.setState(tfState.getText());
        q.setCounty(tfCounty.getText());
        q.setNotes(tfNotes.getText());
        q.setQslSent(cbQslSent.isSelected());
        q.setQslReceived(cbQslReceived.isSelected());
        return q;
    }

    private void populateFromRecord(QsoRecord q) {
        tfCallsign.setText(q.getCallsign());
        cbBand.setValue(q.getBand());
        cbMode.setValue(q.getMode());
        tfFrequency.setText(q.getFrequency());
        tfPower.setText(String.valueOf(q.getPowerWatts()));
        tfRstSent.setText(q.getRstSent());
        tfRstReceived.setText(q.getRstReceived());
        tfCountry.setText(q.getCountry());
        tfOperatorName.setText(q.getOperatorName());
        tfState.setText(q.getState());
        tfCounty.setText(q.getCounty());
        tfNotes.setText(q.getNotes());
        cbQslSent.setSelected(q.isQslSent());
        cbQslReceived.setSelected(q.isQslReceived());
    }

    private void applyFieldsTo(QsoRecord q) {
        q.setCallsign(tfCallsign.getText().trim().toUpperCase());
        q.setBand(cbBand.getValue());
        q.setMode(cbMode.getValue());
        q.setFrequency(tfFrequency.getText());
        try { q.setPowerWatts(Integer.parseInt(tfPower.getText())); } catch (Exception e) {}
        q.setRstSent(tfRstSent.getText());
        q.setRstReceived(tfRstReceived.getText());
        q.setCountry(tfCountry.getText());
        q.setOperatorName(tfOperatorName.getText());
        q.setState(tfState.getText());
        q.setCounty(tfCounty.getText());
        q.setNotes(tfNotes.getText());
        q.setQslSent(cbQslSent.isSelected());
        q.setQslReceived(cbQslReceived.isSelected());
    }

    // ---------------------------------------------------------------
    // Spot selection — fill entry bar from a DX spot
    // ---------------------------------------------------------------

    private void fillFromSpot(DxSpot spot) {
        if (spot.getDxCallsign() != null && !spot.getDxCallsign().isBlank()) {
            tfCallsign.setText(spot.getDxCallsign().toUpperCase());
        }
        if (spot.getFrequencyKHz() > 0) {
            tfFrequency.setText(String.format("%.3f", spot.getFrequencyKHz() / 1000.0));
            cbBand.setValue(spot.getBand());
        }
        if (spot.getMode() != null && !spot.getMode().isBlank()) {
            cbMode.setValue(mapMode(spot.getMode()));
        }
        if (spot.getCountry() != null && !spot.getCountry().isBlank()) {
            tfCountry.setText(spot.getCountry());
            lblCountry.setText(spot.getCountry());
        }
        if (spot.getBearing() > 0)    lblBearing.setText(String.format("%.0f°", spot.getBearing()));
        if (spot.getDistanceKm() > 0) lblDistance.setText(String.format("%.0f km", spot.getDistanceKm()));

        civTune(spot);
        tfCallsign.requestFocus();
        setStatus(I18n.get("status.spot.selected", spot.getDxCallsign()));
    }

    private void civTune(DxSpot spot) {
        CivEngine civ = CivEngine.getInstance();
        if (!civ.isConnected()) return;
        if (spot.getFrequencyKHz() > 0)
            civ.setFrequency((long)(spot.getFrequencyKHz() * 1000));
        if (spot.getMode() != null && !spot.getMode().isBlank())
            civ.setMode(mapMode(spot.getMode()));
    }

    private String mapMode(String mode) {
        if (mode == null) return "USB";
        return switch (mode.toUpperCase()) {
            case "CW"             -> "CW";
            case "FT8"            -> "FT8";
            case "FT4"            -> "FT4";
            case "RTTY"           -> "RTTY";
            case "PSK31"          -> "PSK31";
            case "LSB"            -> "LSB";
            case "AM"             -> "AM";
            case "FM"             -> "FM";
            case "SSB", "USB"     -> "USB";
            default               -> "USB";
        };
    }

    private void connectCiv() {
        AppConfig cfg = AppConfig.getInstance();
        String port = cfg.getCivPort();
        int baud = Integer.parseInt(cfg.getCivBaud());
        int addr = Integer.parseInt(cfg.getCivAddress(), 16);
        boolean ok = CivEngine.getInstance().connect(port, baud, (byte) addr);
        Platform.runLater(() -> lblCivStatus.setText(ok ?
            I18n.get("civ.connected", port) : I18n.get("civ.failed")));
    }

    private void openSetup() {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081").start(); }
        catch (Exception e) { setStatus("Could not open browser: " + e.getMessage()); }
    }

    private void updateStationCallLabel() {
        String call = AppConfig.getInstance().getStationCallsign();
        lblStationCall.setText(call.isEmpty() ? "" : call + " | ");
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> lblStatus.setText(msg));
    }

    private Stage getStage() {
        return (Stage) tfCallsign.getScene().getWindow();
    }
}
