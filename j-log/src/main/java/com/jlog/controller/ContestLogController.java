package com.jlog.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jlog.app.JLogApp;
import com.jlog.civ.CivEngine;
import com.jlog.cluster.HubEngine;
import com.jlog.db.ContestQsoDao;
import com.jlog.i18n.I18n;
import com.jlog.macro.MacroEngine;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.ui.contest.DxccListPane;
import com.jlog.ui.contest.PerModeMultGridPane;
import com.jlog.ui.contest.SweepProgressPane;
import com.jlog.ui.contest.WorkedBeforePane;
import com.jlog.ui.contest.WorkedGridsPane;
import com.jlog.ui.contest.WorkedMultsPane;
import com.jlog.ui.map.RegionMapPane;
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

    // ---- QSO table ----
    @FXML private TableView<QsoRecord>          qsoTable;
    @FXML private TableColumn<QsoRecord,String> colCall;
    @FXML private TableColumn<QsoRecord,String> colTime;
    @FXML private TableColumn<QsoRecord,String> colBand;
    @FXML private TableColumn<QsoRecord,String> colMode;
    @FXML private TableColumn<QsoRecord,String> colSentSerial;
    @FXML private TableColumn<QsoRecord,String> colExchange;
    @FXML private TableColumn<QsoRecord,String> colPts;
    @FXML private TableColumn<QsoRecord,String> colOp;

    // ---- Stats labels ----
    @FXML private Label lblQsoCount;
    @FXML private Label lblScore;
    @FXML private Label lblMults;
    @FXML private Label lblQsoHour;
    @FXML private Label lblStatus;
    @FXML private Label lblCivStatus;

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
    private WorkedGridsPane      gridsPane;
    private RegionMapPane        ssSectionMapPane;
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

    // Divider position captured when DX pane was last expanded; restored on re-expand
    private double dxExpandedDividerPos = 0.5;

    private static final DateTimeFormatter TABLE_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

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

        if (AppConfig.getInstance().getCivAutoConnect()) connectCiv();

        HubEngine.getInstance().sendContestActive(p);
        // Wire CONTEST_INACTIVE to window close (double runLater ensures scene is attached)
        Platform.runLater(() -> Platform.runLater(() -> {
            javafx.scene.Scene sc = entryBar.getScene();
            if (sc != null && sc.getWindow() instanceof javafx.stage.Stage st) {
                st.setOnCloseRequest(e -> HubEngine.getInstance().sendContestInactive());
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

        HBox rcvdRow = makeExchangeRow("Rcvd");
        HBox sentRow = makeExchangeRow("Sent");

        Label yourCall = new Label(AppConfig.getInstance().getSsCallsign());
        yourCall.setId("sentCallsign");
        yourCall.getStyleClass().add("sent-callsign");
        yourCall.setPrefWidth(130);
        sentRow.getChildren().add(yourCall);

        Label lblSerial = new Label(I18n.get("label.serial") + ":");
        lblSerial.getStyleClass().add("entry-label");
        Label serialDisplay = new Label(String.valueOf(serialCounter.get()));
        serialDisplay.getStyleClass().add("serial-display");
        serialDisplay.setId("serialDisplay");
        sentRow.getChildren().addAll(lblSerial, serialDisplay);

        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            Label lbl = new Label(fd.getLabel() + ":");
            lbl.getStyleClass().add("entry-label");
            Control ctrl = buildFieldControl(fd);
            ctrl.setId(fd.getId());
            entryFields.put(fd.getId(), ctrl);
            entryLabels.put(fd.getId(), lbl);

            if (fd.getEntryRow() == 1) {
                sentRow.getChildren().addAll(lbl, ctrl);
            } else {
                rcvdRow.getChildren().addAll(lbl, ctrl);
            }
        }

        Label lblOp = new Label(I18n.get("label.operator") + ":");
        lblOp.getStyleClass().add("entry-label");
        tfOperator = new TextField(AppConfig.getInstance().getOperatorName());
        tfOperator.setPrefWidth(80);
        rcvdRow.getChildren().addAll(lblOp, tfOperator);

        Button btnSave  = new Button(I18n.get("button.save"));
        Button btnClear = new Button(I18n.get("button.clear"));
        btnSave .getStyleClass().add("primary-button");
        btnClear.getStyleClass().add("secondary-button");
        btnSave .setOnAction(e -> doSave());
        btnClear.setOnAction(e -> doClear());
        rcvdRow.getChildren().addAll(btnSave, btnClear);

        Label hint = new Label("  " + plugin.getExchangeFormat());
        hint.getStyleClass().add("exchange-hint");
        rcvdRow.getChildren().add(hint);

        prefillSentFields();
        entryBar.getChildren().addAll(rcvdRow, sentRow);
    }

    private HBox makeExchangeRow(String prefix) {
        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        Label lbl = new Label(prefix + ":");
        lbl.getStyleClass().add("exchange-row-label");
        row.getChildren().add(lbl);
        return row;
    }

    private Control buildFieldControl(ContestPlugin.FieldDef fd) {
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
        if ("callsign".equals(fd.getId()) || "text".equals(fd.getType())) {
            tf.textProperty().addListener((obs, ov, nv) -> {
                if (nv != null && nv.contains(" "))
                    tf.setText(nv.replace(" ", "").toUpperCase());
            });
        }
        // Unified options-based validation: any text field whose plugin FieldDef
        // declares options (or is a band/mode with a generic fallback) gets its
        // value validated against that list — red ring on mismatch.
        Collection<String> allowed = effectiveOptions(fd);
        if (allowed != null && !allowed.isEmpty()) {
            tf.textProperty().addListener((obs, o, n) -> applyValidStyle(tf, isAllowed(allowed, n)));
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
            case "numeric"    -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank() || n.trim().matches("[0-9]+")));
            // Field Day station class: digits followed by A/B/C/D/E/F/I.
            case "fd_class"   -> tf.textProperty().addListener((obs, o, n) ->
                applyValidStyle(tf, n == null || n.isBlank()
                    || n.trim().toUpperCase().matches("[0-9]{1,2}[A-FI]")));
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
            if (allowed != null && !isAllowed(allowed, value))
                return "Invalid " + fd.getLabel() + ": '" + value + "'";
            String v = fd.getValidator();
            if ("maidenhead".equals(v) && !Maidenhead.isValid(value))
                return "Invalid grid: '" + value + "'";
            if ("numeric".equals(v) && !value.trim().matches("[0-9]+"))
                return "Field " + fd.getLabel() + " must be numeric";
            if ("fd_class".equals(v) && !value.trim().toUpperCase().matches("[0-9]{1,2}[A-FI]"))
                return "Field " + fd.getLabel() + " must be a Field Day class (e.g. 2A, 1D)";
        }
        return null;
    }

    private void prefillSentFields() {
        AppConfig cfg = AppConfig.getInstance();
        setFieldValue("prec_sent",  cfg.getSsPrecedence());
        setFieldValue("check_sent", cfg.getSsCheck());
        setFieldValue("sect_sent",  cfg.getSsSection());
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
        return "";
    }

    private void buildRow2Panes() {
        row2PaneContainer.getChildren().clear();
        if (plugin.getRow2Panes() == null) return;

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
                    tp.setContent(buildSectionPane(pd));
                    HBox.setHgrow(tp, Priority.ALWAYS);
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
                    HBox.setHgrow(tp, Priority.ALWAYS);
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
                case "ss_section_map" -> {
                    ssSectionMapPane = RegionMapPane.ssSections();
                    ssSectionMapPane.setTooltipProvider(this::stateTooltip);
                    ssSectionMapPane.setOnRegionClicked(sec -> onMultiplierSelected(sec, "US"));
                    ScrollPane sp = new ScrollPane(ssSectionMapPane);
                    sp.setFitToWidth(true);
                    sp.setPrefHeight(320);
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
            row2PaneContainer.getChildren().add(tp);
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
     *  monotonically across app restarts (counter = max previously sent + 1). */
    private void initSerialCounter() {
        try {
            int maxSoFar = ContestQsoDao.getInstance().maxSerialSent(plugin.getContestId());
            serialCounter.set(maxSoFar + 1);
        } catch (Exception e) {
            log.warn("could not resume serial counter for {}", plugin.getContestId(), e);
            serialCounter.set(1);
        }
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
            case "US", "CA" -> firstPresent("state_prov_rcvd", "state_rcvd", "section");
            case "DX"       -> firstPresent("dxcc_rcvd", "country_rcvd");
            default          -> null;
        };
        if (target != null) setFieldValue(target, value);
        if (tfCallsign != null) tfCallsign.requestFocus();
        if (usMapPane  != null && "US".equals(region)) usMapPane.setCurrent(value);
        if (caMapPane  != null && "CA".equals(region)) caMapPane.setCurrent(value);
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
            HBox zonesBox = new HBox(2);
            zonesBox.getStyleClass().add("section-zones");

            for (Map<String, Object> zone : zoneGroups) {
                String zoneName = (String) zone.get("name");
                List<String> sects = (List<String>) zone.get("sections");

                VBox col = new VBox(1);
                col.getStyleClass().add("zone-column");

                Label header = new Label(zoneName);
                header.getStyleClass().add("zone-header");
                col.getChildren().add(header);

                if (sects != null) {
                    for (String sec : sects) {
                        Label lbl = new Label(sec);
                        lbl.getStyleClass().add("section-label");
                        sectionLabels.put(sec, lbl);
                        col.getChildren().add(lbl);
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
        colExchange  .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getExchange()));
        colPts       .setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getPoints())));
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
                    serialCounter.incrementAndGet();
                    updateSerialDisplay();
                }
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
            } else if (plugin.isPerModeMultipliers()) {
                // Dupe rule is mode-specific, band-independent (e.g. ARRL 10M).
                dupe = ContestQsoDao.getInstance().isDuplicatePerMode(
                    plugin.getContestId(), q.getCallsign(),
                    q.getMode() != null ? q.getMode() : "");
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
        StringBuilder exch = new StringBuilder();

        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            Control ctrl = entryFields.get(fd.getId());
            String val = getControlValue(ctrl);
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
            if (val != null && !val.isBlank()) {
                if (exch.length() > 0) exch.append(" ");
                exch.append(val);
            }
        }
        q.setExchange(exch.toString());
        q.setPoints(computeQsoPoints(q));
    }

    /** Resolve QSO points honouring region-pair (ARRL 160M) / band-class (Intl
     *  Digital) / rookie-roundup rules when declared; falls back to mode / default. */
    private int computeQsoPoints(QsoRecord q) {
        String mode = q.getMode() != null ? q.getMode() : "";
        String band = q.getBand() != null ? q.getBand() : "";
        var rules = plugin.getScoringRules();
        if (rules != null && rules.isRookieRoundupScoring()) {
            // Received 2-digit "year first licensed" vs current 2-digit year.
            // Rookie = licensed within the last 3 calendar years (current + 2 prior).
            String yearField = firstPresent("year_rcvd", "chk_rcvd", "check_rcvd");
            String yearStr   = yearField == null ? "" : getFieldValue(yearField);
            if (yearStr != null && yearStr.trim().matches("[0-9]{1,2}")) {
                int yy    = Integer.parseInt(yearStr.trim());
                int curYy = LocalDateTime.now(ZoneOffset.UTC).getYear() % 100;
                int delta = ((curYy - yy) + 100) % 100;   // wraparound-safe
                return delta <= 2 ? 2 : 1;
            }
            return 1;
        }
        if (rules != null && rules.getPointsByRegionPair() != null
                && !rules.getPointsByRegionPair().isEmpty()) {
            String myCall = AppConfig.getInstance().getStationCallsign();
            if (myCall == null || myCall.isBlank()) myCall = AppConfig.getInstance().getSsCallsign();
            return plugin.pointsFor(regionTag(myCall), regionTag(q.getCallsign()), mode);
        }
        if (rules != null && ((rules.getPointsByBand() != null && !rules.getPointsByBand().isEmpty())
                || (rules.getPointsByBandClass() != null && !rules.getPointsByBandClass().isEmpty()))) {
            return plugin.pointsForBand(band, mode);
        }
        return plugin.pointsForMode(mode);
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
        if (usMapPane != null) usMapPane.setAllWorked(allWorked);
        if (caMapPane != null) caMapPane.setAllWorked(allWorked);
        if (dxccPane  != null) dxccPane.setAllWorked(allWorked);
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

    @FXML private void menuDatabaseTools() {
        DatabaseToolsController.show(getStage());
    }

    @FXML private void menuSetup() {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081").start(); }
        catch (Exception e) { setStatus(e.getMessage()); }
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

    private Stage getStage() {
        return (Stage) entryBar.getScene().getWindow();
    }
}
