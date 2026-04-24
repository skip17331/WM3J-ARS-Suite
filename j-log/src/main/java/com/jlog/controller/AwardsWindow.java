package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.award.AwardLoader;
import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardProgress;
import com.jlog.award.AwardService;
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
        refresh.setOnAction(e -> {
            AwardLoader.getInstance().reload();
            rebuild();
        });
        Button importBtn = new Button("Import Award…");
        importBtn.setOnAction(e -> doImport());

        HBox toolbar = new HBox(10, refresh, importBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        cards.setPrefColumns(3);
        cards.setHgap(12);
        cards.setVgap(12);
        cards.setPadding(new Insets(12));
        rebuild();

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

    private void rebuild() {
        cards.getChildren().clear();
        List<AwardPlugin> awards = AwardLoader.getInstance().getAvailableAwards();
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

    private void showDetails(AwardPlugin a, AwardProgress prog) {
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
