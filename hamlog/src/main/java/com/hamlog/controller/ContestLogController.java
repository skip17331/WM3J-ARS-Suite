package com.hamlog.controller;

import com.hamlog.app.HamLogApp;
import com.hamlog.civ.CivEngine;
import com.hamlog.db.ContestQsoDao;
import com.hamlog.i18n.I18n;
import com.hamlog.macro.MacroEngine;
import com.hamlog.model.QsoRecord;
import com.hamlog.plugin.ContestPlugin;
import com.hamlog.util.AppConfig;
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
 *   Row 5 — DX Spotting window
 */
public class ContestLogController implements Initializable {

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

    // ---- State ----
    private ContestPlugin plugin;
    private final ObservableList<QsoRecord> qsoData = FXCollections.observableArrayList();
    private final AtomicInteger serialCounter = new AtomicInteger(1);
    private ScheduledExecutorService statsService;

    // Dynamic field map: fieldId -> Control
    private final Map<String, Control> entryFields = new LinkedHashMap<>();
    private TextField tfCallsign;
    private TextField tfOperator;

    private static final DateTimeFormatter TABLE_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initTable();
        initMacroBar();
    }

    /** Called after FXML load with the chosen plugin. */
    public void initPlugin(ContestPlugin p) {
        this.plugin = p;
        buildEntryBar();
        buildRow2Panes();
        initCivListeners();
        initKeyHandlers();
        loadQsos();
        startStatsPoller();
        if (AppConfig.getInstance().getCivAutoConnect()) connectCiv();
    }

    // ---------------------------------------------------------------
    // Dynamic UI construction
    // ---------------------------------------------------------------

    private void buildEntryBar() {
        entryBar.getChildren().clear();
        entryFields.clear();

        HBox rcvdRow = makeExchangeRow("Rcvd");
        HBox sentRow = makeExchangeRow("Sent");

        // Sent row: your callsign (read-only display, from SS setup)
        Label yourCall = new Label(AppConfig.getInstance().getSsCallsign());
        yourCall.setId("sentCallsign");
        yourCall.getStyleClass().add("sent-callsign");
        yourCall.setPrefWidth(130);
        sentRow.getChildren().add(yourCall);

        // Sent row: serial# display (auto-incrementing)
        Label lblSerial = new Label(I18n.get("label.serial") + ":");
        lblSerial.getStyleClass().add("entry-label");
        Label serialDisplay = new Label(String.valueOf(serialCounter.get()));
        serialDisplay.getStyleClass().add("serial-display");
        serialDisplay.setId("serialDisplay");
        sentRow.getChildren().addAll(lblSerial, serialDisplay);

        // Plugin-defined fields routed to rcvd or sent row
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            Label lbl = new Label(fd.getLabel() + ":");
            lbl.getStyleClass().add("entry-label");
            Control ctrl = buildFieldControl(fd);
            ctrl.setId(fd.getId());
            entryFields.put(fd.getId(), ctrl);

            if (fd.getEntryRow() == 1) {
                sentRow.getChildren().addAll(lbl, ctrl);
            } else {
                rcvdRow.getChildren().addAll(lbl, ctrl);
            }
        }

        // Operator field (rcvd row, after plugin fields)
        Label lblOp = new Label(I18n.get("label.operator") + ":");
        lblOp.getStyleClass().add("entry-label");
        tfOperator = new TextField(AppConfig.getInstance().getOperatorName());
        tfOperator.setPrefWidth(80);
        rcvdRow.getChildren().addAll(lblOp, tfOperator);

        // Save / Clear buttons (rcvd row)
        Button btnSave  = new Button(I18n.get("button.save"));
        Button btnClear = new Button(I18n.get("button.clear"));
        btnSave .getStyleClass().add("primary-button");
        btnClear.getStyleClass().add("secondary-button");
        btnSave .setOnAction(e -> doSave());
        btnClear.setOnAction(e -> doClear());
        rcvdRow.getChildren().addAll(btnSave, btnClear);

        // Exchange format hint (rcvd row)
        Label hint = new Label("  " + plugin.getExchangeFormat());
        hint.getStyleClass().add("exchange-hint");
        rcvdRow.getChildren().add(hint);

        // Pre-fill sent-row fields from SS config
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
        if ("callsign".equals(fd.getId())) tfCallsign = tf;
        return tf;
    }

    private void prefillSentFields() {
        AppConfig cfg = AppConfig.getInstance();
        setFieldValue("prec_sent",  cfg.getSsPrecedence());
        setFieldValue("check_sent", cfg.getSsCheck());
        setFieldValue("sect_sent",  cfg.getSsSection());
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
                default -> {
                    tp.setContent(new Label(pd.getTitle()));
                    HBox.setHgrow(tp, Priority.NEVER);
                }
            }
            row2PaneContainer.getChildren().add(tp);
        }
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

        // Fallback: flat grid without scroll
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
            Control bandCtrl = entryFields.get("band");
            if (bandCtrl instanceof ComboBox<?> cb)
                ((ComboBox<String>) cb).setValue(CivEngine.freqToBand(hz));
        }));
        CivEngine.getInstance().setModeListener(mode -> Platform.runLater(() -> {
            Control modeCtrl = entryFields.get("mode");
            if (modeCtrl instanceof ComboBox<?> cb)
                ((ComboBox<String>) cb).setValue(mode);
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
                if (nv != null && nv.length() >= 3) checkDupe(nv.toUpperCase());
            });
        }
    }

    // ---------------------------------------------------------------
    // Save / clear
    // ---------------------------------------------------------------

    @FXML private void doSave() {
        if (tfCallsign == null || tfCallsign.getText().isBlank()) {
            setStatus(I18n.get("error.callsign.required"));
            return;
        }
        QsoRecord q = buildRecord();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                ContestQsoDao.getInstance().insert(q);
                return null;
            }
            @Override protected void succeeded() {
                serialCounter.incrementAndGet();
                updateSerialDisplay();
                doClear();
                loadQsos();
                updateStats();
                if (q.isDupe()) setStatus("⚠ DUPE logged: " + q.getCallsign());
                else setStatus(I18n.get("status.saved", q.getCallsign()));
            }
            @Override protected void failed() {
                setStatus(I18n.get("error.save.failed") + ": " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    @FXML private void doClear() {
        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            // Persistent fields (band) stay; sent-row fields stay
            if (fd.isPersistent() || fd.getEntryRow() == 1) continue;
            Control ctrl = entryFields.get(fd.getId());
            if (ctrl instanceof TextField tf && !tf.getStyleClass().contains("auto-field")) tf.clear();
            else if (ctrl instanceof ComboBox<?> cb) ((ComboBox<String>) cb).getSelectionModel().clearSelection();
        }
        if (tfCallsign != null) { tfCallsign.clear(); tfCallsign.requestFocus(); }
        if (dupeStatusLabel != null) dupeStatusLabel.setText("");
        if (dupeList != null) dupeList.getItems().clear();
    }

    private QsoRecord buildRecord() {
        QsoRecord q = new QsoRecord();
        q.setContestId(plugin.getContestId());
        q.setCallsign(tfCallsign != null ? tfCallsign.getText().trim().toUpperCase() : "");
        q.setDateTimeUtc(LocalDateTime.now(ZoneOffset.UTC));
        q.setOperator(tfOperator != null ? tfOperator.getText() : "");

        String[] fieldSlots = {"field1","field2","field3","field4","field5"};
        int slot = 0;
        StringBuilder exch = new StringBuilder();

        for (ContestPlugin.FieldDef fd : plugin.getEntryFields()) {
            Control ctrl = entryFields.get(fd.getId());
            String val = getControlValue(ctrl);
            switch (fd.getId()) {
                // These are handled outside the slot system
                case "callsign", "prec_sent", "check_sent", "sect_sent" -> {}
                case "serial_sent" -> q.setSerialSent(val);
                case "serial_rcvd" -> q.setSerialReceived(val);
                case "band"        -> q.setBand(val);
                case "mode"        -> q.setMode(val);
                case "rst_sent"    -> q.setRstSent(val);
                case "rst_rcvd"    -> q.setRstReceived(val);
                default -> {
                    if (slot < fieldSlots.length) { setFieldSlot(q, slot, val); slot++; }
                }
            }
            if (val != null && !val.isBlank()) {
                if (exch.length() > 0) exch.append(" ");
                exch.append(val);
            }
        }
        q.setExchange(exch.toString());
        q.setSerialSent(String.valueOf(serialCounter.get()));

        try {
            boolean dupe = ContestQsoDao.getInstance().isDuplicate(
                plugin.getContestId(), q.getCallsign(),
                q.getBand() != null ? q.getBand() : "",
                q.getMode() != null ? q.getMode() : "");
            q.setDupe(dupe);
        } catch (Exception ignored) {}

        q.setPoints(plugin.pointsForMode(q.getMode() != null ? q.getMode() : ""));
        return q;
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
            String multCol = getMultiplierColumn();
            int count  = ContestQsoDao.getInstance().countByContest(plugin.getContestId());
            int total  = ContestQsoDao.getInstance().totalPointsByContest(plugin.getContestId());
            List<String> worked = ContestQsoDao.getInstance()
                .distinctFieldByColumn(plugin.getContestId(), multCol);
            int mults  = worked.size();
            int score  = total * mults;
            int qsoHr  = computeQsoHour();

            Platform.runLater(() -> {
                if (lblQsoCount != null) lblQsoCount.setText(String.valueOf(count));
                if (lblScore    != null) lblScore.setText(String.valueOf(score));
                if (lblMults    != null) lblMults.setText(String.valueOf(mults));
                if (lblQsoHour  != null) lblQsoHour.setText(String.valueOf(qsoHr));
                // Reset all section labels then re-highlight worked ones
                sectionLabels.values().forEach(l -> l.getStyleClass().remove("section-worked"));
                worked.forEach(sec -> {
                    Label lbl = sectionLabels.get(sec);
                    if (lbl != null) lbl.getStyleClass().add("section-worked");
                });
            });
        } catch (Exception ignored) {}
    }

    /**
     * Determine which database column (field1–field5) holds the multiplier value,
     * by computing the slot index of the multiplier field id in the plugin's entry fields.
     * Fields with special handling (callsign, band, rst, serial, sent-row) don't consume slots.
     */
    private String getMultiplierColumn() {
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
                java.nio.file.Paths.get(System.getProperty("user.home"), ".hamlog", "contest.db"),
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
                com.hamlog.export.CabrilloExporter.export(plugin, f.toPath());
                return null;
            }
            @Override protected void succeeded() { setStatus(I18n.get("status.export.done")); }
            @Override protected void failed()    { setStatus(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void menuSetup() {
        try { new ProcessBuilder("xdg-open", "http://localhost:8081").start(); }
        catch (Exception e) { setStatus(e.getMessage()); }
    }

    @FXML private void menuDxSpotting() { dxPane.setExpanded(true); }
    @FXML private void menuConnectCiv()    { connectCiv(); }
    @FXML private void menuDisconnectCiv() {
        CivEngine.getInstance().disconnect();
        lblCivStatus.setText(I18n.get("civ.disconnected"));
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

        grid.add(new Label("Callsign:"),    0, 0); grid.add(tfCall,  1, 0);
        grid.add(new Label("Precedence:"),  0, 1); grid.add(cbPrec,  1, 1);
        grid.add(new Label("Check (year):"),0, 2); grid.add(tfCheck, 1, 2);
        grid.add(new Label("Section:"),     0, 3); grid.add(cbSect,  1, 3);

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
            // Refresh sent row
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
        HamLogApp.applyTheme(scene);
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
