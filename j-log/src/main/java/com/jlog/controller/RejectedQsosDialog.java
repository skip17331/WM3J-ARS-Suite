package com.jlog.controller;

import com.jlog.db.QsoDao;
import com.jlog.export.AdifImporter;
import com.jlog.model.QsoRecord;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Modal "Rejected QSO Records" review dialog.
 * <p>
 * Opens after an ADIF import that produced one or more rejections (e.g.
 * missing CALL, DB constraint violation). Operator sees one row per
 * rejection with the reason and whatever was parsed; an "Edit &amp; Approve"
 * button per row pops a small editor pre-filled from the parsed fields,
 * lets the operator fix what was missing, and inserts the record on Save.
 * Closing the dialog triggers {@code onClose} so the main controller
 * refreshes its QSO table.
 */
public final class RejectedQsosDialog {

    private static final DateTimeFormatter DT_DISPLAY =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DT_PARSE =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private RejectedQsosDialog() {}

    /** Open the dialog on the JavaFX thread. {@code onClose} runs after the
     *  operator dismisses the window — typically a {@code loadQsos()} call
     *  so any approved records show up in the main log table. */
    public static void show(List<AdifImporter.RejectedRecord> rejected, Runnable onClose) {
        Stage stage = new Stage();
        stage.setTitle("Rejected QSO Records (" + rejected.size() + ")");
        stage.initModality(Modality.NONE);   // non-modal so operator can switch tabs

        ObservableList<AdifImporter.RejectedRecord> data =
            FXCollections.observableArrayList(rejected);

        TableView<AdifImporter.RejectedRecord> table = new TableView<>(data);
        table.setPlaceholder(new Label("All rejections resolved — close this window."));

        TableColumn<AdifImporter.RejectedRecord, String> colNum = new TableColumn<>("#");
        colNum.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().recordNumber)));
        colNum.setPrefWidth(50);
        colNum.setStyle("-fx-alignment: CENTER;");

        TableColumn<AdifImporter.RejectedRecord, String> colReason = new TableColumn<>("Reason");
        colReason.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().reason));
        colReason.setPrefWidth(260);

        TableColumn<AdifImporter.RejectedRecord, String> colCall = new TableColumn<>("Callsign");
        colCall.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().partial != null ? safe(c.getValue().partial.getCallsign()) : ""));
        colCall.setPrefWidth(100);
        colCall.setStyle("-fx-alignment: CENTER;");

        TableColumn<AdifImporter.RejectedRecord, String> colDate = new TableColumn<>("Date / Time");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().partial != null && c.getValue().partial.getDateTimeUtc() != null
                ? c.getValue().partial.getDateTimeUtc().format(DT_DISPLAY) : ""));
        colDate.setPrefWidth(130);
        colDate.setStyle("-fx-alignment: CENTER;");

        TableColumn<AdifImporter.RejectedRecord, String> colBand = new TableColumn<>("Band");
        colBand.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().partial != null ? safe(c.getValue().partial.getBand()) : ""));
        colBand.setPrefWidth(60);
        colBand.setStyle("-fx-alignment: CENTER;");

        TableColumn<AdifImporter.RejectedRecord, String> colMode = new TableColumn<>("Mode");
        colMode.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().partial != null ? safe(c.getValue().partial.getMode()) : ""));
        colMode.setPrefWidth(70);
        colMode.setStyle("-fx-alignment: CENTER;");

        TableColumn<AdifImporter.RejectedRecord, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(200);
        colAction.setCellFactory(actionCellFactory(stage, data));

        table.getColumns().addAll(colNum, colReason, colCall, colDate, colBand, colMode, colAction);

        Label header = new Label(
            "Review records the import couldn't insert. Click Edit & Approve to fix the\n" +
            "missing or invalid fields, or Skip to drop the record from this list.");
        header.setWrapText(true);
        header.setPadding(new Insets(6, 8, 6, 8));

        Button btnCloseAll = new Button("Close");
        btnCloseAll.setOnAction(e -> stage.close());
        HBox bottom = new HBox(btnCloseAll);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(table);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 980, 460);
        stage.setScene(scene);
        stage.setOnHiding(e -> { if (onClose != null) onClose.run(); });
        stage.show();
    }

    /** Builds the per-row "Edit & Approve" + "Skip" button cell. */
    private static Callback<TableColumn<AdifImporter.RejectedRecord, Void>,
                             TableCell<AdifImporter.RejectedRecord, Void>>
        actionCellFactory(Stage parent, ObservableList<AdifImporter.RejectedRecord> data) {
        return col -> new TableCell<>() {
            private final Button edit = new Button("Edit & Approve");
            private final Button skip = new Button("Skip");
            private final HBox box   = new HBox(6, edit, skip);
            {
                box.setAlignment(Pos.CENTER);
                edit.setOnAction(e -> {
                    AdifImporter.RejectedRecord rej = getTableView().getItems().get(getIndex());
                    QsoRecord seed = rej.partial != null ? rej.partial : new QsoRecord();
                    QsoRecord edited = openEditor(parent, seed, rej.reason);
                    if (edited == null) return;  // cancelled
                    try {
                        QsoDao.getInstance().insert(edited);
                        data.remove(rej);
                    } catch (Exception ex) {
                        Alert a = new Alert(Alert.AlertType.ERROR,
                            "Insert failed: " + ex.getMessage(), ButtonType.OK);
                        a.setHeaderText("Could not insert record");
                        a.showAndWait();
                    }
                });
                skip.setOnAction(e -> {
                    AdifImporter.RejectedRecord rej = getTableView().getItems().get(getIndex());
                    data.remove(rej);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    /** Open a small modal editor pre-populated from {@code seed}. Returns
     *  the edited record on Save, or null on Cancel. Callsign is required;
     *  Save stays disabled until it has a value. */
    private static QsoRecord openEditor(Stage parent, QsoRecord seed, String rejectionReason) {
        Dialog<QsoRecord> dlg = new Dialog<>();
        dlg.setTitle("Edit & Approve Record");
        dlg.setHeaderText("Rejection reason: " + rejectionReason);
        dlg.initOwner(parent);

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dlg.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField tfCall  = new TextField(safe(seed.getCallsign()));
        TextField tfDate  = new TextField(seed.getDateTimeUtc() != null
            ? seed.getDateTimeUtc().format(DT_PARSE) : "");
        TextField tfBand  = new TextField(safe(seed.getBand()));
        TextField tfMode  = new TextField(safe(seed.getMode()));
        TextField tfFreq  = new TextField(safe(seed.getFrequency()));
        TextField tfRstS  = new TextField(safe(seed.getRstSent()));
        TextField tfRstR  = new TextField(safe(seed.getRstReceived()));
        TextField tfCtry  = new TextField(safe(seed.getCountry()));
        TextField tfName  = new TextField(safe(seed.getOperatorName()));
        TextField tfState = new TextField(safe(seed.getState()));
        TextField tfCnty  = new TextField(safe(seed.getCounty()));
        TextField tfNotes = new TextField(safe(seed.getNotes()));

        tfDate.setPromptText("yyyy-MM-dd HH:mm (UTC)");

        GridPane grid = new GridPane();
        grid.setHgap(8); grid.setVgap(6);
        grid.setPadding(new Insets(10));
        int row = 0;
        grid.add(new Label("Callsign *"), 0, row); grid.add(tfCall,  1, row++);
        grid.add(new Label("Date/Time"),  0, row); grid.add(tfDate,  1, row++);
        grid.add(new Label("Band"),       0, row); grid.add(tfBand,  1, row++);
        grid.add(new Label("Mode"),       0, row); grid.add(tfMode,  1, row++);
        grid.add(new Label("Frequency"),  0, row); grid.add(tfFreq,  1, row++);
        grid.add(new Label("RST Sent"),   0, row); grid.add(tfRstS,  1, row++);
        grid.add(new Label("RST Rcvd"),   0, row); grid.add(tfRstR,  1, row++);
        grid.add(new Label("Country"),    0, row); grid.add(tfCtry,  1, row++);
        grid.add(new Label("Op Name"),    0, row); grid.add(tfName,  1, row++);
        grid.add(new Label("State"),      0, row); grid.add(tfState, 1, row++);
        grid.add(new Label("County"),     0, row); grid.add(tfCnty,  1, row++);
        grid.add(new Label("Notes"),      0, row); grid.add(tfNotes, 1, row++);

        dlg.getDialogPane().setContent(grid);

        Node saveBtn = dlg.getDialogPane().lookupButton(saveType);
        saveBtn.setDisable(tfCall.getText().trim().isEmpty());
        tfCall.textProperty().addListener((o, oldv, newv) ->
            saveBtn.setDisable(newv == null || newv.trim().isEmpty()));

        dlg.setResultConverter(bt -> {
            if (bt != saveType) return null;
            QsoRecord q = new QsoRecord();
            q.setCallsign(tfCall.getText());
            q.setBand(tfBand.getText());
            q.setMode(tfMode.getText());
            q.setFrequency(tfFreq.getText());
            q.setRstSent(tfRstS.getText());
            q.setRstReceived(tfRstR.getText());
            q.setCountry(tfCtry.getText());
            q.setOperatorName(tfName.getText());
            q.setState(tfState.getText());
            q.setCounty(tfCnty.getText());
            q.setNotes(tfNotes.getText());
            String d = tfDate.getText() == null ? "" : tfDate.getText().trim();
            if (!d.isEmpty()) {
                try { q.setDateTimeUtc(LocalDateTime.parse(d, DT_PARSE)); }
                catch (DateTimeParseException ignored) {}
            }
            return q;
        });
        return dlg.showAndWait().orElse(null);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
