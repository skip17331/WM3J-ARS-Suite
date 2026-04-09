package com.hamlog.controller;

import com.hamlog.app.HamLogApp;
import com.hamlog.civ.CivEngine;
import com.hamlog.db.MacroDao;
import com.hamlog.i18n.I18n;
import com.hamlog.macro.MacroEngine;
import com.hamlog.model.Macro;
import com.hamlog.util.AppConfig;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Setup window controller (Setup.fxml).
 *
 * Tabs: Station, CI-V, Lookup, Macros, Display, Database, Logging
 */
public class SetupController implements Initializable {

    // ---- Station tab ----
    @FXML private TextField tfCallsign;
    @FXML private TextField tfOperator;
    @FXML private TextField tfQth;
    @FXML private TextField tfGrid;
    @FXML private TextField tfLat;
    @FXML private TextField tfLon;
    @FXML private TextField tfRadio;
    @FXML private TextField tfAntenna;
    @FXML private TextField tfPower;

    // ---- CI-V tab ----
    @FXML private TextField       tfCivPort;
    @FXML private ComboBox<String> cbCivBaud;
    @FXML private TextField       tfCivAddress;
    @FXML private CheckBox        chkCivAutoConnect;
    @FXML private Label           lblCivTestResult;

    // ---- Lookup tab ----
    @FXML private TextField     tfQrzUser;
    @FXML private PasswordField pfQrzPass;

    // ---- Macros tab ----
    @FXML private ListView<Macro>  macroList;
    @FXML private TextField        tfMacroName;
    @FXML private ComboBox<String> cbMacroFKey;
    @FXML private ComboBox<String> cbActionType;
    @FXML private TextField        tfActionData;
    @FXML private TextArea         taMacroActions;
    @FXML private Label            lblMacroHelp;

    // ---- Display tab ----
    @FXML private ToggleGroup      themeToggle;
    @FXML private RadioButton      rbLight;
    @FXML private RadioButton      rbDark;
    @FXML private Spinner<Integer> spFontSize;
    @FXML private ComboBox<String> cbLanguage;

    // ---- Database tab ----
    @FXML private Label lblDbStatus;

    // ---- Logging tab ----
    @FXML private ToggleGroup logToggle;
    @FXML private RadioButton rbLogNormal;
    @FXML private RadioButton rbLogDebug;

    // ---- Buttons ----
    @FXML private Button btnSave;
    @FXML private Button btnCancel;

    private List<Macro> macros = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        AppConfig cfg = AppConfig.getInstance();

        // Station
        tfCallsign.setText(cfg.getStationCallsign());
        tfOperator.setText(cfg.getOperatorName());
        tfQth     .setText(cfg.getQth());
        tfGrid    .setText(cfg.getGridSquare());
        tfLat     .setText(cfg.getLatitude());
        tfLon     .setText(cfg.getLongitude());
        tfRadio   .setText(cfg.getRadioModel());
        tfAntenna .setText(cfg.getAntenna());
        tfPower   .setText(cfg.getDefaultPower());

        // CI-V
        tfCivPort        .setText(cfg.getCivPort());
        tfCivAddress     .setText(cfg.getCivAddress());
        chkCivAutoConnect.setSelected(cfg.getCivAutoConnect());
        cbCivBaud.setItems(FXCollections.observableArrayList(
            "300","1200","2400","4800","9600","19200","38400","57600","115200"));
        cbCivBaud.setValue(cfg.getCivBaud());

        // QRZ
        tfQrzUser.setText(cfg.getQrzUsername());
        pfQrzPass.setText(cfg.getQrzPassword());

        // Theme
        String theme = cfg.getTheme();
        rbLight.setSelected("light".equals(theme));
        rbDark .setSelected("dark" .equals(theme));

        // Font size
        spFontSize.setValueFactory(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(8, 28, cfg.getFontSize()));

        // Language
        cbLanguage.setItems(FXCollections.observableArrayList("en", "de", "es", "fr", "pt", "ja"));
        cbLanguage.setValue(cfg.getLanguage());

