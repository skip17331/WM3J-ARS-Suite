package com.ars.fx.surface;

import com.ars.fx.Theme;
import com.ars.fx.Themes;
import com.ars.fx.data.ContestLogReader;
import com.ars.fx.data.ContestLogs;
import com.ars.fx.data.JLogDb;
import com.ars.fx.data.LogExport;
import com.ars.fx.data.QslLabel;
import com.jlog.model.QsoRecord;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.ars.fx.shell.Shell.lbl;

/**
 * A whole log in one sortable, searchable table: the normal log, or (from the Log list at the left) any contest log, archived ones
 * included. Click a column heading to sort (click again to reverse); the search box matches call, name, QTH, grid, state, county, notes
 * and IOTA / POTA / SOTA references (a contest log: call, exchange, notes); the Band and Mode lists narrow it further.
 * <p>Normal log: double-click a row (or press Edit) to load it into the J-Log form; Delete asks first.
 * A contest log is view-only here unless it is the contest on the contest page now: then Edit loads the QSO into that page's form and
 * Delete erases it exactly as the contest page does (dupes re-checked, N1MM and the contest network told). In both, Export saves what is shown as ADIF or CSV and
 * Print label prints the selected QSO's QSL label.
 */
final class LogBrowser {
    private LogBrowser() {}

    private static Stage stage;
    private static Runnable reload;          // everything: the list of logs and the rows of the one chosen
    private static Runnable refreshRows;     // just the rows of the one chosen (after the log changes)

    private static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String NORMAL_PROMPT = "Search call, name, QTH, grid, state, county, notes…";
    private static final String NORMAL_HINT = "Click a heading to sort · double-click a row to edit it in J-Log · CONF: Q = QSL card, L = LoTW, E = eQSL";
    private static final String CONTEST_EDIT_HINT = "Contest log · double-click a row to edit it on the contest page · Delete erases it from the log and the score · Label prints the selected QSO's QSL label · FREQ is in kHz";
    private static final String CONTEST_HINT = "Contest log, view only (the contest on the contest page can be edited here) · click a heading to sort · Label prints the selected QSO's QSL label · FREQ is in kHz";

