package com.ars.fx.surface;

import com.ars.fx.Theme;
import com.ars.fx.Themes;
import com.ars.fx.data.AwardLevels;
import com.ars.fx.data.Awards;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.ars.fx.shell.Shell.lbl;

/**
 * The awards screen: one tab per award, each showing what is worked and what is confirmed. Opened from J-Log's Awards drawer.
 * DXCC has a band-by-band matrix; WAS, WAC and the zones are chip grids; grids, IOTA, POTA and SOTA are reference lists.
 */
public final class AwardsWindow {
    private AwardsWindow() {}

    private static Stage stage;
    private static TabPane tabs;
    private static Label status;

    /** Open (or raise) the awards screen from anywhere - the dock button uses this. */
    public static void open() { show(null); }

    static void show(Node owner) {
        if (stage != null) { stage.show(); stage.toFront(); load(); return; }
        tabs = new TabPane(); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        status = lbl("Working out your awards…", "jl-emeta");
        Label refresh = lbl("⟳ Refresh", "jl-clearbtn"); refresh.setStyle("-fx-cursor:hand;");
        refresh.setOnMouseClicked(e -> load());
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, lbl("Awards", "jhub-card-title"), status, sp, refresh);
        bar.setAlignment(Pos.CENTER_LEFT); bar.setPadding(new Insets(10, 14, 10, 14));
        VBox.setVgrow(tabs, Priority.ALWAYS);
        VBox root = new VBox(bar, tabs);