        // Logging
        rbLogNormal.setSelected(!cfg.isDebugMode());
        rbLogDebug .setSelected( cfg.isDebugMode());

        // Macros
        loadMacros();

        // Action type choices
        cbActionType.setItems(FXCollections.observableArrayList(
            "PTT_ON", "PTT_OFF", "CW_TEXT", "VOICE_PLAY", "DELAY_MS", "EXCHANGE_INSERT"));
        cbActionType.setValue("CW_TEXT");

        lblMacroHelp.setText(
            "PTT_ON / PTT_OFF — no data needed\n" +
            "CW_TEXT — data: text to send (e.g. CQ DE W1AW)\n" +
            "VOICE_PLAY — data: audio filename\n" +
            "DELAY_MS — data: milliseconds (e.g. 500)\n" +
            "EXCHANGE_INSERT — data: exchange text to insert");
    }

    // ---------------------------------------------------------------
    // Macros
    // ---------------------------------------------------------------

    private void loadMacros() {
        macros = MacroDao.getInstance().fetchAll();
        macroList.setItems(FXCollections.observableArrayList(macros));
        macroList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Macro item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? "" :
                    (item.getFKey() > 0 ? "F" + item.getFKey() + " — " : "") + item.getName());
            }
        });

        cbMacroFKey.setItems(FXCollections.observableArrayList(
            "0 (none)","1","2","3","4","5","6","7","8","9","10","11","12"));

        macroList.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) {
                tfMacroName.setText(nv.getName());
                cbMacroFKey.setValue(nv.getFKey() == 0 ? "0 (none)" : String.valueOf(nv.getFKey()));
                try {
                    taMacroActions.setText(
                        new com.fasterxml.jackson.databind.ObjectMapper()
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(nv.getActions()));
                } catch (Exception ex) { taMacroActions.setText("[]"); }
            }
        });
    }

    @FXML private void doNewMacro() {
        Macro m = new Macro();
        m.setName("New Macro");
        m.setFKey(0);
        try {
            MacroDao.getInstance().insert(m);
            loadMacros();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    @FXML private void doSaveMacro() {
        Macro sel = macroList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        sel.setName(tfMacroName.getText());
        String fkStr = cbMacroFKey.getValue();
        sel.setFKey(fkStr == null || fkStr.startsWith("0") ? 0 : Integer.parseInt(fkStr));
        try {
            String json = taMacroActions.getText().trim();
            List<Macro.MacroAction> actions = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, new com.fasterxml.jackson.databind.ObjectMapper()
                    .getTypeFactory().constructCollectionType(List.class, Macro.MacroAction.class));
            sel.setActions(actions);
            MacroDao.getInstance().update(sel);
            loadMacros();
        } catch (Exception ex) {
            showError("Invalid JSON: " + ex.getMessage());
        }
    }

    @FXML private void doDeleteMacro() {
        Macro sel = macroList.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            MacroDao.getInstance().delete(sel.getId());
            loadMacros();
            taMacroActions.clear();
            tfMacroName.clear();
        } catch (Exception ex) {
            showError(ex.getMessage());
        }
    }

    /** Append a new action to the JSON in the macro TextArea. */
    @FXML private void doAddAction() {
        String type = cbActionType.getValue();
        String data = tfActionData.getText().trim();
        if (type == null) return;

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();

            // Parse existing JSON (default to empty array)
            String existing = taMacroActions.getText().trim();
            if (existing.isEmpty()) existing = "[]";
            com.fasterxml.jackson.databind.node.ArrayNode array =
                (com.fasterxml.jackson.databind.node.ArrayNode) mapper.readTree(existing);

            // Build new action node
            com.fasterxml.jackson.databind.node.ObjectNode action =
                mapper.createObjectNode();
            action.put("type", type);
            if (!data.isEmpty()) {
                if ("DELAY_MS".equals(type)) {
                    try { action.put("intData", Integer.parseInt(data)); }
                    catch (NumberFormatException e) { action.put("data", data); }
                } else {
                    action.put("data", data);
                }
            }
            array.add(action);

            taMacroActions.setText(
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(array));
            tfActionData.clear();
        } catch (Exception ex) {
            showError("Could not add action: " + ex.getMessage());
        }
    }

    @FXML private void doClearActions() {
        taMacroActions.setText("[]");
    }

    // ---------------------------------------------------------------
    // CI-V test
    // ---------------------------------------------------------------

    @FXML private void doCivTest() {
        try {
            int baud = Integer.parseInt(cbCivBaud.getValue());
            int addr = Integer.parseInt(tfCivAddress.getText().trim(), 16);
            boolean ok = CivEngine.getInstance().connect(
                tfCivPort.getText().trim(), baud, (byte) addr);
            lblCivTestResult.setText(ok ? "✓ Connected" : "✗ Failed");
        } catch (Exception ex) {
            lblCivTestResult.setText("Error: " + ex.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Database tab
    // ---------------------------------------------------------------

    @FXML private void doBackupDatabase() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Backup hamlog.db");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        fc.setInitialFileName("hamlog_backup.db");
        File dest = fc.showSaveDialog(getStage());
        if (dest == null) return;
        try {
            java.nio.file.Files.copy(
                java.nio.file.Paths.get(System.getProperty("user.home"), ".hamlog", "hamlog.db"),
                dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            lblDbStatus.setText(I18n.get("status.backup.done"));
        } catch (Exception ex) {
            lblDbStatus.setText(ex.getMessage());
        }
    }

    @FXML private void doExportAdif() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ADIF Files", "*.adi"));
        File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.hamlog.export.AdifExporter.exportAdif(f.toPath());
                return null;
            }
            @Override protected void succeeded() { lblDbStatus.setText(I18n.get("status.export.done")); }
            @Override protected void failed()    { lblDbStatus.setText(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    @FXML private void doExportCsv() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showSaveDialog(getStage());
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.hamlog.export.AdifExporter.exportCsv(f.toPath());
                return null;
            }
            @Override protected void succeeded() { lblDbStatus.setText(I18n.get("status.export.done")); }
            @Override protected void failed()    { lblDbStatus.setText(getException().getMessage()); }
        };
        new Thread(task).start();
    }

    // ---------------------------------------------------------------
    // Save / Cancel
    // ---------------------------------------------------------------

    @FXML private void doSave() {
        AppConfig cfg = AppConfig.getInstance();

        // Station
        cfg.setStationCallsign(tfCallsign.getText().trim().toUpperCase());
        cfg.setOperatorName   (tfOperator.getText().trim());
        cfg.setQth            (tfQth.getText().trim());
        cfg.setGridSquare     (tfGrid.getText().trim().toUpperCase());
        cfg.setLatitude       (tfLat.getText().trim());
        cfg.setLongitude      (tfLon.getText().trim());
        cfg.setRadioModel     (tfRadio.getText().trim());
        cfg.setAntenna        (tfAntenna.getText().trim());
        cfg.setDefaultPower   (tfPower.getText().trim());

        // CI-V
        cfg.setCivPort       (tfCivPort.getText().trim());
        cfg.setCivBaud       (cbCivBaud.getValue());
        cfg.setCivAddress    (tfCivAddress.getText().trim());
        cfg.setCivAutoConnect(chkCivAutoConnect.isSelected());

        // QRZ
        cfg.setQrzUsername(tfQrzUser.getText().trim());
        cfg.setQrzPassword(pfQrzPass.getText());

        // Theme + font
        cfg.setTheme(rbDark.isSelected() ? "dark" : "light");
        cfg.setFontSize(spFontSize.getValue());

        // Language
        cfg.setLanguage(cbLanguage.getValue());

        // Logging
        boolean debug = rbLogDebug.isSelected();
        cfg.setDebugMode(debug);
        com.hamlog.util.LoggingConfigurator.configure(debug);

        // Re-apply theme / font to this window immediately
        HamLogApp.applyTheme(btnSave.getScene());

        closeWindow();
    }

    @FXML private void doCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }

    private Stage getStage() {
        return (Stage) btnSave.getScene().getWindow();
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }
}