    /** Open the browser (or bring it forward and refresh it). {@code onEdit} loads a QSO into the J-Log form. */
    static void show(Node owner, Consumer<Long> onEdit, Runnable afterChange) {
        if (stage != null) { stage.show(); stage.toFront(); reload.run(); return; }

        ObservableList<JLogDb.QsoFull> nRows = FXCollections.observableArrayList();
        FilteredList<JLogDb.QsoFull> shown = new FilteredList<>(nRows, q -> true);
        ObservableList<QsoRecord> cRows = FXCollections.observableArrayList();
        FilteredList<QsoRecord> cShown = new FilteredList<>(cRows, q -> true);

        ComboBox<String> source = new ComboBox<>(); source.setMinWidth(210); source.setMaxWidth(320);
        List<ContestLogs.Info> logs = new ArrayList<>();           // logs.get(i) is the Log list's item i + 1; item 0 is the normal log
        TextField search = new TextField(); search.setPromptText(NORMAL_PROMPT);
        search.getStyleClass().add("jhub-mini-field"); search.setMinWidth(260);
        ComboBox<String> band = new ComboBox<>(); band.setMinWidth(110);
        ComboBox<String> mode = new ComboBox<>(); mode.setMinWidth(110);
        Label count = lbl("", "jl-emeta");
        Label edit = lbl("Edit", "jl-clearbtn"); edit.setStyle("-fx-cursor:hand;");
        Label del = lbl("Delete", "jl-clearbtn"); del.setStyle("-fx-cursor:hand;");
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        Label export = lbl("Export…", "jl-clearbtn"); export.setStyle("-fx-cursor:hand;");
        Label printLbl = lbl("Print label", "jl-clearbtn"); printLbl.setStyle("-fx-cursor:hand;");
        Label hint = lbl(NORMAL_HINT, "jl-emeta");
        hint.setPadding(new Insets(0, 14, 8, 14));
        com.ars.fx.shell.Tips.tip(source, "Which log to look at: the normal log, or any contest log (archived ones too)");

        // ---- the normal log's table ----
        TableView<JLogDb.QsoFull> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);   // NOTES takes whatever width is left
        table.setPlaceholder(lbl("No QSOs match", "jl-emeta"));
        table.getColumns().addAll(List.of(
                col("DATE (UTC)", 126, q -> JLogDb.dateOf(q.datetimeUtc()) + " " + JLogDb.hhmm(q.datetimeUtc())),
                col("CALL", 96, JLogDb.QsoFull::call),
                col("FREQ", 74, JLogDb.QsoFull::freq),
                col("BAND", 52, JLogDb.QsoFull::band),
                col("MODE", 58, JLogDb.QsoFull::mode),
                col("RST S", 50, JLogDb.QsoFull::rstSent),
                col("RST R", 50, JLogDb.QsoFull::rstRcvd),
                col("NAME", 120, JLogDb.QsoFull::name),
                col("QTH", 120, JLogDb.QsoFull::qth),
                col("GRID", 70, JLogDb.QsoFull::grid),
                col("ST", 40, JLogDb.QsoFull::state),
                col("COUNTY", 90, JLogDb.QsoFull::county),
                col("PWR", 46, q -> q.powerW() > 0 ? String.valueOf(q.powerW()) : ""),
                col("CONF", 54, q -> (q.refs().qsl() ? "Q" : "") + (q.refs().lotw() ? "L" : "") + (q.refs().eqsl() ? "E" : "")),
                col("REFS", 170, q -> refsText(q.refs())),
                col("NOTES", 100, JLogDb.QsoFull::notes)));
        // the date column holds "yyyy-MM-dd HH:mm", which sorts correctly as text — newest first to start
        @SuppressWarnings("unchecked")
        TableColumn<JLogDb.QsoFull, String> dateCol = (TableColumn<JLogDb.QsoFull, String>) table.getColumns().get(0);
        dateCol.setComparator(Comparator.naturalOrder());
        SortedList<JLogDb.QsoFull> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        // ---- a contest log's table (view only) ----
        TableView<QsoRecord> ctable = new TableView<>();
        ctable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        ctable.setPlaceholder(lbl("No QSOs match", "jl-emeta"));
        TableColumn<QsoRecord, String> cDateCol = ccol("DATE (UTC)", 126, q -> q.getDateTimeUtc() == null ? "" : q.getDateTimeUtc().format(MINUTE));
        cDateCol.setComparator(Comparator.naturalOrder());
        ctable.getColumns().add(cDateCol);
        ctable.getColumns().add(ccol("CALL", 96, QsoRecord::getCallsign));
        ctable.getColumns().add(ccol("FREQ kHz", 76, QsoRecord::getFrequency));
        ctable.getColumns().add(ccol("BAND", 52, QsoRecord::getBand));
        ctable.getColumns().add(ccol("MODE", 58, QsoRecord::getMode));
        ctable.getColumns().add(ccol("SN S", 52, QsoRecord::getSerialSent));
        ctable.getColumns().add(ccol("RST", 74, q -> nz(q.getRstSent()).isEmpty() && nz(q.getRstReceived()).isEmpty() ? "" : nz(q.getRstSent()) + " / " + nz(q.getRstReceived())));
        ctable.getColumns().add(ccol("SN R", 52, QsoRecord::getSerialReceived));
        ctable.getColumns().add(ccol("EXCHANGE", 190, LogExport::exchange));
        ctable.getColumns().add(ccol("PTS", 56, q -> q.isDupe() ? "DUPE" : String.valueOf(q.getPoints())));
        ctable.getColumns().add(ccol("NOTES", 100, QsoRecord::getNotes));
        SortedList<QsoRecord> cSorted = new SortedList<>(cShown);
        cSorted.comparatorProperty().bind(ctable.comparatorProperty());
        ctable.setItems(cSorted);
        ctable.setVisible(false); ctable.setManaged(false);

