package com.jlog.controller;

import com.jlog.db.TranslationDao;
import com.jlog.model.Translation;
import com.jlog.util.DxccLanguageMap;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * TranslationWindow — shared base for the two translator viewers
 * (user-selectable and DXCC-driven). Wraps a scrollable, editable
 * TableView; auto-hides empty target columns; persists edits via
 * {@link TranslationDao}; can be pinned always-on-top.
 *
 * <p>Subclasses control which language columns are visible by
 * supplying a {@link Set} of ISO 639-1 codes from
 * {@link DxccLanguageMap}.
 */
public class TranslationWindow {

    private static final Logger log = LoggerFactory.getLogger(TranslationWindow.class);

    /** Static row backing the JavaFX table — bridges to {@link Translation}
     *  but uses JavaFX-friendly properties so cell-edit commits propagate
     *  without manual listeners. */
    public static class Row {
        public final Translation src;
        public final SimpleStringProperty category;
        public final SimpleStringProperty english;
        public final SimpleStringProperty spanish;
        public final SimpleStringProperty german;
        public final SimpleStringProperty portuguese;
        public final SimpleStringProperty phonetic;
        public Row(Translation t) {
            this.src        = t;
            this.category   = new SimpleStringProperty(nullSafe(t.getCategory()));
            this.english    = new SimpleStringProperty(nullSafe(t.getEnglish()));
            this.spanish    = new SimpleStringProperty(nullSafe(t.getSpanish()));
            this.german     = new SimpleStringProperty(nullSafe(t.getGerman()));
            this.portuguese = new SimpleStringProperty(nullSafe(t.getPortuguese()));
            this.phonetic   = new SimpleStringProperty(nullSafe(t.getPhonetic()));
        }
        private static String nullSafe(String s) { return s == null ? "" : s; }
        public SimpleStringProperty propertyFor(String code) {
            switch (code) {
                case "en":       return english;
                case "es":       return spanish;
                case "de":       return german;
                case "pt":       return portuguese;
                case "phonetic": return phonetic;
                case "category": return category;
                default: return null;
            }
        }
    }

    protected final Stage         stage;
    protected final TableView<Row> table;
    protected final ObservableList<Row> rows = FXCollections.observableArrayList();
    protected final ToggleButton  pinBtn;
    protected final Label         statusLbl;
    protected final HBox          languageBar;
    /** Currently-displayed languages — ordered. */
    protected Set<String>         visibleLangs = new LinkedHashSet<>(DxccLanguageMap.ALL);

    public TranslationWindow(String title) {
        this.stage     = new Stage();
        this.stage.setTitle(title);
        this.table     = new TableView<>(rows);
        this.pinBtn    = new ToggleButton("📌 Pin");
        this.statusLbl = new Label("");
        this.languageBar = new HBox(8);

        configureTable();
        configurePin();
        loadAll();

        BorderPane root = new BorderPane();
        root.setTop(buildTopBar());
        root.setCenter(table);
        root.setBottom(buildBottomBar());
        root.setPadding(new Insets(8));

        Scene scene = new Scene(root, 940, 540);
        var cssUrl = TranslationWindow.class.getResource("/com/jlog/css/base.css");
        if (cssUrl != null) scene.getStylesheets().add(cssUrl.toExternalForm());
        stage.setScene(scene);
    }

    // ---------------------------------------------------------------
    // Public surface
    // ---------------------------------------------------------------

    public void show() { stage.show(); stage.toFront(); }
    public Stage getStage() { return stage; }

    /**
     * Replace the set of visible language columns. Columns are
     * additionally auto-hidden when every cell in them is empty so a
     * "Spanish" column doesn't show as a wall of blanks before
     * translations are filled in.
     */
    public void setVisibleLanguages(Set<String> langs) {
        Set<String> ordered = new LinkedHashSet<>();
        ordered.add(DxccLanguageMap.EN); // always visible
        for (String code : langs) ordered.add(code);
        this.visibleLangs = ordered;
        applyColumnVisibility();
    }

    // ---------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------

    private VBox buildTopBar() {
        Label title = new Label(stage.getTitle());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        HBox row1 = new HBox(12, title);
        HBox.setHgrow(row1, Priority.ALWAYS);

        languageBar.setPadding(new Insets(4, 0, 0, 0));
        Label langPrompt = new Label("Languages:");
        languageBar.getChildren().add(langPrompt);

        VBox box = new VBox(4, row1, languageBar);
        box.setPadding(new Insets(0, 0, 6, 0));
        return box;
    }

    private HBox buildBottomBar() {
        Button addBtn = new Button("+ Add row");
        addBtn.setOnAction(e -> addBlankRow());

        Button deleteBtn = new Button("Delete row");
        deleteBtn.setOnAction(e -> deleteSelected());

        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> loadAll());

        HBox.setHgrow(statusLbl, Priority.ALWAYS);
        statusLbl.setMaxWidth(Double.MAX_VALUE);

