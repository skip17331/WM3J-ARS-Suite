package com.jlog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jlog.cluster.HubEngine;
import com.jlog.model.DxSpot;
import com.jlog.civ.CivEngine;
import com.jlog.db.QsoDao;
import com.jlog.i18n.I18n;
import com.jlog.macro.MacroEngine;
import com.jlog.model.QsoRecord;
import com.jlog.util.AppConfig;
import com.jlog.util.BandPlan;
import com.jlog.util.MacroVariableEngine;
import com.jlog.util.BandPlan.ValidationResult;
import com.jlog.util.QrzLookup;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

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
 *   Row 2 — Data entry pane + country/bearing info panes
 *   Row 3 — QSO database table
 *   Row 4 — DX Spotting pane
 */
public class NormalLogController implements Initializable {

    // ---- Data entry fields ----
    @FXML private TextField tfCallsign;
    @FXML private DatePicker dpDate;
    @FXML private TextField tfBand;
    @FXML private TextField tfMode;
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
    @FXML private Button    btnSave;

    /** The QSO currently loaded into the entry form for editing. When non-null,
     *  doSave issues an UPDATE (preserving the original timestamp + id) instead
     *  of INSERT. Cleared by doClear() and on successful save. */
    private QsoRecord editingRecord = null;

    // ---- Info panes ----
    @FXML private Label lblCountry;
    @FXML private Label lblContinent;
    @FXML private Label lblBearing;
    @FXML private Label lblDistance;
    @FXML private TitledPane spaceWxPane;
    @FXML private Label lblSolarSfi;
    @FXML private Label lblSolarK;
    @FXML private Label lblSolarSsn;
    @FXML private Label lblSolarXray;
    @FXML private Label lblSolarWind;
    @FXML private Label lblSolarGeo;

    // ---- Embedded CW / Digital keyer ----
    @FXML private VBox             keyerPane;
    @FXML private CheckMenuItem    miToggleKeyer;
    @FXML private ComboBox<String> cbKeyerMode;
    @FXML private Label            lblCwWpm;
    @FXML private Spinner<Integer> spCwWpm;
    @FXML private TextArea         taKeyerRx;
    @FXML private TextField        tfKeyerTx;
    @FXML private Label            lblKeyerStatus;

    // ---- QSO table ----
    @FXML private TableView<QsoRecord>          qsoTable;
    @FXML private TableColumn<QsoRecord,String> colCallsign;
    @FXML private TableColumn<QsoRecord,String> colDate;
    @FXML private TableColumn<QsoRecord,String> colBand;
    @FXML private TableColumn<QsoRecord,String> colMode;
    @FXML private TableColumn<QsoRecord,String> colRstSent;
    @FXML private TableColumn<QsoRecord,String> colRstRcvd;
    @FXML private TableColumn<QsoRecord,String> colCountry;
    @FXML private TableColumn<QsoRecord,String> colNotes;
    @FXML private javafx.scene.control.ChoiceBox<String> cbSortOrder;
    private static final String SORT_NEWEST = "Newest first";
    private static final String SORT_OLDEST = "Oldest first";

    // ---- Previous QSOs panel (live search by callsign) ----
    @FXML private TitledPane                       prevQsosPane;
    @FXML private TableView<QsoRecord>             prevQsosTable;
    @FXML private TableColumn<QsoRecord,String>    colPqDate;
    @FXML private TableColumn<QsoRecord,String>    colPqBand;
    @FXML private TableColumn<QsoRecord,String>    colPqMode;
    @FXML private TableColumn<QsoRecord,String>    colPqRstS;
    @FXML private TableColumn<QsoRecord,String>    colPqRstR;
    @FXML private TableColumn<QsoRecord,String>    colPqState;
    @FXML private TableColumn<QsoRecord,String>    colPqNotes;
    private final ObservableList<QsoRecord>        prevQsosData = FXCollections.observableArrayList();
    /** Debounce timer for the live callsign → DB lookup. */
    private javafx.animation.PauseTransition       prevQsosDebounce;

    // ---- DX Spotting ----
    @FXML private SplitPane  mainSplitPane;
    @FXML private TitledPane dxPane;
    @FXML private AnchorPane dxContainer;

    // ---- Heard-By (PSK Reporter / WSPR) ----
    @FXML private TitledPane                 heardByPane;
    @FXML private TableView<HeardBySpot>     heardByTable;
    @FXML private TableColumn<HeardBySpot,String> colHbTime;
    @FXML private TableColumn<HeardBySpot,String> colHbReporter;
    @FXML private TableColumn<HeardBySpot,String> colHbGrid;
    @FXML private TableColumn<HeardBySpot,String> colHbBand;
    @FXML private TableColumn<HeardBySpot,String> colHbMode;
    @FXML private TableColumn<HeardBySpot,String> colHbSnr;
    @FXML private TableColumn<HeardBySpot,String> colHbSource;
    private final ObservableList<HeardBySpot> heardByData = FXCollections.observableArrayList();
    private static final int HEARD_BY_MAX_ROWS = 200;

    // ---- Status / clock bar ----
    @FXML private Label lblStatus;
    @FXML private Label lblCivStatus;
    @FXML private Label lblAmpStatus;
    @FXML private Label lblAntStatus;
    @FXML private Label lblStationCall;

    // ---- Band / mode warning ----
    @FXML private Label lblBandWarning;
    private final PauseTransition freqValidateDelay = new PauseTransition(Duration.millis(400));

    // ---- Macro buttons ----
    @FXML private HBox macroButtonBar;

    // ---- POTA/SOTA activation bar ----
    @FXML private HBox      activationBar;
    @FXML private TextField tfActivationSig;
    @FXML private TextField tfActivationSigInfo;
    @FXML private Label     lblActivationStatus;

    private volatile String currentSig;
    private volatile String currentSigInfo;

    private final ObservableList<QsoRecord> qsoData = FXCollections.observableArrayList();
    private ScheduledExecutorService clockService;
    private QrzLookup qrzLookup;
    private int qsoPageOffset = 0;
    private static final int PAGE_SIZE = 20;

    private DxSpotController dxSpotController;

    // Divider position captured when DX pane was last expanded; restored on re-expand
    private double dxExpandedDividerPos = 0.5;

    private static final DateTimeFormatter UTC_FMT   = DateTimeFormatter.ofPattern("HH:mm:ss 'UTC'");
    private static final DateTimeFormatter LOCAL_FMT  = DateTimeFormatter.ofPattern("HH:mm:ss z");
    private static final DateTimeFormatter TABLE_FMT  = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initEntryFields();
        initTable();
        initHeardByTable();
        initKeyer();
        initClock();
        initCivListeners();
        initMacroBar();
        initKeyHandlers();
        initBandWarning();
        loadQsos();
        initQrzLookup();

        Platform.runLater(this::restoreDividers);

        if (AppConfig.getInstance().getCivAutoConnect()) {
            connectCiv();
        }