        BooleanSupplier isContest = () -> source.getSelectionModel().getSelectedIndex() > 0;
        Path[] active = {null};              // the contest log file being shown, kept across a refresh of the list
        boolean[] busy = {false};            // the list of logs is being rebuilt: its selection events are not the operator's

        Runnable refilter = () -> {
            String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            String b = band.getValue(), m = mode.getValue();
            if (isContest.getAsBoolean()) {
                cShown.setPredicate(q -> pick(b, "All bands", q.getBand()) && pick(m, "All modes", q.getMode())
                        && (needle.isEmpty() || contestHaystack(q).contains(needle)));
                count.setText(cShown.size() + " of " + cRows.size() + " QSOs");
            } else {
                shown.setPredicate(q -> pick(b, "All bands", q.band()) && pick(m, "All modes", q.mode())
                        && (needle.isEmpty() || haystack(q).contains(needle)));
                count.setText(shown.size() + " of " + nRows.size() + " QSOs");
            }
        };

        Runnable reloadNormal = () -> {
            List<JLogDb.QsoFull> rows = JLogDb.all();
            nRows.setAll(rows);
            TreeSet<String> bands = bandSet(), modes = new TreeSet<>();
            for (JLogDb.QsoFull q : rows) { if (!nz(q.band()).isBlank()) bands.add(q.band()); if (!nz(q.mode()).isBlank()) modes.add(q.mode()); }
            fillFilters(band, mode, bands, modes);
            refilter.run();
        };
        Runnable reloadContest = () -> {
            int i = source.getSelectionModel().getSelectedIndex() - 1;
            if (i < 0 || i >= logs.size()) return;
            ContestLogs.Info info = logs.get(i);
            List<QsoRecord> rows;
            try { rows = ContestLogReader.read(info.path(), info.contestId()); }
            catch (Exception ex) { cRows.clear(); count.setText("✗ Could not read " + info.fileName() + ": " + ex.getMessage()); return; }
            cRows.setAll(rows);
            TreeSet<String> bands = bandSet(), modes = new TreeSet<>();
            for (QsoRecord q : rows) { if (!nz(q.getBand()).isBlank()) bands.add(q.getBand()); if (!nz(q.getMode()).isBlank()) modes.add(q.getMode()); }
            fillFilters(band, mode, bands, modes);
            refilter.run();
        };

        // which table shows, and what the buttons can do for it
        Runnable applyMode = () -> {
            boolean c = isContest.getAsBoolean();
            table.setVisible(!c); table.setManaged(!c);
            ctable.setVisible(c); ctable.setManaged(c);
            int i = source.getSelectionModel().getSelectedIndex() - 1;
            boolean live = c && i >= 0 && i < logs.size() && changeable(logs.get(i));
            edit.setDisable(c && !live); del.setDisable(c && !live);
            hint.setText(!c ? NORMAL_HINT : live ? CONTEST_EDIT_HINT : CONTEST_HINT);
            search.setPromptText(c ? "Search call, exchange, notes…" : NORMAL_PROMPT);
            active[0] = c && i >= 0 && i < logs.size() ? logs.get(i).path() : null;
        };
        // the Log list: the normal log, then every usable and archived contest log (the one chosen stays chosen across a refresh)
        Runnable rebuildSources = () -> {
            List<ContestLogs.Info> all = new ArrayList<>();
            try { all.addAll(ContestLogs.usable()); all.addAll(ContestLogs.archived()); } catch (Throwable ignored) { /* just the normal log */ }
            busy[0] = true;
            try {
                logs.clear(); logs.addAll(all);
                List<String> items = new ArrayList<>(); items.add("Normal log");
                for (ContestLogs.Info i : all) items.add(sourceLabel(i));
                source.getItems().setAll(items);
                int sel = 0;
                for (int k = 0; k < all.size(); k++) if (active[0] != null && all.get(k).path().equals(active[0])) sel = k + 1;
                source.getSelectionModel().select(sel);
            } finally { busy[0] = false; }
        };

