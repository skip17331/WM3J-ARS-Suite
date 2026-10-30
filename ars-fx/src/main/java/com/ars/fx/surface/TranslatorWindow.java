package com.ars.fx.surface;

import com.ars.fx.Theme;
import com.ars.fx.Themes;
import com.ars.fx.data.ContestConfigBridge;
import com.jlog.db.TranslationDao;
import com.jlog.model.Translation;
import com.jlog.scoring.DxccResolver;
import com.jlog.util.DxccLanguageMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The QSO phrase translator, ported from the original J-Log: an editable table of common QSO phrases (English, Spanish,
 * German, Portuguese, a phonetic guide and a category). It works offline from {@code ~/.j-log/translations.db}.
 *
 * <p>Two ways to choose which language columns show, as in the original: <b>By country</b> follows the callsign being
 * logged (its DXCC country picks the languages — see {@link #follow(String)}), or you can type a country yourself;
 * <b>Choose languages</b> shows exactly the ones you tick. English, the phonetic guide and the category always show.
 * Edits are saved as you make them.
 */
final class TranslatorWindow {
    private TranslatorWindow() {}

    /** One table row, backed by a database row. */
    private static final class Row {
        final long id;
        final SimpleStringProperty category, english, spanish, german, portuguese, phonetic, phoneticDe, phoneticPt;
        Row(Translation t) {
            id = t.getId();
            category = p(t.getCategory()); english = p(t.getEnglish()); spanish = p(t.getSpanish());
            german = p(t.getGerman()); portuguese = p(t.getPortuguese()); phonetic = p(t.getPhonetic());
            phoneticDe = p(t.getPhoneticDe()); phoneticPt = p(t.getPhoneticPt());
        }
        private static SimpleStringProperty p(String s) { return new SimpleStringProperty(s == null ? "" : s); }
    }

    private static Stage stage;
    private static boolean byCountry = true;
    private static final Set<String> chosen = new LinkedHashSet<>(Set.of(DxccLanguageMap.EN));   // "Choose languages" mode
    private static Set<String> followed = Set.of(DxccLanguageMap.EN);                                // "By country" mode
    private static String followedCountry = "";
    private static Runnable refreshColumns, refreshCountryLabel;

    /** Open the translator (or bring it forward). {@code call} seeds the country when "By country" is on. */
    static void show(Node owner, String call) {
        if (stage != null) { stage.show(); stage.toFront(); if (call != null && !call.isBlank()) follow(call); return; }
        ContestConfigBridge.bootstrap();                  // opens ~/.j-log/translations.db and seeds the starter phrases
        TranslationDao dao = TranslationDao.getInstance();

        ObservableList<Row> all = FXCollections.observableArrayList();
        Runnable load = () -> { all.clear(); for (Translation t : dao.getAll()) all.add(new Row(t)); };
        load.run();
        FilteredList<Row> shown = new FilteredList<>(all, r -> true);

        // ── toolbar ─────────────────────────────────────────────────────────
        Label modeCountry = lbl("By country", "jl-clearbtn"), modeChoose = lbl("Choose languages", "jl-clearbtn");
        TextField country = new TextField(); country.setPromptText("country, e.g. Spain"); country.getStyleClass().add("jhub-mini-field");
        country.setPrefWidth(190);
        Label langs = lbl("", "jl-emeta");
        CheckBox es = new CheckBox("Spanish"), de = new CheckBox("German"), pt = new CheckBox("Portuguese");
        for (CheckBox c : new CheckBox[]{es, de, pt}) c.setStyle("-fx-text-fill:-ars-t1;-fx-cursor:hand;");
        HBox countryBox = new HBox(8, lbl("Country", "jl-flabel"), country, langs); countryBox.setAlignment(Pos.CENTER_LEFT);
        HBox chooseBox = new HBox(12, lbl("Show", "jl-flabel"), lbl("English", "jl-emeta"), es, de, pt); chooseBox.setAlignment(Pos.CENTER_LEFT);
        HBox modeRow = new HBox(8, modeCountry, modeChoose, new Region(), countryBox, chooseBox);
        HBox.setHgrow(modeRow.getChildren().get(2), Priority.ALWAYS); modeRow.setAlignment(Pos.CENTER_LEFT);

        TextField search = new TextField(); search.setPromptText("search phrases…"); search.getStyleClass().add("jhub-mini-field"); search.setPrefWidth(220);
        ComboBox<String> cat = new ComboBox<>(); cat.setMinWidth(160);
        Runnable fillCats = () -> {
            String cur = cat.getValue();
            Set<String> cs = new LinkedHashSet<>(); for (Row r : all) if (!r.category.get().isBlank()) cs.add(r.category.get());
            cat.getItems().setAll("All categories"); cat.getItems().addAll(cs);
            cat.setValue(cur != null && cat.getItems().contains(cur) ? cur : "All categories");
        };
        fillCats.run();
        Runnable filter = () -> {
            String q = search.getText() == null ? "" : search.getText().trim().toLowerCase(), c = cat.getValue();
            shown.setPredicate(r -> (c == null || c.equals("All categories") || c.equals(r.category.get()))
                    && (q.isEmpty() || r.english.get().toLowerCase().contains(q) || r.spanish.get().toLowerCase().contains(q)
                        || r.german.get().toLowerCase().contains(q) || r.portuguese.get().toLowerCase().contains(q)));
        };
        search.textProperty().addListener((o, a, b) -> filter.run());
        cat.valueProperty().addListener((o, a, b) -> filter.run());

        Label add = lbl("+ Add phrase", "jl-clearbtn"), del = lbl("Delete", "jl-clearbtn");
        CheckBox pin = new CheckBox("Keep on top"); pin.setStyle("-fx-text-fill:-ars-t1;-fx-cursor:hand;");
        HBox actions = new HBox(10, search, cat, new Region(), add, del, pin); actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(actions.getChildren().get(2), Priority.ALWAYS);

        // ── table ───────────────────────────────────────────────────────────
        TableView<Row> table = new TableView<>(shown); table.setEditable(true);
        table.setPlaceholder(lbl("No phrases match.", "jl-emeta"));
        VBox.setVgrow(table, Priority.ALWAYS);
        TableColumn<Row, String> en = col("English", r -> r.english, "en", dao, 300);
        TableColumn<Row, String> esC = col("Spanish", r -> r.spanish, "es", dao, 260);
        TableColumn<Row, String> deC = col("German", r -> r.german, "de", dao, 260);
        TableColumn<Row, String> ptC = col("Portuguese", r -> r.portuguese, "pt", dao, 260);
        // each language's own pronunciation guide sits beside it and shows with it
        TableColumn<Row, String> phEs = col("Say it (Spanish)", r -> r.phonetic, "phonetic_es", dao, 190);
        TableColumn<Row, String> phDe = col("Say it (German)", r -> r.phoneticDe, "phonetic_de", dao, 190);
        TableColumn<Row, String> phPt = col("Say it (Portuguese)", r -> r.phoneticPt, "phonetic_pt", dao, 190);
        TableColumn<Row, String> ct = col("Category", r -> r.category, "category", dao, 130);
        esC.setUserData("es"); phEs.setUserData("es"); deC.setUserData("de"); phDe.setUserData("de"); ptC.setUserData("pt"); phPt.setUserData("pt");
        table.getColumns().addAll(en, esC, phEs, deC, phDe, ptC, phPt, ct);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        refreshColumns = () -> {
            Set<String> want = byCountry ? followed : chosen;
            for (TableColumn<Row, ?> c : table.getColumns()) if (c.getUserData() instanceof String code) c.setVisible(want.contains(code));
            modeCountry.setStyle("-fx-cursor:hand;" + (byCountry ? "-fx-background-color:-ars-accent;-fx-text-fill:#06121a;-fx-background-radius:6;-fx-padding:4 10;" : "-fx-padding:4 10;"));
            modeChoose.setStyle("-fx-cursor:hand;" + (!byCountry ? "-fx-background-color:-ars-accent;-fx-text-fill:#06121a;-fx-background-radius:6;-fx-padding:4 10;" : "-fx-padding:4 10;"));
            countryBox.setVisible(byCountry); countryBox.setManaged(byCountry);
            chooseBox.setVisible(!byCountry); chooseBox.setManaged(!byCountry);
        };
        refreshCountryLabel = () -> {
            if (!country.isFocused()) country.setText(followedCountry);
            langs.setText("→ " + String.join(", ", names(followed)));
        };
        modeCountry.setOnMouseClicked(e -> { byCountry = true; refreshColumns.run(); });
        modeChoose.setOnMouseClicked(e -> { byCountry = false; refreshColumns.run(); });
        for (CheckBox c : new CheckBox[]{es, de, pt}) {
            String code = c == es ? DxccLanguageMap.ES : c == de ? DxccLanguageMap.DE : DxccLanguageMap.PT;
            c.setSelected(chosen.contains(code));
            c.selectedProperty().addListener((o, a, on) -> { if (on) chosen.add(code); else chosen.remove(code); refreshColumns.run(); });
        }
        country.textProperty().addListener((o, a, b) -> { if (country.isFocused()) applyCountry(b); });   // typed by hand

        add.setOnMouseClicked(e -> {
            Translation t = new Translation(); t.setEnglish("New phrase"); t.setCategory(cat.getValue() == null || cat.getValue().equals("All categories") ? "" : cat.getValue());
            long id = dao.insert(t);
            if (id > 0) { load.run(); fillCats.run(); table.getSelectionModel().select(all.size() - 1); table.scrollTo(all.size() - 1); }
        });
        del.setOnMouseClicked(e -> {
            Row r = table.getSelectionModel().getSelectedItem();
            if (r != null && dao.delete(r.id)) { load.run(); fillCats.run(); }
        });
        pin.selectedProperty().addListener((o, a, b) -> { if (stage != null) stage.setAlwaysOnTop(b); });

        Label hint = lbl("Double-click a cell to edit; changes are saved at once. Works offline.", "jl-skim-meta");
        VBox root = new VBox(10, modeRow, actions, table, hint);
        root.setPadding(new Insets(14, 16, 14, 16)); root.setStyle("-fx-background-color:-ars-bg;");

        Scene scene = new Scene(root, 1100, 560);
        Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        Themes.applyTo(root, "log");
        Stage st = new Stage(); st.setTitle("QSO phrase translator"); st.setScene(scene); com.ars.fx.AppIcons.apply(st, null);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        st.setOnHidden(e -> { stage = null; refreshColumns = null; refreshCountryLabel = null; });
        stage = st;

        if (call != null && !call.isBlank()) follow(call); else { refreshColumns.run(); refreshCountryLabel.run(); }
        st.show();
    }

    /** The callsign being logged changed: when the translator is open and set to "By country", follow its DXCC country. */
    static void follow(String call) {
        if (stage == null || call == null) return;
        String c = call.trim().toUpperCase();
        if (c.length() < 2) return;
        String entity;
        try { entity = DxccResolver.getInstance().entityOf(c); } catch (Exception e) { entity = null; }
        if (entity == null) return;
        applyCountry(com.ars.fx.data.DxccInfo.countryName(entity));
    }

    private static void applyCountry(String entity) {
        followedCountry = entity == null ? "" : entity;
        followed = DxccLanguageMap.languagesFor(entity);
        if (refreshColumns != null) refreshColumns.run();
        if (refreshCountryLabel != null) refreshCountryLabel.run();
    }

    private static java.util.List<String> names(Set<String> codes) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String c : codes) out.add(switch (c) { case "en" -> "English"; case "es" -> "Spanish"; case "de" -> "German"; case "pt" -> "Portuguese"; default -> c; });
        return out;
    }

    private static TableColumn<Row, String> col(String title, Function<Row, SimpleStringProperty> prop, String column, TranslationDao dao, double width) {
        TableColumn<Row, String> c = new TableColumn<>(title);
        c.setCellValueFactory(cd -> prop.apply(cd.getValue()));
        c.setCellFactory(TextFieldTableCell.forTableColumn());
        c.setPrefWidth(width); c.setEditable(true);
        c.setOnEditCommit(ev -> {
            Row r = ev.getRowValue(); String v = ev.getNewValue() == null ? "" : ev.getNewValue().trim();
            if (column.equals("en") && v.isEmpty()) return;                    // an English phrase is required
            prop.apply(r).set(v);
            dao.updateColumn(r.id, column, v);
        });
        return c;
    }
}