        HubEngine.getInstance().setSpotSelectedListener(spot ->
            Platform.runLater(() -> fillFromSpot(spot)));

        HubEngine.getInstance().setLogEntryDraftListener(node ->
            Platform.runLater(() -> fillFromLogDraft(node)));

        HubEngine.getInstance().setCallsignResultListener(node ->
            Platform.runLater(() -> fillFromCallsignResult(node)));

        HubEngine.getInstance().setSolarFluxListener(node ->
            Platform.runLater(() -> updateSolarLabel(node)));

        HubEngine.getInstance().setHeardBySpotListener(node ->
            Platform.runLater(() -> addHeardBySpot(node)));

        // AMP_STATUS — when configured, j-hub publishes amp telemetry every poll cycle.
        // The label stays hidden until the first message arrives so unconfigured rigs
        // see no extra UI clutter (graceful degradation per P3 spec).
        HubEngine.getInstance().setAmpStatusListener(node ->
            Platform.runLater(() -> updateAmpStatus(node)));

        // ANT_STATUS — j-hub publishes whenever a switch position changes or
        // the PTT lockout flips. Stays hidden until the first message arrives
        // so unconfigured stations see no extra status-bar clutter.
        HubEngine.getInstance().setAntStatusListener(node ->
            Platform.runLater(() -> updateAntStatus(node)));

