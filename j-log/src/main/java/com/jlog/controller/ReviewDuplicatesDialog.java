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
import javafx.stage.Stage;
import javafx.util.Callback;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Non-modal "Review Duplicates" dialog. Opens after an ADIF import that
 * ran in {@link AdifImporter.DupeMode#REVIEW}. For each incoming record
 * that collided with an existing log row on the (callsign, band, mode)
 * dupe key, the operator picks:
 *
 *   Keep      — insert the incoming row as a new QSO (APPEND for this row)
 *   Overwrite — delete the existing row and insert the incoming row
 *   Skip      — drop the incoming row without changing the DB
 *
 * Bulk "Apply to All" buttons at the bottom run the chosen action over
 * every remaining row. {@code onClose} fires when the operator dismisses
 * the dialog so the main controller can refresh its QSO table.
 */
public final class ReviewDuplicatesDialog {

    private static final DateTimeFormatter DT_DISPLAY =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReviewDuplicatesDialog() {}

    public static void show(List<AdifImporter.DuplicateRecord> duplicates, Runnable onClose) {
        if (duplicates == null || duplicates.isEmpty()) {
            if (onClose != null) onClose.run();
            return;
        }
        Stage stage = new Stage();
        stage.setTitle("Review Duplicates (" + duplicates.size() + ")");

        ObservableList<AdifImporter.DuplicateRecord> data =
            FXCollections.observableArrayList(duplicates);

        TableView<AdifImporter.DuplicateRecord> table = new TableView<>(data);
        table.setPlaceholder(new Label("All duplicates resolved — close this window."));

        TableColumn<AdifImporter.DuplicateRecord, String> colNum = simpleCol(
            "#", 50, d -> String.valueOf(d.recordNumber));
        TableColumn<AdifImporter.DuplicateRecord, String> colCall = simpleCol(
            "Callsign", 100, d -> safe(d.incoming.getCallsign()));
        TableColumn<AdifImporter.DuplicateRecord, String> colDate = simpleCol(
            "Incoming Date/Time", 140, d ->
                d.incoming.getDateTimeUtc() != null
                    ? d.incoming.getDateTimeUtc().format(DT_DISPLAY) : "");
        TableColumn<AdifImporter.DuplicateRecord, String> colBand = simpleCol(
            "Band", 60, d -> safe(d.incoming.getBand()));
        TableColumn<AdifImporter.DuplicateRecord, String> colMode = simpleCol(
            "Mode", 70, d -> safe(d.incoming.getMode()));
        TableColumn<AdifImporter.DuplicateRecord, String> colExisting = simpleCol(
            "Existing", 220, d -> {
                if (d.existingMatches.isEmpty()) return "—";
                QsoRecord first = d.existingMatches.get(0);
                String dt = first.getDateTimeUtc() != null
                    ? first.getDateTimeUtc().format(DT_DISPLAY) : "(no date)";
                String extra = d.existingMatches.size() > 1
                    ? " (+ " + (d.existingMatches.size() - 1) + " more)" : "";
                return dt + extra;
            });
        TableColumn<AdifImporter.DuplicateRecord, Void> colAction = new TableColumn<>("Action");
        colAction.setPrefWidth(260);
        colAction.setCellFactory(actionCellFactory(data));

        table.getColumns().addAll(colNum, colCall, colDate, colBand, colMode,
                                  colExisting, colAction);

        Label header = new Label(
            "Per-row choices: Keep inserts the incoming record as a new QSO;\n"
          + "Overwrite replaces the existing row(s); Skip drops the incoming row.\n"
          + "The bulk buttons at the bottom apply your choice to every remaining row.");
        header.setWrapText(true);
        header.setPadding(new Insets(6, 8, 6, 8));

        Button btnKeepAll      = new Button("Keep all");
        Button btnOverwriteAll = new Button("Overwrite all");
        Button btnSkipAll      = new Button("Skip all");
        Button btnClose        = new Button("Close");
        btnKeepAll.setOnAction(e -> bulk(stage, data, BulkAction.KEEP));
        btnOverwriteAll.setOnAction(e -> bulk(stage, data, BulkAction.OVERWRITE));
        btnSkipAll.setOnAction(e -> bulk(stage, data, BulkAction.SKIP));
        btnClose.setOnAction(e -> stage.close());

        HBox bulkBar = new HBox(8, btnKeepAll, btnOverwriteAll, btnSkipAll);
        bulkBar.setAlignment(Pos.CENTER_LEFT);
        HBox bottom = new HBox(8, bulkBar, spacer(), btnClose);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(8));

        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(table);
        root.setBottom(bottom);

        Scene scene = new Scene(root, 1040, 480);
        stage.setScene(scene);
        stage.setOnHiding(e -> { if (onClose != null) onClose.run(); });
        stage.show();
    }

    // ---- per-row buttons -----------------------------------------------

    private enum BulkAction { KEEP, OVERWRITE, SKIP }

    private static Callback<TableColumn<AdifImporter.DuplicateRecord, Void>,
                             TableCell<AdifImporter.DuplicateRecord, Void>>
        actionCellFactory(ObservableList<AdifImporter.DuplicateRecord> data) {
        return col -> new TableCell<>() {
            private final Button keep      = new Button("Keep");
            private final Button overwrite = new Button("Overwrite");
            private final Button skip      = new Button("Skip");
            private final HBox box = new HBox(6, keep, overwrite, skip);
            {
                box.setAlignment(Pos.CENTER);
                keep.setOnAction(e -> applyRow(getRow(), data, BulkAction.KEEP, this));
                overwrite.setOnAction(e -> applyRow(getRow(), data, BulkAction.OVERWRITE, this));
                skip.setOnAction(e -> applyRow(getRow(), data, BulkAction.SKIP, this));
            }
            private AdifImporter.DuplicateRecord getRow() {
                int idx = getIndex();
                if (idx < 0 || idx >= getTableView().getItems().size()) return null;
                return getTableView().getItems().get(idx);
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private static void applyRow(AdifImporter.DuplicateRecord rec,
                                 ObservableList<AdifImporter.DuplicateRecord> data,
                                 BulkAction action, Node ownerForAlerts) {
        if (rec == null) return;
        try {
            switch (action) {
                case KEEP      -> QsoDao.getInstance().insert(rec.incoming);
                case OVERWRITE -> {
                    String call = rec.incoming.getCallsign();
                    String band = rec.incoming.getBand() == null ? "" : rec.incoming.getBand();
                    String mode = rec.incoming.getMode() == null ? "" : rec.incoming.getMode();
                    java.time.LocalDate date = rec.incoming.getDateTimeUtc() != null
                        ? rec.incoming.getDateTimeUtc().toLocalDate() : null;
                    QsoDao.getInstance().deleteByKey(call, band, mode, date);
                    QsoDao.getInstance().insert(rec.incoming);
                }
                case SKIP -> { /* no-op */ }
            }
            data.remove(rec);
        } catch (Exception ex) {
            Alert a = new Alert(Alert.AlertType.ERROR,
                action + " failed: " + ex.getMessage(), ButtonType.OK);
            a.setHeaderText("Could not apply choice");
            a.showAndWait();
        }
    }

    private static void bulk(Stage parent,
                             ObservableList<AdifImporter.DuplicateRecord> data,
                             BulkAction action) {
        if (data.isEmpty()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            action + " all " + data.size() + " remaining duplicate(s)?",
            ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Apply to all");
        confirm.initOwner(parent);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        // Snapshot — applyRow mutates `data`.
        for (AdifImporter.DuplicateRecord rec : List.copyOf(data)) {
            applyRow(rec, data, action, parent.getScene().getRoot());
        }
    }

    // ---- helpers --------------------------------------------------------

    private static TableColumn<AdifImporter.DuplicateRecord, String>
        simpleCol(String title, double width,
                  java.util.function.Function<AdifImporter.DuplicateRecord, String> getter) {
        TableColumn<AdifImporter.DuplicateRecord, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new SimpleStringProperty(getter.apply(cd.getValue())));
        c.setStyle("-fx-alignment: CENTER;");
        return c;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
