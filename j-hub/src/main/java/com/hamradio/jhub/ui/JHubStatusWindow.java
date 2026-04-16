package com.hamradio.jhub.ui;

import com.hamradio.jhub.*;
import com.hamradio.jhub.model.JHubConfig;
import com.hamradio.jhub.model.RigStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * JHubStatusWindow — JavaFX status window showing live j-hub state.
 *
 * Updates every second via a JavaFX Timeline.
 *
 * Managed Apps section shows j-map, j-log, j-bridge, and j-digi.
 * Each managed app row shows a coloured running/stopped indicator,
 * a Launch button (when stopped) and a Stop button (when running).
 * j-bridge uses Catppuccin Mauve (#cba6f7) to distinguish it
 * from the green "connected" colour used for WebSocket registrations.
 *
 * When J-Hub launches j-log or j-bridge it passes --launched-by-hub
 * so those apps suppress their own splash screens.
 */
public class JHubStatusWindow {

    private static final Logger log = LoggerFactory.getLogger(JHubStatusWindow.class);

    private final Stage      stage;
    private final JHubServer jHubServer;

    private Timeline ticker;

    // Status grid labels
    private Label lblStatus;
    private Label lblUptime;
    private Label lblWsPort;
    private Label lblWebUrl;
    private Label lblCluster;
    private Label lblSpotRate;
    private Label lblRig;

    // Connected-apps section (WebSocket registrations)
    private VBox appListBox;

    // Managed-apps section (process launcher)
    private Label  lblJMap;
    private Label  lblJLog;
    private Label  lblJBridge;
    private Label  lblJDigi;
    private Button btnJMap;
    private Button btnJLog;
    private Button btnJBridge;
    private Button btnJDigi;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    // Catppuccin Mocha palette
    private static final String C_GREEN  = "#a6e3a1";
    private static final String C_RED    = "#f38ba8";
    private static final String C_MAUVE  = "#cba6f7";
    private static final String C_BLUE   = "#89b4fa";
    private static final String C_TEXT   = "#cdd6f4";
    private static final String C_SUBTLE = "#6c7086";
    private static final String C_BASE   = "#1e1e2e";
    private static final String C_SURF0  = "#313244";

    public JHubStatusWindow(Stage stage, JHubServer jHubServer) {
        this.stage      = stage;
        this.jHubServer = jHubServer;
    }

    public void show() {
        stage.setTitle("j-Hub");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            e.consume();
            ticker.stop();
            lblStatus.setText("Closing DX Telnet Session…");
            lblStatus.setTextFill(Color.web("#fab387"));
            lblCluster.setText("○ Disconnecting");
            lblCluster.setTextFill(Color.web("#fab387"));
            log.info("Closing DX telnet session before exit");
            new Thread(() -> {
                ClusterManager.getInstance().disconnect();
                Platform.exit();
            }, "shutdown-ui").start();
        });

        VBox root = buildUI();
        Scene scene = new Scene(root, 420, 680);
        scene.setFill(Color.web(C_BASE));
        stage.setScene(scene);
        stage.show();

        ticker = new Timeline(new KeyFrame(Duration.seconds(1), e -> refresh()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();

        refresh();
    }

    // ---------------------------------------------------------------
    // UI layout
    // ---------------------------------------------------------------

    private VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: " + C_BASE + ";");

        // Title
        Label title = new Label("\uD83D\uDD2D j-Hub  |  WM3j ARS Suite");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web(C_TEXT));

        // ── Status grid ───────────────────────────────────────────────────────
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 8, 0));

        lblStatus   = styledValue("● Running");
        lblStatus.setTextFill(Color.web(C_GREEN));
        lblUptime   = styledValue("—");
        lblWsPort   = styledValue("—");
        lblWebUrl   = styledValue("—");
        lblCluster  = styledValue("—");
        lblSpotRate = styledValue("—");
        lblRig      = styledValue("—");

        int r = 0;
        grid.add(styledKey("Status:"),    0, r); grid.add(lblStatus,   1, r++);
        grid.add(styledKey("Uptime:"),    0, r); grid.add(lblUptime,   1, r++);
        grid.add(styledKey("WS Port:"),   0, r); grid.add(lblWsPort,   1, r++);
        grid.add(styledKey("Web UI:"),    0, r); grid.add(lblWebUrl,   1, r++);
        grid.add(styledKey("Cluster:"),   0, r); grid.add(lblCluster,  1, r++);
        grid.add(styledKey("Spot rate:"), 0, r); grid.add(lblSpotRate, 1, r++);
        grid.add(styledKey("Rig:"),       0, r); grid.add(lblRig,      1, r++);

        Button btnOpen = new Button("Open Web Config UI");
        btnOpen.setStyle("-fx-background-color: " + C_BLUE + "; -fx-text-fill: " + C_BASE +
                         "; -fx-font-weight: bold;");
        btnOpen.setOnAction(e -> openBrowser());

        // ── WebSocket connected apps ──────────────────────────────────────────
        Label wsTitle = new Label("Connected Apps");
        wsTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        wsTitle.setTextFill(Color.web(C_MAUVE));

        appListBox = new VBox(4);
        appListBox.setPadding(new Insets(4, 0, 0, 0));

        // ── Managed apps (process launcher) ──────────────────────────────────
        Label managedTitle = new Label("Managed Apps");
        managedTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        managedTitle.setTextFill(Color.web(C_MAUVE));

        lblJMap    = styledValue("○ j-map");
        lblJLog    = styledValue("○ j-log");
        lblJBridge = styledValue("○ j-bridge");
        lblJDigi   = styledValue("○ j-digi");

        btnJMap    = launchButton("jMap");
        btnJLog    = launchButton("j-log");
        btnJBridge = launchButton("j-bridge");
        btnJDigi   = launchButton("j-digi");

        VBox managedBox = new VBox(6,
            appRow(lblJMap,    btnJMap),
            appRow(lblJLog,    btnJLog),
            appRow(lblJBridge, btnJBridge),
            appRow(lblJDigi,   btnJDigi));
        managedBox.setPadding(new Insets(4, 0, 0, 0));

        root.getChildren().addAll(
            title,
            new Separator(),
            grid,
            btnOpen,
            new Separator(),
            wsTitle,
            appListBox,
            new Separator(),
            managedTitle,
            managedBox);

        return root;
    }

    // ---------------------------------------------------------------
    // Periodic refresh
    // ---------------------------------------------------------------

    private void refresh() {
        var cfg   = ConfigManager.getInstance().getConfig();
        var cache = StateCache.getInstance();

        // Uptime
        long sec = java.time.Duration.between(JHubMain.START_TIME, Instant.now()).getSeconds();
        lblUptime.setText(formatUptime(sec));

        if (cfg != null) {
            lblWsPort.setText(String.valueOf(cfg.jHub.websocketPort));
            lblWebUrl.setText("http://localhost:" + cfg.jHub.webConfigPort);
        }

        // Cluster
        boolean clusterOk = ClusterManager.getInstance().isConnected();
        lblCluster.setText(clusterOk ? "● Connected" : "○ Disconnected");
        lblCluster.setTextFill(clusterOk ? Color.web(C_GREEN) : Color.web(C_RED));

        // Spot rate / rig
        lblSpotRate.setText(String.format("%.0f / min", cache.getSpotsPerMinute()));
        RigStatus rig = cache.getLastRigStatus();
        if (rig != null) {
            lblRig.setText(String.format("%.3f MHz  %s  %dW",
                rig.frequency / 1_000_000.0, rig.mode, rig.power));
        }

        // WebSocket registered apps
        appListBox.getChildren().clear();
        if (jHubServer != null) {
            jHubServer.getSessions().values().stream()
                .filter(s -> s.registered)
                .forEach(s -> {
                    String text = s.appName + (s.version.isEmpty() ? "" : " v" + s.version);
                    // Colour-code by app name for quick visual scanning
                    Color c = "j-bridge".equals(s.appName)
                            ? Color.web(C_MAUVE)
                            : Color.web(C_GREEN);
                    Label lbl = styledValue("● " + text);
                    lbl.setTextFill(c);
                    appListBox.getChildren().add(lbl);
                });
        }
        if (appListBox.getChildren().isEmpty()) {
            appListBox.getChildren().add(styledValue("No apps connected"));
        }

        // Managed-app process state
        AppLauncher al = AppLauncher.getInstance();
        refreshAppRow("jMap",     al.isRunning("jMap"),     lblJMap,    btnJMap);
        refreshAppRow("j-log",    al.isRunning("j-log"),    lblJLog,    btnJLog);
        refreshAppRow("j-bridge", al.isRunning("j-bridge"), lblJBridge, btnJBridge);
        refreshAppRow("j-digi",   al.isRunning("j-digi"),   lblJDigi,   btnJDigi);
    }

    // ---------------------------------------------------------------
    // Managed-app row helpers
    // ---------------------------------------------------------------

    /** Update a single managed-app row's label and button state. */
    private void refreshAppRow(String name, boolean running, Label lbl, Button btn) {
        if (running) {
            lbl.setText("● " + name);
            lbl.setTextFill(Color.web(C_GREEN));
            btn.setText("Stop");
            btn.setStyle("-fx-background-color: " + C_SURF0 + "; -fx-text-fill: " + C_RED + ";");
            btn.setOnAction(e -> {
                AppLauncher.getInstance().kill(name);
                log.info("Stopped {} from status window", name);
            });
        } else {
            lbl.setText("○ " + name);
            lbl.setTextFill(Color.web(C_SUBTLE));
            btn.setText("Launch");
            btn.setStyle("-fx-background-color: " + C_SURF0 + "; -fx-text-fill: " + C_TEXT + ";");
            btn.setOnAction(e -> launchApp(name));
        }
    }

    /**
     * Launch a managed app using its configured command.
     * J-Log and J-Bridge receive --launched-by-hub so they suppress their splash screens.
     * Shows an inline error label if no command is configured.
     */
    private void launchApp(String name) {
        JHubConfig.AppsSection apps = ConfigManager.getInstance().getApps();
        JHubConfig.AppLaunchEntry entry;
        boolean passHubFlag;

        if ("jMap".equals(name)) {
            entry = apps != null ? apps.jMap : null;
            passHubFlag = false;
        } else if ("j-log".equals(name)) {
            entry = apps != null ? apps.jLog : null;
            passHubFlag = true;
        } else if ("j-bridge".equals(name)) {
            entry = apps != null ? apps.jBridge : null;
            passHubFlag = true;
        } else if ("j-digi".equals(name)) {
            entry = apps != null ? apps.jDigi : null;
            passHubFlag = false;
        } else {
            entry = null;
            passHubFlag = false;
        }

        if (entry == null || entry.command == null || entry.command.isBlank()) {
            log.warn("No command configured for '{}' — open Web Config UI to set it", name);
            showInlineError(name + ": no command configured. Set it in the Web Config UI.");
            return;
        }

        String cmd = passHubFlag ? entry.command + " --launched-by-hub" : entry.command;
        String err = AppLauncher.getInstance().launch(name, cmd);
        if (err != null) {
            log.error("Failed to launch {}: {}", name, err);
            showInlineError("Failed to launch " + name + ": " + err);
        } else {
            log.info("Launched {} from status window", name);
        }
    }

    private void showInlineError(String message) {
        // Briefly display error in the rig label area (resets on next tick)
        lblRig.setText(message);
        lblRig.setTextFill(Color.web(C_RED));
    }

    private Button launchButton(String name) {
        Button btn = new Button("Launch");
        btn.setStyle("-fx-background-color: " + C_SURF0 + "; -fx-text-fill: " + C_TEXT + ";");
        btn.setPrefWidth(70);
        return btn;
    }

    private HBox appRow(Label lbl, Button btn) {
        HBox row = new HBox(10, lbl, btn);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(lbl, Priority.ALWAYS);
        return row;
    }

    // ---------------------------------------------------------------
    // Shared helpers
    // ---------------------------------------------------------------

    private Label styledKey(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web(C_SUBTLE));
        lbl.setMinWidth(80);
        return lbl;
    }

    private Label styledValue(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web(C_TEXT));
        return lbl;
    }

    private String formatUptime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void openBrowser() {
        var cfg = ConfigManager.getInstance().getJHub();
        String url = "http://localhost:" + cfg.webConfigPort;
        try {
            if (Desktop.isDesktopSupported() &&
                Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}
        try {
            new ProcessBuilder("xdg-open", url).start();
        } catch (Exception e) {
            log.warn("Could not open browser: {}", e.getMessage());
        }
    }
}
