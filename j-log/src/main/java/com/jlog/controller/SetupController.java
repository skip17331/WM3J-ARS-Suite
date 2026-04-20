package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.civ.CivEngine;
import com.jlog.db.MacroDao;
import com.jlog.i18n.I18n;
import com.jlog.macro.MacroEngine;
import com.jlog.model.Macro;
import com.jlog.util.AppConfig;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    // ---- DX Cluster tab ----
    @FXML private TextField tfClusterServer;
    @FXML private TextField tfClusterPort;
    @FXML private TextField tfClusterLogin;
    @FXML private CheckBox  chkClusterAutoConnect;
    @FXML private CheckBox  chkBand160, chkBand80, chkBand40, chkBand30, chkBand20;
    @FXML private CheckBox  chkBand17, chkBand15, chkBand12, chkBand10, chkBand6, chkBand2;
    @FXML private CheckBox  chkModeSSB, chkModeCW, chkModeFT8, chkModeFT4;
    @FXML private CheckBox  chkModeRTTY, chkModePSK31, chkModeJS8;
    @FXML private Label     lblClusterStatus;

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

        // DX Cluster — load from j-hub
        loadClusterConfig();

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
        fc.setTitle("Backup j-log.db");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite DB", "*.db"));
        fc.setInitialFileName("j-log_backup.db");
        File dest = fc.showSaveDialog(getStage());
        if (dest == null) return;
        try {
            java.nio.file.Files.copy(
                java.nio.file.Paths.get(System.getProperty("user.home"), ".j-log", "j-log.db"),
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
                com.jlog.export.AdifExporter.exportAdif(f.toPath());
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
                com.jlog.export.AdifExporter.exportCsv(f.toPath());
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

    // ---------------------------------------------------------------
    // DX Cluster
    // ---------------------------------------------------------------

    private void loadClusterConfig() {
        new Thread(() -> {
            try {
                String json = hubGet("http://localhost:8081/api/config");
                com.fasterxml.jackson.databind.ObjectMapper m =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode root = m.readTree(json);
                com.fasterxml.jackson.databind.JsonNode c = root.path("cluster");
                boolean autoC  = c.path("autoConnect").asBoolean(false);
                String  server = c.path("server").asText("");
                int     port   = c.path("port").asInt(7373);
                String  login  = c.path("loginCallsign").asText("");
                Set<String> bands = new HashSet<>();
                c.path("filters").path("bands").forEach(n -> bands.add(n.asText()));
                Set<String> modes = new HashSet<>();
                c.path("filters").path("modes").forEach(n -> modes.add(n.asText()));
                Platform.runLater(() -> {
                    chkClusterAutoConnect.setSelected(autoC);
                    tfClusterServer.setText(server);
                    tfClusterPort.setText(String.valueOf(port));
                    tfClusterLogin.setText(login);
                    applyBandFilters(bands);
                    applyModeFilters(modes);
                });
            } catch (Exception ignored) {}
        }, "load-cluster-cfg").start();
    }

    private void applyBandFilters(Set<String> enabled) {
        boolean all = enabled.isEmpty();
        bandBoxes().forEach((b, box) -> box.setSelected(all || enabled.contains(b)));
    }

    private void applyModeFilters(Set<String> enabled) {
        boolean all = enabled.isEmpty();
        modeBoxes().forEach((m, box) -> box.setSelected(all || enabled.contains(m)));
    }

    private Map<String, CheckBox> bandBoxes() {
        Map<String, CheckBox> m = new LinkedHashMap<>();
        m.put("160m", chkBand160); m.put("80m",  chkBand80);  m.put("40m", chkBand40);
        m.put("30m",  chkBand30);  m.put("20m",  chkBand20);  m.put("17m", chkBand17);
        m.put("15m",  chkBand15);  m.put("12m",  chkBand12);  m.put("10m", chkBand10);
        m.put("6m",   chkBand6);   m.put("2m",   chkBand2);
        return m;
    }

    private Map<String, CheckBox> modeBoxes() {
        Map<String, CheckBox> m = new LinkedHashMap<>();
        m.put("SSB",   chkModeSSB);  m.put("CW",    chkModeCW);   m.put("FT8",   chkModeFT8);
        m.put("FT4",   chkModeFT4);  m.put("RTTY",  chkModeRTTY); m.put("PSK31", chkModePSK31);
        m.put("JS8",   chkModeJS8);
        return m;
    }

    @FXML private void doClusterConnect() {
        String server = tfClusterServer.getText().trim();
        if (server.isBlank()) { lblClusterStatus.setText("Server required"); return; }
        int port;
        try { port = Integer.parseInt(tfClusterPort.getText().trim()); }
        catch (Exception e) { port = 7373; }
        final int finalPort = port;
        String login = tfClusterLogin.getText().trim();
        String body  = String.format("{\"server\":\"%s\",\"port\":%d,\"loginCallsign\":\"%s\"}",
            server, finalPort, login);
        lblClusterStatus.setText("Connecting...");
        new Thread(() -> {
            try {
                hubPost("http://localhost:8081/api/cluster/connect", body);
                Platform.runLater(() -> lblClusterStatus.setText("Connected"));
            } catch (Exception e) {
                Platform.runLater(() -> lblClusterStatus.setText("Error: " + e.getMessage()));
            }
        }, "cluster-connect").start();
    }

    @FXML private void doClusterDisconnect() {
        new Thread(() -> {
            try {
                hubPost("http://localhost:8081/api/cluster/disconnect", "");
                Platform.runLater(() -> lblClusterStatus.setText("Disconnected"));
            } catch (Exception e) {
                Platform.runLater(() -> lblClusterStatus.setText("Error: " + e.getMessage()));
            }
        }, "cluster-disconnect").start();
    }

    private void saveClusterConfig() {
        boolean autoC  = chkClusterAutoConnect.isSelected();
        String  server = tfClusterServer.getText().trim();
        int     port;
        try { port = Integer.parseInt(tfClusterPort.getText().trim()); } catch (Exception e) { port = 7373; }
        String  login  = tfClusterLogin.getText().trim();
        Set<String> bands = new LinkedHashSet<>();
        bandBoxes().forEach((b, box) -> { if (box.isSelected()) bands.add(b); });
        Set<String> modes = new LinkedHashSet<>();
        modeBoxes().forEach((m, box) -> { if (box.isSelected()) modes.add(m); });
        final int fp = port;
        new Thread(() -> {
            try {
                String currentJson = hubGet("http://localhost:8081/api/config");
                com.fasterxml.jackson.databind.ObjectMapper mapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.node.ObjectNode root =
                    (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(currentJson);
                com.fasterxml.jackson.databind.node.ObjectNode cluster = mapper.createObjectNode();
                cluster.put("autoConnect",    autoC);
                cluster.put("server",         server);
                cluster.put("port",           fp);
                cluster.put("loginCallsign",  login);
                com.fasterxml.jackson.databind.node.ObjectNode filters = mapper.createObjectNode();
                com.fasterxml.jackson.databind.node.ArrayNode bandsArr = mapper.createArrayNode();
                bands.forEach(bandsArr::add);
                com.fasterxml.jackson.databind.node.ArrayNode modesArr = mapper.createArrayNode();
                modes.forEach(modesArr::add);
                filters.set("bands", bandsArr);
                filters.set("modes", modesArr);
                cluster.set("filters", filters);
                root.set("cluster", cluster);
                hubPost("http://localhost:8081/api/config", mapper.writeValueAsString(root));
            } catch (Exception ignored) {}
        }, "save-cluster-cfg").start();
    }

    private static String hubGet(String url) throws Exception {
        java.net.HttpURLConnection c =
            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(3000);
        return new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void hubPost(String url, String body) throws Exception {
        java.net.HttpURLConnection c =
            (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        c.setRequestMethod("POST");
        c.setDoOutput(true);
        c.setConnectTimeout(2000);
        c.setReadTimeout(3000);
        c.setRequestProperty("Content-Type", "application/json");
        if (!body.isEmpty())
            c.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
        c.getInputStream().readAllBytes();
    }

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
        com.jlog.util.LoggingConfigurator.configure(debug);

        // DX Cluster — save to j-hub
        saveClusterConfig();

        // Re-apply theme / font to this window immediately
        JLogApp.applyTheme(btnSave.getScene());

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
