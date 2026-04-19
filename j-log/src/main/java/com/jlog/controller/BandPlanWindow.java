package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.util.BandPlan;
import com.jlog.util.BandPlan.BandSegment;
import com.jlog.util.BandPlan.LicenseClass;
import com.jlog.util.BandPlan.ModeGroup;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * BandPlanWindow — US FCC Part 97 band plan reference viewer.
 *
 * Displays all segments for the selected band in a color-coded table:
 *   Cyan/teal  — CW only
 *   Green      — CW / Data (no phone)
 *   Blue       — Phone / CW (all modes)
 *   Purple     — FM / Repeaters
 *   Gray       — Satellite
 *   Teal bold  — USB-only (60m)
 *
 * Rows are additionally marked with the minimum license class.
 * The segment containing {@code currentFreqKhz} is scrolled to and highlighted.
 */
public class BandPlanWindow {

    private final Stage stage;

    @SuppressWarnings("unchecked")
    public BandPlanWindow(Window owner, String initialBand, double currentFreqKhz) {

        stage = new Stage();
        stage.setTitle("US Amateur Band Plan  —  FCC Part 97");
        stage.setWidth(760);
        stage.setHeight(520);
        if (owner != null) stage.initOwner(owner);

        // ── Band selector ─────────────────────────────────────────────
        Label bandLbl = new Label("Band:");
        ComboBox<String> bandCb = new ComboBox<>(
                FXCollections.observableArrayList(BandPlan.allBands()));
        bandCb.setValue(initialBand != null ? initialBand : "20m");

        Label legendLbl = new Label(
                "  \u25A0 CW only   \u25A0 CW/Data   \u25A0 Phone/CW   \u25A0 FM   \u25A0 Satellite");
        legendLbl.getStyleClass().add("bp-legend");

        HBox header = new HBox(8, bandLbl, bandCb, new Region(), legendLbl);
        HBox.setHgrow(header.getChildren().get(2), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 10, 8, 10));
        header.getStyleClass().add("bp-header");

        // ── Table ─────────────────────────────────────────────────────
        TableView<BandSegment> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getStyleClass().add("bp-table");

        TableColumn<BandSegment, String> colFreq = col("Frequency (MHz)", 190,
                s -> s.freqRangeLabel());
        TableColumn<BandSegment, String> colMode = col("Usage / Mode", 145,
                s -> s.modeGroup().label());
        TableColumn<BandSegment, String> colLic  = col("Min. License", 100,
                s -> s.minLicense().label);
        TableColumn<BandSegment, String> colDesc = col("Segment", 150,
                BandSegment::description);
        TableColumn<BandSegment, String> colNotes = col("Common Frequencies / Notes", 230,
                BandSegment::notes);

        table.getColumns().addAll(colFreq, colMode, colLic, colDesc, colNotes);

        // Row factory: color by mode group + license
        table.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(BandSegment seg, boolean empty) {
                super.updateItem(seg, empty);
                getStyleClass().removeIf(c -> c.startsWith("bp-") && !c.equals("bp-table"));
                if (empty || seg == null) return;
                getStyleClass().add(seg.modeGroup().styleClass());
                getStyleClass().add(seg.minLicense().styleClass());
                // Highlight the row containing the current operating frequency
                if (currentFreqKhz > 0
                        && currentFreqKhz >= seg.startKhz()
                        && currentFreqKhz <= seg.endKhz()) {
                    getStyleClass().add("bp-current");
                }
            }
        });

        // ── License key ───────────────────────────────────────────────
        Label keyLbl = new Label(
                "License:  \u25CF Extra only   \u25CF General+   \u25CF All (incl. Technician)   " +
                "\u2605 = current operating frequency");
        keyLbl.getStyleClass().add("bp-key");
        keyLbl.setPadding(new Insets(4, 10, 4, 10));

        // ── Layout ────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setTop(header);
        root.setCenter(table);
        root.setBottom(keyLbl);

        // ── Theme ─────────────────────────────────────────────────────
        Scene scene = new Scene(root);
        JLogApp.applyTheme(scene);
        stage.setScene(scene);

        // ── Load data and scroll to current row ───────────────────────
        bandCb.valueProperty().addListener((obs, o, band) -> loadBand(table, band, currentFreqKhz));
        loadBand(table, bandCb.getValue(), currentFreqKhz);
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private static void loadBand(TableView<BandSegment> table, String band, double freqKhz) {
        List<BandSegment> segs = BandPlan.getSegments(band);
        table.setItems(FXCollections.observableArrayList(segs));

        // Scroll to the segment containing the current frequency
        if (freqKhz > 0) {
            for (int i = 0; i < segs.size(); i++) {
                BandSegment s = segs.get(i);
                if (freqKhz >= s.startKhz() && freqKhz <= s.endKhz()) {
                    final int idx = i;
                    javafx.application.Platform.runLater(() -> {
                        table.scrollTo(idx);
                        table.getSelectionModel().select(idx);
                    });
                    break;
                }
            }
        }
    }

    private static TableColumn<BandSegment, String> col(String title, double width,
            java.util.function.Function<BandSegment, String> fn) {
        TableColumn<BandSegment, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setSortable(false);
        c.setCellValueFactory(cell -> new SimpleStringProperty(fn.apply(cell.getValue())));
        return c;
    }
}