        refreshRows = () -> { if (isContest.getAsBoolean()) reloadContest.run(); else reloadNormal.run(); };
        reload = () -> { rebuildSources.run(); applyMode.run(); refreshRows.run(); };

        source.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> { if (!busy[0]) { applyMode.run(); refreshRows.run(); } });
        search.textProperty().addListener((o, a, b) -> refilter.run());
        band.valueProperty().addListener((o, a, b) -> refilter.run());
        mode.valueProperty().addListener((o, a, b) -> refilter.run());
        refresh.setOnMouseClicked(e -> reload.run());

        Runnable doEdit = () -> {
            if (isContest.getAsBoolean()) {                             // a contest QSO is edited in the contest page's form
                QsoRecord r = ctable.getSelectionModel().getSelectedItem();
                int i = source.getSelectionModel().getSelectedIndex() - 1;
                if (r == null || i < 0 || i >= logs.size() || !changeable(logs.get(i))) return;
                if (!JLogContest.editFromBrowser(r)) count.setText("Open the contest page first, then Edit loads the QSO into its form");
                else count.setText("✎ " + r.getCallsign() + " is loaded in the contest page: Enter saves it, Esc cancels");
                return;
            }
            JLogDb.QsoFull q = table.getSelectionModel().getSelectedItem();
            if (q != null && onEdit != null) {
                onEdit.accept(q.id());
                if (stage != null && stage.getOwner() != null) { stage.getOwner().requestFocus(); }   // back to the J-Log form
            }
        };
        edit.setOnMouseClicked(e -> doEdit.run());
        table.setOnMouseClicked(e -> { if (e.getClickCount() == 2) doEdit.run(); });
        ctable.setOnMouseClicked(e -> { if (e.getClickCount() == 2) doEdit.run(); });
        del.setOnMouseClicked(e -> {
            if (isContest.getAsBoolean()) {
                QsoRecord r = ctable.getSelectionModel().getSelectedItem();
                int i = source.getSelectionModel().getSelectedIndex() - 1;
                if (r != null && i >= 0 && i < logs.size() && changeable(logs.get(i)))
                    JLogContest.eraseFromBrowser(ctable, r, () -> Platform.runLater(() -> { refreshRows.run(); if (afterChange != null) afterChange.run(); }));
                return;
            }
            JLogDb.QsoFull q = table.getSelectionModel().getSelectedItem();
            if (q == null) return;
            Confirm.show(table, "Delete this QSO?", q.call() + " · " + JLogDb.dateOf(q.datetimeUtc()) + " " + JLogDb.hhmm(q.datetimeUtc())
                            + " · " + nz(q.band()) + " " + nz(q.mode()) + "\nThis cannot be undone.",
                    new Confirm.Choice("Cancel", false, null),
                    new Confirm.Choice("Delete", true, () -> {
                        if (JLogDb.delete(q.id())) { com.ars.fx.data.N1mmBroadcast.normalDeleted(com.ars.fx.data.N1mmBroadcast.of(q)); refreshRows.run(); if (afterChange != null) afterChange.run(); }
                    }));
        });

        // Export what the table shows now (search + Band + Mode filters, in the sort order on screen) to ADIF or CSV.
        export.setOnMouseClicked(e -> {
            boolean contest = isContest.getAsBoolean();
            int n = contest ? cSorted.size() : sorted.size();
            if (n == 0) { count.setText("Nothing to export — no QSOs match"); return; }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Export " + n + " QSO" + (n == 1 ? "" : "s"));
            fc.getExtensionFilters().addAll(new javafx.stage.FileChooser.ExtensionFilter("ADIF (*.adi)", "*.adi"),
                    new javafx.stage.FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
            ContestLogs.Info info = contest ? logs.get(source.getSelectionModel().getSelectedIndex() - 1) : null;
            fc.setInitialFileName(contest ? info.fileName().replaceAll("(?i)\\.db$", "") + "-filtered" : "j-log-filtered");
            java.io.File dir = LogExport.defaultBackupDir().toFile();
            if (dir.isDirectory()) fc.setInitialDirectory(dir);
            java.io.File f = fc.showSaveDialog(export.getScene().getWindow());
            if (f == null) return;
            boolean csv = fc.getSelectedExtensionFilter() != null && fc.getSelectedExtensionFilter().getDescription().startsWith("CSV");
            String name = f.getName().toLowerCase();
            if (!name.endsWith(".csv") && !name.endsWith(".adi") && !name.endsWith(".adif"))
                f = new java.io.File(f.getPath() + (csv ? ".csv" : ".adi"));      // the chosen filter decides when no extension was typed
            LogExport.Result r = contest
                    ? LogExport.exportContest(f.toPath(), new ArrayList<>(cSorted), cabrilloName(info))
                    : LogExport.exportFiltered(f.toPath(), new ArrayList<>(sorted));
            count.setText(r.ok() ? "✓ Exported " + r.count() + " QSO" + (r.count() == 1 ? "" : "s") + " to " + f.getName()
                                 : "✗ Export failed: " + r.error());
        });
        com.ars.fx.shell.Tips.tip(export, "Save the QSOs shown now (after the search, Band and Mode filters) as ADIF or CSV");

        // Reprint the QSL label for the selected QSO (the "print on save" switch only covers a QSO as it is first logged).
        printLbl.setOnMouseClicked(e -> {
            QslLabel.Qso q; String call;
            if (isContest.getAsBoolean()) {
                QsoRecord r = ctable.getSelectionModel().getSelectedItem();
                q = r == null ? null : QslLabel.fromContest(r); call = r == null ? "" : r.getCallsign();
            } else {
                JLogDb.QsoFull r = table.getSelectionModel().getSelectedItem();
                q = r == null ? null : QslLabel.fromFull(r); call = r == null ? "" : r.call();
            }
            if (q == null) { count.setText("Select a QSO to print its label"); return; }
            count.setText("⋯ printing label for " + call + "…");
            QslLabel.printAsync(q, r -> javafx.application.Platform.runLater(() -> count.setText((r.ok() ? "✓ " : "✗ ") + r.message())));
        });
        com.ars.fx.shell.Tips.tip(printLbl, "Print the QSL label for the selected QSO again — printer and label size are set in Setup ▸ Label printers");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, source, search, band, mode, count, sp, refresh, export, printLbl, edit, del);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10, 14, 10, 14));
        StackPane holder = new StackPane(table, ctable);
        VBox.setVgrow(holder, Priority.ALWAYS);
        VBox root = new VBox(bar, hint, holder);

        Scene scene = new Scene(root, 1500, 620);
        Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        Themes.applyTo(root, "log");
        Stage st = new Stage(); st.setTitle("J-Log — full log"); st.setScene(scene); com.ars.fx.AppIcons.apply(st, null);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        st.setOnHidden(e -> { stage = null; reload = null; refreshRows = null; });
        stage = st;
        reload.run();
        table.getSortOrder().add(dateCol); dateCol.setSortType(TableColumn.SortType.DESCENDING);
        ctable.getSortOrder().add(cDateCol); cDateCol.setSortType(TableColumn.SortType.DESCENDING);
        st.show();
    }

    /** Refresh the browser's rows if it is open (call after the log changes). Does not re-scan the list of contest logs. */
    static void refreshIfOpen() { if (stage != null && refreshRows != null) refreshRows.run(); }

    private static TableColumn<JLogDb.QsoFull, String> col(String title, double w, Function<JLogDb.QsoFull, String> get) {
        TableColumn<JLogDb.QsoFull, String> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(nz(get.apply(cd.getValue()))));
        return c;
    }

    private static TableColumn<QsoRecord, String> ccol(String title, double w, Function<QsoRecord, String> get) {
        TableColumn<QsoRecord, String> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(nz(get.apply(cd.getValue()))));
        return c;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    /** True when the Band / Mode list is on "All ..." or on this value. */
    static boolean pick(String choice, String all, String value) {
        return choice == null || choice.equals(all) || choice.equalsIgnoreCase(nz(value));
    }

    private static TreeSet<String> bandSet() {
        return new TreeSet<>(Comparator.comparingDouble(LogBrowser::bandOrder).thenComparing(Comparator.naturalOrder()));
    }

    /** Refill the Band and Mode lists from the rows now shown, keeping the choice if it is still there. */
    private static void fillFilters(ComboBox<String> band, ComboBox<String> mode, Collection<String> bands, Collection<String> modes) {
        String keepB = band.getValue(), keepM = mode.getValue();
        band.getItems().setAll("All bands"); band.getItems().addAll(bands);
        mode.getItems().setAll("All modes"); mode.getItems().addAll(modes);
        band.setValue(keepB != null && band.getItems().contains(keepB) ? keepB : "All bands");
        mode.setValue(keepM != null && mode.getItems().contains(keepM) ? keepM : "All modes");
    }

    /** A contest log the app is writing to and showing on the contest page: its QSOs can be edited and erased here. */
    static boolean changeable(ContestLogs.Info i) {
        return i != null && i.current() && !i.archived() && JLogContest.isActiveContest(i.contestId());
    }

    /** "CQ WW SSB · NovCqWw.db · 412 QSOs (archived)": what the Log list shows for a contest log. */
    static String sourceLabel(ContestLogs.Info i) {
        String what = !nz(i.contestName()).isBlank() ? i.contestName() : i.fileName();
        return what + " · " + i.fileName() + " · " + i.qsos() + " QSO" + (i.qsos() == 1 ? "" : "s")
                + (i.archived() ? " (archived)" : i.current() ? " (in use)" : "");
    }

    /** The contest's Cabrillo name for ADIF's CONTEST_ID, or null when its definition is not known. */
    private static String cabrilloName(ContestLogs.Info info) {
        try {
            com.jlog.plugin.ContestPlugin p = com.jlog.plugin.PluginLoader.getInstance().getById(info.contestId());
            return p == null ? null : p.getCabrilloContestName();
        } catch (Throwable e) { return null; }
    }

    /** "IOTA NA-005 · POTA K-0039" — only the references the QSO has. */
    private static String refsText(JLogDb.Refs r) {
        StringBuilder b = new StringBuilder();
        if (!nz(r.iota()).isEmpty()) b.append("IOTA ").append(r.iota());
        if (!nz(r.pota()).isEmpty()) b.append(b.length() > 0 ? " · " : "").append("POTA ").append(r.pota());
        if (!nz(r.sota()).isEmpty()) b.append(b.length() > 0 ? " · " : "").append("SOTA ").append(r.sota());
        return b.toString();
    }

    private static String haystack(JLogDb.QsoFull q) {
        return (nz(q.call()) + " " + nz(q.name()) + " " + nz(q.qth()) + " " + nz(q.grid()) + " " + nz(q.state()) + " "
                + nz(q.county()) + " " + nz(q.notes()) + " " + refsText(q.refs())).toLowerCase();
    }

    /** What the search box matches in a contest QSO: call, serials, the exchange, operator and notes. */
    static String contestHaystack(QsoRecord q) {
        return (nz(q.getCallsign()) + " " + nz(q.getSerialSent()) + " " + nz(q.getSerialReceived()) + " " + LogExport.exchange(q) + " "
                + nz(q.getOperator()) + " " + nz(q.getNotes())).toLowerCase();
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
