package com.jlog.controller;

import com.jlog.db.QsoDao;
import com.jlog.model.QsoRecord;
import com.jlog.util.AppConfig;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Scale;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * QSL card printer. Prints a 4-up letter page of QSO data cells suitable for
 * pasting onto pre-printed card fronts, or for trimming and using directly as
 * minimalist QSL cards. Station call / grid / operator are pulled from
 * AppConfig and shown as a small return-address block per card.
 *
 * Source options:
 *   • Currently-selected rows from the Normal Log table
 *   • All QSOs where qsl_sent=false (pending outgoing cards)
 *
 * After successful printing the dialog optionally flips qsl_sent=true on
 * every printed record so the same cards don't queue up again.
 */
public class QslPrintWindow {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm 'UTC'");

    // 4-up layout on US Letter (8.5" × 11"): 2 cols × 2 rows, ~3.8" × 5" each
    // at 96 DPI after margins.
    private static final double PAGE_W = 816;   // 8.5 * 96
    private static final double PAGE_H = 1056;  // 11  * 96
    private static final double MARGIN = 36;    // 0.375"
    private static final int    COLS   = 2;
    private static final int    ROWS   = 2;
    private static final int    PER_PAGE = COLS * ROWS;

    private final Stage owner;
    private final Stage dialog = new Stage();
    private final List<QsoRecord> initialSelection;

    private final RadioButton rbSelection = new RadioButton("Selected rows");
    private final RadioButton rbUnsent    = new RadioButton("All unsent QSLs");
    private final CheckBox    cbMarkSent  = new CheckBox("Mark as QSL sent after printing");
    private final Label       lblCount    = new Label();
    private final VBox        previewBox  = new VBox(12);
    private final ScrollPane  preview     = new ScrollPane(previewBox);
    private final Label       status      = new Label();

    private List<QsoRecord> queue = new ArrayList<>();

    public QslPrintWindow(Stage owner, List<QsoRecord> initialSelection) {
        this.owner = owner;
        this.initialSelection = initialSelection != null ? initialSelection : List.of();
    }

    public void show() {
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Print QSL Cards");

        ToggleGroup tg = new ToggleGroup();
        rbSelection.setToggleGroup(tg);
        rbUnsent   .setToggleGroup(tg);
        if (initialSelection.isEmpty()) {
            rbSelection.setDisable(true);
            rbUnsent.setSelected(true);
        } else {
            rbSelection.setSelected(true);
        }
        cbMarkSent.setSelected(true);

        tg.selectedToggleProperty().addListener((obs, a, b) -> refresh());

        HBox options = new HBox(12, rbSelection, rbUnsent, cbMarkSent, lblCount);
        options.setAlignment(Pos.CENTER_LEFT);
        options.setPadding(new Insets(8));

        Button btnPrint  = new Button("Print…");
        Button btnCancel = new Button("Close");
        btnPrint.setDefaultButton(true);
        btnPrint.setOnAction(e -> doPrint());
        btnCancel.setOnAction(e -> dialog.close());
        HBox buttonBar = new HBox(8, status, btnPrint, btnCancel);
        HBox.setHgrow(status, Priority.ALWAYS);
        status.setMaxWidth(Double.MAX_VALUE);
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        buttonBar.setPadding(new Insets(8));

        preview.setFitToWidth(true);
        preview.setPrefViewportHeight(600);
        previewBox.setAlignment(Pos.TOP_CENTER);
        previewBox.setPadding(new Insets(12));

        BorderPane root = new BorderPane();
        root.setTop(options);
        root.setCenter(preview);
        root.setBottom(buttonBar);

        dialog.setScene(new Scene(root, 720, 800));
        refresh();
        dialog.show();
    }

    // -----------------------------------------------------------------
    // Queue building + preview
    // -----------------------------------------------------------------

    private void refresh() {
        queue.clear();
        try {
            if (rbSelection.isSelected()) {
                queue.addAll(initialSelection);
            } else {
                for (QsoRecord q : QsoDao.getInstance().fetchAll()) {
                    if (!q.isQslSent()) queue.add(q);
                }
            }
        } catch (Exception ex) {
            status.setText("Load failed: " + ex.getMessage());
        }
        lblCount.setText(queue.size() + " card(s)");
        rebuildPreview();
    }

    private void rebuildPreview() {
        previewBox.getChildren().clear();
        if (queue.isEmpty()) {
            previewBox.getChildren().add(new Label("No QSOs match."));
            return;
        }
        for (int i = 0; i < queue.size(); i += PER_PAGE) {
            List<QsoRecord> pageRecs = queue.subList(i, Math.min(i + PER_PAGE, queue.size()));
            previewBox.getChildren().add(buildPageNode(pageRecs));
        }
    }

    private Region buildPageNode(List<QsoRecord> pageRecs) {
        GridPane g = new GridPane();
        g.setPrefSize(PAGE_W, PAGE_H);
        g.setMinSize(PAGE_W, PAGE_H);
        g.setMaxSize(PAGE_W, PAGE_H);
        g.setStyle("-fx-background-color: white; -fx-border-color: #888; -fx-border-width: 1;");
        g.setHgap(0);
        g.setVgap(0);
        g.setPadding(new Insets(MARGIN));

        double cellW = (PAGE_W - 2 * MARGIN) / COLS;
        double cellH = (PAGE_H - 2 * MARGIN) / ROWS;

        for (int i = 0; i < PER_PAGE; i++) {
            int col = i % COLS;
            int row = i / COLS;
            Region cell;
            if (i < pageRecs.size()) {
                cell = buildCardCell(pageRecs.get(i), cellW, cellH);
            } else {
                cell = new Region();
                cell.setPrefSize(cellW, cellH);
            }
            g.add(cell, col, row);
        }
        return g;
    }