        Scene scene = new Scene(root, 1180, 680);
        Theme.apply(scene, false); com.ars.fx.shell.Tips.install(scene);
        Themes.applyTo(root, "log");
        Stage st = new Stage(); st.setTitle("J-Log — awards"); st.setScene(scene); com.ars.fx.AppIcons.apply(st, null);
        if (owner != null && owner.getScene() != null && owner.getScene().getWindow() != null) st.initOwner(owner.getScene().getWindow());
        st.setOnHidden(e -> { stage = null; tabs = null; status = null; });
        stage = st;
        load();
        st.show();
    }

    /** Recompute off the FX thread, then rebuild the tabs (keeping the one the operator was on). */
    private static void load() {
        if (status != null) status.setText("Working out your awards…");
        Thread t = new Thread(() -> {
            Awards.Snapshot snap = Awards.compute();
            Platform.runLater(() -> {
                if (tabs == null) return;
                int keep = Math.max(0, tabs.getSelectionModel().getSelectedIndex());
                tabs.getTabs().setAll(
                        tab("Summary", summary(snap)),
                        tab("DXCC", dxcc(snap)),
                        tab("WAS", chips(snap.was, "Worked All States — the 50 states.", snap)),
                        tab("WAC", chips(snap.wac, "Worked All Continents.", snap)),
                        tab("Grids", refs(snap.grids, snap.gridStats, "Reference", "Four-character Maidenhead squares from the GRID field. VUCC counts squares on 50 MHz and up; this lists them on every band.")),
                        tab("IOTA", refs(snap.iota, snap.iotaStats, "Island", "Island references entered on the QSO or imported (for example NA-005). Enter the other station's island in J-Log's IOTA field.")),
                        tab("POTA", refs(snap.pota, snap.potaStats, "Park", "Parks you have hunted — the park reference is for the other station.")),
                        tab("SOTA", refs(snap.sota, snap.sotaStats, "Summit", "Summits you have chased — the summit reference is for the other station.")),
                        tab("Zones", zones(snap)),
                        tab("Levels", levels(snap)),
                        tab("My awards", CustomAwardsView.build()));
                tabs.getSelectionModel().select(Math.min(keep, tabs.getTabs().size() - 1));
                status.setText(snap.qsoCount + " QSOs · " + snap.dxcc.worked() + " entities worked, " + snap.dxcc.confirmed() + " confirmed"
                        + (snap.unresolved > 0 ? " · " + snap.unresolved + " calls with no entity" : ""));
            });
        }, "awards");
        t.setDaemon(true); t.start();
    }

    private static Tab tab(String name, Node content) { Tab t = new Tab(name, content); return t; }

    private static VBox page(Node... n) {
        VBox v = new VBox(12, n); v.setPadding(new Insets(14)); return v;
    }

    // ---- summary -----------------------------------------------------------------------------------
    private static Node summary(Awards.Snapshot s) {
        VBox rows = new VBox(10);
        for (Awards.Progress p : s.all()) rows.getChildren().add(progressRow(p));
        Label hint = lbl("Worked = logged. Confirmed = a paper QSL, LoTW or eQSL has come back — tick them on a QSO in J-Log, "
                + "or import an ADIF file downloaded from LoTW / eQSL and the QSOs already in your log are marked.", "jl-emeta");
        hint.setWrapText(true);
        return scroll(page(hint, rows));
    }

    /** One award: name, bar (confirmed darker inside worked), and the counts. */
    static Node progressRow(Awards.Progress p) {
        double w = p.target() > 0 ? Math.min(1, (double) p.worked() / p.target()) : 0;
        double c = p.target() > 0 ? Math.min(1, (double) p.confirmed() / p.target()) : 0;
        Label name = lbl(p.code() + "  " + p.name(), "jl-tr-call"); name.setMinWidth(300);
        StackPane bar = new StackPane(); bar.setMinHeight(10); bar.setMaxHeight(10); bar.setMaxWidth(Double.MAX_VALUE);
        Region track = new Region(); track.getStyleClass().add("jl-award-bar-track"); track.setMinHeight(10); track.setMaxHeight(10);
        Region fillW = new Region(); fillW.getStyleClass().add("jl-award-bar-fill"); fillW.setMinHeight(10); fillW.setMaxHeight(10); fillW.setOpacity(0.45);
        Region fillC = new Region(); fillC.getStyleClass().add("jl-award-bar-fill"); fillC.setMinHeight(10); fillC.setMaxHeight(10);
        StackPane.setAlignment(fillW, Pos.CENTER_LEFT); StackPane.setAlignment(fillC, Pos.CENTER_LEFT);
        bar.getChildren().addAll(track, fillW, fillC);
        fillW.maxWidthProperty().bind(bar.widthProperty().multiply(w));
        fillC.maxWidthProperty().bind(bar.widthProperty().multiply(c));
        Label cnt = lbl(p.worked() + " worked · " + p.confirmed() + " confirmed · of " + p.target(), "jl-award-pct"); cnt.setMinWidth(250);
        HBox.setHgrow(bar, Priority.ALWAYS);
        HBox r = new HBox(14, name, bar, cnt); r.setAlignment(Pos.CENTER_LEFT);
        return r;
    }

    // ---- DXCC matrix -------------------------------------------------------------------------------
    private static Node dxcc(Awards.Snapshot s) {
        FlowPane counts = new FlowPane(6, 6);
        counts.getChildren().add(lbl("Entities by band (worked / confirmed):", "jl-emeta"));
        for (Map.Entry<String, int[]> e : s.dxccByBand.entrySet())
            counts.getChildren().add(chip(e.getKey() + "  " + e.getValue()[0] + "/" + e.getValue()[1], e.getValue()[1] > 0 ? "conf" : e.getValue()[0] > 0 ? "worked" : ""));
        FlowPane modes = new FlowPane(6, 6);
        modes.getChildren().add(lbl("By mode:", "jl-emeta"));
        for (Map.Entry<String, int[]> e : s.dxccByMode.entrySet())
            modes.getChildren().add(chip(e.getKey() + "  " + e.getValue()[0] + "/" + e.getValue()[1], e.getValue()[1] > 0 ? "conf" : e.getValue()[0] > 0 ? "worked" : ""));

        TableView<Awards.Entity> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        table.setPlaceholder(lbl("No DXCC entities match", "jl-emeta"));
        table.getColumns().add(col("ENTITY", 190, en -> en.name));
        TableColumn<Awards.Entity, String> num = col("#", 46, en -> en.number > 0 ? String.valueOf(en.number) : "");
        num.setComparator(Comparator.comparingInt(x -> x.isEmpty() ? 0 : Integer.parseInt(x)));
        table.getColumns().add(num);
        table.getColumns().add(col("CONT", 50, en -> en.continent == null ? "" : en.continent));
        table.getColumns().add(col("FIRST", 100, en -> en.firstDate));
        table.getColumns().add(col("QSOs", 50, en -> String.valueOf(en.qsos)));
        for (String b : Awards.BANDS) table.getColumns().add(col(b, 52, en -> en.bandsConfirmed.contains(b) ? "✓" : en.bandsWorked.contains(b) ? "w" : ""));
        for (String m : Awards.MODE_GROUPS) table.getColumns().add(col(m.toUpperCase(), 70, en -> en.modesConfirmed.contains(m) ? "✓" : en.modesWorked.contains(m) ? "w" : ""));
        TableColumn<Awards.Entity, String> last = col("CONFIRMED", 110, en -> en.confirmed ? "✓" : "");
        table.getColumns().add(last);

        FilteredList<Awards.Entity> shown = new FilteredList<>(FXCollections.observableArrayList(s.entities.values()), x -> true);
        SortedList<Awards.Entity> sorted = new SortedList<>(shown);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);
        table.getSortOrder().add(table.getColumns().get(0));      // by entity name to start
        TextField search = new TextField(); search.setPromptText("Search entity…"); search.getStyleClass().add("jhub-mini-field"); search.setMinWidth(220);
        ComboBox<String> filter = new ComboBox<>(); filter.getItems().addAll("All entities", "Worked, not confirmed", "Confirmed");
        filter.setValue("All entities"); filter.setMinWidth(190);
        Label count = lbl("", "jl-emeta");
        Runnable refilter = () -> {
            String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase(); String f = filter.getValue();
            shown.setPredicate(en -> (needle.isEmpty() || en.name.toLowerCase().contains(needle) || String.valueOf(en.number).equals(needle))
                    && (f == null || f.startsWith("All") || (f.startsWith("Worked") ? !en.confirmed : en.confirmed)));
            count.setText(shown.size() + " of " + s.entities.size() + " entities");
        };
        search.textProperty().addListener((o, a, b) -> refilter.run());
        filter.valueProperty().addListener((o, a, b) -> refilter.run());
        refilter.run();
        Label key = lbl("✓ confirmed · w worked only", "jl-emeta");
        HBox bar = new HBox(10, search, filter, count, key); bar.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(table, Priority.ALWAYS);
        VBox v = page(progressRow(s.dxcc), counts, modes, bar, table);
        return v;
    }

    // ---- chip grids (WAS / WAC / zones) ------------------------------------------------------------
    private static Node chips(Awards.Progress p, String blurb, Awards.Snapshot s) {
        FlowPane fp = new FlowPane(6, 6);
        for (String item : p.all()) fp.getChildren().add(chip(item, p.confirmedSet().contains(item) ? "conf" : p.workedSet().contains(item) ? "worked" : ""));
        Label legend = lbl(blurb + "  Green = worked, blue = confirmed, grey = still needed.", "jl-emeta"); legend.setWrapText(true);
        return scroll(page(progressRow(p), legend, fp));
    }

    // ---- endorsement levels -------------------------------------------------------------------------
    private static Node levels(Awards.Snapshot snap) {
        VBox rows = new VBox(8);
        String group = "";
        for (AwardLevels.Level l : AwardLevels.compute(snap)) {
            if (!l.group().equals(group)) {
                group = l.group();
                Label g = lbl(group, "jhub-card-title"); g.setPadding(new Insets(14, 0, 2, 0));
                rows.getChildren().add(g);
            }
            rows.getChildren().add(levelRow(l));
        }
        Label hint = lbl("Confirmed (a QSL / LoTW / eQSL tick on the QSO) is what the sponsors credit; the bar shows confirmed, the first number is worked. "
                + "Thresholds are the published ones - check the current rules before you apply.", "jl-emeta");
        hint.setWrapText(true);
        ScrollPane sp = new ScrollPane(page(rows, hint)); sp.setFitToWidth(true);
        return sp;
    }
    private static Node levelRow(AwardLevels.Level l) {
        StackPane bar = new StackPane(); bar.setMinHeight(6); bar.setMaxHeight(6); bar.setMaxWidth(Double.MAX_VALUE);
        Region track = new Region(); track.getStyleClass().add("jl-award-bar-track"); track.setMinHeight(6); track.setMaxHeight(6); track.setMaxWidth(Double.MAX_VALUE);
        Region fill = new Region(); fill.getStyleClass().add("jl-award-bar-fill"); fill.setMinHeight(6); fill.setMaxHeight(6);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT); bar.getChildren().addAll(track, fill);
        fill.maxWidthProperty().bind(bar.widthProperty().multiply(l.fraction(true)));
        Label name = lbl(l.name(), "jl-skim-meta"); name.setMinWidth(260);
        Label cnt = lbl(l.worked() + " worked  \u00b7  " + l.confirmed() + "\u2714 of " + l.target() + (l.done() ? "   DONE" : ""), "jl-award-pct");
        cnt.setMinWidth(210);
        HBox r = new HBox(10, name, bar, cnt); HBox.setHgrow(bar, Priority.ALWAYS); r.setAlignment(Pos.CENTER_LEFT);
        if (!l.note().isEmpty()) com.ars.fx.shell.Tips.tip(r, l.note());
        return r;
    }

    private static Node zones(Awards.Snapshot s) {
        FlowPane cq = new FlowPane(6, 6), itu = new FlowPane(6, 6);
        for (String z : s.cqz.all()) cq.getChildren().add(chip(z, s.cqz.confirmedSet().contains(z) ? "conf" : s.cqz.workedSet().contains(z) ? "worked" : ""));
        for (String z : s.ituz.all()) itu.getChildren().add(chip(z, s.ituz.confirmedSet().contains(z) ? "conf" : s.ituz.workedSet().contains(z) ? "worked" : ""));
        Label note = lbl("Zones are not worked out from the callsign. They count where the QSO carries a CQ / ITU zone — "
                + "for now that means QSOs imported from an ADIF file that has CQZ / ITUZ fields. Green = worked, blue = confirmed.", "jl-emeta");
        note.setWrapText(true);
        return scroll(page(progressRow(s.cqz), cq, progressRow(s.ituz), itu, note));
    }

    // ---- reference lists ---------------------------------------------------------------------------
    private static Node refs(Awards.Progress p, Map<String, Awards.RefStat> stats, String keyName, String blurb) {
        TableView<Awards.RefStat> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_LAST_COLUMN);
        table.setPlaceholder(lbl("Nothing logged yet.", "jl-emeta"));
        table.getColumns().addAll(List.of(
                col(keyName.toUpperCase(), 160, r -> r.key), col("QSOs", 60, r -> String.valueOf(r.qsos)),
                col("FIRST WORKED", 110, r -> r.firstDate), col("LAST CALL", 120, r -> r.lastCall),
                col("BANDS", 240, r -> String.join(" ", r.bands)), col("CONFIRMED", 90, r -> r.confirmed ? "✓" : "")));
        table.setItems(FXCollections.observableArrayList(stats.values()));
        Label b = lbl(blurb, "jl-emeta"); b.setWrapText(true);
        VBox.setVgrow(table, Priority.ALWAYS);
        return page(progressRow(p), b, table);
    }

    // ---- small helpers -----------------------------------------------------------------------------
    private static <T> TableColumn<T, String> col(String title, double w, Function<T, String> get) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setPrefWidth(w);
        c.setCellValueFactory(cd -> new SimpleStringProperty(get.apply(cd.getValue())));
        return c;
    }
    private static Label chip(String text, String state) {
        Label l = lbl(text, "jl-award-chip");
        if (!state.isEmpty()) l.getStyleClass().add(state);
        return l;
    }
    private static Node scroll(Node n) {
        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(n);
        sp.setFitToWidth(true); sp.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        sp.setStyle("-fx-background-color:transparent;");
        return sp;
    }
}
