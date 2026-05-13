package com.jlog.controller;

import com.jlog.db.PriorityCallsignDao;
import com.jlog.model.PriorityCallsign;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.Locale;

/**
 * PriorityCallsignWindow — Tools → "Priority Callsigns…" dialog.
 * Manages the operator's priority watch list (add / edit / delete);
 * reloads {@link PrioritySpotAlertService} on every change so the
 * alert engine picks up edits immediately.
 *
 * <p>Each row exposes inline editable controls for note, mute,
 * audible, and banner toggles.
 */
public class PriorityCallsignWindow {

    public static class Row {
        public final PriorityCallsign src;
        public final SimpleStringProperty callsign;
        public final SimpleStringProperty note;
        public final SimpleBooleanProperty muted;
        public final SimpleBooleanProperty audible;
        public final SimpleBooleanProperty banner;
        public Row(PriorityCallsign p) {
            this.src      = p;
            this.callsign = new SimpleStringProperty(p.getCallsign());
            this.note     = new SimpleStringProperty(p.getNote() == null ? "" : p.getNote());
            this.muted    = new SimpleBooleanProperty(p.isMuted());
            this.audible  = new SimpleBooleanProperty(p.isAudible());
            this.banner   = new SimpleBooleanProperty(p.isBanner());
        }
    }

    private final Stage stage = new Stage();
    private final ObservableList<Row> rows = FXCollections.observableArrayList();
    private final TableView<Row> table = new TableView<>(rows);
    private final TextField newCallField = new TextField();
    private final TextField newNoteField = new TextField();

    public PriorityCallsignWindow() {
        stage.setTitle("Priority Callsigns");
        buildUI();
        load();
    }

    public void show() { stage.show(); stage.toFront(); }

    // ---------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------

    private void buildUI() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("No priority callsigns — add one below."));

        TableColumn<Row, String> callCol = new TableColumn<>("Callsign");
        callCol.setCellValueFactory(cd -> cd.getValue().callsign);
        callCol.setMinWidth(110);
        callCol.setPrefWidth(120);

        TableColumn<Row, String> noteCol = new TableColumn<>("Note");
        noteCol.setCellValueFactory(cd -> cd.getValue().note);
        noteCol.setCellFactory(TextFieldTableCell.forTableColumn());
        noteCol.setMinWidth(180);
        noteCol.setOnEditCommit(ev -> {
            Row r = ev.getRowValue();
            r.note.set(ev.getNewValue());
            r.src.setNote(ev.getNewValue());
            PriorityCallsignDao.getInstance().update(r.src);
            PrioritySpotAlertService.getInstance().reload();
        });

        TableColumn<Row, Boolean> mutedCol = checkboxCol("Muted",   r -> r.muted,   true);
        TableColumn<Row, Boolean> audCol   = checkboxCol("Audible", r -> r.audible, false);
        TableColumn<Row, Boolean> banCol   = checkboxCol("Banner",  r -> r.banner,  false);

        TableColumn<Row, Void> deleteCol = new TableColumn<>("");
        deleteCol.setMinWidth(60);
        deleteCol.setPrefWidth(64);
        deleteCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Delete");
            {
                btn.setOnAction(e -> {
                    Row r = getTableView().getItems().get(getIndex());
                    if (PriorityCallsignDao.getInstance().delete(r.src.getId())) {
                        rows.remove(r);
                        PrioritySpotAlertService.getInstance().reload();
                    }
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        table.getColumns().addAll(callCol, noteCol, mutedCol, audCol, banCol, deleteCol);

        // ── Add-row bar ─────────────────────────────────────────────
        newCallField.setPromptText("Callsign (e.g. W1AW)");
        newCallField.setPrefColumnCount(10);
        newCallField.setOnAction(e -> doAdd());

        newNoteField.setPromptText("Note (optional)");
        HBox.setHgrow(newNoteField, Priority.ALWAYS);
        newNoteField.setOnAction(e -> doAdd());

        Button addBtn = new Button("+ Add");
        addBtn.setDefaultButton(true);
        addBtn.setOnAction(e -> doAdd());

        HBox addBar = new HBox(8, new Label("Callsign:"), newCallField,
                                  new Label("Note:"),     newNoteField, addBtn);
        addBar.setPadding(new Insets(8, 0, 0, 0));

        Label hint = new Label("Inbound DX spots matching any un-muted callsign fire a banner + audible alert. "
                             + "60-second per-callsign debounce. Suffix-aware ("
                             + "W1AW/P, W1AW/M, W1AW/QRP all match W1AW).");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill: #88aacc; -fx-font-size: 11px;");

        BorderPane root = new BorderPane();
        root.setTop(hint);
        BorderPane.setMargin(hint, new Insets(0, 0, 8, 0));
        root.setCenter(table);
        root.setBottom(addBar);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 720, 460);
        var cssUrl = PriorityCallsignWindow.class.getResource("/com/jlog/css/base.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        stage.setScene(scene);
    }

    private TableColumn<Row, Boolean> checkboxCol(String label,
                                                  java.util.function.Function<Row, SimpleBooleanProperty> prop,
                                                  boolean isMutedColumn) {
        TableColumn<Row, Boolean> col = new TableColumn<>(label);
        col.setEditable(true);
        col.setCellValueFactory(cd -> prop.apply(cd.getValue()));
        col.setCellFactory(CheckBoxTableCell.forTableColumn(idx -> {
            SimpleBooleanProperty p = prop.apply(rows.get(idx));
            // Persist on any change
            p.addListener((obs, oldVal, newVal) -> {
                if (oldVal == newVal) return;
                Row r = rows.get(idx);
                if (isMutedColumn) r.src.setMuted(newVal);
                else if (label.equals("Audible")) r.src.setAudible(newVal);
                else if (label.equals("Banner"))  r.src.setBanner(newVal);
                PriorityCallsignDao.getInstance().update(r.src);
                PrioritySpotAlertService.getInstance().reload();
            });
            return p;
        }));
        col.setMinWidth(70);
        col.setPrefWidth(74);
        return col;
    }

    // ---------------------------------------------------------------
    // Data flow
    // ---------------------------------------------------------------

    private void load() {
        rows.clear();
        for (PriorityCallsign p : PriorityCallsignDao.getInstance().getAll()) {
            rows.add(new Row(p));
        }
    }

    private void doAdd() {
        String call = newCallField.getText().trim().toUpperCase(Locale.ROOT);
        if (call.isEmpty()) return;
        PriorityCallsign p = new PriorityCallsign(call, newNoteField.getText().trim());
        long id = PriorityCallsignDao.getInstance().insert(p);
        if (id > 0) {
            rows.add(new Row(p));
            PrioritySpotAlertService.getInstance().reload();
            newCallField.clear();
            newNoteField.clear();
            newCallField.requestFocus();
        }
    }
}
