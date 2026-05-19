package com.jlog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jlog.app.JLogApp;
import com.jlog.civ.CivEngine;
import com.jlog.cluster.HubEngine;
import com.jlog.db.ContestQsoDao;
import com.jlog.db.ContestQtcDao;
import com.jlog.i18n.I18n;
import com.jlog.macro.MacroEngine;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.DxccResolver;
import com.jlog.scoring.AriDx;
import com.jlog.scoring.AsianEntities;
import com.jlog.scoring.OceaniaDx;
import com.jlog.scoring.QsoParty;
import com.jlog.scoring.RussianDx;
import com.jlog.scoring.Scandinavian;
import com.jlog.scoring.Wag;
import com.jlog.scoring.WaeMultiplier;
import com.jlog.ui.contest.DxccListPane;
import com.jlog.ui.contest.PerModeMultGridPane;
import com.jlog.ui.contest.SweepProgressPane;
import com.jlog.ui.contest.QtcPane;
import com.jlog.ui.contest.WorkedBeforePane;
import com.jlog.ui.contest.WorkedGridsPane;
import com.jlog.ui.contest.WorkedMultsPane;
import com.jlog.ui.map.ArrlSectionMap;
import com.jlog.ui.map.CountyMap;
import com.jlog.ui.map.CqZoneMap;
import com.jlog.ui.map.DxccMap;
import com.jlog.ui.map.DxccTable;
import com.jlog.ui.map.MaidenheadGridMap;
import com.jlog.ui.map.RegionMapPane;
import com.jlog.ui.map.StatesMap;
import com.jlog.util.AppConfig;
import com.jlog.util.BandPlan;
import com.jlog.util.CallsignRegion;
import com.jlog.util.Maidenhead;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.*;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for the Contest Log window (ContestLog.fxml).
 *
 * Layout rows:
 *   Row 1 — Menu bar
 *   Row 2 — Dynamic entry bar: two HBox rows (Rcvd / Sent) inside a VBox
 *   Row 3 — Plugin panes (dupe checker, section tracker by FCC zone, stats)
 *   Row 4 — QSO database table
 *   Row 5 — DX Spotting pane
 */