    private Region buildCardCell(QsoRecord q, double w, double h) {
        AppConfig cfg = AppConfig.getInstance();
        String stationCall = cfg.getStationCallsign();
        String grid        = cfg.getGridSquare();
        String op          = cfg.getOperatorName();
        String qth         = cfg.getQth();

        VBox box = new VBox(4);
        box.setPadding(new Insets(14));
        box.setPrefSize(w, h);
        box.setStyle("-fx-border-color: black; -fx-border-width: 1;");

        Label header = new Label(stationCall == null || stationCall.isBlank() ? "QSL" : stationCall);
        header.setFont(Font.font("Serif", FontWeight.BOLD, 24));

        Label sub = new Label(
            (op != null && !op.isBlank() ? op : "")
          + (grid != null && !grid.isBlank() ? "   " + grid : "")
          + (qth != null && !qth.isBlank() ? "   " + qth : "")
        );
        sub.setFont(Font.font("Serif", 11));

        Region spacer = new Region();
        spacer.setPrefHeight(10);

        Label toLine = new Label("Confirming QSO with  " + safe(q.getCallsign()));
        toLine.setFont(Font.font("Serif", FontWeight.BOLD, 14));

        String dateStr = q.getDateTimeUtc() != null ? DATE_FMT.format(q.getDateTimeUtc()) : "";
        String timeStr = q.getDateTimeUtc() != null ? TIME_FMT.format(q.getDateTimeUtc()) : "";

        GridPane info = new GridPane();
        info.setHgap(10);
        info.setVgap(3);
        int r = 0;
        addKv(info, r++, "Date",  dateStr);
        addKv(info, r++, "UTC",   timeStr);
        addKv(info, r++, "Band",  safe(q.getBand()));
        addKv(info, r++, "Mode",  safe(q.getMode()));
        addKv(info, r++, "RST Sent", safe(q.getRstSent()));
        if (q.getFrequency() != null && !q.getFrequency().isBlank()) {
            addKv(info, r++, "Freq", q.getFrequency());
        }

        Region spacer2 = new Region();
        VBox.setVgrow(spacer2, Priority.ALWAYS);

        Label tnx = new Label("PSE  ☐     TNX  ☐     73!");
        tnx.setFont(Font.font("Serif", FontWeight.BOLD, 12));

        box.getChildren().addAll(header, sub, spacer, toLine, info, spacer2, tnx);
        return box;
    }

    private static void addKv(GridPane g, int row, String k, String v) {
        Label lk = new Label(k + ":");
        lk.setFont(Font.font("Serif", FontWeight.BOLD, 11));
        Label lv = new Label(v == null ? "" : v);
        lv.setFont(Font.font("Serif", 11));
        g.add(lk, 0, row);
        g.add(lv, 1, row);
    }

    private static String safe(String s) { return s == null ? "" : s; }

    // -----------------------------------------------------------------
    // Print
    // -----------------------------------------------------------------

    private void doPrint() {
        if (queue.isEmpty()) { status.setText("Nothing to print."); return; }

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { status.setText("No printer available."); return; }
        if (!job.showPrintDialog(dialog)) return;

        Printer printer = job.getPrinter();
        PageLayout pl = printer.createPageLayout(
            Paper.NA_LETTER, PageOrientation.PORTRAIT, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(pl);

        boolean allOk = true;
        for (int i = 0; i < queue.size(); i += PER_PAGE) {
            List<QsoRecord> pageRecs = queue.subList(i, Math.min(i + PER_PAGE, queue.size()));
            Region pageNode = buildPageNode(pageRecs);
            // Render off-screen so layout resolves before print.
            new Scene(new Group(pageNode));
            pageNode.applyCss();
            pageNode.layout();

            double sx = pl.getPrintableWidth()  / PAGE_W;
            double sy = pl.getPrintableHeight() / PAGE_H;
            double s  = Math.min(sx, sy);
            pageNode.getTransforms().add(new Scale(s, s));

            if (!job.printPage(pl, pageNode)) { allOk = false; break; }
        }
        job.endJob();

        if (!allOk) { status.setText("Print failed."); return; }
        status.setText("Printed " + queue.size() + " card(s).");

        if (cbMarkSent.isSelected()) markQueueAsSent();
    }

    private void markQueueAsSent() {
        try {
            for (QsoRecord q : queue) {
                q.setQslSent(true);
                QsoDao.getInstance().update(q);
            }
            status.setText("Printed " + queue.size() + " card(s). Marked as QSL sent.");
        } catch (Exception ex) {
            status.setText("Printed, but mark-sent failed: " + ex.getMessage());
        }
    }

    // Workaround: Region must be attached to a Scene before applyCss() resolves
    // layout; use a throwaway Group as the root.
    private static final class Group extends javafx.scene.Group {
        Group(javafx.scene.Node child) { getChildren().add(child); }
    }
}
