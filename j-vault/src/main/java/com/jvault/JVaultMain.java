package com.jvault;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Path;

/**
 * JVaultMain — application entry point.
 *
 * Launches the embedded Jetty web server (default port 8083) and a
 * small JavaFX status window with an "Open in Browser" button.
 *
 * The actual UI lives in the browser at http://localhost:8083.
 *
 * Override the port with -Djvault.port=NNNN.
 */
public class JVaultMain extends Application {

    private static final Logger log = LoggerFactory.getLogger(JVaultMain.class);

    private JVaultServer server;
    private int port;

    @Override public void start(Stage stage) {
        port = Integer.getInteger("jvault.port", 8083);

        // Start the web server.
        server = new JVaultServer(port);
        try {
            server.start();
        } catch (Exception e) {
            log.error("J-Vault failed to start web server: {}", e.getMessage(), e);
            showFatal(stage, "Web server failed to start:\n" + e.getMessage());
            return;
        }

        // Touch the DAO once to trigger schema creation + migration.
        try {
            InventoryDao.getInstance().listTypes();
        } catch (Exception e) {
            log.warn("Initial DAO touch failed: {}", e.getMessage());
        }

        Platform.setImplicitExit(false);
        buildStatusWindow(stage);

        // Shutdown hook so the server stops cleanly on Ctrl-C / SIGTERM.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down J-Vault.");
            if (server != null) server.stop();
        }, "jvault-shutdown"));
    }

    private void buildStatusWindow(Stage stage) {
        Path dbPath = Path.of(System.getProperty("user.home"), ".j-vault", "inventory.db");
        Label title = new Label("J-Vault");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #cba6f7;");

        Label sub = new Label("Shack inventory + estate handoff");
        sub.setStyle("-fx-font-size: 12px; -fx-text-fill: #a6adc8;");

        Label status = new Label("● Running on http://localhost:" + port);
        status.setStyle("-fx-font-size: 13px; -fx-text-fill: #a6e3a1;");

        Label dbLabel = new Label("Data: " + dbPath);
        dbLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c7086;");

        Button open = new Button("Open in Browser");
        open.setStyle("-fx-background-color: #cba6f7; -fx-text-fill: #1e1e2e; "
                    + "-fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");
        open.setOnAction(e -> openBrowser());

        Button hide = new Button("Hide Window");
        hide.setStyle("-fx-background-color: transparent; -fx-text-fill: #a6adc8; "
                    + "-fx-border-color: #45475a; -fx-border-radius: 3; "
                    + "-fx-padding: 8 14; -fx-cursor: hand;");
        hide.setOnAction(e -> stage.hide());

        Button quit = new Button("Quit J-Vault");
        quit.setStyle("-fx-background-color: transparent; -fx-text-fill: #f38ba8; "
                    + "-fx-border-color: #45475a; -fx-border-radius: 3; "
                    + "-fx-padding: 8 14; -fx-cursor: hand;");
        quit.setOnAction(e -> {
            if (server != null) server.stop();
            Platform.exit();
            System.exit(0);
        });

        HBox buttons = new HBox(8, open, hide, quit);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, title, sub, new Label(""), status, dbLabel, new Label(""), buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(28, 32, 24, 32));
        root.setStyle("-fx-background-color: #1e1e2e;");

        Scene scene = new Scene(root, 460, 280);
        scene.setFill(Color.web("#1e1e2e"));
        stage.setScene(scene);
        stage.setTitle("J-Vault");
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> stage.hide());  // close = hide; quit via button
        stage.show();
    }

    private void openBrowser() {
        try {
            URI uri = URI.create("http://localhost:" + port + "/");
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return;
            }
            // Linux fallback when AWT Desktop is unavailable on the JDK build.
            Runtime.getRuntime().exec(new String[]{"xdg-open", uri.toString()});
        } catch (Exception ex) {
            log.warn("openBrowser failed: {}", ex.getMessage());
        }
    }

    private void showFatal(Stage stage, String message) {
        Label l = new Label(message);
        l.setStyle("-fx-font-size: 13px; -fx-text-fill: #f38ba8; -fx-padding: 20px;");
        l.setWrapText(true);
        VBox root = new VBox(l);
        root.setStyle("-fx-background-color: #1e1e2e;");
        stage.setScene(new Scene(root, 480, 200));
        stage.setTitle("J-Vault — error");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