public class ContestLogController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ContestLogController.class);

    // ---- Row 1: dynamic entry fields (created programmatically) ----
    @FXML private VBox  entryBar;
    @FXML private HBox  macroButtonBar;

    // ---- Row 2: plugin panes ----
    @FXML private HBox  row2PaneContainer;

    // ---- Resizable right-hand side column (plugin "placement":"column") ----
    @FXML private SplitPane  cockpitSplitPane;
    @FXML private ScrollPane sideColumnScroll;
    @FXML private VBox       sideColumnContainer;

    // ---- QSO table ----
    @FXML private TableView<QsoRecord>          qsoTable;
    @FXML private TableColumn<QsoRecord,String> colCall;
    @FXML private TableColumn<QsoRecord,String> colTime;
    @FXML private TableColumn<QsoRecord,String> colBand;
    @FXML private TableColumn<QsoRecord,String> colMode;
    @FXML private TableColumn<QsoRecord,String> colSentSerial;
    @FXML private TableColumn<QsoRecord,String> colRstSent;
    @FXML private TableColumn<QsoRecord,String> colRstRcvd;
    @FXML private TableColumn<QsoRecord,String> colExchange;
    @FXML private TableColumn<QsoRecord,String> colOp;

    // ---- Stats labels ----
    @FXML private Label lblQsoCount;
    @FXML private Label lblScore;
    @FXML private Label lblMults;
    @FXML private Label lblQsoHour;
    @FXML private Label lblStatus;
    @FXML private Label lblCivStatus;
    @FXML private Label lblRoster;
    @FXML private Label lblSyncProgress;
    @FXML private Label lblSolar;

    // ---- DX pane ----
    @FXML private SplitPane  mainSplitPane;
    @FXML private TitledPane dxPane;

    // ---- Plugin pane sections ----
    private ListView<String> dupeList;
    private Label            dupeStatusLabel;
    private final Map<String, Label> sectionLabels = new LinkedHashMap<>();
    private RegionMapPane        usMapPane;
    private RegionMapPane        caMapPane;
    private DxccListPane         dxccPane;
    private PerModeMultGridPane  perModeGrid;
    private WorkedBeforePane     workedBeforePane;
    private QtcPane              qtcPane;
    private WorkedGridsPane      gridsPane;
    private ArrlSectionMap       ssSectionMapPane;
    private Stage                sectionMapStage;
    private DxccMap              dxccMapPane;       // popup world-map window
    private DxccMap              dxccColumnMapPane; // embedded "dxcc_map" side-column pane
    private DxccTable            dxccTablePane;
    private Stage                dxccMapStage;
    private StatesMap            statesMapPane;
    private Stage                statesMapStage;
    private CqZoneMap            cqZoneMapPane;
    private Stage                cqZoneMapStage;
    private CountyMap            countyMapPane;
    private Stage                countyMapStage;
    private MaidenheadGridMap    gridMapPane;
    private Stage                gridMapStage;
    private SweepProgressPane    sweepProgressPane;
    private WorkedMultsPane      workedMultsPane;
    // Column (field1..field5) holding the multiplier value for this plugin.
    private String multColumn = "field1";
    // Modes we track for per-mode accounting (e.g. ["CW","Phone"] for 10M).
    private static final List<String> TRACKED_MODES_DEFAULT = List.of("CW", "Phone");

    // ---- State ----
    private ContestPlugin plugin;
    private final ObservableList<QsoRecord> qsoData = FXCollections.observableArrayList();
    private final AtomicInteger serialCounter = new AtomicInteger(1);
    private ScheduledExecutorService statsService;

    // Multi-op sync state. stationCount/Index default to solo (stride=1, offset=0);
    // a CONTEST_ROSTER from the hub updates them when peers join.
    private volatile int stationCount = 1;
    private volatile int stationIndex = 0;
    private String       stationName  = "";
    // Full-sync request is fired once per session — the first time we see a
    // roster that includes at least one other station beyond ourselves.
    private boolean      fullSyncRequested = false;

    // Dynamic field map: fieldId -> Control
    private final Map<String, Control> entryFields = new LinkedHashMap<>();
    // Label that precedes each field, used to hide label + control together.
    private final Map<String, Label>   entryLabels = new LinkedHashMap<>();
    private TextField tfCallsign;
    private TextField tfOperator;

    // When non-null, doSave() updates this record instead of inserting a new one.
    private QsoRecord editingRecord;

    private static final List<String> VALID_BANDS = BandPlan.allBands();
    private static final Set<String>  VALID_MODES = Set.of(
        "CW","USB","LSB","AM","FM","RTTY","FT8","FT4","PSK31","OLIVIA","DV","JS8");

    // ARRL Field Day station class (Rules 4 & 5): transmitter count (>= 1,
    // Rule 4) followed by a category designator. AB? -> Class A / A-Battery,
    // BB? -> Class B / B-Battery, C/D/E/F per Rules 4.5-4.8. There is no
    // Class "I"; a bare letter with no count is not a valid class.
    private static final String FD_CLASS_RE = "[0-9]{1,2}(AB?|BB?|C|D|E|F)";

    // ARRL November Sweepstakes "check" (Rule 4.4.1): the last two digits of
    // the year the operator/station was first licensed — always exactly two
    // digits (e.g. 79, 03, 24). Shared by the live and save-time validators.
    private static final String SS_CHECK_RE = "[0-9]{2}";

    // Divider position captured when DX pane was last expanded; restored on re-expand
    private double dxExpandedDividerPos = 0.5;

    private static final DateTimeFormatter TABLE_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Max section rows per sub-column in a section_tracker zone. A zone
    // with more sections than this flows into as many balanced
    // side-by-side sub-columns as needed, so the tallest column always
    // fits the pane row with no vertical scrollbar. 5 => single-column
    // zones go dual and the big ones go triple (W4/VE -> 3x, W1/W5/W0 ->
    // 2x), tallest column = 5 rows.
    private static final int SECTION_COL_MAX_ROWS = 5;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initMacroBar();
    }

    /** Called after FXML load with the chosen plugin. */
    public void initPlugin(ContestPlugin p) {
        this.plugin = p;
        this.multColumn = computeMultiplierColumn();
        initSerialCounter();
        buildEntryBar();
        buildRow2Panes();
        applyLockedBand();
        wireConditionalFields();
        wireRegionMapClicks();
        initCivListeners();
        initKeyHandlers();
        initDxPaneRestore();
        loadQsos();
        startStatsPoller();

        HubEngine.getInstance().setLogEntryDraftListener(node ->
            Platform.runLater(() -> fillFromLogDraft(node)));

        HubEngine.getInstance().setSolarFluxListener(node ->
            Platform.runLater(() -> updateSolarLabel(node)));

        if (AppConfig.getInstance().getCivAutoConnect()) connectCiv();

        HubEngine.getInstance().sendContestActive(p);
        joinContestRoster();

        // If the hub drops and reconnects, re-announce contest + re-join the
        // roster automatically so the peer group repopulates itself.
        HubEngine.getInstance().enableAutoReconnect();
        HubEngine.getInstance().setOnConnected(() -> Platform.runLater(() -> {
            HubEngine.getInstance().sendContestActive(p);
            fullSyncRequested = false;
            joinContestRoster();
            setStatus("Reconnected to hub.");
        }));

        // Wire CONTEST_INACTIVE + LEAVE_CONTEST to window close.
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Scene sc = entryBar.getScene();
            if (sc != null && sc.getWindow() instanceof javafx.stage.Stage st) {
                st.setOnCloseRequest(e -> {
                    HubEngine.getInstance().disableAutoReconnect();
                    HubEngine.getInstance().sendLeaveContest(p.getContestId(), stationName);
                    HubEngine.getInstance().sendContestInactive();
                });
            }
        }));
    }

    // ---------------------------------------------------------------
    // Dynamic UI construction
    // ---------------------------------------------------------------

    private void buildEntryBar() {
        entryBar.getChildren().clear();
        entryFields.clear();
        entryLabels.clear();

        // Column plan: every logical exchange field gets a fixed slot so the
        // Sent row lines up vertically under the Rcvd row — my call under the
        // worked call, RST S under RST R, My Co/St under Co/St R, etc.
        // Received fields define the column order; sent-only items (e.g. the
        // serial counter) are slotted next to the sent field they follow.
        List<String> order = new ArrayList<>();
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields())
            if (fd.getEntryRow() != 1) addKey(order, baseKey(fd.getId()));
        addKey(order, "operator");

        // A contest that doesn't exchange a serial number (e.g. CQ WW DX —
        // RST + zone only) gets no running-serial box in the entry bar.
        boolean usesSerial =
            plugin.getEntryFields().stream()
                  .anyMatch(f -> "serial".equals(baseKey(f.getId())))
            || hasSerialToken(plugin.getCabrilloSent())
            || hasSerialToken(plugin.getCabrilloRcvd());

        // Resolve every Sent field to a Rcvd column slot so the two rows stack:
        //  • same base key as a Rcvd field → sit directly under that twin
        //    (rst_sent↕rst_rcvd, and the SS/CQ pairs folded by baseKey());
        //  • no twin → reuse the next still-unclaimed Rcvd exchange column so an
        //    asymmetric exchange (ARRL DX: send state/power/mode, receive
        //    dxcc/power) stays compact and aligned instead of staircasing into
        //    empty cells;
        //  • only once no Rcvd column is left over is a fresh column appended.
        Set<String> structural = Set.of("callsign", "serial", "band", "mode", "operator");
        Deque<String> freeExchange = new ArrayDeque<>();
        for (String k : order) if (!structural.contains(k)) freeExchange.add(k);

        if (usesSerial && !order.contains("serial")) {   // running serial counter
            int ci = order.indexOf("callsign");
            order.add(ci >= 0 ? ci + 1 : order.size(), "serial");
        }

        Map<String, String> sentSlotKey = new LinkedHashMap<>();
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() != 1) continue;
            String bk = baseKey(fd.getId());
            String key;
            if ("callsign".equals(bk) || "serial".equals(bk)) {
                key = bk;                            // owned by a synthetic widget
            } else if (order.contains(bk)) {
                key = bk;                            // stacks under its Rcvd twin
                freeExchange.remove(bk);
            } else if (!freeExchange.isEmpty()) {
                key = freeExchange.poll();           // reuse a free Rcvd column
            } else {
                key = bk;                            // genuine overflow → new column
                order.add(bk);
            }
            sentSlotKey.put(fd.getId(), key);
        }

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(4);
        grid.setAlignment(Pos.CENTER_LEFT);

        Label rcvdTag = new Label("Rcvd:");
        rcvdTag.getStyleClass().add("exchange-row-label");
        Label sentTag = new Label("Sent:");
        sentTag.getStyleClass().add("exchange-row-label");
        grid.add(rcvdTag, 0, 0);
        grid.add(sentTag, 0, 1);

        // ---- Received row (gridRow 0) ----
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() == 1) continue;
            int slot = order.indexOf(baseKey(fd.getId()));
            Label lbl = new Label(fd.getLabel() + ":");
            lbl.getStyleClass().add("entry-label");
            lbl.setMinWidth(Region.USE_PREF_SIZE);
            Control ctrl = buildFieldControl(fd);
            ctrl.setId(fd.getId());
            entryFields.put(fd.getId(), ctrl);
            entryLabels.put(fd.getId(), lbl);
            grid.add(lbl,  slot * 2 + 1, 0);
            grid.add(ctrl, slot * 2 + 2, 0);
        }

        int opSlot = order.indexOf("operator");
        Label lblOp = new Label(I18n.get("label.operator"));   // i18n value already carries its colon
        lblOp.getStyleClass().add("entry-label");
        lblOp.setMinWidth(Region.USE_PREF_SIZE);
        tfOperator = new TextField(AppConfig.getInstance().getOperatorName());
        tfOperator.setPrefWidth(90);
        forceUpperCase(tfOperator, false);   // operator is logged to the DB
        grid.add(lblOp,      opSlot * 2 + 1, 0);
        grid.add(tfOperator, opSlot * 2 + 2, 0);

        // ---- Sent row (gridRow 1), aligned column-for-column ----
        int callSlot = order.indexOf("callsign");
        Label yourCall = new Label(AppConfig.getInstance().getSsCallsign());
        yourCall.setId("sentCallsign");
        yourCall.getStyleClass().add("sent-callsign");
        yourCall.setMinWidth(Region.USE_PREF_SIZE);
        grid.add(yourCall, callSlot * 2 + 2, 1);

        int serSlot = order.indexOf("serial");
        if (usesSerial) {
            Label lblSerial = new Label(I18n.get("label.serial") + ":");
            lblSerial.getStyleClass().add("entry-label");
            lblSerial.setMinWidth(Region.USE_PREF_SIZE);
            Label serialDisplay = new Label(String.valueOf(serialCounter.get()));
            serialDisplay.getStyleClass().add("serial-display");
            serialDisplay.setId("serialDisplay");
            serialDisplay.setMinWidth(Region.USE_PREF_SIZE);
            grid.add(lblSerial,     serSlot * 2 + 1, 1);
            grid.add(serialDisplay, serSlot * 2 + 2, 1);
        }

        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (fd.getEntryRow() != 1) continue;
            int slot = order.indexOf(sentSlotKey.get(fd.getId()));
            Label lbl = new Label(fd.getLabel() + ":");
            lbl.getStyleClass().add("entry-label");
            lbl.setMinWidth(Region.USE_PREF_SIZE);
            Control ctrl = buildFieldControl(fd);
            ctrl.setId(fd.getId());
            entryFields.put(fd.getId(), ctrl);
            entryLabels.put(fd.getId(), lbl);
            // The Sent row's callsign and serial slots belong to the synthetic
            // my-call / running-serial widgets. A plugin field that maps here
            // (e.g. the redundant serial_sent in ca/pa_qso_party) stays in the
            // field maps for the data & Cabrillo paths but is not drawn, so it
            // cannot overlap the synthetic widget in the same grid cell.
            if (slot == callSlot || slot == serSlot) continue;
            grid.add(lbl,  slot * 2 + 1, 1);
            grid.add(ctrl, slot * 2 + 2, 1);
        }

        Button btnSave  = new Button(I18n.get("button.save"));
        Button btnClear = new Button(I18n.get("button.clear"));
        btnSave .getStyleClass().add("primary-button");
        btnClear.getStyleClass().add("secondary-button");
        btnSave .setOnAction(e -> doSave());
        btnClear.setOnAction(e -> doClear());
        HBox btnBox = new HBox(6, btnSave, btnClear);
        btnBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(btnBox, order.size() * 2 + 1, 0);

        // ---- Entry grid in a horizontal scroller: boxes keep their natural
        // size; a narrow window scrolls instead of squeezing them. ----
        ScrollPane gridScroll = new ScrollPane(grid);
        gridScroll.setFitToHeight(true);
        gridScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        gridScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        gridScroll.getStyleClass().add("entry-scroll");
        entryBar.getChildren().add(gridScroll);

        // ---- Contest help text: its own wrapped line under the Sent row ----
        String fmt = plugin.getExchangeFormat();
        if (fmt != null && !fmt.isBlank()) {
            Label help = new Label(fmt);
            help.getStyleClass().add("exchange-help");
            help.setWrapText(true);
            help.maxWidthProperty().bind(entryBar.widthProperty().subtract(28));
            entryBar.getChildren().add(help);
        }

        prefillSentFields();
    }

    /** Append {@code key} to {@code order} unless already present. */
    private static void addKey(List<String> order, String key) {
        if (!order.contains(key)) order.add(key);
    }

    /** True if a Cabrillo exchange spec references a serial token — covers a
     *  contest that sends a serial without a serial_* entry field. */
    private static boolean hasSerialToken(List<String> spec) {
        return spec != null && spec.stream().anyMatch(t -> t.contains("serial"));
    }

    /** Logical slot key shared by a field's rcvd/sent variants
     *  (rst_rcvd & rst_sent → "rst", serial_rcvd → "serial"). */
    private static String baseKey(String id) {
        if (id == null) return "";
        String b = id;
        if (b.endsWith("_rcvd") || b.endsWith("_sent"))
            b = b.substring(0, b.length() - 5);
        // Canonicalise rcvd/sent spelling variants so a Sent field still lines
        // up under its Rcvd counterpart when the plugin abbreviates one side
        // (rcvd "precedence" vs sent "prec_sent", rcvd "section" vs sent
        // "sect_sent", rcvd "cq_zone" vs sent "zone_sent"). Only these bundled
        // variants are folded — genuinely asymmetric exchanges such as ARRL DX
        // (sent state vs rcvd dxcc/power) keep their own distinct slots.
        return switch (b) {
            case "precedence" -> "prec";
            case "section"    -> "sect";
            case "cq_zone"    -> "zone";
            default           -> b;
        };
    }

    /** The pinned value for a band/mode field the contest constrains to a
     *  single choice — plugin lockedBand/lockedMode, or a one-entry options
     *  list (a CW-only contest's "CW", a 160m-only contest's "160m"). Else
     *  null. Such a field is shown as fixed text, not a one-item dropdown. */
    private String fixedBandModeValue(ContestPlugin.FieldDef fd) {
        String id = fd.getId();
        if (!"band".equals(id) && !"mode".equals(id)) return null;
        String locked = "band".equals(id) ? plugin.getLockedBand()
                                           : plugin.getLockedMode();
        if (locked != null && !locked.isBlank()) return locked.trim();
        List<String> opts = fd.getOptions();
        if (opts != null && opts.size() == 1) return opts.get(0);
        return null;
    }

    private Control buildFieldControl(ContestPlugin.FieldDef fd) {
        String fixed = fixedBandModeValue(fd);
        if (fixed != null) {                       // single-value band/mode
            Label fx = new Label(fixed);
            fx.getStyleClass().add("fixed-field");
            fx.setMinWidth(Region.USE_PREF_SIZE);
            return fx;
        }
        if ("combo".equals(fd.getType()) && fd.getOptions() != null) {
            ComboBox<String> cb = new ComboBox<>();
            cb.setItems(FXCollections.observableArrayList(fd.getOptions()));
            cb.setPrefWidth(fd.getWidth() > 0 ? fd.getWidth() : 100);
            return cb;
        }
        TextField tf = new TextField();
        tf.setPrefWidth(fd.getWidth() > 0 ? fd.getWidth() : 100);
        if (fd.isAutoIncrement()) {
            tf.setText(String.valueOf(serialCounter.get()));
            tf.setEditable(false);
            tf.getStyleClass().add("auto-field");
        }
        // Every entry field is written to the contest DB → force ham-convention
        // uppercase, caret-safe. Callsigns additionally drop embedded spaces so a
        // pasted "W1 AW" lands as "W1AW".
        forceUpperCase(tf, "callsign".equals(fd.getId()));

        // Unified options-based validation: any text field whose plugin FieldDef
        // declares options (or is a band/mode with a generic fallback) gets its
        // value validated against that list — red ring on mismatch. The band
        // field also accepts a bare metre number ("15" ≡ "15m").
        Collection<String> allowed = effectiveOptions(fd);
        if (allowed != null && !allowed.isEmpty()) {
            final ContestPlugin.FieldDef ffd = fd;
            tf.textProperty().addListener((obs, o, n) -> applyValidStyle(tf, isFieldValueAllowed(ffd, allowed, n)));
        }
        applyFieldValidator(tf, fd.getValidator());
        if ("callsign".equals(fd.getId())) tfCallsign = tf;
        return tf;
    }

    /** Allowed-value set for a text field: plugin options first, else a
     *  generic catalogue for band/mode, else null (no options-based validation). */
    private static Collection<String> effectiveOptions(ContestPlugin.FieldDef fd) {
        if (fd.getOptions() != null && !fd.getOptions().isEmpty()) return fd.getOptions();
        if ("band".equals(fd.getId())) return VALID_BANDS;
        if ("mode".equals(fd.getId())) return VALID_MODES;
        return null;
    }

    private static boolean isAllowed(Collection<String> allowed, String value) {
        if (value == null) return true;
        String t = value.trim();
        return t.isEmpty() || allowed.stream().anyMatch(v -> v.equalsIgnoreCase(t));
    }

    private static void applyFieldValidator(TextField tf, String validator) {
        if (validator == null || validator.isBlank()) return;
        switch (validator) {
            case "maidenhead" -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank() || Maidenhead.isValid(n)));
            // Strict 6-char grid (ARRL 222 MHz & Up Distance, Rule 4.1).
            case "maidenhead6" -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank() || Maidenhead.isValid6(n)));
            case "numeric"    -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank() || n.trim().matches("[0-9]+")));
            // Field Day station class: transmitter count + category (e.g. 2A).
            case "fd_class"   -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank()
                    || n.trim().toUpperCase().matches(FD_CLASS_RE)));
            // ARRL Sweepstakes check: exactly two digits (year first licensed).
            case "ss_check"   -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank()
                    || n.trim().matches(SS_CHECK_RE)));
            default -> { /* unknown validator — ignore */ }
        }
    }

    private static void applyValidStyle(TextField tf, boolean valid) {
        String txt = tf.getText() == null ? "" : tf.getText().trim();
        boolean invalid = !txt.isEmpty() && !valid;
        // removeAll clears any accumulated duplicates; one atomic state change.
        tf.getStyleClass().removeAll("field-invalid");
        if (invalid) tf.getStyleClass().add("field-invalid");
    }

    /** Caret-safe live uppercase for DB-bound entry fields; optionally also
     *  strips embedded spaces (callsigns never contain them). */
    private static void forceUpperCase(TextField tf, boolean stripSpaces) {
        tf.textProperty().addListener((obs, o, n) -> {
            if (n == null) return;
            String up = stripSpaces ? n.replace(" ", "").toUpperCase() : n.toUpperCase();
            if (!up.equals(n)) {
                int caret = tf.getCaretPosition();
                tf.setText(up);
                tf.positionCaret(Math.min(caret, up.length()));
            }
        });
    }

    /** Options check that, for the band field, also accepts a bare metre
     *  number ("15" ≡ "15m") the way operators actually enter it. */
    private static boolean isFieldValueAllowed(ContestPlugin.FieldDef fd,
                                               Collection<String> allowed, String value) {
        if (isAllowed(allowed, value)) return true;
        return "band".equals(fd.getId()) && value != null
            && isAllowed(allowed, value.trim() + "m");
    }

    /** Band canonicalisation: a bare metre number ("15", "20") maps to its
     *  metre-band option; result is uppercased (DB convention). Unrecognised
     *  input is returned untouched so validation can still flag it. */
    private static String canonicalBand(ContestPlugin.FieldDef fd, String raw) {
        Collection<String> opts = effectiveOptions(fd);
        String v = raw == null ? "" : raw.trim();
        if (opts == null || v.isEmpty()) return v;
        if (isAllowed(opts, v)) return v.toUpperCase();
        String withM = v + "m";
        for (String o : opts) if (o.equalsIgnoreCase(withM)) return o.toUpperCase();
        return v;
    }

    /** Returns null if every required field is non-empty and every field with
     *  options / validator holds a permissible value; otherwise a short error
     *  message naming the offending field. Called by doSave to block invalid
     *  QSOs before they reach the database. */
    private String validateAllFields() {
        if (plugin == null) return null;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            Control ctrl = entryFields.get(fd.getId());
            if (ctrl == null) continue;
            String value = getControlValue(ctrl);
            if (fd.isRequired() && (value == null || value.isBlank()))
                return "Missing required field: " + fd.getLabel();
            if (value == null || value.isBlank()) continue;
            Collection<String> allowed = effectiveOptions(fd);
            if (allowed != null && !isFieldValueAllowed(fd, allowed, value))
                return "Invalid " + fd.getLabel() + ": '" + value + "'";
            String v = fd.getValidator();
            if ("maidenhead".equals(v) && !Maidenhead.isValid(value))
                return "Invalid grid: '" + value + "'";
            if ("maidenhead6".equals(v) && !Maidenhead.isValid6(value))
                return "Field " + fd.getLabel()
                    + " must be a 6-character Maidenhead grid, e.g. EN41vr";
            if ("numeric".equals(v) && !value.trim().matches("[0-9]+"))
                return "Field " + fd.getLabel() + " must be numeric";
            if ("fd_class".equals(v) && !value.trim().toUpperCase().matches(FD_CLASS_RE))
                return "Field " + fd.getLabel()
                    + " must be a Field Day class — transmitter count + category, e.g. 1A, 2B, 1D, 3F";
            if ("ss_check".equals(v) && !value.trim().matches(SS_CHECK_RE))
                return "Field " + fd.getLabel()
                    + " must be the 2-digit year first licensed, e.g. 79, 03, 24";
        }
        return null;
    }

    private void prefillSentFields() {
        AppConfig cfg = AppConfig.getInstance();
        setFieldValue("prec_sent",  cfg.getSsPrecedence());
        setFieldValue("check_sent", cfg.getSsCheck());
        setFieldValue("sect_sent",  cfg.getSsSection());
        // Generic operator-constant sent fields (e.g. Rookie Roundup
        // name/year/section): restore last-used value from station config.
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields())
            if (fd.isConstant())
                setFieldValue(fd.getId(),
                    cfg.getContestConstant(plugin.getContestId(), fd.getId()));
        // Band and mode prefill from last saved value, but only if the value is
        // acceptable under the plugin's options — otherwise leave the field empty
        // rather than stuck in an invalid state (e.g. lastBand="30m" vs SS bands).
        prefillIfAllowed("band", cfg.getLastBand());
        prefillIfAllowed("mode", cfg.getLastMode());
    }

    private void prefillIfAllowed(String id, String value) {
        if (value == null || value.isBlank()) return;
        ContestPlugin.FieldDef fd = plugin.getField(id);
        Collection<String> allowed = fd != null ? effectiveOptions(fd) : null;
        if (allowed == null || isAllowed(allowed, value)) setFieldValue(id, value);
    }

    private void setFieldValue(String id, String value) {
        Control ctrl = entryFields.get(id);
        if (ctrl instanceof TextField tf) tf.setText(value);
        else if (ctrl instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(value);
    }

    private String getFieldValue(String id) {
        Control ctrl = entryFields.get(id);
        if (ctrl instanceof TextField tf) return tf.getText();
        if (ctrl instanceof ComboBox<?> cb) return cb.getValue() != null ? cb.getValue().toString() : "";
        if (ctrl instanceof Label l) return l.getText();   // fixed band/mode
        return "";
    }

    private void buildRow2Panes() {
        row2PaneContainer.getChildren().clear();
        sideColumnContainer.getChildren().clear();
        dxccColumnMapPane = null;
        if (plugin.getRow2Panes() == null) { collapseSideColumn(); return; }

        boolean hasSectionTracker = false;
        for (ContestPlugin.PaneDef pd : plugin.getRow2Panes()) {
            TitledPane tp = new TitledPane();
            tp.setText(pd.getTitle());
            tp.setCollapsible(false);

            switch (pd.getPaneType()) {
                case "dupe_checker" -> {
                    tp.setContent(buildDupePane());
                    tp.setMaxWidth(180);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "section_tracker" -> {
                    // No ScrollPane: zone sub-columns are bounded
                    // (SECTION_COL_MAX_ROWS) so every section button fits
                    // the pane row — no vertical scrollbar.
                    tp.setContent(buildSectionPane(pd));
                    HBox.setHgrow(tp, Priority.ALWAYS);
                    hasSectionTracker = true;
                }
                case "statistics" -> {
                    tp.setContent(buildStatsPane());
                    HBox.setHgrow(tp, Priority.NEVER);
                    tp.setMaxWidth(160);
                }
                case "us_state_map" -> {
                    usMapPane = RegionMapPane.usStates();
                    usMapPane.setTooltipProvider(this::stateTooltip);
                    tp.setContent(usMapPane);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "canada_map" -> {
                    caMapPane = RegionMapPane.canada();
                    caMapPane.setTooltipProvider(this::stateTooltip);
                    tp.setContent(caMapPane);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "dxcc_list" -> {
                    List<String> entities = defaultDxccEntities();
                    dxccPane = new DxccListPane(entities);
                    tp.setContent(dxccPane);
                    tp.setMaxWidth(160);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "dxcc_map" -> {
                    DxccMap map = new DxccMap();
                    map.setTooltipProvider(entity -> {
                        DxccMap.EntityInfo info = map.entityInfo().get(entity);
                        return info != null ? entity + " — " + info.name() : entity;
                    });
                    dxccColumnMapPane = map;
                    StackPane holder = new StackPane(map);
                    holder.setMinWidth(0);
                    // Rescale the world map to whatever width the (resizable)
                    // column gives us, preserving aspect ratio.
                    holder.widthProperty().addListener(
                        (o, ov, nv) -> map.fitToWidth(nv.doubleValue()));
                    tp.setContent(holder);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                case "per_mode_mult_grid" -> {
                    perModeGrid = new PerModeMultGridPane(
                        TRACKED_MODES_DEFAULT,
                        multsForGrid());
                    ScrollPane sp = new ScrollPane(perModeGrid);
                    sp.setFitToWidth(true);
                    sp.setPrefHeight(200);
                    tp.setContent(sp);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                case "worked_before" -> {
                    workedBeforePane = new WorkedBeforePane();
                    tp.setContent(workedBeforePane);
                    tp.setMaxWidth(240);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "grid_map" -> {
                    gridsPane = new WorkedGridsPane(contestBands());
                    tp.setContent(gridsPane);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                case "qtc" -> {
                    qtcPane = new QtcPane(plugin.getContestId());
                    tp.setContent(qtcPane);
                    tp.setMaxWidth(360);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                case "ss_section_map" -> {
                    ssSectionMapPane = new ArrlSectionMap();
                    ssSectionMapPane.setTooltipProvider(this::stateTooltip);
                    ssSectionMapPane.setOnRegionClicked(sec -> onMultiplierSelected(sec, "US"));
                    ScrollPane sp = new ScrollPane(ssSectionMapPane);
                    sp.setFitToWidth(true);
                    sp.setPrefHeight(400);
                    tp.setContent(sp);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                case "sweep_progress" -> {
                    int total = plugin.getSections() != null ? plugin.getSections().size() : 0;
                    sweepProgressPane = new SweepProgressPane(total);
                    tp.setContent(sweepProgressPane);
                    tp.setMaxWidth(220);
                    HBox.setHgrow(tp, Priority.NEVER);
                }
                case "worked_mults" -> {
                    workedMultsPane = new WorkedMultsPane();
                    // Seed the full multiplier universe if the plugin declared one.
                    String listPath = plugin.getMultiplierList();
                    if (listPath != null && !listPath.isBlank()) {
                        List<String> universe = com.jlog.plugin.MultiplierLists.load(listPath);
                        if (!universe.isEmpty()) workedMultsPane.setUniverse(universe);
                    }
                    tp.setContent(workedMultsPane);
                    HBox.setHgrow(tp, Priority.ALWAYS);
                }
                default -> {
                    tp.setContent(new Label(pd.getTitle()));
                    HBox.setHgrow(tp, Priority.NEVER);
                }
            }
            if ("column".equalsIgnoreCase(pd.getPlacement())) {
                // Column panes fill the column width and stack vertically;
                // the sideColumnScroll handles overflow.
                tp.setMaxWidth(Double.MAX_VALUE);
                VBox.setVgrow(tp, Priority.NEVER);
                sideColumnContainer.getChildren().add(tp);
            } else {
                row2PaneContainer.getChildren().add(tp);
            }
        }

        if (hasSectionTracker) {
            // The FXML/CSS height floor (~180px) starves this strip, so the
            // section zone columns get clipped at the bottom. Size the strip
            // to its tallest pane's content and pin min == pref so the parent
            // VBox can never shrink it — every section button shows in full
            // with no scrollbar. Scoped to section-tracker contests so the
            // map-pane contests keep their tuned fixed height.
            row2PaneContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
            row2PaneContainer.setMinHeight(Region.USE_PREF_SIZE);
        }

        if (sideColumnContainer.getChildren().isEmpty()) collapseSideColumn();
        else                                            restoreSideColumn();
    }

    /** Drop the side column from the split entirely when a contest declares
     *  no "column" panes, so those contests render exactly as before. */
    private void collapseSideColumn() {
        if (cockpitSplitPane != null && sideColumnScroll != null) {
            cockpitSplitPane.getItems().remove(sideColumnScroll);
        }
    }

    private void restoreSideColumn() {
        if (cockpitSplitPane != null && sideColumnScroll != null
                && !cockpitSplitPane.getItems().contains(sideColumnScroll)) {
            cockpitSplitPane.getItems().add(sideColumnScroll);
            cockpitSplitPane.setDividerPositions(0.72);
        }
    }

    private List<String> contestBands() {
        // Use band-field options when declared; otherwise all BandPlan bands.
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if ("band".equals(fd.getId()) && fd.getOptions() != null && !fd.getOptions().isEmpty())
                return fd.getOptions();
        }
        return BandPlan.allBands();
    }

    private List<String> multsForGrid() {
        List<String> mults = new ArrayList<>();
        if (plugin.getUsStates() != null)        mults.addAll(plugin.getUsStates());
        if (plugin.getCanadaProvinces() != null) mults.addAll(plugin.getCanadaProvinces());
        if (mults.isEmpty() && plugin.getMultiplierModel() != null
                && plugin.getMultiplierModel().getValidValues() != null) {
            mults.addAll(plugin.getMultiplierModel().getValidValues());
        }
        return mults;
    }

    private String stateTooltip(String id) {
        StringBuilder sb = new StringBuilder(id);
        try {
            for (String mode : TRACKED_MODES_DEFAULT) {
                int count = ContestQsoDao.getInstance()
                    .countQsosForMultValue(plugin.getContestId(), multColumn, id, mode);
                LocalDateTime first = ContestQsoDao.getInstance()
                    .firstWorkedAt(plugin.getContestId(), multColumn, id, mode);
                if (count > 0) {
                    sb.append("\n").append(mode).append(": ").append(count).append(" QSO")
                      .append(count == 1 ? "" : "s");
                    if (first != null) sb.append("  first ").append(first);
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private static List<String> defaultDxccEntities() {
        // Short list of commonly-worked DXCC entities for 10M; supplemented by live log data.
        return List.of(
            "United States","Canada","Mexico","Cuba","Puerto Rico","Jamaica","Bahamas",
            "Haiti","Dominican Republic","Costa Rica","Panama","Colombia","Venezuela",
            "Brazil","Argentina","Chile","Peru","Uruguay","Paraguay","Ecuador","Bolivia",
            "England","Scotland","Wales","Ireland","France","Germany","Italy","Spain",
            "Portugal","Netherlands","Belgium","Switzerland","Austria","Sweden","Norway",
            "Finland","Denmark","Poland","Czech Republic","Slovakia","Hungary","Romania",
            "Greece","Turkey","Russia","Ukraine","Japan","China","South Korea","Taiwan",
            "Australia","New Zealand","South Africa","Egypt","Morocco","Israel","India",
            "Indonesia","Philippines","Thailand","Vietnam","Malaysia","Singapore",
            "Hong Kong","UAE","Saudi Arabia");
    }

    private VBox buildDupePane() {
        dupeStatusLabel = new Label();
        dupeStatusLabel.getStyleClass().add("dupe-status");
        dupeList = new ListView<>();
        dupeList.setPrefHeight(100);
        VBox box = new VBox(4, dupeStatusLabel, dupeList);
        box.getStyleClass().add("pane-content");
        return box;
    }

    // -----------------------------------------------------------------
    // Cockpit extensions: locked band, conditional fields, region clicks
    // -----------------------------------------------------------------

    /** Resume the serial counter from the database so exchanges continue
     *  monotonically across app restarts. In multi-op mode the counter is
     *  aligned to this station's stride slot — every station in the operation
     *  uses a disjoint arithmetic progression, so network partitions can never
     *  cause serial collisions. */
    private void initSerialCounter() {
        try {
            int maxSoFar = ContestQsoDao.getInstance().maxSerialSent(plugin.getContestId());
            serialCounter.set(nextSerialForStride(maxSoFar, stationIndex, stationCount));
        } catch (Exception e) {
            log.warn("could not resume serial counter for {}", plugin.getContestId(), e);
            serialCounter.set(nextSerialForStride(0, stationIndex, stationCount));
        }
    }

    /** Smallest serial ≥ max+1 that satisfies serial-mod-N == index. */
    static int nextSerialForStride(int max, int index, int count) {
        if (count <= 1) return max + 1;
        int start = max + 1;
        while (((start - 1) % count) != index) start++;
        return start;
    }

    // -----------------------------------------------------------------
    // Multi-op sync
    // -----------------------------------------------------------------

    /** Join the networked contest roster via j-hub. The station's stride/offset
     *  come from the CONTEST_ROSTER response and are applied on the FX thread
     *  by {@link #onRosterUpdate}. Station name defaults to the logged-in
     *  callsign so everyone in the roster sees who's who. */
    private void joinContestRoster() {
        stationName = AppConfig.getInstance().getStationCallsign();
        if (stationName == null || stationName.isBlank()) stationName = "station";

        HubEngine.getInstance().setContestRosterListener(node ->
            Platform.runLater(() -> onRosterUpdate(node)));
        HubEngine.getInstance().setContestQsoSavedListener(node ->
            Platform.runLater(() -> onRemoteQsoSaved(node)));
        HubEngine.getInstance().setContestQsoDeletedListener(node ->
            Platform.runLater(() -> onRemoteQsoDeleted(node)));
        HubEngine.getInstance().setContestFullSyncRequestListener(node ->
            onFullSyncRequest(node));   // runs on worker thread to stream QSOs
        HubEngine.getInstance().setContestFullSyncStartListener(node ->
            Platform.runLater(() -> onFullSyncStart(node)));

        HubEngine.getInstance().sendJoinContest(plugin.getContestId(), stationName);
    }

    /** Respond to a peer's REQUEST_FULL_SYNC by streaming every local contest
     *  QSO as a QSO_SAVED broadcast. Idempotent upsert on the receiving side
     *  means even if multiple peers respond, the joining station collapses
     *  duplicates automatically. */
    private void onFullSyncRequest(com.fasterxml.jackson.databind.JsonNode node) {
        if (plugin == null) return;
        String contestId = node.path("contestId").asText("");
        if (!plugin.getContestId().equals(contestId)) return;
        String requester = node.path("stationName").asText("");
        if (stationName.equals(requester)) return;   // ignore our own request echo

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                var qsos = ContestQsoDao.getInstance().fetchByContest(contestId);
                HubEngine.getInstance().sendFullSyncStart(contestId, requester, qsos.size());
                for (QsoRecord q : qsos) HubEngine.getInstance().sendContestQsoSaved(q);
                log.info("Streamed {} QSOs to peer '{}' for contest {}",
                    qsos.size(), requester, contestId);
                return null;
            }
            @Override protected void failed() {
                log.warn("full-sync response failed", getException());
            }
        };
        new Thread(task).start();
    }

    // -----------------------------------------------------------------
    // Full-sync progress (requester side)
    // -----------------------------------------------------------------

    private volatile Integer expectedSyncCount;
    private volatile int     receivedSyncCount;

    private void onFullSyncStart(com.fasterxml.jackson.databind.JsonNode node) {
        String contestId = node.path("contestId").asText("");
        if (plugin == null || !plugin.getContestId().equals(contestId)) return;
        String to = node.path("toStation").asText("");
        if (!to.isBlank() && !stationName.equals(to)) return;
        expectedSyncCount = node.path("count").asInt(0);
        receivedSyncCount = 0;
        updateSyncProgress();
    }

    private void noteSyncTick() {
        if (expectedSyncCount == null) return;
        receivedSyncCount++;
        updateSyncProgress();
        if (receivedSyncCount >= expectedSyncCount) {
            // Hold the final "done" state briefly, then clear.
            final int done = expectedSyncCount;
            new Thread(() -> {
                try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    if (expectedSyncCount != null && expectedSyncCount == done) {
                        expectedSyncCount = null;
                        updateSyncProgress();
                    }
                });
            }, "sync-progress-clear").start();
        }
    }

    private void updateSyncProgress() {
        if (lblSyncProgress == null) return;
        if (expectedSyncCount == null) { lblSyncProgress.setText(""); return; }
        lblSyncProgress.setText(String.format("⟳ syncing %d / %d",
            Math.min(receivedSyncCount, expectedSyncCount), expectedSyncCount));
    }

    /** Process a CONTEST_ROSTER broadcast — pick our index out of the list and
     *  realign the serial counter to the new stride. Keeps the serial counter
     *  monotone (we never rewind). */
    private void onRosterUpdate(com.fasterxml.jackson.databind.JsonNode node) {
        String contestId = node.path("contestId").asText("");
        if (!plugin.getContestId().equals(contestId)) return;

        com.fasterxml.jackson.databind.JsonNode arr = node.path("stations");
        if (!arr.isArray() || arr.isEmpty()) return;

        int newCount = arr.size();
        int newIndex = -1;
        for (int i = 0; i < arr.size(); i++) {
            if (stationName.equals(arr.get(i).asText(""))) { newIndex = i; break; }
        }
        if (newIndex < 0) {
            log.warn("roster update for {} missing station '{}' — staying solo",
                contestId, stationName);
            return;
        }

        stationCount = newCount;
        stationIndex = newIndex;
        // Realign the counter to the new stride without rewinding. Use the
        // max of (current, database max) so we never re-issue a used serial.
        try {
            int maxSoFar = Math.max(
                serialCounter.get() - 1,
                ContestQsoDao.getInstance().maxSerialSent(plugin.getContestId()));
            serialCounter.set(nextSerialForStride(maxSoFar, stationIndex, stationCount));
            updateSerialDisplay();
            setStatus("Multi-op: station " + (stationIndex + 1) + " of " + stationCount);
        } catch (Exception e) {
            log.warn("roster apply: max-serial read failed", e);
        }

        updateRosterDisplay(arr);

        // First time we see peers beyond ourselves, pull the shared log once.
        if (!fullSyncRequested && stationCount > 1) {
            fullSyncRequested = true;
            HubEngine.getInstance().sendRequestFullSync(plugin.getContestId(), stationName);
        }
    }

    private void updateRosterDisplay(com.fasterxml.jackson.databind.JsonNode arr) {
        if (lblRoster == null) return;
        if (arr == null || arr.size() <= 1) { lblRoster.setText(""); return; }
        StringBuilder sb = new StringBuilder("Sync: ");
        for (int i = 0; i < arr.size(); i++) {
            if (i > 0) sb.append(", ");
            String n = arr.get(i).asText("");
            sb.append(n);
            if (n.equals(stationName)) sb.append(" (you)");
        }
        lblRoster.setText(sb.toString());
    }

    /** Apply a QSO_SAVED broadcast from a peer station. Idempotent via
     *  natural-key upsert, so it's safe to handle our own echo (though we
     *  filter it out by default). */
    private void onRemoteQsoSaved(com.fasterxml.jackson.databind.JsonNode node) {
        if (plugin == null) return;
        com.fasterxml.jackson.databind.JsonNode qn = node.path("qso");
        if (qn.isMissingNode()) return;

        QsoRecord q = HubEngine.qsoFromJson(qn);
        if (q.getContestId() == null || !q.getContestId().equals(plugin.getContestId())) return;

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ContestQsoDao.getInstance().upsertByNaturalKey(q);
                return null;
            }
            @Override protected void succeeded() { loadQsos(); updateStats(); noteSyncTick(); }
            @Override protected void failed()    { log.warn("remote QSO apply failed", getException()); }
        };
        new Thread(task).start();
    }

    /** Apply a QSO_DELETED broadcast from a peer station. */
    private void onRemoteQsoDeleted(com.fasterxml.jackson.databind.JsonNode node) {
        if (plugin == null) return;
        String contestId = node.path("contestId").asText("");
        if (!plugin.getContestId().equals(contestId)) return;

        String callsign = node.path("callsign").asText("");
        String dtStr    = node.path("datetimeUtc").asText("");
        java.time.LocalDateTime dt;
        try { dt = java.time.LocalDateTime.parse(dtStr); }
        catch (Exception e) { log.warn("remote delete: bad datetimeUtc: {}", dtStr); return; }

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ContestQsoDao.getInstance().deleteByNaturalKey(contestId, callsign, dt);
                return null;
            }
            @Override protected void succeeded() { loadQsos(); updateStats(); }
            @Override protected void failed()    { log.warn("remote QSO delete failed", getException()); }
        };
        new Thread(task).start();
    }

    private void applyLockedBand() {
        applyLockedField("band", plugin.getLockedBand());
        applyLockedField("mode", plugin.getLockedMode());
    }

    private void applyLockedField(String id, String value) {
        if (value == null || value.isBlank()) return;
        Control ctrl = entryFields.get(id);
        if (ctrl == null) return;
        setFieldValue(id, value);
        ctrl.setDisable(true);
        ctrl.setOpacity(0.65);
        ctrl.setTooltip(new Tooltip(id + " locked to " + value + " for this contest"));
    }

    private void wireConditionalFields() {
        if (plugin.getConditionalFields() == null || plugin.getConditionalFields().isEmpty()) return;
        if (tfCallsign == null) return;
        tfCallsign.textProperty().addListener((obs, ov, nv) -> applyConditionalFields(nv));
        applyConditionalFields(tfCallsign.getText());
    }

    private void applyConditionalFields(String callsign) {
        String region = classifyRegion(callsign);
        for (ContestPlugin.ConditionalField cf : plugin.getConditionalFields()) {
            Control ctrl = entryFields.get(cf.getFieldId());
            Label   lbl  = entryLabels.get(cf.getFieldId());
            if (ctrl == null) continue;
            boolean show = true;
            if (cf.getShowForRegions() != null && !cf.getShowForRegions().isEmpty()) {
                show = region != null && cf.getShowForRegions().contains(region);
            }
            if (cf.getHideForRegions() != null && cf.getHideForRegions().contains(region)) {
                show = false;
            }
            ctrl.setVisible(show); ctrl.setManaged(show);
            if (lbl != null) { lbl.setVisible(show); lbl.setManaged(show); }
        }
    }

    private String classifyRegion(String callsign) {
        if (!"callsignRegion".equals(plugin.getStationClassifier())) return null;
        return regionTag(callsign);
    }

    /** True if the callsign has a "/R" rover suffix. */
    private static boolean isRover(String call) {
        if (call == null) return false;
        String c = call.trim().toUpperCase();
        return c.endsWith("/R") || c.endsWith("/ROVER");
    }

    /** ARRL Field Day mode category (Rule 6.6/6.7): CW is its own; all voice
     *  modes are equivalent ("PH"); everything else (FT8/FT4/RTTY/PSK/…) is
     *  one equivalent "DG" digital class. Used only for the FD dupe key. */
    private static String fdModeClass(String mode) {
        if (mode == null) return "DG";
        String m = mode.trim().toUpperCase();
        if (m.equals("CW")) return "CW";
        if (m.equals("SSB") || m.equals("USB") || m.equals("LSB")
                || m.equals("FM") || m.equals("AM") || m.equals("PHONE")
                || m.equals("DV") || m.equals("VOICE")) return "PH";
        return "DG";
    }

    /** Callsign → "US" | "CA" | "DX" irrespective of plugin classifier setting.
     *  Used by scoring rules (region-pair points) which must always know the region. */
    private static String regionTag(String callsign) {
        return switch (CallsignRegion.classify(callsign)) {
            case US     -> "US";
            case CANADA -> "CA";
            case DX     -> "DX";
        };
    }

    private void wireRegionMapClicks() {
        if (usMapPane != null) {
            usMapPane.setOnRegionClicked(id -> onMultiplierSelected(id, "US"));
        }
        if (caMapPane != null) {
            caMapPane.setOnRegionClicked(id -> onMultiplierSelected(id, "CA"));
        }
        if (dxccPane != null) {
            dxccPane.setOnEntityClicked(entity -> onMultiplierSelected(entity, "DX"));
        }
        if (dxccColumnMapPane != null) {
            dxccColumnMapPane.setOnRegionClicked(entity -> onMultiplierSelected(entity, "DX"));
        }
        if (gridsPane != null) {
            gridsPane.setOnGridClicked(this::onGridSelected);
        }
        if (workedMultsPane != null) {
            workedMultsPane.setOnMultClicked(v -> {
                String target = firstPresent("state_prov_rcvd","state_rcvd","sect_rcvd","dxcc");
                if (target != null) setFieldValue(target, v);
                if (tfCallsign != null) tfCallsign.requestFocus();
            });
        }
    }

    private void onGridSelected(String grid) {
        String target = firstPresent("grid_rcvd", "gridsquare_rcvd");
        if (target != null) setFieldValue(target, grid);
        if (tfCallsign != null) tfCallsign.requestFocus();
    }

    /** A multiplier was clicked on a map/list: fill the appropriate received-exchange field. */
    private void onMultiplierSelected(String value, String region) {
        // state_prov_rcvd for US / Canada, dxcc_rcvd for DX — controller tolerates either name.
        String target = switch (region) {
            case "US", "CA" -> firstPresent("state_prov_rcvd", "state_rcvd", "section", "sect_rcvd");
            case "DX"       -> firstPresent("dxcc_rcvd", "country_rcvd");
            default          -> null;
        };
        if (target != null) setFieldValue(target, value);
        if (tfCallsign != null) tfCallsign.requestFocus();
        if (usMapPane         != null && "US".equals(region)) usMapPane.setCurrent(value);
        if (caMapPane         != null && "CA".equals(region)) caMapPane.setCurrent(value);
        if (dxccMapPane       != null && "DX".equals(region)) dxccMapPane.setCurrent(value);
        if (dxccColumnMapPane != null && "DX".equals(region)) dxccColumnMapPane.setCurrent(value);
    }

    private String firstPresent(String... ids) {
        for (String id : ids) if (entryFields.containsKey(id)) return id;
        return null;
    }

    @SuppressWarnings("unchecked")
    private Node buildSectionPane(ContestPlugin.PaneDef pd) {
        sectionLabels.clear();

        Map<String, Object> config = pd.getConfig();
        List<Map<String, Object>> zoneGroups = config != null
            ? (List<Map<String, Object>>) config.get("zoneGroups") : null;

        if (zoneGroups != null && !zoneGroups.isEmpty()) {
            HBox zonesBox = new HBox(12);
            zonesBox.getStyleClass().add("section-zones");

            for (Map<String, Object> zone : zoneGroups) {
                String zoneName = (String) zone.get("name");
                List<String> sects = (List<String>) zone.get("sections");

                if (!zonesBox.getChildren().isEmpty()) {
                    Separator sep = new Separator(Orientation.VERTICAL);
                    sep.getStyleClass().add("zone-sep");
                    zonesBox.getChildren().add(sep);
                }

                VBox col = new VBox(1);
                col.getStyleClass().add("zone-column");

                Label header = new Label(zoneName);
                header.getStyleClass().add("zone-header");
                col.getChildren().add(header);

                if (sects != null) {
                    if (sects.size() > SECTION_COL_MAX_ROWS) {
                        // Flow into N balanced side-by-side sub-columns so
                        // no column exceeds SECTION_COL_MAX_ROWS rows — the
                        // pane fits every button without a scrollbar.
                        int numCols = (sects.size() + SECTION_COL_MAX_ROWS - 1)
                                / SECTION_COL_MAX_ROWS;
                        int per = (sects.size() + numCols - 1) / numCols;
                        HBox subs = new HBox(6);
                        for (int c = 0; c < numCols; c++) {
                            VBox sub = new VBox(1);
                            int start = c * per;
                            int end = Math.min(start + per, sects.size());
                            for (int i = start; i < end; i++) {
                                String sec = sects.get(i);
                                Label lbl = new Label(sec);
                                lbl.getStyleClass().add("section-label");
                                sectionLabels.put(sec, lbl);
                                sub.getChildren().add(lbl);
                            }
                            subs.getChildren().add(sub);
                        }
                        col.getChildren().add(subs);
                    } else {
                        for (String sec : sects) {
                            Label lbl = new Label(sec);
                            lbl.getStyleClass().add("section-label");
                            sectionLabels.put(sec, lbl);
                            col.getChildren().add(lbl);
                        }
                    }
                }
                zonesBox.getChildren().add(col);
            }
            return zonesBox;
        }

        GridPane grid = new GridPane();
        grid.setHgap(4); grid.setVgap(2);
        grid.getStyleClass().add("section-grid");
        List<String> sections = plugin.getSections();
        if (sections != null) {
            int col = 0, row = 0;
            for (String sec : sections) {
                Label lbl = new Label(sec);
                lbl.getStyleClass().add("section-label");
                sectionLabels.put(sec, lbl);
                grid.add(lbl, col, row);
                if (++col >= 8) { col = 0; row++; }
            }
        }
        return grid;
    }

    private VBox buildStatsPane() {
        VBox box = new VBox(6);
        box.getStyleClass().add("pane-content");
        box.getChildren().addAll(
            makeStatRow(I18n.get("stat.qso"),    lblQsoCount = new Label("0")),
            makeStatRow(I18n.get("stat.score"),  lblScore    = new Label("0")),
            makeStatRow(I18n.get("stat.mults"),  lblMults    = new Label("0")),
            makeStatRow(I18n.get("stat.qso.hr"), lblQsoHour  = new Label("0"))
        );
        return box;
    }

    private HBox makeStatRow(String label, Label value) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");
        value.getStyleClass().add("stat-value");
        return new HBox(8, lbl, value);
    }

    // ---------------------------------------------------------------
    // Table
    // ---------------------------------------------------------------

    private void initTable() {
        colCall      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCallsign()));
        colTime      .setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getDateTimeUtc() != null ? c.getValue().getDateTimeUtc().format(TABLE_FMT) : ""));
        colBand      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getBand()));
        colMode      .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getMode()));
        colSentSerial.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSerialSent()));
        colRstSent   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstSent()));
        colRstRcvd   .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getRstReceived()));
        // Received exchange = the structured field1..5 the plugin captured
        // (replaces the old redundant concatenated `exchange` blob).
        colExchange  .setCellValueFactory(c -> new SimpleStringProperty(rcvdExchange(c.getValue())));
        colOp        .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOperator()));
        qsoTable.setItems(qsoData);

        qsoTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(QsoRecord item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && item.isDupe())
                    getStyleClass().add("dupe-row");
                else
                    getStyleClass().remove("dupe-row");
            }
        });
    }

    private void initMacroBar() {
        macroButtonBar.getChildren().clear();
        for (int fk = 1; fk <= 12; fk++) {
            final int fkey = fk;
            Button btn = new Button("F" + fk);
            btn.getStyleClass().add("macro-button");
            btn.setOnAction(e -> MacroEngine.getInstance().triggerFKey(fkey));
            macroButtonBar.getChildren().add(btn);
        }
    }

    private void initCivListeners() {
        CivEngine.getInstance().setFrequencyListener(hz -> Platform.runLater(() -> {
            String band = CivEngine.freqToBand(hz);
            if (band != null) setFieldValue("band", band);
        }));
        CivEngine.getInstance().setModeListener(mode -> Platform.runLater(() -> {
            if (mode != null) setFieldValue("mode", mode);
        }));
    }

    private void initKeyHandlers() {
        Platform.runLater(() -> {
            if (entryBar.getScene() == null) return;
            entryBar.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode().isFunctionKey()) {
                    int fk = e.getCode().ordinal() - KeyCode.F1.ordinal() + 1;
                    if (fk >= 1 && fk <= 12) {
                        MacroEngine.getInstance().triggerFKey(fk);
                        e.consume();
                    }
                }
                if (e.getCode() == KeyCode.ENTER && tfCallsign != null && tfCallsign.isFocused()) {
                    doSave();
                    e.consume();
                }
            });
        });

        if (tfCallsign != null) {
            tfCallsign.textProperty().addListener((obs, ov, nv) -> {
                if (nv != null && nv.length() >= 3) {
                    String up = nv.toUpperCase();
                    checkDupe(up);
                    refreshWorkedBefore(up);
                    maybeAutoFillDxccPrefix(up);
                    maybeAutoFillWpxPrefix(up);
                } else if (workedBeforePane != null) {
                    workedBeforePane.clear();
                }
            });
        }
    }

    /** For single-exchange contests (ARRL 160M): when a DX callsign is entered,
     *  auto-fill the state/prov-rcvd field with the callsign's DXCC prefix so the
     *  multiplier count differentiates between DX entities. */
    private void maybeAutoFillDxccPrefix(String callsign) {
        if (plugin == null || !plugin.isAutoFillDxccPrefix()) return;
        if (CallsignRegion.classify(callsign) != CallsignRegion.Region.DX) return;
        String target = firstPresent("state_prov_rcvd", "state_rcvd");
        if (target == null) return;
        Control ctrl = entryFields.get(target);
        // Only overwrite if the operator hasn't typed something else.
        String current = getFieldValue(target);
        if (current != null && !current.isBlank()
                && !current.equalsIgnoreCase(CallsignRegion.dxccPrefix(callsign))
                && !isPriorDxccPrefix(current)) return;
        String prefix = CallsignRegion.dxccPrefix(callsign);
        if (!prefix.isBlank()) setFieldValue(target, prefix);
    }

    /** True if the value looks like a DXCC prefix (letters only, length 1-3) —
     *  used to decide whether to overwrite auto-filled content safely. */
    private static boolean isPriorDxccPrefix(String v) {
        return v != null && v.matches("[A-Z]{1,3}");
    }

    /** CQ WPX contests: auto-fill a "prefix_rcvd" field with the WPX prefix
     *  (letters + first digit) derived from the worked callsign. Unlike the
     *  DXCC version this fires for every callsign, not just DX. */
    private void maybeAutoFillWpxPrefix(String callsign) {
        if (plugin == null || !plugin.isAutoFillWpxPrefix()) return;
        String target = firstPresent("prefix_rcvd", "wpx_prefix_rcvd");
        if (target == null) return;
        String current = getFieldValue(target);
        String prefix  = CallsignRegion.wpxPrefix(callsign);
        // Only overwrite if blank or previously auto-populated (looks like a prefix).
        if (current != null && !current.isBlank()
                && !current.equalsIgnoreCase(prefix)
                && !current.matches("[A-Z]{1,3}[0-9]?")) return;
        if (!prefix.isBlank()) setFieldValue(target, prefix);
    }

    private void refreshWorkedBefore(String callsign) {
        if (workedBeforePane == null || plugin == null) return;
        String currentMode = getFieldValue("mode");
        // Fetch exact-match QSOs AND prefix matches so the pane can flag
        // "possible dupe" when the operator has typed a partial callsign.
        Task<WorkedBeforeResult> task = new Task<>() {
            @Override protected WorkedBeforeResult call() throws Exception {
                var exact    = ContestQsoDao.getInstance().findByCallsign(plugin.getContestId(), callsign);
                var partials = ContestQsoDao.getInstance().partialMatch(plugin.getContestId(), callsign);
                // Remove the exact match from the "partial" list so it isn't reported twice.
                partials.removeIf(c -> c.equalsIgnoreCase(callsign));
                return new WorkedBeforeResult(exact, partials);
            }
            @Override protected void succeeded() {
                var r = getValue();
                workedBeforePane.update(callsign, currentMode, r.exact, r.partials);
            }
        };
        new Thread(task).start();
    }

    private record WorkedBeforeResult(List<QsoRecord> exact, List<String> partials) {}

    /**
     * Wires the DX pane expand/collapse listener so its height is preserved
     * across minimise/restore cycles.
     */
    private void initDxPaneRestore() {
        AppConfig cfg = AppConfig.getInstance();
        double saved = cfg.getDivider("contestLog.div0", 0.5);
        dxExpandedDividerPos = saved;

        Platform.runLater(() -> {
            mainSplitPane.setDividerPositions(saved);

            mainSplitPane.getDividers().forEach(div ->
                div.positionProperty().addListener((o, ov, nv) -> {
                    if (dxPane.isExpanded()) {
                        dxExpandedDividerPos = nv.doubleValue();
                        cfg.setDivider("contestLog.div0", dxExpandedDividerPos);
                    }
                }));

            dxPane.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
                if (!isExpanded) {
                    dxExpandedDividerPos = mainSplitPane.getDividerPositions()[0];
                    cfg.setDivider("contestLog.div0", dxExpandedDividerPos);
                } else {
                    final double target = dxExpandedDividerPos;
                    Platform.runLater(() -> mainSplitPane.setDividerPositions(target));
                }
            });
        });
    }

    // ---------------------------------------------------------------
    // Save / clear
    // ---------------------------------------------------------------

    @FXML private void doSave() {
        if (tfCallsign == null || tfCallsign.getText().isBlank()) {
            setStatus(I18n.get("error.callsign.required"));
            return;
        }
        String problem = validateAllFields();
        if (problem != null) { setStatus("⛔ " + problem); return; }
        final boolean isEdit = editingRecord != null;
        final QsoRecord q = isEdit ? editingRecord : buildRecord();
        if (isEdit) applyFieldsToRecord(q);

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                if (isEdit) ContestQsoDao.getInstance().update(q);
                else        ContestQsoDao.getInstance().insert(q);
                return null;
            }
            @Override protected void succeeded() {
                rememberLastBandMode();
                if (!isEdit) {
                    // Stride-aware advance: +N where N = station count (1 in solo mode).
                    serialCounter.addAndGet(stationCount);
                    updateSerialDisplay();
                }
                // Broadcast the saved QSO to peer stations (idempotent on receipt).
                HubEngine.getInstance().sendContestQsoSaved(q);
                editingRecord = null;
                doClear();
                loadQsos();
                updateStats();
                if (isEdit) setStatus(I18n.get("status.updated"));
                else if (q.isDupe()) setStatus("⚠ DUPE logged: " + q.getCallsign());
                else setStatus(I18n.get("status.saved", q.getCallsign()));
            }
            @Override protected void failed() {
                setStatus(I18n.get("error.save.failed") + ": " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    @FXML private void doClear() {
        editingRecord = null;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (fd.isPersistent() || fd.getEntryRow() == 1) continue;
            // Band and mode persist until the user changes them.
            if ("band".equals(fd.getId()) || "mode".equals(fd.getId())) continue;
            Control ctrl = entryFields.get(fd.getId());
            if (ctrl instanceof TextField tf && !tf.getStyleClass().contains("auto-field")) tf.clear();
            else if (ctrl instanceof ComboBox<?> cb) ((ComboBox<String>) cb).getSelectionModel().clearSelection();
        }
        if (tfCallsign != null) { tfCallsign.clear(); tfCallsign.requestFocus(); }
        if (dupeStatusLabel != null) dupeStatusLabel.setText("");
        if (dupeList != null) dupeList.getItems().clear();
    }

    private void rememberLastBandMode() {
        AppConfig cfg = AppConfig.getInstance();
        String band = getFieldValue("band");
        String mode = getFieldValue("mode");
        if (band != null && !band.isBlank()) cfg.setLastBand(band.trim());
        if (mode != null && !mode.isBlank()) cfg.setLastMode(mode.trim());
        // Persist operator-constant sent fields so they survive restarts and
        // are available to the Cabrillo exporter (which has no UI access).
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (!fd.isConstant()) continue;
            String v = getFieldValue(fd.getId());
            if (v != null && !v.isBlank())
                cfg.setContestConstant(plugin.getContestId(), fd.getId(), v.trim());
        }
    }

    private QsoRecord buildRecord() {
        QsoRecord q = new QsoRecord();
        q.setContestId(plugin.getContestId());
        q.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
        applyFieldsToRecord(q);
        q.setSerialSent(String.valueOf(serialCounter.get()));

        try {
            boolean dupe;
            if (plugin.isContestWideDupe()) {
                // ARRL Sweepstakes: one QSO per callsign for the entire contest.
                dupe = ContestQsoDao.getInstance().isDuplicateContestWide(
                    plugin.getContestId(), q.getCallsign());
            } else if (plugin.isRoverAwareDupe() && isRover(q.getCallsign())) {
                // June VHF rovers: (callsign, band, grid) — new grid = new QSO.
                String grid = getFieldValue(firstPresent("grid_rcvd", "gridsquare_rcvd"));
                dupe = ContestQsoDao.getInstance().isDuplicateBandGrid(
                    plugin.getContestId(), q.getCallsign(),
                    q.getBand() != null ? q.getBand() : "",
                    multColumn,
                    grid != null ? grid : "");
            } else if (plugin.isRoverAwareDupe()) {
                // Non-rover on a rover-aware contest: (callsign, band).
                dupe = ContestQsoDao.getInstance().isDuplicatePerBand(
                    plugin.getContestId(), q.getCallsign(),
                    q.getBand() != null ? q.getBand() : "");
            } else if (plugin.isPerBandGridDupe()) {
                // ARRL 10 GHz & Up: once per band from each specific location.
                // Any station re-worked from a new grid is a new QSO (Rule 2.4
                // / 6.3) — keyed (callsign, band, grid), no /R suffix needed.
                String grid = getFieldValue(firstPresent("grid_rcvd", "gridsquare_rcvd"));
                dupe = ContestQsoDao.getInstance().isDuplicateBandGrid(
                    plugin.getContestId(), q.getCallsign(),
                    q.getBand() != null ? q.getBand() : "",
                    multColumn,
                    grid != null ? grid : "");
            } else if (plugin.isPerModeMultipliers()) {
                // Dupe rule is mode-specific, band-independent (e.g. ARRL 10M).
                dupe = ContestQsoDao.getInstance().isDuplicatePerMode(
                    plugin.getContestId(), q.getCallsign(),
                    q.getMode() != null ? q.getMode() : "");
            } else if (plugin.isFieldDayModeDupe()) {
                // ARRL Field Day (Rule 6.3/6.6/6.7): once per band per mode
                // CATEGORY — all voice equivalent, all non-CW digital
                // equivalent, CW its own. The stored mode column is the raw
                // mode, so collapse to CW/PH/DG and compare in Java against
                // prior same-callsign QSOs.
                final String fdCls = fdModeClass(q.getMode());
                final String fdBand = q.getBand() != null ? q.getBand() : "";
                dupe = ContestQsoDao.getInstance()
                    .findByCallsign(plugin.getContestId(), q.getCallsign())
                    .stream()
                    .anyMatch(r -> !r.isDupe()
                        && fdBand.equals(r.getBand() != null ? r.getBand() : "")
                        && fdCls.equals(fdModeClass(r.getMode())));
            } else if (plugin.getScoringRules() != null
                    && "qso_party".equals(plugin.getScoringRules().getMultiplierType())) {
                // QSO party: workable once per band per MODE-CLASS from
                // each county/QTH. Mode class (PH/CW/RY/DG, RY→DG when
                // mergeRttyDigital) is compared in Java so USB/LSB, and
                // FT8/FT4/RTTY where merged, collapse correctly; a
                // mobile/rover that moves and sends a new county is a new
                // QSO (MNQP county line, VTQP straddling, SCQP mobiles).
                var qpc = plugin.getScoringRules().getQsoParty();
                final boolean qpMerge = qpc != null && qpc.isMergeRttyDigital();
                final boolean qpMrgCw = qpc != null && qpc.isMergeCwDigital();
                final String qpCls  = QsoParty.modeClass(q.getMode(), qpMerge, qpMrgCw);
                final String qpBand = q.getBand() != null ? q.getBand() : "";
                final String qpQth  = getFieldValue("state_prov_rcvd");
                final String qpQthN = qpQth == null ? "" : qpQth.trim().toUpperCase();
                dupe = ContestQsoDao.getInstance()
                    .findByCallsign(plugin.getContestId(), q.getCallsign())
                    .stream()
                    .anyMatch(r -> !r.isDupe()
                        && qpBand.equals(r.getBand() != null ? r.getBand() : "")
                        && qpCls.equals(QsoParty.modeClass(r.getMode(), qpMerge, qpMrgCw))
                        && qpQthN.equals(r.getContestField1() == null ? ""
                                : r.getContestField1().trim().toUpperCase()));
            } else {
                dupe = ContestQsoDao.getInstance().isDuplicate(
                    plugin.getContestId(), q.getCallsign(),
                    q.getBand() != null ? q.getBand() : "",
                    q.getMode() != null ? q.getMode() : "");
            }
            q.setDupe(dupe);
        } catch (Exception e) {
            log.warn("dupe check failed", e);
        }

        return q;
    }

    /** Copies the current entry-field values into the given record, preserving
     *  id/contestId/datetime/serialSent (caller manages those). */
    private void applyFieldsToRecord(QsoRecord q) {
        q.setCallsign(tfCallsign != null ? tfCallsign.getText().trim().toUpperCase() : "");
        q.setOperator(tfOperator != null ? tfOperator.getText() : "");

        int slot = 0;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            if (fd.isConstant()) continue;   // operator-constant: not stored per QSO
            Control ctrl = entryFields.get(fd.getId());
            String val = getControlValue(ctrl);
            if ("band".equals(fd.getId())) val = canonicalBand(fd, val);
            switch (fd.getId()) {
                case "callsign", "prec_sent", "check_sent", "sect_sent", "serial_sent" -> {}
                case "serial_rcvd" -> q.setSerialReceived(val);
                case "band"        -> q.setBand(val);
                case "mode"        -> q.setMode(val);
                case "rst_sent"    -> q.setRstSent(val);
                case "rst_rcvd"    -> q.setRstReceived(val);
                default -> {
                    if (slot < 5) { setFieldSlot(q, slot, val); slot++; }
                }
            }
        }
        q.setPoints(computeQsoPoints(q));
    }

    /** Display string for the received exchange: the structured field1..5
     *  values the plugin captured, space-joined, blanks skipped. */
    private static String rcvdExchange(QsoRecord q) {
        StringBuilder sb = new StringBuilder();
        for (String v : new String[]{ q.getContestField1(), q.getContestField2(),
                q.getContestField3(), q.getContestField4(), q.getContestField5() }) {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(v);
            }
        }
        return sb.toString();
    }

    /** Resolve QSO points honouring region-pair (ARRL 160M) / band-class (Intl
     *  Digital) / rookie-roundup rules when declared; falls back to mode / default. */
    private int computeQsoPoints(QsoRecord q) {
        String mode = q.getMode() != null ? q.getMode() : "";
        String band = q.getBand() != null ? q.getBand() : "";
        var rules = plugin.getScoringRules();
        if (rules != null && "state_prov_country".equals(rules.getMultiplierType())) {
            return cq160Points(q);
        }
        if (rules != null && "all_asian".equals(rules.getMultiplierType())) {
            return allAsianPoints(q);
        }
        if (rules != null && "russian_dx".equals(rules.getMultiplierType())) {
            return russianDxPoints(q);
        }
        if (rules != null && "sac".equals(rules.getMultiplierType())) {
            return sacPoints(q);
        }
        if (rules != null && "ari_dx".equals(rules.getMultiplierType())) {
            return ariDxPoints(q);
        }
        if (rules != null && "wag".equals(rules.getMultiplierType())) {
            return wagPoints(q);
        }
        if (rules != null && "oceania_dx".equals(rules.getMultiplierType())) {
            return oceaniaDxPoints(q);
        }
        if (rules != null && "qso_party".equals(rules.getMultiplierType())) {
            return qsoPartyPoints(q);
        }
        if (rules != null && "wpx_prefix".equals(rules.getMultiplierType())) {
            return cqWpxPoints(q);
        }
        if (rules != null && "zone_country_state".equals(rules.getMultiplierType())) {
            return cqWwRttyPoints(q);
        }
        if (rules != null && "zone_country".equals(rules.getMultiplierType())) {
            return cqWwPoints(q);
        }
        if (rules != null && rules.isRookieRoundupScoring()) {
            // Received 2-digit "year first licensed" vs current 2-digit year.
            // Rookie (Rules 3.1.1.1 / 4.1) = first licensed in the current OR any
            // of the preceding THREE calendar years — i.e. the 2023 events accept
            // checks 23/22/21/20 → delta 0..3 inclusive. RK↔RK = 2 pts, RK↔non-RK
            // = 1 pt (Rule 5.1).
            String yearField = firstPresent("year_rcvd", "chk_rcvd", "check_rcvd");
            String yearStr   = yearField == null ? "" : getFieldValue(yearField);
            if (yearStr != null && yearStr.trim().matches("[0-9]{1,2}")) {
                int yy    = Integer.parseInt(yearStr.trim());
                int curYy = LocalDateTime.now(ZoneOffset.UTC).getYear() % 100;
                int delta = ((curYy - yy) + 100) % 100;   // wraparound-safe
                return delta <= 3 ? 2 : 1;
            }
            return 1;
        }
        if (rules != null && rules.getPointsByRegionPair() != null
                && !rules.getPointsByRegionPair().isEmpty()) {
            String myCall = AppConfig.getInstance().getStationCallsign();
            if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
            return plugin.pointsFor(regionTag(myCall), regionTag(q.getCallsign()), mode);
        }
        if (rules != null && rules.getDistanceScoring() != null) {
            // Great-circle distance scoring (ARRL 222 & Up, ARRL Intl Digital).
            // Claimed/running score — ARRL re-adjudicates from submitted grids.
            var ds = rules.getDistanceScoring();
            String theirId = ds.getTheirGridField() != null ? ds.getTheirGridField() : "grid_rcvd";
            String theirGrid = getFieldValue(theirId);
            String ownGrid = ds.getOwnGridField() != null ? getFieldValue(ds.getOwnGridField()) : null;
            if (ownGrid == null || ownGrid.isBlank())
                ownGrid = AppConfig.getInstance().getGridSquare();
            double km = Maidenhead.distanceKm(ownGrid, theirGrid);
            if (km < 0) return 0;                       // missing/invalid grid — cannot score
            if (km < ds.getMinKm()) km = ds.getMinKm(); // same-grid / floor
            String f = ds.getFormula() == null ? "" : ds.getFormula();
            if ("km_x_bandfactor".equals(f)) {
                int bf = 1;
                if (ds.getBandFactor() != null) {
                    for (var e : ds.getBandFactor().entrySet())
                        if (e.getKey().equalsIgnoreCase(band)) { bf = e.getValue(); break; }
                }
                return (int) Math.round(km) * bf;
            }
            if ("one_plus_ceil_km_div".equals(f)) {
                double div = ds.getDivisorKm() > 0 ? ds.getDivisorKm() : 500;
                int dist = (int) Math.ceil(km / div);
                if (dist < ds.getMinDistancePoints()) dist = ds.getMinDistancePoints();
                return ds.getBasePoints() + dist;
            }
            if ("one_plus_floor_km_div".equals(f)) {
                // WW Digi Rule IV.B: 1 pt + 1 pt per full `div` km between grid
                // centers (FLOOR — e.g. 5541 km / 3000 = 1 ⇒ 2 pts).
                double div = ds.getDivisorKm() > 0 ? ds.getDivisorKm() : 3000;
                int dist = (int) Math.floor(km / div);
                if (dist < ds.getMinDistancePoints()) dist = ds.getMinDistancePoints();
                return ds.getBasePoints() + dist;
            }
            return 0;                                   // unknown formula — flagged stub
        }
        if (rules != null && ((rules.getPointsByBand() != null && !rules.getPointsByBand().isEmpty())
                || (rules.getPointsByBandClass() != null && !rules.getPointsByBandClass().isEmpty()))) {
            return plugin.pointsForBand(band, mode);
        }
        return plugin.pointsForMode(mode);
    }

    /** CQ WW DX Rule IV.B QSO points from the my-station ↔ worked
     *  entity/continent relationship: same DXCC entity = 0, same continent
     *  (non-NA) different country = 1, different country within North
     *  America = 2, different continents = 3. Claimed/running value — the
     *  CQ WW Committee re-adjudicates from submitted logs. If either side
     *  is unresolvable (MM/AM, unconfigured station call) it falls back to
     *  1 so the running tally stays non-zero. */
    private int cqWwPoints(QsoRecord q) {
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(myCall);
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)            return 1;   // best-effort default
        if (me.id().equals(them.id()))             return 0;   // same DXCC entity
        if (!me.continent().equals(them.continent())) return 3; // different continents
        return "NA".equals(me.continent()) ? 2 : 1;            // same continent (NA = 2)
    }

    /** CQ WW RTTY QSO points — differs from CW/SSB: same-country = 1 (not 0),
     *  and ANY different-country same-continent contact = 2 (not just NA);
     *  different continents = 3. Claimed/running value; unresolvable → 1. */
    private int cqWwRttyPoints(QsoRecord q) {
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(myCall);
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)               return 1; // best-effort default
        if (me.id().equals(them.id()))                return 1; // same country = 1
        if (!me.continent().equals(them.continent())) return 3; // different continents
        return 2;                                               // diff country, same continent
    }

    /** CQ WPX QSO points (Rule V.B) — continent relationship × band group.
     *  LF = 7/3.5/1.8 MHz (40/80/160m) doubles the HF (28/21/14 = 10/15/20m)
     *  value: diff-continent 3/6; same-continent diff-country 1/2, but NA↔NA
     *  2/4; same-country = 1 regardless of band. Claimed/running value;
     *  unresolvable → same-continent-diff-country baseline. */
    private int cqWpxPoints(QsoRecord q) {
        String band = q.getBand() == null ? "" : q.getBand();
        boolean lf = band.equals("40m") || band.equals("80m") || band.equals("160m");
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(myCall);
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)        return lf ? 2 : 1;  // best-effort default
        if (me.id().equals(them.id()))         return 1;            // same country = 1
        if (!me.continent().equals(them.continent())) return lf ? 6 : 3; // diff continent
        if ("NA".equals(me.continent()) && "NA".equals(them.continent()))
            return lf ? 4 : 2;                                      // NA↔NA exception
        return lf ? 2 : 1;                                          // same continent, diff country
    }

    /** CQ WW 160m QSO points (Rule VI): own country 2, other country same
     *  continent 5, different continent 10; maritime mobile = 5 (no mult).
     *  Claimed/running value; unresolvable → 5 (same-continent baseline). */
    private int cq160Points(QsoRecord q) {
        if (DxccResolver.isMaritimeOrAir(q.getCallsign())) return 5;
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver r = DxccResolver.getInstance();
        DxccResolver.Entity me   = r.resolve(myCall);
        DxccResolver.Entity them = r.resolve(q.getCallsign());
        if (me == null || them == null)               return 5;  // best-effort default
        if (me.id().equals(them.id()))                return 2;  // own country
        if (!me.continent().equals(them.continent())) return 10; // different continent
        return 5;                                                // same continent, diff country
    }

    /** JARL All Asian DX QSO points (Rule §7). Asymmetric by entrant:
     *  Asian entrant — same-entity = 0; Asian QSO = 3/2/2/1 by band
     *  (160 / 80,10 / 80,10 / other); non-Asian QSO = 9/6/6/3. Non-Asian
     *  entrant — only Asian stations count, 3/2/2/1; non-Asian worked = 0.
     *  Maritime mobile scored as an Asian station. Claimed/running value
     *  (JARL re-adjudicates); unresolvable entrant defaults to non-Asian. */
    private int allAsianPoints(QsoRecord q) {
        String band = q.getBand() != null ? q.getBand() : "";
        int aPts  = switch (band) { case "160m" -> 3; case "80m","10m" -> 2; default -> 1; };
        int naPts = switch (band) { case "160m" -> 9; case "80m","10m" -> 6; default -> 3; };
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        boolean meAsian   = AsianEntities.isAsian(myCall);
        String  them      = q.getCallsign();
        boolean themMM    = DxccResolver.isMaritimeOrAir(them);
        boolean themAsian = themMM || AsianEntities.isAsian(them);
        if (meAsian) {
            if (!themMM) {                                  // same entity = 0 (Rule §7(1))
                String me = DxccResolver.getInstance().entityOf(myCall);
                String te = DxccResolver.getInstance().entityOf(them);
                if (me != null && me.equals(te)) return 0;
            }
            return themAsian ? aPts : naPts;
        }
        return themAsian ? aPts : 0;                         // non-Asian entrant: Asian only
    }

    /** Russian DX Contest QSO points (Rule §7). MM = 5 for all (§7.4).
     *  Russian entrant: vs Russia same-continent 2 / other-continent 5;
     *  vs other country same-continent 3 / other-continent 5. Non-Russian
     *  entrant: vs Russian 10; own country 2; same-continent diff-country
     *  3; other continent 5. Claimed/running — RDXC re-adjudicates;
     *  unresolved continent defaults to "other continent" (5). */
    private int russianDxPoints(QsoRecord q) {
        String them = q.getCallsign();
        if (DxccResolver.isMaritimeOrAir(them)) return 5;        // §7.4
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver R = DxccResolver.getInstance();
        boolean meRu   = RussianDx.isRussian(myCall);
        boolean themRu = RussianDx.isRussian(them);
        DxccResolver.Entity meE   = R.resolve(myCall);
        DxccResolver.Entity themE = R.resolve(them);
        String meCont   = meRu   ? RussianDx.russianContinent(myCall)
                                 : (meE   != null ? meE.continent()   : null);
        String themCont = themRu ? RussianDx.russianContinent(them)
                                 : (themE != null ? themE.continent() : null);
        boolean sameCont = meCont != null && meCont.equals(themCont);
        if (meRu) {
            if (themRu) return sameCont ? 2 : 5;                 // own country (Russia)
            return sameCont ? 3 : 5;                             // different country
        }
        if (themRu) return 10;                                   // non-Russian works Russian
        if (meE != null && themE != null && meE.id().equals(themE.id())) return 2; // own country
        return sameCont ? 3 : 5;
    }

    /** Scandinavian Activity Contest QSO points (Rule §7). Asymmetric by
     *  entrant. Scandinavian entrant: intra-Scandinavian = 0; vs European
     *  (non-Scandinavian) = 2; vs DX (non-European) = 3. Non-Scandinavian
     *  entrant: only Scandinavian stations count — a European entrant
     *  scores 1 on every band, a DX entrant 3 on 80/40m and 1 on
     *  20/15/10m (the low-band bonus). Continent resolved from the
     *  callsign; Greenland/Svalbard/etc. classify Scandinavian first so
     *  their geographic continent never bites. Claimed/running value —
     *  the SAC committee re-adjudicates; unresolved continent → non-EU. */
    private int sacPoints(QsoRecord q) {
        String them   = q.getCallsign();
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        boolean meScand   = Scandinavian.isScandinavian(myCall);
        boolean themScand = Scandinavian.isScandinavian(them);
        DxccResolver R = DxccResolver.getInstance();
        if (meScand) {
            if (themScand) return 0;                              // intra-Scandinavian
            DxccResolver.Entity te = R.resolve(them);
            return (te != null && "EU".equals(te.continent())) ? 2 : 3;
        }
        if (!themScand) return 0;                                 // non-Scand entrant: Scand only
        DxccResolver.Entity me = R.resolve(myCall);
        if (me != null && "EU".equals(me.continent())) return 1;  // European entrant: 1/band
        String band = q.getBand() == null ? "" : q.getBand();
        return (band.equals("80m") || band.equals("40m")) ? 3 : 1; // DX: low-band bonus
    }

    /** ARI International DX Contest QSO points. Own DXCC entity = 0 (still
     *  good for a multiplier); any Italian station (I id 248 / IS0 id
     *  225) = 10; otherwise own continent = 1, different continent = 3.
     *  Own-country is checked first, so an Italian working another
     *  Italian of the same DXCC scores 0 while I↔IS0 (different DXCC)
     *  scores the Italian 10. Claimed/running — the ARI committee
     *  re-adjudicates; an unresolvable side falls back to 1. */
    private int ariDxPoints(QsoRecord q) {
        String them = q.getCallsign();
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        DxccResolver R = DxccResolver.getInstance();
        DxccResolver.Entity meE   = R.resolve(myCall);
        DxccResolver.Entity themE = R.resolve(them);
        if (meE != null && themE != null && meE.id().equals(themE.id())) return 0; // own country
        if (AriDx.isItalian(them)) return 10;                                       // any Italian
        if (meE == null || themE == null) return 1;                                 // best-effort
        return meE.continent().equals(themE.continent()) ? 1 : 3;                    // cont rel
    }

    /** Worked All Germany QSO points (Rule §6). A valid contest QSO is
     *  non-German↔German or German↔German only. Non-German entrant:
     *  every QSO with a German station = 3 (a non-German↔non-German
     *  contact is not a valid contest QSO → 0). German entrant: vs
     *  German 1, vs European 3, vs DX (non-European) 5. "NM" affects the
     *  multiplier only, never points. Claimed/running — the WAG
     *  committee re-adjudicates; unresolved continent → European (3). */
    private int wagPoints(QsoRecord q) {
        String them = q.getCallsign();
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        boolean meGer   = Wag.isGerman(myCall);
        boolean themGer = Wag.isGerman(them);
        if (!meGer) return themGer ? 3 : 0;                  // non-German works German only
        if (themGer) return 1;                                // German ↔ German
        DxccResolver.Entity themE = DxccResolver.getInstance().resolve(them);
        if (themE == null) return 3;                          // best-effort European
        return "EU".equals(themE.continent()) ? 3 : 5;        // European 3 / DX 5
    }

    /** Oceania DX Contest QSO points (Rule 10 + Object 4b). Band table
     *  160=20 / 80=10 / 40=5 / 20=1 / 15=2 / 10=3. An Oceania entrant
     *  scores every QSO; a non-Oceania entrant scores only QSOs with
     *  Oceania stations (a non-Oceania↔non-Oceania contact earns no
     *  points or multiplier, Rule 4b). Claimed/running — the OCDX
     *  committee re-adjudicates. */
    private int oceaniaDxPoints(QsoRecord q) {
        int bp = OceaniaDx.bandPoints(q.getBand());
        if (bp == 0) return 0;
        String myCall = AppConfig.getInstance().getStationCallsign();
        if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
        if (OceaniaDx.isOceania(myCall)) return bp;            // Oceania works everyone
        return OceaniaDx.isOceania(q.getCallsign()) ? bp : 0;  // non-Oc: Oceania only
    }

    // ---- QSO-party engine (multiplierType:"qso_party") ----------------

    private ContestPlugin.QsoPartyConfig qpCfg() {
        return plugin.getScoringRules() == null ? null
             : plugin.getScoringRules().getQsoParty();
    }

    private Set<String> qpUpper(List<String> in) {
        Set<String> s = new HashSet<>();
        if (in != null) for (String v : in) if (v != null) s.add(v.trim().toUpperCase());
        return s;
    }

    /** In-state county universe — explicit config list, else the
     *  declared multiplierList resource (the worked_mults universe). */
    private Set<String> qpCounties(ContestPlugin.QsoPartyConfig c) {
        if (c != null && c.getInStateCounties() != null && !c.getInStateCounties().isEmpty())
            return qpUpper(c.getInStateCounties());
        return qpUpper(com.jlog.plugin.MultiplierLists.load(plugin.getMultiplierList()));
    }

    /** Field-slot column ("field1".."field5") for an entry-field id,
     *  using the same special-skip rule as computeMultiplierColumn. */
    private String qpColumnFor(String id) {
        int slot = 0;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            switch (fd.getId()) {
                case "callsign", "serial_sent", "serial_rcvd", "band", "mode",
                     "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent" -> {}
                default -> {
                    if (fd.getId().equals(id)) return "field" + (slot + 1);
                    slot++;
                }
            }
        }
        return null;
    }

    /** Operator's own sent QTH (live entry field, else the stored
     *  state_prov_sent column resolved to its real slot). */
    private String qpMyQth() {
        String s = getFieldValue("state_prov_sent");
        if (s != null && !s.isBlank()) return s.trim().toUpperCase();
        String col = qpColumnFor("state_prov_sent");
        if (col == null) return "";
        try {
            for (QsoRecord r : ContestQsoDao.getInstance().fetchByContest(plugin.getContestId())) {
                String f = switch (col) {
                    case "field1" -> r.getContestField1();
                    case "field2" -> r.getContestField2();
                    case "field3" -> r.getContestField3();
                    case "field4" -> r.getContestField4();
                    case "field5" -> r.getContestField5();
                    default -> null;
                };
                if (f != null && !f.isBlank()) return f.trim().toUpperCase();
            }
        } catch (Exception e) {
            log.warn("qpMyQth fetch failed", e);
        }
        return "";
    }

    /** Is the worked station in-state? County match, or (digital) an
     *  in-state grid, or a known club/in-state special call. */
    private boolean qpWorkedInState(QsoRecord q, ContestPlugin.QsoPartyConfig c,
                                    Set<String> counties, Set<String> grids,
                                    Set<String> clubs) {
        if (QsoParty.callIn(q.getCallsign(), clubs)) return true;
        String mc = QsoParty.modeClass(q.getMode());
        String r  = q.getContestField1();
        if ("DG".equals(mc) && c != null && c.getFt8GridDivisor() > 0) {
            String g = QsoParty.grid4(r);
            return g != null && grids.contains(g);
        }
        return QsoParty.isCounty(r, counties, c == null ? 0 : c.getCountyCodeLen(),
                c != null && c.isCountyByExclusion());
    }

    /** QSO-party QSO points. Per-mode-class table (fallback pointsPerQso);
     *  an out-of-state entrant only scores QSOs with in-state stations
     *  (Rule "outside X can only work X"); bonus calls add points for the
     *  out-of-state side. Claimed/running — sponsor re-adjudicates. */
    private int qsoPartyPoints(QsoRecord q) {
        ContestPlugin.QsoPartyConfig c = qpCfg();
        var rules = plugin.getScoringRules();
        boolean merge = c != null && c.isMergeRttyDigital();
        String mc = QsoParty.modeClass(q.getMode(), merge,
                c != null && c.isMergeCwDigital());
        int base = rules.getPointsPerQso() > 0 ? rules.getPointsPerQso() : 1;
        if (c != null && c.getPointsByModeClass() != null
                && c.getPointsByModeClass().containsKey(mc))
            base = c.getPointsByModeClass().get(mc);
        Set<String> counties = qpCounties(c);
        Set<String> grids = c == null ? Set.of() : qpUpper(c.getInStateGrids());
        Set<String> clubs = c == null ? Set.of() : qpUpper(c.getClubMultCalls());
        boolean meIn = QsoParty.isCounty(qpMyQth(), counties,
                c == null ? 0 : c.getCountyCodeLen(),
                c != null && c.isCountyByExclusion());
        boolean themIn = qpWorkedInState(q, c, counties, grids, clubs);
        boolean allQ = c != null && c.isPointsAllQsos();
        if (!allQ && !meIn && !themIn) return 0;        // out-of-state works in-state only
        // Entrant-asymmetric per-mode points: an out-of-state entrant
        // uses pointsByModeClassOut when present (DEQP: in-DE PH1/CW2,
        // out-DE PH10/CW20). In-state side keeps pointsByModeClass.
        if (!meIn && c != null && c.getPointsByModeClassOut() != null
                && c.getPointsByModeClassOut().containsKey(mc))
            base = c.getPointsByModeClassOut().get(mc);
        // Entrant-symmetric points by the WORKED station's location
        // (MEQP: a QSO with a Maine station = 2, any other = 1, the
        // same for every entrant).
        if (c != null && (c.getPtsWorkedInState() != 0
                || c.getPtsWorkedOutState() != 0))
            base = themIn ? c.getPtsWorkedInState() : c.getPtsWorkedOutState();
        // Relationship point model (SCQP): same-side / cross-side values
        // override the per-mode table when configured.
        if (c != null && (c.getPtsInToIn() != 0 || c.getPtsInToOut() != 0
                || c.getPtsOutToIn() != 0)) {
            base = meIn ? (themIn ? c.getPtsInToIn() : c.getPtsInToOut())
                        : c.getPtsOutToIn();            // !meIn&&!themIn already returned 0
        }
        // Fixed per-QSO points for specific club/bonus calls, ALL entrants
        // (e.g. OQP/QCQP CCO/RAC stations = 10 pts). Overrides the table.
        if (c != null && c.getQsoPointCalls() != null) {
            Integer fp = c.getQsoPointCalls().get(QsoParty.baseCall(q.getCallsign()));
            if (fp != null) base = fp;
        }
        if (!meIn && c != null && c.getBonusPointCalls() != null) {
            String call = q.getCallsign() == null ? "" : q.getCallsign().trim().toUpperCase();
            Integer b = c.getBonusPointCalls().get(call);
            if (b != null) base += b;                   // e.g. VTQP W1AW/1 = +2
        }
        // "Rarest" county QSO-point multiplier (NCQP §Bonus QSO Points):
        // a QSO with a designated rare county scores ×N, applied to the
        // mode points BEFORE the multiplier multiply.
        if (c != null && c.getRareQsoMultiplier() > 1 && c.getRareCounties() != null) {
            String rc = q.getContestField1() == null ? ""
                    : q.getContestField1().trim().toUpperCase();
            if (qpUpper(c.getRareCounties()).contains(rc))
                base *= c.getRareQsoMultiplier();
        }
        return base;
    }

    private void populateFromRecord(QsoRecord q) {
        if (tfCallsign != null) tfCallsign.setText(q.getCallsign() != null ? q.getCallsign() : "");
        if (tfOperator != null) tfOperator.setText(q.getOperator() != null ? q.getOperator() : "");

        String[] slots = {q.getContestField1(), q.getContestField2(), q.getContestField3(),
                          q.getContestField4(), q.getContestField5()};
        int slot = 0;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            switch (fd.getId()) {
                case "callsign", "prec_sent", "check_sent", "sect_sent", "serial_sent" -> {}
                case "serial_rcvd" -> setFieldValue(fd.getId(), q.getSerialReceived());
                case "band"        -> setFieldValue(fd.getId(), q.getBand() != null ? q.getBand() : "");
                case "mode"        -> setFieldValue(fd.getId(), q.getMode() != null ? q.getMode() : "");
                case "rst_sent"    -> setFieldValue(fd.getId(), q.getRstSent());
                case "rst_rcvd"    -> setFieldValue(fd.getId(), q.getRstReceived());
                default -> {
                    if (slot < slots.length) {
                        setFieldValue(fd.getId(), slots[slot] != null ? slots[slot] : "");
                        slot++;
                    }
                }
            }
        }
    }

    @FXML private void doEditSelected() {
        QsoRecord sel = qsoTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        editingRecord = sel;
        populateFromRecord(sel);
        setStatus("Editing: " + sel.getCallsign());
    }

    @FXML private void doDeleteSelected() {
        QsoRecord sel = qsoTable.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete QSO with " + sel.getCallsign() + "?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    ContestQsoDao.getInstance().delete(sel.getId());
                    return null;
                }
                @Override protected void succeeded() {
                    if (editingRecord != null && editingRecord.getId() == sel.getId()) {
                        editingRecord = null;
                        doClear();
                    }
                    setStatus("Deleted: " + sel.getCallsign());
                    loadQsos();
                    updateStats();
                    // Broadcast the delete to peer stations.
                    HubEngine.getInstance().sendContestQsoDeleted(
                        plugin.getContestId(), sel.getCallsign(), sel.getDateTimeUtc());
                }
                @Override protected void failed() {
                    setStatus("Delete failed: " + getException().getMessage());
                }
            };
            new Thread(task).start();
        });
    }

    private void setFieldSlot(QsoRecord q, int slot, String val) {
        switch (slot) {
            case 0 -> q.setContestField1(val);
            case 1 -> q.setContestField2(val);
            case 2 -> q.setContestField3(val);
            case 3 -> q.setContestField4(val);
            case 4 -> q.setContestField5(val);
        }
    }

    private String getControlValue(Control ctrl) {
        if (ctrl == null) return "";
        if (ctrl instanceof TextField tf) return tf.getText();
        if (ctrl instanceof ComboBox<?> cb) return cb.getValue() != null ? cb.getValue().toString() : "";
        if (ctrl instanceof Label l) return l.getText();   // fixed band/mode
        return "";
    }

    // ---------------------------------------------------------------
    // LOG_ENTRY_DRAFT intake
    // ---------------------------------------------------------------

    private void fillFromLogDraft(JsonNode node) {
        if (node == null || plugin == null) return;

        String callsign = node.path("callsign").asText("").trim().toUpperCase();
        String mode     = node.path("mode").asText("").trim();
        String band     = node.path("band").asText("").trim();
        String rstSent  = node.path("rstSent").asText("").trim();
        String rstRcvd  = node.path("rstReceived").asText("").trim();
        String exchange = node.path("exchange").asText("").trim();

        if (tfCallsign != null && !callsign.isBlank()) {
            tfCallsign.setText(callsign);
            checkDupe(callsign);
        }

        setIfPresent("mode",     mode);
        setIfPresent("band",     band);
        setIfPresent("rst_sent", rstSent);
        setIfPresent("rst_rcvd", rstRcvd);

        if (!exchange.isBlank()) applyExchangeToFields(exchange);

        if (tfCallsign != null) tfCallsign.requestFocus();
        setStatus("Log draft received" + (callsign.isBlank() ? "" : ": " + callsign));
    }

    private void setIfPresent(String fieldId, String value) {
        if (value == null || value.isBlank()) return;
        Control ctrl = entryFields.get(fieldId);
        if (ctrl instanceof TextField tf) tf.setText(value);
        else if (ctrl instanceof ComboBox<?> cb) ((ComboBox<String>) cb).setValue(value);
    }

    private void applyExchangeToFields(String exchange) {
        if (exchange == null || exchange.isBlank()) return;
        String[] tokens = exchange.trim().split("\\s+");
        if (tokens.length == 0) return;

        List<ContestPlugin.FieldDef> rcvdFields = plugin.getEntryFields().stream()
            .filter(fd -> fd.getEntryRow() != 1)
            .filter(fd -> !"callsign".equals(fd.getId()))
            .filter(fd -> !"band".equals(fd.getId()))
            .filter(fd -> !"mode".equals(fd.getId()))
            .filter(fd -> !"rst_sent".equals(fd.getId()))
            .filter(fd -> !"rst_rcvd".equals(fd.getId()))
            .filter(fd -> !"serial_sent".equals(fd.getId()))
            .collect(java.util.stream.Collectors.toList());

        int tokenIndex = 0;
        for (ContestPlugin.FieldDef fd : rcvdFields) {
            if (tokenIndex >= tokens.length) break;
            Control ctrl = entryFields.get(fd.getId());
            String token = tokens[tokenIndex];
            if (ctrl instanceof TextField tf) {
                if (tf.getText() == null || tf.getText().isBlank()) { tf.setText(token); tokenIndex++; }
            } else if (ctrl instanceof ComboBox<?> cb) {
                if (cb.getValue() == null || cb.getValue().toString().isBlank()) {
                    ((ComboBox<String>) cb).setValue(token); tokenIndex++;
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // Dupe checker
    // ---------------------------------------------------------------

    private void checkDupe(String partial) {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return ContestQsoDao.getInstance().partialMatch(plugin.getContestId(), partial);
            }
            @Override protected void succeeded() {
                List<String> matches = getValue();
                if (dupeList != null) dupeList.setItems(FXCollections.observableArrayList(matches));
                boolean fullMatch = matches.stream().anyMatch(c -> c.equalsIgnoreCase(partial));
                if (dupeStatusLabel != null) {
                    dupeStatusLabel.setText(fullMatch ? "⚠ DUPE" : (matches.isEmpty() ? "✓ New" : "Partial: " + matches.size()));
                    dupeStatusLabel.getStyleClass().removeAll("dupe-status-ok", "dupe-status-warn");
                    dupeStatusLabel.getStyleClass().add(fullMatch ? "dupe-status-warn" : "dupe-status-ok");
                }
            }
        };
        new Thread(task).start();
    }

    // ---------------------------------------------------------------
    // Stats
    // ---------------------------------------------------------------

    private void startStatsPoller() {
        statsService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "contest-stats");
            t.setDaemon(true);
            return t;
        });
        statsService.scheduleAtFixedRate(this::updateStats, 2, 10, TimeUnit.SECONDS);
    }

    private void updateStats() {
        if (plugin == null) return;
        try {
            String multCol = multColumn;
            int count  = ContestQsoDao.getInstance().countByContest(plugin.getContestId());
            int qsoHr  = computeQsoHour();

            // Field Day-style scoring: final score = QSO points (no multiplier).
            // Bonus points are computed off-log at submission time.
            if (plugin.getScoringRules() != null && plugin.getScoringRules().isScoreIsPointsOnly()) {
                int total = ContestQsoDao.getInstance().totalPointsByContest(plugin.getContestId());
                List<String> worked = ContestQsoDao.getInstance()
                    .distinctFieldByColumn(plugin.getContestId(), multCol);
                final int score = total;
                final int sections = worked.size();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(sections));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                    sectionLabels.values().forEach(l -> l.getStyleClass().remove("section-worked"));
                    worked.forEach(sec -> {
                        Label lbl = sectionLabels.get(sec);
                        if (lbl != null) lbl.getStyleClass().add("section-worked");
                    });
                });
                return;
            }

            if (plugin.isPerModeMultipliers()) {
                // Per-mode mults + per-mode point sums. Score = (P_cw + P_phone) × (M_cw + M_phone).
                Map<String, List<String>> workedByMode = new LinkedHashMap<>();
                int totalMults = 0, totalPoints = 0;
                for (String mode : TRACKED_MODES_DEFAULT) {
                    List<String> w = ContestQsoDao.getInstance()
                        .distinctFieldByColumnAndMode(plugin.getContestId(), multCol, mode);
                    workedByMode.put(mode, w);
                    totalMults  += w.size();
                    totalPoints += ContestQsoDao.getInstance()
                        .pointsByMode(plugin.getContestId(), mode);
                }
                final int score = totalPoints * totalMults;
                final int mults = totalMults;

                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                    refreshMapsWorked(workedByMode);
                    refreshPerModeGrid(workedByMode);
                });
            } else if (plugin.getScoringRules() != null
                    && "state_prov_country".equals(plugin.getScoringRules().getMultiplierType())) {
                // CQ WW 160m (Rule V/VII): one combined multiplier set =
                // US state | VE province | DXCC country, contest-wide (single
                // band). W/VE mult = the state/prov the op logged (field1);
                // DX mult = DXCC entity resolved from the callsign (the zone
                // they send is a location indicator only, NOT a multiplier).
                // MM stations carry no multiplier (Rule VI). US=dxcc 291,
                // Canada=dxcc 1; everything else = its own country.
                DxccResolver dxr = DxccResolver.getInstance();
                Set<String> mset = new HashSet<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String call = q.getCallsign();
                    if (DxccResolver.isMaritimeOrAir(call)) continue;   // no mult
                    DxccResolver.Entity e = dxr.resolve(call);
                    if (e == null) continue;                            // unresolved
                    String key;
                    if ("291".equals(e.id()) || "1".equals(e.id())) {
                        String sp = q.getContestField1();               // state/prov logged
                        if (sp == null || sp.isBlank()) continue;
                        key = e.id() + ":" + sp.trim().toUpperCase();
                    } else {
                        key = "DX:" + e.id();
                    }
                    mset.add(key);
                }
                int total  = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = mset.size();
                final int score = total * mults;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "all_asian".equals(plugin.getScoringRules().getMultiplierType())) {
                // JARL All Asian DX (Rule §7/§8): per-band multiplier is
                // asymmetric by entrant. Asian entrant → distinct DXCC
                // entities per band (same-entity & MM excluded). Non-Asian
                // entrant → distinct Asian WPX prefixes per band (only Asian
                // stations count). Score = Σ pts (all bands) × Σ mults (all
                // bands). Callsign-derived; no logged mult field.
                String aaCall = AppConfig.getInstance().getStationCallsign();
                if (aaCall == null || aaCall.isBlank())
                    aaCall = AppConfig.getInstance().getSsCallsign();
                boolean meAsian = AsianEntities.isAsian(aaCall);
                String meEnt = DxccResolver.getInstance().entityOf(aaCall);
                Map<String, Set<String>> aaByBand = new LinkedHashMap<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String call = q.getCallsign();
                    if (DxccResolver.isMaritimeOrAir(call)) continue;   // MM = no mult
                    String tok;
                    if (meAsian) {
                        String e = DxccResolver.getInstance().entityOf(call);
                        if (e == null) continue;
                        if (meEnt != null && meEnt.equals(e)) continue; // same entity = no mult
                        tok = e;
                    } else {
                        if (!AsianEntities.isAsian(call)) continue;     // only Asian count
                        tok = CallsignRegion.wpxPrefix(call);
                        if (tok == null || tok.isBlank()) continue;
                    }
                    aaByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
                }
                int aaMults = aaByBand.values().stream().mapToInt(Set::size).sum();
                int aaTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = aaMults;
                final int score = aaTotal * aaMults;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "russian_dx".equals(plugin.getScoringRules().getMultiplierType())) {
                // Russian DX (Rule §9/§10): dual per-band multiplier =
                // distinct oblast + distinct DXCC/WAE country. Oblast comes
                // from the logged exchange field (field1) for Russian
                // stations; UA2F/RI1FJ/RI1AN count as a separate country AND
                // a separate oblast (§7.3 double mult). Country is resolved
                // from the callsign. MM = no mult (§7.4). Score = Σ pts ×
                // (Σ oblast mults + Σ country mults).
                Set<String> rdMult = new HashSet<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String call = q.getCallsign();
                    if (DxccResolver.isMaritimeOrAir(call)) continue;     // §7.4 no mult
                    rdMult.add(b + "|C|" + RussianDx.countryToken(call)); // country/band
                    if (RussianDx.isRussian(call)) {
                        String ct = RussianDx.countryToken(call);
                        String ob;
                        if ("RI1FJ".equals(ct) || "RI1AN".equals(ct) || "UA2F".equals(ct))
                            ob = ct;                                       // §7.3 separate oblast
                        else {
                            String f1 = q.getContestField1();
                            ob = f1 == null ? "" : f1.trim().toUpperCase();
                        }
                        if (!ob.isBlank()) rdMult.add(b + "|O|" + ob);     // oblast/band
                    }
                }
                int rdTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = rdMult.size();
                final int score = rdTotal * rdMult.size();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "sac".equals(plugin.getScoringRules().getMultiplierType())) {
                // Scandinavian Activity Contest (Rule §8): per-band
                // multiplier is asymmetric by entrant. Scandinavian entrant
                // → distinct DXCC entities per band over every station
                // worked. Non-Scandinavian entrant → distinct Scandinavian
                // district tokens (entity + call-area digit, §8.2) per
                // band, only Scandinavian stations counting. Score = Σ QSO
                // pts (all bands) × Σ mults (all bands). Callsign-derived;
                // no logged multiplier field. Claimed/running — the SAC
                // committee re-adjudicates.
                String scCall = AppConfig.getInstance().getStationCallsign();
                if (scCall == null || scCall.isBlank())
                    scCall = AppConfig.getInstance().getSsCallsign();
                boolean meScand = Scandinavian.isScandinavian(scCall);
                DxccResolver dxr = DxccResolver.getInstance();
                Map<String, Set<String>> scByBand = new LinkedHashMap<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String call = q.getCallsign();
                    String tok;
                    if (meScand) {
                        DxccResolver.Entity e = dxr.resolve(call);
                        if (e == null) continue;
                        tok = e.id();
                    } else {
                        tok = Scandinavian.multToken(call);     // null if not Scandinavian
                        if (tok == null) continue;
                    }
                    scByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
                }
                int scMults = scByBand.values().stream().mapToInt(Set::size).sum();
                int scTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = scMults;
                final int score = scTotal * scMults;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "ari_dx".equals(plugin.getScoringRules().getMultiplierType())) {
                // ARI DX (Multipliers/Final score): one combined per-band
                // multiplier set = Italian province (the 2-letter code the
                // Italian station logged, field1) + DXCC entity for every
                // non-Italian station. I (248) and IS0 (225) are NEVER a
                // country multiplier — only their province counts. Each
                // value once per band; the same station re-worked in
                // another mode adds no new key (set-dedup = "only the
                // first QSO is good for multiplier credit"). Score = Σ QSO
                // pts (all bands) × Σ mults (all bands). Claimed — the ARI
                // committee re-adjudicates.
                DxccResolver dxr = DxccResolver.getInstance();
                Set<String> arMult = new HashSet<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String call = q.getCallsign();
                    if (AriDx.isItalian(call)) {
                        String pr = q.getContestField1();
                        pr = pr == null ? "" : pr.trim().toUpperCase();
                        if (!pr.isBlank()) arMult.add(b + "|P|" + pr); // province
                    } else {
                        String e = dxr.entityOf(call);
                        if (e == null) continue;
                        if (AriDx.ITALY.equals(e) || AriDx.SARDINIA.equals(e)) continue;
                        arMult.add(b + "|C|" + e);                     // DXCC country
                    }
                }
                int arTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = arMult.size();
                final int score = arTotal * arMult.size();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "oceania_dx".equals(plugin.getScoringRules().getMultiplierType())) {
                // Oceania DX (Rule 9/11): multiplier = distinct WPX
                // prefixes, the same prefix counting once PER BAND. An
                // Oceania entrant counts every station's prefix; a non-
                // Oceania entrant counts only Oceania stations' prefixes
                // (non-Oceania↔non-Oceania = no mult, Rule 4b). Prefix is
                // callsign-derived (CallsignRegion.wpxPrefix — the shared
                // WPX helper, same coarse spots as CQ WPX). Score = Σ QSO
                // pts (all bands) × Σ prefixes (all bands).
                String ocCall = AppConfig.getInstance().getStationCallsign();
                if (ocCall == null || ocCall.isBlank())
                    ocCall = AppConfig.getInstance().getSsCallsign();
                boolean meOc = OceaniaDx.isOceania(ocCall);
                Set<String> ocMult = new HashSet<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String call = q.getCallsign();
                    if (!meOc && !OceaniaDx.isOceania(call)) continue; // Rule 4b
                    String pfx = CallsignRegion.wpxPrefix(call);
                    if (pfx == null || pfx.isBlank()) continue;
                    ocMult.add(b + "|" + pfx);
                }
                int ocTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = ocMult.size();
                final int score = ocTotal * ocMult.size();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "qso_party".equals(plugin.getScoringRules().getMultiplierType())) {
                // Reusable QSO-party engine. Multiplier scope per_mode /
                // per_band / once. In-state entrant counts own counties +
                // (config) W/VE states & provinces + DXCC (each or one) +
                // club calls; an out-of-state entrant counts ONLY in-state
                // counties + club calls. Digital (FT8/WSJT) when a grid
                // divisor is configured contributes grid mults instead of
                // QTH: in-state = floor(Σ distinct grids / divisor),
                // out-of-state = min(distinct in-state grids, cap). The
                // power multiplier (a station category, not log-derivable)
                // is intentionally NOT applied. Claimed/running — the
                // sponsor's checker re-adjudicates.
                ContestPlugin.QsoPartyConfig c = qpCfg();
                Set<String> counties = qpCounties(c);
                Set<String> grids = c == null ? Set.of() : qpUpper(c.getInStateGrids());
                Set<String> clubs = c == null ? Set.of() : qpUpper(c.getClubMultCalls());
                String scope = c != null && c.getMultScope() != null
                        ? c.getMultScope() : "once";
                boolean inCounties = c == null || c.isInStateCountsCounties();
                boolean ownState   = c != null && c.isInStateOwnStateMult();
                boolean inStates   = c != null && c.isInStateCountsStates();
                boolean dxEach     = c != null && c.isInStateCountsDxccEach();
                boolean noDx       = c != null && c.isInStateNoDxMult();
                boolean selfState  = c != null && c.isInStateSelfStateMult();
                boolean clubMemMult = c != null && c.isClubMemberMult();
                boolean merge      = c != null && c.isMergeRttyDigital();
                boolean mergeCw    = c != null && c.isMergeCwDigital();
                boolean gridCeil   = c != null && c.isGridDivisorCeil();
                boolean gridUncap  = c != null && c.isOutStateGridUncapped();
                int divisor = c == null ? 0 : c.getFt8GridDivisor();
                int outCap  = c == null ? 0 : c.getOutStateGridCap();
                String stateAbbr = c == null || c.getStateAbbr() == null
                        ? "" : c.getStateAbbr().toUpperCase();
                int countyLen = c == null ? 0 : c.getCountyCodeLen();
                boolean byExcl = c != null && c.isCountyByExclusion();
                int areaPfx = c == null ? 0 : c.getAreaStatePrefixLen();
                Map<String,Integer> bonusMap = c == null || c.getBonusStations() == null
                        ? Map.of() : c.getBonusStations();
                Map<String,Integer> bonusOnce = c == null || c.getBonusStationsOnce() == null
                        ? Map.of() : c.getBonusStationsOnce();
                Map<String,Integer> bonusPerMode = c == null || c.getBonusStationsPerMode() == null
                        ? Map.of() : c.getBonusStationsPerMode();
                boolean multsAll = c != null && c.isMultsAllEntrants();
                boolean meIn = QsoParty.isCounty(qpMyQth(), counties, countyLen, byExcl);
                // Entrant-asymmetric multiplier scope: an out-of-state
                // entrant may count on a different scope than the in-state
                // entrant (HQP: in-HI once, non-HI per band).
                if (!meIn && c != null && c.getMultScopeOut() != null
                        && !c.getMultScopeOut().isBlank())
                    scope = c.getMultScopeOut();
                Set<String> mset = new HashSet<>();
                Set<String> allDg = new HashSet<>();
                Set<String> inDg  = new HashSet<>();
                Set<String> bonusSeen = new HashSet<>();   // baseCall|band|mc credited once
                Set<String> bonusOnceSeen = new HashSet<>(); // baseCall credited once total
                Set<String> bonusPerModeSeen = new HashSet<>(); // baseCall|mc credited once
                int bonusPts = 0;
                Set<String> rareSet  = c == null ? Set.of() : qpUpper(c.getRareCounties());
                Set<String> rareSeen = new HashSet<>();     // distinct rare counties (sweep)
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b  = q.getBand() == null ? "" : q.getBand();
                    String mc = QsoParty.modeClass(q.getMode(), merge, mergeCw);
                    String call = q.getCallsign() == null ? "" : q.getCallsign().trim().toUpperCase();
                    String R  = q.getContestField1() == null ? ""
                            : q.getContestField1().trim().toUpperCase();
                    if (clubMemMult) {
                        // ARC QSO Party: the multiplier is the count of
                        // distinct club-member base callsigns. A club
                        // member signs "call /###" so the received
                        // exchange carries the club-age digits; a non-club
                        // member sends only a name (no digit). Counted
                        // once overall, not per band/mode.
                        if (R.chars().anyMatch(Character::isDigit))
                            mset.add("M|" + QsoParty.baseCall(call));
                        continue;
                    }
                    String sk = "per_mode".equals(scope) ? mc + "|"
                              : "per_band".equals(scope) ? b + "|"
                              : "per_band_mode".equals(scope) ? b + "|" + mc + "|" : "";
                    // post-multiply bonus station (base-call, once per band+mode)
                    if (!bonusMap.isEmpty()) {
                        String bc = QsoParty.baseCall(call);
                        Integer bp = bonusMap.get(bc);
                        if (bp != null && bonusSeen.add(bc + "|" + b + "|" + mc))
                            bonusPts += bp;
                    }
                    // once-total bonus station (whole log, e.g. N5LCC / W1AW/5)
                    if (!bonusOnce.isEmpty()) {
                        String bc = QsoParty.baseCall(call);
                        Integer bp = bonusOnce.get(bc);
                        if (bp != null && bonusOnceSeen.add(bc))
                            bonusPts += bp;
                    }
                    // once-per-mode-class bonus station (WA Salmon Run W7DX
                    // = +500 per mode, Phone+CW, max 1000 — not per band)
                    if (!bonusPerMode.isEmpty()) {
                        String bc = QsoParty.baseCall(call);
                        Integer bp = bonusPerMode.get(bc);
                        if (bp != null && bonusPerModeSeen.add(bc + "|" + mc))
                            bonusPts += bp;
                    }
                    if (rareSet.contains(R)) rareSeen.add(R);   // sweep tracking
                    boolean club = QsoParty.callIn(call, clubs);
                    if ("DG".equals(mc) && divisor > 0) {           // digital → grid only
                        String g = QsoParty.grid4(R);
                        if (g != null) {
                            if (meIn) allDg.add(g);
                            else if (grids.contains(g)) inDg.add(g);
                        }
                        if (club) mset.add(sk + "K|" + call);
                        continue;
                    }
                    boolean isDx  = "DX".equals(regionTag(call)) || "DX".equals(R);
                    // A callsign-classified DX station never counts as an
                    // in-state county even if its sent token (a DXCC
                    // prefix, OKQP) coincides with a county code length.
                    boolean isCty = !isDx && QsoParty.isCounty(R, counties, countyLen, byExcl);
                    if (meIn || multsAll) {     // MEQP: every entrant counts the full mult set
                        if (isCty) {
                            if (areaPfx > 0 && R.length() >= areaPfx)
                                mset.add(sk + "S|" + R.substring(0, areaPfx)); // 7QP: in-area code's state token
                            else if (ownState) mset.add(sk + "S|" + stateAbbr); // in-state stn = state mult
                            else if (inCounties) mset.add(sk + "C|" + R);
                        } else if (isDx) {
                            if (!noDx) {
                                String e = DxccResolver.getInstance().entityOf(call);
                                mset.add(sk + "X|" + (dxEach ? (e == null ? "DX" : e) : "DX"));
                            }
                        } else if (inStates && !R.isBlank() && !R.equals(stateAbbr)) {
                            mset.add(sk + "S|" + R);
                        }
                        if (selfState) mset.add(sk + "S|" + stateAbbr); // WVQP: own state counts among the 50
                        if (club) mset.add(sk + "K|" + call);
                    } else {
                        if (isCty) mset.add(sk + "C|" + R);
                        if (club) mset.add(sk + "K|" + call);
                    }
                }
                if (c != null && c.getSweepBonusThreshold() > 0
                        && rareSeen.size() >= c.getSweepBonusThreshold())
                    bonusPts += c.getSweepBonusPoints();        // post-multiply sweep
                else if (c != null && c.getSweepBonusThreshold2() > 0
                        && rareSeen.size() >= c.getSweepBonusThreshold2())
                    bonusPts += c.getSweepBonusPoints2();        // lower fallback tier (MDC 13→250)
                int qpMults = mset.size();
                if (meIn && divisor > 0)
                    qpMults += gridCeil
                        ? (int) Math.ceil(allDg.size() / (double) divisor)
                        : allDg.size() / divisor;
                if (!meIn && (outCap > 0 || gridUncap))
                    qpMults += gridUncap ? inDg.size()
                                         : Math.min(inDg.size(), outCap);
                if (c != null && c.getMultCap() > 0)
                    qpMults = Math.min(qpMults, c.getMultCap()); // CQP: 58 scored of 63
                int qpTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = qpMults;
                final int score = qpTotal * qpMults + bonusPts;   // bonus added post-multiply
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "wag".equals(plugin.getScoringRules().getMultiplierType())) {
                // Worked All Germany (Rule §5): multiplier counted per
                // band AND per mode-class — "once in CW and once in SSB"
                // (new 2024). German entrant → each DXCC/WAE area worked
                // (WaeMultiplier token). Non-German entrant → German
                // district = first letter of the worked German station's
                // DOK (logged field1); "NM" / blank = no mult, and only
                // German stations carry a district. Score = Σ QSO pts
                // (all bands) × Σ mults (all bands/modes). Claimed — the
                // WAG committee re-adjudicates.
                String wgCall = AppConfig.getInstance().getStationCallsign();
                if (wgCall == null || wgCall.isBlank())
                    wgCall = AppConfig.getInstance().getSsCallsign();
                boolean meGer = Wag.isGerman(wgCall);
                Set<String> wgMult = new HashSet<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String mc = Wag.modeClass(q.getMode());
                    String call = q.getCallsign();
                    if (meGer) {
                        String tok = WaeMultiplier.token(call);
                        if (tok != null) wgMult.add(b + "|" + mc + "|" + tok);
                    } else {
                        if (!Wag.isGerman(call)) continue;          // district = German only
                        String d = Wag.dokDistrict(q.getContestField1());
                        if (d != null) wgMult.add(b + "|" + mc + "|D|" + d);
                    }
                }
                int wgTotal = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = wgMult.size();
                final int score = wgTotal * wgMult.size();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && plugin.getScoringRules().getMultiplierType() != null
                    && plugin.getScoringRules().getMultiplierType().startsWith("zone_country")) {
                // CQ WW dual/triple multiplier (Rule IV.C), each counted once
                // PER BAND. Zone = received-zone field (slot field1 = multColumn);
                // country resolved from the worked callsign (no DB column);
                // MM/AM resolve to no entity (zone mult only). CQ WW RTTY adds a
                // THIRD multiplier — US states + VE provinces — logged in the
                // state_rcvd field (slot field3), blank for non-W/VE.
                boolean withState = "zone_country_state"
                        .equals(plugin.getScoringRules().getMultiplierType());
                DxccResolver dxr = DxccResolver.getInstance();
                Map<String, Set<String>> zonesByBand = new LinkedHashMap<>();
                Map<String, Set<String>> ctrysByBand = new LinkedHashMap<>();
                Map<String, Set<String>> stateByBand = new LinkedHashMap<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String zone = q.getContestField1();
                    if (zone != null && !zone.isBlank())
                        zonesByBand.computeIfAbsent(b, k -> new HashSet<>()).add(zone.trim());
                    String ent = dxr.entityOf(q.getCallsign());
                    if (ent != null)
                        ctrysByBand.computeIfAbsent(b, k -> new HashSet<>()).add(ent);
                    if (withState) {
                        String st = q.getContestField3();
                        if (st != null && !st.isBlank())
                            stateByBand.computeIfAbsent(b, k -> new HashSet<>())
                                    .add(st.trim().toUpperCase());
                    }
                }
                int zoneMults  = zonesByBand.values().stream().mapToInt(Set::size).sum();
                int ctryMults  = ctrysByBand.values().stream().mapToInt(Set::size).sum();
                int stateMults = stateByBand.values().stream().mapToInt(Set::size).sum();
                int total  = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = zoneMults + ctryMults + stateMults;
                final int score = total * mults;
                final List<String> zonesWorked = zonesByBand.values().stream()
                        .flatMap(Set::stream).distinct().toList();
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                    if (cqZoneMapPane != null) cqZoneMapPane.setAllWorked(zonesWorked);
                });
            } else if (plugin.getScoringRules() != null
                    && "grid_field".equals(plugin.getScoringRules().getMultiplierType())) {
                // WW Digi (Rule IV.C): multiplier = distinct 2-char Maidenhead
                // grid FIELD (first 2 chars of the 4-char grid) counted once
                // PER BAND. Grid is the received-grid field (slot field1).
                // Score = Σ QSO points (distance-scored, all bands) × Σ grid
                // fields (per band).
                Map<String, Set<String>> gfByBand = new LinkedHashMap<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (b.isBlank()) continue;
                    String g = q.getContestField1();
                    if (g == null || g.trim().length() < 2) continue;
                    gfByBand.computeIfAbsent(b, k -> new HashSet<>())
                            .add(g.trim().substring(0, 2).toUpperCase());
                }
                int totalMults = gfByBand.values().stream().mapToInt(Set::size).sum();
                int total = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                final int mults = totalMults;
                final int score = total * mults;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getScoringRules() != null
                    && "wae".equals(plugin.getScoringRules().getMultiplierType())) {
                // WAE-DC (Rule §6/§8): per-band distinct WAE multiplier token,
                // each band's count band-WEIGHTED (80m×4, 40m×3, 20/15/10m×2),
                // summed. Score = (Σ QSO pts [1 each] + Σ QTC pts [1 each]) ×
                // weighted multiplier sum. Token is callsign-derived
                // (WaeMultiplier): WAE country for EU, DXCC / call-area split
                // for non-EU.
                Map<String, Set<String>> tokByBand = new LinkedHashMap<>();
                for (QsoRecord q : ContestQsoDao.getInstance()
                        .fetchByContest(plugin.getContestId())) {
                    if (q.isDupe()) continue;
                    String b = q.getBand() == null ? "" : q.getBand();
                    if (WaeMultiplier.bandWeight(b) == 0) continue;
                    String tok = WaeMultiplier.token(q.getCallsign());
                    if (tok != null)
                        tokByBand.computeIfAbsent(b, k -> new HashSet<>()).add(tok);
                }
                int weighted = 0;
                for (var e : tokByBand.entrySet())
                    weighted += e.getValue().size() * WaeMultiplier.bandWeight(e.getKey());
                int qsoPts = ContestQsoDao.getInstance()
                        .totalPointsByContest(plugin.getContestId());
                int qtcPts = ContestQtcDao.getInstance()
                        .totalQtcPointsByContest(plugin.getContestId());
                final int mults = weighted;
                final int score = (qsoPts + qtcPts) * weighted;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                });
            } else if (plugin.getMultiplierModel() != null
                    && plugin.getMultiplierModel().isPerBand()) {
                // Per-band multiplier accounting (ARRL Intl Digital): total mults =
                // Σ over bands of distinct values on that band. Feeds WorkedGridsPane.
                Map<String, List<String>> workedByBand = new LinkedHashMap<>();
                int totalMults = 0;
                for (String band : contestBands()) {
                    List<String> w = ContestQsoDao.getInstance()
                        .distinctFieldByColumnAndBand(plugin.getContestId(), multCol, band);
                    workedByBand.put(band, w);
                    totalMults += w.size();
                }
                int total = ContestQsoDao.getInstance().totalPointsByContest(plugin.getContestId());
                final int score = total * totalMults;
                final int mults = totalMults;
                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                    if (gridsPane != null) {
                        workedByBand.forEach(gridsPane::setWorked);
                    }
                });
            } else {
                List<String> worked = ContestQsoDao.getInstance()
                    .distinctFieldByColumn(plugin.getContestId(), multCol);
                int total  = ContestQsoDao.getInstance().totalPointsByContest(plugin.getContestId());
                int mults  = worked.size();
                int score  = total * mults;

                Platform.runLater(() -> {
                    if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                    if (lblScore    != null) lblScore.setText(String.valueOf(score));
                    if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                    if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                    sectionLabels.values().forEach(l -> l.getStyleClass().remove("section-worked"));
                    worked.forEach(sec -> {
                        Label lbl = sectionLabels.get(sec);
                        if (lbl != null) lbl.getStyleClass().add("section-worked");
                    });
                    if (ssSectionMapPane  != null) ssSectionMapPane.setAllWorked(worked);
                    if (sweepProgressPane != null) sweepProgressPane.setWorked(mults);
                    if (workedMultsPane   != null) workedMultsPane.setWorked(worked);
                    if (statesMapPane     != null) statesMapPane.setAllWorked(worked);
                    if (dxccMapPane       != null) dxccMapPane.setAllWorked(worked);
                    if (dxccColumnMapPane != null) dxccColumnMapPane.setAllWorked(worked);
                    if (dxccTablePane     != null) dxccTablePane.setAllWorked(worked);
                    if (countyMapPane     != null) countyMapPane.setAllWorked(worked);
                    if (gridMapPane       != null) gridMapPane.setAllWorked(worked);
                });
            }
        } catch (Exception e) {
            log.warn("updateStats failed", e);
        }
    }

    /** Colour each region on the US / Canada maps and the DXCC list if any mode worked it. */
    private void refreshMapsWorked(Map<String, List<String>> workedByMode) {
        Set<String> allWorked = new HashSet<>();
        workedByMode.values().forEach(allWorked::addAll);
        if (usMapPane     != null) usMapPane.setAllWorked(allWorked);
        if (caMapPane     != null) caMapPane.setAllWorked(allWorked);
        if (dxccPane      != null) dxccPane.setAllWorked(allWorked);
        if (dxccMapPane   != null) dxccMapPane.setAllWorked(allWorked);
        if (dxccColumnMapPane != null) dxccColumnMapPane.setAllWorked(allWorked);
        if (dxccTablePane != null) dxccTablePane.setAllWorked(allWorked);
        if (statesMapPane != null) statesMapPane.setAllWorked(allWorked);
        if (cqZoneMapPane != null) cqZoneMapPane.setAllWorked(allWorked);
        if (countyMapPane != null) countyMapPane.setAllWorked(allWorked);
        if (gridMapPane   != null) gridMapPane.setAllWorked(allWorked);
    }

    private void refreshPerModeGrid(Map<String, List<String>> workedByMode) {
        if (perModeGrid == null) return;
        for (String mode : perModeGrid.getModes()) {
            List<String> worked = workedByMode.getOrDefault(mode, List.of());
            for (String mult : perModeGrid.getMults()) {
                perModeGrid.setWorked(mode, mult, worked.contains(mult));
            }
        }
    }

    private String computeMultiplierColumn() {
        if (plugin.getMultiplierModel() == null) return "field1";
        String targetId = plugin.getMultiplierModel().getField();
        int slot = 0;
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            switch (fd.getId()) {
                case "callsign", "serial_sent", "serial_rcvd", "band", "mode",
                     "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent" -> {}
                default -> {
                    if (fd.getId().equals(targetId)) return "field" + (slot + 1);
                    slot++;
                }
            }
        }
        return "field1";
    }

    private int computeQsoHour() {
        try {
            List<QsoRecord> all = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
            LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusHours(1);
            return (int) all.stream()
                .filter(q -> q.getDateTimeUtc() != null && q.getDateTimeUtc().isAfter(cutoff))
                .count();
        } catch (Exception e) { return 0; }
    }

    private void loadQsos() {
        if (plugin == null) return;
        Task<List<QsoRecord>> task = new Task<>() {
            @Override protected List<QsoRecord> call() throws Exception {
                return ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
            }
            @Override protected void succeeded() { qsoData.setAll(getValue()); }
        };
        new Thread(task).start();
    }

    private void updateSerialDisplay() {
        Platform.runLater(() -> {
            Label lbl = (Label) entryBar.lookup("#serialDisplay");
            if (lbl != null) lbl.setText(String.valueOf(serialCounter.get()));
        });
    }

    // ---------------------------------------------------------------
    // Table navigation
    // ---------------------------------------------------------------

    @FXML private void doPageForward() {}
    @FXML private void doPageBack()    {}

    // ---------------------------------------------------------------
    // Menu actions
    // ---------------------------------------------------------------

    @FXML private void menuContestSetup() {
        openContestSetup();
    }

    /** Open the Maidenhead grid square map (for VHF contests). */
    @FXML private void menuGridMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the grid map needs a contest's worked set.").showAndWait();
            return;
        }
        boolean tracksGrid =
            (plugin.getMultiplierModel() != null
                && "grid_rcvd".equals(plugin.getMultiplierModel().getField()))
            || (plugin.getEntryFields() != null
                && plugin.getEntryFields().stream().anyMatch(f -> "grid_rcvd".equals(f.getId())));
        if (!tracksGrid) {
            new Alert(Alert.AlertType.INFORMATION,
                "Grid Square Map is only available for contests that track Maidenhead grids "
                + "(ARRL January/June/September VHF).").showAndWait();
            return;
        }
        if (gridMapStage != null && gridMapStage.isShowing()) {
            gridMapStage.toFront();
            return;
        }
        MaidenheadGridMap map = new MaidenheadGridMap();
        map.setTooltipProvider(grid -> grid);
        map.setOnRegionClicked(grid -> onMultiplierSelected(grid, "DX"));
        gridMapPane = map;

        ScrollPane sp = new ScrollPane(map);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        BorderPane root = new BorderPane(sp);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 1240, 760);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle("Grid Square Map — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            gridMapStage = null;
            gridMapPane  = null;
        });
        gridMapStage = st;
        st.show();

        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed grid map worked-set: {}", e.getMessage());
        }
    }

    /** Open the per-state county map for the active state QSO party plugin. */
    @FXML private void menuCountyMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the county map needs a contest's worked set.").showAndWait();
            return;
        }
        String state = CountyMap.stateFromMultiplierListPath(plugin.getMultiplierList());
        if (state == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "County Map is only available for state QSO party plugins.").showAndWait();
            return;
        }
        if (countyMapStage != null && countyMapStage.isShowing()) {
            countyMapStage.toFront();
            return;
        }
        CountyMap map = new CountyMap(state);
        if (map.regionIds().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION,
                "County map for " + state + " hasn't been built yet — "
              + "no county-" + state.toLowerCase() + ".json resource on the classpath.").showAndWait();
            return;
        }
        map.setTooltipProvider(code -> code);
        map.setOnRegionClicked(code -> onMultiplierSelected(code, "US"));
        countyMapPane = map;

        ScrollPane sp = new ScrollPane(map);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        BorderPane root = new BorderPane(sp);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 940, 730);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle(state + " Counties — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            countyMapStage = null;
            countyMapPane  = null;
        });
        countyMapStage = st;
        st.show();

        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed county map worked-set: {}", e.getMessage());
        }
    }

    /** Open the geographic CQ Zones map in its own window. */
    @FXML private void menuCqZonesMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the CQ Zones map needs a contest's worked set.").showAndWait();
            return;
        }
        boolean tracksZones =
            (plugin.getMultiplierModel() != null
                && "cq_zone".equals(plugin.getMultiplierModel().getField()))
            || (plugin.getEntryFields() != null
                && plugin.getEntryFields().stream().anyMatch(f -> "cq_zone".equals(f.getId())));
        if (!tracksZones) {
            new Alert(Alert.AlertType.INFORMATION,
                "CQ Zones Map is only available for contests that track CQ Zones "
                + "(CQ WW CW/SSB/RTTY/Digital).").showAndWait();
            return;
        }
        if (cqZoneMapStage != null && cqZoneMapStage.isShowing()) {
            cqZoneMapStage.toFront();
            return;
        }
        CqZoneMap map = new CqZoneMap();
        map.setTooltipProvider(zone -> "Zone " + zone);
        map.setOnRegionClicked(zone -> onMultiplierSelected(zone, "DX"));
        cqZoneMapPane = map;

        ScrollPane sp = new ScrollPane(map);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        BorderPane root = new BorderPane(sp);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 1240, 700);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle("CQ Zones Map — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            cqZoneMapStage = null;
            cqZoneMapPane  = null;
        });
        cqZoneMapStage = st;
        st.show();

        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed CQ zones map worked-set: {}", e.getMessage());
        }
    }

    /** Open the geographic states + provinces map in its own window. */
    @FXML private void menuStatesMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the states map needs a contest's worked set.").showAndWait();
            return;
        }
        String mt = plugin.getScoringRules() != null
                ? plugin.getScoringRules().getMultiplierType() : null;
        if (!"states".equals(mt)) {
            new Alert(Alert.AlertType.INFORMATION,
                "States/Provinces Map is only available for contests whose multiplier is "
                + "US states + Canadian provinces (ARRL DX DX-side, ARRL RTTY Roundup).").showAndWait();
            return;
        }
        if (statesMapStage != null && statesMapStage.isShowing()) {
            statesMapStage.toFront();
            return;
        }
        StatesMap map = new StatesMap();
        map.setTooltipProvider(this::stateTooltip);
        map.setOnRegionClicked(code -> onMultiplierSelected(code, "US"));
        statesMapPane = map;

        ScrollPane sp = new ScrollPane(map);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        BorderPane root = new BorderPane(sp);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 1240, 830);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle("States/Provinces Map — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            statesMapStage = null;
            statesMapPane  = null;
        });
        statesMapStage = st;
        st.show();

        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed states map worked-set: {}", e.getMessage());
        }
    }

    /** Open the geographic DXCC world map in its own resizable window. */
    @FXML private void menuWorldMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the world map needs a contest's worked DXCC set.").showAndWait();
            return;
        }
        String mt = plugin.getScoringRules() != null
                ? plugin.getScoringRules().getMultiplierType() : null;
        if (!"dxcc".equals(mt)) {
            new Alert(Alert.AlertType.INFORMATION,
                "World Map is only available for contests whose multiplier is DXCC entities "
                + "(CQ WW, ARRL DX W/VE, WAE, Oceania DX, Baltic, etc.).").showAndWait();
            return;
        }
        if (dxccMapStage != null && dxccMapStage.isShowing()) {
            dxccMapStage.toFront();
            return;
        }
        DxccMap map = new DxccMap();
        map.setTooltipProvider(entity -> {
            DxccMap.EntityInfo info = map.entityInfo().get(entity);
            return info != null ? entity + " — " + info.name() : entity;
        });
        map.setOnRegionClicked(entity -> onMultiplierSelected(entity, "DX"));
        map.setRenderScale(0.80);
        dxccMapPane = map;

        DxccTable table = new DxccTable(map.entityInfo());
        table.setOnRowClicked(prefix -> onMultiplierSelected(prefix, "DX"));
        table.setOnRowHovered(prefix -> map.setCurrent(prefix));
        table.setPrefWidth(420);

        ScrollPane mapScroll = new ScrollPane(map);
        mapScroll.setFitToWidth(false);
        mapScroll.setFitToHeight(false);

        HBox split = new HBox(mapScroll, table);
        HBox.setHgrow(mapScroll, Priority.ALWAYS);

        BorderPane root = new BorderPane(split);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 1440, 720);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle("World Map — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            dxccMapStage  = null;
            dxccMapPane   = null;
            dxccTablePane = null;
        });
        dxccMapStage  = st;
        dxccTablePane = table;
        st.show();

        // Seed with current worked-entity set so the map reflects state
        // immediately. updateStats() will refresh on every QSO save.
        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
            table.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed DXCC map worked-set: {}", e.getMessage());
        }
    }

    /** Open the geographic ARRL section map in its own resizable window. */
    @FXML private void menuSectionMap() {
        if (plugin == null) {
            new Alert(Alert.AlertType.INFORMATION,
                "Load a contest first — the section map needs a contest's worked set.").showAndWait();
            return;
        }
        String mt = plugin.getScoringRules() != null
                ? plugin.getScoringRules().getMultiplierType() : null;
        if (!"sections".equals(mt)) {
            new Alert(Alert.AlertType.INFORMATION,
                "Section Map is only available for contests whose multiplier is ARRL sections "
                + "(currently: Sweepstakes, Field Day, Rookie Roundup).").showAndWait();
            return;
        }
        if (sectionMapStage != null && sectionMapStage.isShowing()) {
            sectionMapStage.toFront();
            return;
        }
        ArrlSectionMap map = new ArrlSectionMap();
        map.setTooltipProvider(this::stateTooltip);
        map.setOnRegionClicked(sec -> onMultiplierSelected(sec, "US"));

        // Route the existing refresh path into this map.
        ssSectionMapPane = map;

        ScrollPane sp = new ScrollPane(map);
        sp.setFitToWidth(false);
        sp.setFitToHeight(false);
        BorderPane root = new BorderPane(sp);
        root.setStyle("-fx-background-color: -primary-bg;");

        Scene scene = new Scene(root, 1240, 830);
        JLogApp.applyTheme(scene);

        Stage st = new Stage();
        st.setTitle("Section Map — " + plugin.getContestName());
        st.setScene(scene);
        st.setOnHidden(ev -> {
            sectionMapStage = null;
            ssSectionMapPane = null;
        });
        sectionMapStage = st;
        st.show();

        // Seed with current worked set so the map reflects state immediately
        // (the next QSO save will also refresh via updateStats()).
        try {
            String multCol = this.multColumn;
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            map.setAllWorked(worked);
        } catch (Exception e) {
            log.warn("Failed to seed section map worked-set: {}", e.getMessage());
        }
    }

    @FXML private void menuNewDatabase() {
        new Alert(Alert.AlertType.INFORMATION, I18n.get("msg.not.implemented")).showAndWait();
    }

    @FXML private void menuBackupDatabase() {
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            java.io.File dest = fc.showSaveDialog(getStage());
            if (dest == null) return;
            java.nio.file.Files.copy(
                java.nio.file.Paths.get(System.getProperty("user.home"), ".j-log", "contest.db"),
                dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            setStatus(I18n.get("status.backup.done"));
        } catch (Exception ex) {
            setStatus(ex.getMessage());
        }
    }

    @FXML private void menuExportCabrillo() {
        // Collect category fields first — they're required in the header and
        // vary between events. Choices persist in AppConfig so the next export
        // inherits the operator's preferences.
        if (!showCabrilloCategoryDialog()) return;

        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Cabrillo", "*.log"));
        java.io.File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.jlog.export.CabrilloExporter.export(plugin, f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    /** Modal dialog collecting the Cabrillo 3.0 category fields. Returns true
     *  if the user clicked Export, false if they cancelled. */
    private boolean showCabrilloCategoryDialog() {
        AppConfig cfg = AppConfig.getInstance();
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(8); grid.setVgap(6);
        grid.setPadding(new Insets(12));

        ComboBox<String> cbOp    = new ComboBox<>(FXCollections.observableArrayList(
            "SINGLE-OP","SINGLE-OP-ASSISTED","MULTI-OP","MULTI-OP-ONE","MULTI-OP-TWO","MULTI-OP-MULTI","CHECKLOG"));
        ComboBox<String> cbBand  = new ComboBox<>(FXCollections.observableArrayList(
            "ALL","160M","80M","40M","20M","15M","10M","6M","2M","70CM","VHF-3-BAND","VHF-FM-ONLY"));
        ComboBox<String> cbMode  = new ComboBox<>(FXCollections.observableArrayList(
            "MIXED","CW","SSB","RTTY","DIGI","FM"));
        ComboBox<String> cbPower = new ComboBox<>(FXCollections.observableArrayList(
            "HIGH","LOW","QRP"));
        ComboBox<String> cbAsst  = new ComboBox<>(FXCollections.observableArrayList(
            "NON-ASSISTED","ASSISTED"));
        ComboBox<String> cbTx    = new ComboBox<>(FXCollections.observableArrayList(
            "ONE","TWO","LIMITED","UNLIMITED","SWL"));
        ComboBox<String> cbStn   = new ComboBox<>(FXCollections.observableArrayList(
            "FIXED","MOBILE","PORTABLE","ROVER","EXPEDITION","HQ","SCHOOL","DISTRIBUTED"));
        ComboBox<String> cbTime  = new ComboBox<>(FXCollections.observableArrayList(
            "","6-HOURS","8-HOURS","12-HOURS","24-HOURS"));
        TextField tfEmail = new TextField(cfg.getCabEmail());
        TextField tfClub  = new TextField(cfg.getCabClub());

        cbOp.setValue(cfg.getCabOperator());
        cbBand.setValue(cfg.getCabBand());
        cbMode.setValue(cfg.getCabMode());
        cbPower.setValue(cfg.getCabPower());
        cbAsst.setValue(cfg.getCabAssisted());
        cbTx.setValue(cfg.getCabTransmitter());
        cbStn.setValue(cfg.getCabStation());
        cbTime.setValue(cfg.getCabTime());

        int r = 0;
        grid.add(new Label("Operator:"),    0, r); grid.add(cbOp,    1, r++);
        grid.add(new Label("Band:"),        0, r); grid.add(cbBand,  1, r++);
        grid.add(new Label("Mode:"),        0, r); grid.add(cbMode,  1, r++);
        grid.add(new Label("Power:"),       0, r); grid.add(cbPower, 1, r++);
        grid.add(new Label("Assisted:"),    0, r); grid.add(cbAsst,  1, r++);
        grid.add(new Label("Transmitter:"), 0, r); grid.add(cbTx,    1, r++);
        grid.add(new Label("Station:"),     0, r); grid.add(cbStn,   1, r++);
        grid.add(new Label("Time:"),        0, r); grid.add(cbTime,  1, r++);
        grid.add(new Label("Email:"),       0, r); grid.add(tfEmail, 1, r++);
        grid.add(new Label("Club:"),        0, r); grid.add(tfClub,  1, r++);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.initOwner(getStage());
        dlg.setTitle("Cabrillo Export — Categories");
        dlg.getDialogPane().setContent(grid);
        ButtonType exportBtn = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(exportBtn, ButtonType.CANCEL);
        ButtonType result = dlg.showAndWait().orElse(ButtonType.CANCEL);
        if (result != exportBtn) return false;

        cfg.setCabOperator(cbOp.getValue());
        cfg.setCabBand(cbBand.getValue());
        cfg.setCabMode(cbMode.getValue());
        cfg.setCabPower(cbPower.getValue());
        cfg.setCabAssisted(cbAsst.getValue());
        cfg.setCabTransmitter(cbTx.getValue());
        cfg.setCabStation(cbStn.getValue());
        cfg.setCabTime(cbTime.getValue() == null ? "" : cbTime.getValue());
        cfg.setCabEmail(tfEmail.getText() == null ? "" : tfEmail.getText().trim());
        cfg.setCabClub(tfClub.getText() == null ? "" : tfClub.getText().trim());
        return true;
    }

    @FXML private void menuExportAdif() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("ADIF", "*.adi","*.adif"));
        java.io.File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.jlog.export.AdifExporter.exportContestAdif(plugin.getContestId(), f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuLotwUpload() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Sign the contest log and upload to LoTW?\n\n"
          + "Contest: " + plugin.getContestName() + "\n"
          + "Station location (tqsl): " + AppConfig.getInstance().getLotwStationLocation(),
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("LoTW Upload");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        Task<com.jlog.export.LotwService.TqslResult> task = new Task<>() {
            @Override protected com.jlog.export.LotwService.TqslResult call() throws Exception {
                var qsos = ContestQsoDao.getInstance().fetchByContest(plugin.getContestId());
                return com.jlog.export.LotwService.signAndUpload(qsos);
            }
            @Override protected void succeeded() {
                var r = getValue();
                setStatus(r.ok() ? "LoTW upload succeeded" : "LoTW upload failed (exit " + r.exitCode + ")");
                if (!r.ok() && !r.stderr.isBlank()) new Alert(Alert.AlertType.ERROR, r.stderr).showAndWait();
            }
            @Override protected void failed() { setStatus("LoTW: " + getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuSetup() {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081").start(); }
        catch (Exception e) { setStatus(e.getMessage()); }
    }

    @FXML private void menuMacros()    { openJHubSetupAt("macros");    }
    @FXML private void menuAmp()       { openJHubSetupAt("amp");       }
    @FXML private void menuAntenna()   { openJHubSetupAt("antenna");   }
    @FXML private void menuUploaders() { openJHubSetupAt("uploaders"); }
    @FXML private void menuBackup()    { openJHubSetupAt("logging");   }  // backup card lives on Logging tab
    @FXML private void menuLearn()     { openJHubSetupAt("learn");     }

    @FXML private void menuReportIssue() {
        String log = System.getProperty("user.home", "") + "/.j-log/logs/j-log.log";
        com.jlog.util.IssueReporter.openGitHubIssue("j-log (contest)", "1.0.51", log);
    }

    private UserTranslationWindow translatorUserWin;
    private DxccTranslationWindow translatorDxccWin;

    @FXML private void openTranslatorUser() {
        if (translatorUserWin == null) translatorUserWin = new UserTranslationWindow();
        translatorUserWin.show();
    }

    @FXML private void openTranslatorDxcc() {
        if (translatorDxccWin == null) translatorDxccWin = new DxccTranslationWindow();
        translatorDxccWin.show();
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
    // Contest Setup dialog (SS exchange: callsign, prec, check, sect)
    // ---------------------------------------------------------------

    private void openContestSetup() {
        AppConfig cfg = AppConfig.getInstance();

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        TextField tfCall = new TextField(cfg.getSsCallsign());
        tfCall.setPrefWidth(150);
        tfCall.textProperty().addListener((o, ov, nv) -> {
            if (nv != null && !nv.equals(nv.toUpperCase())) tfCall.setText(nv.toUpperCase());
        });

        ComboBox<String> cbPrec = new ComboBox<>(
            FXCollections.observableArrayList("A","B","M","Q","S","U"));
        cbPrec.setValue(cfg.getSsPrecedence().isBlank() ? null : cfg.getSsPrecedence());
        cbPrec.setPrefWidth(80);

        TextField tfCheck = new TextField(cfg.getSsCheck());
        tfCheck.setPrefWidth(60);
        tfCheck.setPromptText("e.g. 87");

        List<String> allSections = plugin.getSections() != null
            ? plugin.getSections() : List.of();
        ComboBox<String> cbSect = new ComboBox<>(
            FXCollections.observableArrayList(allSections));
        cbSect.setValue(cfg.getSsSection().isBlank() ? null : cfg.getSsSection());
        cbSect.setPrefWidth(100);

        grid.add(new Label("Callsign:"),     0, 0); grid.add(tfCall,  1, 0);
        grid.add(new Label("Precedence:"),   0, 1); grid.add(cbPrec,  1, 1);
        grid.add(new Label("Check (year):"), 0, 2); grid.add(tfCheck, 1, 2);
        grid.add(new Label("Section:"),      0, 3); grid.add(cbSect,  1, 3);

        Button btnSave   = new Button("Save");
        Button btnCancel = new Button("Cancel");
        btnSave.getStyleClass().add("primary-button");

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(getStage());
        dialog.setTitle("Contest Setup — Sent Exchange");
        dialog.setResizable(false);

        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            cfg.setSsCallsign(tfCall.getText().trim().toUpperCase());
            cfg.setSsPrecedence(cbPrec.getValue() != null ? cbPrec.getValue() : "");
            cfg.setSsCheck(tfCheck.getText().trim());
            cfg.setSsSection(cbSect.getValue() != null ? cbSect.getValue() : "");
            prefillSentFields();
            Label callLbl = (Label) entryBar.lookup("#sentCallsign");
            if (callLbl != null) callLbl.setText(cfg.getSsCallsign());
            dialog.close();
        });

        HBox buttons = new HBox(8, btnSave, btnCancel);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setPadding(new Insets(0, 16, 12, 16));

        VBox root = new VBox(0, grid, buttons);
        Scene scene = new Scene(root);
        JLogApp.applyTheme(scene);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void connectCiv() {
        AppConfig cfg = AppConfig.getInstance();
        String port = cfg.getCivPort();
        int baud = Integer.parseInt(cfg.getCivBaud());
        int addr = Integer.parseInt(cfg.getCivAddress(), 16);
        boolean ok = CivEngine.getInstance().connect(port, baud, (byte) addr);
        Platform.runLater(() -> lblCivStatus.setText(ok ?
            I18n.get("civ.connected", port) : I18n.get("civ.failed")));
    }

    private void setStatus(String msg) {
        Platform.runLater(() -> { if (lblStatus != null) lblStatus.setText(msg); });
    }

    private void updateSolarLabel(JsonNode node) {
        if (lblSolar == null || node == null) return;
        StringBuilder sb = new StringBuilder();
        if (node.hasNonNull("sfi"))  sb.append("SFI ").append(node.get("sfi").asText());
        if (node.hasNonNull("a"))    sb.append(sb.length() > 0 ? " | A " : "A ").append(node.get("a").asText());
        if (node.hasNonNull("k"))    sb.append(sb.length() > 0 ? " K " : "K ").append(node.get("k").asText());
        if (node.hasNonNull("sn"))   sb.append(sb.length() > 0 ? " | SN " : "SN ").append(node.get("sn").asText());
        if (node.hasNonNull("muf"))  sb.append(sb.length() > 0 ? " | MUF " : "MUF ").append(node.get("muf").asText());
        lblSolar.setText(sb.toString());
    }

    private Stage getStage() {
        return (Stage) entryBar.getScene().getWindow();
    }
}
