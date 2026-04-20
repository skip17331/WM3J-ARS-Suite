package com.jlog.controller;

import com.jlog.app.JLogApp;
import com.jlog.util.BandPlan;
import com.jlog.util.BandPlan.BandSegment;
import com.jlog.util.BandPlan.LicenseClass;
import com.jlog.util.BandPlan.ModeGroup;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.List;

/**
 * BandPlanWindow — graphical US FCC Part 97 band plan reference.
 *
 * HF bands show three stacked license-class rows (Extra / General / Tech),
 * matching the ARRL band-plan chart layout. VHF/UHF bands where all segments
 * are open to all classes show a single unified bar. Mode type is color-coded
 * across all rows. The band containing {@code currentFreqKhz} is highlighted
 * with a vertical marker and scrolled into view.
 */
public class BandPlanWindow {

    // Row heights
    private static final double CLASS_BAR_H  = 11;  // per license-class sub-bar (HF)
    private static final double SINGLE_BAR_H = 22;  // unified bar (VHF/UHF)
    private static final double TICK_H       = 13;
    private static final double CLASS_GAP    =  1;  // gap between sub-bars

    // Column widths
    private static final double NAME_W      = 68;
    private static final double CLASS_COL_W = 16;   // "E" / "G" / "T" labels
    private static final double RANGE_W     = 150;

    private final Stage stage;

