package com.hamradio.hub.ui;

import com.hamradio.hub.*;
import com.hamradio.hub.model.RigStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
 * HubStatusWindow — JavaFX status window showing live hub state.
 *
 * Updates every second via a JavaFX Timeline (no background thread needed).
 * Provides a system-tray icon on supported platforms and an "Open Web UI"
 * link to launch the browser config page.
 */
public class HubStatusWindow {

    private static final Logger log = LoggerFactory.getLogger(HubStatusWindow.class);

    private final Stage     stage;
    private final HubServer hubServer;

    // Refreshed labels
    private Timeline ticker;

    private Label lblStatus;
    private Label lblUptime;
    private Label lblWsPort;
    private Label lblWebUrl;
    private Label lblCluster;
    private Label lblSpotRate;
    private Label lblRig;
    private VBox  appListBox;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    public HubStatusWindow(Stage stage, HubServer hubServer) {
        this.stage     = stage;
        this.hubServer = hubServer;
    }

    public void show() {
        stage.setTitle("j-Hub");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> {
            e.consume(); // hold the window open briefly so the message is visible
            ticker.stop();
            lblStatus.setText("Closing DX Telnet Session…");
            lblStatus.setTextFill(Color.web("#fab387")); // orange
            lblCluster.setText("○ Disconnecting");
            lblCluster.setTextFill(Color.web("#fab387"));
            log.info("Closing DX telnet session before exit");
            new Thread(() -> {
                ClusterManager.getInstance().disconnect();
                Platform.exit();
            }, "shutdown-ui").start();
        });

        VBox root = buildUI();
        Scene scene = new Scene(root, 420, 480);
        scene.setFill(Color.web("#1e1e2e"));
        stage.setScene(scene);
        stage.show();

        // Refresh every second
        ticker = new Timeline(
            new KeyFrame(Duration.seconds(1), e -> refresh())
        );
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();

        refresh(); // immediate first paint
    }

    // ---------------------------------------------------------------
    // UI layout
    // ---------------------------------------------------------------

    private VBox buildUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-background-color: #1e1e2e;");

        // Title
        Label title = new Label("🔭 j-Hub  |  WM3j ARS Suite");
        title.setFont(Font.font("System", FontWeight.BOLD, 18));
        title.setTextFill(Color.web("#cdd6f4"));

        // Status grid
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(6);
        grid.setPadding(new Insets(8, 0, 8, 0));

        lblStatus   = styledValue("● Running");
        lblStatus.setTextFill(Color.web("#a6e3a1"));
        lblUptime   = styledValue("—");
        lblWsPort   = styledValue("—");
        lblWebUrl   = styledValue("—");
        lblCluster  = styledValue("—");
        lblSpotRate = styledValue("—");
        lblRig      = styledValue("—");

        int row = 0;
        grid.add(styledKey("Status:"),    0, row); grid.add(lblStatus,   1, row++);
        grid.add(styledKey("Uptime:"),    0, row); grid.add(lblUptime,   1, row++);
        grid.add(styledKey("WS Port:"),   0, row); grid.add(lblWsPort,   1, row++);
        grid.add(styledKey("Web UI:"),    0, row); grid.add(lblWebUrl,   1, row++);
        grid.add(styledKey("Cluster:"),   0, row); grid.add(lblCluster,  1, row++);
        grid.add(styledKey("Spot rate:"), 0, row); grid.add(lblSpotRate, 1, row++);
        grid.add(styledKey("Rig:"),       0, row); grid.add(lblRig,      1, row++);

        // Open web UI button
        Button btnOpen = new Button("Open Web Config UI");
        btnOpen.setStyle("-fx-background-color: #89b4fa; -fx-text-fill: #1e1e2e; -fx-font-weight: bold;");
        btnOpen.setOnAction(e -> openBrowser());

        // Connected apps
        Label appsTitle = new Label("Connected Apps");
        appsTitle.setFont(Font.font("System", FontWeight.BOLD, 13));
        appsTitle.setTextFill(Color.web("#cba6f7"));

        appListBox = new VBox(4);
        appListBox.setPadding(new Insets(4, 0, 0, 0));

        root.getChildren().addAll(title, new Separator(), grid, btnOpen,
                                  new Separator(), appsTitle, appListBox);
        return root;
    }

    // ---------------------------------------------------------------
    // Periodic refresh
    // ---------------------------------------------------------------

    private void refresh() {
        var cfg = ConfigManager.getInstance().getConfig();
        var cache = StateCache.getInstance();

        // Uptime
        long sec = java.time.Duration.between(HubMain.START_TIME, Instant.now()).getSeconds();
        lblUptime.setText(formatUptime(sec));

        // Ports
        if (cfg != null) {
            lblWsPort.setText(String.valueOf(cfg.hub.websocketPort));
            lblWebUrl.setText("http://localhost:" + cfg.hub.webConfigPort);
        }

        // Cluster
        boolean clusterOk = ClusterManager.getInstance().isConnected();
        lblCluster.setText(clusterOk ? "● Connected" : "○ Disconnected");
        lblCluster.setTextFill(clusterOk ? Color.web("#a6e3a1") : Color.web("#f38ba8"));

        // Spot rate
        lblSpotRate.setText(String.format("%.0f / min", cache.getSpotsPerMinute()));

        // Rig
        RigStatus rig = cache.getLastRigStatus();
        if (rig != null) {
            lblRig.setText(String.format("%.3f MHz  %s  %dW",
                rig.frequency / 1_000_000.0, rig.mode, rig.power));
        }

        // Connected apps (rebuild list)
        appListBox.getChildren().clear();
        if (hubServer != null) {
            hubServer.getSessions().values().stream()
                .filter(s -> s.registered)
                .forEach(s -> {
                    String text = s.appName + (s.version.isEmpty() ? "" : " v" + s.version);
                    Label lbl = styledValue("● " + text);
                    lbl.setTextFill(Color.web("#a6e3a1"));
                    appListBox.getChildren().add(lbl);
                });
        }
        if (appListBox.getChildren().isEmpty()) {
            appListBox.getChildren().add(styledValue("No apps connected"));
        }
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Label styledKey(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#6c7086"));
        lbl.setMinWidth(80);
        return lbl;
    }

    private Label styledValue(String text) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.web("#cdd6f4"));
        return lbl;
    }

    private String formatUptime(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void openBrowser() {
        var cfg = ConfigManager.getInstance().getHub();
        String url = "http://localhost:" + cfg.webConfigPort;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return;
            }
        } catch (Exception ignored) {}
        // Fallback: xdg-open (Linux)
        try {
            new ProcessBuilder("xdg-open", url).start();
        } catch (Exception e) {
            log.warn("Could not open browser: {}", e.getMessage());
        }
    }
}