        HBox bar = new HBox(8, addBtn, deleteBtn, refreshBtn, statusLbl, pinBtn);
        bar.setPadding(new Insets(8, 0, 0, 0));
        return bar;
    }

    private void configureTable() {
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Loading translations…"));

        // English — always shown, always editable
        TableColumn<Row, String> enCol = stringCol("English", r -> r.english, "en");
        enCol.setUserData("en");
        enCol.setMinWidth(160);
        table.getColumns().add(enCol);

        // Spanish / German / Portuguese — visibility driven by setVisibleLanguages()
        for (String code : new String[]{ DxccLanguageMap.ES, DxccLanguageMap.DE, DxccLanguageMap.PT }) {
            String label = switch (code) {
                case DxccLanguageMap.ES -> "Español";
                case DxccLanguageMap.DE -> "Deutsch";
                case DxccLanguageMap.PT -> "Português";
                default                 -> code;
            };
            TableColumn<Row, String> col = stringCol(label, r -> r.propertyFor(code), code);
            col.setUserData(code);
            col.setMinWidth(140);
            table.getColumns().add(col);
        }

        // Phonetic — always visible (helps the operator pronounce)
        TableColumn<Row, String> phCol = stringCol("Phonetic (es)", r -> r.phonetic, "phonetic");
        phCol.setUserData("phonetic");
        phCol.setMinWidth(120);
        table.getColumns().add(phCol);

        TableColumn<Row, String> catCol = stringCol("Category", r -> r.category, "category");
        catCol.setUserData("category");
        catCol.setMinWidth(80);
        catCol.setPrefWidth(90);
        table.getColumns().add(catCol);
    }

    private TableColumn<Row, String> stringCol(String label,
                                                java.util.function.Function<Row, SimpleStringProperty> prop,
                                                String column) {
        TableColumn<Row, String> col = new TableColumn<>(label);
        col.setCellValueFactory(cd -> prop.apply(cd.getValue()));
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(ev -> {
            Row r = ev.getRowValue();
            SimpleStringProperty p = prop.apply(r);
            p.set(ev.getNewValue() == null ? "" : ev.getNewValue());
            boolean ok = TranslationDao.getInstance().updateColumn(r.src.getId(), column, p.get());
            setStatus(ok ? "Saved" : "Save failed", !ok);
        });
        return col;
    }

    private void configurePin() {
        pinBtn.setOnAction(e -> {
            stage.setAlwaysOnTop(pinBtn.isSelected());
            pinBtn.setText(pinBtn.isSelected() ? "📌 Pinned" : "📌 Pin");
        });
    }

    // ---------------------------------------------------------------
    // Data flow
    // ---------------------------------------------------------------

    protected void loadAll() {
        rows.clear();
        List<Translation> all = TranslationDao.getInstance().getAll();
        for (Translation t : all) rows.add(new Row(t));
        applyColumnVisibility();
        setStatus("Loaded " + rows.size() + " phrase" + (rows.size() == 1 ? "" : "s"), false);
    }

    /**
     * Hide columns whose ISO code isn't in {@link #visibleLangs} OR whose
     * cells are all blank for the current data set. Always keeps English,
     * Phonetic, and Category visible regardless of content.
     */
    protected void applyColumnVisibility() {
        for (TableColumn<Row, ?> col : table.getColumns()) {
            String code = (String) col.getUserData();
            if (code == null) continue;
            boolean alwaysVisible =
                "en".equals(code) || "phonetic".equals(code) || "category".equals(code);
            boolean wantedByLanguage = alwaysVisible || visibleLangs.contains(code);
            boolean anyValue = alwaysVisible || rows.stream()
                .anyMatch(r -> {
                    SimpleStringProperty p = r.propertyFor(code);
                    return p != null && p.get() != null && !p.get().isBlank();
                });
            col.setVisible(wantedByLanguage && anyValue);
        }
    }

    protected void addBlankRow() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.initModality(Modality.WINDOW_MODAL);
        dlg.initOwner(stage);
        dlg.setTitle("Add phrase");
        dlg.setHeaderText("English phrase (required)");
        dlg.setContentText("English:");
        var result = dlg.showAndWait();
        if (result.isEmpty() || result.get().trim().isEmpty()) return;
        Translation t = new Translation();
        t.setEnglish(result.get().trim());
        long id = TranslationDao.getInstance().insert(t);
        if (id > 0) {
            Row r = new Row(t);
            rows.add(r);
            table.scrollTo(r);
            table.getSelectionModel().select(r);
            applyColumnVisibility();
            setStatus("Added phrase #" + id, false);
        } else {
            setStatus("Add failed", true);
        }
    }

    protected void deleteSelected() {
        Row selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("No row selected", true); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete: \"" + selected.english.get() + "\"?",
            ButtonType.YES, ButtonType.CANCEL);
        confirm.initOwner(stage);
        confirm.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                if (TranslationDao.getInstance().delete(selected.src.getId())) {
                    rows.remove(selected);
                    setStatus("Deleted phrase #" + selected.src.getId(), false);
                } else {
                    setStatus("Delete failed", true);
                }
            }
        });
    }

    protected void setStatus(String text, boolean isError) {
        statusLbl.setText(text);
        statusLbl.setStyle(isError ? "-fx-text-fill: #f38ba8;" : "-fx-text-fill: #a6e3a1;");
        if (!isError) {
            // Auto-fade success messages after 4s so the bar doesn't stay green forever
            new Thread(() -> {
                try { Thread.sleep(4000); } catch (InterruptedException ignored) {}
                Platform.runLater(() -> {
                    if (statusLbl.getText().equals(text)) statusLbl.setText("");
                });
            }, "translation-status-fade").start();
        }
    }
}