    public BandPlanWindow(Window owner, String initialBand, double currentFreqKhz) {
        stage = new Stage();
        stage.setTitle("US Amateur Band Plan  —  FCC Part 97");
        stage.setMinWidth(700);
        stage.setMinHeight(400);
        stage.setWidth(960);
        stage.setHeight(680);
        if (owner != null) stage.initOwner(owner);

        // ── Chart content ───────────────────────────────────────────────
        VBox bandsBox = new VBox(3);
        bandsBox.setPadding(new Insets(8, 12, 10, 12));
        bandsBox.getStyleClass().add("bp-chart");

        // Column headers
        Label hName  = new Label("Band");
        hName.setPrefWidth(NAME_W + CLASS_COL_W + 4);
        hName.getStyleClass().add("bp-col-hdr");
        Label hBar   = new Label("Frequency Allocation");
        hBar.getStyleClass().add("bp-col-hdr");
        HBox.setHgrow(hBar, Priority.ALWAYS);
        Label hRange = new Label("Range");
        hRange.setPrefWidth(RANGE_W);
        hRange.getStyleClass().add("bp-col-hdr");
        HBox colHdr = new HBox(6, hName, hBar, hRange);
        colHdr.setAlignment(Pos.CENTER_LEFT);
        colHdr.setPadding(new Insets(0, 0, 4, 0));
        bandsBox.getChildren().addAll(colHdr, new Separator());

        for (String band : BandPlan.allBands()) {
            List<BandSegment> segs = BandPlan.getSegments(band);
            if (segs.isEmpty()) continue;

            double bandStart = segs.get(0).startKhz();
            double bandEnd   = segs.get(segs.size() - 1).endKhz();
            double bandSpan  = bandEnd - bandStart;

            boolean isCurrentBand = currentFreqKhz > 0
                    && currentFreqKhz >= bandStart
                    && currentFreqKhz <= bandEnd;

            // HF bands have license-class tiers; VHF/UHF bands open to all → single bar
            boolean tiered = segs.stream().anyMatch(s -> s.minLicense() != LicenseClass.ALL);

            // ── Band name ──────────────────────────────────────────────
            Label nameLbl = new Label(band);
            nameLbl.setPrefWidth(NAME_W);
            nameLbl.setAlignment(Pos.CENTER_RIGHT);
            nameLbl.getStyleClass().add("bp-band-name");
            if (isCurrentBand) nameLbl.getStyleClass().add("bp-band-name-active");

            // ── Class label column ─────────────────────────────────────
            VBox classCol = new VBox(CLASS_GAP);
            classCol.setPrefWidth(CLASS_COL_W);
            classCol.setMinWidth(CLASS_COL_W);
            classCol.setAlignment(Pos.TOP_RIGHT);
            if (tiered) {
                classCol.getChildren().addAll(
                    classLbl("E", CLASS_BAR_H),
                    classLbl("G", CLASS_BAR_H),
                    classLbl("T", CLASS_BAR_H));
            }

            // ── Bar area (VBox of sub-bars + tick pane) ────────────────
            VBox barArea = new VBox(CLASS_GAP);
            HBox.setHgrow(barArea, Priority.ALWAYS);

            if (tiered) {
                // Three rows: tier 0 = Extra, tier 1 = General, tier 2 = Tech/All
                // Condition: show segment in tier t if seg.minLicense().ordinal() >= t
                //   EXTRA=0, GENERAL=1, ALL=2
                //   Extra row (t=0): shows all segments   (ordinal >= 0)
                //   General row (t=1): shows General+All  (ordinal >= 1)
                //   Tech row (t=2): shows All only        (ordinal >= 2)
                for (int tier = 0; tier < 3; tier++) {
                    Pane cp = new Pane();
                    cp.setPrefHeight(CLASS_BAR_H);
                    cp.setMinHeight(CLASS_BAR_H);
                    final int t = tier;
                    for (BandSegment seg : segs) {
                        if (seg.minLicense().ordinal() < t) continue;
                        addSegRegion(cp, seg, bandStart, bandSpan, CLASS_BAR_H);
                    }
                    barArea.getChildren().add(cp);
                }
            } else {
                // Single unified bar
                Pane cp = new Pane();
                cp.setPrefHeight(SINGLE_BAR_H);
                cp.setMinHeight(SINGLE_BAR_H);
                for (BandSegment seg : segs) {
                    addSegRegion(cp, seg, bandStart, bandSpan, SINGLE_BAR_H);
                }
                barArea.getChildren().add(cp);
            }

            // Tick labels at each segment's start frequency
            Pane tickPane = new Pane();
            tickPane.setPrefHeight(TICK_H);
            tickPane.setMinHeight(TICK_H);
            for (BandSegment seg : segs) {
                double relPos = (seg.startKhz() - bandStart) / bandSpan;
                Label tick = new Label(tickLabel(seg.startKhz()));
                tick.getStyleClass().add("bp-tick");
                tick.setLayoutY(1);
                tick.layoutXProperty().bind(tickPane.widthProperty().multiply(relPos).add(2));
                tickPane.getChildren().add(tick);
            }
            barArea.getChildren().add(tickPane);

            // ── Current-frequency marker ───────────────────────────────
            // Drawn in the first bar pane; extends visually through all rows
            // (Pane does not clip children, so the line spans naturally)
            if (isCurrentBand) {
                double relPos = (currentFreqKhz - bandStart) / bandSpan;
                Pane topPane = (Pane) barArea.getChildren().get(0);
                double markerH = tiered
                    ? (3 * CLASS_BAR_H + 2 * CLASS_GAP)
                    : SINGLE_BAR_H;

                Line line = new Line(0, 1, 0, markerH - 1);
                line.getStyleClass().add("bp-marker-line");
                line.layoutXProperty().bind(topPane.widthProperty().multiply(relPos));
                line.setLayoutY(0);

                Polygon tri = new Polygon(-4.0, 1.0, 4.0, 1.0, 0.0, 8.0);
                tri.getStyleClass().add("bp-marker-tri");
                tri.layoutXProperty().bind(topPane.widthProperty().multiply(relPos));
                tri.setLayoutY(0);

                topPane.getChildren().addAll(line, tri);
            }

            // ── Frequency range ────────────────────────────────────────
            Label rangeLbl = new Label(freqRangeStr(bandStart, bandEnd));
            rangeLbl.setPrefWidth(RANGE_W);
            rangeLbl.setAlignment(Pos.TOP_LEFT);
            rangeLbl.getStyleClass().add("bp-band-mhz");

            HBox row = new HBox(4, nameLbl, classCol, barArea, rangeLbl);
            row.setAlignment(Pos.TOP_LEFT);
            row.getStyleClass().add("bp-band-row");
            if (isCurrentBand) row.getStyleClass().add("bp-active-row");

            bandsBox.getChildren().add(row);
        }

        ScrollPane scroll = new ScrollPane(bandsBox);
        scroll.setFitToWidth(true);

        HBox legend = buildLegend();
        legend.getStyleClass().add("bp-header");

        Label key = new Label(
            "HF rows:  E = Extra   G = General   T = Technician/All    "
            + "▲ = current operating frequency   Hover segment for details");
        key.getStyleClass().add("bp-key");
        key.setPadding(new Insets(5, 10, 5, 10));

        BorderPane root = new BorderPane();
        root.setTop(legend);
        root.setCenter(scroll);
        root.setBottom(key);

        Scene scene = new Scene(root);
        JLogApp.applyTheme(scene);
        stage.setScene(scene);

        // Auto-scroll to active band
        if (currentFreqKhz > 0) {
            Platform.runLater(() -> {
                List<String> bands = BandPlan.allBands();
                for (int i = 0; i < bands.size(); i++) {
                    List<BandSegment> segs = BandPlan.getSegments(bands.get(i));
                    if (segs.isEmpty()) continue;
                    if (currentFreqKhz >= segs.get(0).startKhz()
                            && currentFreqKhz <= segs.get(segs.size() - 1).endKhz()) {
                        double vval = (double)(i + 2) / (bands.size() + 2);
                        scroll.setVvalue(Math.max(0.0, vval - 0.12));
                        break;
                    }
                }
            });
        }
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    // ── Segment region builder ──────────────────────────────────────────

    private static void addSegRegion(Pane cp, BandSegment seg,
                                     double bandStart, double bandSpan, double barH) {
        double relStart = (seg.startKhz() - bandStart) / bandSpan;
        double relWidth = (seg.endKhz()   - seg.startKhz()) / bandSpan;

        Region r = new Region();
        r.setPrefHeight(barH);
        r.setMinHeight(barH);
        r.setLayoutY(0);
        r.layoutXProperty().bind(cp.widthProperty().multiply(relStart));
        r.prefWidthProperty().bind(cp.widthProperty().multiply(relWidth));
        r.getStyleClass().addAll("bp-seg", modeClass(seg.modeGroup()));

        Tooltip tt = new Tooltip(
            seg.description() + "\n"
            + seg.freqRangeLabel() + " MHz\n"
            + seg.modeGroup().label() + " · " + seg.minLicense().label
            + (seg.notes().isEmpty() ? "" : "\n" + seg.notes()));
        Tooltip.install(r, tt);
        cp.getChildren().add(r);
    }

    // ── Legend ──────────────────────────────────────────────────────────

    private static HBox buildLegend() {
        HBox box = new HBox(10);
        box.setPadding(new Insets(7, 12, 7, 12));
        box.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("US Amateur Band Plan  —  FCC Part 97");
        title.getStyleClass().add("bp-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        box.getChildren().addAll(title, spacer);

        addSwatch(box, "bp-seg-cw",    "CW Only");
        addSwatch(box, "bp-seg-data",  "CW / Data");
        addSwatch(box, "bp-seg-phone", "Phone / CW");
        addSwatch(box, "bp-seg-fm",    "FM");
        addSwatch(box, "bp-seg-sat",   "Satellite");
        addSwatch(box, "bp-seg-usb",   "USB Only");
        return box;
    }

    private static void addSwatch(HBox parent, String styleClass, String label) {
        Region sw = new Region();
        sw.setPrefSize(14, 10);
        sw.getStyleClass().addAll("bp-seg", styleClass);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("bp-legend-txt");
        HBox item = new HBox(3, sw, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        parent.getChildren().add(item);
    }

    private static Label classLbl(String text, double height) {
        Label l = new Label(text);
        l.setPrefHeight(height);
        l.setMinHeight(height);
        l.setMaxHeight(height);
        l.setAlignment(Pos.CENTER);
        l.getStyleClass().add("bp-class-lbl");
        return l;
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private static String modeClass(ModeGroup mg) {
        return switch (mg) {
            case CW_ONLY   -> "bp-seg-cw";
            case CW_DATA   -> "bp-seg-data";
            case PHONE     -> "bp-seg-phone";
            case FM        -> "bp-seg-fm";
            case SATELLITE -> "bp-seg-sat";
            case USB_ONLY  -> "bp-seg-usb";
        };
    }

    private static String tickLabel(double khz) {
        if (khz < 1000) return String.format("%.1f", khz);
        return mhzStr(khz / 1000.0);
    }

    private static String freqRangeStr(double startKhz, double endKhz) {
        if (startKhz < 1000) return String.format("%.1f – %.1f kHz", startKhz, endKhz);
        return mhzStr(startKhz / 1000.0) + " – " + mhzStr(endKhz / 1000.0) + " MHz";
    }

    private static String mhzStr(double mhz) {
        if (mhz == Math.floor(mhz)) return String.format("%.0f", mhz);
        String s = String.format("%.3f", mhz).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
