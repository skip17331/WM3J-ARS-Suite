package com.ars.fx.surface;

import com.ars.fx.Theme;
import com.ars.fx.Themes;
import com.ars.fx.data.JLogDb;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The whole normal log in one sortable, searchable table. Click a column heading to sort (click again to reverse);
 * the search box matches call, name, QTH, grid, state, county and notes; the Band and Mode lists narrow it further.
 * Double-click a row (or press Edit) to load it into the J-Log form; Delete asks first.
 */
final class LogBrowser {
    private LogBrowser() {}

    private static Stage stage;
    private static Runnable reload;

    /** Open the browser (or bring it forward and refresh it). {@code onEdit} loads a QSO into the J-Log form. */
    static void show(Node owner, Consumer<Long> onEdit, Runnable afterChange) {
        if (stage != null) { stage.show(); stage.toFront(); reload.run(); return; }

        FilteredList<JLogDb.QsoFull> shown = new FilteredList<>(FXCollections.observableArrayList(), q -> true);
        TextField search = new TextField(); search.setPromptText("Search call, name, QTH, grid, state, county, notes…");
        search.getStyleClass().add("jhub-mini-field"); search.setMinWidth(300);
        ComboBox<String> band = new ComboBox<>(); band.setMinWidth(110);
        ComboBox<String> mode = new ComboBox<>(); mode.setMinWidth(110);
        Label count = lbl("", "jl-emeta");
        Label edit = lbl("Edit", "jl-clearbtn"); edit.setStyle("-fx-cursor:hand;");
        Label del = lbl("Delete", "jl-clearbtn"); del.setStyle("-fx-cursor:hand;");
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");

        TableView<JLogDb.QsoFull> table = new TableView<>();
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(lbl("No QSOs match", "jl-emeta"));
        table.getColumns().addAll(List.of(
                col("DATE (UTC)", 130, q -> JLogDb.dateOf(q.datetimeUtc()) + " " + JLogDb.hhmm(q.datetimeUtc())),
                col("CALL", 110, JLogDb.QsoFull::call),
                col("FREQ", 84, JLogDb.QsoFull::freq),
                col("BAND", 60, JLogDb.QsoFull::band),
                col("MODE", 66, JLogDb.QsoFull::mode),
                col("RST S", 56, JLogDb.QsoFull::rstSent),
                col("RST R", 56, JLogDb.QsoFull::rstRcvd),
                col("NAME", 150, JLogDb.QsoFull::name),
                col("QTH", 170, JLogDb.QsoFull::qth),
                col("GRID", 76, JLogDb.QsoFull::grid),
                col("ST", 50, JLogDb.QsoFull::state),
                col("COUNTY", 120, JLogDb.QsoFull::county),
                col("PWR", 56, q -> q.powerW() > 0 ? String.valueOf(q.powerW()) : ""),
                col("NOTES", 260, JLogDb.QsoFull::notes)));
        // the date column holds "yyyy-MM-dd HH:mm", which sorts correctly as text — newest first to start
        @SuppressWarnings("unchecked")
        TableColumn<JLogDb.QsoFull, String> dateCol = (TableColumn<JLogDb.QsoFull, String>) table.getColumns().get(0);
        dateCol.setComparator(Comparator.naturalOrder());
        SortedList<JLogDb.QsoFull> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        Runnable refilter = () -> {
            String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String b = band.getValue(), m = mode.getValue();
            shown.setPredicate(q -> (b == null || b.equals("All bands") || b.equalsIgnoreCase(nz(q.band())))
                    && (m == null || m.equals("All modes") || m.equalsIgnoreCase(nz(q.mode())))
                    && (needle.isEmpty() || haystack(q).contains(needle)));
            count.setText(shown.size() + " of " + ((FilteredList<?>) shown).getSource().size() + " QSOs");
        };
        reload = () -> {
            String keepB = band.getValue(), keepM = mode.getValue();
            List<JLogDb.QsoFull> rows = JLogDb.all();
            ((javafx.collections.ObservableList<JLogDb.QsoFull>) shown.getSource()).setAll(rows);
            TreeSet<String> bands = new TreeSet<>(Comparator.comparingDouble(LogBrowser::bandOrder).thenComparing(Comparator.naturalOrder()));
            TreeSet<String> modes = new TreeSet<>();
            for (JLogDb.QsoFull q : rows) { if (!nz(q.band()).isBlank()) bands.add(q.band()); if (!nz(q.mode()).isBlank()) modes.add(q.mode()); }
            band.getItems().setAll("All bands"); band.getItems().addAll(bands);
            mode.getItems().setAll("All modes"); mode.getItems().addAll(modes);
            band.setValue(keepB != null && band.getItems().contains(keepB) ? keepB : "All bands");
            mode.setValue(keepM != null && mode.getItems().contains(keepM) ? keepM : "All modes");
            refilter.run();
        };
        search.textProperty().addListener((o, a, b) -> refilter.run());
        band.valueProperty().addListener((o, a, b) -> refilter.run());
        mode.valueProperty().addListener((o, a, b) -> refilter.run());
        refresh.setOnMouseClicked(e -> reload.run());

        Runnable doEdit = () -> {
            JLogDb.QsoFull q = table.getSelectionModel().getSelectedItem();
            if (q != null && onEdit != null) {
                onEdit.accept(q.id());
                if (stage != null && stage.getOwner() != null) { stage.getOwner().requestFocus(); }   // back to the J-Log form
            }
        };
        edit.setOnMouseClicked(e -> doEdit.run());
        table.setOnMouseClicked(e -> { if (e.getClickCount() == 2) doEdit.run(); });
        del.setOnMouseClicked(e -> {
            JLogDb.QsoFull q = table.getSelectionModel().getSelectedItem();
            if (q == null) return;
            Confirm.show(table, "Delete this QSO?", q.call() + " · " + JLogDb.dateOf(q.datetimeUtc()) + " " + JLogDb.hhmm(q.datetimeUtc())
                            + " · " + nz(q.band()) + " " + nz(q.mode()) + "\nThis cannot be undone.",
                    new Confirm.Choice("Cancel", false, null),
                    new Confirm.Choice("Delete", true, () -> {
                        if (JLogDb.delete(q.id())) { reload.run(); if (afterChange != null) afterChange.run(); }
                    }));
        });

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, search, band, mode, count, sp, refresh, edit, del);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10, 14, 10, 14));
        Label hint = lbl("Click a heading to sort · double-click a row to edit it in J-Log", "jl-emeta");
        hint.setPadding(new Insets(0, 14, 8, 14));
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox root = new VBox(bar, hint, table);

        Scene scene = new Scene(root, 1180, 620);
        Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        Themes.applyTo(root, "log");
        Stage st = new Stage(); st.setTitle("J-Log — full log"); st.setScene(scene); com.ars.fx.AppIcons.apply(st, null);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        st.setOnHidden(e -> { stage = null; reload = null; });
        stage = st;
        reload.run();
        table.getSortOrder().add(dateCol); dateCol.setSortType(TableColumn.SortType.DESCENDING);
        st.show();
    }

    /** Refresh the browser if it is open (call after the log changes). */
    static void refreshIfOpen() { if (stage != null && reload != null) reload.run(); }

    private static TableColumn<JLogDb.QsoFull, String> col(String title, double w, Function<JLogDb.QsoFull, String> get) {
        TableColumn<JLogDb.QsoFull, String> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(nz(get.apply(cd.getValue()))));
        return c;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    private static String haystack(JLogDb.QsoFull q) {
        return (nz(q.call()) + " " + nz(q.name()) + " " + nz(q.qth()) + " " + nz(q.grid()) + " " + nz(q.state()) + " "
                + nz(q.county()) + " " + nz(q.notes())).toLowerCase();
    }

    /** Orders band labels by wavelength-ish size: 160m, 80m ... 70cm. */
    private static double bandOrder(String b) {
        try {
            String s = b.trim().toLowerCase();
            if (s.endsWith("cm")) return Double.parseDouble(s.substring(0, s.length() - 2)) / 100.0;
            if (s.endsWith("m")) return Double.parseDouble(s.substring(0, s.length() - 1));
        } catch (Exception ignored) {}
        return 1e6;
    }
}