        HubEngine.getInstance().setAdifImportListener(node -> {
            String path = node.path("path").asText("");
            if (path.isBlank()) return;
            Task<com.jlog.export.AdifImporter.Result> task = new Task<>() {
                @Override protected com.jlog.export.AdifImporter.Result call() throws Exception {
                    return com.jlog.export.AdifImporter.importAdif(
                        java.nio.file.Path.of(path),
                        com.jlog.export.AdifImporter.DupeMode.SKIP);
                }
                @Override protected void succeeded() {
                    var r = getValue();
                    setStatus("ADIF import: " + r.imported + " imported, " + r.skipped + " skipped");
                    loadQsos();
                    try { java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(path)); }
                    catch (Exception ignored) {}
                }
                @Override protected void failed() {
                    setStatus("ADIF import failed: " + getException().getMessage());
                }
            };
            new Thread(task).start();
        });

        // Space Weather pane — visibility driven by jLogSettings.showSpaceWeather
        // in j-hub.json. Default true so the pane appears as soon as data arrives.
        boolean showSpaceWx = readJLogBool("showSpaceWeather", true);
        if (spaceWxPane != null) {
            spaceWxPane.setVisible(showSpaceWx);
            spaceWxPane.setManaged(showSpaceWx);
        }
        // Prime the info-pane values with em-dashes so the pane isn't blank
        // until the first solar broadcast arrives (~10–20 seconds after hub start).
        if (lblSolarSfi  != null) lblSolarSfi .setText("—");
        if (lblSolarK    != null) lblSolarK   .setText("—");
        if (lblSolarSsn  != null) lblSolarSsn .setText("—");
        if (lblSolarXray != null) lblSolarXray.setText("—");
        if (lblSolarWind != null) lblSolarWind.setText("—");
        if (lblSolarGeo  != null) lblSolarGeo .setText("—");

        setStatus(I18n.get("status.ready"));
        updateStationCallLabel();
        refreshSaveButton();
    }

    /** Read a boolean from jLogSettings in j-hub.json. Returns {@code dflt} if
     *  the file is unreadable or the key is missing. */
    private static boolean readJLogBool(String key, boolean dflt) {
        String home = System.getProperty("user.home", "");
        java.nio.file.Path[] candidates = {
            java.nio.file.Paths.get(home, "ARS_Suite", "j-hub", "j-hub.json"),
            java.nio.file.Paths.get(".", "j-hub.json"),
        };
        for (java.nio.file.Path p : candidates) {
            if (!java.nio.file.Files.isReadable(p)) continue;
            try {
                com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(p.toFile());
                com.fasterxml.jackson.databind.JsonNode v = root.path("jLogSettings").path(key);
                if (!v.isMissingNode() && v.isBoolean()) return v.asBoolean();
            } catch (Exception ignored) {}
        }
        return dflt;
    }

    /** Render the latest AMP_STATUS into the status bar label. Stays hidden until
     *  the first message arrives, then becomes visible for the rest of the session.
     *  Faulted state shows a red background and a "FAULT — &lt;reason&gt;" banner per
     *  the P3 spec's "clear visual indicator" requirement. */
    private void updateAmpStatus(JsonNode node) {
        if (node == null || lblAmpStatus == null) return;

        boolean faulted     = node.path("faulted").asBoolean(false);
        String  faultReason = node.path("faultReason").asText("");
        String  band        = node.path("band").asText("");
        long    freqHz      = node.path("frequency").asLong(0);
        double  swr         = node.path("swr").asDouble(Double.NaN);
        double  pwr         = node.path("fwdPower").asDouble(Double.NaN);
        String  powerstat   = node.path("powerstat").asText("UNKNOWN");

        StringBuilder sb = new StringBuilder("AMP: ");
        if (faulted) {
            sb.append("FAULT — ").append(faultReason.isEmpty() ? "see amp panel" : faultReason);
        } else {
            if (freqHz > 0) sb.append(String.format("%.3f MHz", freqHz / 1_000_000.0));
            if (!band.isEmpty()) sb.append(' ').append(band);
            if (!Double.isNaN(swr)) sb.append(String.format(" | SWR %.1f", swr));
            if (!Double.isNaN(pwr)) sb.append(String.format(" | %.0f W", pwr));
            if (!"UNKNOWN".equals(powerstat)) sb.append(" | ").append(powerstat);
        }

        lblAmpStatus.setText(sb.toString());
        // Make sure the label is visible the first time a status arrives.
        if (!lblAmpStatus.isVisible()) {
            lblAmpStatus.setVisible(true);
            lblAmpStatus.setManaged(true);
        }
        // Inline style so we don't need a CSS round-trip; latches red on fault,
        // clears when the next non-faulted status arrives.
        if (faulted) {
            lblAmpStatus.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-padding: 2 8 2 8;");
        } else {
            lblAmpStatus.setStyle("");
        }
    }

    /** Render the latest ANT_STATUS into the status bar. Hidden until the first
     *  message arrives. PTT lockout shows "ANT: LOCKED"; an active operator
     *  override shows "ANT: SW1=2*" (asterisk = override); otherwise a brief
     *  "ANT: SW1=2 SW2=1" rollup of the current selections. */
    private void updateAntStatus(JsonNode node) {
        if (node == null || lblAntStatus == null) return;
        boolean locked  = node.path("locked").asBoolean(false);
        boolean faulted = node.path("faulted").asBoolean(false);
        StringBuilder sb = new StringBuilder("ANT: ");
        if (locked) {
            sb.append("LOCKED");
        } else {
            JsonNode switches = node.path("switches");
            boolean first = true;
            for (var it = switches.fields(); it.hasNext(); ) {
                var entry = it.next();
                if (!first) sb.append(' ');
                first = false;
                int  ant = entry.getValue().path("antenna").asInt(0);
                boolean ov = entry.getValue().path("overridden").asBoolean(false);
                sb.append(entry.getKey()).append('=').append(ant);
                if (ov) sb.append('*');
            }
            if (first) sb.append("(none)");
        }
        lblAntStatus.setText(sb.toString());
        if (!lblAntStatus.isVisible()) {
            lblAntStatus.setVisible(true);
            lblAntStatus.setManaged(true);
        }
        if (faulted) {
            lblAntStatus.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white; -fx-padding: 2 8 2 8;");
        } else if (locked) {
            lblAntStatus.setStyle("-fx-background-color: #f39c12; -fx-text-fill: black; -fx-padding: 2 8 2 8;");
        } else {
            lblAntStatus.setStyle("");
        }
    }

    /** Tools-menu action: prompt for switchId + antenna and send an override. */
    @FXML private void doAntennaOverride() {
        TextInputDialog d = new TextInputDialog("main:2");
        d.setTitle("Antenna Override");
        d.setHeaderText("Force-select an antenna until cleared");
        d.setContentText("Enter switchId:antenna (e.g. main:2):");
        d.showAndWait().ifPresent(input -> {
            String[] parts = input.split(":");
            if (parts.length != 2) return;
            try {
                HubEngine.getInstance().sendAntOverride(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {}
        });
    }

    /** Tools-menu action: drop all antenna overrides; rules take over again. */
    @FXML private void doAntennaOverrideClear() {
        HubEngine.getInstance().sendAntOverrideClear(null);
    }


    private void updateSolarLabel(JsonNode node) {
        if (node == null) return;
        // Fields match the SOLAR_FLUX payload built by j-hub SolarFluxService
        // from the NOAA-backed WeatherService cache.
        String sfi  = node.hasNonNull("sfi")       ? node.get("sfi")      .asText() : "—";
        String k    = node.hasNonNull("k")         ? node.get("k")        .asText() : "—";
        String sn   = node.hasNonNull("sn")        ? node.get("sn")       .asText() : "—";
        String xray = node.hasNonNull("xray")      ? node.get("xray")     .asText() : "—";
        String wind = node.hasNonNull("solarwind") ? node.get("solarwind").asText() : "—";
        String geo  = node.hasNonNull("geomag")    ? node.get("geomag")   .asText() : "—";

        // Info-pane grid — each value lives in its own cell with its own label,
        // so the eye lands on the number instead of hunting through a string.
        if (lblSolarSfi  != null) lblSolarSfi .setText(sfi);
        if (lblSolarK    != null) lblSolarK   .setText(k);
        if (lblSolarSsn  != null) lblSolarSsn .setText(sn);
        if (lblSolarXray != null) lblSolarXray.setText(xray);
        if (lblSolarWind != null) lblSolarWind.setText(wind.equals("—") ? wind : wind + " km/s");
        if (lblSolarGeo  != null) lblSolarGeo .setText(geo);
    }

    // ---------------------------------------------------------------
    // Initialisation helpers
    // ---------------------------------------------------------------

    private static final List<String> VALID_BANDS = BandPlan.allBands();
    /** Mirrors the ADIF mode list + common aliases the contest engine accepts
     *  (see {@code BandPlan.isDigital} / {@code AdifExporter}). Kept broad so
     *  operators aren't fighting the UI. */
    private static final Set<String>  VALID_MODES = Set.of(
        "SSB","USB","LSB","AM","FM","DV","CW","CW-R","RTTY","FSK-R",
        "FT8","FT4","PSK31","PSK63","PSK125","OLIVIA","CONTESTI","MFSK",
        "JS8","JT65","JT9","MSK144","Q65","PACKET","PKT","HELL","THROB",
        "DIGITALVOICE","DOMINOEX","FREEDV","ARDOP","VARA");

    private void initEntryFields() {
        tfRstSent.setText("599");
        tfRstReceived.setText("599");
        tfPower.setText(AppConfig.getInstance().getDefaultPower());
        tfBand.textProperty().addListener((obs, o, n) -> {
            applyValidStyle(tfBand, isBandValid(n));
            refreshSaveButton();
        });
        tfMode.textProperty().addListener((obs, o, n) -> {
            applyValidStyle(tfMode, isModeValid(n));
            refreshSaveButton();
        });

        // Every field that lands in the QSO database is forced to upper case
        // (ham log convention). Numeric fields (RST/power/frequency) are left
        // alone — uppercasing digits is a no-op. Connection/auth fields live in
        // Setup and are intentionally not touched here.
        forceUpperCase(tfCallsign);
        forceUpperCase(tfBand);
        forceUpperCase(tfMode);
        forceUpperCase(tfState);
        forceUpperCase(tfCounty);
        forceUpperCase(tfCountry);
        forceUpperCase(tfOperatorName);
        forceUpperCase(tfNotes);
        if (tfActivationSig != null)     forceUpperCase(tfActivationSig);
        if (tfActivationSigInfo != null) forceUpperCase(tfActivationSigInfo);

        tfCallsign.textProperty().addListener((obs, o, n) -> refreshSaveButton());
    }

    /** "20m" → true; "20" → true (accept bare number); case-insensitive. */
    private static boolean isBandValid(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toLowerCase();
        if (s.isEmpty()) return false;
        for (String b : VALID_BANDS) {
            if (b.equalsIgnoreCase(s)) return true;
            if (b.toLowerCase().replaceAll("[a-z]", "").equals(s)) return true;  // "20" matches "20m"
        }
        return false;
    }

    private static boolean isModeValid(String raw) {
        if (raw == null) return false;
        String s = raw.trim().toUpperCase();
        return !s.isEmpty() && VALID_MODES.contains(s);
    }

    private static void applyValidStyle(TextField tf, boolean valid) {
        String txt = tf.getText() == null ? "" : tf.getText().trim();
        if (txt.isEmpty() || valid) tf.getStyleClass().remove("field-invalid");
        else if (!tf.getStyleClass().contains("field-invalid")) tf.getStyleClass().add("field-invalid");
    }

    /** Live caret-safe uppercase transform for ham-convention fields. */
    private static void forceUpperCase(TextField tf) {
        tf.textProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String up = n.toUpperCase();
            if (!up.equals(n)) {
                int caret = tf.getCaretPosition();
                tf.setText(up);
                tf.positionCaret(Math.min(caret, up.length()));
            }
        });
    }

    /** Returns true when the form is in a saveable state: callsign is required,
     *  band/mode are optional but when filled must be recognized values. Empty
     *  band/mode is allowed so quick logs don't need both typed first. */
    private boolean isFormValid() {
        if (tfCallsign.getText() == null || tfCallsign.getText().trim().isEmpty()) return false;
        String b = tfBand.getText();
        if (b != null && !b.trim().isEmpty() && !isBandValid(b)) return false;
        String m = tfMode.getText();
        if (m != null && !m.trim().isEmpty() && !isModeValid(m)) return false;
        return true;
    }

    private void refreshSaveButton() {
        if (btnSave != null) btnSave.setDisable(!isFormValid());
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
        qsoTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        initSortDropdown();
        initPrevQsosPanel();

        // Center-align every column except Notes (which reads as prose and
        // wants left-alignment for scanability).
        for (TableColumn<QsoRecord, ?> c : qsoTable.getColumns()) {
            if (c != colNotes) c.setStyle("-fx-alignment: CENTER;");
        }
        qsoTable.setRowFactory(tv -> {
            TableRow<QsoRecord> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    QsoRecord r = row.getItem();
                    editingRecord = r;
                    populateFromRecord(r);
                    refreshSaveButton();
                }
            });
            return row;
        });
    }

    // ---------------------------------------------------------------
    // Sort dropdown — explicit "Newest / Oldest first" over the log table.
    // ---------------------------------------------------------------
    private void initSortDropdown() {
        if (cbSortOrder == null) return;
        cbSortOrder.setItems(FXCollections.observableArrayList(SORT_NEWEST, SORT_OLDEST));
        cbSortOrder.getSelectionModel().select(SORT_NEWEST);          // matches DAO default
        cbSortOrder.valueProperty().addListener((obs, o, n) -> resortQsoData());
    }

    /** Re-sort {@link #qsoData} in place according to {@link #cbSortOrder}.
     *  Nulls (corrupt rows with no datetime) trail at the end so the sort
     *  never throws. Called both on dropdown change and after any reload. */
    private void resortQsoData() {
        if (cbSortOrder == null) return;
        String pick = cbSortOrder.getValue();
        java.util.Comparator<QsoRecord> cmp = java.util.Comparator.comparing(
            QsoRecord::getDateTimeUtc,
            java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));
        if (SORT_NEWEST.equals(pick)) cmp = cmp.reversed();
        FXCollections.sort(qsoData, cmp);
    }

    // ---------------------------------------------------------------
    // Previous-QSOs panel — live DB lookup keyed on the callsign field.
    // Debounced 300ms so a fast typist doesn't fire a query per keystroke.
    // ---------------------------------------------------------------
    private void initPrevQsosPanel() {
        if (prevQsosTable == null) return;
        colPqDate .setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateTimeUtc() != null ? c.getValue().getDateTimeUtc().format(TABLE_FMT) : ""));
        colPqBand .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBand()));
        colPqMode .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMode()));
        colPqRstS .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstSent()));
        colPqRstR .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstReceived()));
        colPqState.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getState()));
        colPqNotes.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNotes()));
        prevQsosTable.setItems(prevQsosData);
        for (TableColumn<QsoRecord, ?> c : prevQsosTable.getColumns()) {
            if (c != colPqNotes) c.setStyle("-fx-alignment: CENTER;");
        }

        prevQsosDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        prevQsosDebounce.setOnFinished(e -> refreshPrevQsos());
        if (tfCallsign != null) {
            tfCallsign.textProperty().addListener((obs, o, n) -> prevQsosDebounce.playFromStart());
        }
        refreshPrevQsos();   // initial state — usually empty
    }

    /** Re-query the QSO DB for any previous contacts with the call currently
     *  in {@link #tfCallsign}. Updates the panel title to show the call and
     *  match count. Runs on the JavaFX thread — query is local SQLite and
     *  takes <1ms for a personal-sized log; if logs ever balloon this could
     *  move to a daemon. */
    private void refreshPrevQsos() {
        if (prevQsosTable == null || tfCallsign == null) return;
        String call = tfCallsign.getText();
        if (call != null) call = call.trim().toUpperCase();
        if (call == null || call.isEmpty()) {
            prevQsosData.clear();
            if (prevQsosPane != null) prevQsosPane.setText("Previous QSOs");
            return;
        }
        try {
            java.util.List<QsoRecord> matches =
                com.jlog.db.QsoDao.getInstance().findByCallsign(call);
            prevQsosData.setAll(matches);
            if (prevQsosPane != null) {
                prevQsosPane.setText("Previous QSOs with " + call + " (" + matches.size() + ")");
            }
        } catch (Exception ex) {
            prevQsosData.clear();
            if (prevQsosPane != null) prevQsosPane.setText("Previous QSOs — lookup failed");
        }
    }

    // ---------------------------------------------------------------
    // CW / Digital keyer — embedded pane (replaces the old popup window).
    // Receives decoded text via MODEM_DECODE; right-click selection → callsign.
    // ---------------------------------------------------------------

    private static final DateTimeFormatter KEYER_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private void initKeyer() {
        cbKeyerMode.setItems(FXCollections.observableArrayList(
            "CW", "RTTY", "PSK31", "PSK63", "FT8", "FT4"));
        cbKeyerMode.getSelectionModel().select("CW");

        // CW WPM spinner — visible only in CW mode, persisted via AppConfig.
        // Range matches Hamlib's KEYSPD level limits on most rigs (5–60 WPM).
        int initialWpm = AppConfig.getInstance().getCwWpm();
        spCwWpm.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 60, initialWpm));
        spCwWpm.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) AppConfig.getInstance().setCwWpm(newV);
        });
        Runnable applyCwVisibility = () -> {
            boolean cw = "CW".equals(cbKeyerMode.getValue());
            lblCwWpm.setVisible(cw); lblCwWpm.setManaged(cw);
            spCwWpm .setVisible(cw); spCwWpm .setManaged(cw);
        };
        applyCwVisibility.run();
        cbKeyerMode.valueProperty().addListener((obs, oldV, newV) -> applyCwVisibility.run());

        taKeyerRx.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");

        // Enter in TX field = Send (Ctrl+Enter also works)
        tfKeyerTx.setOnAction(e -> doKeyerSend());

        // Right-click context menu on the RX area — copy selection into callsign,
        // operator name, or state fields in the entry form. No selection = no-op.
        ContextMenu ctx = new ContextMenu();
        MenuItem miCall  = new MenuItem("Use selection as Callsign");
        MenuItem miName  = new MenuItem("Use selection as Name");
        MenuItem miState = new MenuItem("Use selection as State");
        MenuItem miClear = new MenuItem("Clear RX");
        miCall .setOnAction(e -> applySelection(tfCallsign, true));
        miName .setOnAction(e -> applySelection(tfOperatorName, false));
        miState.setOnAction(e -> applySelection(tfState, true));
        miClear.setOnAction(e -> taKeyerRx.clear());
        ctx.getItems().addAll(miCall, miName, miState, new SeparatorMenuItem(), miClear);
        taKeyerRx.setContextMenu(ctx);

        // Double-click in RX auto-detects a callsign-shaped token under the cursor
        // and fills tfCallsign — saves the right-click for quick logging.
        taKeyerRx.setOnMouseClicked(e -> {
            if (e.getClickCount() != 2) return;
            String sel = taKeyerRx.getSelectedText();
            if (sel == null || sel.isBlank()) return;
            String token = sel.trim().toUpperCase();
            if (token.matches("[A-Z0-9]{3,8}") && token.matches(".*\\d.*")) {
                tfCallsign.setText(token);
            }
        });

        // MODEM_DECODE → append to RX
        HubEngine.getInstance().setModemDecodeListener(node ->
            Platform.runLater(() -> onKeyerDecode(node)));

        // Supply the live QSO context to MacroEngine so CW_TEXT macros can
        // expand {CALL}, {MYCALL}, {RST_S}, {RST_R}, {NAME}, {EXCH}, etc.
        MacroEngine.getInstance().setVariableContextSupplier(this::buildMacroContext);

        // One-shot notice when the configured rig doesn't support Hamlib CW —
        // surfaced in the keyer status label, then suppressed for the session.
        HubEngine.getInstance().setCwUnsupportedListener(reason ->
            Platform.runLater(() -> {
                if (lblKeyerStatus != null) {
                    lblKeyerStatus.setText("Rig does not support Hamlib CW — using CI-V fallback");
                }
            }));
    }

    /** Snapshot the entry-bar fields and rig state into a MacroVariableEngine
     *  context. Called from MacroEngine on every CW_TEXT expansion, so reads
     *  must be cheap and side-effect-free. */
    private MacroVariableEngine.Context buildMacroContext() {
        MacroVariableEngine.Context ctx = new MacroVariableEngine.Context();
        ctx.myCall      = AppConfig.getInstance().getStationCallsign();
        ctx.dxCall      = tfCallsign       != null ? tfCallsign.getText()       : "";
        ctx.rstSent     = tfRstSent        != null ? tfRstSent.getText()        : "";
        ctx.rstReceived = tfRstReceived    != null ? tfRstReceived.getText()    : "";
        ctx.name        = tfOperatorName   != null ? tfOperatorName.getText()   : "";
        ctx.exchange    = tfNotes          != null ? tfNotes.getText()          : "";
        ctx.frequencyHz = CivEngine.getInstance().getCurrentFrequencyHz();
        ctx.mode        = (cbKeyerMode != null && cbKeyerMode.getValue() != null)
                          ? cbKeyerMode.getValue() : "";
        return ctx;
    }

    @FXML private void toggleKeyerPane() {
        boolean show = (miToggleKeyer != null) ? miToggleKeyer.isSelected()
                                               : !(keyerPane != null && keyerPane.isVisible());
        if (keyerPane != null) {
            keyerPane.setVisible(show);
            keyerPane.setManaged(show);
        }
        if (miToggleKeyer != null) miToggleKeyer.setSelected(show);
        if (show && tfKeyerTx != null) tfKeyerTx.requestFocus();
    }

    @FXML private void doKeyerSend() {
        if (tfKeyerTx == null) return;
        String text = tfKeyerTx.getText();
        if (text == null || text.isBlank()) return;
        String mode = cbKeyerMode.getValue();
        HubEngine.getInstance().sendModemTx(mode, text);
        String line = String.format("[%s TX %s] %s%n",
            LocalTime.now().format(KEYER_TIME_FMT), mode, text.trim());
        taKeyerRx.appendText(line);
        taKeyerRx.setScrollTop(Double.MAX_VALUE);
        if (lblKeyerStatus != null) lblKeyerStatus.setText("sent " + text.length() + " chars");
        tfKeyerTx.clear();
        tfKeyerTx.requestFocus();
    }

    @FXML private void doKeyerClear() {
        if (tfKeyerTx != null) tfKeyerTx.clear();
    }

    private void onKeyerDecode(JsonNode node) {
        if (taKeyerRx == null) return;
        String text = node.path("text").asText("");
        if (text == null || text.isBlank()) return;
        String mode = node.path("mode").asText("");
        double snr  = node.path("snr").asDouble(Double.NaN);
        String prefix = String.format("[%s RX %s%s] ",
            LocalTime.now().format(KEYER_TIME_FMT),
            mode.isBlank() ? "?" : mode,
            Double.isNaN(snr) ? "" : String.format(" %+.1fdB", snr));
        taKeyerRx.appendText(prefix + text + "\n");
        taKeyerRx.setScrollTop(Double.MAX_VALUE);
    }

    /** Copy RX-area selected text into the given form field. When {@code upper}
     *  is true the value is upper-cased (callsigns, states). */
    private void applySelection(TextField target, boolean upper) {
        if (target == null || taKeyerRx == null) return;
        String sel = taKeyerRx.getSelectedText();
        if (sel == null || sel.isBlank()) return;
        target.setText(upper ? sel.trim().toUpperCase() : sel.trim());
    }

    private void initHeardByTable() {
        colHbTime    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().time));
        colHbReporter.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reporter));
        colHbGrid    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().grid));
        colHbBand    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().band));
        colHbMode    .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().mode));
        colHbSnr     .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().snr));
        colHbSource  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().source));
        heardByTable.setItems(heardByData);
    }

    /** Shape of a row in the Heard-By table. */
    private static final class HeardBySpot {
        final String time, reporter, grid, band, mode, snr, source;
        HeardBySpot(String time, String reporter, String grid, String band,
                    String mode, String snr, String source) {
            this.time = time; this.reporter = reporter; this.grid = grid;
            this.band = band; this.mode = mode; this.snr = snr; this.source = source;
        }
    }

    private void addHeardBySpot(JsonNode node) {
        if (node == null) return;
        String ts   = node.path("timestampUtc").asText("");
        String time = ts.length() >= 16 ? ts.substring(11, 16) : ts;   // HH:mm
        String band = node.path("band").asText("");
        if (band.isBlank()) {
            long hz = node.path("frequencyHz").asLong(0);
            if (hz > 0) {
                String b = CivEngine.freqToBand(hz);
                if (b != null) band = b;
            }
        }
        HeardBySpot row = new HeardBySpot(
            time,
            node.path("receiverCall").asText(""),
            node.path("receiverGrid").asText(""),
            band,
            node.path("mode").asText(""),
            Integer.toString(node.path("snr").asInt(0)),
            node.path("source").asText("")
        );
        heardByData.add(0, row);   // newest first
        while (heardByData.size() > HEARD_BY_MAX_ROWS) {
            heardByData.remove(heardByData.size() - 1);
        }
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
            if (band != null) tfBand.setText(band);
            checkBandWarning();
        }));
        civ.setModeListener(mode -> Platform.runLater(() -> {
            if (mode != null) tfMode.setText(mode);
            checkBandWarning();
        }));
    }

    private void initMacroBar() {
        MacroEngine engine = MacroEngine.getInstance();
        engine.setExchangeInsertHandler(text -> {
            if (tfNotes.isFocused()) tfNotes.setText(text);
        });
        engine.setAutofillHandler(this::doCallsignLookup);

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
                    // ESC aborts any in-flight CW transmission AND voice macro
                    // (Hamlib stop_morse + WAV stop + PTT release). Filter
                    // rather than handler so it works regardless of focus.
                    if (e.getCode() == KeyCode.ESCAPE) {
                        MacroEngine.getInstance().abortCw();
                        MacroEngine.getInstance().abortVoice();
                    }
                });
            }
        });

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

    /**
     * Restores the main SplitPane divider to its last persisted position and
     * wires the DX pane expand/collapse listener so the divider height is
     * preserved across minimise/restore cycles.
     */
    private void restoreDividers() {
        AppConfig cfg = AppConfig.getInstance();
        double saved = cfg.getDivider("normalLog.div0", 0.5);
        dxExpandedDividerPos = saved;
        mainSplitPane.setDividerPositions(saved);

        // Persist divider changes only while the DX pane is open
        mainSplitPane.getDividers().forEach(div ->
            div.positionProperty().addListener((o, ov, nv) -> {
                if (dxPane.isExpanded()) {
                    dxExpandedDividerPos = nv.doubleValue();
                    cfg.setDivider("normalLog.div0", dxExpandedDividerPos);
                }
            }));

        // Capture height before collapse; restore it on re-expand
        dxPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (!isExpanded) {
                dxExpandedDividerPos = mainSplitPane.getDividerPositions()[0];
                cfg.setDivider("normalLog.div0", dxExpandedDividerPos);
            } else {
                final double target = dxExpandedDividerPos;
                Platform.runLater(() -> mainSplitPane.setDividerPositions(target));
            }
        });
    }

    // ---------------------------------------------------------------
    // Data entry actions
    // ---------------------------------------------------------------

    @FXML private void doSave() {
        if (!isFormValid()) {
            // Refresh per-field styles so the user sees which fields are red
            applyValidStyle(tfBand, isBandValid(tfBand.getText()));
            applyValidStyle(tfMode, isModeValid(tfMode.getText()));
            String reason = (tfCallsign.getText() == null || tfCallsign.getText().trim().isEmpty())
                ? I18n.get("error.callsign.required")
                : "Band or mode invalid — check highlighted fields";
            setStatus(reason);
            return;
        }

        final boolean isUpdate = (editingRecord != null);
        final QsoRecord q;
        if (isUpdate) {
            // Overlay form fields onto the existing record — preserves id and
            // original dateTimeUtc so edits don't rewrite the QSO timestamp.
            q = editingRecord;
            applyFieldsTo(q);
        } else {
            q = buildRecord();
        }
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (isUpdate) QsoDao.getInstance().update(q);
                else          QsoDao.getInstance().insert(q);
                return null;
            }
            @Override protected void succeeded() {
                setStatus(isUpdate
                    ? I18n.get("status.updated")
                    : I18n.get("status.saved", q.getCallsign()));
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
        editingRecord = null;
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
        refreshSaveButton();
        tfCallsign.requestFocus();
    }

    @FXML private void doCallsignLookup() {
        String call = tfCallsign.getText().trim().toUpperCase();
        if (call.isEmpty()) return;
        // Route through hub so all apps get CALLSIGN_RESULT; falls back to QRZ if hub unavailable
        if (HubEngine.getInstance().isConnected()) {
            HubEngine.getInstance().sendCallsignLookup(call);
        } else if (qrzLookup != null) {
            Task<Map<String, String>> task = new Task<>() {
                @Override protected Map<String,String> call() { return qrzLookup.lookup(call); }
                @Override protected void succeeded() { applyQrzData(getValue()); }
            };
            new Thread(task).start();
        }
    }

    private void applyQrzData(Map<String, String> data) {
        if (data == null || data.isEmpty()) return;
        if (!data.getOrDefault("fullname", "").isEmpty()) tfOperatorName.setText(data.get("fullname"));
        if (!data.getOrDefault("country",  "").isEmpty()) { tfCountry.setText(data.get("country")); lblCountry.setText(data.get("country")); }
        if (!data.getOrDefault("state",    "").isEmpty()) tfState.setText(data.get("state"));
        if (!data.getOrDefault("county",   "").isEmpty()) tfCounty.setText(data.get("county"));
        try {
            double myLat = Double.parseDouble(AppConfig.getInstance().getLatitude());
            double myLon = Double.parseDouble(AppConfig.getInstance().getLongitude());
            double dxLat = Double.parseDouble(data.getOrDefault("lat", "0"));
            double dxLon = Double.parseDouble(data.getOrDefault("lon", "0"));
            lblBearing.setText(String.format("%.0f°",  QrzLookup.bearing(myLat, myLon, dxLat, dxLon)));
            lblDistance.setText(String.format("%.0f km", QrzLookup.distanceKm(myLat, myLon, dxLat, dxLon)));
        } catch (Exception ignored) {}
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
        if (sel != null) {
            editingRecord = sel;
            populateFromRecord(sel);
            refreshSaveButton();
        }
    }

    /** "Save Edit" delegates to the main save path, which branches on
     *  editingRecord so insert vs. update is one code path. */
    @FXML private void doSaveEdited() {
        doSave();
    }

    @FXML private void doDeleteSelected() {
        // Copy the selection list up-front — the confirmation dialog can lose
        // focus and clear the selection on some window managers. Also supports
        // bulk delete when multiple rows are selected.
        java.util.List<QsoRecord> targets =
            new java.util.ArrayList<>(qsoTable.getSelectionModel().getSelectedItems());
        if (targets.isEmpty()) {
            setStatus("Select a QSO first.");
            return;
        }
        String msg = targets.size() == 1
            ? "Delete QSO with " + targets.get(0).getCallsign() + "?"
            : "Delete " + targets.size() + " selected QSOs?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, msg,
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.initOwner(getStage());
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    for (QsoRecord r : targets) QsoDao.getInstance().delete(r.getId());
                    return null;
                }
                @Override protected void succeeded() {
                    setStatus("Deleted " + targets.size() + " QSO(s).");
                    doClear();
                    loadQsos();
                }
                @Override protected void failed() {
                    Throwable ex = getException();
                    setStatus("Delete failed: "
                        + (ex != null ? ex.getClass().getSimpleName() + ": " + ex.getMessage() : "unknown"));
                }
            };
            new Thread(task).start();
        });
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
                com.jlog.export.AdifExporter.exportAdif(f.toPath());
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
                com.jlog.export.AdifExporter.exportCsv(f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuImportAdif() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Import ADIF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ADIF Files", "*.adi","*.adif"));
        File f = fc.showOpenDialog(getStage());
        if (f == null) return;

        // Three choices: Skip / Overwrite / Append
        Alert dupeChoice = new Alert(Alert.AlertType.CONFIRMATION,
            "How should duplicates be handled?\n\n"
          + "Skip — keep existing, drop incoming.\n"
          + "Append — insert all rows, even duplicates.\n"
          + "Cancel — abort import.",
            new ButtonType("Skip"), new ButtonType("Append"), ButtonType.CANCEL);
        dupeChoice.setHeaderText("Duplicate handling");
        ButtonType choice = dupeChoice.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == ButtonType.CANCEL) return;
        final com.jlog.export.AdifImporter.DupeMode mode =
            "Append".equals(choice.getText())
                ? com.jlog.export.AdifImporter.DupeMode.APPEND
                : com.jlog.export.AdifImporter.DupeMode.SKIP;

        Task<com.jlog.export.AdifImporter.Result> task = new Task<>() {
            @Override protected com.jlog.export.AdifImporter.Result call() throws Exception {
                return com.jlog.export.AdifImporter.importAdif(f.toPath(), mode);
            }
            @Override protected void succeeded() {
                var r = getValue();
                setStatus("Imported " + r.imported + ", skipped " + r.skipped
                          + ", failed " + r.failed);
                loadQsos();
            }
            @Override protected void failed() { setStatus("Import failed: " + getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuLotwUpload() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Sign the full log and upload to LoTW?\n\n"
          + "Uses tqsl with station location: "
          + AppConfig.getInstance().getLotwStationLocation(),
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("LoTW Upload");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Task<com.jlog.export.LotwService.TqslResult> task = new Task<>() {
            @Override protected com.jlog.export.LotwService.TqslResult call() throws Exception {
                return com.jlog.export.LotwService.signAndUpload(
                    com.jlog.db.QsoDao.getInstance().fetchAll());
            }
            @Override protected void succeeded() {
                var r = getValue();
                setStatus(r.ok() ? "LoTW upload succeeded" : "LoTW upload failed (exit " + r.exitCode + ")");
                if (!r.ok() && !r.stderr.isBlank()) {
                    new Alert(Alert.AlertType.ERROR, r.stderr).showAndWait();
                }
            }
            @Override protected void failed() { setStatus("LoTW: " + getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuLotwDownload() {
        Task<com.jlog.export.LotwService.DownloadResult> task = new Task<>() {
            @Override protected com.jlog.export.LotwService.DownloadResult call() throws Exception {
                return com.jlog.export.LotwService.downloadConfirmations(null);
            }
            @Override protected void succeeded() {
                var r = getValue();
                if (r.ok()) {
                    setStatus("LoTW confirmations: " + r.matched + " matched, " + r.unmatched + " unmatched");
                    loadQsos();
                } else {
                    setStatus("LoTW download failed (exit " + r.exitCode + ")");
                    if (!r.stderr.isBlank()) new Alert(Alert.AlertType.ERROR, r.stderr).showAndWait();
                }
            }
            @Override protected void failed() { setStatus("LoTW: " + getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuAwards() {
        try {
            new AwardsWindow(getStage()).show();
        } catch (Throwable t) {
            // Surface the error instead of swallowing — the menu was silently
            // no-op'ing before when anything upstream threw.
            Alert a = new Alert(Alert.AlertType.ERROR,
                "Could not open Awards Dashboard:\n" + t.getClass().getSimpleName() + ": " + t.getMessage(),
                ButtonType.OK);
            a.setHeaderText("Awards Dashboard");
            a.showAndWait();
        }
    }

    @FXML private void menuPrintQsl() {
        new QslPrintWindow(getStage(),
            new ArrayList<>(qsoTable.getSelectionModel().getSelectedItems())
        ).show();
    }

    @FXML private void menuSetup() {
        openJHubSetup();
        updateStationCallLabel();
    }

    @FXML private void menuMacros()    { openJHubSetupAt("macros");    }
    @FXML private void menuAmp()       { openJHubSetupAt("amp");       }
    @FXML private void menuAntenna()   { openJHubSetupAt("antenna");   }
    @FXML private void menuUploaders() { openJHubSetupAt("uploaders"); }
    @FXML private void menuBackup()    { openJHubSetupAt("logging");   }  // backup card lives on Logging tab
    @FXML private void menuLearn()     { openJHubSetupAt("learn");     }

    @FXML private void menuReportIssue() {
        String log = System.getProperty("user.home", "") + "/.j-log/logs/j-log.log";
        com.jlog.util.IssueReporter.openGitHubIssue("j-log", "1.0.51", log);
        setStatus("Opening GitHub Issues in your browser…");
    }

    /** Two translator viewers. Kept as singletons so the operator can pin
     *  one and reopen via the menu without losing state. */
    private UserTranslationWindow translatorUserWin;
    private DxccTranslationWindow translatorDxccWin;

    @FXML private void openTranslatorUser() {
        if (translatorUserWin == null) translatorUserWin = new UserTranslationWindow();
        translatorUserWin.show();
    }

    @FXML private void openTranslatorDxcc() {
        if (translatorDxccWin == null) translatorDxccWin = new DxccTranslationWindow();
        translatorDxccWin.show();
        // If the operator has a row selected, drive the entity from it
        var sel = qsoTable.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getCountry() != null && !sel.getCountry().isBlank()) {
            translatorDxccWin.applyEntity(sel.getCountry());
        }
    }

    private PriorityCallsignWindow priorityWin;
    @FXML private void openPriorityCallsigns() {
        if (priorityWin == null) priorityWin = new PriorityCallsignWindow();
        priorityWin.show();
    }

    private void openJHubSetupAt(String tab) {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081#" + tab).start(); }
        catch (Exception e) { setStatus("Could not open browser: " + e.getMessage()); }
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
        q.setBand(tfBand.getText().trim());
        q.setMode(tfMode.getText().trim());
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
        applyActivationTag(q);
        return q;
    }

    /** If an activation is running, stamp SIG / SIG_INFO onto the QSO. */
    private void applyActivationTag(QsoRecord q) {
        if (currentSig == null || currentSig.isBlank()) return;
        if (currentSigInfo == null || currentSigInfo.isBlank()) return;
        q.setSig(currentSig);
        q.setSigInfo(currentSigInfo);
    }

    private void populateFromRecord(QsoRecord q) {
        tfCallsign.setText(q.getCallsign());
        tfBand.setText(q.getBand() != null ? q.getBand() : "");
        tfMode.setText(q.getMode() != null ? q.getMode() : "");
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
        q.setBand(tfBand.getText().trim());
        q.setMode(tfMode.getText().trim());
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
        applyActivationTag(q);
    }

    // ---------------------------------------------------------------
    // POTA / SOTA activation mode
    // ---------------------------------------------------------------

    /** Called by the splash when launching in activation mode — unhides the
     *  activation bar and defaults SIG to POTA. */
    public void enableActivationMode() {
        if (activationBar != null) {
            activationBar.setVisible(true);
            activationBar.setManaged(true);
        }
        if (tfActivationSig != null && (tfActivationSig.getText() == null || tfActivationSig.getText().isBlank()))
            tfActivationSig.setText("POTA");
        if (lblActivationStatus != null) lblActivationStatus.setText("(not active)");
    }

    @FXML private void startActivation() {
        if (tfActivationSig == null || tfActivationSigInfo == null) return;
        String sig  = tfActivationSig.getText() == null ? "" : tfActivationSig.getText().trim().toUpperCase();
        String sigInfo = tfActivationSigInfo.getText() == null ? "" : tfActivationSigInfo.getText().trim().toUpperCase();
        if (sig.isBlank() || sigInfo.isBlank()) {
            setStatus("Enter SIG (POTA/SOTA) and reference (e.g. K-0001) first.");
            return;
        }
        currentSig     = sig;
        currentSigInfo = sigInfo;
        if (lblActivationStatus != null) lblActivationStatus.setText("🏔️ Activating " + sig + " " + sigInfo);
        setStatus("Activation started: " + sig + " " + sigInfo);
    }

    @FXML private void stopActivation() {
        currentSig     = null;
        currentSigInfo = null;
        if (lblActivationStatus != null) lblActivationStatus.setText("(not active)");
        setStatus("Activation stopped.");
    }

    // ---------------------------------------------------------------
    // Spot / log-draft intake
    // ---------------------------------------------------------------

    private void fillFromCallsignResult(com.fasterxml.jackson.databind.JsonNode node) {
        if (!node.path("found").asBoolean(false)) return;
        String call = node.path("callsign").asText("").trim().toUpperCase();
        // Only apply if the result matches the currently-entered callsign
        if (!call.isEmpty() && !call.equals(tfCallsign.getText().trim().toUpperCase())) return;

        String name    = node.path("name").asText("").trim();
        String state   = node.path("state").asText("").trim();
        String country = node.path("country").asText("").trim();

        if (!name.isEmpty())    tfOperatorName.setText(name);
        if (!state.isEmpty())   tfState.setText(state);
        if (!country.isEmpty()) { tfCountry.setText(country); lblCountry.setText(country); }

        double dxLat = node.path("lat").asDouble(0);
        double dxLon = node.path("lon").asDouble(0);
        if (dxLat != 0 || dxLon != 0) {
            try {
                double myLat = Double.parseDouble(AppConfig.getInstance().getLatitude());
                double myLon = Double.parseDouble(AppConfig.getInstance().getLongitude());
                lblBearing.setText(String.format("%.0f°",  QrzLookup.bearing(myLat, myLon, dxLat, dxLon)));
                lblDistance.setText(String.format("%.0f km", QrzLookup.distanceKm(myLat, myLon, dxLat, dxLon)));
            } catch (Exception ignored) {}
        }
    }

    private void fillFromSpot(DxSpot spot) {
        if (spot.getDxCallsign() != null && !spot.getDxCallsign().isBlank()) {
            tfCallsign.setText(spot.getDxCallsign().toUpperCase());
            HubEngine.getInstance().sendCallsignLookup(spot.getDxCallsign());
        }
        if (spot.getFrequencyKHz() > 0) {
            tfFrequency.setText(String.format("%.3f", spot.getFrequencyKHz() / 1000.0));
            if (spot.getBand() != null) tfBand.setText(spot.getBand());
        }
        if (spot.getMode() != null && !spot.getMode().isBlank()) {
            tfMode.setText(mapMode(spot.getMode()));
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

    private void fillFromLogDraft(JsonNode node) {
        if (node == null) return;

        String callsign = node.path("callsign").asText("").trim().toUpperCase();
        String mode     = node.path("mode").asText("").trim();
        String band     = node.path("band").asText("").trim();
        long   frequency = node.path("frequency").asLong(0L);
        String rstSent  = node.path("rstSent").asText("").trim();
        String rstRcvd  = node.path("rstReceived").asText("").trim();
        String notes    = node.path("notes").asText("").trim();

        if (!callsign.isBlank())  tfCallsign.setText(callsign);
        if (!mode.isBlank())      tfMode.setText(mapMode(mode));
        if (!band.isBlank())      tfBand.setText(band);
        if (frequency > 0) {
            tfFrequency.setText(String.format("%.3f", frequency / 1_000_000.0));
            if (band.isBlank()) {
                String derived = CivEngine.freqToBand(frequency);
                if (derived != null && !derived.isBlank()) tfBand.setText(derived);
            }
        }
        if (!rstSent.isBlank()) tfRstSent.setText(rstSent);
        if (!rstRcvd.isBlank()) tfRstReceived.setText(rstRcvd);
        if (!notes.isBlank())   tfNotes.setText(notes);

        tfCallsign.requestFocus();
        setStatus("Log draft received" + (callsign.isBlank() ? "" : ": " + callsign));
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
            case "CW"         -> "CW";
            case "FT8"        -> "FT8";
            case "FT4"        -> "FT4";
            case "RTTY"       -> "RTTY";
            case "PSK31"      -> "PSK31";
            case "LSB"        -> "LSB";
            case "AM"         -> "AM";
            case "FM"         -> "FM";
            case "SSB", "USB" -> "USB";
            default           -> "USB";
        };
    }

    // ---------------------------------------------------------------
    // Band / mode warning
    // ---------------------------------------------------------------

    private void initBandWarning() {
        freqValidateDelay.setOnFinished(e -> checkBandWarning());
        tfFrequency.textProperty().addListener((obs, o, n) -> freqValidateDelay.playFromStart());
        tfMode.textProperty().addListener((obs, o, n) -> checkBandWarning());
    }

    private void checkBandWarning() {
        String freqText = tfFrequency.getText().trim();
        String mode     = tfMode.getText().trim();
        if (freqText.isEmpty() || mode.isEmpty()) { clearBandWarning(); return; }
        double freqKhz;
        try {
            freqKhz = Double.parseDouble(freqText) * 1000.0;
        } catch (NumberFormatException e) {
            clearBandWarning();
            return;
        }
        ValidationResult result = BandPlan.validate(freqKhz, mode);
        if (result.isOk()) clearBandWarning();
        else showBandWarning(result);
    }

    private void showBandWarning(ValidationResult result) {
        lblBandWarning.setText((result.isError() ? "\u26D4 " : "\u26A0 ") + result.message());
        lblBandWarning.getStyleClass().removeAll("band-warning-error", "band-warning-warn");
        lblBandWarning.getStyleClass().add(result.isError() ? "band-warning-error" : "band-warning-warn");
        lblBandWarning.setVisible(true);
        lblBandWarning.setManaged(true);
    }

    private void clearBandWarning() {
        lblBandWarning.setVisible(false);
        lblBandWarning.setManaged(false);
        lblBandWarning.setText("");
    }

    @FXML private void showBandPlan() {
        double freqKhz = 0;
        try { freqKhz = Double.parseDouble(tfFrequency.getText().trim()) * 1000.0; }
        catch (NumberFormatException ignored) {}
        new BandPlanWindow(getStage(), tfBand.getText().trim(), freqKhz).show();
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

    private void openJHubSetup() {
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
