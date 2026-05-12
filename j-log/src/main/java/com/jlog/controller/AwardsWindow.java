package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.award.AwardLoader;
import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardProgress;
import com.jlog.award.AwardService;
import com.jlog.db.QsoDao;
import com.jlog.export.ColoniesLogSheetPdf;
import com.jlog.model.QsoRecord;
import com.jlog.ui.map.DxccMap;
import com.jlog.ui.map.DxccTable;
import com.jlog.ui.map.StatesMap;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Standalone "Awards" window for the normal-log side of j-log. Renders every
 * {@link AwardPlugin} known to {@link AwardLoader} as a progress card — title,
 * bar, current tier, and count — and opens a drill-down dialog on click that
 * shows the worked/missing target breakdown.
 *
 * The window is data-driven from the award plugins alone; when a user drops a
 * new JSON into ~/.j-log/awards/, it appears here after a Refresh.
 */
public class AwardsWindow {

    private final Stage   owner;
    private final Stage   dialog = new Stage();
    private final TilePane cards = new TilePane();

    public AwardsWindow(Stage owner) {
        this.owner = owner;
    }

    public void show() {
        dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);
        dialog.setTitle("Awards");

        Label title = new Label("Awards progress");
        title.getStyleClass().add("chooser-prompt");

        Button refresh = new Button("Refresh");
        refresh.getStyleClass().add("primary-button");
        refresh.setOnAction(e -> {
            AwardLoader.getInstance().reload();
            try { rebuild(); }
            catch (Throwable t) { showError(t); }
        });
        Button importBtn = new Button("Import Award…");
        importBtn.getStyleClass().add("secondary-button");
        importBtn.setOnAction(e -> doImport());

        HBox toolbar = new HBox(10, refresh, importBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(4, 0, 4, 0));

