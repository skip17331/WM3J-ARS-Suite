package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.service.DatabaseToolsService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.*;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Controller for the Database Tools modal popup (DatabaseTools.fxml).
 * Open via {@link #show(Stage)}.
 */
public class DatabaseToolsController implements Initializable {

    @FXML private ComboBox<String> cbDatabase;
    @FXML private Label            lblDbStatus;
    @FXML private Label            lblExportStatus;

    private Stage stage;

    private final DatabaseToolsService svc = DatabaseToolsService.getInstance();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshDatabases();
    }

    /** Opens the Database Tools popup as an application-modal dialog. */
    public static void show(Stage owner) {
        try {
            FXMLLoader loader = new FXMLLoader(
                DatabaseToolsController.class.getResource("/com/jlog/fxml/DatabaseTools.fxml"));
            VBox root = loader.load();
            DatabaseToolsController ctrl = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(owner);
            dialog.setTitle("Database Tools");
            dialog.setResizable(false);
            ctrl.stage = dialog;

            String active = svc_getActive();
            if (active != null) ctrl.cbDatabase.setValue(active);

            Scene scene = new Scene(root);
            JLogApp.applyTheme(scene);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                "Could not open Database Tools: " + e.getMessage(),
                ButtonType.OK).showAndWait();
        }
    }

    // ---------------------------------------------------------------
    // Database management handlers
    // ---------------------------------------------------------------

    @FXML private void doRefreshDatabases() {
        refreshDatabases();
    }

    @FXML private void doAddDatabase() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Add Database");
        dlg.setHeaderText("Enter a name for the new log database.");
        dlg.setContentText("Name:");
        Optional<String> result = dlg.showAndWait();
        result.ifPresent(name -> {
            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return svc.createDatabase(name);
                }
                @Override protected void succeeded() {
                    refreshDatabases();
                    cbDatabase.setValue(getValue());
                    setDbStatus("Created: " + getValue());
                }
                @Override protected void failed() {
                    setDbStatus("Error: " + getException().getMessage());
                }
            };
            new Thread(task).start();
        });
    }

    @FXML private void doSelectDatabase() {
        String selected = cbDatabase.getValue();
        if (selected == null || selected.isBlank()) {
            setDbStatus("No database selected.");
            return;
        }
        svc.setActiveDatabase(selected);
        setDbStatus("Active database set to: " + selected + "  (restart to apply)");
    }

    @FXML private void doDeleteDatabase() {
        String selected = cbDatabase.getValue();
        if (selected == null || selected.isBlank()) {
            setDbStatus("No database selected.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete '" + selected + "'?  This cannot be undone.",
            ButtonType.YES, ButtonType.CANCEL);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.YES) return;
            Task<Void> task = new Task<>() {
                @Override protected Void call() throws Exception {
                    svc.deleteDatabase(selected);
                    return null;
                }
                @Override protected void succeeded() {
                    refreshDatabases();
                    setDbStatus("Deleted: " + selected);
                }
                @Override protected void failed() {
                    setDbStatus("Error: " + getException().getMessage());
                }
            };
            new Thread(task).start();
        });
    }

    // ---------------------------------------------------------------
    // Export handlers
    // ---------------------------------------------------------------

    @FXML private void doExportAdif() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export ADIF");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("ADIF Files", "*.adi"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.jlog.export.AdifExporter.exportAdif(f.toPath());
                return null;
            }
            @Override protected void succeeded() {
                setExportStatus("ADIF exported: " + f.getName());
            }
            @Override protected void failed() {
                setExportStatus("Export failed: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    @FXML private void doExportCsv() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Export CSV");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File f = fc.showSaveDialog(stage);
        if (f == null) return;
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                com.jlog.export.AdifExporter.exportCsv(f.toPath());
                return null;
            }
            @Override protected void succeeded() {
                setExportStatus("CSV exported: " + f.getName());
            }
            @Override protected void failed() {
                setExportStatus("Export failed: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    @FXML private void doClose() {
        if (stage != null) stage.close();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void refreshDatabases() {
        Task<List<String>> task = new Task<>() {
            @Override protected List<String> call() throws Exception {
                return svc.listLogDatabases();
            }
            @Override protected void succeeded() {
                String current = cbDatabase.getValue();
                cbDatabase.setItems(FXCollections.observableArrayList(getValue()));
                if (current != null && getValue().contains(current)) cbDatabase.setValue(current);
                else if (!getValue().isEmpty()) cbDatabase.setValue(getValue().get(0));
                setDbStatus("Active: " + svc.getActiveDatabase());
            }
            @Override protected void failed() {
                setDbStatus("Error listing databases: " + getException().getMessage());
            }
        };
        new Thread(task).start();
    }

    private void setDbStatus(String msg) {
        Platform.runLater(() -> lblDbStatus.setText(msg));
    }

    private void setExportStatus(String msg) {
        Platform.runLater(() -> lblExportStatus.setText(msg));
    }

    private static String svc_getActive() {
        try { return DatabaseToolsService.getInstance().getActiveDatabase(); }
        catch (Exception e) { return null; }
    }
}