        cards.setPrefColumns(3);
        cards.setHgap(12);
        cards.setVgap(12);
        cards.setPadding(new Insets(12));
        try {
            rebuild();
        } catch (Throwable t) {
            showError(t);
        }

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);

        VBox root = new VBox(10, title, toolbar, scroll);
        root.setPadding(new Insets(14));
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Scene scene = new Scene(root, 900, 640);
        JLogApp.applyTheme(scene);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showError(Throwable t) {
        cards.getChildren().clear();
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
        Label err = new Label("Awards could not be loaded:\n"
            + t.getClass().getSimpleName() + ": " + msg
            + "\n\nDrop award JSONs into ~/.j-log/awards/ and click Refresh.");
        err.setWrapText(true);
        cards.getChildren().add(err);
    }

    private void rebuild() {
        cards.getChildren().clear();
        // AwardLoader.getAvailableAwards() returns an immutable copy — wrap
        // in ArrayList before sorting so Collection.sort() doesn't throw.
        List<AwardPlugin> awards = new ArrayList<>(AwardLoader.getInstance().getAvailableAwards());
        // Sort: active special events first, then standard, then expired
        awards.sort(Comparator
            .<AwardPlugin, Integer>comparing(this::sortCategory)
            .thenComparing(AwardPlugin::getAwardName));
        for (AwardPlugin a : awards) {
            cards.getChildren().add(buildCard(a));
        }
        if (awards.isEmpty()) {
            cards.getChildren().add(new Label(
                "No awards loaded. Drop a JSON into ~/.j-log/awards/ and click Refresh."));
        }
    }

    /** Lower = shown first. 0 = active event, 1 = standard, 2 = expired. */
    private int sortCategory(AwardPlugin a) {
        if (a.getWindow() == null) return 1;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime end = a.getWindow().endDateTime();
        if (end != null && end.isBefore(now)) return 2;
        return 0;
    }

    private Region buildCard(AwardPlugin a) {
        AwardProgress prog = AwardService.getInstance().compute(a);

        Label name = new Label(a.getAwardName());
        name.setWrapText(true);
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

        ProgressBar bar = new ProgressBar(prog.progressRatio());
        bar.setPrefWidth(Double.MAX_VALUE);
        bar.setPrefHeight(14);

        int total = prog.totalRequired();
        String countText = a.isSetMatch()
            ? prog.count() + " / " + total
            : prog.count() + " " + (a.getTargetLabel() == null ? "worked" : a.getTargetLabel().toLowerCase());
        Label count = new Label(countText);
        count.setStyle("-fx-font-family: monospace;");

        AwardPlugin.Tier tier = prog.currentTier();
        Label tierLbl = new Label(tier == null ? "—" : "🏅 " + tier.getName());
        tierLbl.setStyle("-fx-font-size: 11px;");

        VBox windowInfo = new VBox(2);
        if (a.getWindow() != null) {
            LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
            LocalDateTime end = a.getWindow().endDateTime();
            String state = (end != null && end.isBefore(now)) ? "ENDED" : "ACTIVE";
            Label l = new Label("Event: " + state);
            l.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                + ("ENDED".equals(state) ? "#888" : "#2e7d32") + ";");
            windowInfo.getChildren().add(l);
        }

        Button details = new Button("Details…");
        details.setOnAction(e -> showDetails(a, prog));

        VBox box = new VBox(6, name, bar, count, tierLbl, windowInfo, details);
        box.setPadding(new Insets(10));
        box.setPrefWidth(250);
        box.setStyle("-fx-border-color: #aaa; -fx-border-radius: 4; "
                  + "-fx-background-radius: 4; -fx-background-color: -fx-control-inner-background;");
        return box;
    }

    /** Per the 2025 organizer sheet — K2x to state assignment changes year
     *  to year, but this is the authoritative mapping for this build. */
    private static final Map<String, String> COLONY_CALLSIGN_TO_STATE = Map.ofEntries(
        Map.entry("K2A", "NY"), Map.entry("K2B", "VA"), Map.entry("K2C", "RI"),
        Map.entry("K2D", "CT"), Map.entry("K2E", "DE"), Map.entry("K2F", "MD"),
        Map.entry("K2G", "GA"), Map.entry("K2H", "MA"), Map.entry("K2I", "NJ"),
        Map.entry("K2J", "NC"), Map.entry("K2K", "NH"), Map.entry("K2L", "SC"),
        Map.entry("K2M", "PA")
    );

    private void showDetails(AwardPlugin a, AwardProgress prog) {
        if ("DXCC".equalsIgnoreCase(a.getAwardId())) {
            showDxccMapDetails(a, prog);
            return;
        }
        if ("WAS".equalsIgnoreCase(a.getAwardId())) {
            showStateMapDetails(a, prog, prog.workedValues, /*highlightOnly*/ null);
            return;
        }
        if ("13_COLONIES_2026".equalsIgnoreCase(a.getAwardId())) {
            // Translate worked callsigns (K2A…K2M) to state codes (CT…VA).
            java.util.Set<String> workedStates = new java.util.HashSet<>();
            for (String call : prog.workedValues) {
                String st = COLONY_CALLSIGN_TO_STATE.get(call);
                if (st != null) workedStates.add(st);
            }
            showStateMapDetails(a, prog, workedStates,
                new java.util.HashSet<>(COLONY_CALLSIGN_TO_STATE.values()));
            return;
        }
        Stage d = new Stage();
        d.initOwner(dialog);
        d.initModality(Modality.APPLICATION_MODAL);
        d.setTitle(a.getAwardName());

        Label header = new Label(a.getDescription() == null ? "" : a.getDescription());
        header.setWrapText(true);
        header.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");

        Label status = new Label(String.format(
            "Worked: %d%s   Current tier: %s",
            prog.count(),
            a.isSetMatch() ? " / " + prog.totalRequired() : "",
            prog.currentTier() == null ? "—" : prog.currentTier().getName()));
        status.setStyle("-fx-font-weight: bold;");

        TabPane tabs = new TabPane();
        if (a.isSetMatch()) {
            tabs.getTabs().add(targetsTab("Worked", matchedList(a, prog), true));
            tabs.getTabs().add(targetsTab("Missing", prog.missingTargets, false));
            if (a.getBonus() != null && !a.getBonus().isEmpty()) {
                tabs.getTabs().add(targetsTab("Bonus", bonusList(a, prog), true));
            }
        } else {
            List<AwardPlugin.Target> synthetic = prog.workedValues.stream()
                .sorted()
                .map(v -> { var t = new AwardPlugin.Target(); t.setId(v); t.setLabel(v); return t; })
                .collect(Collectors.toList());
            tabs.getTabs().add(targetsTab("Worked (" + synthetic.size() + ")", synthetic, true));
        }

        VBox root = new VBox(10, header, status, tabs);
        root.setPadding(new Insets(14));
        VBox.setVgrow(tabs, Priority.ALWAYS);

        Scene scene = new Scene(root, 520, 520);
        JLogApp.applyTheme(scene);
        d.setScene(scene);
        d.show();
    }

    /** DXCC-specific details view: world map under a continent-grouped table.
     *  Worked-set is the lifetime distinct DXCC-prefix set (already computed by
     *  {@link AwardService} into {@code prog.workedValues}). */
    private void showDxccMapDetails(AwardPlugin a, AwardProgress prog) {
        Stage d = new Stage();
        d.initOwner(dialog);
        d.initModality(Modality.NONE);
        d.setTitle(a.getAwardName());

        DxccMap map = new DxccMap();
        map.setTooltipProvider(entity -> {
            DxccMap.EntityInfo info = map.entityInfo().get(entity);
            return info != null ? entity + " — " + info.name() : entity;
        });
        map.setOnRegionClicked(prefix -> { /* awards view: clicking does nothing */ });
        map.setRenderScale(0.80);

        DxccTable table = new DxccTable(map.entityInfo());
        table.setOnRowHovered(map::setCurrent);
        table.setPrefWidth(420);

        ScrollPane mapScroll = new ScrollPane(map);
        mapScroll.setFitToWidth(false);
        mapScroll.setFitToHeight(false);

        HBox split = new HBox(mapScroll, table);
        HBox.setHgrow(mapScroll, Priority.ALWAYS);

        Label header = new Label(String.format(
                "Worked: %d / %d   Current tier: %s",
                prog.count(), prog.totalRequired(),
                prog.currentTier() == null ? "—" : prog.currentTier().getName()));
        header.setStyle("-fx-font-weight: bold; -fx-padding: 6 10 6 10;");

        BorderPane root = new BorderPane(split);
        root.setTop(header);
        root.setStyle("-fx-background-color: -fx-control-inner-background;");

        Scene scene = new Scene(root, 1440, 720);
        JLogApp.applyTheme(scene);
        d.setScene(scene);

        map.setAllWorked(prog.workedValues);
        table.setAllWorked(prog.workedValues);

        d.show();
    }

    /**
     * State-based map view used by WAS and the 13 Colonies awards.
     *
     * @param workedStates  set of 2-letter state codes the user has worked
     * @param highlightOnly if non-null, restrict the map and table to only
     *                      these state codes (used by the 13 Colonies award);
     *                      pass null for the full 50-state view (WAS).
     */
    private void showStateMapDetails(AwardPlugin a, AwardProgress prog,
                                     Set<String> workedStates,
                                     Set<String> highlightOnly) {
        Stage d = new Stage();
        d.initOwner(dialog);
        d.initModality(Modality.NONE);
        d.setTitle(a.getAwardName());

        StatesMap map = new StatesMap();
        map.setTooltipProvider(code -> code);
        map.setRenderScale(0.80);

        // Build a (code → label) lookup from the award targets so the table
        // can show state names alongside the codes.
        Map<String, String> codeToLabel = new LinkedHashMap<>();
        if (a.getTargets() != null) {
            for (AwardPlugin.Target t : a.getTargets()) {
                String id    = t.getId();
                String label = t.getLabel() == null ? id : t.getLabel();
                String stateCode = highlightOnly != null
                    ? COLONY_CALLSIGN_TO_STATE.get(id)   // colonies: id is a callsign
                    : id;                                 // WAS: id is already a state code
                if (stateCode == null) continue;
                if (highlightOnly != null && !highlightOnly.contains(stateCode)) continue;
                codeToLabel.put(stateCode, label);
            }
        }

        TableView<StateRow> table = buildStateTable(codeToLabel, workedStates);

        ScrollPane mapScroll = new ScrollPane(map);
        mapScroll.setFitToWidth(false);
        mapScroll.setFitToHeight(false);

        HBox split = new HBox(mapScroll, table);
        HBox.setHgrow(mapScroll, Priority.ALWAYS);

        Label headerLabel = new Label(String.format(
                "Worked: %d / %d   Current tier: %s",
                prog.count(), prog.totalRequired(),
                prog.currentTier() == null ? "—" : prog.currentTier().getName()));
        headerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 6 10 6 10;");

        HBox header = new HBox(10, headerLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        // Spacer pushes any extras to the right.
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().add(spacer);
        if ("13_COLONIES_2026".equalsIgnoreCase(a.getAwardId())) {
            Button pdfBtn = new Button("Log Sheet PDF…");
            pdfBtn.getStyleClass().add("primary-button");
            pdfBtn.setOnAction(ev -> exportColoniesPdf(d));
            HBox.setMargin(pdfBtn, new Insets(4, 10, 4, 0));
            header.getChildren().add(pdfBtn);
        }

        BorderPane root = new BorderPane(split);
        root.setTop(header);
        root.setStyle("-fx-background-color: -fx-control-inner-background;");

        Scene scene = new Scene(root, 1440, 720);
        JLogApp.applyTheme(scene);
        d.setScene(scene);

        map.setAllWorked(workedStates);

        d.show();
    }

    /** "Log Sheet PDF…" handler — pulls the first QSO logged for each
     *  Colony callsign and writes the certificate-request form. */
    private void exportColoniesPdf(Stage owner) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save 13 Colonies Log Sheet");
        fc.setInitialFileName("13-colonies-log-sheet.pdf");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("PDF", "*.pdf"));
        java.io.File out = fc.showSaveDialog(owner);
        if (out == null) return;

        Map<String, QsoRecord> firstPerCall = new LinkedHashMap<>();
        try {
            // All 16 callsigns we care about — 13 colonies + 3 bonus.
            Set<String> targets = new HashSet<>(COLONY_CALLSIGN_TO_STATE.keySet());
            targets.add("WM3PEN");
            targets.add("GB13COL");
            targets.add("TM13COL");

            for (QsoRecord q : QsoDao.getInstance().fetchAll()) {
                String call = q.getCallsign() == null ? "" : q.getCallsign().toUpperCase();
                if (!targets.contains(call)) continue;
                firstPerCall.putIfAbsent(call, q); // earliest QSO wins (fetchAll is ASC by time)
            }
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                "Could not read QSOs: " + e.getMessage()).showAndWait();
            return;
        }

        try {
            ColoniesLogSheetPdf.write(out.toPath(), firstPerCall, /*operator*/ null);
            new Alert(Alert.AlertType.INFORMATION,
                "Saved log sheet to:\n" + out.getAbsolutePath()).showAndWait();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR,
                "PDF write failed: " + e.getMessage()).showAndWait();
        }
    }

    /** Three-column table (Code, State, Status) for the WAS / Colonies map view. */
    private TableView<StateRow> buildStateTable(Map<String, String> codeToLabel,
                                                Set<String> workedStates) {
        TableView<StateRow> table = new TableView<>();
        table.setPrefWidth(420);
        table.getStyleClass().add("dxcc-table");   // reuse worked/current CSS

        TableColumn<StateRow, String> cCode = new TableColumn<>("Code");
        cCode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().code));
        cCode.setPrefWidth(70);

        TableColumn<StateRow, String> cName = new TableColumn<>("State");
        cName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name));
        cName.setPrefWidth(220);

        TableColumn<StateRow, String> cStatus = new TableColumn<>("Worked");
        cStatus.setCellValueFactory(c ->
            new SimpleStringProperty(c.getValue().worked ? "✔" : ""));
        cStatus.setPrefWidth(90);

        table.getColumns().addAll(cCode, cName, cStatus);

        ObservableList<StateRow> rows = FXCollections.observableArrayList();
        for (var e : codeToLabel.entrySet()) {
            rows.add(new StateRow(e.getKey(), e.getValue(), workedStates.contains(e.getKey())));
        }
        table.setItems(rows);
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(StateRow item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("dxcc-row-worked");
                if (!empty && item != null && item.worked) {
                    getStyleClass().add("dxcc-row-worked");
                }
            }
        });
        return table;
    }

    private static class StateRow {
        final String code, name;
        final boolean worked;
        StateRow(String code, String name, boolean worked) {
            this.code = code; this.name = name; this.worked = worked;
        }
    }

    private Tab targetsTab(String title, List<AwardPlugin.Target> items, boolean worked) {
        ListView<String> list = new ListView<>();
        List<String> rows = items.stream()
            .map(t -> (worked ? "✔ " : "· ") + t.getId()
                + (t.getLabel() == null || t.getLabel().equals(t.getId()) ? ""
                    : "  — " + t.getLabel()))
            .collect(Collectors.toList());
        list.setItems(FXCollections.observableArrayList(rows));
        Tab tab = new Tab(title, list);
        tab.setClosable(false);
        return tab;
    }

    private List<AwardPlugin.Target> matchedList(AwardPlugin a, AwardProgress prog) {
        if (a.getTargets() == null) return List.of();
        return a.getTargets().stream()
            .filter(t -> prog.matchedTargets.contains(t.getId().toUpperCase()))
            .collect(Collectors.toList());
    }

    private List<AwardPlugin.Target> bonusList(AwardPlugin a, AwardProgress prog) {
        if (a.getBonus() == null) return List.of();
        return a.getBonus().stream()
            .filter(t -> prog.matchedBonus.contains(t.getId().toUpperCase()))
            .collect(Collectors.toList());
    }

    private void doImport() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Import Award Plugin");
        fc.getExtensionFilters().add(
            new javafx.stage.FileChooser.ExtensionFilter("Award JSON", "*.json"));
        java.io.File f = fc.showOpenDialog(dialog);
        if (f == null) return;
        try {
            AwardLoader.getInstance().importAward(f.toPath());
            rebuild();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR,
                "Failed to import award:\n" + ex.getMessage()).showAndWait();
        }
    }
}
